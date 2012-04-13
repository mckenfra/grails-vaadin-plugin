import grails.util.GrailsNameUtils

/**
 * Gant script that creates the Vaadin Application class, updates required config
 * and installs Vaadin grails theme.
 *
 * @author Francis McKenzie
 */

includeTargets << grailsScript("_GrailsInit")
includeTargets << grailsScript("_GrailsCreateArtifacts")

vaadinApplicationShortName = ""
vaadinApplicationPackage = ""
vaadinApplicationFullName = ""

target ('initGenerateVaadinApp': 'Initialises required parameters and dependencies') {
    depends(checkVersion, parseArguments)
    
    promptForName(type: "Vaadin Application Class")
    
    // Get the class and package
    def name = argsMap["params"][0]
    checkName(name)
    def pkg = null
    def pos = name.lastIndexOf('.')
    if (pos != -1) {
        pkg = name[0..<pos]
        name = name[(pos + 1)..-1]
        if (pkg.startsWith("~")) {
            pkg = pkg.replace("~", createRootPackage())
        }
    }
    else {
        pkg = createRootPackage()
    }
    
    // Save result
    vaadinApplicationShortName = GrailsNameUtils.getClassNameRepresentation(name)
    vaadinApplicationPackage = pkg
    vaadinApplicationFullName = "${vaadinApplicationPackage}.${vaadinApplicationShortName}"
}

target ('createVaadinApp': 'Creates Vaadin Application class') {
    depends(initGenerateVaadinApp)
    
    // Create application
    createArtifact(name:vaadinApplicationFullName, suffix: "", type: "VaadinApplication", path: "grails-app/vaadin", templatePath: "templates/vaadin/artifacts")
}

target ('updateVaadinConfig': 'Sets applicationClass in VaadinConfig.groovy') {
    depends(initGenerateVaadinApp)
    // Update VaadinConfig
    def configFile = new File("${basedir}/grails-app/conf/VaadinConfig.groovy")
    if (configFile.exists()) {
        def pattern = /(?i)((?<=^|\s|\/)applicationClass\s*=\s*["'])([^"']+)(["'])/
        def replacement = "\$1${vaadinApplicationFullName}\$3"
        boolean succeeded = replaceTextInFile(file:configFile, pattern:pattern, replacement:replacement)
        if (succeeded) {
            event("StatusUpdate", ["Updated VaadinConfig.groovy"])
        } else {
            grailsConsole.updateStatus "Config setting 'applicationClass' not found in VaadinConfig.groovy! Please update your config manually."
        }
    } else {
        grailsConsole.updateStatus "VaadinConfig.groovy not found! Please update your config manually."
    }
}

target ('installVaadinTheme': "Installs Vaadin grails theme resources") {
    // No dependencies

    // Create theme
    def themeName = 'main'
    def themePath = "web-app/VAADIN/themes/${themeName}"
    def themeSrcDir = "${vaadinPluginDir}/${themePath}"
    def themeTgtDir = "${basedir}/${themePath}"
    def themeExists = new File(themeTgtDir).exists()
    def overwrite = false
    
    // only if theme dir already exists in, ask to overwrite theme
    if (themeExists) {
        if (!isInteractive || confirmInput("Vaadin theme '${themeName}' already exists. Overwrite?","theme.${themeName}.overwrite")) {
            overwrite = true
        }
    }
    else {
        ant.mkdir(dir: themeTgtDir)
    }
    
    // Recursive copy
    if (!themeExists || (themeExists && overwrite)) {
        if (new File(themeTgtDir).exists()) {
            ant.copy(todir:themeTgtDir, overwrite:overwrite) {
                fileset(dir:themeSrcDir)
            }
            event("StatusUpdate", ["Installed theme resources"])
        } else {
            grailsConsole.updateStatus "Unable to copy Vaadin theme to ${themeTgtDir}"
        }
    }
}

target ('installVaadinViews': "Installs Vaadin fiew files") {
    // No dependencies

    // Ensure views dir exists
    def viewsSrcDir = "${vaadinPluginDir}/grails-app/views"
    def viewsTgtDir = "${basedir}/grails-app/views"
    ant.mkdir(dir:viewsTgtDir)
    
    // Recursive copy
    ant.copy(todir:viewsTgtDir, overwrite:false) {
        fileset(dir:viewsSrcDir)
    }
    
    event("StatusUpdate", ["Installed Vaadin base views"])
}

target ('disableUrlMappings': 'Comments out all mappings in UrlMappings.groovy') {
    // No dependencies
    
    // Update UrlMappings
    def mappingsFile = new File("${basedir}/grails-app/conf/UrlMappings.groovy")
    if (mappingsFile.exists()) {
        def within = /(?ims)\A(.*(?<=^|\s|\/)class\s+UrlMappings\s*\{\s*static\s+mappings\s*=\s*\{.*?)(^.*)(^.*\}\s*\}\s*)\z/
        def pattern = /(?ims)^(?!\/\/)/
        def replacement = "//"
        boolean succeeded = replaceTextInFile(file:mappingsFile, pattern:pattern, replacement:replacement, within:within)
        if (succeeded) {
            event("StatusUpdate", ["Disabled UrlMappings.groovy"])
        } else {
            grailsConsole.updateStatus "No changes made to UrlMappings.groovy"
        }
    } else {
        // Safely ignore - no UrlMappings, so no problem!
    }
}

target ('default': "Creates a new Vaadin Application") {
    depends(createVaadinApp, updateVaadinConfig, installVaadinTheme, installVaadinViews, disableUrlMappings)
    
    event("StatusFinal", ["Finished Vaadin application generation"])
}

boolean replaceTextInFile(Map args) {
    boolean succeeded = false
    
    // Get file contents
    def file = args.file instanceof File ? args.file : new File("${args.file}")
    def text = file.text
    
    // Replace text within some block of text
    if (args.within) {
        def m = text =~ args.within
        boolean foundWithin = m
        if (foundWithin && m.groupCount() == 3) {
            def beforeText = m[0][1]
            def oldText = m[0][2]
            def afterText = m[0][3]
            
            boolean foundPattern = text =~ args.pattern
            if (foundPattern) {
                file.withWriter {
                    it << beforeText
                    it << replaceAllOccurrences(oldText, args.pattern, args.replacement)
                    it << afterText
                }
                succeeded = true
            }
        }
        
    // Just replace text, wherever it is
    } else {
        boolean foundPattern = text =~ args.pattern
        if (foundPattern) {
            file.text = replaceAllOccurrences(text, args.pattern, args.replacement)
            succeeded = true
        }
    }
    
    return succeeded
}

String replaceAllOccurrences(text, pattern, replacement) {
    return text && pattern && replacement ? text.replaceAll(pattern, replacement) : text
}

def createRootPackage() {
    compile()
    createConfig()
    return (config.grails.project.groupId ?: grailsAppName).replace('-','.').toLowerCase()
}

def checkName(name) {
    if (!name) {
        fatalError "You must specify a name!"
    }
    if (! (name ==~ /([\w\d]+\.)*\w[\w\d]*/) ) {
        fatalError "That name is invalid! '${name}'"
    }
    if (name.endsWith("VaadinApplication")) {
        fatalError "Please use a different name!"
    }
}

def fatalError(msg) {
    event("StatusUpdate", ["ERROR: ${msg}"])
    Ant.fail(message: msg)
}