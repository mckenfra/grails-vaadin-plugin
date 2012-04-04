package org.grails.plugin.vaadin.ui

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Window;

/**
 * Mimics the Grails main.gsp top-level template.
 * <p>
 * Note that the layout can be customised by modifying the HTML
 * in VAADIN/themes/{theme}/layouts/main.html
 * <p>
 * You can also specify a different HTML layout to use, in the constructor
 * 
 * @author Francis McKenzie
 */
class DefaultWindow extends Window {
    public CustomLayout mainLayout
    
    /**
     * Create the window using the default 'main.html' page
     * in web-app/VAADIN/themes/{theme}/layouts, and with the
     * default title 'Grails'
     */
    public DefaultWindow() {
        this("Grails")
    }
    
    /**
     * Create the window using the default 'main.html' page
     * in web-app/VAADIN/themes/{theme}/layouts, and with the specified
     * title.
     * 
     * @param caption The window title (caption)
     */
    public DefaultWindow(String caption) {
        this(caption, "main")
    }
    
    /**
     * Create the window using the specified HTML layout page
     * in web-app/VAADIN/themes/{theme}/layouts, and with the specified
     * title.
     * 
     * @param caption The window title (caption)
     * @param layoutName The name of the HTML page to use for the layout
     */
    public DefaultWindow(String caption, String layoutName) {
        super(caption)
        
        // Attach main container
        this.mainLayout = new CustomLayout(layoutName)
        this.addComponent(this.mainLayout)

        // Create UI
        buildLayout()
    }

    /**
     * Subclasses may override to customise layout
     */
    protected void buildLayout() {
        // Spacing
        this.content.setMargin(false)
        
        // Add logo
        this.header = new DefaultLogo()
    }

    /**
     * Get the banner header component (the logo should be added to this)
     */
    public Component getHeader() { mainLayout.getComponent("header") }
    /**
     * Set the banner header component (the logo should be added to this)
     */
    public void setHeader(Component header) { mainLayout.addComponent(header, "header") }
    /**
     * Get the body component (the list, show, edit, create, etc. view should be added to this)
     */
    public Component getBody() { mainLayout.getComponent("body") }
    /**
     * Set the body component (the list, show, edit, create, etc. view should be added to this)
     */
    public void setBody(Component body) { mainLayout.addComponent(body, "body") }
    /**
     * Get the footer component (currently empty in standard Grails 2.0.x scaffolding)
     */
    public Component getFooter() { mainLayout.getComponent("footer") }
    /**
     * Set the footer component (currently empty in standard Grails 2.0.x scaffolding)
     */
    public void setFooter(Component footer) { mainLayout.addComponent(footer, "footer") }
}
