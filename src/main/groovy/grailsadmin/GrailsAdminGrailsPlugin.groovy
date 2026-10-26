package grailsadmin

import grails.plugins.Plugin
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.core.Ordered

class GrailsAdminGrailsPlugin extends Plugin {

    def grailsVersion = '6.2.3 > *'
    def title = 'Grails Admin'
    def description = 'Registered-domain administration for Grails applications'
    def profiles = ['web']
    def license = 'APACHE'
    def pluginExcludes = ['grails-app/views/index.gsp', 'grails-app/views/error.gsp', 'grails-app/views/notFound.gsp']

    Closure doWithSpring() {
        { ->
            grailsAdminErrorFilterRegistration(FilterRegistrationBean) {
                filter = new AdminErrorFilter()
                urlPatterns = ['/admin', '/admin/*']
                order = Ordered.HIGHEST_PRECEDENCE + 50
            }
        }
    }

    void doWithApplicationContext() {
        applicationContext.getBean('adminRegistryService').initialize()
    }

}
