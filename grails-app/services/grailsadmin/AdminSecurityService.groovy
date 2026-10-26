package grailsadmin

import grails.core.GrailsApplication

class AdminSecurityService {

    GrailsApplication grailsApplication
    AdminRegistryService adminRegistryService
    AdminMetadataService adminMetadataService
    AdminAuthenticationService adminAuthenticationService
    private getContext() { grailsApplication.mainContext }

    private Object policy() {
        context.containsBean('adminAccessPolicy') ? context.getBean('adminAccessPolicy') : null
    }

    boolean authenticated() {
        adminAuthenticationService.authenticated()
    }

    String username() {
        adminAuthenticationService.username()
    }

    boolean allowed(AdminModel model, String operation) {
        if (!authenticated()) return false
        if (model && ((operation == 'add' && !model.allowCreate) || {
            (operation == 'change' && !model.allowChange) || (operation == 'delete' && !model.allowDelete))) return false
        }
        def custom = policy()
        if (custom) return custom.authenticated() == true && custom.allowed(model, operation) == true
        true
    }

    boolean readable(AdminModel model, String field) {
        if (!allowed(model, 'view')) return false
        def custom = policy()
        custom ? custom.readable(model, field) == true : true
    }

    boolean writable(AdminModel model, String field) {
        if (model.readonlyFields.contains(field) || !model.formFields.contains(field) || !allowed(model, 'change')) return false
        def custom = policy()
        custom ? custom.writable(model, field) == true : true
    }

    boolean fieldWritable(AdminModel model, String field, String operation) {
        if (model.readonlyFields.contains(field) || !model.formFields.contains(field) || !allowed(model, operation)) return false
        Map definition = adminMetadataService.inspect(model.domainClass).byName[field]
        if (!definition) return false
        if (definition.association) {
            AdminModel target = adminRegistryService.all().find { it.domainClass == definition.type }
            if (!target || !allowed(target, 'view')) return false
        }
        def custom = policy()
        custom ? custom.writable(model, field) == true : true
    }

}
