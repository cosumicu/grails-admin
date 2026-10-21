package grailsadmin

import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

class AdminControllerSpec extends Specification implements ControllerUnitTest<AdminController> {
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
