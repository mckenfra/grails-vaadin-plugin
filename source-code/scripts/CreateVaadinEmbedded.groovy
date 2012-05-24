/**
 * Gant script that creates a Vaadin embedded controller and view
 *
 * @author Francis McKenzie
 */

includeTargets << grailsScript("_GrailsInit")
includeTargets << grailsScript("_GrailsCreateArtifacts")

target ('default': "Creates a new embedded controller and view") {
    depends(checkVersion, parseArguments)

    def type = "Controller"
    promptForName(type: type)

    for (name in argsMap["params"]) {
        name = purgeRedundantArtifactSuffix(name, type)
        createArtifact(name: name, suffix: type, type: type, path: "grails-app/controllers")

        def viewsDir = "${basedir}/grails-app/views/${propertyName}"
        def view = "${viewsDir}/index.gsp"
        ant.mkdir(dir:viewsDir)
        ant.copy(file:"${vaadinPluginDir}/src/templates/vaadin/artifacts/index-embedded.gsp", tofile:view)
        event("CreatedFile", [view])
        
        createUnitTest(name: name, suffix: type, superClass: "ControllerUnitTestCase")
    }

}
