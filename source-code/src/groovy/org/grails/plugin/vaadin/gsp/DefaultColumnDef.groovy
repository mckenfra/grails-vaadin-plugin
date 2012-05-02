package org.grails.plugin.vaadin.gsp

import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;

class DefaultColumnDef implements ColumnDef {
    protected String name
    String header
    Table.ColumnGenerator generator
    
    public DefaultColumnDef(String name, String header = null, Table.ColumnGenerator generator = null) {
        if (!name) {
            throw new IllegalArgumentException("Column must have a name!")
        }
        this.name = name
        this.header = header
        this.generator = generator
    }
    
    public String getHeader() {
        return this.header ?: this.name
    }

    public String getName() {
        return this.name
    }
}
