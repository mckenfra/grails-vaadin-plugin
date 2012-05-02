package org.grails.plugin.vaadin.utils

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
    
    public DefaultValuePropertyConverter(Object defaultValue) {
        this.defaultValue = defaultValue
    }
    
    @Override
    public Object convert(Object value) {
        return !readOnly && value == null & defaultValue != null ? defaultValue : value
    }

    @Override
    public Object restore(Object value) throws Exception {
        defaultValue = null
        return value
    }

}
