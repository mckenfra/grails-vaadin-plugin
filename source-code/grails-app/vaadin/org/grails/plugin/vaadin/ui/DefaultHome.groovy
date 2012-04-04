package org.grails.plugin.vaadin.ui

import com.vaadin.ui.CustomLayout
import com.vaadin.ui.Label
import com.vaadin.ui.themes.BaseTheme

import grails.util.Metadata

import org.grails.plugin.vaadin.ui.BulletedList
import org.grails.plugin.vaadin.ui.GrailsButton

/**
 * Mimics the Grails root index.gsp (i.e. showing Grails
 * installation details, controllers etc.).
 * <p>
 * Uses the layout page 'home.html'
 * in web-app/VAADIN/themes/{theme}/layouts.
 * 
 * @author Francis
 */
class DefaultHome extends CustomLayout {
    public DefaultHome() {
        // Set layout
        super("home")
        
        // Window title
        application.mainWindow.caption = "Grails"

        // Skip message
        this.addComponent(new Label(message(code:"default.link.skip.label", default:"Skip to content&hellip;"), Label.CONTENT_RAW), "skip")
        
        // Side bar - app info
        def grailsApplication = getBean('grailsApplication')
        this.addComponent(new Label("App version: ${Metadata.current['app.version']}"), "appVersion")
        this.addComponent(new Label("Grails version: ${Metadata.current['app.grails.version']}", Label.CONTENT_RAW), "grailsVersion")
        this.addComponent(new Label("Groovy version: ${org.codehaus.groovy.runtime.InvokerHelper.getVersion()}", Label.CONTENT_RAW), "groovyVersion")
        this.addComponent(new Label("JVM version: ${System.getProperty('java.version')}", Label.CONTENT_RAW), "javaVersion")
        this.addComponent(new Label("Reloading active: ${grails.util.Environment.reloadingAgentEnabled}", Label.CONTENT_RAW), "reloadingActive")
        this.addComponent(new Label("Controllers: ${grailsApplication.controllerClasses.size()}", Label.CONTENT_RAW), "controllerCount")
        this.addComponent(new Label("Domains: ${grailsApplication.domainClasses.size()}", Label.CONTENT_RAW), "domainCount")
        this.addComponent(new Label("Services: ${grailsApplication.serviceClasses.size()}", Label.CONTENT_RAW), "serviceCount")
        this.addComponent(new Label("Tag Libraries: ${grailsApplication.tagLibClasses.size()}", Label.CONTENT_RAW), "tagLibCount")

        // Side bar - plugins list
        def pluginsMsg = getBean('pluginManager').allPlugins.collect { plugin ->
            "<li>${plugin.name} - ${plugin.version}</li>"
        }.join("\n")
        this.addComponent(new Label("<ul>${pluginsMsg}</ul>", Label.CONTENT_RAW), "plugins")
        
        // Controllers list
        BulletedList controllers = new BulletedList()
        controllers.setSizeUndefined()
        controllers.spacing = true
        grailsApplication.controllerClasses.sort { it.fullName }.each { c ->
            GrailsButton controllerLink = new GrailsButton(c.fullName, [controller:c])
            controllerLink.addStyleName(BaseTheme.BUTTON_LINK)
            controllerLink.setSizeUndefined()
            controllers.addComponent(controllerLink)
        }
        this.addComponent(controllers, "controllers")
    }
}
