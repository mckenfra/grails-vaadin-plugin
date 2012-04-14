package org.grails.plugin.vaadin.ui

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
     * Add default click listener when attaching button to component graph.
     */
    @Override
    public void attach() {
        // For following the link
        def dispatch = args ?
            { application.dispatcher.dispatch(args) } :
            { application.dispatcher.dispatchWithFragment(fragment) }
        
        // But we need to catch a cancel
        final cancellableClick = {
            if (onclick && onclick(dispatch) == false) {
                // Cancelled
            } else {
                // Not cancelled
                dispatch()
            }
        }
        
        // Put it all together by adding the full listener
        this.addListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                cancellableClick()
            }
        })
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
    * Get the onclick closure of the link.
    */
    Closure getOnclick() { args.onclick }
    /**
    * Set the onclick closure of the link.
    */
    void setOnclick(Closure onclick) { args.onclick = onclick } 
}
