package org.grails.plugin.vaadin.utils

import org.apache.commons.logging.LogFactory

/**
 * Simple class for recording time taken.
 * 
 * @author Francis McKenzie
 */
class Stopwatch {
    static log = LogFactory.getLog(Stopwatch.class)
    static boolean isEnabled() { return log.isDebugEnabled() }
    
    String caption
    Class source
    long startTime
    
    public Stopwatch(String caption, Object source = null) {
        this.caption = caption
        this.source = source instanceof Class ? source : source?.class
        this.startTime = new Date().getTime()
    }

    public String stop() {
        String result = this.toString()
        log.debug result
        return result
    }
    
    public String toString() {
        long millis = new Date().getTime() - startTime
        String secs = String.format('%.3f', millis / 1000.0)
        String src = source ? source.simpleName + " " : "" 
        return "[${secs}] ${src}${caption}"
    }
}
