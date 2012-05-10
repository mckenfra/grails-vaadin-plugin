package org.grails.plugin.vaadin.data

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;

import com.vaadin.data.Buffered;
import com.vaadin.data.Property;
import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.util.MethodPropertyDescriptor;
import com.vaadin.data.util.NestedPropertyDescriptor;
import com.vaadin.data.util.PropertysetItem;
import com.vaadin.data.util.VaadinPropertyDescriptor;

/**
 * Similar to <a href="/com/vaadin/data/util/BeanItem.html">BeanItem</a> but
 * for <a href="api/org/codehaus/groovy/grails/commons/GrailsDomainClass.html">GrailsDomainClass</a>
 * instances.
 * <p>
 * Main benefits over BeanItem is that this class supports embedded property types,
 * and arbitrary nesting of properties.
 * 
 * @author Francis McKenzie
 */
class DomainItem extends PropertysetItem {
    /**
     * The domain instance of this item
     */
    final Object domainInstance
    /**
     * The domain class of the domain instance
     */
    final GrailsDomainClass domainClass
    /**
     * All changed properties for the domain instance. Whenever a field value is changed,
     * all the properties are re-applied to the domain instance using
     * <code>domainInstance.properties = changedProperties</code>.
     * <p>
     * This ensures that errors are not lost for other fields, whenever we set a value
     * on a particular field.
     */
    protected Map changedProperties
    
    /**
     * Create a domain item for the specified domain instance.
     * 
     * @param grailsApplication Required to obtain the domain instance's domain class.
     * @param domainInstance The domain instance for which the item will be created.
     */
    public DomainItem(GrailsApplication grailsApplication, Object domainInstance) {
        this((GrailsDomainClass) grailsApplication.getArtefact("Domain", domainInstance.class.name), domainInstance)
    }
       
    /**
     * Create a domain item for the specified domain instance with the specified
     * domain class.
     * 
     * @param domainClass The domain class of the specified instance.
     * @param domainInstance The domain instance for which the item will be created.
     */
    public DomainItem(GrailsDomainClass domainClass, Object domainInstance) {
        super()
        if (!domainClass) {
            throw new IllegalArgumentException("Domain class not found for instance ${domainInstance?.class}!")
        }
        if (!domainInstance) {
            throw new IllegalArgumentException("Instance of domain class ${domainClass} must not be null!")
        }
        this.domainClass = domainClass
        this.domainInstance = domainInstance
        this.changedProperties = [:]
        
        // Add the domain instance's default property descriptors to this item
        getPropertyDescriptors(domainClass).each { pd->
            addItemProperty(pd.name, pd.createProperty(domainInstance, changedProperties))
        }
    }
    
    /**
     * Gets the property with the specified name from the domainInstance
     * stored in this item. 
     * <p>
     * Note that the property may be arbitrarily-nested, for example
     * foo.bar.tick.tock
     * <p>
     * If the property is valid, and has not been accessed before, its name
     * is added to the list of property id's for this item.
     *
     * @param id the identifier (name) of the Property to get.
     * @return the Property with the given ID or <code>null</code>
     */
    public Property getItemProperty(Object id) {
        Property result = super.getItemProperty(id)
        if (result == null && !this.itemPropertyIds.contains("${id}")) {
            // Try to create a new property
            def pd = new DomainPropertyDescriptor(domainClass, "${id}")
            addItemProperty(pd.name, pd.createProperty(domainInstance, changedProperties))
        }
        return result
    }

    /**
     * Get GrailsDomainClass properties as VaadinPropertyDescriptors
     *
     * @param domainClass The Grails domain class to get properties for.
     * @return a list of property descriptors
     */
    protected List<DomainPropertyDescriptor> getPropertyDescriptors(GrailsDomainClass domainClass) {
        def result = []
 
        domainClass.properties.each { p->
            if (p.embedded) {
                p.component.properties.each { ep->
                    result << new DomainPropertyDescriptor(domainClass, "${p.name}.${ep.name}")
                }
            } else {
                result << new DomainPropertyDescriptor(p)
            }
        }
        
        return result
    }
}
