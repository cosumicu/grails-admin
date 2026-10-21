package grailsadmin

import grails.core.GrailsApplication
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder

class AdminController {
    private static final String TERMINAL_ERROR_PREFIX = '\u001B[1;31m[GRAILS-ADMIN ERROR]\u001B[0m'

    static allowedMethods = [save: 'POST', update: 'POST', remove: 'POST', bulk: 'POST',
        authenticate: 'POST', logout: 'POST', login: 'GET', confirmDelete: 'GET']

    AdminRegistryService adminRegistryService
    AdminMetadataService adminMetadataService
    AdminCrudService adminCrudService
    AdminSecurityService adminSecurityService
    AdminAuthenticationService adminAuthenticationService
    AdminAccountService adminAccountService
    AdminLoginThrottleService adminLoginThrottleService
    GrailsApplication grailsApplication
    MessageSource messageSource

    private boolean gate(AdminModel model, String operation = 'view') {
        if (!adminRegistryService.enabled) {
            adminStatus(404, 'Page not found', 'Grails Admin is not available.')
            return false
        }
        if (!adminSecurityService.authenticated()) {
            reportAdminError(operation, model, 'Admin login session is missing or expired')
            denyUnauthenticated()
            return false
        }
        if (model == null) {
            adminStatus(404, 'Page not found', 'This admin page does not exist.')
            return false
        }
        if (!adminSecurityService.allowed(model, operation)) {
            adminStatus(403, 'Access denied', 'You do not have permission to perform this admin action.')
            return false
        }
        true
    }

    private AdminModel model() { adminRegistryService.resolve(params.group?.toString(), params.model?.toString()) }
    private static String pageView(AdminModel model, String page) { model.pageTemplates[page] ?: page }
    private void adminStatus(int code, String title, String message, String actionUrl = '/admin', String actionLabel = 'Admin home') {
        render view: 'error', status: code,
            model: [title: title, message: message, actionUrl: actionUrl, actionLabel: actionLabel]
    }

    def error() {
        String errorId = request.getAttribute(AdminErrorFilter.ERROR_ID_ATTRIBUTE)?.toString()
        if (!errorId) { adminStatus(404, 'Page not found', 'This admin page does not exist.'); return }
        render view: 'error', status: 500,
            model: [title: 'Internal server error', message: 'An unexpected error occurred. Please try again later.', errorId: errorId]
    }
    private Map common(AdminModel model) {
        String logoutUrl = '/admin/logout'
        String changePasswordUrl = grailsApplication.config.getProperty('grailsAdmin.changePasswordUrl', String, '')
        String viewSiteUrl = grailsApplication.config.getProperty('grailsAdmin.viewSiteUrl', String, '/')
        [logoutUrl, changePasswordUrl, viewSiteUrl].findAll().each {
            if (!it.startsWith('/') || it.startsWith('//')) throw new IllegalArgumentException('Admin navigation URLs must be local paths')
        }
        [adminModel: model, groups: adminRegistryService.all().findAll { adminSecurityService.allowed(it, 'view') }.groupBy { it.group },
         username: adminSecurityService.username(), adminSecurityService: adminSecurityService,
         logoutUrl: logoutUrl, changePasswordUrl: changePasswordUrl, viewSiteUrl: viewSiteUrl]
    }

    def index() {
        if (!adminRegistryService.enabled) { adminStatus(404, 'Page not found', 'Grails Admin is not available.'); return }
        if (!adminSecurityService.authenticated()) { denyUnauthenticated(); return }
        if (!adminSecurityService.allowed(null, 'view')) { adminStatus(403, 'Access denied', 'You do not have permission to view Grails Admin.'); return }
        render view: 'index', model: common(null)
    }

    private void denyUnauthenticated() {
        if (request.method == 'GET') {
            String uri = request.requestURI?.substring(request.contextPath?.length() ?: 0)
            if (uri == '/admin' || uri?.startsWith('/admin/'))
                request.getSession(true).setAttribute('grailsAdminReturnTo', uri)
            redirect uri: '/admin/login'
        }
        else adminStatus(403, 'Session expired', 'Sign in again, then retry from a fresh form.', '/admin/login', 'Sign in')
    }

    def login() {
        if (!adminRegistryService.enabled) { adminStatus(404, 'Page not found', 'Grails Admin is not available.'); return }
        if (!adminAccountService.configured()) { adminStatus(503, 'Setup required', 'Run the admin setup command on the server.'); return }
        if (adminAuthenticationService.authenticated()) { redirect uri: '/admin'; return }
        render view: 'login'
    }

