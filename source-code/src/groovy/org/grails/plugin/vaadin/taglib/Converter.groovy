package org.grails.plugin.vaadin.taglib

import groovy.lang.Closure;

import java.util.Currency;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.grails.plugin.vaadin.gsp.ColumnDef;
import org.grails.plugin.vaadin.gsp.DefaultColumnDef;
import org.grails.plugin.vaadin.gsp.DefaultFieldDef;
import org.grails.plugin.vaadin.gsp.FieldDef;
import org.grails.plugin.vaadin.ui.DefaultCustomField;
import org.grails.plugin.vaadin.ui.DefaultUploadField;
import org.grails.plugin.vaadin.utils.DomainProxy;
import org.grails.plugin.vaadin.utils.Utils;
import org.vaadin.easyuploads.UploadField;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property.ReadOnlyStatusChangeEvent;
import com.vaadin.data.Property.ReadOnlyStatusChangeListener;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.AbstractBeanContainer.BeanIdResolver;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.terminal.ErrorMessage;
import com.vaadin.terminal.ExternalResource;
import com.vaadin.terminal.Paintable;
import com.vaadin.terminal.Resource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.terminal.UserError;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.DateField;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.Select;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;
import com.vaadin.ui.Window.ResizeEvent;
import com.vaadin.ui.Window.ResizeListener;

/**
 * Collection of methods for converting a tag attribute to a required type,
 * typically a Vaadin object.
 * <p>
 * This class is primarily a support class for Vaadin tag libraries.
 * 
 * @author Francis McKenzie
 */
class Converter {
    /**
     * Converts the value to a {@link com.vaadin.ui.Table.ColumnGenerator}
     * 
     * @param value If a closure, it must accept an {@link com.vaadin.data.Item} as an argument. Otherwise,
     * it must be a {@link com.vaadin.ui.Table.ColumnGenerator} or else an exception will be thrown.
     */
    static Table.ColumnGenerator toColumnGenerator(value) {
        if (value instanceof Table.ColumnGenerator) {
            return value
        } else if (value instanceof Closure) {
            return new ColumnGenerator() {
                public Component generateCell(Table source, Object itemId, Object columnId) {
                    def item = source.getItem(itemId)
                    return value(item)
                }
            }
        } else {
            throw new IllegalArgumentException("Cannot convert '${value}' to Table.ColumnGenerator")
        }
    }
    
    /**
     * Converts the properties to a ColumnDef
     */
    static ColumnDef toColumnDef(Map props) {
        // Remove specific properties
        def name = Utils.getCaseInsensitive(props, 'name')
        def header = Utils.getCaseInsensitive(props, 'header')
        def generator = Utils.getCaseInsensitive(props, 'generator')
        
        // Process specific properties
        if (generator) { generator = toColumnGenerator(generator) }
        
        // Return ColumnDef
        return new DefaultColumnDef(name, header, generator)
    }
    
