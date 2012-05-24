package org.grails.plugin.vaadin

import org.codehaus.groovy.grails.support.PersistenceContextInterceptor;

/**
 * Allows you to wrap a Closure in a grails transaction.
 * 
 * @author Francis McKenzie
 */
class VaadinTransactionManager {
    /**
     * Injected - the grails persistence interceptor for creating the
     * transaction
     */
    def PersistenceContextInterceptor persistenceInterceptor

    /**
     * Excutes the closure in a grails transaction, and returns the result.
     * 
     * @param wrapped the Closure to execute
     * @return The result of executing the closure
     */
    def wrapInTransaction(Closure wrapped) {
        // Wrap in transaction
        if (!persistenceInterceptor.open) {
            persistenceInterceptor.init()
            try {
                return wrapped()
            } finally {
                persistenceInterceptor.flush()
                persistenceInterceptor.destroy()
            }
        // Already in a transaction - just execute
        } else {
            return wrapped()
        }
    }
}