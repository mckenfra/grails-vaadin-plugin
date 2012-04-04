package org.grails.plugin.vaadin

import org.grails.plugin.vaadin.ui.DefaultHome

/**
 * View for displaying default application home page.
 * <p>
 * Users should create a view class with the same name
 * in their project. They can then construct a
 * customised home page.
 * 
 * @author Francis McKenzie
 */
class HomeVaadinView {
    def index() {
        application.mainWindow.body = new DefaultHome()
    }
}
