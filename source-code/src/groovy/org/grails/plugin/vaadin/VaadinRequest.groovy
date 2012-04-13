package org.grails.plugin.vaadin

import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.web.util.TypeConvertingMap;

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

    protected String controller
    protected String action
    protected String view
    protected Map params
    protected Map model
    protected Map flash
    
    /**
     * Indicates if this request has been dispatched 
     */
    protected boolean dispatched
    /**
     * Create an empty request
     */
    public VaadinRequest() {}
    /**
     * Create a request with the specified properties
     */
    public VaadinRequest(Map props) { this.properties = props }
    
    /**
     * The requested controller - for example 'book'
     */
    public String getController() { this.controller }
    /**
     * The requested action - for example 'show'
     */
    public String getAction() { this.action }
    /**
     * The requested view - for example 'show'
     */
    public String getView() { this.view }
    /**
     * The request params - for example [id:15]
     */
    public Map getParams() { this.params }
    /**
     * The request model - for example [bookInstance:bookInstance]
     */
    public Map getModel() { this.model }
    /**
     * The flash message and error - for example [message:"Book 15 created!"]
     */
    public Map getFlash() { this.flash }
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
        if (!args) {
            throw new IllegalArgumentException("Invalid redirect args ${args}")
        } else {
            if (log.isDebugEnabled()) {
                log.debug "REDIRECT: ${args} >> ${this.properties}"
            }
            this.controller = args.controller ?: this.controller
            this.action = args.action ?: this.action
            this.view = args.view ?: this.action
            this.params = args.params ?: [:]
            this.model = [:]
            this.id = args.id
            this.instance = args.instance
        }
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
            this.params = args.params ?: this.params
            this.model = args.model ?: this.model
            this.id = args.id
            this.instance = args.instance
        }
    }
    
    /**
     * Converts this 'request' into a URI fragment - for example '#book/show/15'
     * 
     * @return The URI fragment corresponding to this 'request'
     */
    public String getFragment() {
        def useInlineId = (this.id || this.id == "0") && action != "index"
        def paramsWithoutId = params?.findAll { k,v-> ! (useInlineId && k == "id") }
        def hasParams = paramsWithoutId?.size()
        return "${controller}" +
            (action == "index" ? "" : "/${action}") +
            (useInlineId ? "/${this.id}" : "") +
            (!hasParams ? "" : "?" + paramsWithoutId.entrySet().collect { "${it.key}=${it.value}" }.join("&"))
    }
    
    /**
     * Gets view uri of this request, minus params, e.g. book/show.
     * <p>
     * Note that if the view name starts with "/" then it is resolved
     * using the root views directory. Otherwise the request's controller
     * name is prepended to the view.
     * 
     * @return Uri of view
     */
    public String getViewFullName() {
        return this.view?.startsWith('/') ? this.view : "/${controller}/${view}"
    }
    
    /**
     * Initialises this 'request' based on the 'request' that is currently active, and using
     * the specified set of defaults args.
     * <p>
     * This is called by the {@link VaadinDispatcher} at the beginning of the dispatch cycle.
     * E.g. one of the things it does is to set the 'controller' for the new request to be
     * the same as the 'controller' of the current active request, if it is not
     * explicitly overridden in the request parameters.
     * 
     * @param activeRequest The current active request
     * @param newRequest The parameters for the new request (e.g. [action:show, id:15])
     * @param defaults The default parameters, if any required params are missing
     */
    protected void newRequest(VaadinRequest activeRequest, Map newRequest, Map defaults) {
        this.properties = newRequest
        if (! newRequest.controller) {
            this.controller = activeRequest.controller ?: defaults?.controller
        } else if (newRequest.controller.metaClass.getMetaMethod("getVaadinClass")) {
            this.controller = newRequest.controller.metaClass.getMetaMethod("getVaadinClass").invoke(this.controller).logicalPropertyName
        } else {
            this.controller = newRequest.controller.toString()
        }
        this.action = this.action ?: defaults?.action
        this.view = this.action
        this.params = this.params ?: [:]
        this.model = [:]
        this.flash = [:]
        this.dispatched = false
    }
    
    /**
     * Empties the model before dispatching a request. Called by {@link VaadinDispatcher}
     * during the dispatch cycle.
     */
    protected void startRequest() {
        this.model = [:]
        this.dispatched = false
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
            if (resultObj instanceof Map) {
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
    protected Map getProperties() {
        return [
            controller:controller,
            action:action,
            view:view,
            params:params,
            model:model,
            flash:flash
        ]
    }
    
    /**
     * Set properties of this request
     * 
     * @param props New request properties
     */
    protected void setProperties(Map props) {
        this.controller = props?.controller
        this.action = props?.action
        this.view = props?.view
        this.params = props?.params ?: [:]
        this.model = props?.model ?: [:]
        this.flash = props?.flash ?: [:]
        this.id = props?.id
        this.instance = props?.instance
    }
        
    /**
     * Set properties of this request to match specified request
     * 
     * @param request Other request to copy
     */
    protected void setRequest(VaadinRequest request) {
        this.properties = request.properties
    }
    
    /**
     * This request object represented as a string
     */
    public String toString() {
        // Don't print out the whole params & model maps...
        def props = this.properties
        if (props.params) {
            props.params = VaadinUtils.toString(props.params)
        }
        if (props.model) {
            props.model = VaadinUtils.toString(props.model)
        }
        if (props.flash) {
            props.flash = VaadinUtils.toString(props.flash)
        }
        return props.toString()
    }
}

