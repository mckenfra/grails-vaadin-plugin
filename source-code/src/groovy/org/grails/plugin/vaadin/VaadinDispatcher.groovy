package org.grails.plugin.vaadin

import javax.servlet.ServletException;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.metadata.GrailsPlugin
import org.codehaus.groovy.grails.web.errors.GrailsExceptionResolver;
import org.codehaus.groovy.grails.web.util.TypeConvertingMap
import org.grails.plugin.vaadin.ui.GspLayout;
import org.grails.plugin.vaadin.ui.StartupUriFragmentUtility;
import org.grails.plugin.vaadin.utils.Stopwatch;
import org.grails.plugin.vaadin.utils.Utils;
import org.springframework.util.StringUtils

import com.vaadin.Application;
import com.vaadin.terminal.Terminal;
import com.vaadin.terminal.Terminal.ErrorEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
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
			synchronized(this) {
				if (! this.fragmentUtility ) {
		            if (!vaadinApplication.mainWindow) {
						throw new Exception("Application must have main window!")
		            }
		            this.fragmentUtility = new StartupUriFragmentUtility()
		            vaadinApplication.mainWindow.addComponent(this.fragmentUtility)
				}
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
                        parent.vaadinApplication.dispatcher.dispatch(fragment)
                    }
                }
            })
            this.fragmentListenerStarted = true
        }
    }

    /**
     * Dispatches the active page request again, or goes to home page if no active page
     */
    def refresh() {
		def fragmentUtil = getOrCreateFragmentUtility()
		if (fragmentUtil instanceof StartupUriFragmentUtility) {
			fragmentUtil.restart()
		} else {
			dispatch(currentPage ?: defaultPage)
		}
    }
    
    /**
     * Dispatches a 'request' to show a particular Vaadin 'page' using the
     * specified URI fragment, for example '#book/show/15'
     *
     * @param fragment The URI fragment
     */
    def dispatch(String fragment) {
        dispatch(new VaadinRequest(vaadinApplication, fragment, VaadinRequest.Type.PAGE))
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
        try {
        // Execute the request to build the view component
            this.request(request, true)
        } finally {
            // Update page request
            this.currentPage = request.fragment
            this.currentController = request.controller
            
            // Update browser
            getOrCreateFragmentUtility().setFragment(request.fragment, false)
        }
    }

    /**
     * Executes a new request using the specified using the
     * specified URI fragment, and returns the generated
     * view Vaadin Component.
     *
     * @param fragment The URI fragment
     */
    public Component request(String fragment) {
        return request(new VaadinRequest(vaadinApplication, fragment, VaadinRequest.Type.INCLUDE))
    }

    /**
     * Executes a new request using the specified args, and returns the generated
     * view Vaadin Component.
     *
     * @param requestArgs The args containing the details of the request
     */
    public Component request(Map requestArgs) {
        return request(new VaadinRequest(vaadinApplication, requestArgs, VaadinRequest.Type.INCLUDE))
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
        
        try {
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
        } catch(Throwable t) {
            if (request?.type == VaadinRequest.Type.ERROR) {
                throw t
            } else {
                error (t, request?.fragment)
            }
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
                    view = new GspLayout([uri:request.viewFullName, params:request.params, model:request.model, flash:request.flash, controllerName:request.controller, attributes:request.attributes])
                    view.root = request.type != VaadinRequest.Type.INCLUDE
                } else if (request.view instanceof Component) {
                    view = request.view
                } else {
                    view = new GspLayout({"${request.view}"})
                    view.root = request.type != VaadinRequest.Type.INCLUDE
                }
                
                // Attach if necessary
                if (attach) {
                    attachPage(view)
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
    
    /**
     * Adds the specified component as the main window's top-level page component. 
     */
    protected void attachPage(Component page) {
        def oldPage = vaadinApplication.mainWindow.componentIterator.find { ! (it instanceof UriFragmentUtility) }
        if (oldPage) {
            vaadinApplication.mainWindow.replaceComponent(oldPage, page)
        } else {
            vaadinApplication.mainWindow.addComponent(page)
        }
    }

    /**
     * Dispatches to the error page for the specified error
     * 
     * @param t The error
     */
    public void error(Throwable t, String fragment = null) {
        fragment = "${fragment ?: currentPage}"
        try {
            def errorRequest = [
                controller:"error",
                params:[exception:t],
                attributes:[
                    'javax.servlet.error.status_code':500,
                    'javax.servlet.error.request_uri':fragment
                ]
            ]
            dispatch(new VaadinRequest(vaadinApplication, errorRequest, VaadinRequest.Type.ERROR))
        } catch(err) {
            String defaultHTML = """\
<div class='body'>
  <h1>Original Error</h1>
  <div>${renderDefaultErrorHTML(t)}</div>
  <h1>Error in Error Controller</h1>
  <div>${renderDefaultErrorHTML(err)}</div>
</div>
"""
            attachPage(new Label(defaultHTML, Label.CONTENT_XHTML))
        }
        
        // Now throw the error
        throw t
    }
	
	protected String renderDefaultErrorHTML(Throwable exception) {
		String result
		
		try {
			StringWriter currentOut = new StringWriter()
			
			// Try to get root cause
			currentOut << '<dl class="error-details">'
			def root = GrailsExceptionResolver.getRootCause(exception)
			currentOut << "<dt>Class</dt><dd>${root?.getClass()?.name ?: exception.getClass().name}</dd>"
			currentOut << "<dt>Message</dt><dd>${exception.message?.encodeAsHTML()}</dd>"
			if (root != null && root != exception && root.message != exception.message) {
				currentOut << "<dt>Caused by</dt><dd>${root.message?.encodeAsHTML()}</dd>"
			}
			currentOut << "</dl>"
	
			// Print stack trace
			def errorsViewStackTracePrinter = vaadinApplication?.getBean("errorsViewStackTracePrinter")
			if (errorsViewStackTracePrinter) {
				currentOut << errorsViewStackTracePrinter.prettyPrintCodeSnippet(exception)
				def trace = errorsViewStackTracePrinter.prettyPrint(exception.cause ?: exception)
				if (StringUtils.hasText(trace.trim())) {
					currentOut << "<h2>Trace</h2>"
					currentOut << '<pre class="stack">'
					currentOut << trace.encodeAsHTML()
					currentOut << '</pre>'
				}
				result = currentOut.toString()
				
			// Default to plain HTML
			} else {
				result = '<pre class="stack">${t?.stackTrace?.toString()?.replaceAll("\n", "<br></br>")}</code></p>'
			}
		// Default to plain HTML
		} catch(err) {
			result = '<pre class="stack">${t?.stackTrace?.toString()?.replaceAll("\n", "<br></br>")}</code></p>'
		}
 
		return result
	}
}
