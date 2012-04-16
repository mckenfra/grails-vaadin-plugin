package org.grails.plugin.vaadin.gsp

import org.grails.plugin.vaadin.scaffolding.DefaultVaadinTemplateGenerator;

import com.vaadin.data.Item;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.DateField;
import com.vaadin.ui.DefaultFieldFactory;
import com.vaadin.ui.Field;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Select;
import com.vaadin.ui.TextField;

/**
 * A Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/DefaultFieldFactory.html">DefaultFieldFactory</a>
 * that uses the configuration properties in the specified map to configure
 * new fields.
 * <p>
 * The fields map contains String fieldnames mapped to properties maps. When creating
 * a field, the properties map for that field is looked up in the fields map. If found,
 * all the properties in that properties map are then applied to the field object that is
 * created. 
 * 
 * @author Francis McKenzie
 */
class MapDrivenFieldFactory extends DefaultFieldFactory {
    /**
     * Maps fieldname to individual properties maps
     */
    Map fields
    
    /**
     * Constructs a new map-driven FieldFactory using
     * the specified fields configuration
     * 
     * @param fields The map of fieldnames to properties maps
     */
    public MapDrivenFieldFactory(Map fields) {
        super()
        this.fields = fields
    }
    
    /**
     * Creates a field by calling the superclass createField(...) method,
     * then looks up the configuration for that field in the fields map.
     * If found, applies all the properties in that properties map to the
     * field object.
     * <p>
     * Note that if the field object does not support a property, then
     * an exception will be thrown.
     * <p>
     * Also note that as a convenience this method always sets the
     * nullRepresentation property for TextFields to an empty String.
     */
    @Override
    public Field createField(Item item, Object propertyId, Component uiContext) {
        def fieldProps = fields[propertyId] ?: [:]
        
        // Try to create the field using the 'type' attribute
        Field f = createFieldFromType(fieldProps.remove('type'))
        
        // If not, use the super class to create a suitable field based on the
        // property type.
        if (! f) {
            f = super.createField(item, propertyId, uiContext)
        }
        
        // Automatically set nullRepresentation for TextFields
        if (f instanceof TextField) {
            f.nullRepresentation = ""
        }
        
        if (propertyId) {
            if (fieldProps) {
                fieldProps.each { k,v ->
                    f."${k}" = (v == "false" ? false : (v == "true" ? true : v)) 
                }
            }
        }
        
        return f
    }
    
    protected Field createFieldFromType(type) {
        Field result
        if (type) {
            try {
                // If tyep is a class, create an instance
                if (type.class == Class.class) {
                    result = type.newInstance()
                
                } else {
                    // Type may be a shortcut name
                    switch (type.toString().toLowerCase()) {
                        case 'date': // Fall through
                        case 'datefield': // Fall through
                        case 'datepicker': result = new DateField(); result.setResolution(DateField.RESOLUTION_DAY); break;
                        case 'checkbox': result = new CheckBox(); break;
                        case 'select': result = new Select(); break;
                        case 'combobox': result = new ComboBox(); break;
                        case 'option': // Fall through
                        case 'optiongroup': result = new OptionGroup(); break;
                    }
                    
                    
                    // No result - assume type is class name
                    if (!result) {
                        def clazz = Class.forName(type)
                        result = clazz.newInstance()
                    }
                }
            } catch (err) {
                throw new IllegalArgumentException("Field type not valid '${type}'")
            }
        }
        
        return result
    }
}
