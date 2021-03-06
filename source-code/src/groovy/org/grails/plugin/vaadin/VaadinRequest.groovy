package org.grails.plugin.vaadin

import grails.util.GrailsNameUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.web.util.TypeConvertingMap;
import org.grails.plugin.vaadin.utils.Utils;

import com.vaadin.Application;
import com.vaadin.ui.Component;

/**
 * An internal construct for mimicking a user's request to display
 * a particular Vaadin 'page'. However note that this is in no way
 * connected with the JavaEE ServletRequest.
 * <p>
 * The main benefit is that it brings to Vaadin familiar Grails
 * concepts, i.e.: controllers, actions, views, params, models, flash
 * <p>
 * The intention is that anyone familiar with Grails should have no problem
 * understanding how to put together a Vaadin project, since all the concepts
 * are familiar. In fact, the standard scaffolded Vaadin Controllers are almost
 * identical to the Grails controllers - they do all of the same things.
 * The Vaadin Views actually create the Vaadin layouts and components.
 * <p>
 * Moreover, by using the controller/action paradigm, we can easily add
 * standard browser-history behaviour, by automatically adding fragments such as
 * '#book/show/15' to the browser URI whenever we call a Vaadin 'request'.
 * <p>
 * One might think: Vaadin is not a normal web Http Request/Response-type framework,
 * why is it necessary to structure a Vaadin app in terms of 'requests'? The answer is that
 * generally you will still want to identify a particular set of layouts and
 * components as belonging to a particular 'page' or 'screen' of your user interface.
 * E.g. a user's 'profile' screen, or the 'home' screen etc.
 * <p>
 * With the Vaadin 'request' and 'dispatcher' constructs, you can wire
 * together such 'pages' (consisting of contollers and views) as easily
 * as is currently possible with normal Grails applications.
 * <p>
 * See <a href="http://grails.org/doc/latest/ref/Tags/link.html">Grails g:link</a>
 * for the set of parameters available for a request. Note that this class
 * also supports an 'instance' as a parameter, which is the actual
 * domain instance (as opposed to an id or a set or params). This
 * is one benefit of Vaadin over Grails Requests!
 * 
 * @author Francis McKenzie
 */
class VaadinRequest {
    def log = LogFactory.getLog(this.class)
    
    /**
     * The request can be a top-level browser page or error page, or an include
     * within the page.
     * <p>
     * A page-type will change the browser fragment. An include-type will not. 
     */
    public static enum Type { PAGE, INCLUDE, ERROR }
    
    /**
     * For debugging, a unique id for this instance
     */
    protected uid
    /**
     * Vaadin Application for this request
     */
    Application vaadinApplication
    /**
     * This request's fragment equivalent
     */
    String fragment

    protected String controllerName // May be set as a controller class or instance
    protected String action
    protected Object view
    protected boolean viewIsName = true
    protected Map params
    protected Map attributes
    protected Map model
    protected Map flash
    protected String url // For redirecting to external URLs
    protected Type type = Type.PAGE
    
    /**
     * Indicates if this request has been dispatched 
     */
    protected boolean dispatched
    /**
     * Indicates if this request has been dispatched 
     */
    public boolean isDispatched() { dispatched }
    /**
     * Indicates if this request has been redirected 
     */
    protected boolean redirected
    /**
     * Indicates if this request has been redirected to external 
     */
    protected boolean external
    /**
     * Indicates if this request has been redirected 
     */
    public boolean isRedirected() { redirected }
    /**
     * Indicates if this request has been redirected to an external Url
     */
    public boolean isExternal() { external }
    /**
     * The requested controller - for example 'book'
     */
    public String getController() { this.controllerName }
    /**
     * The requested controller - for example 'book'
     */
    public void setController(Object controller) {
        if (controller == null) {
            this.controllerName = null
        } else if (controller instanceof GrailsClass) {
            this.controllerName = controller.logicalPropertyName - "Vaadin"
        } else {
            Class clazz = controller?.class == Class ? controller : controller?.class
            
            // Get the logical property name of the controller class or instance
            if (clazz?.name?.endsWith("VaadinController")) {
                this.controllerName = GrailsNameUtils.getLogicalName(clazz, "VaadinController")
                
            // Not a VaadinController - just convert to a string
            } else {
                this.controllerName = controller?.toString()
            }
        }
    }
    /**
     * The requested action - for example 'show'
     */
    public String getAction() { this.action }
    /**
     * The requested view - could be Gsp name, Gsp text or any Vaadin Component.
     */
    public Object getView() { this.view }
    /**
     * If true, view is a Gsp name. Otherwise, could be Gsp text or any Vaadin Component.
     */
    public boolean isViewIsName() { this.viewIsName }
    /**
     * If true, this is a browser page request. Otherwise, is a request to render a
     * frame within a page.
     */
    public Type getType() { this.type }
    /**
     * The request params - for example [id:15]
     */
    public Map getParams() { this.params }
    /**
     * The request attributes - for example [foo:'bar']
     */
    public Map getAttributes() { this.attributes }
    /**
     * The request model - for example [bookInstance:bookInstance]
     */
    public Map getModel() { this.model }
    /**
     * The flash message and error - for example [message:"Book 15 created!"]
     */
    public Map getFlash() { this.flash }
    /**
     * The external Url - only applies if this request has been redirected to external.
     */
    public String getUrl() { this.url }
    /**
     * The id of the domain instance - for example '15'
     */
    public String getId() { this.params?.id }
    /**
     * The domain instance itself - for example Book(15)
     */
    public Object getInstance() { this.params?.instance }
    /**
     * Set the id this request - for example '15'
     */
    protected void setId(String id) { if (id) { this.params.id = id } }
    /**
     * Set the domain instance for this request - for example Book(15)
     */
    protected void setInstance(Object instance) { if (instance) { this.params.instance = instance } }
    /**
     * Set the params for this request - for example [id:15]
     */
    protected void setParams(Map params) { this.params = (params ? new TypeConvertingMap(params) : new TypeConvertingMap()) }

