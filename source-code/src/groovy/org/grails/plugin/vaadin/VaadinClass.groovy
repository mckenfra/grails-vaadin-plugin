package org.grails.plugin.vaadin

/**
 * A Vaadin Artefact, e.g. a VaadinController or VaadinView.
 * 
 * @author Francis McKenzie
 */
class VaadinClass {
    /**
     * The type name of the artefact - e.g. 'controller' or 'view'
     */
    protected String type
    /**
     * The referenced class of the artefact - e.g. BookVaadinController
     */
    protected Class clazz
    /**
     * The logical property name of the artefact - e.g. 'book'
     */
    protected String logicalPropertyName
    
    /**
     * Construct the Vaadin artefact using the specified parameters.
     * 
     * @param type The type name of the artefact - e.g. 'controller' or 'view'
     * @param clazz The referenced class of the artefact - e.g. BookVaadinController
     * @param logicalPropertyName The logical property name of the artefact - e.g. 'book'
     */
    public VaadinClass(String type, Class clazz, String logicalPropertyName) {
        this.type = type
        this.clazz = clazz
        this.logicalPropertyName = logicalPropertyName
    }
    
    /**
     * Get the type name of the class - e.g. 'controller' or 'view'
     */
    public String getType() { type }
    /**
     * Get the referenced class of the artefact - e.g. BookVaadinController
     */
    public Class getClazz() { clazz }
    /**
     * Get the logical property name of the artefact - e.g. 'book'
     */
    public String getLogicalPropertyName() { logicalPropertyName }
    
    /**
     * A string representation of the artefact
     */
    public String toString() {
        return "[type:'${type}', clazz:'${clazz}', logicalPropertyName:'${logicalPropertyName}']"
    }
    
    /**
     * For proper behaviour in collections
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof VaadinClass && this.clazz.equals(other.clazz)
    }
    
    /**
     * For proper behaviour in collections
     */
    @Override
    public int hashCode() {
        return this.clazz.hashCode()
    }
}
