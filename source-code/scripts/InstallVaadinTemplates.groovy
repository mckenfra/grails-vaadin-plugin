/**
 * Gant script that installs Vaadin artifact and scaffolding templates.
 * 
 * Based on InstallTemplates in Grails Core. 
 *
 * @author Francis McKenzie
 */

includeTargets << grailsScript("_GrailsInit")

target ('default': "Installs the Vaadin artifact and scaffolding templates") {
    depends(checkVersion, parseArguments)
    event 'InstallTemplatesStart', [ 'Installing Templates...' ]
    targetDir = "${basedir}/src/templates/vaadin"
    sourceDir = "${vaadinPluginDir}/src/templates/vaadin"
    overwrite = false

    // only if template dir already exists in, ask to overwrite templates
    if (new File(targetDir).exists()) {
        if (!isInteractive || confirmInput("Overwrite existing templates? [y/n]","overwrite.templates")) {
            overwrite = true
        }
    }
    else {
        ant.mkdir(dir: targetDir)
    }

    // Copy All
    ant.copy(todir:targetDir, overwrite:overwrite) {
        fileset(dir:sourceDir)
    }

    event("StatusUpdate", [ "Templates installed successfully"])
    event 'InstallTemplatesEnd', [ 'Finished Installing Templates.' ]
}
