package org.grails.plugin.vaadin.ui

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
     * True if this Gsp is the top-level Gsp, and not a Gsp rendered inside another.
     */
    protected boolean isRoot
    /**
     * The uri of the GSP. Only used for logging purposes.
     */
    String gspUri
    
    /**
     * Creates a new GspLayout with the specified GSP.
     * 
     * @param gsp The uri of the GSP view, template or resource.
     * @param params The params map to use when rendering the GSP.
     * @param model The model map to use when rendering the GSP.
     * @param flash The flash scope object to use when rendering the GSP.
     * @param controller The controller name to use when rendering the GSP.
     */
    public GspLayout(String gsp, Map params = null, Map model = null, Map flash = null, String controller = null) {
        super()
        this.gspUri = gsp
        this.isRoot = true
        
        // Timing logging
        def stopwatch = Stopwatch.enabled ? new Stopwatch(gsp, this.class) : null
        
        // Create GSP
        initTemplateContentsFromGsp(gsp, null, params, model, flash, controller)
        
        // Timing logging
        stopwatch?.stop()
    }
    
    /**
     * Creates a new GspLayout with the specified tag body.
     * <p>
     * Note this is primarily used by tag libraries.
     * 
     * @param body The tag's body closure
     */
    public GspLayout(Closure body) {
        super()
        this.gspUri = '[CLOSURE]'
        initTemplateContentsFromClosure(body)
    }
    
    /**
     * Creates a new GspLayout using the specified layout template, and then applies
     * the specified tag body to this template.
     * <p>
     * Note this is primarily used by tag libraries.
     * 
     * @param layout The URI of the GSP layout to use as a template
     * @param body The tag's body closure
     * @param params The params map to use when rendering the layout.
     * @param model The model map to use when rendering the layout.
     * @param flash The flash scope object to use when rendering the layout.
     * @param controller The controller name to use when rendering the GSP.
     */
    public GspLayout(String layout, Closure body, Map params = null, Map model = null, Map flash = null, String controller = null) {
        super()
        this.gspUri = layout
        initTemplateContentsFromGsp(layout, body, params, model, flash, controller)
    }
    
    /**
     * Initialises the superclass <a href="http://vaadin.com/api/com/vaadin/ui/CustomLayout.html">CustomLayout</a>
     * using the specified GSP as the template.
     * <p>
     * The GSP is retrieved using a {@link org.grails.plugin.vaadin.gsp.GspResourceLocator},
     * and rendered using a {@link org.grails.plugin.vaadin.gsp.GspResourcePageRenderer}.
     * <p>
     * Note that a tag body closure can be specified, if the GSP is being used as a template for another
     * GSP (whose contents is contained in the body closure).
     * <p>
     * Any Vaadin Components in the GSP or tag body are automatically added to this CustomLayout, in the
     * order in which they appear. 
     * 
     * @param gspUri The URI of the GSP view, template or resource
     * @param body The tag's body closure, to be evaluated after the GSP is rendered
     * @param params The params map to use when rendering the GSP.
     * @param model The model map to use when rendering the GSP.
     * @param flash The flash scope object to use when rendering the GSP.
     * @param controller The controller name to use when rendering the GSP.
     */
    protected void initTemplateContentsFromGsp(String gspUri, Closure body = null, Map params = null, Map model = null, Map flash = null, String controller = null) {
        // Get required classes
        def vaadinGspLocator = getBean("vaadinGspLocator")
        def vaadinGspRenderer = getBean("vaadinGspRenderer")
        if (! vaadinGspLocator || ! vaadinGspRenderer) {
            throw new Exception("GSP locator and renderer classes required, but not found!")
        }
        
        // Find GSP
        def gsp = vaadinGspLocator.findGsp(gspUri)
        if (! gsp) {
            throw new Exception("GSP not found '${gspUri}'")
        }
        
        // Create input stream using gsp
        def textBuilder = {
            def result = vaadinGspRenderer.render([ (gsp.type) : gsp.uri, params: params, model: model, flash: flash, controller: controller, session: application.context.httpSession ])
            if (body) { body() }
            return result
        }
        initTemplateContentsFromClosure(textBuilder)
    }
    
    /**
     * Initialises the superclass <a href="http://vaadin.com/api/com/vaadin/ui/CustomLayout.html">CustomLayout</a>
     * using the specified tag body as the template.
     * <p>
     * Note this is primarily an internal method used by tag libraries.
     * <p>
     * Any Vaadin Components in the tag body are automatically added to this CustomLayout, in the
     * order in which they appear. 
     * 
     * @param body The tag body closure containing the GSP text.
     */
    protected void initTemplateContentsFromClosure(Closure body) {
        // Execute body
        GspLayoutNode node = new GspLayoutNode(this, body)
        GspContext context = new GspContext(application.context.session)
        String text = context.evaluate(node, isRoot)
        
        // Initialise template from output
        InputStream inputStream = new ByteArrayInputStream(text.toString().getBytes("UTF-8"))
        initTemplateContentsFromInputStream(inputStream)
        
        // Set locations from cache
        node.components.each { k, v ->
            this.addComponent(v, k)
        }
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
        Map components = [:]
        
        public GspLayoutNode(Component component, Closure body) {
            super(component, body)
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
        return "[gsp: '${gspUri}']"
    }
}
