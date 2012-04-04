package org.grails.plugin.vaadin;

/**
 * Annotation for disabling automatic injection of the Vaadin API
 * into what would otherwise appear to be a Vaadin class.
 * <p>
 * Note that a class is considered a Vaadin class if it:
 * <ul>
 * <li>Has 'vaadin' in the package name of itself, or any of its superclasses.</li>
 * <li>Has 'VaadinController' or 'VaadinView' at the end of the class name.</li>
 * </ul>
 * 
 * @author Francis McKenzie
 */
public @interface NoVaadinApi {}
