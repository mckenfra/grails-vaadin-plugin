package org.grails.plugin.vaadin.ui

import com.vaadin.ui.Button

/**
 * Mimics the <a href="http://grails.org/doc/latest/ref/Tags/link.html">g:link</a> tag
 * 
 * @author Francis McKenzie
 * @see org.grails.plugin.vaadin.VaadinRequest
 */
@SuppressWarnings("unchecked")
class GrailsButton extends Button {
    
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
     * and onClick closure.
     * <p>
     * Note that the args can be a map containing 'controller', 'action' etc.
     * or simply a URI fragment, e.g. "#/book/list"
     * <p>
     * See {@link org.grails.plugin.vaadin.VaadinRequest} for the valid
     * list of args that can be used for a Vaadin 'request'.
     * 
     * @param caption The button caption to display
     * @param args Either a map of args or a URI fragment string
     * @arg onClick Specify a closure that mimics the javascript 'onclick'. I.e. if it returns
     * false, we don't action the link.
     * @see org.grails.plugin.vaadin.VaadinRequest
     */
    public GrailsButton(String caption, Object args, Closure onClick) {
        super(caption)
        
        // For following the link
        def dispatch = args instanceof Map ?
            { application.dispatcher.dispatch(args) } :
            { application.dispatcher.dispatchWithFragment(args?.toString()) }
        
        // But we need to catch a cancel
        final cancellableClick = {
            if (onClick && onClick(dispatch) == false) {
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
}
