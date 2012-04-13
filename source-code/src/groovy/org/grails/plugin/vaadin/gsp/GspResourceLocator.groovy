package org.grails.plugin.vaadin.gsp

import javax.servlet.ServletContext;

import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageStaticResourceLocator
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageScriptSource
import org.springframework.core.io.Resource
import org.springframework.web.context.ServletContextAware;

/**
 * For finding GSP files, which can be views, templates or resources.
 *
 * @author Francis McKenzie
 */
class GspResourceLocator implements ServletContextAware {
    def log = LogFactory.getLog(this.class)
    
    static GSP_FILE_EXTENSIONS = ['.gsp']

    // Injected
    GrailsConventionGroovyPageLocator groovyPageLocator
    GroovyPageStaticResourceLocator grailsResourceLocator
    ServletContext servletContext
    
    /**
     * Checks for .gsp suffix in filename or uri
     */
    public boolean isGsp(fileOrUri) {
        def name = fileOrUri?.toString()?.toLowerCase()
        return name && GSP_FILE_EXTENSIONS.any { name.endsWith(it) }
    }

    /**
     * Removes .gsp suffix from filename
     */
    public String generateCompiledFilenameFromOriginal(filename) {
        String result = filename?.toString()
        if (result) {
            GSP_FILE_EXTENSIONS.each { ext ->
                result = result.replaceAll(/(?i)\Q${ext}\E/, '')
            }
        }
        return result
    }
    
    /**
     * Finds a GSP file, using grailsResourceLocator and groovyPageLocator.
     * 
     * Note that the GSP may be a view, a template or a resource. This method tries
     * to find the GSP by searching those 3 types in that order. If a GSP is found,
     * a map is returned with the following elements:
     * 
     *   uri:  uri String of GSP
     *   file: java File of GSP
     *   type: either 'view', 'template' or 'resource'
     *    
     * If no GSP found, an empty map is returned.
     * 
     * @param uri The external URI of the GSP
     * @return Map of results (empty if no GSP found)
     */
    public Map findGsp(uri) {
        Map result = [:]
        if (uri) {
            File gsp;
            if ( (gsp = findGspViewFile(uri)) ) {
                result = [uri:uri.toString(), file:gsp, type:'view']
            } else if ( (gsp = findGspTemplateFile(uri)) ) {
                result = [uri:uri.toString(), file:gsp, type:'template']
            } else if ( (gsp = findGspResourceFile(uri)) ) {
                result = [uri:uri.toString(), file:gsp, type:'resource']
            }
        }
        return result
    }
    
    /**
     * Finds a GSP resource file, using grailsResourceLocator.
     **/
    protected File findGspResourceFile(uri) {
        if (log.isDebugEnabled()) {
            log.debug "Finding GSP Resource ${uri} ..."
        }
        File result;
        try {
            Resource resource = grailsResourceLocator.findResourceForURI(uri)
            if (resource) {
                if (log.isDebugEnabled()) {
                    log.debug "Checking if is a GSP: ${resource.file}"
                }
                
                // Check if GSP
                File file = resource.file
                if (isGsp(file)) {
                    result = file
                }
            }
        } catch(err) {
            log.warn "Error finding GSP resource ${uri} - ${err}"
        }
        return result
    }
    
    /**
     * Finds a GSP view file, using groovyPageLocator.
     **/
    protected File findGspViewFile(uri) {
        if (log.isDebugEnabled()) {
            log.debug "Finding GSP View ${uri} ..."
        }
        File result;
        try {
            GroovyPageScriptSource resource = groovyPageLocator.findViewByPath(uri.toString())
            if (resource) {
                if (log.isDebugEnabled()) {
                    log.debug "Checking if is a GSP: ${resource.URI} ..."
                }
                
                // Check if GSP
                File file = new File(servletContext.getRealPath(resource.URI))
                if (isGsp(file)) {
                    result = file
                }
            }
        } catch(err) {
            log.warn "Error finding GSP view ${uri} - ${err}"
        }
        return result
    }

    /**
     * Finds a GSP template file, using groovyPageLocator.
     **/
    protected File findGspTemplateFile(uri) {
        if (log.isDebugEnabled()) {
            log.debug "Finding GSP Template ${uri} ..."
        }
        File result;
        try {
            GroovyPageScriptSource resource = groovyPageLocator.findTemplateByPath(uri.toString())
            if (resource) {
                if (log.isDebugEnabled()) {
                    log.debug "Checking if is a GSP: ${resource.URI} ..."
                }

                // Check if GSP
                File file = new File(servletContext.getRealPath(resource.URI))
                if (isGsp(file)) {
                    result = file
                }
            }
        } catch(err) {
            log.warn "Error finding GSP template ${uri} - ${err}"
        }
        return result
    }
}
