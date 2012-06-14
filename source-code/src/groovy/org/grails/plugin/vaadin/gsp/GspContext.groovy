package org.grails.plugin.vaadin.gsp

/**
 * For executing a tag's body, using the tag's Vaadin Component as the 
 * 'active' component, to which nested tags will append their Vaadin Components.
 * <p>
 * The active component is stored in the current thread.
 * 
 * @author Francis McKenzie
 */
class GspContext {
    /**
     * Holds the active gspContext in the current thread
     */
    protected static ThreadLocal<GspComponentNode> nodeHolder = new ThreadLocal<GspComponentNode>()
    
    /**
     * Evaluates the body of the specified Gsp Component Node, but first
     * sets the specified node to be the 'active' component node. This means that
     * whenever any child tags add components to their parent, they will be adding
     * those components to the node specified here.
     * 
     * @param node The node that will be the 'active' node, and whose body will be evaluated
     * @param node True if the node is the top-level Gsp - if so, the context has no active node when done
     * @return The output of executing the node's body
     */
    CharSequence evaluate(GspComponentNode node, boolean isRoot = false) {
        // Override session node
        GspComponentNode parent = this.node
        this.node = node
        
        CharSequence bodyText
        try {
            // Evaluate body
            bodyText = node.bodyText

        } finally {
            // Restore previous node, or clear if root
            if (isRoot) {
                this.node = null
            } else {
                this.node = parent
            }
            
            return bodyText
        }
    }
    
    /**
     * Gets the current active node, to which any child nodes will attach their
     * Vaadin Components.
     * 
     * @return The current active node
     */
    public GspComponentNode getNode() {
        return nodeHolder.get()
    }
    
    /**
     * Sets the current active node, to which any child nodes will attach their
     * Vaadin Components.
     */
    protected void setNode(GspComponentNode node) {
        if (!node) nodeHolder.remove()
        else nodeHolder.set(node)
    }
}
