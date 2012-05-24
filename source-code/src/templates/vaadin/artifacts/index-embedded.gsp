<!doctype html>
<html>
	<head>
		<meta name="layout" content="main">
		<title>Welcome to Grails</title>
		
		<!-- Define the application configuration -->
		<script type="text/javascript">
			var vaadin = {
				vaadinConfigurations: {
					vaadin: {
						appUri:"${resource(dir:grailsApplication.config.vaadin?.contextRelativePath)}",
						pathInfo:"${resource(dir:'/')}${controllerName}?embedded=true",
						themeUri:"${resource(dir:'VAADIN/themes/main')}",
						versionInfo : {}
					}
				}};
		</script>
		<!-- Load the widget set, that is, the Client-Side Engine -->
		<script language="javascript" src="${resource(dir:'VAADIN/widgetsets')}/com.vaadin.terminal.gwt.DefaultWidgetSet/com.vaadin.terminal.gwt.DefaultWidgetSet.nocache.js"></script>
		<!-- Load the style sheet -->
		<link rel="stylesheet" href="${resource(dir:'VAADIN/themes/main')}/styles.css" type="text/css"/>
	</head>
	<body>
		<!-- GWT requires an invisible history frame is needed for -->
		<!-- page/fragment history in the browser -->
		<iframe tabIndex="-1" id="__gwt_historyFrame" style="position:absolute;width:0;height:0;border:0;overflow:hidden;" src="javascript:false"></iframe>
	
		<div id="vaadin"></div>
	</body>
</html>
