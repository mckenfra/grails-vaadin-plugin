package org.grails.plugin.vaadin.ui

import com.vaadin.Application;
import com.vaadin.ui.Component;

import org.apache.commons.logging.LogFactory;
import org.grails.plugin.vaadin.gsp.GspEvaluator;
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
class GspLayout extends GspBodyLayout {
    def log = LogFactory.getLog(this.class)

    /**
     * The URI of the Gsp
     */
    String uri
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
     * The attributes for the request
     */
    Map attributes

    /**
     * Create an empty layout
     */
    public GspLayout() {}
    
    /**
     * Create a layout from a Gsp body
     */
    public GspLayout(Closure body) {
        super(body)
    }
    
    /**
     * Initialises this GspLayout from the other specified layout, except with a different template.
     * <p>
     * Note that this method will remove all body components from the specified layout and add them
     * to this one instead.
     * 
     * @param layout The existing layout to apply the new template to
     * @param uri The uri of the new template Gsp to apply
     */
    public GspLayout(GspBodyLayout other, String uri) {
        super(other)

        this.uri = uri
        if (other instanceof GspLayout) {
            this.params = other.params
            this.model = other.model
            this.flash = other.flash
            this.controllerName = other.controllerName
        }
    }
    
    /**
     * Set the Gsp to use as a template for this layout.
     * 
     * @param props The properties of the gsp, as follows:
     * 
     * @param uri The uri of the GSP view, template or resource.
     * @param params The params map to use when rendering the GSP.
     * @param model The model map to use when rendering the GSP.
     * @param flash The flash scope object to use when rendering the GSP.
     * @param controllerName The controller name to use when rendering the GSP.
     */
    public void setGsp(Map props) {
        if (!props) return
        
        if (props.containsKey('uri')) this.uri = props.uri
        if (props.containsKey('params')) this.params = props.params
        if (props.containsKey('model')) this.model = props.model
        if (props.containsKey('flash')) this.flash = props.flash
        if (props.containsKey('controllerName')) this.controllerName = props.controllerName
    }
    
    /**
     * Renders the GSP template and/or body
     */
    protected void render() {
        renderGsp(uri,params,model,flash,controllerName)
        super.render()
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
        
        // Timing logging
        def stopwatch = Stopwatch.enabled ? new Stopwatch(this.toString(), this.class) : null
        
        if (log.isDebugEnabled()) {
            log.debug "RENDER GSP: ${this}"
        }
        
        // Get required classes
        final vaadinApplication = requireVaadinApplication()
        def vaadinGspLocator = vaadinApplication.getBean("vaadinGspLocator")
        def vaadinGspRenderer = vaadinApplication.getBean("vaadinGspRenderer")
        if (! vaadinGspLocator || ! vaadinGspRenderer) {
            throw new Exception("GSP locator and renderer classes required, but not found!")
        }
        
        // Find GSP
        def gspDef = vaadinGspLocator.findGsp(gsp)
        if (! gspDef) {
            throw new Exception("GSP not found '${gsp}'")
        }
        
        // Execute GSP - we use its body text as this layout's template
        def attribs = attributes ?: [:]
        attribs.vaadinApplication = vaadinApplication
        def templateBody = {
            return vaadinGspRenderer.render([
                source: gspDef.source, params: params, model: model,
                flash: flash, controller: controllerName,
                session: vaadinApplication.context.httpSession, attributes:attribs ])
        }
        // Execute body - we use its body text as this layout's template
        def evaluator = new GspEvaluator(this, root)
        def result = evaluator.evaluate(templateBody)
        templateContents = result.text ?: ""
        
        // Timing logging
        stopwatch?.stop()
    }

    /**
     * This component as a String - mainly useful for logging purposes.
     */
    String toString() {
        return uri
    }
}
