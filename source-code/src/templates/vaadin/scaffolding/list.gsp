<%  import grails.persistence.Event

    excludedProps = Event.allEvents.toList() << 'id' << 'version'
    allowedNames = domainClass.persistentProperties*.name << 'dateCreated' << 'lastUpdated'
    props = domainClass.properties.findAll { allowedNames.contains(it.name) && !excludedProps.contains(it.name) && it.type != null && !Collection.isAssignableFrom(it.type) }
    Collections.sort(props, comparator.constructors[0].newInstance([domainClass] as Object[]))

%><%=packageName%>
<%= '<' + '%@ page import="com.vaadin.data.util.BeanItemContainer" %' + '>' %>

<g:set var="entityName" value="\${message(code: '${domainClass.propertyName}.label', default: '${className}')}" />
<v:mainWindow caption="\${message(code:'default.list.label', args:[entityName])}"/>
<g:if test="\${flash.message}">
	<v:warning>\${flash.message}</v:warning>
</g:if>
<v:layout name="main">
<v:location name="body">
	<div class="navigation toolbar" role="navigation">
		<v:horizontalLayout spacing="false">
			<v:link icon="images/skin/house.png" controller="home"><g:message code="default.home.label"/></v:link>
			<v:link icon="images/skin/database_add.png" action="create"><g:message code="default.new.label" args="[entityName]"/></v:link>
		</v:horizontalLayout>
	</div>
	<div id="list-${domainClass.propertyName}" class="content scaffold-list" role="main">
		<h1><g:message code="default.list.label" args="[entityName]"/></h1>
		<div>
			<v:verticalLayout>
				<v:table sizeFull="true" pageLength="\${Math.min(${propertyName}Total, 10)}" containerDataSource="\${new BeanItemContainer(${className}.class, ${propertyName}List)}">
		        <% props.eachWithIndex { p, i ->
		               if (i == 0) { %>
					<v:column name="${p.name}" generator="\${{item->v.createLink(action:'show', class:'link', id:item.getItemProperty('id'), caption:item.getItemProperty('${p.name}'))}}"><g:message code="${domainClass.propertyName}.${p.name}.label" default="${p.naturalName}"/></v:column>
                <%     } else if (i < 6) { %>
					<v:column name="${p.name}"><g:message code="${domainClass.propertyName}.${p.name}.label" default="${p.naturalName}"/></v:column>
		        <% } } %>
				</v:table>
			</v:verticalLayout>
		</div>
	</div>
</v:location>
</v:layout>