package org.grails.plugin.vaadin

import com.vaadin.Application;

/**
 * Holds active {@link VaadinApplication} in current thread. 
 * 
 * @author Francis McKenzie
 */
class VaadinApplicationContextHolder {
    /**
     * Holds the active application in the current thread
     */
    protected static ThreadLocal<Application> activeApplicationHolder = new ThreadLocal<Application>()
    
    /**
     * Gets the active application
     */
    public static Application getVaadinApplication() {
        return activeApplicationHolder.get()
    }

    /**
     * Sets the active application
     */
    public static void setVaadinApplication(Application application) {
        if (!application) activeApplicationHolder.remove()
        else activeApplicationHolder.set(application)
    }
}
