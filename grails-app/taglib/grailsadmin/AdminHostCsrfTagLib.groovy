package grailsadmin

import grails.core.GrailsApplication

/** Adds a host application's CSRF field without replacing Grails Admin's form token. */
class AdminHostCsrfTagLib {
    static namespace = 'adminHost'

    GrailsApplication grailsApplication

    def csrfFields = { attrs ->
        def springToken = request.getAttribute('_csrf')
        if (springToken?.parameterName && springToken?.token) {
            out << g.hiddenField(name: springToken.parameterName.toString(), value: springToken.token.toString())
        }

        String parameterName = grailsApplication.config.getProperty('grailsAdmin.hostCsrf.parameterName', String, '')
        String sessionAttribute = grailsApplication.config.getProperty('grailsAdmin.hostCsrf.sessionAttribute', String, '')
        if (parameterName && sessionAttribute && parameterName != springToken?.parameterName) {
            def token = request.getSession(false)?.getAttribute(sessionAttribute)
            if (token != null) out << g.hiddenField(name: parameterName, value: token.toString())
        }
    }
}
