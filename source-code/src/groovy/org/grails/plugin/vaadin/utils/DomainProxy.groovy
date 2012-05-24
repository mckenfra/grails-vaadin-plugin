package org.grails.plugin.vaadin.utils

/**
 * For adding equals() and hashCode() methods to domain classes using
 * a proxy technique that leaves the domain class unchanged.
 *
 * @author Francis McKenzie
 */
class DomainProxy extends groovy.util.Proxy {
    private equalsProperty
    
    /**
     * Create the proxy using the specified property of the domain
     * class for the equals() and hashCode methods()
     * 
     * @param equalsProperty The property of the domain class to use
     * for the equals() and hashCode() methods.
     */
    public DomainProxy(equalsProperty) {
        this.equalsProperty = equalsProperty
    }
    
    /**
     * Overridden to use the specified property.
     */
    public boolean equals(other) {
        if (other == null) return false
        def myVal = getAdaptee()."${equalsProperty}"
        def otherVal = other."${equalsProperty}"
        if (myVal == null || otherVal == null) return myVal == null && otherVal == null
        return myVal.equals(otherVal)
    }
    
    /**
     * Overridden to use the specified property.
     */
    public int hashCode() {
        return getAdaptee()."${equalsProperty}"?.hashCode()
    }
    
    /**
     * Ensures all properties are proxied, not just methods
     */
    public Object getProperty(String propertyName) { getAdaptee().getProperty(propertyName) }
    /**
     * Ensures all properties are proxied, not just methods
     */
    public void setProperty(String propertyName, Object value) { getAdaptee().setProperty(propertyName, value) }
}