    def authenticate() {
        if (!adminRegistryService.enabled) { adminStatus(404, 'Page not found', 'Grails Admin is not available.'); return }
        if (!adminAccountService.configured()) { adminStatus(503, 'Setup required', 'Run the admin setup command on the server.'); return }
        withForm {
            if (!adminLoginThrottleService.permitted(request.remoteAddr)) { render status: 429; return }
            if (adminAuthenticationService.login(params.username?.toString(), params.password?.toString())) {
                adminLoginThrottleService.succeeded(request.remoteAddr)
                String destination = request.getSession(false)?.getAttribute('grailsAdminReturnTo')?.toString()
                request.getSession(false)?.removeAttribute('grailsAdminReturnTo')
                redirect uri: (destination == '/admin' || destination?.startsWith('/admin/')) ? destination : '/admin'
            } else {
                adminLoginThrottleService.failed(request.remoteAddr)
                render view: 'login', model: [error: 'Invalid username or password'], status: 401
            }
        }.invalidToken { adminStatus(403, 'Form expired', 'Reload the sign-in page and try again.', '/admin/login', 'Sign in') }
    }

    def logout() {
        if (!adminRegistryService.enabled) { adminStatus(404, 'Page not found', 'Grails Admin is not available.'); return }
        withForm {
            adminAuthenticationService.logout()
            redirect uri: '/admin/login'
        }.invalidToken { adminStatus(403, 'Form expired', 'Reload the admin page and try again.') }
    }

    def list() {
        AdminModel m = model()
        if (!gate(m)) return
        try {
            Map data = adminCrudService.page(m, params)
            Map meta = adminMetadataService.inspect(m.domainClass)
            Map linkParams = [q: data.query]
            m.listFilter.each { field ->
                if (data.filters.containsKey(field)) linkParams["filter_${field}"] = params["filter_${field}"]?.toString()
            }
            Map viewModel = common(m) + data + [metadata: meta, linkParams: linkParams,
                bulkActions: adminCrudService.availableBulkActions(m),
                visibleFilters: m.listFilter.findAll { adminSecurityService.readable(m, it) },
                searchable: m.searchFields.any { adminSecurityService.readable(m, it) }]
            render view: pageView(m, 'list'), model: viewModel
        } catch (IllegalArgumentException ignored) { adminStatus(400, 'Invalid search', 'Check the search and filter values, then try again.') }
          catch (SecurityException ignored) { adminStatus(403, 'Access denied', 'You do not have permission to view these records.') }
    }

    def show() {
        AdminModel m = model()
        if (!gate(m)) return
        def record
        try { record = adminCrudService.get(m, params.id?.toString(), 'view') }
        catch (SecurityException ignored) { adminStatus(403, 'Access denied', 'You do not have permission to view this record.'); return }
        if (!record) { adminStatus(404, 'Record not found', 'This record is unavailable.'); return }
        render view: pageView(m, 'show'), model: common(m) + [record: record, metadata: adminMetadataService.inspect(m.domainClass)]
    }

    def bulk() {
        AdminModel m = model()
        if (!gate(m)) return
        withForm {
            try {
                int count = adminCrudService.bulk(m, params.bulkAction?.toString(), params.list('selectedIds'))
                flash.message = "$count ${m.label} records updated."
                redirect uri: "/admin/${params.group}/${params.model}"
            } catch (SecurityException ignored) { adminStatus(403, 'Access denied', 'You do not have permission to update these records.') }
              catch (IllegalArgumentException ignored) { adminStatus(400, 'Invalid selection', 'Check the selected records and try again.') }
        }.invalidToken { adminStatus(403, 'Form expired', 'Reload the admin page and try again.') }
    }

    def create() {
        AdminModel m = model()
        if (!gate(m, 'add')) return
        form(m, m.domainClass.newInstance(), true)
    }

    def edit() {
        AdminModel m = model()
        if (!gate(m, 'change')) return
        def record
        try { record = adminCrudService.get(m, params.id?.toString(), 'change') }
        catch (SecurityException ignored) { adminStatus(403, 'Access denied', 'You do not have permission to edit this record.'); return }
        if (!record) { adminStatus(404, 'Record not found', 'This record is unavailable.'); return }
        form(m, record, false)
    }

    private void form(AdminModel m, Object record, boolean creating, String saveError = null,
                      Map submittedValues = [:], Map extraFieldErrors = [:]) {
        Map meta = adminMetadataService.inspect(m.domainClass)
        Map options = meta.fields.findAll { it.association }.collectEntries { [(it.name): adminCrudService.options(it)] }
        Map validation = AdminFormErrors.collect(record, messageSource, LocaleContextHolder.locale, meta.byName)
        render view: pageView(m, 'form'), model: common(m) + [record: record, metadata: meta, options: options,
            creating: creating, saveError: saveError, submittedValues: submittedValues,
            fieldErrors: validation.fields + extraFieldErrors, generalErrors: validation.general]
    }

