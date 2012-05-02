package org.grails.plugin.vaadin.gsp

import com.vaadin.data.Item
import com.vaadin.ui.Field

interface FieldDef {
    public String getName()
    public Field createField()
    public void configureField(Field field, Item item)
}
