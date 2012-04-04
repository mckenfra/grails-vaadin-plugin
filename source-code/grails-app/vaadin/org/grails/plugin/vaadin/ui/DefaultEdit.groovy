package org.grails.plugin.vaadin.ui

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.Notification;

/**
 * Mimics the Grails scaffolding edit.gsp
 * <p>
 * Note that the layout can be customised by modifying the HTML
 * in VAADIN/themes/{theme}/layouts/edit.html
 * <p>
 * You can also specify a different HTML layout to use, in the constructor
 * 
 * @author Francis McKenzie
 */
class DefaultEdit extends CustomLayout {
    String entityName
    Object instance

    /**
     * Create the layout using the default 'edit.html' page
     * in web-app/VAADIN/themes/{theme}/layouts
     * 
     * @param entityName The entity name to use for labels (e.g. "Book")
     * @param instance The instance of the domain class - e.g. used to get id for links
     */
    public DefaultEdit(String entityName, Object instance) {
        this(entityName, instance, "edit")
    }
    
    /**
     * Create the layout using the specified layout HTML page
     * in web-app/VAADIN/themes/{theme}/layouts
     * 
     * @param entityName The entity name to use for labels (e.g. "Book")
     * @param instance The instance of the domain class - e.g. used to get id for links
     * @param layoutName The name of the HTML page to use for the layout
     */
    public DefaultEdit(String entityName, Object instance, String layoutName) {
        super(layoutName)
        
        // Save entity name
        this.entityName = entityName
        this.instance = instance
        
        // Create UI
        buildLayout()
    }
    
    /**
     * Subclasses may override to customise layout
     */
    protected void buildLayout() {
        // Navigation
        this.navigation = new DefaultNavigation(entityName, ["home", "list", "create"])
        
        // Heading
        this.heading = new Label(message(code:"default.edit.label", args:[entityName]), Label.CONTENT_RAW)
        
        // Buttons
        this.buttons = new DefaultButtons(instance)
    }
    
    /**
     * Triggers display of flash messages when this component has been
     * attached to a window
     */
    @Override
    public void attach() {
        super.attach()
        window.caption = message(code:"default.edit.label", args:[entityName])
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
        
        // Errors
        if (instance?.hasErrors() && this.window) {
            def errMsg = new StringBuilder()
            instance.errors.allErrors.each { error ->
                def errValue = message(error:error)
                errMsg << """\
                    <li ${error in org.springframework.validation.FieldError ? 'data-field-id=' + error.field : ''}>${errValue}</li>
                """
            }
            errMsg = "<ul class='errors' role='alert'>${errMsg}</ul>"
            window.showNotification(errMsg, Notification.TYPE_ERROR_MESSAGE)
        }
    }
    
    /**
     * Get the heading label component
     */
    public Component getHeading() { getComponent("edit.label") }
    /**
     * Set the heading label component
     */
    public void setHeading(Component heading) { addComponent(heading, "edit.label") }
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
    public Component getBody() { getComponent("edit.body") }
    /**
    * Set the body component (the form should be added to this)
    */
    public void setBody(Component body) { addComponent(body, "edit.body") }
    /**
    * Get the buttons toolbar component
    */
    public Component getButtons() { getComponent("buttons") }
    /**
    * Set the buttons toolbar component
    */
    public void setButtons(Component buttons) { addComponent(buttons, "buttons") }
}
