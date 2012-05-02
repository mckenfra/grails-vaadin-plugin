import org.apache.commons.logging.LogFactory

/**
 * Handles the issue that Grails thinks Vaadin controllers are regular Grails controllers.
 * <p> 
 * Redirects /*Vaadin/* to /[vaadin-app]#controller
 * 
 * @author Francis McKenzie
 */
class VaadinPluginFilters {
    def log = LogFactory.getLog('org.grails.plugin.vaadin.VaadinFilters')
    
    // Injected
    def grailsApplication
    
    def filters = {
        vaadinRedirect(controller: '*Vaadin') {
            before = {
                def vaadinPath = grailsApplication.config.vaadin?.contextRelativePath ?: ''
                def slash = vaadinPath && !(vaadinPath.startsWith('/')) ? '/' : ''
                def controller = controllerName - 'Vaadin'
                def action = actionName
                def queryString = request.queryString ?: ""
                def uri = "${slash}${vaadinPath}"
                def fragment = "${controller}/${action}${queryString}"
                def uriWithFragment = "${uri}#${fragment}"
                if (log.isDebugEnabled()) {
                    log.debug "REDIRECT: ${request.requestURI} -> ${uriWithFragment}"
                }
                redirect(uri:uriWithFragment)
                return false
            }
        }
    }
}
