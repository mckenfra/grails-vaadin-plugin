
vaadin {

    // Your Vaadin application class that extends com.vaadin.Application:
    applicationClass = "com.mycompany.MyVaadinApplication"

	// This is optional, GrailsAwareApplicationServlet is provided by default. Use this if you need to add or change application servlet. 
	// You should extend GrailsAwareApplicationServlet or GrailsAwareGAEApplicationServlet (from com.vaadin.grails.terminal.gwt.server package).
	// servletClass = "com.mycompany.MyGrailsAwareApplicationServlet"
	
    autowire = "byName" //how should dependencies be injected? other option is 'byType'

    // The context relative path where you want to access your Vaadin UI. Default is the context root.
    contextRelativePath = "/"
              
    productionMode = false

    googleAppEngineMode = false
    
    // Add paths to any JavaScript libraries that should be added to head element of Vaadin page
    javascriptLibraries = ["","",""]
    
    // Show popup on authentication error
    authenticationErrorNotificationEnabled = true
    // Show popup on communication error
    communicationErrorNotificationEnabled = true
    // Show popup on cookies disabled
    cookiesDisabledNotificationEnabled = true
    // Show popup on internal error
    internalErrorNotificationEnabled = true
    // Show popup on out-of-sync error
    outOfSyncNotificationEnabled = true
    // Show popup on session expired.
    // Note: if false, a new session is created automatically.
    sessionExpiredNotificationEnabled = true
}

environments {
    production {
        vaadin {
            productionMode = true
        }
    }
}
