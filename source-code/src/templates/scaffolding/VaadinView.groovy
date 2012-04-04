<%=packageName ? "package ${packageName}\n\n" : ''%>import org.grails.plugin.vaadin.ui.GrailsButton;
<% import grails.persistence.Event %>
import org.grails.plugin.vaadin.ui.DefaultCreate
import org.grails.plugin.vaadin.ui.DefaultEdit
import org.grails.plugin.vaadin.ui.DefaultList
import org.grails.plugin.vaadin.ui.DefaultShow

import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.terminal.Sizeable;
import com.vaadin.terminal.UserError;
import com.vaadin.ui.Component;
import com.vaadin.ui.DefaultFieldFactory;
import com.vaadin.ui.Field;
import com.vaadin.ui.Form;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.BaseTheme;

class ${className}VaadinView {
    <%  excludedProps = Event.allEvents.toList() << 'id' << 'version'
        allowedNames = domainClass.persistentProperties*.name << 'dateCreated' << 'lastUpdated'
        props = domainClass.properties.findAll { allowedNames.contains(it.name) && !excludedProps.contains(it.name) && it.type != null && !Collection.isAssignableFrom(it.type) }
        Collections.sort(props, comparator.constructors[0].newInstance([domainClass] as Object[])) %>
    def list() {
        // Prepare global vars
        def entityName = message(code:'${domainClass.propertyName}.label', default:'${className}')
        
        // Construct Data Table
        def listTable = new Table()
        def listData = new BeanItemContainer(${className}.class, model.${propertyName}List)
        listTable.containerDataSource = listData
        listTable.pageLength = Math.min(listTable.size(), 20) // Fit to 20 records max
        listTable.setSizeFull()
        
        // Prepare fields list
        def fields = [<%
            props.eachWithIndex { p, i ->
                if (i < 6) { %>${i > 0 ? ',' : ''}
            "${p.name}"<%
                }
            } %>
        ]
        def headers = [<%
            props.eachWithIndex { p, i ->
                if (i < 6) { %>${i > 0 ? ',' : ''}
            message(code:"${domainClass.propertyName}.${p.name}.label", default:"${p.naturalName}")<%
                }
            } %>
        ]
        
        // Determines which fields are shown, and in which order:
        listTable.visibleColumns = fields as String[]
        listTable.columnHeaders = headers as String[]

        <%  linkProp = props ? props[0].name : 'id' %>
        // Generate the first-column link
        listTable.addGeneratedColumn("${linkProp}", new Table.ColumnGenerator() {
            public Component generateCell(Table source, Object itemId, Object columnId) {
                Item item = listTable.getItem(itemId);
                String id = item.getItemProperty("id")
                String caption = item.getItemProperty("${linkProp}").getValue()
                GrailsButton showLink = new GrailsButton(caption, [action:"show",id:id])
                showLink.addStyleName(BaseTheme.BUTTON_LINK)
                return showLink
            }
        });
    
        // Construct layout
        def tableContainer = new VerticalLayout()
        tableContainer.addComponent(listTable)
        def listLayout = new DefaultList(entityName)
        listLayout.body = tableContainer
        listLayout.showFlash()
        application.mainWindow.body = listLayout
    }
            
    <%  excludedProps = Event.allEvents.toList() << 'version' << 'dateCreated' << 'lastUpdated'
        persistentPropNames = domainClass.persistentProperties*.name
        boolean hasHibernate = pluginManager?.hasGrailsPlugin('hibernate')
        if (hasHibernate && org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainBinder.getMapping(domainClass)?.identity?.generator == 'assigned') {
            persistentPropNames << domainClass.identifier.name
        }
        props = domainClass.properties.findAll { persistentPropNames.contains(it.name) && !excludedProps.contains(it.name) }
        Collections.sort(props, comparator.constructors[0].newInstance([domainClass] as Object[]))
        
        def extractDisplayedPropertyDetails(p, owningClass, prefix = "") {
            boolean hasHibernate = pluginManager?.hasGrailsPlugin('hibernate')
            boolean display = true
            boolean required = false
            if (hasHibernate) {
                cp = owningClass.constrainedProperties[p.name]
                display = (cp ? cp.display : true)
                required = (cp ? !(cp.propertyType in [boolean, Boolean]) && !cp.nullable && (cp.propertyType != String || !cp.blank) : false)
            }
            return display ? [property:p, fullName:"${prefix}${p.name}", required:required] : null
        }
        
