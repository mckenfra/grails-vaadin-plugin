package org.grails.plugin.vaadin;

/**
 * Annotation for disabling automatic injection of the Vaadin API
 * into what would otherwise appear to be a Vaadin class.
 * <p>
 * A class is considered a Vaadin class if it meets the following requirements:
 * <ul>
 * <li>Exists under the <code>grails-app</code> directory</li>
 * <li>Has a name ending <code>VaadinController</code> or has the word <code>vaadin</code> in its package name, or the package name of any of its superclasses.</li>
 * </ul>
 * 
 * @author Francis McKenzie
 */
public @interface NoVaadinApi {}
