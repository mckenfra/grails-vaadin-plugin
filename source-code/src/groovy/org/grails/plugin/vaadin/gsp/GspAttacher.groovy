package org.grails.plugin.vaadin.gsp

import com.vaadin.ui.Component;

/**
 * Attaches a Vaadin Component or ComponentConfig to the
 * Vaadin Component that is currently active.
 * <p>
 * Used in constructing GSP views containing Vaadin components.
 * 
 * @author Francis McKenzie
 */
class GspAttacher {
    /**
     * The context that holds the active Vaadin Component
     */
    GspContext context
    /**
     * The writer to use to write out the result of attaching the component.
     * This should just be the 'out' of the tag library. 
     */
    Writer out
    
    /**
     * Constructs a new attacher, for adding a new Component to the active
     * Vaadin Component in the context.
     * 
     * @param context The context for getting a handle to the active Vaadin Component
     * @param out The writer to use to print out the result (if any) of attaching the Component
     */
    public GspAttacher(GspContext context, Writer out) {
        this.context = context
        this.out = out
    }

    /**
     * Attaches the specified Component to the context's active Vaadin Component.
     * 
     * @param component The Vaadin Component to attach
     * @param params The (optional) params to use when adding the child component to this node's component. Typically will contain 'location'
     */
    def attachComponent(Component component, Map params = null) {
        // Attach to parent
        if (context.node) {
            def attachingText = context.node.attachComponent(component, params)
            if (attachingText && out) {
                out << attachingText
            }
        }
    }
    
    /**
     * Applies the specified Component configuration to the context's active Vaadin
     * Component
     * 
     * @param config The configuration to apply to the active Vaadin Component
     */
    def attachConfig(GspComponentConfig config) {
        context.node?.attachConfig(config)
    }
}
