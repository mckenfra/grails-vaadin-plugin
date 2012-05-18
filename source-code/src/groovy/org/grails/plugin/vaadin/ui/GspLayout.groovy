package org.grails.plugin.vaadin.ui

import com.vaadin.Application;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomLayout;

import org.apache.commons.logging.LogFactory;
import org.grails.plugin.vaadin.gsp.GspComponentNode;
import org.grails.plugin.vaadin.gsp.GspContext;
import org.grails.plugin.vaadin.utils.Stopwatch;

/**
 * A Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/CustomLayout.html">CustomLayout</a>
 * that uses a Grails GSP file as the template.
 * <p>
 * This is distinct from Vaadin's custom layout, which typically requires the template to be
 * a static HTML file.
 * <p>
 * This class facilitates mixing normal Grails tags such as &lt;g:message&gt; with Vaadin tags
 * such as &lt;v:label&gt; in the same GSP. The aim is that you get the best of both worlds.
 * <p>
 * The GSP file can be a view, template or resource.
 * See {@link org.grails.plugin.vaadin.gsp.GspResourceLocator}
 * <p>
 * Note that this class uses a 'mock' HttpRequest/HttpResponse when rendering the GSP.
 * Currently, this implementation does not support Sitemesh layouts in GSPs. However,
 * a GSP can use another GSP as a template by utilising the &lt;v:layout&gt; tag instead.
 * 
 * @author Francis McKenzie
 */
class GspLayout extends CustomLayout {
    def log = LogFactory.getLog(this.class)

    /**
     * The URI of the Gsp used as layout
     */
    String templateGsp
    /**
     * The tag body to evaluate, and attach contained components to this layout
     */
    Closure body
    /**
     * The params for the Gsp
     */
    Map params
    /**
     * The model for the Gsp
     */
    Map model
    /**
     * The flash for the Gsp
     */
    Map flash
    /**
     * The controller name for the Gsp
     */
    String controllerName
    /**
     * If true, indicates this Gsp is the top-level Gsp, and not a Gsp rendered
     * inside another.
     */
    boolean root
    /**
     * All components resulting from rendering the Gsp template and/or tag body
     */
    protected Map<String,Component> components = [:]
    /**
     * Components resulting from rendering the body only
     */
    protected Map<String,Component> bodyComponents = [:]
    /**
     * Removes the body components from this layout, because they're needed
     * for another layout
     */
    protected Map<String,Component> removeBodyComponents() {
        def removed = bodyComponents
        removed.values().each { removeComponent(it) }
        bodyComponents = [:]
        return removed
    }
    /**
     * The Vaadin Application - only used if not attached.
     */
    protected vaadinApplication
    /**
     * If this component is attached, gets the Vaadin Application of the parent,
     * else gets the Vaadin Application from this class's <code>vaadinApplication</code>
     * field.
     */
    Application getVaadinApplication() {
        return this.application ?: this.vaadinApplication
    }

    /**
     * Creates a new GspLayout with the specified GSP as the root view.
     * 
     * @param application Current vaadin application - required because we render immediately, not when attached.
     * @param gsp The uri of the GSP view, template or resource.
     * @param params The params map to use when rendering the GSP.
     * @param model The model map to use when rendering the GSP.
     * @param flash The flash scope object to use when rendering the GSP.
     * @param controllerName The controller name to use when rendering the GSP.
     * @param root True if this is the root Gsp of the page (defaults to false)
     */
    public GspLayout(Application application, String gsp, Map params = null, Map model = null, Map flash = null,
        String controllerName = null, boolean root = false) {
        this(application, gsp, null, params, model, flash, controllerName, root, "", null)
    }
    
    /**
     * Creates a new GspLayout with the specified tag body.
     * <p>
     * Note this is primarily used by tag libraries.
     * 
     * @param application Current vaadin application - required because we render immediately, not when attached.
     * @param body The tag's body closure
     * @param root True if this is the root Gsp of the page (defaults to false)
     */
    public GspLayout(Application application, Closure body, boolean root = false) {
        this(application, null, body, null, null, null, null, root, "", null)
    }
    
    /**
     * Creates a new GspLayout using the specified gsp layout template, and then applies
     * the specified tag body to this template.
     * <p>
     * Note this is primarily used by tag libraries.
     * 
     * @param application Current vaadin application - required because we render immediately, not when attached.
     * @param gsp The URI of the GSP layout to use as a template
     * @param body The tag's body closure
     * @param params The params map to use when rendering the layout.
     * @param model The model map to use when rendering the layout.
     * @param flash The flash scope object to use when rendering the layout.
     * @param controllerName The controller name to use when rendering the GSP.
     */
    public GspLayout(Application application, String gsp, Closure body, Map params = null, Map model = null,
        Map flash = null, String controllerName = null) {
        this(application, gsp, body, params, model, flash, controllerName, false, "", null)
    }
    
    /**
     * Creates a new GspLayout from the existing one, but using the specified
     * Gsp template in place of the existing one's template.
     *
     * @param application Current vaadin application - required because we render immediately, not when attached.
     * @param other The existing GspLayout to use.
     * @param gsp The uri of the GSP view, template or resource to use
     */
    public GspLayout(Application application, GspLayout other, String gsp) {
        this(application, gsp, null, other.params, other.model, other.flash,
            other.controllerName, false, "", other.removeBodyComponents())
    }
    
