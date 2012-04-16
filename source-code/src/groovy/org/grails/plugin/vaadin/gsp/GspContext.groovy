package org.grails.plugin.vaadin.gsp

import javax.servlet.http.HttpSession;

/**
 * For executing a tag's body, using the tag's Vaadin Component as the 
 * 'active' component, to which nested tags will append their Vaadin Components.
 * 
 * @author Francis McKenzie
 */
class GspContext {
    /**
     * The current active Vaadin Component node is stored in the session.
     */
    HttpSession session
    
    /**
     * Creates a new GspContext using the specified session to store
     * the current active Vaadin Component.
     * 
     * @param session The session that will store the active component
     */
    public GspContext(HttpSession session) {
        this.session = session
    }

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
        GspComponentNode parent = session.getAttribute("org.grails.plugin.vaadin.component")
        session.setAttribute("org.grails.plugin.vaadin.component", node)

        // Evaluate body
        CharSequence bodyText = node.bodyText

        // Restore session node, or clear if root
        if (isRoot) {
            session.removeAttribute("org.grails.plugin.vaadin.component")
        } else {
            session.setAttribute("org.grails.plugin.vaadin.component", parent)
        }
        
        return bodyText
    }
    
    /**
     * Gets the current active node, to which any child nodes will attach their
     * Vaadin Components.
     * 
     * @return The current active node
     */
    GspComponentNode getNode() {
        return session.getAttribute("org.grails.plugin.vaadin.component")
    }
}
