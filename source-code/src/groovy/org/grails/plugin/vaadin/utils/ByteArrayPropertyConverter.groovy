package org.grails.plugin.vaadin.utils

import org.apache.commons.lang.ArrayUtils;

import com.vaadin.data.Property;

/**
 * Autoboxes an underlying Byte[] to byte[].
 * <p>
 * Note that if original datasource type is not Byte[], then this class does nothing,
 * just passes through the value unchanged.
 * 
 * @author Francis McKenzie
 */
@SuppressWarnings("unchecked")
protected class ByteArrayPropertyConverter extends PropertyConverter {
    /**
     * Initialise empty
     */
    public ByteArrayPropertyConverter() { super() }
    
    /**
     * Initialise with data source
     */
    public ByteArrayPropertyConverter(Property propertyDataSource) { super(propertyDataSource) }
    
    /**
     * Converts value to byte[] if is Byte[], otherwise just returns value unchanged
     */
    @Override
    public Object convert(Object value) {
        if (value instanceof Byte[]) {
            return ArrayUtils.toPrimitive((Byte[]) value)
        } else {
            return value
        }
    }

    /**
     * Restores value to Byte[] if is byte[] and underlying datasource type is Byte[],
     * otherwise just returns value unchanged
     */
    @Override
    public Object restore(Object value) throws Exception {
        if (value instanceof byte[] && dataSource?.type == Byte[]) {
            return ArrayUtils.toObject((byte[]) value)
        } else {
            return value
        }
    }
    
    /**
     * If class of original datasource is Byte[] then returns byte[], else returns
     * original datasource's class unchanged.
     * <p>
     * If no original datasource is set, then returns byte[]
     */
    @Override
    public Class getType() {
        return dataSource && dataSource.type != Byte[] ? dataSource.type : byte[]
    }
}
