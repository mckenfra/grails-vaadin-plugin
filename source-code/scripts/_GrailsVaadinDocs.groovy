/**
 * Enhancing Grails's (2.0.4) doc command to allow configurable
 * links to external API's in groovy docs. 
 * 
 * @author Francis McKenzie
 */

import org.apache.tools.ant.types.Path
 
includeTargets << grailsScript("_GrailsDocs")

apiLinks = [:]

/**
 * Overriding this target from _GrailsDocs, because we need
 * the call to groovydoc to include the links to the external
 * apis 
 */
target(groovydoc:"Produces groovydoc documentation") {
    depends(parseArguments, setupDoc, createConfig)

    if (docsDisabled()) {
        event("DocSkip", ['groovydoc'])
        return
    }

    ant.taskdef(name:"groovydoc", classname:"org.codehaus.groovy.ant.Groovydoc")
    event("DocStart", ['groovydoc'])

    def sourcePath = new Path(ant.project)
    for (dir in projectCompiler.srcDirectories) {
        sourcePath.add new Path(ant.project, dir)
    }

    if (isPluginProject) {
        def pluginDescriptor = grailsSettings.baseDir.listFiles().find { it.name.endsWith "GrailsPlugin.groovy" }
        def tmpDir = new File(grailsSettings.projectWorkDir, "pluginDescForDocs")
        tmpDir.deleteOnExit()

        // Copy the plugin descriptor to a temporary directory and add that
        // directory to groovydoc's source path. This is because adding '.'
        // will cause all Groovy files in the project to be included as source
        // files (including test cases) and it will also cause duplication
        // of classes in the generated docs - see
        //
        //     http://jira.grails.org/browse/GRAILS-6530
        //
        // Also, we can't add a single file to the path. Only directories
        // seem to work. There are quite a few limitations with the GroovyDoc
        // task currently.
        ant.copy file: pluginDescriptor, todir: tmpDir, overwrite: true
    }

    // Prepare links
    readApiLinks()

    try {
        ant.groovydoc(destdir:groovydocDir, sourcepath:sourcePath, use:"true",
                      windowtitle:grailsAppName,'private':"true") {
            apiLinks?.each { k,v ->
                link(packages:"${k}.", href:"${v}")
            }
        }
    }
    catch(Exception e) {
        event("StatusError", ["Error generating groovydoc: ${e.message}"])
    }
    event("DocEnd", ['groovydoc'])
}

def readApiLinks() {
    if (!config?.grails?.doc?.api) return
    def apiLinksFromConfig = config.grails.doc.api.flatten()
    if (apiLinks == null) apiLinks = apiLinksFromConfig
    else apiLinks.putAll(apiLinksFromConfig)
}
