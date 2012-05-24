package org.grails.plugin.vaadin.utils

import com.vaadin.data.Property;

/**
 * Does no conversion, but substitutes the specified default value
 * in place of a null, ONLY when getting the value.
 * <p>
 * As soon as any value is set on the field, the default value
 * ceases to be used from then onwards.
 * 
 * @author Francis McKenzie
 */
@SuppressWarnings("unchecked")
class DefaultValuePropertyConverter extends PropertyConverter {
    def defaultValue
    
    public DefaultValuePropertyConverter(Property propertyDataSource, Object defaultValue) {
        super(propertyDataSource)
        this.defaultValue = defaultValue
    }
    
    /**
     * Substitutes the default value if the specified value is null and the property has not
     * yet been set. 
     */
    @Override
    public Object convert(Object value) {
        return !readOnly && value == null & defaultValue != null ? defaultValue : value
    }

    /**
     * Doesn't change the value, but notes that the property has now been set and therefore
     * the default value should no longer be used. 
     */
    @Override
    public Object restore(Object value) throws Exception {
        defaultValue = null
        return value
    }

}
