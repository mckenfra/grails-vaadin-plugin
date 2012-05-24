/**
 * Gant script that creates a new Grails Vaadin controller
 *
 * @author Francis McKenzie
 */

includeTargets << grailsScript("_GrailsInit")
includeTargets << grailsScript("_GrailsCreateArtifacts")

target ('default': "Creates a new Vaadin controller") {
    depends(checkVersion, parseArguments)

    def type = "VaadinController"
    promptForName(type: type)

    for (name in argsMap["params"]) {
        name = purgeRedundantArtifactSuffix(name, type)
        createArtifact(name: name, suffix: type, type: type, path: "grails-app/controllers", templatePath: "templates/vaadin/artifacts")

        def viewsDir = "${basedir}/grails-app/views/vaadin/${propertyName}"
        ant.mkdir(dir:viewsDir)
        event("CreatedFile", [viewsDir])
        
        createUnitTest(name: name, suffix: type, superClass: "ControllerUnitTestCase")
    }

}
