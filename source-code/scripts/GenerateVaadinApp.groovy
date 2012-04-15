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

/**
* TARGETS
*/
target ('initGenerateVaadinApp': 'Initialises required parameters and dependencies') {
    depends(checkVersion, parseArguments)
    
    promptForName(type: "Vaadin Application Class")
    
    // Get the class and package
    def name = argsMap["params"][0]
    checkApplicationName(name)
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

target ('updateUrlMappings': 'Adds Vaadin exclusions to UrlMappings.groovy') {
    // No dependencies
    
    // Update UrlMappings
    def mappingsFile = new File("${basedir}/grails-app/conf/UrlMappings.groovy")
    if (mappingsFile.exists()) {
        boolean succeeded
        if (containsExcludes(mappingsFile)) {
            succeeded = addExcludes(mappingsFile)
        } else {
            succeeded = createExcludes(mappingsFile)
        }
        if (succeeded) {
            event("StatusUpdate", ["Updated UrlMappings.groovy"])
        } else {
            grailsConsole.updateStatus "No changes made to UrlMappings.groovy"
        }
    } else {
        // Safely ignore - no UrlMappings, so no problem!
    }
}

target ('default': "Creates a new Vaadin Application") {
    depends(createVaadinApp, updateVaadinConfig, installVaadinTheme, installVaadinViews, updateUrlMappings)
    
    event("StatusFinal", ["Finished Vaadin application generation"])
}

/**
 * INIT METHODS
 */
def checkApplicationName(name) {
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

def createRootPackage() {
    compile()
    createConfig()
    return (config.grails.project.groupId ?: grailsAppName).replace('-','.').toLowerCase()
}

/**
 * UTILITY METHODS
 */
def fatalError(msg) {
    event("StatusUpdate", ["ERROR: ${msg}"])
    Ant.fail(message: msg)
}

boolean replaceTextInFile(Map args) {
    boolean succeeded = false
    
    // Prepare replacers
    def replacers = args.replacers
    if (! replacers) {
        if (args.pattern && args.replacement) {
            replacers = [patternReplacer.curry(args.pattern, args.replacement)]
        }
    }
    
    if (! replacers) {
        throw new IllegalArgumentException("Invalid args: ${args}")
    }
            
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
            
            try {
                def newText = applyReplacers(oldText, replacers)
                file.withWriter {
                    it << beforeText
                    it << newText
                    it << afterText
                }
                succeeded = true
            } catch (err) { /* No replacers matched */ }
        }
        
    // Just replace text, wherever it is
    } else {
        try {
            def newText = applyReplacers(text, replacers)
            file.text = newText
            succeeded = true
        } catch (err) { /* No replacers matched */ }
    }
    
    return succeeded
}

String applyReplacers(text, replacers) {
    boolean oneMatch = false
    replacers.each {
        try {
            text = it(text)
            oneMatch = true
        } catch (err) { /* No match */ }
    }
    if (!oneMatch) {
        throw new Exception("No matches!")
    }
    return text
}

// This should be curried, e.g.: patternReplacer.curry(/mypattern/, 'myreplacement')
patternReplacer = { pattern, replacement, text ->
    boolean foundPattern = text =~ pattern
    if (! foundPattern) {
        throw new Exception("No match!")
    }
    return text.replaceAll(pattern, replacement)
}

/**
 * URL MAPPINGS METHODS
 */
withinUrlMappings = /(?ims)\A(.*(?<=^|\s)class\s+UrlMappings\s*\{)()(.*)\z/
withinExcludes = /(?ims)\A(.*(?<=^|\s)class\s+UrlMappings\s*\{.*(?<=^|\s|\{)static\s+excludes\s*=\s*\[)([^\]]*)(\].*)\z/

boolean containsExcludes(File file) {
    return file.text =~ withinExcludes
}
    
boolean createExcludes(File file) {
    def pattern = /^/
    def replacement = """\

    static excludes = [
        // Vaadin static files
        "/VAADIN/*",
        // Vaadin controllers
        "**/*Vaadin/*"
    ]
"""
    return replaceTextInFile(file:file, pattern:pattern, replacement:replacement, within:withinUrlMappings)
}

boolean addExcludes(File file) {
    return replaceTextInFile(file:file, within:withinExcludes, replacers:[
        excludesReplacer.curry("/VAADIN/*", "Vaadin static files"),
        excludesReplacer.curry("**/*Vaadin/*", "Vaadin controllers")
    ])
}

// This should be curried, e.g.: excludeReplacer.curry('/my/path/**/*', 'Some comment about it')
excludesReplacer = { exclude, comment, text ->
    // Check actually have some excludes already, and not just comments or whitespace
    def alreadyHasExcludesRegex = /(?m)^\s*[\"\']/
    boolean alreadyHasExcludes = text =~ alreadyHasExcludesRegex
    
    // Add excludes
    if (text.contains("'${exclude}'") || text.contains("\"${exclude}\"")) {
        throw new Exception("Already contains!")
    }
    
    return """\

        // ${comment}
        "${exclude}"${alreadyHasExcludes ? ',' : ''}
${text}"""
}
