package example

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import grailsadmin.*
import grails.core.GrailsApplication
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
    AdminCrudService adminCrudService
    SessionFactory sessionFactory
    GrailsApplication grailsApplication

    void 'measure list counts and relationship rendering'() {
        given:
        if (!adminAccountService.configured()) {
            adminAccountService.createInitialAccount('perf', 'performance-password'.toCharArray(),
                'performance-password'.toCharArray())
        }
        def request = new MockHttpServletRequest()
        request.getSession(true).setAttribute(AdminAuthenticationService.SESSION_KEY,
            AdminAccount.findBySlot('primary').sessionVersion)
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request))
        30.times { n ->
            def customer = new Customer(firstName: "Perf${n}", lastName: 'Test', email: "perf${n}@example.com")
                .save(failOnError: true)
            new Account(customer: customer, accountNumber: "PERF-${n}", accountType: 'Savings')
                .save(failOnError: true)
        }
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()
        assert adminSecurityService.authenticated()
        def statistics = sessionFactory.statistics
        boolean previouslyEnabled = statistics.statisticsEnabled
        statistics.statisticsEnabled = true
        statistics.clear()
        long started = System.nanoTime()

        when:
        def page = adminCrudService.page(adminRegistryService.resolve('banking', 'account'), [max: '25'])
        long pageStatements = statistics.prepareStatementCount
        page.records.each { assert it.customer.toString() }
        long renderedStatements = statistics.prepareStatementCount

        then:
        page.records.size() == 25
        page.total == 30
        renderedStatements == pageStatements
        println "ADMIN_PERF listRows=25 pageStatements=${pageStatements} relationshipStatements=${renderedStatements - pageStatements} elapsedMs=${(System.nanoTime() - started) / 1_000_000d}"

        when:
        grailsApplication.config.grailsAdmin.countTotal = false
        sessionFactory.currentSession.clear()
        statistics.clear()
        def first = adminCrudService.page(adminRegistryService.resolve('banking', 'account'), [max: '25'])
        long withoutCount = statistics.prepareStatementCount
        def last = adminCrudService.page(adminRegistryService.resolve('banking', 'account'), [max: '25', page: '2'])
        def empty = adminCrudService.page(adminRegistryService.resolve('banking', 'account'), [max: '25', page: '3'])
        def filtered = adminCrudService.page(adminRegistryService.resolve('banking', 'customer'), [q: 'Perf29', max: '1'])

        then:
        withoutCount == 1
        first.total == null
        first.records*.id == page.records*.id
        first.hasNext
        last.records.size() == 5
        !last.hasNext
        empty.records.empty
        !empty.hasNext
        filtered.records*.firstName == ['Perf29']
        !filtered.hasNext
        println "ADMIN_PERF countDisabledStatements=${withoutCount}"

        cleanup:
        statistics.statisticsEnabled = previouslyEnabled
        grailsApplication.config.grailsAdmin.countTotal = true
        RequestContextHolder.resetRequestAttributes()
    }

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
        statements == 1
        println "ADMIN_PERF permissionChecks=100 statements=${statements} elapsedMs=${(System.nanoTime() - started) / 1_000_000d}"

        cleanup:
        statistics.statisticsEnabled = previouslyEnabled
        RequestContextHolder.resetRequestAttributes()
    }
}