    /**
     * Common constructor called by other constructors.
     * 
     * @param application Current vaadin application - required because we render immediately, not when attached.
     * @param gsp The URI of the GSP layout to use as a template
     * @param body The tag's body closure
     * @param params The params map to use when rendering the layout.
     * @param model The model map to use when rendering the layout.
     * @param flash The flash scope object to use when rendering the layout.
     * @param controllerName The controller name to use when rendering the GSP.
     * @param root True if this is the root Gsp of the page
     * @param text The initial text to use for template contents
     * @param components The initial set of components to use for template
     */
    protected GspLayout(Application application, String gsp, Closure body, Map params, Map model, Map flash,
        String controllerName, boolean root, String text, Map<String,Component> components) {
        super()
        this.vaadinApplication = application
        this.templateGsp = gsp
        this.body = body
        this.params = params
        this.model = model
        this.flash = flash
        this.controllerName = controllerName
        this.root = root
        this.templateContents = text ?: ""
        this.components = components ?: [:]
        this.components.each { loc,c -> addComponent(c,loc) }
        render(gsp,body,params,model,flash,controllerName)
    }
    
    /**
     * Renders the layout using the Gsp and/or tag body.
     */
    protected void render(String gsp, Closure body, Map params, Map model, Map flash, String controllerName) {
        // Timing logging
        def stopwatch = Stopwatch.enabled ? new Stopwatch(this.toString(), this.class) : null
        
        // Render
        renderGsp(gsp, params, model, flash, controllerName)
        renderBody(body)

        // Timing logging
        stopwatch?.stop()
    }
    
    /**
     * Initialises the superclass <a href="http://vaadin.com/api/com/vaadin/ui/CustomLayout.html">CustomLayout</a>
     * using the specified GSP as the template.
     * <p>
     * The GSP is retrieved using a {@link org.grails.plugin.vaadin.gsp.GspResourceLocator},
     * and rendered using a {@link org.grails.plugin.vaadin.gsp.GspResourcePageRenderer}.
     * <p>
     * Any Vaadin Components in the GSP are automatically added to this CustomLayout, in the
     * order in which they appear. 
     */
    protected void renderGsp(String gsp, Map params, Map model, Map flash, String controllerName) {
        // Give up if we don't have a Gsp uri
        if (!gsp) return
        
        if (log.isDebugEnabled()) {
            log.debug "GSP: ${this}"
        }
        
        // Get required classes
        def vaadinGspLocator = getVaadinApplication().getBean("vaadinGspLocator")
        def vaadinGspRenderer = getVaadinApplication().getBean("vaadinGspRenderer")
        if (! vaadinGspLocator || ! vaadinGspRenderer) {
            throw new Exception("GSP locator and renderer classes required, but not found!")
        }
        
        // Find GSP
        def gspDef = vaadinGspLocator.findGsp(gsp)
        if (! gspDef) {
            throw new Exception("GSP not found '${gsp}'")
        }
        
        // Execute GSP - we use its body text as this layout's template
        def templateBody = {
            return vaadinGspRenderer.render([
                (gspDef.type) : gspDef.uri, params: params, model: model,
                flash: flash, controller: controllerName,
                session: getVaadinApplication().context.httpSession, attributes:[vaadinApplication:getVaadinApplication()] ])
        }
        templateContents = evaluateGspContent(templateBody, components)
    }
    
    /**
     * Initialises the superclass <a href="http://vaadin.com/api/com/vaadin/ui/CustomLayout.html">CustomLayout</a>
     * using the specified tag body as the template.
     * <p>
     * Any Vaadin Components in the tag body are automatically added to this
     * CustomLayout, in the order in which they appear. 
     * 
     * @param body The closure to execute to obtain the GSP text (could be a tag body)
     */
    protected void renderBody(Closure body) {
        // Execute body - we use its body text as this layout's template
        if (body) {
            if (log.isDebugEnabled()) {
                log.debug "BODY: ${this}"
            }
            
            def bodyText = evaluateGspContent(body, bodyComponents)
            if (!templateContents) templateContents = bodyText
            components.putAll(bodyComponents)
        }
    }

    /**
     * Executes the body closure (could be a tag body) and returns the resulting text.
     * <p>
     * Importantly, any Vaadin Components generated from executing the body closure are
     * added to the specified Components map, stored by location name.
     *
     * @param body The closure to execute to obtain the GSP text and components
     * @param components The map to use to collect any components resulting from evaluating the body
     */
    protected CharSequence evaluateGspContent(Closure body, Map<String,Component> components) {
        // Execute body
        GspLayoutNode node = new GspLayoutNode(this, body, components)
        GspContext context = new GspContext(getVaadinApplication().context.session)
        
        // Evaluate body - discard text
        def text = context.evaluate(node, root)?.toString()
        
        // Set locations from cache
        components.each { location, component ->
            this.addComponent(component, location)
        }
        
        return text
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
        Map<String,Component> components
        
        public GspLayoutNode(Component component, Closure body, Map<String,Component> components) {
            super(component, body)
            this.components = components != null ? components : [:]
        }
        
        @Override
        public CharSequence attachComponent(Component component, Map params = null) {
            params = params ?: [:]

            // Set location
            params.location = params.location ?: "component_${components.size()}"
            
            // Store component in internal collection
            components[params.location] = component
            
            // Format params as wrapper attributes
            def attrs = params.findAll {k,v->k&&v}.collect { "${it.key}='${it.value}'" }.join(" ")
            
            // Return wrapper HTML
            return "<div ${attrs}></div>"
        }
    }
    
    /**
     * This component as a String - mainly useful for logging purposes.
     */
    String toString() {
        return "${templateGsp?:'{GSP BODY}'}"
    }
}
