package org.grails.plugin.vaadin.gsp

import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItem;
import com.vaadin.ui.Field;

class DefaultFieldDef implements FieldDef {
    protected String name
    Class type
    Closure configurer
    Map props

    public DefaultFieldDef(String name, Class type = null, Closure configurer = null, Map props = null) {
        this.name = name
        this.type = type
        this.configurer = configurer
        this.props = props
    }
    
    public String getName() {
        return this.name
    }
    
    public Field createField() {
        return type ? type.newInstance() : null
    }

    public void configureField(Field field, Item item) {
        props.name = this.name
        if (item instanceof BeanItem) props.instance = item.bean
        if (configurer) configurer(props, field)
    }
}
