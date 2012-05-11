/*
 * Copyright 2008 Les Hazlewood
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.grails;

import groovy.util.ConfigObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.codehaus.groovy.grails.commons.ApplicationHolder;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;

import com.vaadin.grails.terminal.gwt.server.GrailsAwareApplicationServlet;

/**
 * Vaadin plugin utility methods - mostly used for supporting dynamic method.
 * 
 * @author Les Hazlewood
 * @since 1.2
 */
public class VaadinUtils {
    private static final transient Logger log = LoggerFactory.getLogger(VaadinUtils.class);

    public static <T> T getBean(final Class<T> clazz) throws BeansException {
        return ApplicationHolder.getApplication().getMainContext().getBean(clazz);
    }

    public static Object getBean(final String name) throws BeansException {
        return ApplicationHolder.getApplication().getMainContext().getBean(name);
    }

    public static MessageSource getMessageSource() {
        return ApplicationHolder.getApplication().getMainContext().getBean(MessageSource.class);
    }

    /**
     * Localization methods, providing access to i18n values.
     * 
     * @param key
     *            for localization properties
     * @param args
     *            arguments, e.g. "Hallo {0}"
     * @param locale
     *            locale
     * @return value from properties file or key (if key value is not found)
     */
    public static String i18n(final String key, final Object[] args, final Locale locale) {
        String message = null;
        try {
            message = VaadinUtils.getMessageSource().getMessage(key, args, locale);
        } catch (final Throwable t) {
            System.err.println(t.getMessage());
        }
        if (message == null) {
            // if fetching values fails, return the key
            message = "[" + key + "]";
        }
        return message;
    }

    /**
     * Localization methods, providing access to i18n values.
     * 
     * @param key
     *            for localization properties
     * @param args
     *            arguments, e.g. "Hello {0}"
     * @defaultValue
     * @param locale
     *            locale
     * @return value from properties file or key (if key value is not found)
     */
    public static String i18n(final String key, final Object[] args, final String defaultValue, final Locale locale) {
        String message = null;
        try {
            message = VaadinUtils.getMessageSource().getMessage(key, args, defaultValue, locale);
        } catch (final Throwable t) {
            System.err.println(t.getMessage());
        }
        if (message == null) {
            // if fetching values fails, return the key
            message = "[" + key + "]";
        }
        return message;
    }
    
    /**
     * Localization methods, providing access to i18n values.
     * 
     * @param resolvable
     *            for localization properties
     * @param locale
     *            locale
     * @return value from properties file or key (if key value is not found)
     */
    String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
        String message = null;
        try {
            message = VaadinUtils.getMessageSource().getMessage(resolvable, locale);
        } catch (final Throwable t) {
            System.err.println(t.getMessage());
        }
        if (message == null) {
            if (resolvable != null && resolvable.getCodes().length > 0) {
                // if fetching values fails, return the last code
                message = "[" + resolvable.getCodes()[resolvable.getCodes().length-1] + "]";
            } else {
                message = "[Error]";
            }
        }
        return message;
    }
    
    
    /**
     * Extracts the 'javascriptLibraries' setting from VaadinConfig
     * @return The list of libraries to add to the head element of the page
     */
    public static List<String> getJavascriptLibraries(GrailsApplication grailsApplication) {
        List<String> result = null;
        
        // Get VaadinConfig
        ConfigObject vaadin = (ConfigObject) grailsApplication.getConfig().getProperty("vaadin");
        if (vaadin == null) {
            log.warn("VaadinConfig not found!");
            return null;
        }
        
        // Get 'javascriptLibraries' config setting
        Object javascriptLibraries = null;
        Map<?,?> config = vaadin.flatten();
        if (config != null) {
            javascriptLibraries = config.get("javascriptLibraries");
        }
        if (javascriptLibraries == null) {
            log.warn("Property 'javascriptLibraries' not found in VaadinConfig");
            return null;
        }
        
        // Extract value
        if (javascriptLibraries instanceof List<?>) {
            List<?> libs = (List<?>) javascriptLibraries;
            result = new ArrayList<String>(libs.size());
            for (Object lib : libs) {
                if (lib != null && lib.toString().trim().length() > 0) {
                    result.add(lib.toString().trim());
                }
            }
        } else if (javascriptLibraries.getClass().isArray()) {
            Object[] libs = (Object[]) javascriptLibraries;
            result = new ArrayList<String>(libs.length);
            for (Object lib : libs) {
                if (lib != null && lib.toString().trim().length() > 0) {
                    result.add(lib.toString().trim());
                }
            }
        } else {
            log.warn("VaadinConfig setting 'javascriptLibraries' is of wrong type: " + javascriptLibraries.getClass());
        }
        
        return result;
    }
}
