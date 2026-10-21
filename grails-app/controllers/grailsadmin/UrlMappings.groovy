package grailsadmin

class UrlMappings {
    static mappings = {
        '/admin'(controller: 'admin', action: 'index')
        '/admin/login'(controller: 'admin', action: 'login')
        '/admin/authenticate'(controller: 'admin', action: 'authenticate')
        '/admin/logout'(controller: 'admin', action: 'logout')
        '/admin/error'(controller: 'admin', action: 'error')
        "/admin/$group/$model/create"(controller: 'admin', action: 'create')
        "/admin/$group/$model/save"(controller: 'admin', action: 'save')
        "/admin/$group/$model/bulk"(controller: 'admin', action: 'bulk')
        "/admin/$group/$model/$id/edit"(controller: 'admin', action: 'edit')
        "/admin/$group/$model/$id/delete"(controller: 'admin', action: 'confirmDelete')
        "/admin/$group/$model/$id/update"(controller: 'admin', action: 'update')
        "/admin/$group/$model/$id/remove"(controller: 'admin', action: 'remove')
        "/admin/$group/$model/$id"(controller: 'admin', action: 'show')
        "/admin/$group/$model"(controller: 'admin', action: 'list')
    }
}
