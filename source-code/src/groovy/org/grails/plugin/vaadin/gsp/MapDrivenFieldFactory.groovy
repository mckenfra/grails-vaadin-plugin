package org.grails.plugin.vaadin.gsp

import com.vaadin.data.Item;
import com.vaadin.ui.Component;
import com.vaadin.ui.DefaultFieldFactory;
import com.vaadin.ui.Field;
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
        // Use the super class to create a suitable field based on the
        // property type.
        Field f = super.createField(item, propertyId, uiContext)
        
        // Automatically set nullRepresentation for TextFields
        if (f instanceof TextField) {
            f.nullRepresentation = ""
        }
        
        if (propertyId) {
            def fieldProps = fields[propertyId]
            if (fieldProps) {
                fieldProps.each { k,v ->
                    f."${k}" = (v == "false" ? false : (v == "true" ? true : v)) 
                }
            }
        }
        
        return f
    }
}
