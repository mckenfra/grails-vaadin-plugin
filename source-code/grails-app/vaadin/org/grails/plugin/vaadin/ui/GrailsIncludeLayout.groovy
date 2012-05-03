package org.grails.plugin.vaadin.ui

import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.CustomLayout;

/**
 * A Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/CustomLayout.html">CustomLayout</a>
 * that contains a single view Component resulting from a grails request.
 * <p>
 * The contained view Component is built by executing
 * a request for the specified controller, action, etc.
 * <p>
 * A key benefit of this class is it allows changing the view after the
 * page has been built and rendered. This is achieved by replacing the contained
 * Component with a new one.
 * 
 * @author Francis McKenzie
 */
class GrailsIncludeLayout extends CustomLayout {
    /**
     * The contained view
     */
    protected Component view
    /**
     * Link specified as map - contains for example 'controller', 'action' etc.
     */
    protected Map args = [:]
    /**
     * Specifies fragment we are linking to. Ignored if we specify other
     * parameters such as 'controller' or 'action'
     */
    String fragment
    
    /**
     * Empty constructor
     */
    public GrailsIncludeLayout() {
        super()
    }

    /**
     * Creates a new instance by executing the specified fragment request.
     *
     * @param fragment The fragment to request, for example "book/list"
     */
    public GrailsIncludeLayout(String fragment) {
        super()
        this.fragment = fragment
        include()
    }
    
    /**
     * Initialise as empty view if nothing set
     */
    public void attach() {
        if (!this.templateContents) {
            addView(new Label()) // Initialise empty
        }
        super.attach()
    }
    
    /**
     * Adds the specified view to this container.
     */
    protected void addView(Component view) {
        this.view = view
        if (!this.templateContents) {
            this.templateContents = "<div location='view'/>"
        }
        this.addComponent(view, "view")
    }
    
    /**
     * Submits a new request using the controller, action etc if specified,
     * otherwise the fragment.
     * <p>
     * Replaces the existing Gsp in this container with the result.
     */
    public void include() {
        addView(dispatcher.request(args ?: fragment))
    }
    
    /**
     * Submits a new request using the specified fragment, and replaces the existing
     * Gsp in this container with the result.
     * 
     * @param fragment The fragment for the request, for example "book/list"
     */
    public void include(String fragment) {
        this.args = [:]
        this.fragment = fragment
        include()
    }

    /**
     * Submits a new request using the specified args, and replaces the existing
     * Gsp in this container with the result.
     * 
     * @param args The args containing the request details, for example
     * 'controller', 'action' etc.
     */
    public void include(Map args) {
        this.args = [:]
        if (args != null) { this.args.putAll(args) }
        this.fragment = null
        include()
    }
    
    public String toString() {
        return args ? "${args}" : "${fragment}"
    }
    
    /**
    * Get the controller of the link - either a logical property name, or the class itself.
    */
   Object getController() { args.controller }
   /**
    * Set the controller of the link - either a logical property name, or the class itself.
    */
   void setController(Object controller) { args.controller = controller }
   /**
    * Get the action of the link.
    */
   String getAction() { args.action }
   /**
    * Set the action of the link.
    */
   void setAction(Object action) { args.action = action?.toString() }
   /**
    * Get the domain class id of the link.
    */
   String getId() { args.id }
   /**
    * Set the domain class id of the link.
    */
   void setId(Object id) { args.id = id?.toString() }
   /**
    * Get the domain class instance of the link.
    */
   Object getInstance() { args.instance }
   /**
    * Set the domain class instance of the link.
    */
   void setInstance(Object instance) { args.instance = instance }
   /**
    * Get the params of the link.
    */
   Map getParams() { args.params ?: [:] }
   /**
    * Set the params of the link.
    */
   void setParams(Map params) { args.params = params }
}
