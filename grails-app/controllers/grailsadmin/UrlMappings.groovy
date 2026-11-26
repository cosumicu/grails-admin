package grailsadmin

import grails.util.Holders

class UrlMappings {

    static mappings = {
        String base = AdminPath.from(Holders.config)
        "$base"(controller: 'admin', action: 'index')
        "$base/login"(controller: 'admin', action: 'login')
        "$base/authenticate"(controller: 'admin', action: 'authenticate')
        "$base/logout"(controller: 'admin', action: 'logout')
        "$base/error"(controller: 'admin', action: 'error')
        "$base/$group/$model/create"(controller: 'admin', action: 'create')
        "$base/$group/$model/save"(controller: 'admin', action: 'save')
        "$base/$group/$model/bulk"(controller: 'admin', action: 'bulk')
        "$base/$group/$model/$id/edit"(controller: 'admin', action: 'edit')
        "$base/$group/$model/$id/delete"(controller: 'admin', action: 'confirmDelete')
        "$base/$group/$model/$id/update"(controller: 'admin', action: 'update')
        "$base/$group/$model/$id/remove"(controller: 'admin', action: 'remove')
        "$base/$group/$model/$id"(controller: 'admin', action: 'show')
        "$base/$group/$model"(controller: 'admin', action: 'list')
    }

}
