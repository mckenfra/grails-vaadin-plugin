package org.grails.plugin.vaadin.taglib

import org.springframework.web.servlet.support.RequestContextUtils as RCU

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
import org.grails.plugin.vaadin.ui.GspTemplateLayout;
import org.grails.plugin.vaadin.ui.GspLayout;
import org.grails.plugin.vaadin.ui.GrailsButton;
import org.grails.plugin.vaadin.utils.ByteArrayPropertyConverter;
import org.grails.plugin.vaadin.utils.CalendarPropertyConverter;
import org.grails.plugin.vaadin.utils.DefaultValuePropertyConverter;
import org.grails.plugin.vaadin.utils.PropertyConverter;
import org.grails.plugin.vaadin.utils.Utils;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ReadOnlyStatusChangeEvent;
import com.vaadin.data.Property.ReadOnlyStatusChangeListener;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.AbstractBeanContainer.BeanIdResolver;
import com.vaadin.data.util.AbstractBeanContainer.PropertyBasedBeanIdResolver;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.terminal.ErrorMessage;
import com.vaadin.terminal.Paintable;
import com.vaadin.terminal.Resource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.terminal.UserError;
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
import com.vaadin.ui.Upload;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Window.Notification;

import org.vaadin.addon.customfield.CustomField;
import org.vaadin.easyuploads.FileBuffer;
import org.vaadin.easyuploads.FileFactory;
import org.vaadin.easyuploads.MultiFileUpload;
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
        // Ensure have layout name
        def name = Utils.removeCaseInsensitive(attrs, 'name')
        if (!name) {
            throw new IllegalArgumentException("Tag <${namespace}:createLayout> must have 'name' attribute")
        }
        
        return applyAttrsAndBodyToComponent(attrs, null, new GspTemplateLayout(name, body, params, null, flash, controllerName))
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
     * Returns a new {@link org.grails.plugin.vaadin.ui.GspLayout} using the body
     * 
     * @attr name REQUIRED The name of the layout view
     */
    Closure createLocation = { attrs, body ->
        return applyAttrsAndBodyToComponent(attrs, null, new GspLayout(body))
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
     * Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/HorizontalLayout.html">HorizontalLayout</a> tag
     * <p>
     * Components to be added to the layout should be specified as nested elements.
     */
    Closure horizontalLayout = { attrs, body ->
        def attachAttrs = removeAttachAttrs(attrs)
        attachComponent(createHorizontalLayout(attrs, body), attachAttrs)
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
        def attachAttrs = removeAttachAttrs(attrs)
        attachComponent(createVerticalLayout(attrs, body), attachAttrs)
    }
    
    /**
     * Returns a new Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/VerticalLayout.html">VerticalLayout</a>
     */
    Closure createVerticalLayout = { attrs, body ->
        return applyAttrsAndBodyToComponent(attrs, body, new VerticalLayout())
    }

    /**
     * Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/TabSheet.html">TabSheet</a> tag
     * <p>
     * Tabs to be added to the TabSheet should be specified as nested &lt;v:tab&gt; elements.
     */
    Closure tabs = { attrs, body ->
        def attachAttrs = removeAttachAttrs(attrs)
        attachComponent(createTabs(attrs, body), attachAttrs)
    }
 
    /**
     * Returns a new Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/TabSheet.html">TabSheet</a>
     */
    Closure createTabs = { attrs, body ->
        return applyAttrsAndBodyToComponent(attrs, body, new TabSheet(), tabsConfigurer, null, "tabs")
    }

    /**
     * Configuration tag for describing a tab in a Vaadin
     * <a href="http://vaadin.com/api/com/vaadin/ui/TabSheet.html">TabSheet</a>
     * <p>
     * The tab contents should be specified in the body of the tag.
     */
    Closure tab = { attrs, body ->
        attrs.body = new GspLayout(body)
        attachConfig(attrs, null, TabSheet.class, "tab")
    }

    /**
     * Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/Accordion.html">Accordion</a> tag
     * <p>
     * Tabs to be added to the Accordion should be specified as nested &lt;v:tab&gt; elements.
     */
    Closure accordion = { attrs, body ->
        def attachAttrs = removeAttachAttrs(attrs)
        attachComponent(createAccordion(attrs, body), attachAttrs)
    }
 
    /**
     * Returns a new Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/Accordion.html">Accordion</a>
     */
    Closure createAccordion = { attrs, body ->
        return applyAttrsAndBodyToComponent(attrs, body, new Accordion(), tabsConfigurer, null, "tabs")
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
        def attachAttrs = removeAttachAttrs(attrs)
        attachComponent(createTable(attrs, body), attachAttrs)
    }
    
    /**
     * Returns a new Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/Table.html">Table</a>
     */
    Closure createTable = { attrs, body ->
        return applyAttrsAndBodyToComponent(attrs, body, new Table(), tableConfigurer, null, "columns")
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
        attachConfig(attrs, body, Table.class, "column", "header")
    }

    /**
     * Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/Form.html">Form</a> tag
     * <p>
     * Fields should be configured using nested &lt;v:form&gt; tags
     */
    Closure form = { attrs, body ->
        def attachAttrs = removeAttachAttrs(attrs)
        attachComponent(createForm(attrs, body), attachAttrs)
    }
    
    /**
     * Returns a new Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/Form.html">Form</a>
     */
    Closure createForm = { attrs, body ->
        return applyAttrsAndBodyToComponent(attrs, body, new Form(), formConfigurer, null, "fields")
    }
    
    /**
     * Configuration tag for describing a field in a Vaadin
     * <a href="http://vaadin.com/api/com/vaadin/ui/Form.html">Form</a>
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
     * Returns a new Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/Field.html">Field</a>
     * of the type specified in the attrs.
     */
    Closure createField = { attrs, body ->
        def fieldDef = toFieldDef(attrs)
        return applyAttrsAndBodyToComponent(attrs, body, fieldDef.type?.newInstance(), fieldDef.configurer, "caption")
    }
    
    /**
     * <a href="http://vaadin.com/api/com/vaadin/ui/CheckBox.html">CheckBox</a> field
     */
    Closure checkBox = { attrs, body ->
        attrs.type = 'checkBox'
        fieldTag(attrs, body)
    }
    
    /**
     * <a href="http://vaadin.com/api/com/vaadin/ui/CheckBox.html">CheckBox</a> field
     */
    Closure checkbox = checkBox
    
    /**
     * <a href="http://vaadin.com/api/com/vaadin/ui/ComboBox.html">ComboBox</a> field
     */
    Closure comboBox = { attrs, body ->
        attrs.type = 'comboBox'
        fieldTag(attrs, body)
    }
    
    /**
     * <a href="http://vaadin.com/api/com/vaadin/ui/ComboBox.html">ComboBox</a> field
     */
    Closure combobox = comboBox
    
    /**
     * <a href="http://vaadin.com/api/com/vaadin/ui/DateField.html">DateField</a> field
     */
    Closure date = { attrs, body ->
        attrs.type = 'dateField'
        fieldTag(attrs, body)
    }
    
    /**
     * <a href="http://vaadin.com/api/com/vaadin/ui/ListSelect.html">ListSelect</a> field
     */
    Closure listSelect = { attrs, body ->
        attrs.type = 'listSelect'
        fieldTag(attrs, body)
    }
    
    /**
     * <a href="http://vaadin.com/api/com/vaadin/ui/ListSelect.html">ListSelect</a> field
     */
    Closure listselect = listSelect
    
    /**
     * <a href="http://vaadin.com/api/com/vaadin/ui/OptionGroup.html">OptionGroup</a> field
     */
    Closure optionGroup = { attrs, body ->
        attrs.type = 'optionGroup'
        fieldTag(attrs, body)
    }
    
    /**
     * <a href="http://vaadin.com/api/com/vaadin/ui/OptionGroup.html">OptionGroup</a> field
     */
    Closure optiongroup = optionGroup
    
    /**
     * <a href="http://vaadin.com/api/com/vaadin/ui/PasswordField.html">PasswordField</a> field
     */
    Closure password = { attrs, body ->
        attrs.type = 'password'
        fieldTag(attrs, body)
    }
    
    /**
     * <a href="http://vaadin.com/api/com/vaadin/ui/Select.html">Select</a> field
     */
    Closure select = { attrs, body ->
        attrs.type = 'select'
        fieldTag(attrs, body)
    }
    
    /**
     * <a href="http://vaadin.com/api/com/vaadin/ui/TextField.html">TextField</a> field
     */
    Closure text = { attrs, body ->
        attrs.type = 'text'
        fieldTag(attrs, body)
    }

    /**
     * <a href="http://vaadin.com/api/com/vaadin/ui/TextArea.html">TextArea</a> field
     */
    Closure textArea = { attrs, body ->
        attrs.type = 'textArea'
        fieldTag(attrs, body)
    }

    /**
     * <a href="http://vaadin.com/api/com/vaadin/ui/TextArea.html">TextArea</a> field
     */
    Closure textarea = textArea

    /**
     * TimeZone-populated
     * <a href="http://vaadin.com/api/com/vaadin/ui/ComboBox.html">ComboBox</a> field
     */
    Closure timeZoneSelect = { attrs, body ->
        attrs.type = Utils.removeCaseInsensitive(attrs, 'type') ?: 'comboBox'
        attrs.configurer = timeZoneSelectConfigurer
        fieldTag(attrs, body)
    }
    
    /**
     * TimeZone-populated
     * <a href="http://vaadin.com/api/com/vaadin/ui/ComboBox.html">ComboBox</a> field
     */
    Closure timezoneSelect = timeZoneSelect
    
    /**
     * Locale-populated
     * <a href="http://vaadin.com/api/com/vaadin/ui/ComboBox.html">ComboBox</a> field
     */
    Closure localeSelect = { attrs, body ->
        attrs.type = Utils.removeCaseInsensitive(attrs, 'type') ?: 'comboBox'
        attrs.configurer = localeSelectConfigurer
        fieldTag(attrs, body)
    }
    
    /**
     * Currency-populated
     * <a href="http://vaadin.com/api/com/vaadin/ui/ComboBox.html">ComboBox</a> field
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
        attrs.compositionRoot = new GspLayout(body)
        attachConfig(attrs, null, Form.class, "field")
    }
    
    /**
     * <a href="https://vaadin.com/directory#addon/customfield">CustomField</a>
     */
    Closure customfield = customField

    /**
     * Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/Label.html">Label</a> tag
     * <p>
     * The label message should be specified in the tag body.
     */
    Closure label = { attrs, body ->
        def attachAttrs = removeAttachAttrs(attrs)
        attachComponent(createLabel(attrs, body), attachAttrs)
    }
    
    /**
     * Returns a new Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/Label.html">Label</a>
     */
    Closure createLabel = { attrs, body ->
        return applyAttrsAndBodyToComponent(attrs, body, new Label(), labelConfigurer, "value")
    }

    /**
     * Custom {@link org.grails.plugin.vaadin.ui.RequestButton} tag
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
     * Returns a new custom {@link org.grails.plugin.vaadin.ui.RequestButton}
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
            GspContext context = new GspContext(session)
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
        GspContext context = new GspContext(session)
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
        GspContext context = new GspContext(session)
        GspAttacher attacher = new GspAttacher(context, out)
        attacher.attachConfig(config)
    }
    
    /**
     * Finds the current Vaadin parent component.
     */
    protected Component lookupParentComponent() {
        GspContext context = new GspContext(session)
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
     * Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/TabSheet.html">TabSheet</a>
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
                    if (icon != null) tab.icon = toResource(icon)
                    if (var) set(var:var, value:tab)
                    if (selected) tabSheet.selectedTab = tab.component 
                    // All remaining props
                    tabProps.each { k,v ->
                        tab."${k}" = (v == "false" ? false : (v == "true" ? true : v))
                    }
                }
            }
        }
        if (ontab) { tabSheet.addListener(toSelectedTabChangeListener(ontab)) }
        
        // Remaining props
        defaultConfigurer(props, tabSheet)
    }

    /**
     * Applies the specified properties to a
     * Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/Table.html">Table</a>
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
                    def column = toColumnDef(it.props)
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
     * Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/Form.html">Form</a>
     */
    protected Closure formConfigurer = { Map props, Form form ->
        // Remove specific properties
        def dataSource = Utils.removeCaseInsensitive(props, 'itemDataSource')
        def fields = Utils.removeCaseInsensitive(props, 'fields')

        // Process specific properties
        if (fields) {
            def fieldDefs = []
            fields.each {
                if (it.type == "field") {
                    def field = toFieldDef(it.props)
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
     * Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/AbstractField.html">Field</a>
     */
    protected Closure fieldConfigurer = { Map props, Field field ->
        // Remove specific properties
        def name = Utils.removeCaseInsensitive(props, 'name')
        def instance = Utils.removeCaseInsensitive(props, 'instance')
        def pattern = Utils.removeCaseInsensitive(props, 'pattern')
        def fieldError = Utils.removeCaseInsensitive(props, 'fieldError')
        def resolution = Utils.removeCaseInsensitive(props, 'resolution')
        def textChange = Utils.removeCaseInsensitive(props, 'onTextChange')
        def valueChange = Utils.removeCaseInsensitive(props, 'onValueChange')
        def readOnlyStatusChange = Utils.removeCaseInsensitive(props, 'onReadOnlyStatusChange')
        def defaultValue = Utils.removeCaseInsensitive(props, 'default')

        // Process specific properties
        if (pattern) { field.addValidator(new RegexpValidator(pattern, "Invalid Value")) }
        if (fieldError) { field.componentError = toComponentError(fieldError) }
        if (resolution) { field.resolution = toDateResolution(resolution) }
        if (textChange) { field.addListener(toTextChangeListener(textChange)) }
        if (valueChange) { field.addListener(toValueChangeListener(valueChange)) }
        if (readOnlyStatusChange) { field.addListener(toReadOnlyStatusChangeListener(readOnlyStatusChange)) }
        if (defaultValue != null) { field.propertyDataSource = new DefaultValuePropertyConverter(defaultValue) }

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
     * <a href="http://vaadin.com/api/com/vaadin/ui/DateField.html">DateField</a>
     */
    protected Closure dateConfigurer = { Map props, DateField date ->
        // Allow calendar properties to be used with DateField
        date.propertyDataSource = new CalendarPropertyConverter()

        // Remaining props
        fieldConfigurer(props, date)
    }
    
    /**
     * Applies the specified properties to a
     * Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/AbstractSelect.html">AbstractSelect</a>
     */
    protected Closure selectConfigurer = { Map props, AbstractSelect select ->
        // Remove specific properties
        def from = Utils.removeCaseInsensitive(props, 'from')
        def keys = Utils.removeCaseInsensitive(props, 'keys')
        def optionKey = Utils.removeCaseInsensitive(props, 'optionKey')
        def optionValue = Utils.removeCaseInsensitive(props, 'optionValue')
        def optionIcon = Utils.removeCaseInsensitive(props, 'optionIcon')
        def noSelection = Utils.removeCaseInsensitive(props, 'noSelection')
        def valueMessagePrefix = Utils.removeCaseInsensitive(props, 'valueMessagePrefix')
        def filteringMode = Utils.removeCaseInsensitive(props, 'filteringMode')
        def rows = Utils.removeCaseInsensitive(props, 'rows')
        def columns = Utils.removeCaseInsensitive(props, 'columns')
        
        // Process specific properties
        if (filteringMode) { select.filteringMode = toFilteringMode(filteringMode) }
        if (from) { select.containerDataSource = toContainer(from, keys, optionKey, noSelection) }
        if (noSelection != null) {
            // Use first item as noSelection
            def itemId = select.itemIds?.iterator()?.next()
            if (itemId != null) {
                select.itemCaptionMode = AbstractSelect.ITEM_CAPTION_MODE_EXPLICIT_DEFAULTS_ID
                select.nullSelectionItemId = itemId
                select.nullSelectionAllowed = true
                select.setItemCaption(itemId, noSelection)
            }
        }
        if (optionValue || valueMessagePrefix) {
            select.itemCaptionMode = AbstractSelect.ITEM_CAPTION_MODE_EXPLICIT_DEFAULTS_ID
            select.itemIds.each {
                if (it != select.nullSelectionItemId) {
                    def item = select.getItem(it)
                    if (item) { select.setItemCaption(it, toItemCaption(item, optionValue, valueMessagePrefix)) }
                }
            }
        }
        if (optionIcon) {
            select.itemIds.each {
                if (it != select.nullSelectionItemId) {
                    def item = select.getItem(it)
                    if (item) { select.setItemIcon(it, toItemIcon(item, optionIcon)) }
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
     * Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/AbstractSelect.html">AbstractSelect</a>
     */
    protected Closure timeZoneSelectConfigurer = { Map props, AbstractField select ->
        props.from = toTimeZones(Utils.removeCaseInsensitive(props, 'from') ?: vaadinTagDataService.defaultTimeZones)
        props.default = toTimeZone(Utils.removeCaseInsensitive(props, 'default') ?: TimeZone.default)
        props.nullSelectionAllowed = Utils.removeCaseInsensitive(props, 'nullSelectionAllowed') ?: false
        props.filteringMode = Utils.removeCaseInsensitive(props, 'filteringMode') ?: "contains"
        def date = new Date()
        props.optionValue = Utils.removeCaseInsensitive(props, 'optionValue') ?: {
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
     * Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/AbstractSelect.html">AbstractSelect</a>
     */
    protected Closure localeSelectConfigurer = { Map props, AbstractSelect select ->
        props.from = toLocales(Utils.removeCaseInsensitive(props, 'from') ?: vaadinTagDataService.defaultLocales)
        props.default = toLocale(Utils.removeCaseInsensitive(props, 'default') ?: RCU.getLocale(request))
        props.nullSelectionAllowed = Utils.removeCaseInsensitive(props, 'nullSelectionAllowed') ?: false
        props.optionValue = Utils.removeCaseInsensitive(props, 'optionValue') ?: 'displayName'

        // Configure as select
        selectConfigurer(props, select)
    }
    
    /**
     * Applies the specified properties to a currency-type 
     * Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/AbstractSelect.html">AbstractSelect</a>
     */
    protected Closure currencySelectConfigurer = { Map props, AbstractSelect select ->
        props.from = toCurrencies(Utils.removeCaseInsensitive(props, 'from') ?: vaadinTagDataService.defaultCurrencies)
        props.default = toCurrency(Utils.removeCaseInsensitive(props, 'default') ?: Currency.getInstance(RCU.getLocale(request)))
        props.nullSelectionAllowed = Utils.removeCaseInsensitive(props, 'nullSelectionAllowed') ?: false
        props.optionValue = {
            def symbol = vaadinTagDataService.getSymbol(it)
            return symbol ? "${it.currencyCode} - ${symbol}" : it.currencyCode
        }

        // Configure as select
        selectConfigurer(props, select)
    }
    
    /**
     * Applies the specified properties to a
     * Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/Label.html">Label</a>
     */
    protected Closure labelConfigurer = { Map props, Label label ->
        // Remove specific properties
        def contentMode = Utils.removeCaseInsensitive(props, 'contentMode')
        
        // Process specific properties
        if (contentMode != null) { label.contentMode = toLabelContentMode(contentMode) }

        // Remaining props
        defaultConfigurer(props, label)
    }
    
    /**
     * Applies the specified properties to a
     * Vaadin {@link org.grails.plugin.vaadin.ui.RequestButton}
     */
    protected Closure linkConfigurer = { Map props, GrailsButton link ->
        // Remove specific properties
        def icon = Utils.removeCaseInsensitive(props, 'icon')
        def onclick = Utils.removeCaseInsensitive(props, 'onclick')
        
        // Process specific properties
        if (icon) { link.icon = toResource(icon) }
        if (onclick) { link.onclick = toClosure(onclick) }

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
        upload.fieldType = fieldType ? toUploadFieldType(fieldType) : UploadField.FieldType.BYTE_ARRAY
        upload.storageMode = storageMode ? toUploadStorageMode(storageMode) : UploadField.StorageMode.MEMORY
        upload.setSizeFull()
        if (onupload) { upload.uploadComponent.addListener(toUploadSucceededListener(onupload)) }
        
        // Add fixed style
        upload.addStyleName("v-file")

        // Ensure field works with Byte[] array types, not just byte[]
        if (UploadField.FieldType.BYTE_ARRAY) upload.propertyDataSource = new ByteArrayPropertyConverter()
        
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
     * Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/Component.html">Component</a>
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
                case "oncomponentevent": if (v) component.addListener(toComponentListener(v)); break;
                case "onrepaintrequest": if (v) component.addListener(toRepaintRequestListener(v)); break;
                case "width": if (v) component.setWidth(v.toString()); break;
                case "height": if (v) component.setHeight(v.toString()); break;
                default: component."${k}" = (v == "false" ? false : (v == "true" ? true : v))
            }
        }
    }
    
    /**
     * Converts the properties to a ColumnDef
     */
    protected ColumnDef toColumnDef(props) {
        // Remove specific properties
        def name = Utils.removeCaseInsensitive(props, 'name')
        def header = Utils.removeCaseInsensitive(props, 'header')
        def generator = Utils.removeCaseInsensitive(props, 'generator')
        
        // Process specific properties
        if (generator instanceof Closure) {
            def generatorClosure = generator
            generator = new Table.ColumnGenerator() {
                public Component generateCell(Table source, Object itemId, Object columnId) {
                    def item = source.getItem(itemId)
                    return generatorClosure(item)
                }
            }
        }
        
        // Return ColumnDef
        return new DefaultColumnDef(name, header, generator)
    }
    
    /**
     * Converts the properties to a FieldDef
     */
    protected FieldDef toFieldDef(props) {
        // Remove specific properties
        def name = Utils.removeCaseInsensitive(props, 'name')
        def type = Utils.removeCaseInsensitive(props, 'type')
        def configurer = Utils.removeCaseInsensitive(props, 'configurer')
        
        // Create type and configurer
        if (! (type?.class == Class.class) ) { type = toFieldClass(type) }
        if (! (configurer instanceof Closure) ) { configurer = toConfigurer(type) }
        
        // Return FieldDef
        return new DefaultFieldDef(name, type, configurer, props)
    }

    /**
     * Converts the value to a Field Class
     * <p>
     * Note that a list of aliases for Vaadin Component classes will be used
     * for the conversion. E.g. a value of 'date' will translate to a DateField class. 
     */
    protected Class toFieldClass(value) {
        Class result = TextField.class
        if (value) {
            // If value is a class, we're done
            if (value.class == Class.class) {
                result = value
            } else {
                // Type may be a shortcut name
                switch (value.toString().toLowerCase()) {
                    case 'date': // Fall through
                    case 'datefield': // Fall through
                    case 'datepicker': result = DateField; break;
                    case 'checkbox': result = CheckBox; break;
                    case 'combobox': result = ComboBox; break;
                    case 'listselect': result = ListSelect; break;
                    case 'option': // Fall through
                    case 'optiongroup': result = OptionGroup; break;
                    case 'passwordfield': // Fall through
                    case 'password': result = PasswordField; break;
                    case 'select': result = Select; break;
                    case 'text': result = TextField; break;
                    case 'textarea': result = TextArea; break;
                    case 'upload': // Fall through
                    case 'file': result = DefaultUploadField; break;
                    case 'customfield': result = DefaultCustomField; break;
                }
                
                // No result - assume value is class name
                if (!result) {
                    try {
                        result = Class.forName(value)
                    } catch (err) {
                        throw new IllegalArgumentException("Field value not recognised '${value}'")
                    }
                }
            }
        }
        return result
    }
    
    /**
     * Converts the specified Vaadin component class to a Closure that should configure that class
     */
    protected Closure toConfigurer(Class clazz) {
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
     * Converts the value to a <a href="http://vaadin.com/api/com/vaadin/data/Container.html">Container</a>.
     *
     * @param value If a collection, converted to a BeanContainer. Otherwise, the String representation is
     * treated as a comma-separated list of values and converted to an IndexedContainer.
     * @param keys List of keys to use for items in container, overriding both idProperty param, and the default id
     * @param idProperty If is a closure, is called with bean as parameter to generate key. Otherwise,
     * is treated as name of property of each bean that should be used as id.
     * @param noSelection Caption to use for item that will represent a null selection. This will
     * always be added as first item in list.
     */
    protected Container toContainer(value, keys = null, idProperty = null, noSelection = null) {
        Container result
        
        // Must have a value
        if (value == null) {
            throw new IllegalArgumentException("Cannot convert '${value}' to Container!")
        }
        
        // Convert arrays to collection
        if (value.class.array || value.class.enum) value = value as List
        
        // Collection of beans
        if (value instanceof Collection) {
            result = new BeanContainer(Utils.getCommonSuperclass(value))
            
            // Repeat first item as null item
            if (noSelection != null && value) { result.addItem("null", value[0]) }

            // Add beans to container with explicit list of keys
            if (keys) {
                value.eachWithIndex { v, i ->
                    result.addItem(keys[i], v)
                }
                
            // Add beans to container with keys generated from closure
            } else if (idProperty instanceof Closure) {
                value.each { v ->
                    result.addItem(idProperty(v), v)
                }
                
            // Add beans to container with specific bean property as id
            } else if (idProperty) {
                result.beanIdResolver = new PropertyBasedBeanIdResolver(idProperty.toString())
                result.addAll(value)
                
            // Add beans to container with bean as id
            } else {
                value.each {
                     result.addItem(it, it)   
                }
            }
            
        // Default to a comma-separated string
        } else {
             result = new IndexedContainer(value.toString().split(",") as List)
        }

        return result
    }
    
    /**
     * Generates a caption for a single item using the optionValue
     * and valueMessagePrefix params.
     *  
     * @param item The item for which the caption will be generated
     * @param value An item property name or closure that accepts the item (or wrapped bean)
     * and returns the caption. If not specified, the item's toString() method is used
     * to generate the caption
     * @param valueMessagePrefix A value that will be prepended to the generated caption
     * and the result is then used as an message code in a i18n lookup. If this returns null
     * then the original generated caption is returned.
     *   
     * @return The generated item caption 
     */
    protected String toItemCaption(Item item, value, valueMessagePrefix) {
        def result = value
        if (value && value instanceof Closure) {
            def param = item instanceof BeanItem ? item.bean : item
            result = value(param)
        } else if (value) {
            result = item.getItemProperty(value)?.toString()
        } else {
            result = item instanceof BeanItem ? item.bean.toString() : item.toString()
        }
        if (valueMessagePrefix) {
            def msgCode = valueMessagePrefix + "." + result
            def msg = message(code:msgCode)
            // Not sure why msg sometimes gets wrapped in square brackets...
            if (msg && msg != msgCode && "[${msgCode}]" != msg) { result = msg }
        }
        return result
    }

    /**
     * Generates an icon for a single item using the value, which may be a property id
     * or a closure that accepts the item (or wrapped bean) and returns the icon name
     *  
     * @param item The item for which the icon will be generated
     * @param value An item property name or closure that accepts the item (or wrapped bean)
     * and returns the icon name.
     * 
     * @return The resource for the generated icon name
     */
    protected Resource toItemIcon(Item item, value) {
        def result = value
        if (!value instanceof Resource) {
            def iconName
            if (value instanceof Closure) {
                def param = item instanceof BeanItem ? item.bean : item
                iconName = value(param)
            } else if (value) {
                iconName = item.getItemProperty(value)?.toString()
            } else {
                iconName = item.toString()
            }
            if (iconName) { result = new ThemeResource(iconName) }
        }
        return result
    }

    /**
     * Converts the value to a <a href="http://vaadin.com/api/com/vaadin/terminal/ErrorMessage.html">ErrorMessage</a>
     * if not already.
     */
    protected ErrorMessage toComponentError(value) {
        return value instanceof ErrorMessage ? value : new UserError(value?.toString())
    }
    
    /**
     * Converts the value to a <a href="http://vaadin.com/api/com/vaadin/data/Property.ReadOnlyStatusChangeListener.html">ReadOnlyStatusChangeListener</a>
     * if not already.
     */
    protected ReadOnlyStatusChangeListener toReadOnlyStatusChangeListener(value) {
        if (value instanceof ReadOnlyStatusChangeListener) {
            return value
        } else if (value instanceof Closure) {
            return new ReadOnlyStatusChangeListener() { void readOnlyStatusChange(ReadOnlyStatusChangeEvent event) { value(event) } }
        } else {
            throw new IllegalArgumentException("Cannot convert '${value}' to ReadOnlyStatusChangeListener")
        }
    }
    
    /**
     * Converts the value to a <a href="http://vaadin.com/api/com/vaadin/data/Property.TextChangeListener.html">TextChangeListener</a>
     * if not already.
     */
    protected TextChangeListener toTextChangeListener(value) {
        if (value instanceof TextChangeListener) {
            return value
        } else if (value instanceof Closure) {
            return new TextChangeListener() { void textChange(TextChangeEvent event) { value(event) } }
        } else {
            throw new IllegalArgumentException("Cannot convert '${value}' to TextChangeListener")
        }
    }

    /**
     * Converts the value to a <a href="http://vaadin.com/api/com/vaadin/data/Property.ValueChangeListener.html">ValueChangeListener</a>
     * if not already.
     */
    protected ValueChangeListener toValueChangeListener(value) {
        if (value instanceof ValueChangeListener) {
            return value
        } else if (value instanceof Closure) {
            return new ValueChangeListener() { void valueChange(ValueChangeEvent event) { value(event) } }
        } else {
            throw new IllegalArgumentException("Cannot convert '${value}' to ValueChangeListener")
        }
    }
    
    /**
     * Converts the value to a <a href="http://vaadin.com/api/com/vaadin/ui/Component.Listener.html">Component.Listener</a>
     * if not already.
     */
    protected Component.Listener toComponentListener(value) {
        if (value instanceof Component.Listener) {
            return value
        } else if (value instanceof Closure) {
            return new Component.Listener() { void componentEvent(Component.Event event) { value(event) } }
        } else {
            throw new IllegalArgumentException("Cannot convert '${value}' to Component.Listener")
        }
    }
    
    /**
     * Converts the value to a <a href="http://vaadin.com/api/com/vaadin/terminal/Paintable.RepaintRequestListener.html">RepaintRequestListener</a>
     * if not already.
     */
    protected Paintable.RepaintRequestListener toRepaintRequestListener(value) {
        if (value instanceof Paintable.RepaintRequestListener) {
            return value
        } else if (value instanceof Closure) {
            return new Paintable.RepaintRequestListener() { void repaintRequested(Paintable.RepaintRequestEvent event) { value(event) } }
        } else {
            throw new IllegalArgumentException("Cannot convert '${value}' to Paintable.RepaintRequestListener")
        }
    }

    /**
     * Converts the value to a <a href="http://vaadin.com/api/com/vaadin/ui/Upload.SucceededListener.html">Upload.SucceededListener</a>
     * if not already.
     */
    protected Upload.SucceededListener toUploadSucceededListener(value) {
        if (value instanceof Upload.SucceededListener) {
            return value
        } else if (value instanceof Closure) {
            return new Upload.SucceededListener() { void uploadSucceeded(Upload.SucceededEvent event) { value(event) } }
        } else {
            throw new IllegalArgumentException("Cannot convert '${value}' to Upload.SucceededListener")
        }
    }
    
    /**
     * Converts the value to a <a href="http://vaadin.com/api/com/vaadin/ui/TabSheet.SelectedTabChangeListener.html">TabSheet.SelectedTabChangeListener</a>
     * if not already.
     */
    protected TabSheet.SelectedTabChangeListener toSelectedTabChangeListener(value) {
        if (value instanceof TabSheet.SelectedTabChangeListener) {
            return value
        } else if (value instanceof Closure) {
            return new TabSheet.SelectedTabChangeListener() { void selectedTabChange(TabSheet.SelectedTabChangeEvent event) { value(event) } }
        } else {
            throw new IllegalArgumentException("Cannot convert '${value}' to TabSheet.SelectedTabChangeListener")
        }
    }

    /**
     * Converts specified value to list of
     * <a href="http://docs.oracle.com/javase/6/docs/api/java/util/TimeZone.html">TimeZone</a> objects
     */
    protected List<TimeZone> toTimeZones(value) {
        def result = []
        if (value) {
            if (value instanceof Collection || value.class.array) {
                result = (value as List).collect { toTimeZone(it) }
            } else {
                result = value.toString().split(',').collect { TimeZone.getTimeZone(it.toString()) }
            }
        }
        return result
    }
    
    /**
     * Converts specified value to
     * <a href="http://docs.oracle.com/javase/6/docs/api/java/util/TimeZone.html">TimeZone</a>
     */
    protected TimeZone toTimeZone(value) {
        if (value == null || value instanceof TimeZone) return value
        else return TimeZone.getTimeZone(value.toString())
    }
    
    /**
     * Converts specified value to list of
     * <a href="http://docs.oracle.com/javase/6/docs/api/java/util/Locale.html">Locale</a> objects
     */
    protected List<Locale> toLocales(value) {
        def result = []
        if (value) {
            if (value instanceof Collection || value.class.array) {
                result = (value as List).collect { toLocale(it) }
            } else {
                result = value.toString().split(',').collect { new Locale(it.toString()) }
            }
        }
        return result
    }
    
    /**
     * Converts specified value to
     * <a href="http://docs.oracle.com/javase/6/docs/api/java/util/Locale.html">Locale</a>
     */
    protected Locale toLocale(value) {
        if (value == null || value instanceof Locale) return value
        else return new Locale(value.toString())
    }
    
    /**
     * Converts specified value to list of
     * <a href="http://docs.oracle.com/javase/6/docs/api/java/util/Currency.html">Currency</a> objects
     */
    protected List<Currency> toCurrencies(value) {
        def result = []
        if (value instanceof Collection || value.class.array) {
            result = (value as List).collect { toCurrency(it) }
        } else {
            result = value.toString().split(',').collect { Currency.getInstance(it.toString()) }
        }
        return result
    }
    
    /**
     * Converts specified value to
     * <a href="http://docs.oracle.com/javase/6/docs/api/java/util/Currency.html">Currency</a>
     */
    protected Currency toCurrency(value) {
        if (value == null || value instanceof Currency) return value
        else return Currency.getInstance(value instanceof Locale ? value : value.toString())
    }
    
    /**
     * Converts the value to a <a href="http://vaadin.com/api/com/vaadin/terminal/Resource.html">Resource</a>
     * if not already.
     */
    protected Resource toResource(value) {
        return value instanceof Resource ? value : new ThemeResource(value.toString())
    }
    
    /**
     * Ensures the value is a closure, otherwise throws an exception
     */
    protected Closure toClosure(value) {
        if (value instanceof Closure) {
            return value
        } else {
            throw new Exception("Cannot convert onclick '${value}' to Closure")
        }
    }
    
    /**
     * Converts the value to a DateField Resolution
     */
    protected int toDateResolution(value) {
        int n = DateField.RESOLUTION_DAY
        if (value != null) {
            if (Utils.isNumber(value)) {
                n = value
            } else {
                String name = value.toString().toLowerCase()
                switch(name) {
                    case "day": n = DateField.RESOLUTION_DAY; break;
                    case "hour": n = DateField.RESOLUTION_HOUR; break;
                    case "min": // FALL THROUGH
                    case "minute": n = DateField.RESOLUTION_MIN; break;
                    case "month": n = DateField.RESOLUTION_MONTH; break;
                    case "milli": // FALL THROUGH
                    case "millis": // FALL THROUGH
                    case "msec":  // FALL THROUGH
                    case "millisecond": n = DateField.RESOLUTION_MSEC; break;
                    case "sec": // FALL THROUGH
                    case "second": n = DateField.RESOLUTION_SEC; break;
                    case "year": n = DateField.RESOLUTION_YEAR; break;
                    default: throw new IllegalArgumentException("Unrecognised date resolution '${value}'")
                }
            }
        }
        return n
    }
    
    /**
     * Converts the value to a Vaadin
     * <a href="http://vaadin.com/api/com/vaadin/ui/AbstractSelect.Filtering.html">Filtering Mode</a>
     */
    protected int toFilteringMode(value) {
        int n = AbstractSelect.Filtering.FILTERINGMODE_OFF
        if (value != null) {
            if (Utils.isNumber(value)) {
                n = value
            } else {
                String name = value.toString().toLowerCase()
                switch(name) {
                    case "contains": n = AbstractSelect.Filtering.FILTERINGMODE_CONTAINS; break;
                    case "off": n = AbstractSelect.Filtering.FILTERINGMODE_OFF; break;
                    case "startswith": n = AbstractSelect.Filtering.FILTERINGMODE_STARTSWITH; break;
                    default: throw new IllegalArgumentException("Unrecognised filtering mode '${value}'")
                }
            }
        }
        return n
    }

    /**
     * Converts the value to a Label Content Mode
     */
    protected int toLabelContentMode(value) {
        int n = Label.CONTENT_DEFAULT
        if (value != null) {
            if (Utils.isNumber(value)) {
                n = value
            } else {
                String name = value.toString().toLowerCase()
                switch(name) {
                    case "preformatted": n = Label.CONTENT_PREFORMATTED; break;
                    case "raw": n = Label.CONTENT_RAW; break;
                    case "text": n = Label.CONTENT_TEXT; break;
                    case "xhtml": n = Label.CONTENT_XHTML; break;
                    case "xml": n = Label.CONTENT_XML; break;
                    case "default": n = Label.CONTENT_DEFAULT; break;
                    default: throw new IllegalArgumentException("Unrecognised content mode '${value}'")
                }
            }
        }
        return n
    }
    
    /**
     * Converts the value to an EasyUploads UploadField.FieldType
     */
    protected UploadField.FieldType toUploadFieldType(value) {
        UploadField.FieldType result = UploadField.FieldType.BYTE_ARRAY
        if (value != null) {
            if (value instanceof UploadField.FieldType) {
                result = value
            } else {
                String name = value.toString().toLowerCase()
                switch(name) {
                    case "string": // Fall through
                    case "utf8": // Fall through
                    case "utf8string": // Fall through
                    case "utf8_string": result = UploadField.FieldType.UTF8_STRING; break;
                    case "bytearray": // Fall through
                    case "byte_array": // Fall through
                    case "bytes": result = UploadField.FieldType.BYTE_ARRAY; break;
                    case "file": result = UploadField.FieldType.FILE; break;
                    default: result = UploadField.FieldType.valueOf(value.toString())
                }
            }
        }
        return result
    }
    
    /**
     * Converts the value to an EasyUploads UploadField.StorageMode
     */
    protected UploadField.StorageMode toUploadStorageMode(value) {
        UploadField.StorageMode result = UploadField.StorageMode.MEMORY
        if (value != null) {
            if (value instanceof UploadField.StorageMode) {
                result = value
            } else {
                String name = value.toString().toLowerCase()
                switch(name) {
                    case "memory": result = UploadField.StorageMode.MEMORY; break
                    case "file": result = UploadField.StorageMode.FILE; break
                    default: result = UploadField.StorageMode.valueOf(value.toString())
                }
            }
        }
        return result
    }

    /**
     * Helper method that returns a closure that can be used in the
     * 'onclick' attribute of a {@link org.grails.plugin.vaadin.ui.RequestButton}
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
        return { dispatch->
            createConfirmPopup(application.mainWindow, message, {
                dispatch()
            })
            return false
        }
    }
    
    /**
     * Helper method that returns a closure that can be used in the
     * 'onclick' attribute of a {@link org.grails.plugin.vaadin.ui.RequestButton}
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
        return {dispatch->
            form.commit()
        }
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
