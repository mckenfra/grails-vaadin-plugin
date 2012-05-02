package org.grails.plugin.vaadin.utils

import com.vaadin.data.Property;

/**
 * Makes a Calendar property look an behave like a Date property.
 * <p>
 * This allows a Calendar-type property to be used with the 
 * <a href="http://vaadin.com/api/com/vaadin/ui/DateField.html">DateField</a>
 * component, which only supports Date properties.
 * 
 * @author Francis McKenzie
 */
@SuppressWarnings("unchecked")
class CalendarPropertyConverter extends PropertyConverter {
    /**
     * Initialise empty
     */
    public CalendarPropertyConverter() { super() }
    
    /**
     * Initialise with data source
     */
    public CalendarPropertyConverter(Property propertyDataSource) { super(propertyDataSource) }
    
    /**
     * Converts value to Date if is Calendar, otherwise just returns value unchanged
     */
    @Override
    public Object convert(Object value) {
        if (value instanceof Calendar) {
            return value.time
        } else {
            return value
        }
    }

    /**
     * Restores value to Calendar if is Date and underlying datasource type is Calendar,
     * otherwise just returns value unchanged
     */
    @Override
    public Object restore(Object value) throws Exception {
        if (value instanceof Date && dataSource && Calendar.isAssignableFrom(dataSource?.type)) {
            def cal = dataSource.type.newInstance()
            cal.time = value
            return cal
        } else {
            return value
        }
    }

    /**
     * If class of original datasource is Calendar then returns Date, else returns
     * original datasource's class unchanged.
     * <p>
     * If no original datasource is set, then returns Date
     */
    @Override
    public Class getType() {
        return dataSource && !Calendar.isAssignableFrom(dataSource.type) ? dataSource.type : Date
    }
}
