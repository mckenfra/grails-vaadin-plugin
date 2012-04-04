/**
 * Gant script that creates a new Grails Vaadin controller
 *
 * @author Francis McKenzie
 */

includeTargets << grailsScript("_GrailsInit")
includeTargets << grailsScript("_GrailsCreateArtifacts")

target ('default': "Creates a new Vaadin view") {
    depends(checkVersion, parseArguments)

    def type = "VaadinView"
    promptForName(type: type)

    for (name in argsMap["params"]) {
        name = purgeRedundantArtifactSuffix(name, type)
        createArtifact(name: name, suffix: type, type: type, path: "grails-app/vaadin")
    }

}
