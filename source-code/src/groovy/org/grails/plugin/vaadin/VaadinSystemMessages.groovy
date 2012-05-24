package org.grails.plugin.vaadin

import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.commons.GrailsApplication;

import com.vaadin.Application;

/**
 * System-wide system messages configured from Vaadin Config
 * 
 * @author Francis McKenzie
 */
class VaadinSystemMessages extends Application.CustomizedSystemMessages {
    static log = LogFactory.getLog(VaadinSystemMessages.class)

    /**
     * Initialise default system messages
     */
    public VaadinSystemMessages() {
        super()
    }

    /**
     * Initialise this system messages from Vaadin config retrieved from grails application
     * <p>
     * Note that an exception will be thrown if the config contains entries that should
     * not be applied to system messages.
     * 
     * @param grailsApplication Grails app from which to retrieve Vaadin config
     */
    public VaadinSystemMessages(GrailsApplication grailsApplication) {
        super()
        configure(grailsApplication)
    }
    
    /**
     * Initialise this system messages from specified props
     * <p>
     * Note that an exception will be thrown if the map contains entries that should
     * not be applied to system messages.
     * 
     * @param props The settings to use to configure this system messages
     */
    public VaadinSystemMessages(Map props) {
        super()
        configure(props)
    }
    
    /**
     * Configures system messages from Vaadin config retrieved from grails application
     * <p>
     * Note that an exception will be thrown if the config contains entries that should
     * not be applied to system messages.
     */
    public void configure(GrailsApplication grailsApplication) {
        if (! grailsApplication) {
            throw new IllegalArgumentException("No grailsApplication specified!")
        }
        def config = grailsApplication.config.vaadin?.systemMessages
        if (! config) {
            if (log.isDebugEnabled()) {
                log.debug "MISSING CONFIG: System Messages"
            }
            return
        }
        configure(config.toProperties())
    }
   

    /**
     * Configures system messages form specified map of properties.
     * <p>
     * Note that an exception will be thrown if the map contains entries that should
     * not be applied to system messages.
     */
    public void configure(Map props) {
        props.each { k,v ->
            this."${k}" = (v == "false" ? false : (v == "true" ? true : v))
        }
        
        if (log.isDebugEnabled()) {
            log.debug """\
SYSTEM MESSAGES:
${this}
"""
        }
    }
    
    /**
     * Gets a string representation of this system messages
     */
    String toString() {
        return """\
sessionExpiredURL = ${sessionExpiredURL}
sessionExpiredNotificationEnabled = ${sessionExpiredNotificationEnabled}
sessionExpiredCaption = ${sessionExpiredCaption}
sessionExpiredMessage = ${sessionExpiredMessage}
communicationErrorURL = ${communicationErrorURL}
communicationErrorNotificationEnabled = ${communicationErrorNotificationEnabled}
communicationErrorCaption = ${communicationErrorCaption}
communicationErrorMessage = ${communicationErrorMessage}
internalErrorURL = ${internalErrorURL}
internalErrorNotificationEnabled = ${internalErrorNotificationEnabled}
internalErrorCaption = ${internalErrorCaption}
internalErrorMessage = ${internalErrorMessage}
outOfSyncURL = ${outOfSyncURL}
outOfSyncNotificationEnabled = ${outOfSyncNotificationEnabled}
outOfSyncCaption = ${outOfSyncCaption}
outOfSyncMessage = ${outOfSyncMessage}
cookiesDisabledURL = ${cookiesDisabledURL}
cookiesDisabledNotificationEnabled = ${cookiesDisabledNotificationEnabled}
cookiesDisabledCaption = ${cookiesDisabledCaption}
cookiesDisabledMessage = ${cookiesDisabledMessage}
"""
    }
}
