/*
 * Copyright 2010 Les Hazlewood
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

import org.codehaus.groovy.grails.commons.ArtefactHandlerAdapter;
import org.grails.plugin.vaadin.VaadinClasses;

/**
 * This now depends on {@link org.grails.plugin.vaadin.VaadinClasses}
 * to determine if a class is a Vaadin Class
 * 
 * @author Les Hazlewood, Francis McKenzie
 * @since 1.2
 */
public class VaadinArtefactHandler extends ArtefactHandlerAdapter {
    public static final String TYPE = "Vaadin";

    public static boolean isVaadinClass(final Class<?> clazz) {
        return VaadinClasses.isVaadinClass(clazz);
    }

    public VaadinArtefactHandler() {
        super(VaadinArtefactHandler.TYPE, VaadinGrailsClass.class, DefaultVaadinGrailsClass.class, null);
    }

    @Override
    public boolean isArtefactClass(final Class clazz) {
        return VaadinArtefactHandler.isVaadinClass(clazz);
    }
}
