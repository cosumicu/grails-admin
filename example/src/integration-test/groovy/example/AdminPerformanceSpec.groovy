package example

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import grailsadmin.*
import org.hibernate.SessionFactory
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import spock.lang.Specification

@Integration
@Rollback
class AdminPerformanceSpec extends Specification {
    AdminAccountService adminAccountService
    AdminSecurityService adminSecurityService
    AdminRegistryService adminRegistryService
    SessionFactory sessionFactory

    void 'measure repeated permission checks in one rendered table request'() {
        given:
        if (!adminAccountService.configured()) {
            adminAccountService.createInitialAccount('perf', 'performance-password'.toCharArray(),
                'performance-password'.toCharArray())
        }
        def request = new MockHttpServletRequest()
        request.getSession(true).setAttribute(AdminAuthenticationService.SESSION_KEY,
            AdminAccount.findBySlot('primary').sessionVersion)
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request))
        def model = adminRegistryService.resolve('banking', 'customer')
        def statistics = sessionFactory.statistics
        boolean previouslyEnabled = statistics.statisticsEnabled
        statistics.statisticsEnabled = true
        statistics.clear()
        long started = System.nanoTime()

        when:
        // A 25-row table with four readable columns.
        100.times { assert adminSecurityService.readable(model, 'firstName') }
        long statements = statistics.prepareStatementCount

        then:
        statements > 0
        println "ADMIN_PERF permissionChecks=100 statements=${statements} elapsedMs=${(System.nanoTime() - started) / 1_000_000d}"

        cleanup:
        statistics.statisticsEnabled = previouslyEnabled
        RequestContextHolder.resetRequestAttributes()
    }
}
