package org.grails.plugin.vaadin.scaffolding

import grails.build.logging.GrailsConsole
import groovy.text.Template

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.scaffolding.DefaultGrailsTemplateGenerator;
import org.codehaus.groovy.grails.scaffolding.DomainClassPropertyComparator;
import org.codehaus.groovy.grails.scaffolding.SimpleDomainClassPropertyComparator;
import org.grails.plugin.vaadin.scaffolding.DefaultVaadinTemplateGenerator;
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.ResourceLoader
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert

/**
 * Default implementation of the generator that generates Vaadin artifacts
 * (controllers, views etc.) from the domain model.
 *
 * @author Francis McKenzie
 */
class DefaultVaadinTemplateGenerator extends DefaultGrailsTemplateGenerator {
    String pluginDir
    
    /**
     * Used by the scripts so that they can pass in their AntBuilder instance.
     */
    DefaultVaadinTemplateGenerator(ClassLoader classLoader, String pluginDir) {
        super(classLoader)
        
        this.pluginDir = pluginDir
    }

    /**
     * Default constructor.
     */
    DefaultVaadinTemplateGenerator() { super() }

    /**
     * Almost identical to renderEditor in superclass, but uses
     * additional 'parentProperty' param to handle embedded domain class properties,
     * and 'readOnly' param to allow rendering of both edit/create fields and show fields
     * 
     * @param args The map of args, containing the following listed params.
     * @param property The property to render
     * @param readOnly Whether or not property is read-only
     * @param parentProperty For embedded properties, this is the embedded property's parent property
     */
    def renderEditorWithArgs = { Map args ->
        if (DefaultGrailsTemplateGenerator.LOG.isDebugEnabled()) {
            DefaultGrailsTemplateGenerator.LOG.debug("RENDER EDITOR: ${args}")
        }
        DefaultGrailsTemplateGenerator.LOG.info("RENDER EDITOR: ${args}")
        def property = args?.property
        if (!property) {
            throw new IllegalArgumentException("No property to render!")
        }
        
        def parentProperty = args.parentProperty
        def readOnly = true && args.readOnly
        def domainClass = parentProperty ? parentProperty.domainClass : property.domainClass
        
        def cp
        if (pluginManager?.hasGrailsPlugin('hibernate')) {
            cp = property.domainClass.constrainedProperties[property.name]
        }

        if (!renderEditorTemplate) {
            // create template once for performance
            def templateText = getTemplateText("renderEditor.template")
            renderEditorTemplate = engine.createTemplate(templateText)
        }

        def binding = [pluginManager: pluginManager,
                       property: property,
                       parentProperty: parentProperty,
                       readOnly: readOnly,
                       domainClass: domainClass,
                       cp: cp,
                       domainInstance:getPropertyName(domainClass)]
        return renderEditorTemplate.make(binding).toString()
    }

    /**
     * Copied from superclass, as we need to add 'vaadin-' to the output view directory
     */
    @Override
    void generateViews(GrailsDomainClass domainClass, String destdir) {
        Assert.hasText destdir, "Argument [destdir] not specified"

        def viewsDir = new File("${destdir}/grails-app/views/vaadin-${domainClass.propertyName}")
        if (!viewsDir.exists()) {
            viewsDir.mkdirs()
        }

        for (t in getTemplateNames()) {
            LOG.info "Generating $t Vaadin view for domain class [${domainClass.fullName}]"
            generateView domainClass, t, viewsDir.absolutePath
        }
    }
    
    /**
     * Copied from superclass - calls overridden getTemplateText() and renderEditorWithArgs
     * (This was private in superclass in grails 2.0.1, therefore invisible
     * to this class)
     */
    @Override
    void generateView(GrailsDomainClass domainClass, String viewName, Writer out) {
        def templateText = getTemplateText("${viewName}.gsp")

        if(templateText) {
            def t = engine.createTemplate(templateText)
            def multiPart = domainClass.properties.find {it.type == ([] as Byte[]).class || it.type == ([] as byte[]).class}

            boolean hasHibernate = pluginManager?.hasGrailsPlugin('hibernate')
            def packageName = domainClass.packageName ? "<%@ page import=\"${domainClass.fullName}\" %>" : ""
            def binding = [pluginManager: pluginManager,
                    packageName: packageName,
                    domainClass: domainClass,
                    multiPart: multiPart,
                    className: domainClass.shortName,
                    propertyName:  getPropertyName(domainClass),
                    renderEditor: renderEditorWithArgs,
                    comparator: hasHibernate ? DomainClassPropertyComparator : SimpleDomainClassPropertyComparator]

            t.make(binding).writeTo(out)
        }

    }
    
