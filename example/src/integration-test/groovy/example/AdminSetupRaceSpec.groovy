package example

import grails.testing.mixin.integration.Integration
import grailsadmin.AdminAccount
import grailsadmin.AdminAccountService
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Integration
class AdminSetupRaceSpec extends Specification {

    AdminAccountService adminAccountService

    def setup() {
        AdminAccount.withTransaction { AdminAccount.executeUpdate('delete from AdminAccount') }
    }

    def cleanup() {
        AdminAccount.withTransaction { AdminAccount.executeUpdate('delete from AdminAccount') }
    }

    void 'concurrent first setup attempts create only one account'() {
        given:
        assert !adminAccountService.configured()
        def start = new CountDownLatch(1)
        def workers = Executors.newFixedThreadPool(2)

        when:
        def results = (1..2).collect { number ->
            workers.submit({
                start.await()
                try {
                    adminAccountService.createInitialAccount("owner${number}",
                        'long-test-password'.toCharArray(), 'long-test-password'.toCharArray())
                    true
                } catch (Exception error) {
                    error
                }
            } as java.util.concurrent.Callable<Object>)
        }
        start.countDown()
        def outcomes = results.collect { it.get(20, TimeUnit.SECONDS) }

        then:
        outcomes.count(true) == 1
        outcomes.count { it instanceof Exception } == 1
        AdminAccount.withTransaction([readOnly: true]) { AdminAccount.count() } == 1

        cleanup:
        workers.shutdownNow()
    }

}