    private static void reportAdminError(String operation, AdminModel model, String detail, Throwable cause = null) {
        String safeDetail = (detail ?: 'Unknown error').replaceAll(/[\r\n\u001B]/, ' ')
        System.err.println("${TERMINAL_ERROR_PREFIX} ${operation} ${model?.key ?: 'unknown'}: ${safeDetail}")
        if (cause) cause.printStackTrace(System.err)
        System.err.flush()
    }

    def save() {
        AdminModel m = model()
        if (!gate(m, 'add')) return
        withForm {
            def record = m.domainClass.newInstance()
            try {
                record = adminCrudService.save(m, record, params, true)
                if (record.hasErrors() || record.id == null) {
                    reportAdminError('create', m, record.hasErrors() ? AdminFormErrors.logSummary(record) : 'No record ID was assigned')
                    response.status = 422
                    form(m, record, true, record.hasErrors() ? null : 'The record was not saved. Please try again.', params)
                    return
                }
                flash.message = "${m.label} saved."
                redirect uri: "/admin/${params.group}/${params.model}/${record.id}"
            } catch (AdminFieldConversionException error) {
                reportAdminError('create', m, "Invalid ${error.fieldName}")
                response.status = 422
                Map definition = adminMetadataService.inspect(m.domainClass).byName[error.fieldName]
                form(m, record, true, 'Correct the highlighted field and try again.', params,
                    [(error.fieldName): AdminFormErrors.conversionMessage(error, definition)])
            } catch (SecurityException error) {
                reportAdminError('create', m, error.message, error)
                adminStatus(403, 'Access denied', 'You do not have permission to save this record.')
            }
        }.invalidToken {
            reportAdminError('create', m, 'Invalid or expired form token')
            adminStatus(403, 'Form expired', 'This form has expired or was already submitted. Reload it before trying again.')
        }
    }

    def update() {
        AdminModel m = model()
        if (!gate(m, 'change')) return
        withForm {
            def record
            try {
                record = adminCrudService.get(m, params.id?.toString(), 'change')
                if (!record) {
                    reportAdminError('update', m, 'Record was not found for editing')
                    adminStatus(404, 'Record not found', 'This record is unavailable.')
                    return
                }
                record = adminCrudService.save(m, record, params, false)
                if (record.hasErrors() || record.id == null) {
                    reportAdminError('update', m, record.hasErrors() ? AdminFormErrors.logSummary(record) : 'No record ID was assigned')
                    response.status = 422
                    form(m, record, false, record.hasErrors() ? null : 'The changes were not saved. Please try again.', params)
                    return
                }
                flash.message = "${m.label} saved."
                redirect uri: "/admin/${params.group}/${params.model}/${record.id}"
            } catch (AdminFieldConversionException error) {
                reportAdminError('update', m, "Invalid ${error.fieldName}")
                if (!record) { adminStatus(404, 'Record not found', 'This record is unavailable.'); return }
                response.status = 422
                Map definition = adminMetadataService.inspect(m.domainClass).byName[error.fieldName]
                form(m, record, false, 'Correct the highlighted field and try again.', params,
                    [(error.fieldName): AdminFormErrors.conversionMessage(error, definition)])
            } catch (SecurityException error) {
                reportAdminError('update', m, error.message, error)
                adminStatus(403, 'Access denied', 'You do not have permission to save this record.')
            }
        }.invalidToken {
            reportAdminError('update', m, 'Invalid or expired form token')
            adminStatus(403, 'Form expired', 'This form has expired or was already submitted. Reload it before trying again.')
        }
    }

    def confirmDelete() {
        AdminModel m = model()
        if (!gate(m, 'delete')) return
        def record
        try { record = adminCrudService.get(m, params.id?.toString(), 'delete') }
        catch (SecurityException ignored) { adminStatus(403, 'Access denied', 'You do not have permission to delete this record.'); return }
        if (!record) { adminStatus(404, 'Record not found', 'This record is unavailable.'); return }
        render view: pageView(m, 'delete'), model: common(m) + [record: record]
    }

    def remove() {
        AdminModel m = model()
        if (!gate(m, 'delete')) return
        withForm {
            try {
                def record = adminCrudService.get(m, params.id?.toString(), 'delete')
                if (!record) { adminStatus(404, 'Record not found', 'This record is unavailable.'); return }
                adminCrudService.delete(m, record)
                redirect uri: "/admin/${params.group}/${params.model}"
            } catch (SecurityException ignored) { adminStatus(403, 'Access denied', 'You do not have permission to delete this record.') }
        }.invalidToken { adminStatus(403, 'Form expired', 'Reload the admin page and try again.') }
    }
}
