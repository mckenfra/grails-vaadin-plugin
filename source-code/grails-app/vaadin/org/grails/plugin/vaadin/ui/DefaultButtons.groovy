package org.grails.plugin.vaadin.ui

import com.vaadin.terminal.Resource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * A button toolbar that provides the ability to add the same buttons
 * that are provided in standard Grails scaffolding, i.e.:
 * 'create', 'update', 'delete', 'edit'
 * <p>
 * Note that users can subclass this class to add additional button types.
 * 
 * @author Francis McKenzie
 */
class DefaultButtons extends HorizontalLayout {
    def instance
    
    /**
     * Create the toolbar with the specified domain instance.
     * Note that the domain instance is used when creating the button
     * links - e.g. the "id" of the domain instance is used.
     * 
     * @param instance The instance of the domain class
     */
    public DefaultButtons(instance) {
        super()
        this.instance = instance
        this.spacing = false
    }
    
    /**
     * Adds a 'create' button to the toolbar. This will link
     * to the 'save' action of the current controller by default.
     * 
     * @param args A map of args for customising the button
     * @arg onClick Specify a closure that mimics the javascript 'onclick'. I.e. if it returns
     * false, we don't action the link.
     * @arg controller Change the controller that the button links to
     * @arg action Change the action that the button links to
     * @arg icon Change the icon Resource used for the button
     * @return The added button
     */
    def addCreateButton(Map args) {
        def caption = message(code:"default.button.create.label")
        def icon = args?.icon ?: new ThemeResource("images/skin/database_add.png")
        def onClick = args?.onClick
        def controller = args?.controller ?: null
        def action = args?.action ?: "save"
        def button = new GrailsButton(caption, [controller:controller, action:action, instance:instance], onClick)
        button.icon = icon
        addComponent(button)
        return button
    }
    
    /**
     * Adds a 'update' button to the toolbar. This will link
     * to the 'update' action of the current controller by default.
     * 
     * @param args A map of args for customising the button
     * @arg onClick Specify a closure that mimics the javascript 'onclick'. I.e. if it returns
     * false, we don't action the link.
     * @arg controller Change the controller that the button links to
     * @arg action Change the action that the button links to
     * @arg icon Change the icon Resource used for the button
     * @return The added button
     */
    def addUpdateButton(Map args) {
        def caption = message(code:"default.button.update.label")
        def icon = args?.icon ?: new ThemeResource("images/skin/database_save.png")
        def onClick = args?.onClick
        def controller = args?.controller ?: null
        def action = args?.action ?: "update"
        def button = new GrailsButton(caption, [controller:controller, action:action, instance:instance], onClick)
        button.icon = icon
        addComponent(button)
    }
    
    /**
     * Adds a 'delete' button to the toolbar. This will link
     * to the 'delete' action of the current controller by default.
     * Note that by default this also triggers a modal popup window requesting
     * the user to confirm, before deleting. If you don't want to use the popup
     * window, specify [noConfirm:true] in the args. 
     * 
     * @param args A map of args for customising the button
     * @arg noConfirm Prevent the modal confirmation popup window from being displayed
     * @arg onClick Specify a closure that mimics the javascript 'onclick'. I.e. if it returns
     * false, we don't action the link. Note this will only be called after the user has clicked
     * OK on the modal popup window. If the popup window has been disabled with 'noConfirm',
     * then this onClick closure is called immediately.
     * @arg controller Change the controller that the button links to
     * @arg action Change the action that the button links to
     * @arg icon Change the icon Resource used for the button
     * @return The added button
     */
    def addDeleteButton(Map args) {
        def caption = message(code:"default.button.delete.label")
        def icon = args?.icon ?: new ThemeResource("images/skin/database_delete.png")
        def onClick = args?.onClick
        def controller = args?.controller ?: null
        def action = args?.action ?: "delete"
        def noConfirm = args?.noConfirm
        if (! noConfirm) {
            def confirmationMsg = message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')
            def anotherOnClick = args?.onClick
            onClick = { dispatch->
                // Create the popup
                def popup = createConfirmPopup(confirmationMsg, {
                    if (anotherOnClick && anotherOnClick(dispatch) == false) {
                        // Cancelled by the other onClick method
                    } else {
                        // Call the dispatch callback if OK button clicked
                        dispatch()
                    }
                })
                
                // Stop the link from being followed immediately
                return false
            }
        }
        def button = new GrailsButton(caption, [controller:controller, action:action, id:instance?.id], onClick)
        button.icon = icon
        addComponent(button)
    }
    
    /**
     * Adds a 'edit' button to the toolbar. This will link
     * to the 'edit' action of the current controller by default.
     * 
     * @param args A map of args for customising the button
     * @arg onClick Specify a closure that mimics the javascript 'onclick'. I.e. if it returns
     * false, we don't action the link.
     * @arg controller Change the controller that the button links to
     * @arg action Change the action that the button links to
     * @arg icon Change the icon Resource used for the button
     * @return The added button
     */
    def addEditButton(Map args) {
        def caption = message(code:"default.button.edit.label")
        def icon = args?.icon ?: new ThemeResource("images/skin/database_edit.png")
        def onClick = args?.onClick
        def controller = args?.controller ?: null
        def action = args?.action ?: "edit"
        def button = new GrailsButton(caption, [controller:controller, action:action, id:instance?.id], onClick)
        button.icon = icon
        addComponent(button)
    }

    /**
     * Creates a closure that can be used to open a popup window
     * when a button is clicked.
     * 
     * @arg prompt The prompt for the popup window
     * @arg onOK The closure to run after the users clicks "OK"
     */
    protected createConfirmPopup(String prompt, Closure onOK = null) {
        // Create the popup
        final Window subwindow = new Window("Confirm");
        subwindow.addStyleName("confirm-popup")
        subwindow.resizable = false
        subwindow.modal = true

        // Configure the windws layout; by default a VerticalLayout
        VerticalLayout layout = (VerticalLayout) subwindow.content
        layout.setMargin(true)
        layout.spacing = true

        // Add some content; a label and a close-button
        Label message = new Label(prompt)
        message.setSizeUndefined()
        subwindow.addComponent(message)
        layout.setComponentAlignment(message, Alignment.TOP_CENTER)

        // The responses
        final CLOSE = {
            // close the window by removing it from the parent window
            subwindow.parent.removeWindow(subwindow);
        }
        final OK = {
            CLOSE()
            if (onOK) onOK()
        }

        // OK & Cancel Buttons
        HorizontalLayout buttons = new HorizontalLayout()
        buttons.spacing = true
        buttons.setMargin(false)
        Button okButton = new Button("OK", new Button.ClickListener() {
            public void buttonClick(ClickEvent event) {
                OK()
            }
        })
        buttons.addComponent(okButton)
        Button cancelButton = new Button("Cancel", new Button.ClickListener() {
            public void buttonClick(ClickEvent event) {
                CLOSE()
            }
        });
        buttons.addComponent(cancelButton)
        subwindow.addComponent(buttons)
        layout.setComponentAlignment(buttons, Alignment.BOTTOM_CENTER)
        
        // Ensure window not already open
        if (subwindow.getParent() == null) {
            // Open the subwindow by adding it to the parent window
            window.addWindow(subwindow);
        }
    }
}
