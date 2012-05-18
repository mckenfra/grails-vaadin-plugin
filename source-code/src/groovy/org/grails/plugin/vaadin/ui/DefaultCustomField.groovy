package org.grails.plugin.vaadin.ui

import org.vaadin.addon.customfield.CustomField;

/**
 * An empty <a href="https://vaadin.com/directory#addon/customfield">CustomField</a>
 * that derives its type from the propertyDataSource
 * 
 * @author Francis McKenzie
 */
class DefaultCustomField extends CustomField {
    @Override
    public Class<?> getType() {
        return propertyDataSource?.type ?: Object
    }
}
