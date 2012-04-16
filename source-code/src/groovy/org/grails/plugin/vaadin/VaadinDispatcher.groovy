package org.grails.plugin.vaadin

import org.codehaus.groovy.grails.plugins.metadata.GrailsPlugin
import org.codehaus.groovy.grails.web.util.TypeConvertingMap
import org.grails.plugin.vaadin.ui.GspLayout;
import org.grails.plugin.vaadin.utils.Stopwatch;
import org.grails.plugin.vaadin.utils.Utils;

import com.vaadin.Application;
import com.vaadin.ui.UriFragmentUtility
import com.vaadin.ui.UriFragmentUtility.FragmentChangedEvent;
import com.vaadin.ui.UriFragmentUtility.FragmentChangedListener;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import org.apache.commons.logging.LogFactory

/**
 * Wires VaadinControllers to GSP views using the concept of a VaadinRequest.
 * <p>
 * Note that a {@link org.grails.plugin.vaadin.VaadinRequest} is not a
 * JavaEE ServletRequest. It is simply a construct for capturing a (user's)
 * request to display a given Vaadin 'page'. See the {@link org.grails.plugin.vaadin.VaadinRequest}
 * javadocs for more details.
 * <p>
 * This class also automatically listens to changes to browser URI fragments.
 * The listening is started either by explicitly calling
 * {@link #startFragmentListener} or else it is started automatically
 * after the first 'request' is dispatched.
 * 
 * @author Francis McKenzie
 * @see VaadinRequest
 */
class VaadinDispatcher {
    def log = LogFactory.getLog(this.class)

    // Set by api - provides access to application
    def vaadinApplicationHolder
    final getApplication() { vaadinApplicationHolder.application }
    // Set by api - provides hibernate transaction around controllers
    def vaadinTransactionManager
        
    protected controllers = [:]
    
    protected VaadinRequest activeRequest = new VaadinRequest()
    
    // Delay creation of fragment utility until first called.
    // Allows the user to create the mainWindow first.
    private UriFragmentUtility fragmentUtility
    protected UriFragmentUtility getOrCreateFragmentUtility() {
        if (! this.fragmentUtility ) {
            if (application.mainWindow) {
                this.fragmentUtility = new UriFragmentUtility()
                application.mainWindow.addComponent(this.fragmentUtility)
            } else {
                throw new Exception("Application must have main window!")
            }
        }
        return this.fragmentUtility
    }
    
    // FragmentListener
    protected boolean fragmentListenerStarted = false
    
    /**
     * Registers the controllers to use for dispatching. Note that unlike
     * Grails, a single instance of a controller and view is used - the instance
     * is created in this method. Grails, by contrast, creates a new controller instance
     * for every request.
     * <p>
     * If there is more than one class with the same logical name, then whichever
     * class is in the main project, rather than a plugin, takes precedence.
     * 
     * @param controllerClasses The list of controller classes in the app
     */
    def init(List<VaadinClass> controllerClasses) {
        controllers = [:]
        controllerClasses.each {
            // Don't replace an existing class with a class from a plugin
            if (! (controllers.containsKey(it.logicalPropertyName) &&
                it.clazz.isAnnotationPresent(GrailsPlugin.class)) ) {
                controllers[it.logicalPropertyName] = it.clazz.newInstance()
            }
        }
    }
    
    /**
     * Listens for changes to browser fragment, and dispatches requests to
     * vaadin controllers.
     * <p>
     * If no request has been dispatched yet, this dispatches a request
     * to the home page.
     */
    def startFragmentListener() {
        if (! this.fragmentListenerStarted) {
            if (log.isDebugEnabled()) {
                log.debug("STARTING: Fragment Listener")
            }
            final VaadinDispatcher parent = this
            getOrCreateFragmentUtility().addListener(new FragmentChangedListener() {
                public void fragmentChanged(FragmentChangedEvent source) {
                    String fragment = source?.uriFragmentUtility?.fragment
                    parent.dispatchWithFragment(fragment)
                }
            });
            this.fragmentListenerStarted = true
        }
        if (! this.activeRequest.dispatched) {
            if (log.isDebugEnabled()) {
                log.debug("DEFAULT: Home controller")
            }
            this.dispatch([:]) // Goes home
        }
    }

