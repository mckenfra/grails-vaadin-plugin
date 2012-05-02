package org.grails.plugin.vaadin

import org.codehaus.groovy.grails.plugins.metadata.GrailsPlugin
import org.codehaus.groovy.grails.web.util.TypeConvertingMap
import org.grails.plugin.vaadin.ui.GspLayout;
import org.grails.plugin.vaadin.utils.Stopwatch;
import org.grails.plugin.vaadin.utils.Utils;

import com.vaadin.Application;
import com.vaadin.ui.Component;
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
    
    /**
     * The default request to use if no controller and action specified
     */
    static Map defaultRequest = [controller:"home", action:"index"]
    /**
     * For debugging, a unique id for this instance
     */
    protected cid
    /**
     * Set by api - provides access to current application
     */
    def vaadinApplicationHolder
    /**
     * Current application
     */
    final getApplication() { vaadinApplicationHolder.application }
    /**
     * Set by api - provides hibernate transaction around controllers
     */
    def vaadinTransactionManager
    /**
     * Controller names mapped to controller instances
     */
    protected Map controllers = [:]
    /**
     * Most recent browser page request
     */
    protected VaadinRequest pageRequest = new VaadinRequest()
    /**
     * If true, the dispatcher is stopped and will not send requests to controllers
     */
    protected boolean stopped
    /**
     * If true, the dispatcher is stopped and will not send requests to controllers
     */
    public boolean getStopped() { this.stopped }
    /**
     * Stops the dispatcher
     */
    public void stop() { this.stopped = true }
    /**
     * Starts the dispatcher
     */
    public void start() { this.stopped = false }
    /**
     * Fragment utility for responding to browser fragment changes
     */
    protected UriFragmentUtility fragmentUtility
    /**
     * Delay creation of fragment utility until first called.
     * <p>
     * Allows the user to create the mainWindow first.
     */
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
    /**
     * Ensures we only start fragment utility once
     */
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
        def timestamp = new Date().time
        cid = { timestamp }
        if (log.isDebugEnabled()) {
            log.debug "INIT: ${cid()}"
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
                log.debug("STARTING-${cid()}: Fragment Listener")
            }
            final VaadinDispatcher parent = this
            getOrCreateFragmentUtility().addListener(new FragmentChangedListener() {
                public void fragmentChanged(FragmentChangedEvent source) {
                    if (!parent.stopped) {
                        String fragment = source?.uriFragmentUtility?.fragment
                        parent.application.dispatcher.dispatchWithFragment(fragment)
                    }
                }
            });
            this.fragmentListenerStarted = true
        }
        if (! this.pageRequest.dispatched && !stopped) {
            if (log.isDebugEnabled()) {
                log.debug("DEFAULT-${cid()}: Home controller")
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
     * Dispatches the active page request again, or goes to home page if no active page
     */
    def refresh() {
        if (pageRequest) {
            dispatch(pageRequest)
        } else {
            dispatch([:])
        }
    }
    
    /**
     * Dispatches a 'request' to show a particular Vaadin 'page' using the
     * specified args, containing 'controller', 'action', 'id' etc.
     * <p>
     * Attaches the result to the application's main window, and updates the browser fragment.
     * <p>
     * If a valid request, it is set as the active page request, so that all future requests will
     * be executed relative to this request.
     * 
     * @param requestArgs The args containing the details of the request
     */
    def dispatch(Map requestArgs) {
        dispatch(new VaadinRequest(requestArgs, VaadinRequest.Type.PAGE))
    }
    
    /**
     * Dispatches a 'request' to show a particular Vaadin 'page' using the specified
     * {@org.grails.plugin.vaadin.VaadinRequest}.
     * <p>
     * Attaches the result to the application's main window, and updates the browser fragment.
     * <p>
     * If a valid request, it is set as the active request, so that all future requests will
     * be executed relative to this request.
     * 
     * @param request The 'request' containing e.g. the 'controller' etc.
     */
    protected dispatch(VaadinRequest request) {
        // Execute the request to build the view component
        Component newView = this.request(request)
        
        // Update page request
        this.pageRequest = request
        
        // Update browser
        getOrCreateFragmentUtility().setFragment(request.fragment, false)
        
        // Attach Gsp to Window
        def oldView = application.mainWindow.componentIterator.find { ! (it instanceof UriFragmentUtility) }
        if (oldView) {
            application.mainWindow.replaceComponent(oldView, newView)
        } else {
            application.mainWindow.addComponent(newView)
        }
    }

    /**
     * Executes a new request using the specified args, and returns the generated
     * view Vaadin Component.
     *
     * @param requestArgs The args containing the details of the request
     */
    public Component request(Map requestArgs) {
        return request(new VaadinRequest(requestArgs, VaadinRequest.Type.INCLUDE))
    }
    
    /**
     * Executes the specified {@org.grails.plugin.vaadin.VaadinRequest}, and returns the generated
     * view Vaadin Component.
     * 
     * @param request The 'request' containing e.g. the 'controller' etc.
     */
    protected Component request(VaadinRequest request) {
        Component result
        
        int redirects = 0
        while (true) {
            // Check we're not in an endless redirect loop
            if (redirects > 20) {
                throw new Exception("Too many redirects!")
            }
            
            // Execute the controller and view in a transaction
            result = vaadinTransactionManager.wrapInTransaction({executeRequest(request)})
            
            // Quit if we're not redirecting
            if(!request.redirected) break
            
            // Increment our redirect counter
            redirects++
        }

        // Return view component
        return result
    }
    
    /**
     * Executes the controller and view (if not redirected) for specified request.
     * 
     * @param request The request to execute
     */
    protected Component executeRequest(VaadinRequest request) {
        // Returns generated view, or null if redirected
        Component view
        
        // Initialise request based on active request, clear model
        request.startRequest(activeRequest ?: pageRequest, defaultRequest)

        // Log
        if (log.isDebugEnabled()) {
            log.debug "DISPATCH-${cid()}: ${request}"
        }
        
        // Timing logging
        def stopwatch = Stopwatch.enabled ? new Stopwatch("#${request.fragment}", this.class) : null
        
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
        
        // Set request as thread's active request - this exposes the request to the controller,
        // which can update it through a call to render() or redirect()
        def previousRequest = this.activeRequest
        this.activeRequest = request

        try {
            // Execute controller action
            def result = method.invoke(controllerInstance)
            
            // Log result
            if (log.isDebugEnabled()) {
                log.debug "RETURNED-${cid()}: ${request} ${Utils.toString(result)}"
            }
            
            // Only execute view if not redirected
            if (! request.redirected) {
                // Save model
                request.finishRequest(result)
    
                // Execute view
                if (request.viewIsName) {
                    view = new GspLayout(request.viewFullName, request.params, request.model, request.flash, request.controller, request.type == VaadinRequest.Type.PAGE)
                } else if (request.view instanceof Component) {
                    view = request.view
                } else {
                    view = new GspLayout({"${request.view}"}, request.type == VaadinRequest.Type.PAGE)
                }
            }
        } finally {
            // Remove active request
            this.activeRequest = previousRequest
        
            // Timing logging
            stopwatch?.stop()
        }
        
        return view
    }
    
    /**
     * Holds the active request in the current thread
     */
    protected static ThreadLocal activeRequestHolder = new ThreadLocal<VaadinRequest>()
    /**
     * Gets the active request
     */
    public VaadinRequest getActiveRequest() {
        return activeRequestHolder.get()
    }
    /**
     * Sets the active request
     */
    public void setActiveRequest(VaadinRequest request) {
        if (!request) activeRequestHolder.remove()
        else activeRequestHolder.set(request)
    }  
}
