package grailsadmin

import spock.lang.Specification

class AdminLoginThrottleSpec extends Specification {
    void 'repeated failures are limited per address and success clears them'() {
        given:
        def throttle = new AdminLoginThrottleService()

        when:
        5.times { throttle.failed('127.0.0.1') }

        then:
        !throttle.permitted('127.0.0.1')
        throttle.permitted('127.0.0.2')

        when:
        throttle.succeeded('127.0.0.1')

        then:
        throttle.permitted('127.0.0.1')
    }
}
