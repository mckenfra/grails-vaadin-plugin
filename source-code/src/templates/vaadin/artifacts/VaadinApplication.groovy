@artifact.package@import com.vaadin.ui.Window
import org.grails.plugin.vaadin.GrailsVaadinApplication

class @artifact.name@ extends GrailsVaadinApplication {
    /**
     * Entry point for application
     */
    void init() {
        // CSS Styling
        setTheme("main")
        
        // Attach top-level window
        this.mainWindow = new Window("Grails")
        this.mainWindow.content.setMargin(false)
    }
}