    /**
     * Dispatches a 'request' to show a particular Vaadin 'page' using the
     * specified URI fragment, e.g. '#book/show/15'
     * 
     * @param fragment The URI fragment
     */
    def dispatchWithFragment(String fragment) {
        def controller, action, id, params = [:]
        if (fragment) {
            def m = fragment =~ $/#?/?(\w+)(?:/(\w+)(?:/(\d+))?)?(?:\?(.*))?/$
            if (m) {
                controller = m[0][1]
                action = m[0][2]
                id = m[0][3]
                m[0][4]?.split(/&/)?.each {
                    def kv = it.split(/=/)
                    params[kv[0]] = kv.length > 1 ? kv[1] : null
                } 
            }
        }
        dispatch([controller:controller, action:action, id:id, params:params])
    }
    
    /**
     * Dispatches the active request again, or goes to home page if no active request
     */
    def refresh() {
        if (activeRequest) {
            dispatch(activeRequest)
        } else {
            dispatch([:])
        }
    }
    
    /**
     * Dispatches a 'request' to show a particular Vaadin 'page' using the
     * specified args, containing e.g. 'controller', 'action', 'id' etc.
     * 
     * @param args The args containing the details of the request
     */
    def dispatch(Map args) {
        // Prepare request
        def request = new VaadinRequest()
        request.newRequest(activeRequest, args, [controller:"home", action:"index"])
        
        // Timing logging
        def stopwatch = Stopwatch.enabled ? new Stopwatch("#${request.fragment}", this.class) : null

        // Dispatch
        dispatch(request)
        
        // Timing logging
        stopwatch?.stop()
    }
    
    /**
     * Dispatches the specified {@org.grails.plugin.vaadin.VaadinRequest}
     * 
     * @param request The 'request' containing e.g. the 'controller' etc.
     */
    protected dispatch(VaadinRequest request) {
        // Prepare
        request.startRequest() // Clears model
        
        // Log
        if (log.isDebugEnabled()) {
            log.debug "DISPATCH: ${request}"
        }
        
        // Get controller object
        def controllerInstance = controllers[request.controller]
        if (! controllerInstance) {
            throw new IllegalArgumentException("Controller not found '${request.controller}'")
        }

        // Get action method
        def method = controllerInstance.metaClass.getMetaMethod(request.action)
        if (!method) {
            throw new IllegalArgumentException("Action '${request.action}' not found for controller '${controllerInstance.class}'")
        }
        
        // Run method
        activeRequest.request = request
        def result = vaadinTransactionManager.withTransaction {method.invoke(controllerInstance)}
        
        // Log result
        if (log.isDebugEnabled()) {
            log.debug "RETURNED: ${activeRequest} ${Utils.toString(result)}"
        }
        
        // Check not redirected
        if (activeRequest.controller == request.controller && activeRequest.action == request.action) {
            // Save model
            activeRequest.finishRequest(result)

            // Update URL
            getOrCreateFragmentUtility().setFragment(request.fragment, false)
    
            // Show view
            def oldView = application.mainWindow.componentIterator.find { ! (it instanceof UriFragmentUtility) }
            def newView = new GspLayout(activeRequest.viewFullName, activeRequest.params, activeRequest.model, activeRequest.flash, activeRequest.controller)
            if (oldView) {
                application.mainWindow.replaceComponent(oldView, newView)
            } else {
                application.mainWindow.addComponent(newView)
            }
            
        // Redirected
        } else {
            dispatch(activeRequest)
        }
    }
}
