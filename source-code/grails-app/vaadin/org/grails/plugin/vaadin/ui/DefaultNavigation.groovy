package org.grails.plugin.vaadin.ui

import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;

/**
 * A navigation toolbar that provides the ability to add the same buttons
 * that are provided in standard Grails scaffolding, i.e.:
 * 'home', 'create', 'list'
 * <p>
 * Note that users can subclass this class to add additional button types.
 * 
 * @author Francis McKenzie
 */
class DefaultNavigation extends HorizontalLayout {
    def entityName
    
    /**
     * Create an empty navigation toolbar
     * 
     * @param entityName The entity name to use for button captions (e.g. "Book")
     */
    public DefaultNavigation(String entityName) {
        this(entityName, null)
    }
    
    /**
     * Create a navigation toolbar with the specified list of buttons
     * 
     * @param entityName The entity name to use for button captions (e.g. "Book")
     * @param buttons The names of the buttons to add ('home', 'create', 'list')
     */
    public DefaultNavigation(String entityName, List<String> buttons) {
        super()
        this.entityName = entityName
        this.spacing = false
        this.addButtonsByName(buttons)
    }
    
    /**
     * Add buttons by name to the navigation toolbar.
     * 
     * @param buttons The names of the buttons to add ('home', 'create', 'list')
     */
    def addButtonsByName(List<String> buttons) {
        buttons?.each {
            switch (it) {
                case "home": addHomeButton(); break;
                case "create": addCreateButton(); break;
                case "list": addListButton(); break;
            }
        }
    }
    
    /**
     * Adds a 'home' button to the toolbar. This will link
     * to the 'home' controller by default.
     * 
     * @param args A map of args for customising the button
     * @arg controller Change the controller that the button links to
     * @arg action Change the action that the button links to
     * @arg icon Change the icon Resource used for the button
     * @return The added button
     */
    def addHomeButton(Map args) {
        def controller = args?.controller ?: "home"
        def action = args?.action ?: null
        def icon = args?.icon ?: new ThemeResource("images/skin/house.png")
        def button = new GrailsButton(message(code:"default.home.label"), [controller:controller, action:action])
        button.addStyleName("home")
        button.icon = icon
        this.addComponent(button)
        return button
    }
    
    /**
     * Adds a 'create' button to the toolbar. This will link
     * to the 'create' action by default.
     * 
     * @param args A map of args for customising the button
     * @arg controller Change the controller that the button links to
     * @arg action Change the action that the button links to
     * @arg icon Change the icon Resource used for the button
     * @return The added button
     */
    def addCreateButton(Map args) {
        def controller = args?.controller ?: null
        def action = args?.action ?: "create"
        def icon = args?.icon ?: new ThemeResource("images/skin/database_add.png")
        def button = new GrailsButton(message(code:"default.new.label", args:[entityName]), [controller:controller, action:action])
        button.addStyleName("create")
        button.icon = icon
        this.addComponent(button)
        return button
    }
    
    /**
     * Adds a 'list' button to the toolbar. This will link
     * to the 'list' action by default.
     * 
     * @param args A map of args for customising the button
     * @arg controller Change the controller that the button links to
     * @arg action Change the action that the button links to
     * @arg icon Change the icon Resource used for the button
     * @return The added button
     */
    def addListButton(Map args) {
        def controller = args?.controller ?: null
        def action = args?.action ?: "list"
        def icon = args?.icon ?: new ThemeResource("images/skin/database_table.png")
        def button = new GrailsButton(message(code:"default.list.label", args:[entityName]), [controller:controller, action:action])
        button.addStyleName("list")
        button.icon = icon
        this.addComponent(button)
        return button
    }
}
