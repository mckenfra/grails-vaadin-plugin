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

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.grails.plugin.vaadin.VaadinApi
import org.grails.plugin.vaadin.VaadinSystemMessages
import org.grails.plugin.vaadin.gsp.GspResourcePageRenderer
import org.grails.plugin.vaadin.gsp.GspResourceLocator
import org.grails.plugin.vaadin.VaadinTransactionManager

class VaadinGrailsPlugin {

    private static final String VAADIN_CONFIG_FILE = "VaadinConfig";
    private static final String APPLICATION_SERVLET = "com.vaadin.grails.terminal.gwt.server.GrailsAwareApplicationServlet";
    private static final String GAE_APPLICATION_SERVLET = "com.vaadin.grails.terminal.gwt.server.GrailsAwareGAEApplicationServlet";

    private static final transient Logger log = LoggerFactory.getLogger("org.codehaus.groovy.grails.plugins.VaadinGrailsPlugin");

    // the plugin version
    def version = "1.6.2.2-SNAPSHOT"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.1 > *"
    // the other plugins this plugin depends on
    def dependsOn = [
        'servlets': '2.0.0 > *',
        'groovyPages': '2.0.0 > *'
    ]
    // We need to know about changes to VaadinControllers
    def observe = ["controllers"]
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
        "file:./grails-app/vaadin/**/*.groovy"
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

    /**
     * Returns the Vaadin Application spring bean definition for the
     * relevant class configured in the grails application.
     * <p>
     * Applies the 'autowire' setting from the specified config object
     * when constructing the bean definition.
     */
    def defineVaadinApplicationBean = { GrailsApplication application ->
        def vaadinApplicationClassName = application.config.vaadin?.applicationClass
        def vaadinApplicationClass = application.vaadinClasses.find {
            it.clazz.name.equals(vaadinApplicationClassName)
        }?.clazz
        if (vaadinApplicationClass) {
            "${GrailsAwareApplicationServlet.VAADIN_APPLICATION_BEAN_NAME}"(vaadinApplicationClass) { bean ->
                bean.singleton = false; // prototype scope
                bean.autowire = application.config?.vaadin?.autowire ?: 'byName'
            }
            if (log.isDebugEnabled()) {
                log.debug "APPLICATION BEAN: ${vaadinApplicationClass}"
            }
        } else {
            log.warn "Vaadin Application with class '${vaadinApplicationClassName}' not found!"
        }
    }

    def doWithSpring = {
        // Load config into application, if not already loaded
        def config = loadVaadinConfig(application)
        if (!config) return

        // Vaadin Application Bean
        defineVaadinApplicationBean.delegate = delegate
        defineVaadinApplicationBean(application)
        
        // Beans for scaffolded applications
        vaadinTransactionManager(VaadinTransactionManager) {
            persistenceInterceptor = ref("persistenceInterceptor")
        }
        vaadinApi(VaadinApi)
        vaadinSystemMessages(VaadinSystemMessages, application) { bean ->
            bean.lazyInit = true
        }
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
        // Load config into application, if not already loaded
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
    
    def loadVaadinConfig(GrailsApplication application, boolean forceReload = false) {
        // Return if already loaded
        if (!forceReload && application.config.vaadin?.applicationClass) return application.config.vaadin

        // Load config
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

        // Ensure valid
        if (! (application.config.vaadin?.applicationClass)) {
            log.warn "VaadinConfig is invalid - applicationClass not found!"
            return null
        }
        return application.config.vaadin
    }
    
    def doWithDynamicMethods = { ctx ->
        ctx.vaadinApi.injectApi(application)
    }

    def onChange = { event ->
        // Code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
        log.trace "Received event ${event}"

        def application = event.application
        
        // We are only watching grails-app/vaadin, and grails-app/controllers
        // Therefore if a class has changed but it is not a controller,
        // it must be a Vaadin Artefact.
        boolean isController = application.isControllerClass(event.source)
        boolean isVaadinController = isController && event.source.name.endsWith("VaadinController")
        boolean isVaadinArtefact = !isController && event.source instanceof Class
        
        // Only reload if Vaadin Artefact or Vaadin Controller
        if (isVaadinArtefact || isVaadinController) {
            
            // Register the reloaded Vaadin Artefact
            if (isVaadinArtefact) {
                application.addArtefact(VaadinArtefactHandler.TYPE, event.source)
            }

            // A Vaadin class has changed, but due to 'reachability' we don't
            // know which classes referenced it and might have a stale reference.
            // So, we need to reload all Vaadin classes.
            reloadVaadinArtefacts(application, [event.source])
            
            // Re-inject the Vaadin api
            event.ctx.vaadinApi.injectApi(application)
            
            // Re-register the Vaadin Application bean
            def beans = beans(defineVaadinApplicationBean.curry(application))
            beans.registerBeans(event.ctx)
    
            // Now that a Vaadin class's source code has changed and classes are reloaded, all existing
            // Vaadin application instances tied to end-users's Sessions are stale.  Each end-user will need
            // a new Application instance that reflects the changed source code.  So, create a new token for
            // the new Application.  The GrailsAwareApplicationServlet will react to the changed token value
            // and restart any previous Vaadin Application instances for all further incoming HTTP requests:
            RestartingApplicationHttpServletRequest.restartToken = UUID.randomUUID().toString()

            log.info "Vaadin class ${event.source} has changed. Browser refresh required."
        } else {
            if (!isController) {
                log.info "IGNORING: ${event.source}"
            }
        }
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
    
    /**
     * Reloads all Vaadin Controllers in the application, apart from the specified
     * excluded classes (which have already been reloaded).
     * 
     * @param application The Grails Application containing the artefacts
     * @param excludes The classes to exclude from reloading
     */
    def reloadVaadinArtefacts(GrailsApplication application, List<Class> excludes = []) {
        application.vaadinClasses.each { it ->
            if (! (it.clazz in excludes)) {
                def reloadedClass = application.classLoader.loadClass(it.clazz.name)
                application.addArtefact(VaadinArtefactHandler.TYPE, reloadedClass)
                if (log.isDebugEnabled()) {
                    log.debug "RELOADED: ${reloadedClass}"
                }
            }
        }
    }
}
