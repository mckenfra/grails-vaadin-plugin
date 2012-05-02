package org.grails.plugin.vaadin.gsp

import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;

interface ColumnDef {
    public String getName();
    public String getHeader();
    public Table.ColumnGenerator getGenerator();
}
