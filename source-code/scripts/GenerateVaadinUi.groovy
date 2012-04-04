/**
 * Gant script that generates a Vaadin CRUD controller and matching views for a given domain class
 * 
 * Based on GenerateAll script in Grails Core.
 *
 * @author Francis McKenzie
 */

includeTargets << grailsScript("_GrailsCreateArtifacts")
includeTargets << new File("${vaadinScaffoldPluginDir}/scripts/_GenerateVaadin.groovy")

target ('default': "Generates a Vaadin CRUD interface (controller + views) for a domain class") {
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
        logError("Error running vaadin-generate-ui", e)
        exit(1)
    }
}
