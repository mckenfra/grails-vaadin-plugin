package org.grails.plugin.vaadin.taglib

import grails.artefact.Artefact;
import groovy.lang.Closure;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import org.grails.plugin.vaadin.gsp.GspAttacher;
import org.grails.plugin.vaadin.gsp.GspComponentConfig;
import org.grails.plugin.vaadin.gsp.GspComponentNode;
import org.grails.plugin.vaadin.gsp.GspContext;
import org.grails.plugin.vaadin.gsp.MapDrivenFieldFactory;
import org.grails.plugin.vaadin.ui.GrailsButton;
import org.grails.plugin.vaadin.ui.GspLayout;

import com.vaadin.terminal.ErrorMessage;
import com.vaadin.terminal.Resource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.terminal.UserError;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Form;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Window.Notification;

/**
 * Vaadin tags prefixed with "v", for example &lt;v:label&gt;
 * <p>
 * Tags provided:
 * <ul>
 * <li>layout</li>
 * <li>location</li>
 * <li>horizontalLayout</li>
 * <li>verticalLayout</li>
 * <li>warning</li>
 * <li>error</li>
 * <li>table</li>
 * <li>column</li>
 * <li>form</li>
 * <li>field</li>
 * <li>label</li>
 * <li>link</li>
 * </ul>
 * Also provided are helper methods for creating Components, intended to be called directly from GSP code
 * rather than in a tag:
 * <ul>
 * <li>createHorizontalLayout</li>
 * <li>createVerticalLayout</li>
 * <li>createTable</li>
 * <li>createForm</li>
 * <li>createLabel</li>
 * <li>createLink</li>
 * </ul>
 * Finally, there are additional methods for special situations:
 * <ul>
 * <li>confirm</li>
 * <li>commit</li>
 * </ul> 
 * 
 * @author Francis McKenzie
 */
class VaadinTagLib implements ApplicationContextAware {
    static namespace = "v"
    static returnObjectForTags = [
        'confirm',
        'commit',
        'createForm',
        'createLabel',
        'createLink',
        'createHorizontalLayout',
        'createVerticalLayout',
        'createTable'
    ]
    
	def grailsApplication
	ApplicationContext applicationContext

    /**
     * Adds a {@link org.grails.plugin.vaadin.ui.GspLayout} component with specified layout name.
     * 
     * @attr name REQUIRED The name of the layout view
     */
    Closure layout = { attrs, body ->
        // Ensure have layout name
        if (!attrs.name) {
            throw new IllegalArgumentException("Tag <${namespace}:layout> must have 'name' attribute")
        }
        def layoutName = "/layouts/${attrs.name}"
        
        // Create component - automatically evaluates body
        def component = new GspLayout(layoutName, body)
        
        // Attach to parent (if any)
        attachComponent(component, attrs.location)
    }
    
    /**
     * Adds the tag contents to the specified location in the parent custom layout.
     * 
     * @attr name REQUIRED The name of the location in the parent custom layout
     */
    Closure location = { attrs, body ->
        // Ensure have location name
        if (!attrs.name) {
            throw new IllegalArgumentException("Tag <${namespace}:location> must have 'name' attribute")
        }
        
        // Create component - automatically evaluates body
        def component = new GspLayout(body)
        
        // Attach to parent (if any)
        attachComponent(component, attrs.name)
    }
    
    /**
     * Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/HorizontalLayout.html">HorizontalLayout</a> tag
     * <p>
     * Components to be added to the layout should be specified as nested elements.
     */
    Closure horizontalLayout = { attrs, body ->
        attachComponent(createHorizontalLayout(attrs, body), attrs.location)
    }

    /**
     * Returns a new Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/HorizontalLayout.html">HorizontalLayout</a>
     */
    Closure createHorizontalLayout = { attrs, body ->
        return applyAttrsAndBodyToComponent(attrs, body, new HorizontalLayout())
    }

    /**
     * Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/VerticalLayout.html">VerticalLayout</a> tag
     * <p>
     * Components to be added to the layout should be specified as nested elements.
     */
    Closure verticalLayout = { attrs, body ->
        attachComponent(createVerticalLayout(attrs, body), attrs.location)
    }
    
