package org.grails.plugin.vaadin.utils

import java.util.Map;

/**
 * Utility methods for Vaadin classes.
 * 
 * @author Francis McKenzie
 */
public class Utils {
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
    
    /**
     * Converts the value to an int, if not already. Note this may throw
     * an exception when parsing an invalid value or null.
     */
    public static int toInt(value) {
        if (isNumber(value)) {
            return (int) value
        } else {
            return Integer.parseInt(value?.toString())
        }
    }
    
    /**
     * Removes all entries matching the case-insensitive key.
     *
     * @param props The map from which entries are to be removed
     * @param key The key to lookup
     * @return Either the case-sensitive match if it exists, or else returns the last
     * case-insensitive match with a non-null value.
     */
    public static Object removeCaseInsensitive(Map props, Object key) {
        // Case-sensitive remove
        def hasCaseSensitiveKey = props.containsKey(key)
        def result = hasCaseSensitiveKey ? props.remove(key) : null
        
        // Case-insensitive remove
        if (key != null) {
            String lcaseKey = key.toString().toLowerCase()
            props.keySet().findAll { it.toString().toLowerCase() == lcaseKey }.each {
                def value = props.remove(it)
                if (!hasCaseSensitiveKey && value != null) {
                    result = value
                }
            }
        }
        return result
    }
    
    /**
     * Removes all entries matching the case-insensitive keys.
     *
     * @param props The map from which entries are to be removed
     * @param keys The keys to lookup
     * @return A map containing all entries in the specified map that were removed because
     * their key matched one or more of the specified keys, when compared in a case-insensitive
     * manner.
     */
    public static Map removeAllCaseInsensitive(Map props, keys) {
        def result = [:]
        keys.each { key ->
            String lcaseKey = key.toString().toLowerCase()
            props.keySet().findAll { it.toString().toLowerCase() == lcaseKey }.each {
                result[key] = props.remove(it)
            }
        }
        return result
    }

    /**
     * Case insensitive containsKey() on map.
     *
     * @param key The key to lookup
     * @return True if the key exists in the map, matched in a case-insensitive manner
     */
    public static boolean containsKeyCaseInsensitive(Map props, Object key) {
        boolean result = props.containsKey(key)
        if (!result) {
            key = key?.toString()?.toLowerCase()
            result = props.find { k,v -> k.toString().toLowerCase() == key }
        }
        return result
    }
    
    /**
     * Case insensitive get() on map.
     *
     * @param key The key to lookup
     * @return The case-sensitive value if matches. Otherwise, the last non-null case-insensitive match
     */
    public static Object getCaseInsensitive(Map props, Object key) {
        def result = props.get(key)
        if (result == null) {
            key = key?.toString()?.toLowerCase()
            result = props.find { k,v -> if (k.toString().toLowerCase() == key && v != null) v }
        }
        return result
    }

    /**
     * Gets superclass that all members of specified collection have in common
     */
    public static Class getCommonSuperclass(Object value) {
        Class result = value?.class
        if (value && (value instanceof Collection || value.class.array) ) {
            value = (value as List).findAll { it != null }
            if (value) {
                result = value[0].class
                boolean verified = false
                while(!verified && result && result != String && result != Object) {
                    verified = true
                    for (v in value) {
                        if (! result.isAssignableFrom(v.class)) {
                            verified = false
                            result = result.superclass
                            break
                        }
                    }
                }
            }
        }
        return result
    }
    
    /**
     * Checks if the value is a number
     */
    public static boolean isNumber(value) {
        return value != null && (
            value instanceof Number ||
            int.class.isAssignableFrom(value.class) ||
            long.class.isAssignableFrom(value.class) ||
            float.class.isAssignableFrom(value.class) ||
            double.class.isAssignableFrom(value.class)
        )
    }
}
