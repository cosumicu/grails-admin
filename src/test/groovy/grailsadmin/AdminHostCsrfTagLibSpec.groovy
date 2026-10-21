package grailsadmin

import grails.testing.web.taglib.TagLibUnitTest
import spock.lang.Specification

class AdminHostCsrfTagLibSpec extends Specification implements TagLibUnitTest<AdminHostCsrfTagLib> {
    void 'configured session token is included in plugin forms'() {
        given:
        grailsApplication.config.grailsAdmin.hostCsrf.parameterName = 'csrfToken'
        grailsApplication.config.grailsAdmin.hostCsrf.sessionAttribute = 'csrfToken'
        session.csrfToken = 'host-token'

        expect:
        applyTemplate('<adminHost:csrfFields/>').contains('name="csrfToken" value="host-token"')
    }

    void 'Spring Security token is included when available'() {
        given:
        request.setAttribute('_csrf', [parameterName: '_csrf', token: 'spring-token'])

        expect:
        applyTemplate('<adminHost:csrfFields/>').contains('name="_csrf" value="spring-token"')
    }

    void 'no host token field is emitted without configuration'() {
        expect:
        applyTemplate('<adminHost:csrfFields/>') == ''
    }
}
