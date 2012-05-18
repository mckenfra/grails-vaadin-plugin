package org.grails.plugin.vaadin.ui

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
    protected GspLayout gspLayout
    /**
     * The layout name or URI of the Gsp to use as layout.
     * <p>
     * If starts with '/' then is resolved relative to 'views' dir. Otherwise,
     * is resolved relative to 'views/layouts'
     */
    String name

    /**
     * Empty constructor
     */
    public GspTemplateLayout() {
        super()
        this.templateContents = "" // Prevent NPE
    }

    /**
     * Creates a new instance using the specified gsp layout template, and then applies
     * the specified tag body to this template.
     * <p>
     * Note this is primarily used by tag libraries.
     *
     * @param application Current vaadin application - required because we render immediately, not when attached.
     * @param name The layout name or URI of the GSP to use as a template
     * @param body The tag's body closure
     * @param params The params map to use when rendering the layout.
     * @param model The model map to use when rendering the layout.
     * @param flash The flash scope object to use when rendering the layout.
     * @param controllerName The controller name to use when rendering the GSP.
     */
    public GspTemplateLayout(Application application, String name, Closure body, Map params = null, Map model = null, Map flash = null, String controllerName = null) {
        super()
        this.name = name
        addGspLayout(new GspLayout(application, toLayoutName(name), body, params, model, flash, controllerName))
    }
    
    /**
     * Adds the specified GspLayout to this container.
     */
    protected void addGspLayout(GspLayout gspLayout) {
        this.gspLayout = gspLayout
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
        if (!application) {
            throw new Exception("Cannot update this component if not attached!")
        }
        addGspLayout(new GspLayout(application, this.gspLayout, toLayoutName(name)))
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
        update()
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
        if (args.name) {
            this.name = args.name
            update()
        }
    }
    
    /**
     * Converts the layout name to the URI of the Gsp.
     * <p>
     * If starts with '/' then is resolved relative to 'views' dir. Otherwise,
     * is resolved relative to 'views/layouts'
     */
    protected String toLayoutName(String name) {
        return name?.startsWith('/') ? name : "/layouts/${name}"
    }
}
