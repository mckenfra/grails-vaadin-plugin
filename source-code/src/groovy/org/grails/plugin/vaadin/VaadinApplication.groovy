package org.grails.plugin.vaadin

import java.net.URL;
import java.util.Properties;

import com.vaadin.Application;
import com.vaadin.service.ApplicationContext;

import org.apache.commons.logging.LogFactory

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
abstract class VaadinApplication extends Application {
    def log = LogFactory.getLog(this.class)
    
    /**
     * Injects Vaadin API into all project Vaadin classes,
     * and starts URL Fragment listener.
     */
    @Override
    public void start(URL applicationUrl, Properties applicationProperties,
        ApplicationContext context) {
        
        // Log starting status
        if (log.isDebugEnabled()) {
            log.debug "STARTING: ${this}"
        }
        
        // Inject API into all Vaadin classes
        def vaadinApi = getBean("vaadinApi")
        def grailsApp = getBean("grailsApplication")
        if (! vaadinApi || ! grailsApp) {
            throw new NullPointerException("Unable to retrieve spring beans 'vaadinApi' or 'grailsApplication'")
        } else {
            VaadinClasses classes = new VaadinClasses(grailsApp)
            vaadinApi.injectApi(this, classes)
        }
        
        // Trigger call to init() method
        super.start(applicationUrl, applicationProperties, context)
        
        // Start listener to respond to URI fragment changes
        dispatcher.startFragmentListener()
        
        // Log started status
        if (log.isDebugEnabled()) {
            log.debug "STARTED: ${this}"
        }
    }
}
