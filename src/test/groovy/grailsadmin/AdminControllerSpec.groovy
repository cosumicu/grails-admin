package grailsadmin

import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

class AdminControllerSpec extends Specification implements ControllerUnitTest<AdminController> {

    void 'pager supports optional totals and preserves the page size'() {
        given:
        webRequest.controllerName = 'admin'
        def adminModel = new AdminModel('thing', 'General', 'thing', 'Thing', Object,
            [], [], [], [], [], [], true, true, false, null, null, [:], 100, [:], [:], [:])
        params.group = 'general'
        params.model = 'thing'

        when:
        String page = render(template: '/admin/results', model: [adminModel: adminModel,
            metadata: [byName: [:]], records: [], total: total, hasNext: hasNext,
            page: 2, max: 10, sort: 'id', order: 'asc', linkParams: [max: 10], bulkActions: [], adminPath: '/admin'])

        then:
        page.contains('Previous')
        page.contains('max=10')
        page.contains('Next') == hasNext
        page.contains('records</span>') == (total != null)

        where:
        total | hasNext
        null  | true
        null  | false
        20    | false
    }

    void 'create loads choices only for editable configured associations'() {
        given:
        def adminModel = new AdminModel('thing', 'General', 'thing', 'Thing', Expando,
            [], [], [], [], ['readonly'], ['editable', 'readonly', 'denied'], true, true, false,
            null, null, [:], 100, [:], [:], [:])
        Map fields = ['editable', 'readonly', 'denied', 'omitted'].collectEntries {
            [(it): [name: it, association: true, type: Object]]
        }
        params.group = 'general'
        params.model = 'thing'
        controller.adminRegistryService = Stub(AdminRegistryService) {
            isEnabled() >> true
            resolve('general', 'thing') >> adminModel
            all() >> [adminModel]
        }
        controller.adminSecurityService = Stub(AdminSecurityService) {
            authenticated() >> true
            allowed(_, _) >> true
            fieldWritable(adminModel, 'editable', 'add') >> true
        }
        controller.adminMetadataService = Stub(AdminMetadataService) {
            inspect(Expando) >> [fields: fields.values().toList(), byName: fields]
        }
        def crud = Mock(AdminCrudService)
        controller.adminCrudService = crud

        when:
        controller.create()

        then:
        1 * crud.options(fields.editable, null) >> []
        0 * crud.options(_, _)
        model.options == [editable: []]
    }

    void 'disabled plugin hides the dashboard'() {
        given:
        controller.adminRegistryService = Stub(AdminRegistryService) { isEnabled() >> false }
        when:
        controller.index()
        then:
        response.status == 404
        view == '/admin/error'
        model.title == 'Page not found'
    }

    void 'anonymous user is sent to admin login'() {
        given:
        controller.adminRegistryService = Stub(AdminRegistryService) { isEnabled() >> true }
        controller.adminSecurityService = Stub(AdminSecurityService) { authenticated() >> false }
        when:
        controller.index()
        then:
        response.status == 302
        response.redirectedUrl == '/admin/login'
    }

    void 'custom base path is used for login redirect'() {
        given:
        grailsApplication.config.grailsAdmin.basePath = '/backoffice'
        controller.adminRegistryService = Stub(AdminRegistryService) { isEnabled() >> true }
        controller.adminSecurityService = Stub(AdminSecurityService) { authenticated() >> false }

        when:
        controller.index()

        then:
        response.redirectedUrl == '/backoffice/login'

        cleanup:
        grailsApplication.config.grailsAdmin.basePath = '/admin'
    }

    void 'old admin route is rejected when the base path changes'() {
        given:
        grailsApplication.config.grailsAdmin.basePath = '/backoffice'
        request.setRequestURI('/admin/login')

        when:
        controller.login()

        then:
        response.status == 404

        cleanup:
        grailsApplication.config.grailsAdmin.basePath = '/admin'
    }

    void 'unknown registered model returns 404'() {
        given:
        params.group = 'banking'
        params.model = 'missing'
        controller.adminRegistryService = Stub(AdminRegistryService) {
            isEnabled() >> true
            resolve('banking', 'missing') >> null
        }
        controller.adminSecurityService = Stub(AdminSecurityService) { authenticated() >> true }
        when:
        controller.list()
        then:
        response.status == 404
        view == '/admin/error'
        model.title == 'Page not found'
    }

    void 'create without a form token is rejected'() {
        given:
        params.group = 'general'
        params.model = 'thing'
        request.method = 'POST'
        def adminModel = new AdminModel('thing', 'General', 'thing', 'Thing', Object,
            [], [], [], [], [], [], true, true, false, null, null, [:], 100, [:], [:], [:])
        controller.adminRegistryService = Stub(AdminRegistryService) {
            isEnabled() >> true
            resolve('general', 'thing') >> adminModel
        }
        controller.adminSecurityService = Stub(AdminSecurityService) {
            authenticated() >> true
            allowed(adminModel, 'add') >> true
        }
        when:
        controller.save()
        then:
        response.status == 403
        view == '/admin/error'
        model.title == 'Form expired'
    }

    void 'anonymous mutation is rejected before reading a form token'() {
        given:
        request.method = 'POST'
        params.group = 'general'
        params.model = 'thing'
        def adminModel = new AdminModel('thing', 'General', 'thing', 'Thing', Object,
            [], [], [], [], [], [], true, true, false, null, null, [:], 100, [:], [:], [:])
        controller.adminRegistryService = Stub(AdminRegistryService) {
            isEnabled() >> true
            resolve('general', 'thing') >> adminModel
        }
        controller.adminSecurityService = Stub(AdminSecurityService) { authenticated() >> false }

        when:
        controller.save()

        then:
        response.status == 403
        view == '/admin/error'
        model.title == 'Session expired'
    }

    void 'record access revoked during lookup returns forbidden'() {
        given:
        params.group = 'general'
        params.model = 'thing'
        params.id = '1'
        def adminModel = new AdminModel('thing', 'General', 'thing', 'Thing', Object,
            [], [], [], [], [], [], true, true, false, null, null, [:], 100, [:], [:], [:])
        controller.adminRegistryService = Stub(AdminRegistryService) {
            isEnabled() >> true
            resolve('general', 'thing') >> adminModel
        }
        controller.adminSecurityService = Stub(AdminSecurityService) {
            authenticated() >> true
            allowed(adminModel, 'view') >> true
        }
        controller.adminCrudService = Stub(AdminCrudService) {
            get(adminModel, '1', 'view') >> { throw new SecurityException('Revoked') }
        }
        when:
        controller.show()
        then:
        response.status == 403
        view == '/admin/error'
        model.title == 'Access denied'
    }

    void 'login rejects a missing form token'() {
        given:
        request.method = 'POST'
        controller.adminRegistryService = Stub(AdminRegistryService) { isEnabled() >> true }
        controller.adminAccountService = Stub(AdminAccountService) { configured() >> true }

        when:
        controller.authenticate()

        then:
        response.status == 403
    }

    void 'unexpected admin error renders a branded page with its error ID'() {
        given:
        request.setAttribute(AdminErrorFilter.ERROR_ID_ATTRIBUTE, 'test-error-id')

        when:
        controller.error()
        String page = render(view: '/admin/error', model: model)

        then:
        response.status == 500
        page.contains('Grails Administration')
        page.contains('test-error-id')
    }

}
