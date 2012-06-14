package org.grails.plugin.vaadin.taglib

import org.springframework.web.servlet.support.RequestContextUtils as RCU

import org.grails.plugin.vaadin.data.DomainItem;
import org.grails.plugin.vaadin.gsp.ColumnDef;
import org.grails.plugin.vaadin.gsp.DefaultColumnDef;
import org.grails.plugin.vaadin.gsp.DefaultFieldDef;
import org.grails.plugin.vaadin.gsp.FieldDef;
import org.grails.plugin.vaadin.gsp.FieldDefDrivenFieldFactory;
import org.grails.plugin.vaadin.gsp.GspAttacher;
import org.grails.plugin.vaadin.gsp.GspComponentConfig;
import org.grails.plugin.vaadin.gsp.GspComponentNode;
import org.grails.plugin.vaadin.gsp.GspContext;
import org.grails.plugin.vaadin.ui.DefaultCustomField;
import org.grails.plugin.vaadin.ui.DefaultUploadField;
import org.grails.plugin.vaadin.ui.GrailsIncludeLayout;
import org.grails.plugin.vaadin.ui.GspBodyLayout;
import org.grails.plugin.vaadin.ui.GspTemplateLayout;
import org.grails.plugin.vaadin.ui.GrailsButton;
import org.grails.plugin.vaadin.utils.ByteArrayPropertyConverter;
import org.grails.plugin.vaadin.utils.CalendarPropertyConverter;
import org.grails.plugin.vaadin.utils.DefaultValuePropertyConverter;
import org.grails.plugin.vaadin.utils.DomainProxyPropertyConverter;
import org.grails.plugin.vaadin.utils.Utils;

import com.vaadin.data.util.BeanItem;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.Accordion;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.DateField;
import com.vaadin.ui.Field;
import com.vaadin.ui.Form;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.Select;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Window.Notification;

import org.vaadin.addon.customfield.CustomField;
import org.vaadin.easyuploads.UploadField;
import org.vaadin.easyuploads.UploadField.FieldType;
import org.vaadin.easyuploads.UploadField.StorageMode;

