@artifact.package@import org.grails.plugin.vaadin.VaadinApplication
import org.grails.plugin.vaadin.ui.DefaultWindow

class @artifact.name@ extends VaadinApplication {
    /**
     * Entry point for application
     */
    void init() {
        // CSS Styling
        setTheme("main")
        
        // Attach top-level window
        this.mainWindow = new DefaultWindow("Grails")
    }
}
