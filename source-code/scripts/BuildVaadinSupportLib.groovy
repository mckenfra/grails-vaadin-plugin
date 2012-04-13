includeTargets << new File("${vaadinPluginDir}/scripts/_VaadinSupportLib.groovy")

target(default: "Builds library") {
    build()
}