    /**
     * Create an empty request
     */
    public VaadinRequest(Application application) { this(application, [:]) }
    /**
     * Create a request with specified fragment
     */
    public VaadinRequest(Application application, String fragment, Type type = Type.PAGE) {
        this(application, fromFragment(fragment), type)
    }
    /**
     * Create a request with the specified properties
     */
    public VaadinRequest(Application application, Map props, Type type = Type.PAGE) {
        this.vaadinApplication = application
        this.properties = props
        this.type = type
        
        // For debugging
        uid = new Date().time
        if (log.isDebugEnabled()) {
            log.debug "INIT: ${uid}"
        }
    }

    /**
     * Used by VaadinControllers to redirect the current 'request'
     * to another controller or action. Mimics the method of the same
     * name provided to standard Grails controllers.
     * <p>
     * See <a href="http://grails.org/doc/latest/ref/Controllers/redirect.html">Grails redirect</a>
     * for controllers
     * 
     * @param args E.g. 'controller' or 'action' to redirect to
     */
    public void redirect(Map args) {
        if (!args || this.controller == args.controller && this.action == args.action) {
            throw new IllegalArgumentException("Invalid redirect args ${args}")
        }
        if (log.isDebugEnabled()) {
            log.debug "REDIRECT-${uid}: ${args} >> ${this.properties}"
        }
        this.controller = args.controller ?: this.controller
        this.action = args.action ?: this.action
        this.view = this.action
        this.params = args.params ?: [:]
        this.attributes = args.attributes ?: this.attributes
        this.model = [:]
        this.id = args.id
        this.instance = args.instance
        this.redirected = true
        this.url = args.url
        this.external = this.url
    }
    
    /**
     * Used by VaadinControllers to render a particular view. Mimics the method of the same
     * name provided to standard Grails controllers.
     * <p>
     * See <a href="http://grails.org/doc/latest/ref/Controllers/render.html">Grails render</a>
     * for controllers
     *
     * @param view Either an args map containing view name as 'view', or Gsp content text, or a Vaadin Component.
     */
    public void render(Object view) {
        if (view instanceof Map && view) render((Map) view)
        else if (view instanceof Component) render((Component) view)
        else render("${view}")
    }
    
    /**
     * Used by VaadinControllers to render a particular view. Mimics the method of the same
     * name provided to standard Grails controllers.
     * <p>
     * See <a href="http://grails.org/doc/latest/ref/Controllers/render.html">Grails render</a>
     * for controllers
     * 
     * @param view The view content text to render
     */
    public void render(String view) {
        this.view = view
        this.viewIsName = false
    }

    /**
     * Used by VaadinControllers to render a particular view. Mimics the method of the same
     * name provided to standard Grails controllers.
     * <p>
     * See <a href="http://grails.org/doc/latest/ref/Controllers/render.html">Grails render</a>
     * for controllers
     *
     * @param view The Vaadin Component to render
     */
    public void render(Component view) {
        this.view = view
        this.viewIsName = false
    }

    /**
     * Used by VaadinControllers to render a particular view. Mimics the method of the same
     * name provided to standard Grails controllers.
     * <p>
     * See <a href="http://grails.org/doc/latest/ref/Controllers/render.html">Grails render</a>
     * for controllers
     * 
     * @param args E.g. 'view' to render to, 'model' to use, etc.
     */
    public void render(Map args) {
        if (args) {
            this.view = args.view ?: this.action
            this.viewIsName = true
            this.params = args.params ?: this.params
            this.model = args.model ?: this.model
            this.id = args.id
            this.instance = args.instance
        }
    }
    
