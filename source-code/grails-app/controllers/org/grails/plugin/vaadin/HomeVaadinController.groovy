package org.grails.plugin.vaadin

/**
 * Controller for displaying default application home page.
 * <p>
 * There should be no need to create a controller with the same
 * name in a project, as this class always simply displays the
 * "index" view.  
 * 
 * @author Francis McKenzie
 */
class HomeVaadinController {
    def index() {
        render view:"/vaadin/index"
    }
}
