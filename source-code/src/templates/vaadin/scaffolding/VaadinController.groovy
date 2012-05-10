<%=packageName ? "package ${packageName}\n\n" : ''%>import org.springframework.dao.DataIntegrityViolationException

class ${className}VaadinController {
    def index() {
        redirect(action: "list", params: params)
    }

    def list() {
        [${propertyName}List: ${className}.list(params), ${propertyName}Total: ${className}.count()]
    }

    def create() {
        [${propertyName}: new ${className}(params)]
    }

    def save() {
        def ${propertyName} = params.instance
        if (! (${propertyName} instanceof ${className})) {
            redirect(action:"create")
            return
        }

        if (!${propertyName}.save(flush: true)) {
            render(view: "create", model: [${propertyName}: ${propertyName}])
            return
        }

		flash.message = message(code: 'default.created.message', args: [message(code: '${domainClass.propertyName}.label', default: '${className}'), ${propertyName}.id])
        redirect(action: "show", id: ${propertyName}.id)
    }

    def show() {
        def ${propertyName} = ${className}.get(params.id)
        if (!${propertyName}) {
			flash.message = message(code: 'default.not.found.message', args: [message(code: '${domainClass.propertyName}.label', default: '${className}'), params.id])
            redirect(action: "list")
            return
        }

        [${propertyName}: ${propertyName}]
    }

    def edit() {
        def ${propertyName} = ${className}.get(params.id)
        if (!${propertyName}) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: '${domainClass.propertyName}.label', default: '${className}'), params.id])
            redirect(action: "list")
            return
        }

        [${propertyName}: ${propertyName}]
    }

    def update() {
        if (! (params.instance instanceof ${className})) {
            redirect(action:"show", params:params)
            return
        }
        
        // Attempt to merge ${className} instance into new persistence context
        def mergedAndValidated = params.instance.merge()
        if (!mergedAndValidated) {
            render(view: "edit", model: [${propertyName}: params.instance])
            return
        }
        params.instance = mergedAndValidated
        
        def ${propertyName} = ${className}.get(params.instance.id)
        if (!${propertyName}) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: '${domainClass.propertyName}.label', default: '${className}'), params.instance.id])
            redirect(action: "list")
            return
        }

        if (${propertyName}.version > params.instance.version) {<% def lowerCaseName = grails.util.GrailsNameUtils.getPropertyName(className) %>
            ${propertyName}.errors.rejectValue("version", "default.optimistic.locking.failure",
                      [message(code: '${domainClass.propertyName}.label', default: '${className}')] as Object[],
                      "Another user has updated this ${className} while you were editing")
            render(view: "edit", model: [${propertyName}: ${propertyName}])
            return
        }

        ${propertyName}.properties = params.instance.properties

        if (!${propertyName}.save(flush: true)) {
            render(view: "edit", model: [${propertyName}: ${propertyName}])
            return
        }

		flash.message = message(code: 'default.updated.message', args: [message(code: '${domainClass.propertyName}.label', default: '${className}'), ${propertyName}.id])
        redirect(action: "show", id: ${propertyName}.id)
    }

    def delete() {
        def ${propertyName} = ${className}.get(params.id)
        if (!${propertyName}) {
			flash.message = message(code: 'default.not.found.message', args: [message(code: '${domainClass.propertyName}.label', default: '${className}'), params.id])
            redirect(action: "list")
            return
        }

        try {
            ${propertyName}.delete(flush: true)
			flash.message = message(code: 'default.deleted.message', args: [message(code: '${domainClass.propertyName}.label', default: '${className}'), params.id])
            redirect(action: "list")
        }
        catch (DataIntegrityViolationException e) {
			flash.message = message(code: 'default.not.deleted.message', args: [message(code: '${domainClass.propertyName}.label', default: '${className}'), params.id])
            redirect(action: "show", id: params.id)
        }
    }
}
