/**
 * Gant script that generates a Vaadin CRUD controller and matching views for a given domain class,
 * or all domain classes in an app.
 * 
 * Based on GenerateAll script in Grails Core.
 *
 * @author Francis McKenzie
 */

includeTargets << grailsScript("_GrailsCreateArtifacts")
includeTargets << new File("${vaadinPluginDir}/scripts/_GenerateVaadin.groovy")

generateViews = true
generateController = true

target ('default': "Generates a Vaadin CRUD interface (controller + views) for one or all domain classes") {
    depends(checkVersion, parseArguments, packageApp)
    promptForName(type: "Domain Class")

    try {
        def name = argsMap["params"][0]
        if (!name || name == "*") {
            uberGenerate()
        }
        else {
            generateForName = name
            generateForOne()
        }
    }
    catch (Exception e) {
        logError("Error running generate-vaadin-all", e)
        exit(1)
    }
}
