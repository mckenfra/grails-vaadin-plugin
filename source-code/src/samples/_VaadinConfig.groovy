
vaadin {

    // Your Vaadin application class that extends com.vaadin.Application:
    applicationClass = "com.mycompany.MyVaadinApplication"

	// This is optional, GrailsAwareApplicationServlet is provided by default. Use this if you need to add or change application servlet. 
	// You should extend GrailsAwareApplicationServlet or GrailsAwareGAEApplicationServlet (from com.vaadin.grails.terminal.gwt.server package).
	// servletClass = "com.mycompany.MyGrailsAwareApplicationServlet"
	
    autowire = "byName" //how should dependencies be injected? other option is 'byType'

    // The context relative path where you want to access your Vaadin UI.
    contextRelativePath = "/vaadin"
              
    productionMode = false

    googleAppEngineMode = false
    
    // Add paths to any JavaScript libraries that should be added to head element of Vaadin page
    javascriptLibraries = ["","",""]
    
    // Controls what happens when for example session expires
    systemMessages {
        authenticationErrorNotificationEnabled = true
        communicationErrorNotificationEnabled = true
        cookiesDisabledNotificationEnabled = true
        internalErrorNotificationEnabled = true
        outOfSyncNotificationEnabled = true
        sessionExpiredNotificationEnabled = true
    }
}

environments {
    production {
        vaadin {
            productionMode = true
        }
    }
}
