package org.grails.plugin.vaadin.gsp

import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.CustomLayout;

/**
 * Holds a Vaadin Component and a tag body, and facilitates adding
 * components & config nested within the tag body to this component.
 * 
 * @author Francis McKenzie
 */
class GspComponentNode {
    /**
     * The Vaadin Component to which the tag body applies
     */
    Component component
    /**
     * The tag body that will be evaluated
     */
    Closure body
    /**
     * A closure to run against the result of evaluating the tag body
     */
    Closure withBody
    /**
     * A closure to run against the collection of config objects that result
     * from evaluating the tag body.
     */
    Closure withConfig
    /**
     * The body text that results from evaluating the tag body. (Note this
     * will be null until the first call to {@link #evaluateBody()})
     */
    CharSequence bodyText
    /**
     * Indicates if {@link #evaluateBody()} has been run yet or not.
     */
    protected boolean isBodyEvaluated
    /**
     * The collection of config objects that is populated by nested 
     * config tags when the tag body is evaluated.
     */
    List<GspComponentConfig> configs = []
    
    /**
     * Creates a new node with the specified Vaadin Component and tag body.
     * 
     * @param component The Vaadin Component to which components in the tag body will be added
     * @param body The tag body that will be evaluated
     */
    public GspComponentNode(Component component, Closure body) {
        this(component, body, null, null)    
    }
    
    /**
     * Creates a new node with the specified Vaadin Component, tag body
     * and closure to run against the result of evaluating the tag body.
     * 
     * @param component The Vaadin Component to which components in the tag body will be added
     * @param body The tag body that will be evaluated
     * @param withBody The closure to run against the result of evaluating the tag body
     */
    public GspComponentNode(Component component, Closure body, Closure withBody) {
        this(component, body, withBody, null)
    }
    
    /**
     * Creates a new node with the specified Vaadin Component, tag body,
     * and closures to run against the result of evaluating the tag body, and
     * the collection of config objects added during evaluation of the tag body.
     * 
     * @param component The Vaadin Component to which components in the tag body will be added
     * @param body The tag body that will be evaluated
     * @param withBody The closure to run against the result of evaluating the tag body
     * @param withConfig The closure to run against the collection of config objects
     * resulting from evaluating the tag body.
     */
    public GspComponentNode(Component component, Closure body, Closure withBody, Closure withConfig) {
        this.component = component
        this.body = body
        this.withBody = withBody
        this.withConfig = withConfig
    }
    
    /**
     * Gets the text of evaluating the tag body. Only evaluates the body the
     * first time this method is called, then stores the result for future
     * calls to this method.
     * 
     * @return The text result of executing the tag body
     */
    public CharSequence getBodyText() {
        if (!this.isBodyEvaluated) {
            this.isBodyEvaluated = true
            this.bodyText = evaluateBody()
        }
        return this.bodyText
    }
    
    /**
     * Evaluates the tag body, and returns the result. Also calls the
     * withBody and withConfig closures (if they exist) on the resulting
     * body text and collection of config objects.
     * 
     * @return The text result of executing the tag body
     */
    protected CharSequence evaluateBody() {
        // Evaluate body
        CharSequence text = this.body ? this.body() : null
        
        // Execute withBody
        if (this.withBody) {
            this.withBody(text)
        }
        
        // Execute withConfig
        if (this.withConfig) {
            this.withConfig(configs)
        }
        
        return text
    }
    
    /**
     * Attaches the specified child Vaadin Component to the Vaadin Component stored
     * in this node.
     * <p>
     * Note this node's Vaadin Component MUST be of type
     * {@link com.vaadin.ui.ComponentContainer}
     * <p>
     * If this node's Vaadin Component is a CustomLayout, then calls
     * addComponent(com.vaadin.ui.Component,java.lang.String) on the component.
     * <p>
     * Otherwise, it calls addComponent(com.vaadin.ui.Component)
     * <p>
     * Node that this method can return text, which is then used by other classes
     * such as @{link GspAttacher} to replace the component in the parent tag's body
     * text with some HTML. However, this class always returns null from this method.
     * Subclasses of this class should override this method if they need to return
     * text when adding a child component.
     *  
     * @param child The child Vaadin Component to add to this node's container component
     * @param params The (optional) params to use when adding the child component to this node's component. Typically will contain 'location'
     * @return The text result of adding the child component - always returns null, because
     * this is only useful to subclasses.
     */
    public CharSequence attachComponent(Component child, Map params = null) {
        if (component instanceof CustomLayout && location) {
            component.addComponent(child, location)
        } else if (component instanceof ComponentContainer) {
            component.addComponent(child)
        } else {
            throw new Exception("Cannot attach ${child?.class} to ${component?.class}")
        }
        return null
    }
    
    /**
     * Adds the specified component config to the collection of component configs
     * held by this node. The component configs will be passed to the withConfig
     * closure when the {@link #evaluateBody()} method is called.
     * <p>
     * Note the componentClass of the {@link GspComponentConfig} must be the same
     * as the class of the Component in this node, otherwise an error will be thrown.
     * 
     * @param componentConfig The component config to add this node
     */
    public void attachConfig(GspComponentConfig componentConfig) {
        if (componentConfig?.componentClass?.isAssignableFrom(component?.class)) {
            configs << componentConfig
        } else {
            throw new Exception("Cannot add config for ${componentConfig?.componentClass} to ${component?.class}")
        }
    }
}
