package org.grails.plugin.vaadin.ui

import java.util.Map;

import com.vaadin.Application;
import com.vaadin.ui.CustomLayout;

/**
 * A Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/CustomLayout.html">CustomLayout</a>
 * that contains a single {@link org.grails.plugin.vaadin.ui.GrailsLayout}.
 * <p>
 * The contained {@link org.grails.plugin.vaadin.ui.GrailsLayout} is built using
 * the specified layout template.
 * <p>
 * A key benefit of this class is it allows changing the Gsp after the
 * page has been built and rendered. This is achieved by replacing the contained
 * {@link org.grails.plugin.vaadin.ui.GrailsLayout} with a new one.
 * 
 * @author Francis McKenzie
 */
class GspTemplateLayout extends CustomLayout {
    /**
     * The contained layout
     */
    GspLayout gspLayout

    /**
     * The layout name or URI of the Gsp to use as layout.
     * <p>
     * If starts with '/' then is resolved relative to 'views' dir. Otherwise,
     * is resolved relative to 'views/vaadin/layouts'
     */
    String name

    /**
     * Empty constructor
     */
    public GspTemplateLayout() {
        this.templateContents = "" // Prevent NPE
    }

    /**
     * Sets the body of the gsp layout tag
     * 
     * @param body The tag body
     */
    public void setBody(Closure body) {
        if (!this.gspLayout) this.gspLayout = new GspLayout()
        this.gspLayout.body = body
    }
    
    /**
     * Sets the context of the Gsp template.
     * 
     * @param gsp The gsp context args, which are listed below.
     * 
     * @param uri The uri of the GSP view, template or resource.
     * @param params The params map to use when rendering the GSP.
     * @param model The model map to use when rendering the GSP.
     * @param flash The flash scope object to use when rendering the GSP.
     * @param controllerName The controller name to use when rendering the GSP.
     */
    public void setContext(Map gsp) {
        if (!this.gspLayout) this.gspLayout = new GspLayout()
        this.gspLayout.gsp = gsp
    }
    
    /**
     * Constructs the gsp
     */
    protected void render() {
        if (!name) return
        this.gspLayout = new GspLayout(this.gspLayout, toLayoutName(name))
        if (!this.templateContents) {
            this.templateContents = "<div location='gsp'/>"
        }
        this.addComponent(gspLayout, "gsp")
    }
    
    /**
     * Renders the Gsp corresponding to the configured layout name or URI,
     * and replaces the existing Gsp in this container with the result.
     * <p>
     * Should only be called after this component is attached.
     */
    public void update() {
        requestRender(true)
    }
    
    /**
     * Changes the template used for rendering the Gsp. The original body
     * components are added to the new template.
     * <p>
     * Should only be called after this component is attached.
     * 
     * @param name The layout name or URI of the Gsp to use as layout.
     */
    public void update(String name) {
        this.name = name
        requestRender(true)
    }

    /**
     * Changes the template used for rendering the Gsp. The original body
     * components are added to the new template.
     * <p>
     * Should only be called after this component is attached.
     * 
     * @param name The layout name or URI of the Gsp to use as layout.
     */
    public void update(Map args) {
        this.name = args?.name
        requestRender(true)
    }
    
    /**
     * Converts the layout name to the URI of the Gsp.
     * <p>
     * If starts with '/' then is resolved relative to 'views' dir. Otherwise,
     * is resolved relative to 'views/layouts'
     */
    protected String toLayoutName(String name) {
        return name?.startsWith('/') ? name : "/vaadin/layouts/${name}"
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
