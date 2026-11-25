package grailsadmin

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import spock.lang.Specification

class AdminAuthenticationServiceSpec extends Specification {
    def accounts = Mock(AdminAccountService)
    def service = new AdminAuthenticationService(adminAccountService: accounts)

    private MockHttpServletRequest bind(String version) {
        def request = new MockHttpServletRequest()
        request.getSession(true).setAttribute(AdminAuthenticationService.SESSION_KEY, version)
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request))
        request
    }

    void cleanup() { RequestContextHolder.resetRequestAttributes() }

    void 'reuse account within request but recheck a revoked session on next request'() {
        given:
        bind('v1')

        when:
        assert service.authenticated()
        assert service.username() == 'owner'
        assert service.authenticated()

        then:
        1 * accounts.currentAccount('v1') >> new AdminAccount(username: 'owner')

        when:
        bind('v1')
        assert !service.authenticated()
        assert !service.authenticated()
        assert service.username() == ''

        then:
        1 * accounts.currentAccount('v1') >> null
    }

    void 'logout and login invalidate the request account'() {
        given:
        bind('v1')

        when:
        assert service.authenticated()
        service.logout()
        assert !service.authenticated()
        assert service.login('new-owner', 'password')
        assert service.username() == 'new-owner'

        then:
        1 * accounts.currentAccount('v1') >> new AdminAccount(username: 'owner')
        1 * accounts.verify('new-owner', 'password') >> new AdminAccount(username: 'new-owner', sessionVersion: 'v2')
        1 * accounts.currentAccount('v2') >> new AdminAccount(username: 'new-owner')
    }
}