        displayedProps = []
        for (p in props) {
            if (p.embedded) {
                def embeddedPropNames = p.component.persistentProperties*.name
                def embeddedProps = p.component.properties.findAll { embeddedPropNames.contains(it.name) && !excludedProps.contains(it.name) }
                Collections.sort(embeddedProps, comparator.constructors[0].newInstance([p.component] as Object[]))
                for (ep in embeddedProps) {
                    def details = extractDisplayedPropertyDetails(ep, p.component, "${p.name}.")
                    if (details) {
                        displayedProps << details
                    }
                }
            } else {
                def details = extractDisplayedPropertyDetails(p, domainClass)
                if (details) {
                    displayedProps << details
                }
            }
        } %>
    def create() {
        // Prepare global vars
        def entityName = message(code:'${domainClass.propertyName}.label', default:'${className}')
        
        // Create the Form
        final Form createForm = new Form()
        createForm.writeThrough = false // we want explicit 'apply'
        createForm.invalidCommitted = false // no invalid values in datamodel

        // FieldFactory for customizing the fields and adding validators
        createForm.formFieldFactory = new ${className}FieldFactory(model.${propertyName})
        createForm.itemDataSource = new BeanItem<${className}>(model.${propertyName}) // bind to POJO via BeanItem

        // Prepare field list
        def fields = [<%
            displayedProps.eachWithIndex { p, i -> %>${i > 0 ? ',' : ''}
            "${p.fullName}"<%
            } %>
        ]

        // Determines which fields are shown, and in which order:
        createForm.visibleItemProperties = fields as String[]
        
        // Construct layout
        def formContainer = new VerticalLayout()
        formContainer.addComponent(createForm)
        def createLayout = new DefaultCreate(entityName, model.${propertyName})
        createLayout.body = formContainer
        createLayout.buttons.addCreateButton(onClick:{createForm.commit()})
        createLayout.showFlash()
        application.mainWindow.body = createLayout
    }
    
    def show() {
        // Prepare global vars
        def entityName = message(code:'${domainClass.propertyName}.label', default:'${className}')
        
        // Create the Form
        final Form showForm = new Form()
        showForm.writeThrough = false // we want explicit 'apply'
        showForm.invalidCommitted = false // no invalid values in datamodel
        
        // FieldFactory for customizing the fields and adding validators
        showForm.readOnly = true
        showForm.formFieldFactory = new ${className}FieldFactory(model.${propertyName}, true)
        showForm.itemDataSource = new BeanItem<${className}>(model.${propertyName}) // bind to POJO via BeanItem

        // Prepare field list
        def fields = [<%
            displayedProps.eachWithIndex { p, i -> %>${i > 0 ? ',' : ''}
            "${p.fullName}"<%
            } %>
        ]

        // Determines which fields are shown, and in which order:
        showForm.visibleItemProperties = fields as String[]
        
        // Construct layout
        def formContainer = new VerticalLayout()
        formContainer.addComponent(showForm)
        def showLayout = new DefaultShow(entityName, model.${propertyName})
        showLayout.body = formContainer
        showLayout.buttons.addEditButton()
        showLayout.buttons.addDeleteButton()
        showLayout.showFlash()
        application.mainWindow.body = showLayout
    }
    
    def edit() {
        // Prepare global vars
        def entityName = message(code:'${domainClass.propertyName}.label', default:'${className}')
        
        // Create the Form
        final Form editForm = new Form()
        editForm.writeThrough = false // we want explicit 'apply'
        editForm.invalidCommitted = false // no invalid values in datamodel
        
        // FieldFactory for customizing the fields and adding validators
        editForm.readOnly = false
        editForm.formFieldFactory = new ${className}FieldFactory(model.${propertyName}, false)
        editForm.itemDataSource = new BeanItem<${className}>(model.${propertyName}) // bind to POJO via BeanItem

        // Prepare field list
        def fields = [<%
            displayedProps.eachWithIndex { p, i -> %>${i > 0 ? ',' : ''}
            "${p.fullName}"<%
            } %>
        ]

        // Determines which fields are shown, and in which order:
        editForm.visibleItemProperties = fields as String[]
        
        // Construct layout
        def formContainer = new VerticalLayout()
        formContainer.addComponent(editForm)
        def editLayout = new DefaultEdit(entityName, model.${propertyName})
        editLayout.body = formContainer
        editLayout.buttons.addUpdateButton(onClick:{editForm.commit()})
        editLayout.buttons.addDeleteButton()
        editLayout.showFlash()
        application.mainWindow.body = editLayout
    }

    protected class ${className}FieldFactory extends DefaultFieldFactory {
        def instance
        def readOnly
        
        public ${className}FieldFactory(instance, readOnly = false) {
            this.instance = instance
            this.readOnly = readOnly
        }
        
        @Override
        public Field createField(Item item, Object propertyId, Component uiContext) {
            // Use the super class to create a suitable field based on the
            // property type.
            Field f = super.createField(item, propertyId, uiContext)
            
            // Apply any field-specific settings
            <%  displayedProps.eachWithIndex { p,i-> %>${i > 0 ? ' else ' : ''}if (propertyId == "${p.fullName}") {
                f.required = ${true && p.required}
                f.requiredError = "Please enter a ${p.property.naturalName}"
            }<% } %>
            
            // Apply readonly
            f.readOnly = this.readOnly
            
            // Hide 'null'
            if (f instanceof TextField) { f.nullRepresentation = "" }

            // Show errors
            if (!readOnly && instance?.errors?.hasFieldErrors(propertyId)) {
                def errMsg = message(error:instance.errors.getFieldError(propertyId), args:[instance])
                f.componentError = new UserError(errMsg)
            }
            
            return f
        }
    }
}