    /**
     * Returns a new Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/VerticalLayout.html">VerticalLayout</a>
     */
    Closure createVerticalLayout = { attrs, body ->
        return applyAttrsAndBodyToComponent(attrs, body, new VerticalLayout())
    }

    /**
     * Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/Window.Notification.html">Window Notification</a> with Warning type
     * <p>
     * The message should be specified in the body of the tag.
     */
    Closure warning = { attrs, body ->
        def msg = body()
        application.mainWindow.showNotification(msg?.toString(), Notification.TYPE_WARNING_MESSAGE)
    }

    /**
     * Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/Window.Notification.html">Window Notification</a> with Error type
     * <p>
     * The message should be specified in the body of the tag.
     */
    Closure error = { attrs, body ->
        def msg = body()
        application.mainWindow.showNotification(msg?.toString(), Notification.TYPE_ERROR_MESSAGE)
    }

    /**
     * Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/Table.html">Table</a> tag
     * <p>
     * Columns should be configured using nested &lt;v:column&gt; tags
     */
    Closure table = { attrs, body ->
        attachComponent(createTable(attrs, body), attrs.location)
    }
    
    /**
     * Returns a new Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/Table.html">Table</a>
     */
    Closure createTable = { attrs, body ->
        // Create component
        def component = new Table()
        
        // Apply component-specific attributes
        def pageLength = attrs?.remove('pageLength')
        if (pageLength) {
            if (isNumber(pageLength)) {
                component.pageLength = (int) pageLength
            } else {
                component.pageLength = Integer.parseInt(pageLength.toString())
            }
        }

        // Config parser
        def withConfig = { configs ->
            def visibleColumns = []
            def columnHeaders = []
            configs.each {
                if (it.type == "column") {
                    if (it.props.name && it.props.header) {
                        visibleColumns << it.props.name
                        columnHeaders << it.props.header
                        if (it.props.generator) {
                            def generatorClosure = it.props.generator 
                            def generator = generatorClosure
                            if (generator instanceof Closure) {
                                generator = new Table.ColumnGenerator() {
                                    public Component generateCell(Table source, Object itemId, Object columnId) {
                                        def item = source.getItem(itemId)
                                        return generatorClosure(item)
                                    }
                                }
                            }
                            component.addGeneratedColumn(it.props.name, generator)
                        }
                    }
                }
            }
            component.visibleColumns = visibleColumns
            component.columnHeaders = columnHeaders
        }
        
        // Apply remaining attributes & body to component
        return applyAttrsAndBodyToComponent(attrs, body, component, null, withConfig)
    }

    /**
     * Configuration tag for describing a column in a Vaadin
     * <a href="http://vaadin.com/api/com/vaadin/ui/Table.html">Table</a>
     * <p>
     * The column header should be specified as the body of the tag.
     * <p>
     * A column generator can be specified using the 'generator' attribute,
     * which expects a Closure accepting an
     * <a href="http://vaadin.com/api/com/vaadin/data/Item.html">Item</a> parameter.
     * The item is the underlying data of a row in the table.
     */
    Closure column = { attrs, body ->
        // Ensure have a name
        if (!attrs.name) {
            throw new IllegalArgumentException("Tag <${namespace}:column> must have 'name' attribute")
        }

        attachConfig(attrs, body, Table.class, "column", "header")
    }

    /**
     * Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/Form.html">Form</a> tag
     * <p>
     * Fields should be configured using nested &lt;v:form&gt; tags
     */
    Closure form = { attrs, body ->
        attachComponent(createForm(attrs, body), attrs.location)
    }
    
