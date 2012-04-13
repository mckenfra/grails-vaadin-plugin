package org.grails.plugin.vaadin

/**
 * Utility methods for Vaadin classes.
 * 
 * @author Francis McKenzie
 */
public class VaadinUtils {
    /**
     * Converts an object to a String. Main benefit is that it creates a smaller
     * representation of a Map containing long lists of objects, than is created
     * by just calling map.toString()
     * 
     * @param obj The object to stringify
     * @return The String representation of the object.
     */
    public static String toString(obj) {
        if (obj instanceof Map) {
            return '[' + obj.collect { "${it.key}:${it.value instanceof Collection ? '[0..' + it.value.size() + ']' : it.value}" }.join(', ') + ']'
        } else {
            return obj?.toString()
        }
    }
}
