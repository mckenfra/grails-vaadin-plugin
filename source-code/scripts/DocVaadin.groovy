/**
 * Enhancing Grails's (2.0.4) doc command to allow configurable
 * links to external API's in groovy docs. 
 * 
 * @author Francis McKenzie
 */
 
includeTargets << new File("${vaadinPluginDir}/scripts/_GrailsVaadinDocs.groovy")

setDefaultTarget("docs")