    /**
     * Returns a new Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/Form.html">Form</a>
     */
    Closure createForm = { attrs, body ->
        // Create component
        def component = new Form()
        
        // Apply component-specific attributes
        
        // Remove data source first
        // Note that you have to set the form's itemDatasource attribute:
        //   - AFTER you've set the formFieldFactory attribute, and
        //   - BEFORE you set the visibleItemProperties
        // Otherwise, strange things happen.
        def dataSource = attrs.remove("itemDataSource")

        // Config parser
        def withConfig = { configs ->
            def fields = []
            def fieldProps = [:]
            configs.each {
                if (it.type == "field") {
                    def name = it.props.remove("name")
                    if (name) {
                        fields << name
                        fieldProps[name] = it.props
                    }
                }
            }
            component.formFieldFactory = new MapDrivenFieldFactory(fieldProps)
            if (dataSource) {
                component.itemDatasource = dataSource
            }
            component.visibleItemProperties = fields
        }
        
        // Apply remaining attributes & body to component
        return applyAttrsAndBodyToComponent(attrs, body, component, null, withConfig)
    }

    /**
     * Configuration tag for describing a field in a Vaadin
     * <a href="http://vaadin.com/api/com/vaadin/ui/Form.html">Form</a>
     * <p>
     * The field label should be specified as the body of the tag.
     */
    Closure field = { attrs, body ->
        // Ensure have a name
        if (!attrs.name) {
            throw new IllegalArgumentException("Tag <${namespace}:field> must have 'name' attribute")
        }
        
        // Clean up some attributes
        def error = attrs.componentError
        if (error && ! (error instanceof ErrorMessage) ) {
            attrs.componentError = new UserError(error.toString())
        }
        
        // Attach
        attachConfig(attrs, body, Form.class, "field", "caption")
    }

    /**
     * Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/Label.html">Label</a> tag
     * <p>
     * The label message should be specified in the tag body.
     */
    Closure label = { attrs, body ->
        attachComponent(createLabel(attrs, body), attrs.location)
    }
    
    /**
     * Returns a new Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/Label.html">Label</a>
     */
    Closure createLabel = { attrs, body ->
        // Create component
        def component = new Label()
        
        // Apply component-specific attributes
        def contentMode = attrs?.remove('contentMode')
        if (contentMode) { component.contentMode = toLabelContentMode(contentMode) }

        // Apply remaining attributes & body to component
        return applyAttrsAndBodyToComponent(attrs, body, component, "value")
    }

    /**
     * Custom {@link org.grails.plugin.vaadin.ui.GrailsButton} tag
     * <p>
     * The button caption should be specified in the tag body.
     * 
     * @attr icon The icon as a resource or path
     * @attr onclick An closure expecting a 'dispatch' argument, used to manually trigger
     * the link. Alternatively, simply return true from the closure to follow the link.
     */
    Closure link = { attrs, body ->
        attachComponent(createLink(attrs, body), attrs.location)
    }
    
    /**
     * Returns a new custom {@link org.grails.plugin.vaadin.ui.GrailsButton}
     */
    Closure createLink = { attrs, body ->
        // Create component
        def component = new GrailsButton()
        
        // Apply component-specific attributes
        def icon = attrs?.remove('icon')
        if (icon) {
            if (icon instanceof Resource) {
                component.icon = icon
            } else {
                component.icon = new ThemeResource(icon.toString())
            }
        }
        def onclick = attrs?.remove('onclick')
        if (onclick) {
            if (onclick instanceof Closure) {
                component.onclick = onclick
            } else {
                throw new Exception("Argument 'onclick' for tag <${namespace}:link> must be of type Closure")
            }
        }

        // Apply remaining attributes & body to component
        return applyAttrsAndBodyToComponent(attrs, body, component, "caption")
    }
    
    /**
     * Helper method that returns a closure that can be used in the
     * 'onclick' attribute of a {@link org.grails.plugin.vaadin.ui.GrailsButton}
     * <p>
     * Used to trigger a confirm yes/no popup window, e.g. before deleting a domain object.
     * 
     * @attr message REQUIRED The confirmation message
     */
    Closure confirm = { attrs ->
        // Get message
        String message = attrs.remove('message')?.toString()
        if (!message) {
            throw new IllegalArgumentException("Method ${namespace}.confirm() must have 'message' attribute")
        }
        return {dispatch->
            createConfirmPopup(application.mainWindow, message, {
                dispatch()
            })
            return false
        }
    }
    
