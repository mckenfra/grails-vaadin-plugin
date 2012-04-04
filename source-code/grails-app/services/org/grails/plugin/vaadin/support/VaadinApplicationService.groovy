package org.grails.plugin.vaadin.support

import com.vaadin.Application

/**
 * Holds a reference to the Vaadin Application in session scope.
 *
 * @author Francis McKenzie
 */
public class VaadinApplicationService {
    static scope = 'session'
    
    Application application
}
