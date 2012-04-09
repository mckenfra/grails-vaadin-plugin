package org.grails.plugin.vaadin.support

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
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.ResourceLoader
import org.springframework.util.Assert

/**
 * Default implementation of the generator that generates Vaadin artifacts
 * (controllers, views etc.) from the domain model.
 *
 * @author Francis McKenzie
 */
class DefaultVaadinTemplateGenerator extends DefaultGrailsTemplateGenerator {

    static final Log VLOG = LogFactory.getLog(DefaultVaadinTemplateGenerator)

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

    void generateUI(GrailsDomainClass domainClass, String destdir) {
        generateController(domainClass, destdir)
        generateViews(domainClass, destdir)
    }


    @Override
    void generateViews(GrailsDomainClass domainClass, String destdir) {
        generateVaadinArtefact(domainClass, destdir, "VaadinView")
    }

    @Override
    void generateController(GrailsDomainClass domainClass, String destdir) {
        generateVaadinArtefact(domainClass, destdir, "VaadinController")
    }

    void generateVaadinArtefact(GrailsDomainClass domainClass, String destdir, String type) {
        Assert.hasText destdir, "Argument [destdir] not specified"
        
        if (domainClass) {
            def fullName = domainClass.fullName
            def pkg = ""
            def pos = fullName.lastIndexOf('.')
            if (pos != -1) {
                // Package name with trailing '.'
                pkg = fullName[0..pos]
            }

            def destFile = new File("${destdir}/grails-app/vaadin/${pkg.replace('.' as char, '/' as char)}${domainClass.shortName}${type}.groovy")
            if (canWrite(destFile)) {
                destFile.parentFile.mkdirs()

                destFile.withWriter { w ->
                    generateVaadinArtefact(domainClass, w, type)
                }

                VLOG.info("${type} generated at ${destFile}")
            }
        }
    }
    
    @Override
    void generateTest(GrailsDomainClass domainClass, String destDir) {
        File destFile = new File("$destDir/${domainClass.packageName.replace('.','/')}/${domainClass.shortName}VaadinControllerTests.groovy")
        def templateText = getTemplateText("VaadinTest.groovy")
        def t = engine.createTemplate(templateText)

        def binding = [pluginManager: pluginManager,
                       packageName: domainClass.packageName,
                       domainClass: domainClass,
                       className: domainClass.shortName,
                       propertyName: domainClass.logicalPropertyName]

        if (canWrite(destFile)) {
            destFile.parentFile.mkdirs()
            destFile.withWriter {
                t.make(binding).writeTo(it)
            }
        }
    }

    void generateVaadinArtefact(GrailsDomainClass domainClass, Writer out, String type) {
        def templateText = getTemplateText("${type}.groovy")
        
        boolean hasHibernate = pluginManager?.hasGrailsPlugin('hibernate')
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
    private String getPropertyName(GrailsDomainClass domainClass) { "${domainClass.propertyName}${domainSuffix}" }

    // Unfortunately private in superclass, so copied again here
    private canWrite(File testFile) {
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

    // Unfortunately private in superclass until grails 2.0.1, so copied again here
    public getTemplateText(String template) {
        def application = grailsApplication
        // first check for presence of template in application
        if (resourceLoader && application?.warDeployed) {
            return resourceLoader.getResource("/WEB-INF/templates/scaffolding/${template}").inputStream.text
        }

        // Try to get from project first
        def templateFile = new FileSystemResource(new File("${basedir}/src/templates/scaffolding/${template}").absoluteFile)
        if (!templateFile.exists()) {
            
            // Not found, so get from plugin
            templateFile = new FileSystemResource(new File("${pluginDir}/src/templates/scaffolding/${template}").absoluteFile)
        }
        if(templateFile.exists()) {
            return templateFile.inputStream.getText()
        } else {
            throw new Exception("Template file not found: ${template}")
        }
    }
}
