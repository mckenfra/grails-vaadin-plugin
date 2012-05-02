/*
 * Copyright 2009-2010 Daniel Bell, Les Hazlewood
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * under the License.
 */

import grails.util.Environment
import com.vaadin.grails.VaadinArtefactHandler
import com.vaadin.grails.terminal.gwt.server.RestartingApplicationHttpServletRequest
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import com.vaadin.grails.terminal.gwt.server.GrailsAwareApplicationServlet

import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.i18n.LocaleContextHolder
import com.vaadin.grails.VaadinUtils
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.grails.plugin.vaadin.VaadinApi
import org.springframework.aop.scope.ScopedProxyFactoryBean
import org.grails.plugin.vaadin.gsp.GspResourcePageRenderer
import org.grails.plugin.vaadin.gsp.GspResourceLocator
import org.grails.plugin.vaadin.VaadinTransactionManager

class VaadinGrailsPlugin {

    private static final String VAADIN_CONFIG_FILE = "VaadinConfig";
    private static final String APPLICATION_SERVLET = "com.vaadin.grails.terminal.gwt.server.GrailsAwareApplicationServlet";
    private static final String GAE_APPLICATION_SERVLET = "com.vaadin.grails.terminal.gwt.server.GrailsAwareGAEApplicationServlet";

    private static final transient Logger log = LoggerFactory.getLogger("org.codehaus.groovy.grails.plugins.VaadinGrailsPlugin");

