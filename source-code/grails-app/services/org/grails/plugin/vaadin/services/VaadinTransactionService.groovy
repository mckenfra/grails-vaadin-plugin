package org.grails.plugin.vaadin.services

/**
 * Executes specified closure in a transaction.
 *
 * @author Francis McKenzie
 */
class VaadinTransactionService {
    static transactional = true
    static scope = 'session'
 
    def withTransaction(Closure closureRequiringExecutionInTransaction) {
        // We're now in a transaction!
        closureRequiringExecutionInTransaction()
    }   
}
