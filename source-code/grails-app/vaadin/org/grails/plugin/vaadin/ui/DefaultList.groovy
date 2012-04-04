package org.grails.plugin.vaadin.ui

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.Notification;

/**
 * Mimics the Grails scaffolding list.gsp
 * <p>
 * Note that the layout can be customised by modifying the HTML
 * in VAADIN/themes/{theme}/layouts/list.html
 * <p>
 * You can also specify a different HTML layout to use, in the constructor
 * 
 * @author Francis McKenzie
 */
class DefaultList extends CustomLayout {
    String entityName
    
    /**
     * Create the layout using the default 'list.html' page
     * in web-app/VAADIN/themes/{theme}/layouts
     * 
     * @param entityName The entity name to use for labels (e.g. "Book")
     */
    public DefaultList(String entityName) {
        this(entityName, "list")
    }
    
    /**
     * Create the layout using the specified layout HTML page
     * in web-app/VAADIN/themes/{theme}/layouts
     * 
     * @param entityName The entity name to use for labels (e.g. "Book")
     * @param layoutName The name of the HTML page to use for the layout
     */
    public DefaultList(String entityName, String layoutName) {
        super(layoutName)
        
        // Save entity name
        this.entityName = entityName
        
        // Create UI
        buildLayout()
    }
    
    /**
    * Subclasses may override to customise layout
    */
   protected void buildLayout() {
        // Navigation
        this.navigation = new DefaultNavigation(entityName, ["home", "create"])
        
        // Heading
        this.heading = new Label(message(code:"default.list.label", args:[entityName]), Label.CONTENT_RAW)
    }

    /**
     * Triggers display of flash messages when this component has been
     * attached to a window
     */
    @Override
    public void attach() {
        super.attach()
        window.caption = message(code:"default.list.label", args:[entityName])
        showFlash()
    }
    
    /**
     * Manually trigger display of flash messages. Only works if this component
     * has already been attached to a window.
     */
    public void showFlash() {
        // Flash
        if (flash.message && this.window) {
            window.showNotification(flash.message, Notification.TYPE_WARNING_MESSAGE)
        }
    }

    /**
     * Get the heading label component
     */
    public Component getHeading() { getComponent("list.label") }
    /**
     * Set the heading label component
     */
    public void setHeading(Component heading) { addComponent(heading, "list.label") }
    /**
    * Get the navigation toolbar component
    */
    public Component getNavigation() { getComponent("navigation") }
    /**
    * Set the navigation toolbar component
    */
    public void setNavigation(Component navigation) { addComponent(navigation, "navigation") }
    /**
    * Get the body component (the form should be added to this)
    */
    public Component getBody() { getComponent("list.body") }
    /**
    * Set the body component (the form should be added to this)
    */
    public void setBody(Component body) { addComponent(body, "list.body") }
}
