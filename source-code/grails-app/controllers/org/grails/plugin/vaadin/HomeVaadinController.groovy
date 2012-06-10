package org.grails.plugin.vaadin

/**
 * Controller for displaying default application home page.
 * <p>
 * End users can create a controller with the same name in a project
 * if, for example, they want to redirect to another controller
 * as the home page.
 * 
 * @author Francis McKenzie
 */
class HomeVaadinController {
    /**
     * Renders the default home page <code>/vaadin/index</code>
     */
    def index() {
        render view:"/vaadin/index"
    }
}
