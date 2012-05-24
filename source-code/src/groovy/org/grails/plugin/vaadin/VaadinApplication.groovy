package org.grails.plugin.vaadin

import java.net.URL;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.vaadin.Application;
import com.vaadin.service.ApplicationContext;
import com.vaadin.terminal.Terminal;
import com.vaadin.terminal.gwt.server.HttpServletRequestListener;

import org.apache.commons.logging.LogFactory
import org.grails.plugin.vaadin.utils.Stopwatch;

/**
 * Users should override this class, when creating the Application class for
 * their Vaadin project.
 * <p>
 * This class provides enhancements to the basic Vaadin functionality,
 * for implementing a "Model-View-Controller"-type interface.
 * <p>
 * Specifically, it does the following:
 * <ul>
 * <li>Injects a base API into all project Vaadin classes. See
 * {@link org.grails.plugin.vaadin.VaadinApi}</li>
 * <li>Starts listening to changes to the fragment in the browser
 * URL, and dispatches requests to models / views accordingly.</li>
 * </ul>
 * Note that this functionality is optional - a user can disable this
 * by simply overriding the com.vaadin.Application class directly.
 * 
 * @author Francis McKenzie
 */
abstract class VaadinApplication extends Application implements HttpServletRequestListener {
    static log = LogFactory.getLog(VaadinApplication.class)
    
    /**
     * System messages configured from Vaadin config
     */
    static Application.CustomizedSystemMessages systemMessages
    /**
     * Lazy-creation of system messages configured from Vaadin config
     */
    public static Application.CustomizedSystemMessages getSystemMessages() {
        if (!systemMessages) {
            synchronized(VaadinApplication) {
                if (!systemMessages) {
                    systemMessages = getBean("vaadinSystemMessages")
                }
            }
        }
    }
    /**
     * The dispatcher for routing requests
     */
    VaadinDispatcher dispatcher

    /**
     * Injects Vaadin API into all project Vaadin classes,
     * and starts URL Fragment listener.
     */
    @Override
    public void start(URL applicationUrl, Properties applicationProperties,
        ApplicationContext context) {
        
        // Log start status
        log.info "STARTING: ${this.class.simpleName} ${applicationUrl}....."
        
        // Timing
        def stopwatch = Stopwatch.enabled ? new Stopwatch("Startup", this.class) : null
        
        // Start the dispatcher
        dispatcher = new VaadinDispatcher(this)
        
        // Trigger call to init() method
        super.start(applicationUrl, applicationProperties, context)
        
        // Start listener to respond to URI fragment changes
        // This will dispatch /home/index if no page has been shown yet
        dispatcher.startFragmentListener()
        
        // Log start status
        log.info "STARTED ${this.class.simpleName}"
        
        // Log timing
        stopwatch?.stop()
    }
    
    /**
     * Fixes an issue where a browser refresh does not trigger a repaint of the
     * interface, if the refresh fails to expire the session.
     * <p>
     * In this case, we manually dispatch the existing 'request' again.
     */
    public void onRequestStart(HttpServletRequest request, HttpServletResponse response) {
        // Log
        if (log.isDebugEnabled()) {
            log.debug "${request.requestURL}${request.queryString?'?'+request.queryString:''}"
        }
        
        // Set the application
        VaadinApplicationContextHolder.setVaadinApplication(this)
        
        // Ignore /appName/UIDL requests - these are the AJAX calls
        // Otherwise, assume it's the main request
        // Then, if the app is already running we have a problem - no repaint
        // will be called. So manually trigger the refresh.
        if (isBrowserRefresh(request)) {
            // Log
            if (log.isDebugEnabled()) {
                log.debug "Browser refresh detected!"
            }
            
            // Refresh
            if (dispatcher) {
                dispatcher.refresh()
            } else {
                // Log
                if (log.isDebugEnabled()) {
                    log.debug "No dispatcher to refresh!"
                }
            }
        }
    }
    
    /**
     * Clears the Vaadin Application stored in ThreadLocal.
     */
    public void onRequestEnd(HttpServletRequest request, HttpServletResponse response) {
        // Clear the application
        VaadinApplicationContextHolder.setVaadinApplication(null)
    }
    
    /**
     * Detects if specified request is a browser refresh
     */
    protected boolean isBrowserRefresh(HttpServletRequest request) {
        String url = request.requestURL.toString()
        // Must already be running for it to be a refresh
        return isRunning() &&
            // AJAX
            !url.endsWith("UIDL") &&
            // File uploads
            !url.contains("/UPLOAD/") &&
            // Links to StreamResources - e.g. /APP/1/someimage.png
            ! (url =~ /\/\d+\//)
    }
}
