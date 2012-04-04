package org.grails.plugin.vaadin

import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsApplication;

/**
 * Constructs a list of Vaadin Classes and Artefacts from
 * the specified GrailsApplication.
 * <p>
 * A class is considered a Vaadin Class if it:
 * <ul>
 * <li>Has 'vaadin' in the package name of itself, or any of its superclasses.</li>
 * <li>Is a Vaadin Artefact (see below)</li>
 * </ul>
 * <p>
 * A class is considered a Vaadin Artefact if it:
 * <ul>
 * <li>Has 'VaadinController' or 'VaadinView' at the end of the class name.</li>
 * </ul>
 * 
 * @author Francis McKenzie
 */
class VaadinClasses {
    def log = LogFactory.getLog(this.class)
    
    /**
     * String(s) to scan for in classname to determine if
     * a class is a Vaadin class
     */
    protected List<String> VAADIN_CLASSNAME_IDENTIFIERS = [
        "vaadin"
    ]
    /**
     * String(s) to scan for in classname to determine if
     * a class is a Vaadin artefact, and the corresponding artefact
     * type if found
     */
    protected Map<String,String> VAADIN_ARTEFACT_IDENTIFIERS = [
        "VaadinController": "controller",
        "VaadinView": "view"
    ]

    /**
     * Hold the list of Vaadin classes
     */
    protected List<Class> allClasses = []
    /**
     * Hold the list of Vaadin artefacts
     */
    protected List<VaadinClass> allArtefacts = []
    
    /**
     * Scans the classes in the GrailsApplication, and generates
     * an internal list of Vaadin Classes and Artefacts.
     * 
     * @param grailsApplication The current Grails Application
     */
    public VaadinClasses(GrailsApplication grailsApplication) {
        grailsApplication.allClasses.each {
            if (isVaadinClass(it)) {
                registerVaadinClass(it)
            }
        }
    }
    
    /**
     * Get the list of Vaadin Artefacts for the specified type.
     * 
     * @param type E.g. 'controller' or 'view'
     * @return List of Vaadin Artefacts for the specified type in the Grails Application.
     */
    public List<VaadinClass> getArtefacts(String type) {
        type == type?.toLowerCase()
        return allArtefacts.findAll{it.type==type}
    }
    
    /**
     * Get all Vaadin classes.
     * 
     * @return List of Vaadin classes in the Grails Application.
     */
    public List<Class> getAllClasses() {
        return allClasses.asImmutable()
    }
    
    /**
     * Look up a Vaadin Artefact by logical property name. E.g. find the
     * 'BookVaadinController' class by specifying property name 'book'
     * and type 'controller'
     * 
     * @param type The artefact type, e.g. 'controller', 'view'
     * @param propertyName The logical property name, e.g. 'book'
     * @return The Vaadin artefact for the specified type and name
     */
    public VaadinClass getArtefactByLogicalPropertyName(String type, String propertyName) {
        if (! (type && propertyName)) {
            throw new IllegalArgumentException("Type and property name required")
        } else {
            type = type.toLowerCase()
            return allArtefacts.find { it.type==type && it.propertyName==propertyName }
        }
    }
    
    /**
     * Checks if a class qualifies as a Vaadin class (see description at top).
     * Used internally when initially creating the list of of Vaadin classes.
     * 
     * @param clazz The candidate class
     * @return true if the specified class is considered a Vaadin class
     */
    protected boolean isVaadinClass(Class clazz) {
        // Copied from vaadin plugin
        // A class is a vaadin class if it has 'vaadin' in the fully qualified class name
        boolean result = false
        if (! (clazz == null || Closure.class.isAssignableFrom(clazz) || GrailsClassUtils.isJdk5Enum(clazz)) ) {
            result = isVaadinArtefact(clazz)
            if (!result) {
                Class<?> testClass = clazz;
                while (testClass != null && !testClass.equals(GroovyObject.class) && !testClass.equals(Object.class)) {
                    if (VAADIN_CLASSNAME_IDENTIFIERS.find { testClass.name.contains(it) }) {
                        result = true;
                        break;
                    }
                    testClass = testClass.superclass
                }
            }
        }
        return result
    }
    
    /**
     * Checks if a class qualifies as a Vaadin artefact (see description at top).
     * Used internally when initially creating the list of of Vaadin artefacts.
     * 
     * @param clazz The candidate class
     * @return true if the specified class is considered a Vaadin artefact
     */
    protected boolean isVaadinArtefact(Class clazz) {
        return clazz && VAADIN_ARTEFACT_IDENTIFIERS.keySet().find { clazz.name.endsWith(it) }
    }

    /**
     * Adds specified class to class list, if it is a Vaadin class.
     * 
     * @param clazz The candidate class
     */
    protected registerVaadinClass(Class clazz) {
        if (! isVaadinClass(clazz)) {
            throw new IllegalArgumentException("Not a Vaadin Class ${clazz}")
        } else {
            this.allClasses.remove(clazz) // We have to replace existing, in case its been reloaded
            this.allClasses.add(clazz)

            if (isVaadinArtefact(clazz)) {
                registerVaadinArtefact(clazz)
            } else {
                if (log.isDebugEnabled()) {
                    log.debug "CLASS: ${clazz}"
                }
            }
        }
    }
    
    /**
     * Adds specified class to artefact list, if it is a Vaadin artefact.
     * 
     * @param clazz The candidate class
     */
    protected registerVaadinArtefact(Class clazz) {
        if (! isVaadinArtefact(clazz)) {
            throw new IllegalArgumentException("Not a Vaadin Artefact ${clazz}")
        } else {
            // Get type and property name
            def kv = VAADIN_ARTEFACT_IDENTIFIERS.entrySet().find { clazz.name.endsWith(it.key) }
            String suffix = kv.key
            String type = kv.value
            String propertyName = clazz.name - suffix - (clazz.package.name + ".")
            propertyName = propertyName[0].toLowerCase() + propertyName.substring(1)
            VaadinClass artefact = new VaadinClass(type, clazz, propertyName)
            allArtefacts.remove(artefact) // We have to replace existing, in case its been reloaded
            allArtefacts << artefact
            if (log.isDebugEnabled()) {
                log.debug "ARTEFACT: ${artefact}"
            }
        }
    }
}
