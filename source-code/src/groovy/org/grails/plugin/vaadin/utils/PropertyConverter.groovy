package org.grails.plugin.vaadin.utils

import com.vaadin.data.Property;
import com.vaadin.data.Property.ConversionException;
import com.vaadin.data.Property.ReadOnlyException;
import com.vaadin.data.util.AbstractProperty;

/**
 * Copy of <a href="http://vaadin.com/api/com/vaadin/data/util/PropertyFormatter.html">PropertyFormatter</a>
 * <p>
 * However, while PropertyFormatter requires the type to be String, this class is more general-purpose,
 * allowing any type to be converted to any type.
 * <p>
 * In fact, PropertyFormatter could easily be rewritten to subclass this class.
 * 
 * @author Francis McKenzie
 */
@SuppressWarnings("serial")
public abstract class PropertyConverter<ORIGINAL,CONVERTED> extends AbstractProperty implements
        Property.Viewer, Property.ValueChangeListener,
        Property.ReadOnlyStatusChangeListener {

    /** Datasource that stores the actual value. */
    Property dataSource;

    /**
     * Construct a new {@code PropertyConverter} that is not connected to any
     * data source. Call {@link #setPropertyDataSource(Property)} later on to
     * attach it to a property.
     * 
     */
    protected PropertyConverter() {
    }

    /**
     * Construct a new converter that is connected to given data source. Calls
     * {@link #format(Object)} which can be a problem if the converter has not
     * yet been initialized.
     * 
     * @param propertyDataSource
     *            to connect this property to.
     */
    public PropertyConverter(Property propertyDataSource) {

        setPropertyDataSource(propertyDataSource);
    }

    /**
     * Gets the current data source of the converter, if any.
     * 
     * @return the current data source as a Property, or <code>null</code> if
     *         none defined.
     */
    public Property getPropertyDataSource() {
        return dataSource;
    }

    /**
     * Sets the specified Property as the data source for the converter.
     * 
     * <p>
     * Remember that new data sources getValue() must return objects that are
     * compatible with convert() and restore() methods.
     * </p>
     * 
     * @param newDataSource
     *            the new data source Property.
     */
    public void setPropertyDataSource(Property newDataSource) {

        boolean readOnly = false;
        ORIGINAL prevValue = null;

        if (dataSource != null) {
            if (dataSource instanceof Property.ValueChangeNotifier) {
                ((Property.ValueChangeNotifier) dataSource)
                        .removeListener(this);
            }
            if (dataSource instanceof Property.ReadOnlyStatusChangeListener) {
                ((Property.ReadOnlyStatusChangeNotifier) dataSource)
                        .removeListener(this);
            }
            readOnly = isReadOnly();
            prevValue = getValue();
        }

        dataSource = newDataSource;

        if (dataSource != null) {
            if (dataSource instanceof Property.ValueChangeNotifier) {
                ((Property.ValueChangeNotifier) dataSource)
                        .addListener((Property.ValueChangeListener) this);
            }
            if (dataSource instanceof Property.ReadOnlyStatusChangeListener) {
                ((Property.ReadOnlyStatusChangeNotifier) dataSource)
                        .addListener((Property.ReadOnlyStatusChangeListener) this);
            }
        }

        if (isReadOnly() != readOnly) {
            fireReadOnlyStatusChange();
        }
        String newVal = getValue();
        if ((prevValue == null && newVal != null)
                || (prevValue != null && !prevValue.equals(newVal))) {
            fireValueChange();
        }
    }

    /* Documented in the interface */
    @SuppressWarnings("unchecked")
    public Class getType() {
        return CONVERTED.class;
    }

    /**
     * Get the converted value.
     * 
     * @return If the datasource returns null, this is null. Otherwise this is
     *         CONVERTED object given by convert().
     */
    public Object getValue() {
        if (dataSource == null) {
            return null;
        }
        Object value = dataSource.getValue();
        if (value == null) {
            return null;
        }
        return convert(value);
    }

    /**
     * Get the string representation of the converted value.
     * 
     * @return If the datasource returns null, this is null. Otherwise this is
     *         String given by convert().toString().
     */
    @Override
    public String toString() {
        Object value = getValue();
        return value == null ? null : value.toString()
    }

    /** Reflects the read-only status of the datasource. */
    @Override
    public boolean isReadOnly() {
        return dataSource == null ? false : dataSource.isReadOnly();
    }

    /**
     * This method must be implemented to convert the values received from
     * DataSource.
     * 
     * The method is required to assure that convert(restore(x)) equals x.
     * 
     * @param value
     *            Value object got from the datasource. This is guaranteed to be
     *            non-null and of the type compatible with getType() of the
     *            datasource.
     * @return
     */
    abstract public CONVERTED convert(Object value);

    /**
     * Parse object and convert it to type compatible with datasource.
     * 
     * The method is required to assure that restore(convert(x)) equals x.
     * 
     * @param value
     *            This is guaranteed to be non-null object.
     * @return Non-null value compatible with datasource.
     * @throws Exception
     *             Any type of exception can be thrown to indicate that the
     *             conversion was not successful.
     */
    abstract public ORIGINAL restore(Object value) throws Exception;

    /**
     * Sets the Property's read-only mode to the specified status.
     * 
     * @param newStatus
     *            the new read-only status of the Property.
     */
    @Override
    public void setReadOnly(boolean newStatus) {
        if (dataSource != null) {
            dataSource.setReadOnly(newStatus);
        }
    }

    public void setValue(Object newValue) throws ReadOnlyException,
            ConversionException {
        if (dataSource == null) {
            return;
        }
        if (newValue == null) {
            if (dataSource.getValue() != null) {
                dataSource.setValue(null);
                fireValueChange();
            }
        } else {
            try {
                dataSource.setValue(restore(newValue));
                if (!newValue.equals(getValue())) {
                    fireValueChange();
                }
            } catch (ConversionException e) {
                throw e;
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }
    }

    /**
     * Listens for changes in the datasource.
     * 
     * This should not be called directly.
     */
    public void valueChange(com.vaadin.data.Property.ValueChangeEvent event) {
        fireValueChange();
    }

    /**
     * Listens for changes in the datasource.
     * 
     * This should not be called directly.
     */
    public void readOnlyStatusChange(
            com.vaadin.data.Property.ReadOnlyStatusChangeEvent event) {
        fireReadOnlyStatusChange();
    }

}
