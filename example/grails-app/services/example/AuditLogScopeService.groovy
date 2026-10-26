package example

import grailsadmin.AdminModel
import grailsadmin.AdminQueryScope

class AuditLogScopeService implements AdminQueryScope {

    Closure criteria(AdminModel model, String operation) { { builder -> builder.eq('actor', 'demo') } }

}
