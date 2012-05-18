package org.grails.plugin.vaadin

import com.vaadin.Application
import com.vaadin.grails.VaadinUtils
import com.vaadin.ui.Component;

import grails.persistence.Entity
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.i18n.LocaleContextHolder

/**
 * Injects the Vaadin API into all Vaadin Classes and Controllers.
 * <p>
 * A class is considered a Vaadin class if it meets the following requirements:
 * <ul>
 * <li>Exists under the <code>grails-app</code> directory</li>
 * <li>Has a name ending <code>VaadinController</code> or has the word <code>vaadin</code> in its package name, or the package name of any of its superclasses.</li>
 * </ul>
 * 
 * <h3>Vaadin Controllers</h3>
 * <ul>
 * <li><b>vaadinApplication</b>: Gets the com.vaadin.Application object for this session.</li>
 * <li><b>message()</b>: Provides same functionality as the Grails 'message' tag</li>
 * <li><b>flash</b>: Gets a Grails-like 'flash' object for the current 'request'</li>
 * <li><b>params</b>: Gets params map for the current 'request'</li>
 * <li><b>redirect</b>: Mimics Grails's redirect function for controllers</li>
 * <li><b>render</b>: Mimics Grails's render function for controllers</li>
 * </ul>
 * 
 * <h3>All Vaadin Classes</h3>
 * <ul>
 * <li><b>i18n()</b>: Provides same functionality as the Grails 'message' tag</li>
 * <li><b>getBean()</b>: Specify a Spring bean to retrieve by name</li>
 * </ul>
 * 
 * @author Francis McKenzie
 */
class VaadinApi {
    def log = LogFactory.getLog(this.class)
    
    /**
     * Injects the Vaadin API into the Vaadin classes and controllers in the specified application.
     * 
     * @param application The application that contains the Vaadin classes and controllers.
     */
    def injectApi(GrailsApplication application) {
        // Vaadin Classes
        application.vaadinClasses.each { injectApi(it) }
        // Vaadin Controllers
        application.getArtefacts("Controller").findAll { it.name.endsWith("Vaadin") }.each { injectApi(it) }
    }
    
    /**
     * Injects the Vaadin API into the specified Vaadin class
     * 
     * @param vaadinClass The Vaadin class requiring API injection
     */
    def injectApi(GrailsClass grailsClass) {
        injectApi(grailsClass.clazz)
    }
    
    /**
     * Injects the Vaadin API into the specified Vaadin class
     * 
     * @param vaadinClass The Vaadin class requiring API injection
     */
    def injectApi(Class clazz) {
        if (!isExcludedClass(clazz)) {
            
            // Base API
            injectBaseApi(clazz)
            
            // Controllers API
            if (clazz.name.endsWith("VaadinController")) {
                injectControllersApi(clazz)
            }
        }
    }
    
    /**
     * Injects the Vaadin Base Api into the specified Vaadin class
     * 
     * @param vaadinClass The Vaadin class requiring API injection
     */
    protected void injectBaseApi(Class clazz) {
        while(! (clazz.interface ||
            clazz.name.startsWith("java") ||
            clazz.name.startsWith("com.vaadin.") ||
            clazz.metaClass.methods.find { it.name == 'i18n' })) {
            if (log.isDebugEnabled()) {
                log.debug "BASE API: ${clazz}"
            }
            // Application
            clazz.metaClass.'static'.getVaadinApplication = {->
                return VaadinApplicationContextHolder.vaadinApplication
            }
            // i18n methods
            clazz.metaClass.'static'.i18n = {String key, Collection args = null, Locale locale = LocaleContextHolder.getLocale() ->
                Object[] oArgs = args ? args as Object[] : null
                return VaadinUtils.i18n(key, oArgs, locale)
            }
            clazz.metaClass.'static'.i18n = {String key, String defaultMsg, Collection args = null, Locale locale = LocaleContextHolder.getLocale() ->
                Object[] oArgs = args ? args as Object[] : null
                return VaadinUtils.i18n(key, oArgs, defaultMsg, locale);
            }
            clazz.metaClass.'static'.i18n = {MessageSourceResolvable resolvable, Locale locale = LocaleContextHolder.getLocale() ->
                return VaadinUtils.i18n(resolvable, locale);
            }
            // Dynamic Spring bean instance lookup
            clazz.metaClass.'static'.getBean = { String name ->
                return VaadinUtils.getBean(name)
            }
            clazz.metaClass.'static'.getBean = { Class type ->
                return VaadinUtils.getBean(type)
            }
            
            // Loop to superclass
            clazz = clazz.superclass
        }
    }
    
    /**
     * Injects the Vaadin Controllers Api into the specified Vaadin class
     * 
     * @param vaadinClass The Vaadin class requiring API injection
     */
    protected void injectControllersApi(Class clazz) {
        if (log.isDebugEnabled()) {
            log.debug("CONTROLLERS API: ${clazz}")
        }

        // Application
        clazz.metaClass.getVaadinApplication = {
            VaadinRequestContextHolder.requestAttributes.vaadinApplication
        }
        // Params
        clazz.metaClass.getParams = {
            VaadinRequestContextHolder.requestAttributes?.params
        }
        // Flash
        clazz.metaClass.getFlash = {->
            VaadinRequestContextHolder.requestAttributes?.flash
        }
        // Redirect
        clazz.metaClass.redirect = {Map args ->
            VaadinRequestContextHolder.requestAttributes?.redirect(args)
        }
        // Render
        clazz.metaClass.render = {Map args ->
            VaadinRequestContextHolder.requestAttributes?.render(args)
        }
        // Render
        clazz.metaClass.render = {String view ->
            VaadinRequestContextHolder.requestAttributes?.render(view)
        }
        // Render
        clazz.metaClass.render = {Component component ->
            VaadinRequestContextHolder.requestAttributes?.render(component)
        }
    }
    
    /**
     * Checks if specified class should be excluded from Api injection
     * 
     * @param clazz The class to check
     * @return True if the class should be excluded form Api injection
     */
    protected boolean isExcludedClass(Class clazz) {
        // Don't inject if there's an explicit annotation
        return clazz.isAnnotationPresent(NoVaadinApi.class) ||
            // Don't inject domain objects
            clazz.isAnnotationPresent(Entity.class)
    }
}
