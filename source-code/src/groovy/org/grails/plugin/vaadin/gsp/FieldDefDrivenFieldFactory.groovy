package org.grails.plugin.vaadin.gsp

import com.vaadin.data.Item;
import com.vaadin.ui.Component;
import com.vaadin.ui.DefaultFieldFactory;
import com.vaadin.ui.Field;

/**
 * A Vaadin <a href="http://vaadin.com/api/com/vaadin/ui/DefaultFieldFactory.html">DefaultFieldFactory</a>
 * that uses specified defs to create and configure the fields.
 * 
 * @author Francis McKenzie
 */
class FieldDefDrivenFieldFactory extends DefaultFieldFactory {
    /**
     * Creators for each named field
     */
    protected Map fieldDefs = [:]
    
    /**
     * Constructs a new FieldFactory using the specified fieldDefs
     * 
     * @param fieldDefs The list of fieldDefs to use to create the fields
     */
    public FieldDefDrivenFieldFactory(List<FieldDef> fieldDefs) {
        super()
        fieldDefs?.each {
            this.fieldDefs[it.name] = it
        }
    }
    
    /**
     * Creates a field by using the fieldDef for the specified propertyId
     * (i.e. field name). If the fieldDef returns null, then instead uses
     * the superclass createField() method to create a default field based
     * on property type.
     * <p>
     * Once created, uses the same fieldDef to configure the field.
     */
    @Override
    public Field createField(Item item, Object propertyId, Component uiContext) {
        
        // Create the field first
        Field field
        FieldDef fieldDef = fieldDefs[propertyId]
        if (fieldDef) {
            field = fieldDef.createField()
        }
        
        // Creation failed - default to using super class to create a suitable field
        // based on the property type
        if (!field) {
            field = super.createField(item, propertyId, uiContext)
        }
        
        // Configure the field
        if (fieldDef) {
            fieldDef.configureField(field, item)
        }
        
        return field
    }
}