/**
 * Vaadin tags prefixed with "v", for example &lt;v:label&gt;
 * <p>
 * Tags provided:
 * <ul>
 * <li>layout</li>
 * <li>location</li>
 * <li>include</li>
 * <li>accordion</li>
 * <li>tabs</li>
 * <li>tab</li>
 * <li>horizontalLayout</li>
 * <li>verticalLayout</li>
 * <li>warning</li>
 * <li>error</li>
 * <li>table</li>
 * <li>column</li>
 * <li>form</li>
 * <li>field</li>
 * <li>checkBox</li>
 * <li>comboBox</li>
 * <li>date</li>
 * <li>listSelect</li>
 * <li>optionGroup</li>
 * <li>password</li>
 * <li>select</li>
 * <li>text</li>
 * <li>textArea</li>
 * <li>timeZoneSelect</li>
 * <li>localeSelect</li>
 * <li>currencySelect</li>
 * <li>file</li>
 * <li>customField</li>
 * <li>label</li>
 * <li>link</li>
 * </ul>
 * Also provided are helper methods for creating Components, intended to be called directly from GSP code
 * rather than in a tag:
 * <ul>
 * <li>createAccordion</li>
 * <li>createInclude</li>
 * <li>createLayout</li>
 * <li>createLocation</li>
 * <li>createHorizontalLayout</li>
 * <li>createVerticalLayout</li>
 * <li>createTable</li>
 * <li>createTabs</li>
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
class VaadinTagLib {
    static namespace = "v"
    static returnObjectForTags = [
        'confirm',
        'commit',
        'createAccordion',
        'createField',
        'createForm',
        'createHorizontalLayout',
        'createInclude',
        'createLabel',
        'createLayout',
        'createLink',
        'createLocation',
        'createTable',
        'createTabs',
        'createVerticalLayout'
    ]

    /**
     * Injected, provides methods for timeZoneSelect, localeSelect, currencySelect
     */
    def vaadinTagDataService

    /**
     * Adds a {@link org.grails.plugin.vaadin.ui.GspTemplateLayout} component with
     * specified layout name.
     * 
     * @attr name REQUIRED The name of the layout view
     */
    Closure layout = { attrs, body ->
        def attachAttrs = removeAttachAttrs(attrs)
        attachComponent(createLayout(attrs, body), attachAttrs)
    }
    
    /**
     * Returns a new {@link org.grails.plugin.vaadin.ui.GspTemplateLayout} using the
     * layout and body
     * 
     * @attr name REQUIRED The name of the layout view
     */
    Closure createLayout = { attrs, body ->
        attrs.body = body
        return applyAttrsAndBodyToComponent(attrs, null, new GspTemplateLayout(), layoutConfigurer)
    }
    
    /**
     * Adds the tag contents to the specified location in the parent custom layout.
     * 
     * @attr name REQUIRED The name of the location in the parent custom layout
     */
    Closure location = { attrs, body ->
        // Ensure have location name
        attrs.location = Utils.removeCaseInsensitive(attrs, 'name') ?: Utils.removeCaseInsensitive(attrs, 'location')
        if (!attrs.location) {
            throw new IllegalArgumentException("Tag <${namespace}:location> must have 'name' attribute")
        }

        def attachAttrs = removeAttachAttrs(attrs)
        attachComponent(createLocation(attrs, body), attachAttrs)
    }

    /**
     * Returns a new {@link org.grails.plugin.vaadin.ui.GspBodyLayout} using the body
     * 
     * @attr name REQUIRED The name of the layout view
     */
    Closure createLocation = { attrs, body ->
        return applyAttrsAndBodyToComponent(attrs, null, new GspBodyLayout(body))
    }
    
    /**
     * Adds a {@link org.grails.plugin.vaadin.ui.GrailsIncludeLayout} component.
     */
    Closure include = { attrs, body ->
        def attachAttrs = removeAttachAttrs(attrs)
        attachComponent(createInclude(attrs, body), attachAttrs)
    }
    
    /**
     * Returns a new {@link org.grails.plugin.vaadin.ui.GrailsIncludeLayout}
     */
    Closure createInclude = { attrs, body ->
        return applyAttrsAndBodyToComponent(attrs, null, new GrailsIncludeLayout(), includeConfigurer)
    }
    
    /**
     * Vaadin {@link com.vaadin.ui.HorizontalLayout} tag
     * <p>
     * Components to be added to the layout should be specified as nested elements.
     */
    Closure horizontalLayout = { attrs, body ->
        def attachAttrs = removeAttachAttrs(attrs)
        attachComponent(createHorizontalLayout(attrs, body), attachAttrs)
    }

    /**
     * Returns a new Vaadin {@link com.vaadin.ui.HorizontalLayout}
     */
    Closure createHorizontalLayout = { attrs, body ->
        return applyAttrsAndBodyToComponent(attrs, body, new HorizontalLayout())
    }

    /**
     * Vaadin {@link com.vaadin.ui.VerticalLayout} tag
     * <p>
     * Components to be added to the layout should be specified as nested elements.
     */
    Closure verticalLayout = { attrs, body ->
        def attachAttrs = removeAttachAttrs(attrs)
        attachComponent(createVerticalLayout(attrs, body), attachAttrs)
    }
    
    /**
     * Returns a new Vaadin {@link com.vaadin.ui.VerticalLayout}
     */
    Closure createVerticalLayout = { attrs, body ->
        return applyAttrsAndBodyToComponent(attrs, body, new VerticalLayout())
    }

    /**
     * Vaadin {@link com.vaadin.ui.TabSheet} tag
     * <p>
     * Tabs to be added to the TabSheet should be specified as nested &lt;v:tab&gt; elements.
     */
    Closure tabs = { attrs, body ->
        def attachAttrs = removeAttachAttrs(attrs)
        attachComponent(createTabs(attrs, body), attachAttrs)
    }
 
    /**
     * Returns a new Vaadin {@link com.vaadin.ui.TabSheet}
     */
    Closure createTabs = { attrs, body ->
        return applyAttrsAndBodyToComponent(attrs, body, new TabSheet(), tabsConfigurer, null, "tabs")
    }

    /**
     * Configuration tag for describing a tab in a Vaadin
     * {@link com.vaadin.ui.TabSheet}
     * <p>
     * The tab contents should be specified in the body of the tag.
     */
    Closure tab = { attrs, body ->
        attrs.body = new GspBodyLayout(body)
        attachConfig(attrs, null, TabSheet.class, "tab")
    }

    /**
     * Vaadin {@link com.vaadin.ui.Accordion} tag
     * <p>
     * Tabs to be added to the Accordion should be specified as nested &lt;v:tab&gt; elements.
     */
    Closure accordion = { attrs, body ->
        def attachAttrs = removeAttachAttrs(attrs)
        attachComponent(createAccordion(attrs, body), attachAttrs)
    }
 
    /**
     * Returns a new Vaadin {@link com.vaadin.ui.Accordion}
     */
    Closure createAccordion = { attrs, body ->
        return applyAttrsAndBodyToComponent(attrs, body, new Accordion(), tabsConfigurer, null, "tabs")
    }
 
    /**
     * Vaadin {@link com.vaadin.ui.Window.Notification} with Warning type
     * <p>
     * The message should be specified in the body of the tag.
     */
    Closure warning = { attrs, body ->
        def msg = body()
        request.vaadinApplication.mainWindow.showNotification(msg?.toString(), Notification.TYPE_WARNING_MESSAGE)
    }

    /**
     * Vaadin {@link com.vaadin.ui.Window.Notification} with Error type
     * <p>
     * The message should be specified in the body of the tag.
     */
    Closure error = { attrs, body ->
        def msg = body()
        request.vaadinApplication.mainWindow.showNotification(msg?.toString(), Notification.TYPE_ERROR_MESSAGE)
    }

    /**
     * Vaadin {@link com.vaadin.ui.Table} tag
     * <p>
     * Columns should be configured using nested &lt;v:column&gt; tags
     */
    Closure table = { attrs, body ->
        def attachAttrs = removeAttachAttrs(attrs)
        attachComponent(createTable(attrs, body), attachAttrs)
    }
    
    /**
     * Returns a new Vaadin {@link com.vaadin.ui.Table}
     */
    Closure createTable = { attrs, body ->
        return applyAttrsAndBodyToComponent(attrs, body, new Table(), tableConfigurer, null, "columns")
    }
    
    /**
     * Configuration tag for describing a column in a Vaadin
     * {@link com.vaadin.ui.Table}
     * <p>
     * The column header should be specified as the body of the tag.
     * <p>
     * A column generator can be specified using the 'generator' attribute,
     * which expects a Closure accepting an
     * {@link com.vaadin.data.Item} parameter.
     * The item is the underlying data of a row in the table.
     */
    Closure column = { attrs, body ->
        attachConfig(attrs, body, Table.class, "column", "header")
    }

    /**
     * Vaadin {@link com.vaadin.ui.Form} tag
     * <p>
     * Fields should be configured using nested &lt;v:form&gt; tags
     */
    Closure form = { attrs, body ->
        def attachAttrs = removeAttachAttrs(attrs)
        attachComponent(createForm(attrs, body), attachAttrs)
    }
    
    /**
     * Returns a new Vaadin {@link com.vaadin.ui.Form}
     */
    Closure createForm = { attrs, body ->
        return applyAttrsAndBodyToComponent(attrs, body, new Form(), formConfigurer, null, "fields")
    }
    
    /**
     * Configuration tag for describing a field in a Vaadin
     * {@link com.vaadin.ui.Form}
     * <p>
     * The field caption should be specified as the body of the tag.
     */
    Closure field = { attrs, body ->
        fieldTag(attrs, body)
    }

    /**
     * Handles processing of all field-type tags.
     */
    protected void fieldTag(attrs, body) {
        if (lookupParentComponent() instanceof Form) {
            attachConfig(attrs, body, Form.class, "field", "caption")
        } else {
            def attachAttrs = removeAttachAttrs(attrs)
            attachComponent(createField(attrs, body), attachAttrs)
        }
    }
        
    /**
     * Returns a new Vaadin {@link com.vaadin.ui.Field}
     * of the type specified in the attrs.
     */
    Closure createField = { attrs, body ->
        def fieldDef = Converter.toFieldDef(attrs, toConfigurer)
        return applyAttrsAndBodyToComponent(attrs, body, fieldDef.type?.newInstance(), fieldDef.configurer, "caption")
    }
    
    /**
     * {@link com.vaadin.ui.CheckBox} field
     */
    Closure checkBox = { attrs, body ->
        attrs.type = 'checkBox'
        fieldTag(attrs, body)
    }
    
    /**
     * {@link com.vaadin.ui.CheckBox} field
     */
    Closure checkbox = checkBox
    
    /**
     * {@link com.vaadin.ui.ComboBox} field
     */
    Closure comboBox = { attrs, body ->
        attrs.type = 'comboBox'
        fieldTag(attrs, body)
    }
    
    /**
     * {@link com.vaadin.ui.ComboBox} field
     */
    Closure combobox = comboBox
    
    /**
     * {@link com.vaadin.ui.DateField} field
     */
    Closure date = { attrs, body ->
        attrs.type = 'dateField'
        fieldTag(attrs, body)
    }
    
    /**
     * {@link com.vaadin.ui.ListSelect} field
     */
    Closure listSelect = { attrs, body ->
        attrs.type = 'listSelect'
        fieldTag(attrs, body)
    }
    
    /**
     * {@link com.vaadin.ui.ListSelect} field
     */
    Closure listselect = listSelect
    
    /**
     * {@link com.vaadin.ui.OptionGroup} field
     */
    Closure optionGroup = { attrs, body ->
        attrs.type = 'optionGroup'
        fieldTag(attrs, body)
    }
    
    /**
     * {@link com.vaadin.ui.OptionGroup} field
     */
    Closure optiongroup = optionGroup
    
    /**
     * {@link com.vaadin.ui.PasswordField} field
     */
    Closure password = { attrs, body ->
        attrs.type = 'password'
        fieldTag(attrs, body)
    }
    
    /**
     * {@link com.vaadin.ui.Select} field
     */
    Closure select = { attrs, body ->
        attrs.type = 'select'
        fieldTag(attrs, body)
    }
    
    /**
     * {@link com.vaadin.ui.TextField} field
     */
    Closure text = { attrs, body ->
        attrs.type = 'text'
        fieldTag(attrs, body)
    }

    /**
     * {@link com.vaadin.ui.TextArea} field
     */
    Closure textArea = { attrs, body ->
        attrs.type = 'textArea'
        fieldTag(attrs, body)
    }

    /**
     * {@link com.vaadin.ui.TextArea} field
     */
    Closure textarea = textArea

    /**
     * TimeZone-populated
     * {@link com.vaadin.ui.ComboBox} field
     */
    Closure timeZoneSelect = { attrs, body ->
        attrs.type = Utils.removeCaseInsensitive(attrs, 'type') ?: 'comboBox'
        attrs.configurer = timeZoneSelectConfigurer
        fieldTag(attrs, body)
    }
    
    /**
     * TimeZone-populated
     * {@link com.vaadin.ui.ComboBox} field
     */
    Closure timezoneSelect = timeZoneSelect
    
    /**
     * Locale-populated
     * {@link com.vaadin.ui.ComboBox} field
     */
    Closure localeSelect = { attrs, body ->
        attrs.type = Utils.removeCaseInsensitive(attrs, 'type') ?: 'comboBox'
        attrs.configurer = localeSelectConfigurer
        fieldTag(attrs, body)
    }
    
    /**
     * Currency-populated
     * {@link com.vaadin.ui.ComboBox} field
     */
    Closure currencySelect = { attrs, body ->
        attrs.type = Utils.removeCaseInsensitive(attrs, 'type') ?: 'comboBox'
        attrs.configurer = currencySelectConfigurer
        fieldTag(attrs, body)
    }
    
    /**
     * <a href="https://vaadin.com/directory#addon/customfield">CustomField</a>
     */
    Closure customField = { attrs, body ->
        attrs.type = 'customField'
        attrs.compositionRoot = new GspBodyLayout(body)
        attachConfig(attrs, null, Form.class, "field")
    }
    
    /**
     * <a href="https://vaadin.com/directory#addon/customfield">CustomField</a>
     */
    Closure customfield = customField

    /**
     * Vaadin {@link com.vaadin.ui.Label} tag
     * <p>
     * The label message should be specified in the tag body.
     */
    Closure label = { attrs, body ->
        def attachAttrs = removeAttachAttrs(attrs)
        attachComponent(createLabel(attrs, body), attachAttrs)
    }
    
    /**
     * Returns a new Vaadin {@link com.vaadin.ui.Label}
     */
    Closure createLabel = { attrs, body ->
        return applyAttrsAndBodyToComponent(attrs, body, new Label(), labelConfigurer, "value")
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
        def attachAttrs = removeAttachAttrs(attrs)
        attachComponent(createLink(attrs, body), attachAttrs)
    }
    
    /**
     * Returns a new custom {@link org.grails.plugin.vaadin.ui.GrailsButton}
     */
    Closure createLink = { attrs, body ->
        return applyAttrsAndBodyToComponent(attrs, body, new GrailsButton(), linkConfigurer, "caption")
    }
    
    /**
     * {@link org.grails.plugin.vaadin.ui.DefaultUploadField}
     */
    Closure file = { attrs, body ->
        attrs.type = 'file'
        fieldTag(attrs, body)
    }
    
    /**
     * Configures the Vaadin application's main
     * {@link com.vaadin.ui.Window}
     */
    Closure mainWindow = { attrs, body ->
        windowConfigurer(attrs, request.vaadinApplication.mainWindow)
    }
    
    /**
     * Evaluates the tag body, which may return text and also may return configs from
     * nested config tags.
     * <p>
     * If 'bodyProperty' is specified, the body text is added to the 'attrs' map with that as the key.
     * <p>
     * If 'configProperty' is specified, the configs are added to the 'attrs' map with that as the key.
     * <p>
     * Finally, the propertiesSetter Closure (or a default) is used to apply the 'attrs' map entries
     * to the component.
     * 
     * @param attrs The tag attributes to apply to the component
     * @param body The tag body to evaluate, and add the results to the attrs map
     * @param bodyProperty The key that will be used for storing the bodyText in the attrs map
     * @param configProperty The key that will be used for storing the configs in the attrs map
     * @param configurer The closure that will apply the resulting attrs map to the component
     * @param component The component to which the attrs should be applied
     * 
     * @return The same component, returned as a convenience
     */
    protected Component applyAttrsAndBodyToComponent(attrs, body, component,
        Closure configurer = null, String bodyProperty = null, String configProperty = null) {

        // Only collect body if no overriding attribute
        def withBody
        if (bodyProperty && !attrs.containsKey(bodyProperty)) {
            withBody = { text-> attrs[bodyProperty] = text }
        }
        
        // Only collect configs if no overriding attribute
        def withConfig
        if (configProperty && !attrs.containsKey(configProperty)) {
            withConfig = { configs-> attrs[configProperty] = configs }
        }
        
        // Evaluate body
        if (body) {
            GspComponentNode node = new GspComponentNode(component, body, withBody, withConfig)
            GspContext context = new GspContext()
            context.evaluate(node)
        }
        
        // Set configurer
        configurer = configurer ?: defaultConfigurer
        configurer(attrs, component)
        
        // Return component as a convenience
        return component
    }
    
    /**
     * Attaches the component to the parent component in the current GspContext
     * 
     * @param component The component to attach
     * @param attrs Any additional attributes to pass to the attacher. E.g. may contain 'location'
     */
    protected void attachComponent(Component component, Map attrs = null) {
        GspContext context = new GspContext()
        GspAttacher attacher = new GspAttacher(context, out)
        attacher.attachComponent(component, attrs)
    }
    
    /**
     * Converts the tag attrs and body into a config object, and attaches this to the
     * parent component in the current GspContext.
     * <p>
     * If 'bodyProperty' is specified, the body text is added to the 'attrs' map with that as the key.
     * 
     * @param attrs The tag attributes to use for the config
     * @param body The tag body to evaluate, and add the results to the attrs map
     * @param bodyProperty The key that will be used for storing the bodyText in the attrs map
     * @param componentClass The expected class of the parent component
     * @param type The type name of this config
     */
    protected void attachConfig(attrs, body, Class componentClass, String type, bodyProperty = null) {
        if (body && bodyProperty && !attrs.containsKey(bodyProperty)) {
            attrs[bodyProperty] = body()?.toString()
        }
        GspComponentConfig config = new GspComponentConfig(componentClass, type, attrs)
        GspContext context = new GspContext()
        GspAttacher attacher = new GspAttacher(context, out)
        attacher.attachConfig(config)
    }
    
    /**
     * Finds the current Vaadin parent component.
     */
    protected Component lookupParentComponent() {
        GspContext context = new GspContext()
        return context.node?.component
    }

    /**
     * Removes entries from attrs map that are relevant to the attaching of the component
     * to its parent, and should not be used to set properties on the component itself.
     * <p>
     * For example 'location' is only of interest to the parent component, as it specifies
     * where in the parent component to attach this component.
     * 
     * @param attrs The attrs map from which 'attaching'-type entries will be removed
     * 
     * @return The map of 'attaching'-type entries that have been removed.
     */
    protected Map removeAttachAttrs(Map attrs) {
        Map result = [:]
        result.location = Utils.removeCaseInsensitive(attrs, 'location')
        result.style = Utils.removeCaseInsensitive(attrs, 'wrapperStyle')
        result.'class' = Utils.removeCaseInsensitive(attrs, 'wrapperClass')
        result.role = Utils.removeCaseInsensitive(attrs, 'role')
        return result
    }
    
    /**
     * Applies the specified properties to a
     * Vaadin {@link com.vaadin.ui.Window}
     */
    protected Closure windowConfigurer = { Map props, Window window ->
        // Remove specific properties
        def onblur = Utils.removeAllCaseInsensitive(props, 'onblur')
        def onfocus = Utils.removeAllCaseInsensitive(props, 'onfocus')
        def onclose = Utils.removeAllCaseInsensitive(props, 'onclose')
        def onresize = Utils.removeAllCaseInsensitive(props, 'onresize')
        
        // Process specific properties
        if (onblur) window.addListener(Converter.toBlurListener(onblur))
        if (onfocus) window.addListener(Converter.toFocusListener(onfocus))
        if (onclose) window.addListener(Converter.toCloseListener(onclose))
        if (onresize) window.addListener(Converter.toResizeListener(onresize))
 
        // Remaining props
        defaultConfigurer(props, window)
    }

    /**
     * Applies the specified properties to a
     * Vaadin {@link org.grails.plugin.vaadin.ui.GrailsIncludeLayout} component
     */
    protected Closure includeConfigurer = { Map props, GrailsIncludeLayout include ->
        // Remove specific properties
        def args = Utils.removeAllCaseInsensitive(props, ['controller', 'action', 'params', 'id', 'instance'])
        def fragment = Utils.removeCaseInsensitive(props, 'fragment')
 
        // Process specific properties
        if (args) { include.include(args) }
        else if (fragment) { include.include(fragment) }
 
        // Remaining props
        defaultConfigurer(props, include)
    }

    /**
     * Applies the specified properties to a
     * Vaadin {@link org.grails.plugin.vaadin.ui.GrailsIncludeLayout} component
     */
    protected Closure layoutConfigurer = { Map props, GspTemplateLayout layout ->
        // Remove specific properties
        def name = Utils.removeCaseInsensitive(props, 'name')
        def body = Utils.removeCaseInsensitive(props, 'body')
        
        // Process specific properties
        if (!name) throw new IllegalArgumentException("Tag <${namespace}:layout> must have 'name' attribute")
        layout.name = name
        layout.body = body
        layout.context = [params:params, flash:flash, controllerName:controllerName]

        // Remaining props
        defaultConfigurer(props, layout)
    }

    /**
     * Applies the specified properties to a
     * Vaadin {@link com.vaadin.ui.TabSheet}
     */
    protected Closure tabsConfigurer = { Map props, TabSheet tabSheet ->
        // Remove specific properties
        def tabs = Utils.removeCaseInsensitive(props, 'tabs')
        def ontab = Utils.removeCaseInsensitive(props, 'ontab')
        
        // Process specific properties
        if (tabs) {
            tabs.findAll { it.type == "tab" }.collect { it.props }.each { tabProps ->
                def body = Utils.removeCaseInsensitive(tabProps, 'body')
                def icon = Utils.removeCaseInsensitive(tabProps, 'icon')
                def position = Utils.removeCaseInsensitive(tabProps, 'position')
                def var = Utils.removeCaseInsensitive(tabProps, 'var')
                def selected = Utils.removeCaseInsensitive(tabProps, 'selected')
                def tab
                if (body && position != null) {
                    tab = tabSheet.addTab(body, Utils.toInt(position))
                } else if (body) {
                    tab = tabSheet.addTab(body)
                }
                if (tab) {
                    if (icon != null) tab.icon = Converter.toResource(icon)
                    if (var) set(var:var, value:tab)
                    if (selected) tabSheet.selectedTab = tab.component 
                    // All remaining props
                    tabProps.each { k,v ->
                        tab."${k}" = (v == "false" ? false : (v == "true" ? true : v))
                    }
                }
            }
        }
        if (ontab) { tabSheet.addListener(Converter.toSelectedTabChangeListener(ontab)) }
        
        // Remaining props
        defaultConfigurer(props, tabSheet)
    }

    /**
     * Applies the specified properties to a
     * Vaadin {@link com.vaadin.ui.Table}
     */
    protected Closure tableConfigurer = { Map props, Table table ->
        // Remove specific properties
        def pageLength = Utils.removeCaseInsensitive(props, 'pageLength')
        def columns = Utils.removeCaseInsensitive(props, 'columns')
        def dataSource = Utils.removeCaseInsensitive(props, 'containerDataSource')
        
        // Process specific properties
        if (pageLength != null) { table.pageLength = Utils.toInt(pageLength) }
        if (dataSource) { table.containerDataSource = dataSource }
        if (columns) {
            def columnDefs = []
            columns.each {
                if (it.type == "column") {
                    def column = Converter.toColumnDef(it.props)
                    if (column) columnDefs << column
                }
            }
            if (columnDefs) {
                columnDefs.each { if (it.generator) table.addGeneratedColumn(it.name, it.generator) }
                table.visibleColumns = columnDefs.collect { it.name }
                table.columnHeaders = columnDefs.collect { it.header }
            }
        }
        
        // Remaining props
        defaultConfigurer(props, table)
    }
    
    /**
     * Applies the specified properties to a
     * Vaadin {@link com.vaadin.ui.Form}
     */
    protected Closure formConfigurer = { Map props, Form form ->
        // Remove specific properties
        def itemDataSource = Utils.removeCaseInsensitive(props, 'itemDataSource')
        def bean = Utils.removeCaseInsensitive(props, 'bean')
        def fields = Utils.removeCaseInsensitive(props, 'fields')

        // Process specific properties
        def dataSource
        if (bean) {
            if (grailsApplication.isArtefactOfType("Domain", bean.getClass())) {
                dataSource = new DomainItem(grailsApplication, bean)
            } else {
                dataSource = new BeanItem(bean)
            }
        }
        if (itemDataSource) { dataSource = itemDataSource }
        if (fields) {
            def fieldDefs = []
            fields.each {
                if (it.type == "field") {
                    def field = Converter.toFieldDef(it.props, toConfigurer)
                    if (field && field.name) fieldDefs << field
                }
            }
     
            // Have to set the field factory BEFORE setting the data source
            def fieldFactory = new FieldDefDrivenFieldFactory(fieldDefs)
            form.formFieldFactory = fieldFactory
            
            // Now set the data source
            if (dataSource) {
                form.setItemDataSource(dataSource, fieldDefs.collect { it.name })
            }
        }
        
        // Remaining props
        defaultConfigurer(props, form)
    }
   
    /**
     * Applies the specified properties to a
     * Vaadin {@link com.vaadin.ui.AbstractField}
     */
    protected Closure fieldConfigurer = { Map props, Field field ->
        // Remove specific properties
        def name = Utils.removeCaseInsensitive(props, 'name')
        def type = Utils.removeCaseInsensitive(props, 'type')
        def instance = Utils.removeCaseInsensitive(props, 'instance')
        def pattern = Utils.removeCaseInsensitive(props, 'pattern')
        def resolution = Utils.removeCaseInsensitive(props, 'resolution')
        def textChange = Utils.removeCaseInsensitive(props, 'onTextChange')
        def valueChange = Utils.removeCaseInsensitive(props, 'onValueChange')
        def readOnlyStatusChange = Utils.removeCaseInsensitive(props, 'onReadOnlyStatusChange')
        def defaultValue = Utils.removeCaseInsensitive(props, 'default')

        // Process specific properties
        if (pattern) { field.addValidator(new RegexpValidator(pattern, "Invalid Value")) }
        if (resolution) { field.resolution = Converter.toDateResolution(resolution) }
        if (textChange) { field.addListener(Converter.toTextChangeListener(textChange)) }
        if (valueChange) { field.addListener(Converter.toValueChangeListener(valueChange)) }
        if (readOnlyStatusChange) { field.addListener(Converter.toReadOnlyStatusChangeListener(readOnlyStatusChange)) }
        if (defaultValue != null) { field.propertyDataSource = new DefaultValuePropertyConverter(field.propertyDataSource, defaultValue) }

        // Remaining props
        defaultConfigurer(props, field)
    }
    
    /**
     * Applies the specified properties to a Vaadin text-type field
     */
    protected Closure textConfigurer = { Map props, AbstractField field ->
        // Default
        field.nullRepresentation = ""

        // Remaining props
        fieldConfigurer(props, field)
    }
    
    /**
     * Applies the specified properties to a Vaadin
     * {@link com.vaadin.ui.DateField}
     */
    protected Closure dateConfigurer = { Map props, DateField date ->
        // Allow calendar properties to be used with DateField
        date.propertyDataSource = new CalendarPropertyConverter(date.propertyDataSource)

        // Remaining props
        fieldConfigurer(props, date)
    }
    
    /**
     * Applies the specified properties to a
     * Vaadin {@link com.vaadin.ui.AbstractSelect}
     */
    protected Closure selectConfigurer = { Map props, AbstractSelect select ->
        // Remove specific properties
        def from = Utils.removeCaseInsensitive(props, 'from')
        def itemIds = Utils.removeCaseInsensitive(props, 'itemIds')
        def itemId = Utils.removeCaseInsensitive(props, 'itemId')
        def itemCaption = Utils.removeCaseInsensitive(props, 'itemCaption')
        def itemCaptionMessagePrefix = Utils.removeCaseInsensitive(props, 'itemCaptionMessagePrefix')
        def itemIcon = Utils.removeCaseInsensitive(props, 'itemIcon')
        def itemEquals = Utils.removeCaseInsensitive(props, 'itemEquals')
        def noSelection = Utils.removeCaseInsensitive(props, 'noSelection')
        def filteringMode = Utils.removeCaseInsensitive(props, 'filteringMode')
        def rows = Utils.removeCaseInsensitive(props, 'rows')
        def columns = Utils.removeCaseInsensitive(props, 'columns')
        
        // Process specific properties
        if (filteringMode) { select.filteringMode = Converter.toFilteringMode(filteringMode) }
        if (itemEquals) { select.propertyDataSource = new DomainProxyPropertyConverter(select.propertyDataSource, itemEquals.toString()) }
        if (from) { select.containerDataSource = Converter.toContainer(from, itemIds, itemId, itemEquals, noSelection) }
        if (noSelection != null) {
            // Use first item as noSelection
            def id = select.itemIds?.iterator()?.next()
            if (id != null) {
                select.itemCaptionMode = AbstractSelect.ITEM_CAPTION_MODE_EXPLICIT_DEFAULTS_ID
                select.nullSelectionItemId = id
                select.nullSelectionAllowed = true
                select.setItemCaption(id, noSelection)
            }
        }
        // Always explicitly set the captions, to avoid no-hibernate-session errors on rendering
        select.itemCaptionMode = AbstractSelect.ITEM_CAPTION_MODE_EXPLICIT_DEFAULTS_ID
        select.itemIds.each { id ->
            if (id != select.nullSelectionItemId) {
                def item = select.getItem(id)
                if (item) { select.setItemCaption(id, Converter.toItemCaption(item, itemCaption, itemCaptionMessagePrefix, message)) }
            }
        }
        if (itemIcon) {
            select.itemIds.each {
                if (it != select.nullSelectionItemId) {
                    def item = select.getItem(it)
                    if (item) { select.setItemIcon(it, Converter.toItemIcon(item, itemIcon)) }
                }
            }
        }
        if (rows) { select.rows = Utils.toInt(rows) }
        if (columns) { select.columns = Utils.toInt(columns) }
        
        // Remaining props
        fieldConfigurer(props, select)
    }

    /**
     * Applies the specified properties to a timeZone-type 
     * Vaadin {@link com.vaadin.ui.AbstractSelect}
     */
    protected Closure timeZoneSelectConfigurer = { Map props, AbstractField select ->
        props.from = Converter.toTimeZones(Utils.removeCaseInsensitive(props, 'from') ?: vaadinTagDataService.defaultTimeZones)
        props.default = Converter.toTimeZone(Utils.removeCaseInsensitive(props, 'default') ?: TimeZone.default)
        props.nullSelectionAllowed = Utils.removeCaseInsensitive(props, 'nullSelectionAllowed') ?: false
        props.filteringMode = Utils.removeCaseInsensitive(props, 'filteringMode') ?: "contains"
        def date = new Date()
        props.itemCaption = Utils.removeCaseInsensitive(props, 'itemCaption') ?: {
            def shortName = it.getDisplayName(it.inDaylightTime(date), TimeZone.SHORT)
            def longName = it.getDisplayName(it.inDaylightTime(date), TimeZone.LONG)

            def offset = it.rawOffset
            def sign = it.rawOffset < 0 ? '-' : '+'
            def hour = String.format("%02d", (int) Math.abs(offset / (1000*60*60)))
            def min = String.format("%02d", (int) Math.abs(offset / (60 * 1000)) % 60)

            return "${sign}${hour}:${min} ${shortName}, ${longName}"
        }
                
        // Configure as select
        selectConfigurer(props, select)
    }
    
    /**
     * Applies the specified properties to a locale-type 
     * Vaadin {@link com.vaadin.ui.AbstractSelect}
     */
    protected Closure localeSelectConfigurer = { Map props, AbstractSelect select ->
        props.from = Converter.toLocales(Utils.removeCaseInsensitive(props, 'from') ?: vaadinTagDataService.defaultLocales)
        props.default = Converter.toLocale(Utils.removeCaseInsensitive(props, 'default') ?: RCU.getLocale(request))
        props.nullSelectionAllowed = Utils.removeCaseInsensitive(props, 'nullSelectionAllowed') ?: false
        props.itemCaption = Utils.removeCaseInsensitive(props, 'itemCaption') ?: 'displayName'

        // Configure as select
        selectConfigurer(props, select)
    }
    
    /**
     * Applies the specified properties to a currency-type 
     * Vaadin {@link com.vaadin.ui.AbstractSelect}
     */
    protected Closure currencySelectConfigurer = { Map props, AbstractSelect select ->
        props.from = Converter.toCurrencies(Utils.removeCaseInsensitive(props, 'from') ?: vaadinTagDataService.defaultCurrencies)
        props.default = Converter.toCurrency(Utils.removeCaseInsensitive(props, 'default') ?: Currency.getInstance(RCU.getLocale(request)))
        props.nullSelectionAllowed = Utils.removeCaseInsensitive(props, 'nullSelectionAllowed') ?: false
        props.itemCaption = {
            def symbol = vaadinTagDataService.getSymbol(it)
            return symbol ? "${it.currencyCode} - ${symbol}" : it.currencyCode
        }

        // Configure as select
        selectConfigurer(props, select)
    }
    
    /**
     * Applies the specified properties to a
     * Vaadin {@link com.vaadin.ui.Label}
     */
    protected Closure labelConfigurer = { Map props, Label label ->
        // Remove specific properties
        def contentMode = Utils.removeCaseInsensitive(props, 'contentMode')
        
        // Process specific properties
        if (contentMode != null) { label.contentMode = Converter.toLabelContentMode(contentMode) }

        // Remaining props
        defaultConfigurer(props, label)
    }
    
    /**
     * Applies the specified properties to a
     * Vaadin {@link org.grails.plugin.vaadin.ui.GrailsButton}
     */
    protected Closure linkConfigurer = { Map props, GrailsButton link ->
        // Remove specific properties
        def icon = Utils.removeCaseInsensitive(props, 'icon')
        def onclick = Utils.removeCaseInsensitive(props, 'onclick')
        
        // Process specific properties
        if (icon) { link.icon = Converter.toResource(icon) }
        if (onclick) { link.onclick = Converter.toClosure(onclick) }

        // Remaining props
        defaultConfigurer(props, link)
    }
    
    /**
     * Applies the specified properties to a
     * {@link org.grails.plugin.vaadin.ui.DefaultUploadField}
     */
    protected Closure uploadConfigurer = { Map props, DefaultUploadField upload ->
        // Remove specific properties
        def fieldType = Utils.removeCaseInsensitive(props, 'fieldType')
        def storageMode = Utils.removeCaseInsensitive(props, 'storageMode')
        def onupload = Utils.removeCaseInsensitive(props, 'onupload')
        
        // Process specific properties
        upload.fieldType = fieldType ? Converter.toUploadFieldType(fieldType) : UploadField.FieldType.BYTE_ARRAY
        upload.storageMode = storageMode ? Converter.toUploadStorageMode(storageMode) : UploadField.StorageMode.MEMORY
        upload.setSizeFull()
        if (onupload) { upload.uploadComponent.addListener(Converter.toUploadSucceededListener(onupload)) }
        
        // Add fixed style
        upload.addStyleName("v-file")

        // Ensure field works with Byte[] array types, not just byte[]
        if (UploadField.FieldType.BYTE_ARRAY) upload.propertyDataSource = new ByteArrayPropertyConverter(upload.propertyDataSource)
        
        // Remaining props
        fieldConfigurer(props, upload)
    }

    /**
     * Applies the specified properties to a
     * <a href="https://vaadin.com/directory#addon/customfield">CustomField</a>
     */
    protected Closure customFieldConfigurer = { Map props, CustomField custom ->
        fieldConfigurer(props, custom)
    }
   
    /**
     * Applies the specified properties to a
     * Vaadin {@link com.vaadin.ui.Component}
     * <p>
     * Note: this method will attempt to apply ALL entries in the map to the component,
     * so may throw errors if an entry cannot be applied to a Vaadin Component.
     */
    protected Closure defaultConfigurer = { Map props, Component component ->
        props.each { k,v ->
            def name = k.toString().toLowerCase()
            switch (name) {
                case "var": set(var:v, value:component); break;
                case "class": v.split(" ").each { component.addStyleName(it) }; break;
                case "style": throw new IllegalArgumentException("CSS 'style' is not supported by Vaadin components"); break;
                case "sizeundefined": component.setSizeUndefined(); break;
                case "sizefull": component.setSizeFull(); break;
                case "oncomponentevent": if (v) component.addListener(Converter.toComponentListener(v)); break;
                case "onrepaintrequest": if (v) component.addListener(Converter.toRepaintRequestListener(v)); break;
                case "width": if (v) component.setWidth(v.toString()); break;
                case "height": if (v) component.setHeight(v.toString()); break;
                case "componenterror": if (v) component.componentError = Converter.toComponentError(v); break;
				case "configurer": break; // Ignore
                default: component."${k}" = (v == "false" ? false : (v == "true" ? true : v))
            }
        }
    }
    
    /**
     * Converts the specified Vaadin component class to a Closure that should configure that class
     */
    protected Closure toConfigurer = { Class clazz ->
        if (DateField.isAssignableFrom(clazz)) return dateConfigurer
        else if (CheckBox.isAssignableFrom(clazz)) return fieldConfigurer
        else if (ComboBox.isAssignableFrom(clazz)) return selectConfigurer
        else if (ListSelect.isAssignableFrom(clazz)) return selectConfigurer
        else if (OptionGroup.isAssignableFrom(clazz)) return selectConfigurer
        else if (PasswordField.isAssignableFrom(clazz)) return textConfigurer
        else if (Select.isAssignableFrom(clazz)) return selectConfigurer
        else if (TextField.isAssignableFrom(clazz)) return textConfigurer
        else if (TextArea.isAssignableFrom(clazz)) return textConfigurer
        else if (DefaultUploadField.isAssignableFrom(clazz)) return uploadConfigurer
        else if (CustomField.isAssignableFrom(clazz)) return customFieldConfigurer
        else return defaultConfigurer
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
        String message = Utils.removeCaseInsensitive(attrs, 'message')?.toString()
        if (!message) {
            throw new IllegalArgumentException("Method ${namespace}.confirm() must have 'message' attribute")
        }
        final application = request.vaadinApplication
        return { button ->
            createConfirmPopup(application.mainWindow, message, {
                button.dispatch()
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
        def form = Utils.removeCaseInsensitive(attrs, 'form')
        if (form && !(form instanceof Form) ) {
            form = this.pageScope."${form}"
        }
        if (!form) {
            throw new IllegalArgumentException("Method ${namespace}.commit() must have 'form' attribute")
        }
        final application = request.vaadinApplication
        return {
            try {
                withTransaction(application, {form.commit()})
                return true
            } catch(err) {
                return false
            }
        }
    }
    
    /**
     * Helper method that excutes the specified closure in a persistence transaction.
     *
     * @attr toBeExecuted REQUIRED The closure to be executed in a transaction.
     */
    Closure withTransaction = { vaadinApplication, toBeExecuted ->
        final vaadinTransactionManager = vaadinApplication.getBean("vaadinTransactionManager")
        if (! vaadinTransactionManager) {
            throw new NullPointerException("Unable to retrieve spring bean 'vaadinTransactionManager'")
        }
        return vaadinTransactionManager.wrapInTransaction(toBeExecuted) 
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
