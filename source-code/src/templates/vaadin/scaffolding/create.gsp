<%  import grails.persistence.Event

    //
    // NOTE: This block of code is identical between show.gsp, create.gsp and edit.gsp
    //
    excludedProps = Event.allEvents.toList() << 'version' << 'dateCreated' << 'lastUpdated'
    persistentPropNames = domainClass.persistentProperties*.name
    boolean hasHibernate = pluginManager?.hasGrailsPlugin('hibernate')
    if (hasHibernate && org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainBinder.getMapping(domainClass)?.identity?.generator == 'assigned') {
        persistentPropNames << domainClass.identifier.name
    }
    props = domainClass.properties.findAll { persistentPropNames.contains(it.name) && !excludedProps.contains(it.name) }
    Collections.sort(props, comparator.constructors[0].newInstance([domainClass] as Object[]))
    
    boolean isHidden(p, owningClass) {
        boolean hidden = false
        boolean hasHibernate = pluginManager?.hasGrailsPlugin('hibernate')
        if (hasHibernate) {
            def constrainedProp = owningClass.constrainedProperties[p.name]
            hidden = constrainedProp && !constrainedProp.display
        }
        return hidden
    }
    
    displayedProps = []
    for (p in props) {
        if (p.embedded) {
            def embeddedPropNames = p.component.persistentProperties*.name
            def embeddedProps = p.component.properties.findAll { embeddedPropNames.contains(it.name) && !excludedProps.contains(it.name) }
            Collections.sort(embeddedProps, comparator.constructors[0].newInstance([p.component] as Object[]))
            for (ep in embeddedProps) {
                if (!isHidden(ep, p.component)) {
                    displayedProps << [property:ep, parentProperty:p, readOnly:false]
                }
            }
        } else {
            if (!isHidden(p, domainClass)) {
                displayedProps << [property:p, readOnly:false]
            }
        }
    }

%><%=packageName%>

<g:set var="entityName" value="\${message(code: '${domainClass.propertyName}.label', default: '${className}')}" />
<g:if test="\${flash.message}">
	<v:warning>\${flash.message}</v:warning>
</g:if>
<g:hasErrors bean="\${${propertyName}}">
	<v:error>
		<ul class="errors" role="alert">
			<g:eachError bean="\${${propertyName}}" var="error">
			<li <g:if test="\${error in org.springframework.validation.FieldError}">data-field-id="\${error.field}"</g:if>><g:message error="\${error}"/></li>
			</g:eachError>
		</ul>
	</v:error>
</g:hasErrors>
<v:layout name="vaadin">
<v:location name="body">
	<div class="navigation toolbar" role="navigation">
		<v:horizontalLayout spacing="false">
			<v:link icon="images/skin/house.png" controller="home"><g:message code="default.home.label"/></v:link>
			<v:link icon="images/skin/database_table.png" action="list"><g:message code="default.list.label" args="[entityName]"/></v:link>
		</v:horizontalLayout>
	</div>
	<div id="list" class="content scaffold-create" role="main">
		<h1><g:message code="default.create.label" args="[entityName]"/></h1>
		<div>
			<v:verticalLayout>
				<v:form var="createForm" bean="\${${propertyName}}" writeThrough="false" invalidCommitted="true">
					<%  displayedProps.each { p-> %>
${renderEditor(p)}<%  } %>
				</v:form>
			</v:verticalLayout>
		</div>
	</div>
	<div class="buttons toolbar">
		<v:horizontalLayout spacing="false">
			<v:link icon="images/skin/database_add.png" action="save" instance="\${${propertyName}}" onclick="\${v.commit(form:'createForm')}"><g:message code="default.button.create.label"/></v:link>
		</v:horizontalLayout>
	</div>
</v:location>
</v:layout>
