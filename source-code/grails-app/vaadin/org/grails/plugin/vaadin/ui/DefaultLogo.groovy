package org.grails.plugin.vaadin.ui

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Window;

/**
 * Displays the grails header logo.
 * <p>
 * Note that the layout can be customised by modifying the HTML
 * in web-app/VAADIN/themes/{theme}/layouts/logo.html
 * <p>
 * You can also specify a different HTML layout to use, in the constructor
 *
 * @author Francis McKenzie
 */
class DefaultLogo extends CustomLayout {
    /**
     * Create the layout using the default 'logo.html' page
     * in web-app/VAADIN/themes/{theme}/layouts
     */
    public DefaultLogo() {
        this("logo")
    }
    
    /**
     * Create the layout using the specified layout HTML page
     * in web-app/VAADIN/themes/{theme}/layouts
     */
    public DefaultLogo(String layoutName) {
        super(layoutName)

        // Create UI
        buildLayout()
    }

    /**
     * Subclasses may override to customise layout
     */
    protected void buildLayout() {
        // Empty
    }
}
