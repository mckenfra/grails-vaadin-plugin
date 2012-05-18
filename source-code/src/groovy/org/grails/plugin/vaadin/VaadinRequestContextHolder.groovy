package org.grails.plugin.vaadin

/**
 * Mimics Spring's
 * <a href="http://static.springsource.org/spring/docs/3.0.x/javadoc-api/org/springframework/web/context/request/RequestContextHolder.html">RequestContextHolder</a>
 * <p>
 * Holds active {@link VaadinRequest} in current thread. 
 * 
 * @author Francis McKenzie
 */
class VaadinRequestContextHolder {
    /**
     * Holds the active request in the current thread
     */
    protected static ThreadLocal<VaadinRequest> activeRequestHolder = new ThreadLocal<VaadinRequest>()
    
    /**
     * Gets the active request
     */
    public static VaadinRequest getRequestAttributes() {
        return activeRequestHolder.get()
    }

    /**
     * Sets the active request
     */
    public static void setRequestAttributes(VaadinRequest request) {
        if (!request) activeRequestHolder.remove()
        else activeRequestHolder.set(request)
    }
}
