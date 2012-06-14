package org.grails.plugin.vaadin.gsp

import com.vaadin.Application;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomLayout;

/**
 * Evaluates a Gsp body, adds any nested Vaadin components to the specified container
 * and returns the resulting text and Vaadin components.
 *
 * @author Francis McKenzie
 */
class GspEvaluator {
    /**
     * Holds the result of an evaluation
     */
    public static class Result {
        /**
         * The components resulting from evaluating the GSP body
         */
        Map<String,Component> components = [:]
        /**
         * The text resulting from evaluating the GSP body
         */
        String text
    }
    
    /**
     * The parent component (required)
     */
    CustomLayout parent
    /**
     * True if this is the root component (optional)
     */
    boolean root = false

    /**
     * Empty constructor
     */
    public GspEvaluator() {}
    
    /**
     * Initialise with specified values. 
     * 
     * @param parent The parent component
     * @param root True if this is the root component 
     */
    public GspEvaluator(CustomLayout parent, boolean root = false) {
        this.parent = parent
        this.root = root
    }
    
    /**
    * Executes the body closure (could be a tag body) and stores the resulting
    * text and components.
    *
    * @param parent The parent component
    * @param body The closure to execute to obtain the GSP text and components
    * @param root True if this is the root component
    * 
    * @return The result of evaluating the body
    */
   public Result evaluate(Closure body) {
       if (!parent) throw new NullPointerException("Parent component required!")
       if (!body) throw new NullPointerException("Body required!")
       
       // Prepare to execute
       GspLayoutNode node = new GspLayoutNode(parent, body)
       GspContext context = new GspContext()
       
       // Execute and return result
       Result result = new Result()
       result.text = context.evaluate(node, root)?.toString()
       result.components = node.components
       
       return result
   }

   /**
    * A special {@link org.grails.plugin.vaadin.gsp.GspComponentNode} that
    * keeps a collection of any Vaadin Components that are added to it,
    * and returns an HTML snippet of the form
    * &lt;div location='component_0' /&gt;
    * to the child component, which should then add this snippet to the
    * page's body text.
    * <p>
    * When the page's body text has been fully rendered, the collection
    * of Vaadin Components can then be added to the GspLayout's location
    * slots.
    *
    * @author Francis McKenzie
    */
   protected class GspLayoutNode extends GspComponentNode {
       Map<String,Component> components = [:]
       protected CustomLayout layout
       
       public GspLayoutNode(CustomLayout layout, Closure body) {
           super(layout, body)
           this.layout = layout
       }
       
       @Override
       public CharSequence attachComponent(Component component, Map params = null) {
           params = params ?: [:]

           // Set location
           params.location = params.location ?: "component_${layout.componentCount}"
           
           // Add component to parent
           components[params.location] = component
           layout.addComponent(component, params.location)
           
           // Format params as wrapper attributes
           def attrs = params.findAll {k,v->k&&v}.collect { "${it.key}='${it.value}'" }.join(" ")
           
           // Return wrapper HTML
           return "<div ${attrs}></div>"
       }
   }
}
