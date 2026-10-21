package example

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import grailsadmin.AdminAccount
import grailsadmin.AdminAccountService
import grailsadmin.AdminAuthenticationService
import grailsadmin.AdminSecurityService
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import spock.lang.Specification

@Integration
@Rollback
class AdminAuthenticationSpec extends Specification {
    AdminAccountService adminAccountService
    AdminAuthenticationService adminAuthenticationService
    AdminSecurityService adminSecurityService

    def cleanup() {
        RequestContextHolder.resetRequestAttributes()
        SecurityContextHolder.clearContext()
    }

    void 'first setup hashes the password and rejects a second account'() {
        expect:
        !adminAccountService.configured()

        when:
        adminAccountService.createInitialAccount('owner', 'long-test-password'.toCharArray(), 'long-test-password'.toCharArray())

        then:
        adminAccountService.configured()
        AdminAccount.count() == 1
        AdminAccount.findBySlot('primary').passwordHash != 'long-test-password'
        adminAccountService.verify('owner', 'long-test-password')
        !adminAccountService.verify('owner', 'wrong-password')

        when:
        adminAccountService.createInitialAccount('second', 'another-long-password'.toCharArray(), 'another-long-password'.toCharArray())

        then:
        thrown(IllegalStateException)
        AdminAccount.count() == 1
    }

    void 'setup rejects missing confirmation'() {
        when:
        adminAccountService.createInitialAccount('owner', 'long-test-password'.toCharArray(), 'different-password'.toCharArray())

        then:
        thrown(IllegalArgumentException)
        !adminAccountService.configured()
    }

    void 'short password needs explicit confirmation and then works'() {
        when:
        adminAccountService.createInitialAccount('owner', 'short'.toCharArray(), 'short'.toCharArray())

        then:
        thrown(IllegalArgumentException)
        !adminAccountService.configured()

        when:
        adminAccountService.createInitialAccount('owner', 'short'.toCharArray(), 'short'.toCharArray(), true)

        then:
        adminAccountService.verify('owner', 'short')
        !adminAccountService.verify('owner', 'other')
    }

    void 'long password hashes and verifies every byte after explicit confirmation'() {
        given:
        String password = 'a' * 72 + 'end'

        when:
        adminAccountService.createInitialAccount('owner', password.toCharArray(), password.toCharArray(), true)

        then:
        AdminAccount.findBySlot('primary').passwordHash.startsWith('{sha256-bcrypt}')
        adminAccountService.verify('owner', password)
        !adminAccountService.verify('owner', 'a' * 72 + 'bad')
    }

    void 'legacy bcrypt hash does not accept an overlong password sharing its first 72 bytes'() {
        given:
        String password = 'a' * 72
        new AdminAccount(slot: 'primary', username: 'owner',
            passwordHash: new BCryptPasswordEncoder(10).encode(password),
            sessionVersion: UUID.randomUUID().toString()).save(failOnError: true, flush: true)

        expect:
        adminAccountService.verify('owner', password)
        !adminAccountService.verify('owner', password + 'extra')
    }

    void 'successful login upgrades an older bcrypt work factor'() {
        given:
        new AdminAccount(slot: 'primary', username: 'owner',
            passwordHash: new BCryptPasswordEncoder(10).encode('long-test-password'),
            sessionVersion: UUID.randomUUID().toString()).save(failOnError: true, flush: true)

        when:
        def account = adminAccountService.verify('owner', 'long-test-password')

        then:
        account
        AdminAccount.findBySlot('primary').passwordHash.startsWith('$2a$12$')
    }

    void 'host login alone cannot access admin and admin logout preserves host login'() {
        given:
        adminAccountService.createInitialAccount('owner', 'long-test-password'.toCharArray(), 'long-test-password'.toCharArray())
        def request = new MockHttpServletRequest()
        String sessionBeforeLogin = request.getSession(true).id
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request))
        def hostAuthentication = new UsernamePasswordAuthenticationToken(
            'host-user', 'unused', [new SimpleGrantedAuthority('ROLE_ADMIN')])
        SecurityContextHolder.context.authentication = hostAuthentication

        expect:
        !adminSecurityService.authenticated()
        !adminSecurityService.allowed(null, 'view')

        when:
        boolean signedIn = adminAuthenticationService.login('owner', 'long-test-password')

        then:
        signedIn
        request.getSession(false).id != sessionBeforeLogin
        adminSecurityService.authenticated()
        adminSecurityService.username() == 'owner'
        adminSecurityService.allowed(null, 'view')

        when:
        adminAuthenticationService.logout()

        then:
        !adminSecurityService.authenticated()
        SecurityContextHolder.context.authentication.is(hostAuthentication)
    }

    void 'plugin login works without a host login'() {
        given:
        adminAccountService.createInitialAccount('owner', 'long-test-password'.toCharArray(), 'long-test-password'.toCharArray())
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()))

        expect:
        !SecurityContextHolder.context.authentication
        !adminAuthenticationService.login('owner', 'wrong-password')
        !adminSecurityService.authenticated()
        adminAuthenticationService.login('owner', 'long-test-password')
        adminSecurityService.allowed(null, 'view')
    }
}
