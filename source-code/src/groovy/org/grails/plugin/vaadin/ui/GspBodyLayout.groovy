package org.grails.plugin.vaadin.ui

import com.vaadin.Application;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomLayout;

import org.apache.commons.logging.LogFactory;
import org.grails.plugin.vaadin.gsp.GspComponentNode;
import org.grails.plugin.vaadin.gsp.GspContext;
import org.grails.plugin.vaadin.gsp.GspEvaluator;
import org.grails.plugin.vaadin.utils.Stopwatch;

/**
 * A Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/CustomLayout.html">CustomLayout</a>
 * that uses a Grails GSP body as the template.
 * 
 * @author Francis McKenzie
 */
class GspBodyLayout extends CustomLayout {
    def log = LogFactory.getLog(this.class)

    /**
     * Components resulting from rendering the body
     */
    protected Map<String,Component> bodyComponents = [:]
    /**
     * Removes the body components from this layout, because they're needed
     * for another layout
     */
    protected Map<String,Component> removeBodyComponents() {
        bodyComponents.values().each { removeComponent(it) }
        def removed = bodyComponents
        bodyComponents = [:]
        return removed
    }
    /**
     * Removes the existing body components and adds the specified components in their place
     */
    protected Map<String,Component> replaceBodyComponents(Map<String,Component> bodyComponents) {
        Map<String,Component> result = this.removeBodyComponents()
        this.bodyComponents = bodyComponents
        this.bodyComponents?.each { loc,c -> addComponent(c,loc) }
        return result
    }
    
    /**
     * The gsp body closure - automatically set to null when evaluated
     */
    Closure body
    /**
     * If true, indicates this Gsp is the top-level Gsp, and not a Gsp rendered
     * inside another.
     */
    boolean root

    /**
     * Create an empty layout
     */
    public GspBodyLayout() {
        this.templateContents = "" // Prevent NPE
    }
    
    /**
     * Create a layout from a Gsp body
     */
    public GspBodyLayout(Closure body) {
        this.templateContents = "" // Prevent NPE
        this.body = body
    }
    
    /**
     * Initialises this GspLayout from the other specified layout.
     * <p>
     * Note that this method will remove all body components from the specified
     * layout and add them to this one instead.
     * 
     * @param layout The existing layout to apply the new template to
     * @param uri The uri of the new template Gsp to apply
     */
    public GspBodyLayout(GspBodyLayout other) {
        this.templateContents = "" // Prevent NPE
        if (!other) return
        
        // Steal other's body components and text
        this.templateContents = other.templateContents
        if (other.body) this.body = other.body
        else this.replaceBodyComponents(other.removeBodyComponents())
    }

    /**
     * Renders the gsp body.    
     */
    protected void render() {
        renderBody()
    }
    
    /**
     * Initialises the superclass <a href="http://vaadin.com/api/com/vaadin/ui/CustomLayout.html">CustomLayout</a>
     * using the specified tag body as the template.
     * <p>
     * Any Vaadin Components in the tag body are automatically added to this
     * CustomLayout, in the order in which they appear.
     */
    protected void renderBody() {
        // Do nothing with null body
        if (!body) return
 
        // Logging
        def stopwatch = Stopwatch.enabled ? new Stopwatch("[BODY] ${this}", this.class) : null
        
        if (log.isDebugEnabled()) {
            log.debug "RENDER BODY: ${this}"
        }
        
        // Execute body - we use its body text as this layout's template
        def evaluator = new GspEvaluator(this, requireVaadinApplication(), root)
        def result = evaluator.evaluate(body)
        if (!templateContents) templateContents = result.text ?: ""
        
        // Clear the body so we don't try to render it again
        this.body = null
        
        // Timing logging
        stopwatch?.stop()
    }

    /**
     * This component as a String - mainly useful for logging purposes.
     */
    String toString() {
        return "{GSP BODY}"
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