    /**
     * Helper method that returns a closure that can be used in the
     * 'onclick' attribute of a {@link org.grails.plugin.vaadin.ui.GrailsButton}
     * <p>
     * Commits the specified form object.
     * 
     * @attr form REQUIRED The form to commit
     */
    Closure commit = { attrs ->
        // Get form
        def form = attrs.remove('form')
        if (form && !(form instanceof Form) ) {
            form = this.pageScope."${form}"
        }
        if (!form) {
            throw new IllegalArgumentException("Method ${namespace}.commit() must have 'form' attribute")
        }
        return {dispatch->
            form.commit()
        }
    }
    
    protected Component applyAttrsAndBodyToComponent(attrs, body, component, bodyProperty = null, Closure withConfig = null) {
        // Evaluate attributes
        evaluateAttributes(component, attrs)
        
        // Only collect body if no attribute
        def withBody = !bodyProperty || !attrs || attrs.containsKey(bodyProperty) ? null : { text->
             component."${bodyProperty}" = text
        }
        
        // Evaluate body
        evaluateBody(component, body, withBody, withConfig)
        
        // Return component as a convenience
        return component
    }
    
    protected Component evaluateAttributes(component, attrs) {
        // Set properties
        setComponentProperties(component, attrs)
        
        return component
    }
    
    protected Component evaluateBody(component, body, Closure withBody = null, Closure withConfig = null) {
        if (body) {
            GspComponentNode node = new GspComponentNode(component, body, withBody, withConfig)
            GspContext context = new GspContext(session)
            context.evaluate(node)
        }
        return component
    }
    
    protected void attachComponent(Component component, String location = null) {
        GspContext context = new GspContext(session)
        GspAttacher attacher = new GspAttacher(context, out)
        attacher.attachComponent(component, location)
    }
    
    protected void attachConfig(attrs, body, Class componentClass, String type, bodyProperty = null) {
        Map props = [:]
        props.putAll(attrs)
        if (body && bodyProperty && !props.containsKey(bodyProperty)) {
            props[bodyProperty] = body()
        }
        GspComponentConfig config = new GspComponentConfig(componentClass, type, props)
        GspContext context = new GspContext(session)
        GspAttacher attacher = new GspAttacher(context, out)
        attacher.attachConfig(config)
    }

    protected void setComponentProperties(Component component, Map attrs) {
        attrs.each { k,v ->
            def name = k.toLowerCase()
            switch (name) {
                case "var": set(var:v, value:component); break;
                case "class": v.split(" ").each { component.addStyleName(it) }; break;
                case "style": throw new IllegalArgumentException("CSS 'style' is not supported by Vaadin components"); break;
                case "sizeundefined": component.setSizeUndefined(); break;
                case "sizefull": component.setSizeFull(); break;
                case "role": break; /* IGNORE */
                case "location": break; /* IGNORE */
                default: component."${k}" = (v == "false" ? false : (v == "true" ? true : v))
            }
        }
    }
    
    protected int toLabelContentMode(contentMode) {
        int n = Label.CONTENT_DEFAULT
        if (contentMode != null) {
            if (isNumber(contentMode)) {
                n = contentMode
            } else {
                String name = contentMode.toString().toLowerCase()
                switch(name) {
                    case "preformatted": n = Label.CONTENT_PREFORMATTED; break;
                    case "raw": n = Label.CONTENT_RAW; break;
                    case "text": n = Label.CONTENT_TEXT; break;
                    case "xhtml": n = Label.CONTENT_XHTML; break;
                    case "xml": n = Label.CONTENT_XML; break;
                    case "default": n = Label.CONTENT_DEFAULT; break;
                    default: throw new IllegalArgumentException("Unrecognised content mode '${contentMode}'")
                }
            }
        }
        return n
    }
    
    protected boolean isNumber(it) {
        return it != null && (
            it instanceof Number ||
            it.class.isAssignableFrom(int.class) ||
            it.class.isAssignableFrom(long.class) ||
            it.class.isAssignableFrom(float.class)
        );
    }
    
    /**
    * Creates a closure that can be used to open a popup window
    * when a button is clicked.
    *
    * @arg prompt The prompt for the popup window
    * @arg onOK The closure to run after the users clicks "OK"
    */
   protected createConfirmPopup(Window window, String prompt, Closure onOK = null) {
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