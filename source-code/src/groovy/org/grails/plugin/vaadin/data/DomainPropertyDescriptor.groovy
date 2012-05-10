package org.grails.plugin.vaadin.data

import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;

import com.vaadin.data.Property;
import com.vaadin.data.util.AbstractProperty;
import com.vaadin.data.util.VaadinPropertyDescriptor;

/**
 * A property descriptor that allows properties nested to arbitrary
 * depth for a domain class.
 * <p>
 * For example, for domain class Book, you could have property
 * <code>author.sister.husband.address.street</code>
 * <p>
 * Also allows separate properties to hold a common map of all changed properties
 * for the domain instance. When each property sets its value on the domain instance,
 * it calls <code>domainInstance.properties = changedProperties</code>.
 * <p>
 * This ensures that spring errors are not lost for other properties, whenever we set a value
 * on a particular property.
 * 
 * @author Francis McKenzie
 */
class DomainPropertyDescriptor<DT> implements VaadinPropertyDescriptor<DT> {
    /**
     * The domain class
     */
    protected GrailsDomainClass domainClass
    /**
     * The dot-separated name split into its component parts
     */
    protected String[] names
    /**
     * The final, most-nested property
     */
    protected GrailsDomainClassProperty property
    
    /**
     * Initialise with simple property.
     * <p>
     * I.e. no nesting, just use a domain class's direct property to
     * initialise this class.
     * 
     * @param property The domain class's direct property to use.
     */
    public DomainPropertyDescriptor(GrailsDomainClassProperty property) {
        this(property.domainClass, property.name)
    }
    
    /**
     * Initialise with a domain class and an arbitrarily-nested property name.
     * <p>
     * Note this will throw an exception if the property name is invalid
     * for the specified domain class.
     * 
     * @param domainClass The domain class to which the property name applies
     * @param propertyName The arbitrarily-nested property name of this domain class.
     */
    public DomainPropertyDescriptor(GrailsDomainClass domainClass, String propertyName) {
        if (!domainClass || !propertyName) {
            throw new IllegalArgumentException("Domain class and property name required!")
        }
        this.domainClass = domainClass
        this.names = propertyName.split(/\./)
        this.property = domainClass.getPropertyByName(names[0])
        if (names.length > 1) {
            this.property = names[1..names.length-1].inject(property) { prop, name ->
                if (prop?.embedded) {
                    prop.component.getPropertyByName(name)
                } else {
                    prop?.referencedDomainClass?.getPropertyByName(name)
                }
            }
        }
        if (! this.property) {
            throw new IllegalArgumentException("No such property '${propertyName}' for ${domainClass}")
        }
    }
    
    /**
     * The dot-separated name
     */
    public String getName() {
        return this.names.join('.')
    }

    /**
     * The type of the final property
     */
    public Class<?> getPropertyType() {
        return this.property.type
    }

    /**
     * Creates a new property for the specified instance
     */
    public Property createProperty(DT instance) {
        return createProperty(instance, [:])
    }
    
    /**
     * Creates a new property for the specified instance, using the specified
     * map to hold all changed properties for the instance.
     * <p>
     * Whenever a field value is changed, all the properties are re-applied to the domain
     * instance using <code>domainInstance.properties = changedProperties</code>.
     * <p>
     * This ensures that spring errors are not lost for other properties, whenever we set a value
     * on a particular property.
     */
    public Property createProperty(DT instance, Map changedProperties) {
        return new DomainProperty(instance, changedProperties)
    }
    
    /**
     * Accesses the property of the specified instance using the
     * details from the enclosing descriptor class.
     * 
     * @author Francis McKenzie
     */
    protected class DomainProperty<DT> extends AbstractProperty {
        protected DT instance
        protected Map changedProperties
        
        /**
         * Construct the property with the specified domain instance and map for pooling
         * multiple-properties' values.
         * 
         * @param instance The domain instance
         * @param changedProperties The 'pool' of changed values for the domain instance
         */
        public DomainProperty(DT instance, Map changedProperties) {
            super()
            this.instance = instance
            this.changedProperties = changedProperties != null ? changedProperties : [:]
        }
        
        /**
         * The fully-qualified name of the property of the domain instance
         */
        public String getName() {
            return DomainPropertyDescriptor.this.name
        }
        
        /**
         * The value of the property of the domain instance
         */
        public Object getValue() {
            return names.inject(instance) { obj, prop -> obj?."${prop}" }
        }

        /**
         * The value of the property of the domain instance.
         * <p>
         * Note setting is done using <code>domainInstance.properties = changedProperties</code>
         * as this takes advantage of Grails's built-in validation and error-handling.
         */
        public void setValue(Object newValue) throws Property.ReadOnlyException, Property.ConversionException {
            changedProperties[name] = newValue
            instance.properties = changedProperties
        }

        /**
         * The type of the property
         */
        public Class<?> getType() {
            return DomainPropertyDescriptor.this.propertyType
        }
    } 
}
