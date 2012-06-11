package org.grails.plugin.vaadin

/**
 * Controller for displaying default error page.
 * <p>
 * End users can create a controller with the same name in a project
 * if, for example, they want to redirect to another controller
 * as the error page.
 *
 * @author Francis McKenzie
 */
class ErrorVaadinController {
    /**
     * Renders the default error page <code>/vaadin/error</code>
     */
    def index() {
        render view:'/vaadin/error', model:[exception:params.exception]
    }
}
