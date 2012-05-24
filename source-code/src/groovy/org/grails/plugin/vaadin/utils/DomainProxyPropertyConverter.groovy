package org.grails.plugin.vaadin.utils

import org.apache.commons.lang.ArrayUtils;

import com.vaadin.data.Property;

/**
 * Wraps an underyling value in a proxy that implements the equals() method
 * 
 * @author Francis McKenzie
 */
@SuppressWarnings("unchecked")
protected class DomainProxyPropertyConverter extends PropertyConverter {
    String equalsProperty
    
    /**
     * Initialise empty
     */
    public DomainProxyPropertyConverter() { super() }
    
    /**
     * Initialise with data source
     */
    public DomainProxyPropertyConverter(Property propertyDataSource, String equalsProperty) {
        super(propertyDataSource)
        this.equalsProperty = equalsProperty
    }
    
    /**
     * Converts value to proxy
     */
    @Override
    public Object convert(Object value) {
        if (value instanceof Collection || value?.class?.array) {
            value = value.collect { it instanceof DomainProxy ? it : new DomainProxy(equalsProperty).wrap(it) }
        } else if (value && ! (value instanceof DomainProxy)) {
            value = new DomainProxy(equalsProperty).wrap(value)
        }
        return value
    }

    /**
     * Restores value from proxy
     */
    @Override
    public Object restore(Object value) throws Exception {
        // Remove proxy
        if (value instanceof DomainProxy) {
            value = value.getAdaptee()
        } else if (value instanceof Collection || value?.class?.array) {
            value = value.collect { it instanceof DomainProxy ? it.getAdaptee() : it }
        }
        return value
    }
    
    /**
     * Returns class of dataSource or Object if none exists
     */
    @Override
    public Class getType() {
        return dataSource ? dataSource.type : Object
    }
}