    /**
     * Converts this 'request' into a URI fragment - for example '#book/show/15'
     * 
     * @params props The request properties
     * @return The URI fragment corresponding to this 'request'
     */
    static String toFragment(Map props) {
        String result = ""
        if (props) {
            def id = props.params?.id
            def useInlineId = (id || id == "0") && props.action != "index"
            def paramsWithoutId = props.params?.findAll { k,v-> ! (useInlineId && k == "id") && k != "instance" }
            def hasParams = paramsWithoutId?.size()
            result = "${props.controller}" +
                (props.action == "index" ? "" : "/${props.action}") +
                (useInlineId ? "/${id}" : "") +
                (!hasParams ? "" : "?" + paramsWithoutId.entrySet().collect { "${it.key}=${it.value}" }.join("&"))
        }
        return result
    }
    
    /**
     * Converts the specified fragment into request props - for example '#book/show/15'
     * 
     * @param fragment The URI fragment to convert into request props
     */
    static Map fromFragment(String fragment) {
        def result = [params:[:]]
        if (fragment) {
            def m = fragment =~ $/#?/?(\w+)(?:/(\w+)(?:/(\d+))?)?(?:\?(.*))?/$
            if (m) {
                result.controller = m[0][1]
                result.action = m[0][2]
                result.id = m[0][3]
                m[0][4]?.split(/&/)?.each {
                    def kv = it.split(/=/)
                    result.params[kv[0]] = kv.length > 1 ? kv[1] : null
                }
            }
        }
        return result
    }
    
    /**
     * Gets view uri of this request (minus params), for example book/show.
     * <p>
     * Returns null if the view is a Gsp content string, or a Vaadin Component.
     * <p>
     * Note that if the view name starts with "/" then it is resolved
     * using the root views directory. Otherwise "/vaadin/{controllerName}/"
     * is prepended to the name.
     * 
     * @return Uri of view, or null if the view is a Gsp content string, or a Vaadin Component.
     */
    public String getViewFullName() {
        if (this.viewIsName) {
            return this.view?.startsWith('/') ? this.view : "/vaadin/${controller}/${view}"
        } else {
            return null
        }
    }
    
    /**
     * Initialises this 'request' based on the 'request' that is currently active, and using
     * the specified set of defaults (in case neither this request or the active
     * request have a particular property).
     * <p>
     * This is called by the {@link VaadinDispatcher} at the beginning of the dispatch cycle.
     * E.g. one of the things it does is to set the 'controller' for the new request to be
     * the same as the 'controller' of the current active request, if it is not
     * explicitly overridden in the request parameters.
     * 
     * @param activeRequest The current active request (if any)
     * @param currentController The default controller to use if neither this nor the active request have one
     * @param defaultPage The default page to use if no controller is found in this request or the active request
     */
    protected void startRequest(VaadinRequest activeRequest, String currentController, String defaultPage) {
        // Ensure we have a controller
        if (! this.controller) { this.controller = activeRequest?.controller ?: currentController  }
        if (! this.controller) {
            this.properties = fromFragment(defaultPage)
        }
        
        // Set all other props
        this.action = this.action ?: "index"
        this.view = this.action
        this.viewIsName = true
        this.params = this.params ?: [:]
        this.attributes = this.attributes ?: [:] // Maintain attributes between redirects
        this.model = [:] // Reset model between redirects
        this.flash = this.flash ?: [:] // Maintain flash between redirects
        this.dispatched = false
        this.redirected = false
        
        // Lock the fragment
        this.fragment = toFragment(this.properties)
    }
    
    /**
     * Updates the model after dispatching a request to a controller, but before
     * calling the required view. Called by {@link VaadinDispatcher}
     * during the dispatch cycle. Also, sets the 'dispatched' field to true
     * 
     * @param resultObj The new model (if returned by the controller)
     */
    protected void finishRequest(Object resultObj) {
        if (resultObj && !this.model) {
            if (resultObj instanceof Component) {
                this.view = resultObj
                this.viewIsName = false
            } else if (resultObj instanceof Map) {
                this.model.putAll(resultObj)
            } else {
                this.model = [result:resultObj]
            }
        }
        this.dispatched = true
    } 
    
    /**
     * Get properties of this request.
     * 
     * @return This request as property map
     */
    public Map getProperties() {
        return url ? [url:url] : [
            controller:controller,
            action:action,
            view:view,
            params:params,
            attributes:attributes,
            model:model,
            flash:flash
        ]
    }
    
    /**
     * Set properties of this request
     * 
     * @param props New request properties
     */
    public void setProperties(Map props) {
        if (props) {
            this.controller = props.controller
            this.action = props.action
            this.view = props.view
            this.params = props.params ?: [:]
            this.attributes = props.attributes ?: [:]
            this.model = props.model ?: [:]
            this.flash = props.flash ?: [:]
            this.id = props.id
            this.instance = props.instance
            this.url = props.url
        }
    }
    
    /**
     * This request object represented as a string
     */
    public String toString() {
        // Don't print out the whole params & model maps...
        def props = this.properties
        if (props.params) {
            props.params = Utils.toString(props.params)
        }
        if (props.attributes) {
            props.attributes = Utils.toString(props.attributes)
        }
        if (props.model) {
            props.model = Utils.toString(props.model)
        }
        if (props.flash) {
            props.flash = Utils.toString(props.flash)
        }
        return "${uid} ${props}"
    }
}