    /**
     * Copied from superclass - target Controller name is hardcoded, so have to
     * copy in the entire method here just so we can change the name...
     */
    @Override
    void generateController(GrailsDomainClass domainClass, String destdir) {
        Assert.hasText destdir, "Argument [destdir] not specified"

        if (domainClass) {
            def fullName = domainClass.fullName
            def pkg = ""
            def pos = fullName.lastIndexOf('.')
            if (pos != -1) {
                // Package name with trailing '.'
                pkg = fullName[0..pos]
            }

            def destFile = new File("${destdir}/grails-app/controllers/${pkg.replace('.' as char, '/' as char)}${domainClass.shortName}VaadinController.groovy")
            if (canWrite(destFile)) {
                destFile.parentFile.mkdirs()

                destFile.withWriter { w ->
                    generateController(domainClass, w)
                }

                LOG.info("VaadinController generated at ${destFile}")
            }
        }
    }

    /**
     * Copied from superclass - calls overridden getTemplateText() and uses
     * name VaadinController.groovy
     */
    @Override
    void generateController(GrailsDomainClass domainClass, Writer out) {
        def templateText = getTemplateText("VaadinController.groovy")

        boolean hasHibernate =pluginManager?.hasGrailsPlugin('hibernate')
        def binding = [pluginManager: pluginManager,
                       packageName: domainClass.packageName,
                       domainClass: domainClass,
                       className: domainClass.shortName,
                       propertyName: getPropertyName(domainClass),
                       comparator: hasHibernate ? DomainClassPropertyComparator : SimpleDomainClassPropertyComparator]

        def t = engine.createTemplate(templateText)
        t.make(binding).writeTo(out)
    }

    // Unfortunately private in superclass, so copied again here
    public String getPropertyName(GrailsDomainClass domainClass) { "${domainClass.propertyName}${domainSuffix}" }

    // Unfortunately private in superclass, so copied again here
    public canWrite(File testFile) {
        if (!overwrite && testFile.exists()) {
            try {
                def response = GrailsConsole.getInstance().userInput("File ${makeRelativeIfPossible(testFile.absolutePath, basedir)} already exists. Overwrite?",['y','n','a'] as String[])
                overwrite = overwrite || response == "a"
                return overwrite || response == "y"
            }
            catch (Exception e) {
                // failure to read from standard in means we're probably running from an automation tool like a build server
                return true
            }
        }
        return true
    }
    
    // Unfortunately private in superclass, so copied again here
    public getTemplateText(String template) {
        def application = grailsApplication
        // first check for presence of template in application
        if (resourceLoader && application?.warDeployed) {
            return resourceLoader.getResource("/WEB-INF/templates/vaadin/scaffolding/${template}").inputStream.text
        }

        // Try to get from project first
        def templateFile = new FileSystemResource(new File("${basedir}/src/templates/vaadin/scaffolding/${template}").absoluteFile)
        if (!templateFile.exists()) {
            
            // Not found, so get from plugin
            templateFile = new FileSystemResource(new File("${pluginDir}/src/templates/vaadin/scaffolding/${template}").absoluteFile)
        }
        
        // Check success
        if(templateFile.exists()) {
            return templateFile.inputStream.getText()
        } else {
            throw new Exception("Template file not found: ${template}")
        }
    }
    
    def getTemplateNames() {
        Closure filter = { it[0..-5] }
        if (resourceLoader && application?.isWarDeployed()) {
            def resolver = new PathMatchingResourcePatternResolver(resourceLoader)
            try {
                return resolver.getResources("/WEB-INF/templates/vaadin/scaffolding/*.gsp").filename.collect(filter)
            }
            catch (e) {
                return []
            }
        }
        
        def resources = []
        def resolver = new PathMatchingResourcePatternResolver()
        String templatesDirPath
        def templatesDir

        // Try to get from project first
        templatesDirPath = "${basedir}/src/templates/vaadin/scaffolding"
        templatesDir = new FileSystemResource(templatesDirPath)
        if (templatesDir.exists()) {
            try {
                resources = resolver.getResources("file:$templatesDirPath/*.gsp").filename.collect(filter)
            }
            catch (e) {
                LOG.info("Error while loading Vaadin views from grails-app vaadin/scaffolding folder", e)
            }
        }

        // Add resources from plugin
        templatesDirPath = "${pluginDir}/src/templates/vaadin/scaffolding"
        templatesDir = new FileSystemResource(templatesDirPath)
        if (templatesDir.exists()) {
            try {
                resources.addAll(resolver.getResources("file:$templatesDirPath/*.gsp").filename.collect(filter))
            }
            catch (e) {
                LOG.info("Error while loading Vaadin views from plugin vaadin/scaffolding folder", e)
            }
        }

        // Check success
        if(resources) {
            return resources
        } else {
            throw new Exception("Vaadin view templates not found!")
        }
    }
}
