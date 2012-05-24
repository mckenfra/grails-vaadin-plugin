package org.grails.plugin.vaadin

import org.codehaus.groovy.grails.commons.GrailsApplication;
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
class VaadinDispatcher implements Serializable {
    def log = LogFactory.getLog(this.class)
    
    /**
     * Vaadin Application for this dispatcher
     */
    Application vaadinApplication
    /**
     * The default page to use if no controller and action specified
     */
    static String defaultPage = "home"
    /**
     * The default controller to use if no controller specified
     */
    static String defaultController = "home"
    /**
     * Most recent browser page
     */
    protected String currentPage
    /**
     * Controller of most recent browser page
     */
    protected String currentController
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
            if (vaadinApplication.mainWindow) {
                this.fragmentUtility = new UriFragmentUtility()
                vaadinApplication.mainWindow.addComponent(this.fragmentUtility)
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
     * Create a dispatcher for the specified VaadinApplication.
     * 
     * @param application The Vaadin Application for this dispatcher
     */
    public VaadinDispatcher(Application application) {
        this.vaadinApplication = application
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
                    if (!parent.stopped) {
                        String fragment = source?.uriFragmentUtility?.fragment
                        parent.vaadinApplication.dispatcher.dispatchWithFragment(fragment)
                    }
                }
            });
            this.fragmentListenerStarted = true
        }
        if (!currentPage && !stopped) {
            if (log.isDebugEnabled()) {
                log.debug("DEFAULT: ${defaultPage}")
            }
            this.dispatchWithFragment(defaultPage)
        }
    }

    /**
     * Dispatches a 'request' to show a particular Vaadin 'page' using the
     * specified URI fragment, e.g. '#book/show/15'
     * 
     * @param fragment The URI fragment
     */
    def dispatchWithFragment(String fragment) {
        dispatch(new VaadinRequest(vaadinApplication, fragment, VaadinRequest.Type.PAGE))
    }
    
    /**
     * Dispatches the active page request again, or goes to home page if no active page
     */
    def refresh() {
        dispatchWithFragment(currentPage ?: defaultPage)
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
        dispatch(new VaadinRequest(vaadinApplication, requestArgs, VaadinRequest.Type.PAGE))
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
        Component newView = this.request(request, true)
        
        // Update page request
        this.currentPage = request.fragment
        this.currentController = request.controller
        
        // Update browser
        getOrCreateFragmentUtility().setFragment(request.fragment, false)
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
     * @param attach If true, the component will be attached to the application's mainWindow before being returned
     */
    protected Component request(VaadinRequest request, boolean attach = false) {
        Component result
        
        // Get the transaction manager for wrapping the request in a transaction
        def vaadinTransactionManager = vaadinApplication.getBean("vaadinTransactionManager")
        if (! vaadinTransactionManager) {
            throw new NullPointerException("Unable to retrieve spring bean 'vaadinTransactionManager'")
        }
        // Get grails application that holds the controllers
        def grailsApplication = vaadinApplication.getBean("grailsApplication")
        if (! grailsApplication) {
            throw new NullPointerException("Unable to retrieve spring bean 'grailsApplication'")
        }
        
        int redirects = 0
        while (true) {
            // Check we're not in an endless redirect loop
            if (redirects > 20) {
                throw new Exception("Too many redirects!")
            }
            
            // Execute the controller and view in a transaction
            result = vaadinTransactionManager.wrapInTransaction({executeRequest(request, grailsApplication, attach)})
            
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
     * @param grailsApplication The grailsApplication that holds the controllers
     * @param attach If true, the component will be attached to the application's mainWindow before being returned
     */
    protected Component executeRequest(VaadinRequest request, GrailsApplication grailsApplication, boolean attach = false) {
        // Returns generated view, or null if redirected
        Component view
        
        // Initialise request based on active request, clear model
        request.startRequest(VaadinRequestContextHolder.requestAttributes, currentController ?: defaultController, defaultPage)

        // Log
        if (log.isDebugEnabled()) {
            log.debug "DISPATCH: ${request}"
        }
        
        // Timing logging
        def stopwatch = Stopwatch.enabled ? new Stopwatch("#${request.fragment}", this.class) : null
        
        // Get controller class
        def controllerClass = grailsApplication.getArtefactByLogicalPropertyName("Controller", "${request.controller}Vaadin")
        if (! controllerClass) {
            throw new IllegalArgumentException("Controller not found '${request.controller}'")
        }

        // Get action method
        def method = controllerClass.metaClass.getMetaMethod(request.action)
        if (!method) {
            throw new IllegalArgumentException("Action '${request.action}' not found for controller '${controllerClass}'")
        }
        
        // Instantiate controller
        def controllerInstance = controllerClass.newInstance()
        
        // Set request as thread's active request - this exposes the request to the controller,
        // which can update it through a call to render() or redirect()
        def previousRequest = VaadinRequestContextHolder.requestAttributes
        VaadinRequestContextHolder.requestAttributes = request

        try {
            // Execute controller action
            def result = method.invoke(controllerInstance)
            
            // Log result
            if (log.isDebugEnabled()) {
                log.debug "RETURNED: ${request} ${Utils.toString(result)}"
            }
            
            // Only execute view if not redirected
            if (! request.redirected) {
                // Save model
                request.finishRequest(result)
    
                // Execute view
                if (request.viewIsName) {
                    view = new GspLayout(vaadinApplication, request.viewFullName, request.params, request.model, request.flash, request.controller, request.type == VaadinRequest.Type.PAGE)
                } else if (request.view instanceof Component) {
                    view = request.view
                } else {
                    view = new GspLayout(vaadinApplication, {"${request.view}"}, request.type == VaadinRequest.Type.PAGE)
                }
                
                // Attach if necessary
                if (attach) {
                    def oldView = vaadinApplication.mainWindow.componentIterator.find { ! (it instanceof UriFragmentUtility) }
                    if (oldView) {
                        vaadinApplication.mainWindow.replaceComponent(oldView, view)
                    } else {
                        vaadinApplication.mainWindow.addComponent(view)
                    }
                }
            }
        } finally {
            // Remove active request
            VaadinRequestContextHolder.requestAttributes = previousRequest
        
            // Timing logging
            stopwatch?.stop()
        }
        
        return view
    }    
}
