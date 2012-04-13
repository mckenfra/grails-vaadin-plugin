package org.grails.plugin.vaadin.gsp

/**
 * Holds details for configuring a Vaadin Component.
 * 
 * @author Francis McKenzie
 */
class GspComponentConfig {
    /**
     * The type of the configuration. For example, a configuration holding
     * a column definition for a Vaadin table has type 'column'. 
     */
    String type
    /**
     * The class of the Vaadin Component that this configuration applies to.
     * For example, com.vaadin.ui.Table
     */
    Class componentClass
    /**
     * A map containing the properties to apply to the Vaadin Component.
     */
    Map props = [:]
    
    /**
     * Constructs a new configuration for a specific Vaadin Component class.
     * 
     * @param componentClass The class of the Vaadin Component.
     * @param type The type of the configuration, for example 'column'
     * @param properties The configuration properties to be applied to the Vaadin Component.
     */
    public GspComponentConfig(Class componentClass, String type, Map properties) {
        this.componentClass = componentClass
        this.type = type
        this.props.putAll(properties)
    }
}
