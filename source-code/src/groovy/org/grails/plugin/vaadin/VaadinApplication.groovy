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

/**
 * A Vaadin Application that supports configuring SystemMessages
 * in <code>grails-app/conf/VaadinConfig.groovy</code> and provides
 * easy access to the application in the current thread using
 * {@link org.grails.plugin.vaadin.VaadinApplicationContextHolder}
 * <p>
 * Note that this functionality is optional - a user can disable this
 * by simply overriding the {@link com.vaadin.Application} class directly.
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
        if (systemMessages) {
			return systemMessages
        } else {
            log.warn("Customized System Messages not found!")
            return Application.getSystemMessages()
        }
    }

    /**
     * Stores the application in the current thread using
     * {@link org.grails.plugin.vaadin.VaadinApplicationContextHolder}
     */
    public void onRequestStart(HttpServletRequest request, HttpServletResponse response) {
        // Log
        if (log.isDebugEnabled()) {
            log.debug "${request.requestURL}${request.queryString?'?'+request.queryString:''}"
        }
        
        // Set the application
        VaadinApplicationContextHolder.setVaadinApplication(this)
    }
    
    /**
     * Clears the application in the current thread using
     * {@link org.grails.plugin.vaadin.VaadinApplicationContextHolder}
     */
    public void onRequestEnd(HttpServletRequest request, HttpServletResponse response) {
        // Clear the application
        VaadinApplicationContextHolder.setVaadinApplication(null)
    }
}
