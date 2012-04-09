import org.grails.plugin.vaadin.support.DefaultVaadinTemplateGenerator
import grails.util.GrailsNameUtils

/**
 * Gant script that generates a Vaadin CRUD controller and matching views for a given domain class
 * 
 * Based on _GrailsGenerate script in Grails Core.
 *
 * @author Francis McKenzie
 */

includeTargets << grailsScript("_GrailsBootstrap")

generateForName = null

target(generateForOne: "Generates Vaadin controllers and views for only one domain class.") {
    depends(loadApp,compile)

    def name = generateForName
    name = name.indexOf('.') > 0 ? name : GrailsNameUtils.getClassNameRepresentation(name)
    def domainClass = grailsApp.getDomainClass(name)

    if (!domainClass) {
        grailsConsole.updateStatus "Domain class not found in grails-app/domain, trying hibernate mapped classes..."
        bootstrap()
        domainClass = grailsApp.getDomainClass(name)
    }

    if (domainClass) {
        generateForDomainClass(domainClass)
        event("StatusFinal", ["Finished generation for domain class ${domainClass.fullName}"])
    }
    else {
        event("StatusFinal", ["No domain class found for name ${name}. Please try again and enter a valid domain class name"])
        exit(1)
    }
}

target(uberGenerate: "Generates controllers and views for all domain classes.") {
    depends(loadApp,compile)

    def domainClasses = grailsApp.domainClasses

    if (!domainClasses) {
        println "No domain classes found in grails-app/domain, trying hibernate mapped classes..."
        bootstrap()
        domainClasses = grailsApp.domainClasses
    }

    if (domainClasses) {
        domainClasses.each { domainClass -> generateForDomainClass(domainClass) }
        event("StatusFinal", ["Finished Vaadin UI generation for domain classes"])
    }
    else {
        event("StatusFinal", ["No domain classes found"])
    }
}

def generateForDomainClass(domainClass) {
    // Unfortunately our groovy classes are not imported automatically...
    // So we have to load manually
    Class generator = classLoader.loadClass("org.grails.plugin.vaadin.support.DefaultVaadinTemplateGenerator", true)
    
    def templateGenerator = generator.newInstance(classLoader, "${vaadinPluginDir}")
    templateGenerator.grailsApplication = grailsApp
    templateGenerator.pluginManager = pluginManager
    
    event("StatusUpdate", ["Generating UI for domain class ${domainClass.fullName}"])
    templateGenerator.generateUI(domainClass, basedir)
    // TO DO: implement test
    //templateGenerator.generateTest(domainClass, "${basedir}/test/unit")
    event("GenerateVaadinUIEnd", [domainClass.fullName])
}