    /**
     * Converts the properties to a FieldDef
     */
    static FieldDef toFieldDef(Map props, Closure toConfigurer) {
        // Remove specific properties
        def name = Utils.getCaseInsensitive(props, 'name')
        def type = Utils.getCaseInsensitive(props, 'type')
        def configurer = Utils.getCaseInsensitive(props, 'configurer')
        
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
     * for the conversion. E.g. a value of 'date' will translate to a 
     * {@link com.vaadin.ui.DateField} class.
     */
    static Class toFieldClass(value) {
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
     * Converts the value to a {@link com.vaadin.data.Container}.
     *
     * @param value If a collection, converted to a BeanContainer. Otherwise, the String representation is
     * treated as a comma-separated list of values and converted to an IndexedContainer.
     * @param itemIds List of itemIds to use for items in container, overriding both itemId param, and the default id
     * @param itemId If is a closure, is called with bean as parameter to generate key. Otherwise,
     * is treated as name of property of each bean that should be used as id.
     * @param itemEquals The name of a property to use to inject an equals method into each item,
     * using a proxy.
     * @param noSelection Caption to use for item that will represent a null selection. This will
     * always be added as first item in list.
     */
    static Container toContainer(value, itemIds = null, itemId = null, itemEquals = null, noSelection = null) {
        Container result
        
        // Already a container
        if (value instanceof Container) return value
        
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
 
            // Add beans to container with explicit list of itemIds
            if (itemIds) {
                value.eachWithIndex { v, i ->
                    result.addItem(itemIds[i], v)
                }
                
            // Add beans to container with itemIds generated from closure
            } else if (itemId instanceof Closure) {
                value.each { v ->
                    result.addItem(itemId(v), v)
                }
                
            // Add beans to container with specific bean property as id
            } else if (itemId) {
                result.beanIdResolver = new BeanIdResolver<Object,Object>() {
                    public Object getIdForBean(Object bean) { return bean."${itemId}" }
                }
                result.addAll(value)
                
            // Add beans to container with bean as id
            } else {
                // Add items
                value.each {
                    result.addItem(itemEquals ? new DomainProxy(itemEquals.toString()).wrap(it) : it, it)
                }
            }
            
        // Default to a comma-separated string
        } else {
             result = new IndexedContainer(value.toString().split(",") as List)
        }
 
        return result
    }
    
    /**
     * Generates a caption for a single item using the itemCaption
     * and itemCaptionMessagePrefix params.
     *
     * @param item The item for which the caption will be generated
     * @param value An item property name or closure that accepts the item (or wrapped bean)
     * and returns the caption. If not specified, the item's toString() method is used
     * to generate the caption
     * @param itemCaptionMessagePrefix A value that will be prepended to the generated caption
     * and the result is then used as an message code in a i18n lookup. If this returns null
     * then the original generated caption is returned.
     * @param toMessage Method for obtaining an i18n message
     *
     * @return The generated item caption
     */
    static String toItemCaption(Item item, value, itemCaptionMessagePrefix, toMessage) {
        def result = value
        if (value && value instanceof Closure) {
            def param = item instanceof BeanItem ? item.bean : item
            result = value(param)
        } else if (value) {
            result = item.getItemProperty(value)?.toString()
        } else {
            result = item instanceof BeanItem ? item.bean.toString() : item.toString()
        }
        if (itemCaptionMessagePrefix) {
            def msgCode = itemCaptionMessagePrefix + "." + result
            def msg = toMessage(code:msgCode)
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
    static Resource toItemIcon(Item item, value) {
        def result = value
        if (! (value instanceof Resource)) {
            if (value instanceof Closure) {
                def param = item instanceof BeanItem ? item.bean : item
                result = value(param)
            } else if (value) {
                result = item.getItemProperty(value)?.toString()
            } else {
                result = item.toString()
            }
            if (result && ! (result instanceof Resource)) {
                result = new ThemeResource(result.toString())
            }
        }
        return result
    }
 
    /**
     * Converts the value to a {@link com.vaadin.terminal.ErrorMessage}
     * if not already.
     */
    static ErrorMessage toComponentError(value) {
        return value instanceof ErrorMessage ? value : new UserError(value?.toString())
    }
    
    /**
     * Converts the value to a {@link com.vaadin.data.Property.ReadOnlyStatusChangeListener}
     * if not already.
     */
    static ReadOnlyStatusChangeListener toReadOnlyStatusChangeListener(value) {
        if (value instanceof ReadOnlyStatusChangeListener) {
            return value
        } else if (value instanceof Closure) {
            return new ReadOnlyStatusChangeListener() { void readOnlyStatusChange(ReadOnlyStatusChangeEvent event) { value(event) } }
        } else {
            throw new IllegalArgumentException("Cannot convert '${value}' to ReadOnlyStatusChangeListener")
        }
    }
    
    /**
     * Converts the value to a {@link com.vaadin.data.Property.TextChangeListener}
     * if not already.
     */
    static TextChangeListener toTextChangeListener(value) {
        if (value instanceof TextChangeListener) {
            return value
        } else if (value instanceof Closure) {
            return new TextChangeListener() { void textChange(TextChangeEvent event) { value(event) } }
        } else {
            throw new IllegalArgumentException("Cannot convert '${value}' to TextChangeListener")
        }
    }
 
    /**
     * Converts the value to a {@link com.vaadin.data.Property.ValueChangeListener}
     * if not already.
     */
    static ValueChangeListener toValueChangeListener(value) {
        if (value instanceof ValueChangeListener) {
            return value
        } else if (value instanceof Closure) {
            return new ValueChangeListener() { void valueChange(ValueChangeEvent event) { value(event) } }
        } else {
            throw new IllegalArgumentException("Cannot convert '${value}' to ValueChangeListener")
        }
    }
    
    /**
     * Converts the value to a {@link com.vaadin.ui.Component.Listener}
     * if not already.
     */
    static Component.Listener toComponentListener(value) {
        if (value instanceof Component.Listener) {
            return value
        } else if (value instanceof Closure) {
            return new Component.Listener() { void componentEvent(Component.Event event) { value(event) } }
        } else {
            throw new IllegalArgumentException("Cannot convert '${value}' to Component.Listener")
        }
    }
 
    /**
     * Converts the value to a {@link com.vaadin.terminal.Paintable.RepaintRequestListener}
     * if not already.
     */
    static Paintable.RepaintRequestListener toRepaintRequestListener(value) {
        if (value instanceof Paintable.RepaintRequestListener) {
            return value
        } else if (value instanceof Closure) {
            return new Paintable.RepaintRequestListener() { void repaintRequested(Paintable.RepaintRequestEvent event) { value(event) } }
        } else {
            throw new IllegalArgumentException("Cannot convert '${value}' to Paintable.RepaintRequestListener")
        }
    }
 
    /**
     * Converts the value to a {@link com.vaadin.ui.Upload.SucceededListener}
     * if not already.
     */
    static Upload.SucceededListener toUploadSucceededListener(value) {
        if (value instanceof Upload.SucceededListener) {
            return value
        } else if (value instanceof Closure) {
            return new Upload.SucceededListener() { void uploadSucceeded(Upload.SucceededEvent event) { value(event) } }
        } else {
            throw new IllegalArgumentException("Cannot convert '${value}' to Upload.SucceededListener")
        }
    }
    
    /**
     * Converts the value to a {@link com.vaadin.ui.TabSheet.SelectedTabChangeListener}
     * if not already.
     */
    static TabSheet.SelectedTabChangeListener toSelectedTabChangeListener(value) {
        if (value instanceof TabSheet.SelectedTabChangeListener) {
            return value
        } else if (value instanceof Closure) {
            return new TabSheet.SelectedTabChangeListener() { void selectedTabChange(TabSheet.SelectedTabChangeEvent event) { value(event) } }
        } else {
            throw new IllegalArgumentException("Cannot convert '${value}' to TabSheet.SelectedTabChangeListener")
        }
    }
 
    /**
     * Converts the value to a {@link com.vaadin.event.FieldEvents.BlurListener}
     * if not already.
     */
    static BlurListener toBlurListener(value) {
        if (value instanceof BlurListener) {
            return value
        } else if (value instanceof Closure) {
            return new BlurListener() { void blur(BlurEvent event) { value(event) } }
        } else {
            throw new IllegalArgumentException("Cannot convert '${value}' to BlurListener")
        }
    }
 
    /**
     * Converts the value to a {@link com.vaadin.event.FieldEvents.FocusListener}
     * if not already.
     */
    static FocusListener toFocusListener(value) {
        if (value instanceof FocusListener) {
            return value
        } else if (value instanceof Closure) {
            return new FocusListener() { void focus(FocusEvent event) { value(event) } }
        } else {
            throw new IllegalArgumentException("Cannot convert '${value}' to FocusListener")
        }
    }

    /**
     * Converts the value to a {@link com.vaadin.ui.Window.CloseListener}
     * if not already.
     */
    static CloseListener toCloseListener(value) {
        if (value instanceof CloseListener) {
            return value
        } else if (value instanceof Closure) {
            return new CloseListener() { void windowClose(CloseEvent event) { value(event) } }
        } else {
            throw new IllegalArgumentException("Cannot convert '${value}' to CloseListener")
        }
    }

    /**
     * Converts the value to a {@link com.vaadin.ui.Window.ResizeListener}
     * if not already.
     */
    static ResizeListener toResizeListener(value) {
        if (value instanceof ResizeListener) {
            return value
        } else if (value instanceof Closure) {
            return new ResizeListener() { void windowResized(ResizeEvent event) { value(event) } }
        } else {
            throw new IllegalArgumentException("Cannot convert '${value}' to ResizeListener")
        }
    }

    /**
     * Converts specified value to list of
     * {@link java.util.TimeZone} objects
     */
    static List<TimeZone> toTimeZones(value) {
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
     * {@link java.util.TimeZone}
     */
    static TimeZone toTimeZone(value) {
        if (value == null || value instanceof TimeZone) return value
        else return TimeZone.getTimeZone(value.toString())
    }
    
    /**
     * Converts specified value to list of
     * {@link java.util.Locale} objects
     */
    static List<Locale> toLocales(value) {
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
     * {@link java.util.Locale}
     */
    static Locale toLocale(value) {
        if (value == null || value instanceof Locale) return value
        else return new Locale(value.toString())
    }
    
    /**
     * Converts specified value to list of
     * {@link java.util.Currency} objects
     */
    static List<Currency> toCurrencies(value) {
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
     * {@link java.util.Currency}
     */
    static Currency toCurrency(value) {
        if (value == null || value instanceof Currency) return value
        else return Currency.getInstance(value instanceof Locale ? value : value.toString())
    }
    
    /**
     * Converts the value to a {@link com.vaadin.terminal.Resource}
     * if not already.
     */
    static Resource toResource(value) {
        if (!value || value instanceof Resource) return value
        String pathOrUrl = value.toString()
        // Url
        if (pathOrUrl.startsWith('/') || pathOrUrl.indexOf('://')) {
            return new ExternalResource(pathOrUrl)
        // Theme resource
        } else {
            return new ThemeResource(pathOrUrl)
        }
    }
    
    /**
     * Ensures the value is a closure, otherwise throws an exception
     */
    static Closure toClosure(value) {
        if (value instanceof Closure) {
            return value
        } else {
            throw new Exception("Cannot convert onclick '${value}' to Closure")
        }
    }
    
    /**
     * Converts the value to a {@link com.vaadin.ui.DateField} Resolution
     */
    static int toDateResolution(value) {
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
     * {@link com.vaadin.ui.AbstractSelect.Filtering}
     */
    static int toFilteringMode(value) {
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
     * Converts the value to a Vaadin {@com.vaadin.ui.Label} Content Mode
     */
    static int toLabelContentMode(value) {
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
    static UploadField.FieldType toUploadFieldType(value) {
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
    static UploadField.StorageMode toUploadStorageMode(value) {
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
}