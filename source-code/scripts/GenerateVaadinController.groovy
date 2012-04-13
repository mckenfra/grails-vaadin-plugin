/**
 * Gant script that generates the Vaadin CRUD controller for a given domain class
 * 
 * Based on GenerateController script in Grails Core.
 *
 * @author Francis McKenzie
 */

includeTargets << grailsScript("_GrailsCreateArtifacts")
includeTargets << new File("${vaadinPluginDir}/scripts/_GenerateVaadin.groovy")

target ('default': "Generates the Vaadin CRUD controller for a domain class") {
    depends(checkVersion, parseArguments, packageApp)
    promptForName(type: "Domain Class")
    generateViews = false
    generateForName = argsMap["params"][0]
    generateForOne()
}