package org.grails.plugin.vaadin

import com.vaadin.Application
import com.vaadin.ui.Component;

import grails.persistence.Entity
import org.apache.commons.logging.LogFactory
import org.grails.plugin.vaadin.services.VaadinApplicationService
import org.springframework.context.i18n.LocaleContextHolder

/**
 * Injects the Vaadin API into all Vaadin classes.
 * <p>
 * Note that a class is considered a Vaadin class if it:
 * <ul>
 * <li>Has 'vaadin' in the package name of itself, or any of its superclasses.</li>
 * <li>Has 'VaadinController' at the end of the class name.</li>
 * <li>Is located under the grails-app directory</li>
 * </ul>
 * <p>
 * Note that injection can be disabled for a particular class by annotating
 * with the {@link org.grails.plugin.vaadin.NoVaadinApi} annotation.
 * <p>
 * The Vaadin API injects the following methods, depending on the type of the
 * Vaadin Class:
 * <h3>All Vaadin Classes</h3>
 * <ul>
 * <li><b>application</b>: Gets the com.vaadin.Application object for this session.</li>
 * <li><b>flash</b>: Gets a Grails-like 'flash' object for the current 'request'</li>
 * <li><b>message()</b>: Provides same functionality as the Grails 'message' tag</li>
 * <li><b>getBean()</b>: Specify a Spring bean to retrieve by name</li>
 * <li><b>dispatcher</b>: Use this to dispatch requests to display different Vaadin pages.
 * See {@link org.grails.plugin.vaadin.VaadinDispatcher}</li>
 * </ul>
 * 
 * <h3>Vaadin Controllers</h3>
 * <ul>
 * <li><b>params</b>: Gets params map for the current 'request'</li>
 * <li><b>redirect</b>: Mimics Grails's redirect function for controllers</li>
 * <li><b>render</b>: Mimics Grails's render function for controllers</li>
 * </ul>
 * 
 * <p>
 * Please note that in the above description, 'request' does not refer to a
 * JavaEE ServletRequest. It is an internal construct for referring to a call
 * to the dispatch() method of {@link org.grails.plugin.vaadin.VaadinDispatcher},
 * in order to display a particular Vaadin 'page', using a VaadinController class
 * and a GSP view.
 * <p>
 * The resulting construction of Vaadin Components in the GSP view may
 * lead to round trip requests to the server, but this is all hidden by Vaadin
 * and handled automatically with Ajax.
 * Refer to <a href="http://vaadin.com">http://vaadin.com</a> for more information.  
 * 
 * @author Francis McKenzie
 */
class VaadinApi {
    def log = LogFactory.getLog(this.class)
    
    /**
     * Injected - session-scoped proxy for holding reference to the app
     */
    def vaadinApplicationHolder
    /**
     * Injected - session-scoped, required by dispatcher
     */
    def vaadinTransactionManager
    
    /**
     * Injects the Vaadin API into the specified Vaadin Classes using
     * the specified Vaadin Application.
     * @param application The current Vaadin Application (should be session-scoped)
     * @param vaadinClasses Holds all Vaadin Classes and Artefacts.
     */
    def injectApi(Application application, VaadinClasses vaadinClasses) {
        if (log.isDebugEnabled()) {
            log.debug("APPLICATION: ${application?.class}")
        }
        
        // Update the session-scoped service
        vaadinApplicationHolder.application = application

        // All classes
        injectBaseApi(vaadinClasses)
        
        // Controllers
        injectControllersApi(vaadinClasses)
        
        // Start the dispatcher
        application.dispatcher.init(vaadinClasses.getArtefacts("controller"))
    }

    /**
     * Injects the Vaadin Base API into the specified Vaadin Classes.
     * 
     * @param vaadinClasses Holds all Vaadin Classes and Artefacts.
     */
    protected injectBaseApi(VaadinClasses vaadinClasses) {
        final dispatcher = new VaadinDispatcher()
        dispatcher.vaadinTransactionManager = vaadinTransactionManager
        dispatcher.vaadinApplicationHolder = vaadinApplicationHolder
        vaadinClasses.allClasses.each { clazz ->
            // Don't inject if there's an explicit annotation
            if ( clazz.isAnnotationPresent(NoVaadinApi.class) ||
                // Don't inject domain objects
                clazz.isAnnotationPresent(Entity.class) ||
                // Don't inject our internal classes
                clazz in this.excludedClasses) {
                if (log.isDebugEnabled()) {
                    log.debug("SKIPPING: ${clazz} -> ANNOTATIONS: ${clazz.declaredAnnotations}")
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("INJECTING API: ${clazz} -> ANNOTATIONS: ${clazz.declaredAnnotations}")
                }

                // Dispatcher
                clazz.metaClass.getDispatcher = {-> dispatcher }
                
                if (clazz != vaadinApplicationHolder.application.class) {
                    // Application
                    clazz.metaClass.getApplication = {-> vaadinApplicationHolder.application }
                    // Beans
                    clazz.metaClass.getBean << { String name ->
                        return vaadinApplicationHolder.application.getBean(name)
                    }
                    // Beans
                    clazz.metaClass.getBean << { Class type ->
                        return vaadinApplicationHolder.application.getBean(type)
                    }
                }
            }
        }
    }
    
    /**
     * Injects the Vaadin Controllers API into the specified Vaadin Classes.
     * 
     * @param vaadinClasses Holds all Vaadin Classes and Artefacts.
     */
    protected injectControllersApi(VaadinClasses vaadinClasses) {
        vaadinClasses.getArtefacts("controller").each { artefact ->
            // VaadinClass
            Class clazz = artefact.clazz
            clazz.metaClass.static.getVaadinClass = {-> artefact }
            // Message
            clazz.metaClass.message = {Map args ->
                if (args?.error) {
                    return vaadinApplicationHolder.application.i18n(args?.error, (args?.locale ?: LocaleContextHolder.getLocale()))
                } else {
                    return vaadinApplicationHolder.application.i18n(args?.code, args?.default, args?.args, (args?.locale ?: LocaleContextHolder.getLocale()))
                }
            }
            // Params
            clazz.metaClass.getParams = {
                vaadinApplicationHolder.application.dispatcher.activeRequest.params
            }
            // Flash
            clazz.metaClass.getFlash = {->
                vaadinApplicationHolder.application.dispatcher.activeRequest.flash
            }
            // Redirect
            clazz.metaClass.redirect = {Map args ->
                vaadinApplicationHolder.application.dispatcher.activeRequest.redirect(args)
            }
            // Render
            clazz.metaClass.render = {Map args ->
                vaadinApplicationHolder.application.dispatcher.activeRequest.render(args)
            }
            // Render
            clazz.metaClass.render = {String view ->
                vaadinApplicationHolder.application.dispatcher.activeRequest.render(view)
            }
            // Render
            clazz.metaClass.render = {Component component ->
                vaadinApplicationHolder.application.dispatcher.activeRequest.render(component)
            }
        }
    }
    
    /**
     * Retrieve classes that should be excluded from API-injection.
     * <p>
     * Note that only classes in grails-app subdirectories, or classes with
     * recognised name suffixes (e.g. *VaadinController) will be considered
     * for api injection
     * @return List of classes to exclude from API-injection
     */
    protected List<Class> getExcludedClasses() {
        return [
            VaadinApplicationService.class
        ]
    }
}
