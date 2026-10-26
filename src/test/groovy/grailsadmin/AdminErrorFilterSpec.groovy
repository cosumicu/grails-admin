package grailsadmin

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import spock.lang.Specification

import javax.servlet.FilterChain
import javax.servlet.ServletException

class AdminErrorFilterSpec extends Specification {

    void '#kind failure gets one admin error ID and forwards to the admin error page'() {
        given:
        def request = new MockHttpServletRequest('GET', '/admin/monitoring/transaction/2/edit')
        def response = new MockHttpServletResponse()
        def output = new ByteArrayOutputStream()
        def previous = System.err
        FilterChain chain = Stub(FilterChain) {
            doFilter(_, _) >> { throw failure }
        }

        when:
        try {
            System.setErr(new PrintStream(output, true, 'UTF-8'))
            new AdminErrorFilter().doFilter(request, response, chain)
        } finally {
            System.setErr(previous)
        }

        then:
        request.getAttribute(AdminErrorFilter.ERROR_ID_ATTRIBUTE)
        response.forwardedUrl == '/admin/error'
        output.toString('UTF-8').count('[GRAILS-ADMIN ERROR]') == 1
        output.toString('UTF-8').contains(failure.message)

        where:
        kind         | failure
        'controller' | new IllegalStateException('Controller failed')
        'rendering'  | new ServletException('Rendering failed')
    }

}
