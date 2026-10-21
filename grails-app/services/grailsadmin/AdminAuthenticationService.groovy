package grailsadmin

import org.springframework.web.context.request.RequestContextHolder

class AdminAuthenticationService {
    static final String SESSION_KEY = 'grailsAdminAuthenticationVersion'

    AdminAccountService adminAccountService

    private static request() { RequestContextHolder.getRequestAttributes()?.request }

    boolean authenticated() {
        def session = request()?.getSession(false)
        session && adminAccountService.currentAccount(session.getAttribute(SESSION_KEY)?.toString()) != null
    }

    String username() {
        def session = request()?.getSession(false)
        adminAccountService.currentAccount(session?.getAttribute(SESSION_KEY)?.toString())?.username ?: ''
    }

    boolean login(String username, String password) {
        AdminAccount account = adminAccountService.verify(username, password)
        if (!account) return false
        def httpRequest = request()
        if (!httpRequest) return false
        httpRequest.getSession(true)
        httpRequest.changeSessionId()
        httpRequest.getSession(true).setAttribute(SESSION_KEY, account.sessionVersion)
        true
    }

    void logout() {
        request()?.getSession(false)?.removeAttribute(SESSION_KEY)
    }
}
