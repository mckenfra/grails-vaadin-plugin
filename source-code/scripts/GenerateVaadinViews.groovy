/**
 * Gant script that generates the Vaadin CRUD views for a given domain class
 * 
 * Based on GenerateViews script in Grails Core.
 *
 * @author Francis McKenzie
 */

includeTargets << grailsScript("_GrailsCreateArtifacts")
includeTargets << new File("${vaadinPluginDir}/scripts/_GenerateVaadin.groovy")

target ('default': "Generates the Vaadin CRUD views for a specified domain class") {
    depends(checkVersion, parseArguments, packageApp)
    promptForName(type: "Domain Class")
    generateController = false
    generateForName = argsMap["params"][0]
    generateForOne()
}
