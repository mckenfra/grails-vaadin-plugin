package org.grails.plugin.vaadin.ui

import com.vaadin.Application;
import com.vaadin.ui.Button

/**
 * Mimics the <a href="http://grails.org/doc/latest/ref/Tags/link.html">g:link</a> tag.
 * <p>
 * Note one key benefit of the Vaadin button is that you can pass the
 * domain instance itself using the 'instance' parameter, instead of using
 * a params map.
 * 
 * @author Francis McKenzie
 * @see org.grails.plugin.vaadin.VaadinRequest
 */
@SuppressWarnings("unchecked")
class GrailsButton extends Button {
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
     * Specify a closure that mimics the javascript 'onclick'. I.e. if it returns
     * false, we don't action the link.
     */
    Closure onclick
    
    /**
     * Construct an empty button.
     */
    public GrailsButton() {
        super()
    }
    
    /**
     * Construct a button using the specified caption and 'request' args.
     * <p>
     * Note that the args can be a map containing 'controller', 'action' etc.
     * or simply a URI fragment, e.g. "#/book/list"
     * <p>
     * See {@link org.grails.plugin.vaadin.VaadinRequest} for the valid
     * list of args that can be used for a Vaadin 'request'.
     * 
     * @param caption The button caption to display
     * @param args Either a map of args or a URI fragment string
     * @see org.grails.plugin.vaadin.VaadinRequest
     */
    public GrailsButton(String caption, Object args) {
        this(caption,args,null)
    }
    
    /**
     * Construct a button using the specified caption, 'request' args
     * and onclick closure.
     * <p>
     * Note that the args can be a map containing 'controller', 'action' etc.
     * or simply a URI fragment, e.g. "#/book/list"
     * <p>
     * See {@link org.grails.plugin.vaadin.VaadinRequest} for the valid
     * list of args that can be used for a Vaadin 'request'.
     * 
     * @param caption The button caption to display
     * @param args Either a map of args or a URI fragment string
     * @arg onclick Specify a closure that mimics the javascript 'onclick'. I.e. if it returns
     * false, we don't action the link.
     * @see org.grails.plugin.vaadin.VaadinRequest
     */
    public GrailsButton(String caption, Object args, Closure onclick) {
        super(caption)
        if (args instanceof Map) {
            this.args.putAll(args)
        } else {
            this.fragment = args?.toString()
        }
        this.onclick = onclick
    }
    
    /**
     * Renders the gsp body.
     */
    protected void render() {
        final application = requireVaadinApplication()
        final transactionManager = requireVaadinTransactionManager()
        
        // Catch a cancel
        final thiz = this
        final cancellableClick = {
            try {
                if (onclick && onclick(thiz) == false) {
                    // Cancelled
                } else {
                    // Not cancelled
                    thiz.dispatch()
                }
            } finally {
                this.enabled = true
            }
        }
        
        // Always disable on click
        this.disableOnClick = true

        // Put it all together by adding the full listener
        this.addListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                transactionManager.wrapInTransaction(cancellableClick)
            }
        })
    }
    
    /**
     * Dispatches the request to the specified controller/action/fragment, without executing
     * the onclick method.
     * <p>
     * If no controller/action/fragment is specified, then this silently does nothing.
     * <p>
     * Note that the button must be attached before this method is called.
     */
    public void dispatch() {
        if (args || fragment) {
            def app = requireVaadinApplication()
            if (args) app.dispatcher.dispatch(args)
            else if (fragment) app.dispatcher.dispatch(fragment)
        }
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
    /**
     * Get the onclick closure of the link.
     */
    Closure getOnclick() { onclick }
    /**
     * Set the onclick closure of the link.
     */
    void setOnclick(Closure onclick) { this.onclick = onclick }
    
    /**
     * Helper method for situations where a Vaadin Transaction Manager is required
     */
    protected requireVaadinTransactionManager() {
        def result = requireVaadinApplication().getBean("vaadinTransactionManager")
        if (!result) throw new NullPointerException("Spring bean not found: 'vaadinTransactionManager'")
        return result
    }
    /**
     * Helper method for situations where a Vaadin Application is required
     */
    protected requireVaadinApplication() {
        def result = this.application
        if (!result) throw new NullPointerException("Application not found - component must be attached")
        return result
    }
    /**
     * Prevent rendering twice
     */
    protected boolean rendered
    /**
     * Requests the render to take place if not yet done.
     * <p>
     * Note that rendering should only be done after the component has been attached,
     * as the Vaadin application is required.
     * 
     * @param force Force the render, even if already done
     */
    public void requestRender(boolean force = false) {
        if ((!rendered && application) || force) {
            requireVaadinApplication()
            render()
            rendered = true
        }
    }
    /**
     * Render when attached
     */
    public void attach() {
        requestRender()
        super.attach()
    }
}
