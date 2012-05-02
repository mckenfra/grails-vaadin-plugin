/**
 * Ensures Vaadin URIs are not handled by Grails's dispatcher.
 * 
 * @author Francis McKenzie
 */
class VaadinPluginUrlMappings {
    static excludes = ["/VAADIN/*"]
    static mappings = {}
}