    // the plugin version
    def version = "1.6.2-SNAPSHOT"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.1 > *"
    // the other plugins this plugin depends on
    def dependsOn = [
        'servlets': '2.0.0 > *',
        'groovyPages': '2.0.0 > *'
    ]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "docs/**/*",
        "web-app/css/**/*",
        "web-app/images/**/*",
        "web-app/js/**/*",
        "i18n/**/*"
    ]
    def artefacts = [VaadinArtefactHandler]
    def watchedResources = [
        "file:./grails-app/vaadin/**/*.groovy",
        "file:./grails-app/controllers/**/*VaadinController.groovy"
    ]
    // release-plugin --zipOnly

    def license = "APACHE"
    
    def author = "Ondrej Kvasnovsky, Francis McKenzie"
    def authorEmail = "ondrej.kvasnovsky@gmail.com, francis.mckenzie@gmail.com"
    
    def developers = [
            [ name: "Daniel Bell", email: "daniel.r.bell@gmail.com" ],
            [ name: "Les Hazlewood", email: "les@katasoft.com" ],
            [ name: "Ondrej Kvasnovsky", email: "ondrej.kvasnovsky@gmail.com" ],
            [ name: "Francis McKenzie", email: "francis.mckenzie@gmail.com" ]
    ]
    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPVAADIN" ]
    def scm = [ url: "https://github.com/ondrej-kvasnovsky/grails-vaadin-plugin" ]
    
    def title = "Vaadin Grails Plugin"
    def description = """\
        A plugin for creating a Vaadin application in Grails.
    """

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/vaadin"

    def configureVaadinApplication = { clazz, config = null ->
        //create the vaadin Application definition:
        "${GrailsAwareApplicationServlet.VAADIN_APPLICATION_BEAN_NAME}"(clazz) { bean ->
            bean.singleton = false; //prototype scope
            bean.autowire = config?.autowire ?: 'byName'
        }
    }

    def configureComponentClass = { Class clazz ->

        //Commented out - end-user should use the logging plugin of their choice
        //(e.g. Grails default or 'sublog' plugin, etc).
        //add log property:
        //def log = LoggerFactory.getLogger(clazz)
        //clazz.metaClass.getLog << {-> log}

        //add i18n methods:
        clazz.metaClass.i18n = {String key, Collection args = null, Locale locale = LocaleContextHolder.getLocale() ->
            Object[] oArgs = args ? args as Object[] : null
            return VaadinUtils.i18n(key, oArgs, locale)
        }
        clazz.metaClass.i18n = {String key, String defaultMsg, Collection args = null, Locale locale = LocaleContextHolder.getLocale() ->
            Object[] oArgs = args ? args as Object[] : null
            return VaadinUtils.i18n(key, oArgs, defaultMsg, locale);
        }
        clazz.metaClass.i18n = {MessageSourceResolvable resolvable, Locale locale = LocaleContextHolder.getLocale() ->
            return VaadinUtils.i18n(resolvable, locale);
        }

        //Dynamic Spring bean instance lookup methods:
        clazz.metaClass.getBean = { String name ->
            return VaadinUtils.getBean(name)
        }
        clazz.metaClass.getBean = { Class type ->
            return VaadinUtils.getBean(type)
        }
    }
    
    /**
     * Currently (to v2.0.2) GrailsApplication.addArtefact() will add multiple copies of the same class.
     * So this method first checks if the class has already been added to GrailsApplication, and only
     * calls the addArtefact method if it hasn't been added already.
     */
    def addVaadinArtefactToGrails(Class artefact, GrailsApplication grailsApplication) {
        if (! grailsApplication.allClasses.contains(artefact)) {
            if (log.isDebugEnabled()) {
                log.debug "ADDING TO GRAILS: ${artefact}"
            }
            grailsApplication.addArtefact(VaadinArtefactHandler.TYPE, artefact)
            // Get it to appear on the controllers list too
            if (artefact.name.endsWith("Controller")) {
                grailsApplication.addArtefact("Controller", artefact)
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug "ALREADY IN GRAILS: ${artefact}"
            }
        }
    }

    def doWithSpring = {
        def config = application.config.vaadin
        if (!config || !(config.applicationClass)) {
            return
        }

        def vaadinApplicationClass = null
        application.vaadinClasses.each { vaadinGrailsClass ->

            configureComponentClass(vaadinGrailsClass.clazz)

            if (vaadinGrailsClass.clazz.name.equals(config.applicationClass)) {
                vaadinApplicationClass = vaadinGrailsClass.clazz
            }
        }

        if (vaadinApplicationClass) {
            configureVaadinApplication.delegate = delegate
            configureVaadinApplication(vaadinApplicationClass, config)
        }
        
        // Beans for scaffolded applications
        vaadinApplicationHolder(ScopedProxyFactoryBean) {
            targetBeanName = 'vaadinApplicationService'
            proxyTargetClass = true
        }
        vaadinTransactionManager(VaadinTransactionManager) {
            persistenceInterceptor = ref("persistenceInterceptor")
        }
        vaadinApi(VaadinApi) {
            vaadinApplicationHolder = vaadinApplicationHolder
            vaadinTransactionManager = vaadinTransactionManager
        }
        
        // Beans for using Vaadin in GSPs
        vaadinGspRenderer(GspResourcePageRenderer, ref("groovyPagesTemplateEngine")) { bean ->
            bean.lazyInit = true
            groovyPageLocator = groovyPageLocator
            grailsResourceLocator = grailsResourceLocator
        }
        
        vaadinGspLocator(GspResourceLocator) { bean ->
            bean.lazyInit = true
            groovyPageLocator = groovyPageLocator
            grailsResourceLocator = grailsResourceLocator
        }
    }

    def doWithApplicationContext = { applicationContext ->
        // TODO Implement post initialization spring config (optional)
    }

    def doWithWebDescriptor = { webXml ->
        // Load config into application
        // Note this closure gets called BEFORE doWithSpring, so we do the loading here
        def config = loadVaadinConfig(application)
        if (!config) return
        def vaadinApplicationClass = config.applicationClass
        def vaadinProductionMode = config.productionMode
        def vaadinGAEMode = config.googleAppEngineMode
        // def applicationServlet = vaadinGAEMode ? GAE_APPLICATION_SERVLET : APPLICATION_SERVLET
        def applicationServlet = config.servletClass ?: (vaadinGAEMode ? GAE_APPLICATION_SERVLET : APPLICATION_SERVLET)

        def contextParams = webXml."context-param"
        contextParams[contextParams.size() - 1] + {
            "context-param" {
                "description"("Vaadin production mode")
                "param-name"("productionMode")
                "param-value"(vaadinProductionMode)
            }
        }

        def servletName = "VaadinServlet"
        def widgetset = config.widgetset

        def servlets = webXml."servlet"
        servlets[servlets.size() - 1] + {
            "servlet" {
                "servlet-name"(servletName)
                "servlet-class"(applicationServlet)
                "init-param" {
                    "description"("Vaadin application class to start")
                    "param-name"("application")
                    "param-value"(vaadinApplicationClass)
                }

                if(widgetset){
                    "init-param" {
                        "description"("Application widgetset")
                        "param-name"("widgetset")
                        "param-value"(widgetset)
                    }
                }

                "load-on-startup"("1")
            }
        }

        def contextRelativePath = config.contextRelativePath ? config.contextRelativePath : "/";
        if (!contextRelativePath.startsWith("/")) {
            contextRelativePath = "/" + contextRelativePath;
        }
        if (!contextRelativePath.endsWith("/")) {
            contextRelativePath += "/";
        }
        def servletMapping = contextRelativePath + "*"
        def servletMappings = webXml."servlet-mapping"
        servletMappings[servletMappings.size() - 1] + {
            "servlet-mapping" {
                "servlet-name"(servletName)
                "url-pattern"(servletMapping)
            }
        }
        if (!contextRelativePath.equals("/")) {
            //need to additionally specify the /VAADIN/* mapping (required by Vaadin):
            servletMappings[servletMappings.size() - 1] + {
                "servlet-mapping" {
                    "servlet-name"(servletName)
                    "url-pattern"("/VAADIN/*")
                }
            }
        }
    }
    
    def loadVaadinConfig(application) {
        ClassLoader parent = getClass().getClassLoader();
        GroovyClassLoader loader = new GroovyClassLoader(parent);

        ConfigObject config;

        try {
            Class configFile = loader.loadClass(VAADIN_CONFIG_FILE);
            log.info "Loading default config file: ${configFile}.groovy ..."
            config = new ConfigSlurper(Environment.current.name).parse(configFile);
            log.info ""
            log.info "Loaded Vaadin config file:"
            log.info "    Application class: ${config?.vaadin?.applicationClass}"
            log.info "    Context Relative Path: ${config?.vaadin?.contextRelativePath}"
            log.info "    Production mode: ${config?.vaadin?.productionMode}"
            log.info "    Google AppEngine compatibility mode: ${config?.vaadin?.googleAppEngineMode}"
            
            // Now merge the config into the applicaton config
            application.config.merge(config)

        } catch (ClassNotFoundException e) {
            log.warn "Unable to find Vaadin plugin config file: ${VAADIN_CONFIG_FILE}.groovy"
        }

        //noinspection GroovyVariableNotAssigned
        return config?.vaadin;
    }
    
    def doWithDynamicMethods = { ctx ->
        // TODO add 'i18n' methods here
    }

    def onChange = { event ->
        // Code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
        log.trace "Received event ${event}"

        if (!(event.source instanceof Class)) {
            return;
        }

        def application = event.application
        def config = application.config.vaadin

        Class changedClass = event.source

        if (application.isVaadinClass(changedClass)) {
            // Ensure new artefacts are always added
            addVaadinArtefactToGrails(changedClass, application)
            
            //a vaadin component class has changed, but due to 'reachability'
            //we don't know which classes referenced it and might have a stale reference
            //So, we need to reload all classes:

            // keep a reference to the vaadinApplicationClass when we find it - we'll use
            // it to reload the Spring bean later
            def vaadinApplicationClass = null

            application.vaadinClasses.each { vaadinGrailsClass ->
                // def reloadedClass = application.classLoader.getClassLoader().reloadClass(vaadinGrailsClass.clazz.name)
                def reloadedClass = application.classLoader.loadClass(vaadinGrailsClass.clazz.name)
                if (reloadedClass.name.equals(config.applicationClass)) {
                    vaadinApplicationClass = reloadedClass
                }
                configureComponentClass(reloadedClass)
                addVaadinArtefactToGrails(reloadedClass, application)
            }

            //Now re-register the vaadin application Spring bean:
            if (vaadinApplicationClass) {
                def beans = beans(configureVaadinApplication.curry(vaadinApplicationClass, config))
                beans.registerBeans(event.ctx)
            }

            //Now that a Vaadin component's source code has changed and classes are reloaded, all existing
            //Vaadin application instances tied to end-users's Sessions are stale.  Each end-user will need
            //a new Application instance that reflects the changed source code.  So, create a new token for
            //the new Application.  The GrailsAwareApplicationServlet will react to the changed token value
            // and restart any previous Vaadin Application instances for all further incoming HTTP requests:
            RestartingApplicationHttpServletRequest.restartToken = UUID.randomUUID().toString()

            log.info "Vaadin artefact ${changedClass} has changed.  Browser refresh required."
        } else {
            log.info "Changed class is not a Vaadin class: ${changedClass}"
        }
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
}
