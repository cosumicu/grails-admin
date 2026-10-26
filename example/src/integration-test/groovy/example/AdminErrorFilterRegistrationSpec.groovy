package example

import grails.testing.mixin.integration.Integration
import grailsadmin.AdminErrorFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.ApplicationContext
import spock.lang.Specification

@Integration
class AdminErrorFilterRegistrationSpec extends Specification {

    @Autowired ApplicationContext applicationContext

    void 'plugin registers the admin error boundary in a host application'() {
        when:
        FilterRegistrationBean registration = applicationContext.getBean('grailsAdminErrorFilterRegistration', FilterRegistrationBean)

        then:
        registration.filter instanceof AdminErrorFilter
        registration.urlPatterns.contains('/admin')
        registration.urlPatterns.contains('/admin/*')
        applicationContext.getBean('grailsUrlMappingsHolder').match('/admin/error').actionName == 'error'
    }

}
