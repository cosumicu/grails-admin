package grailsadmin

import org.springframework.web.context.request.RequestContextHolder

class AdminAuthenticationService {

    static final String SESSION_KEY = 'grailsAdminAuthenticationVersion'
    private static final String ACCOUNT_KEY = 'grailsAdminRequestAccount'

    AdminAccountService adminAccountService

    private static request() { RequestContextHolder.getRequestAttributes()?.request }

    boolean authenticated() {
        currentAccount() != null
    }

    String username() {
        currentAccount()?.username ?: ''
    }

    private AdminAccount currentAccount() {
        def httpRequest = request()
        String version = httpRequest?.getSession(false)?.getAttribute(SESSION_KEY)?.toString()
        if (!version) return null
        Map cached = httpRequest.getAttribute(ACCOUNT_KEY) as Map
        if (cached == null || cached.version != version) {
            cached = [version: version, account: adminAccountService.currentAccount(version)]
            httpRequest.setAttribute(ACCOUNT_KEY, cached)
        }
        cached.account as AdminAccount
    }

    boolean login(String username, String password) {
        AdminAccount account = adminAccountService.verify(username, password)
        if (!account) return false
        def httpRequest = request()
        if (!httpRequest) return false
        httpRequest.getSession(true)
        httpRequest.changeSessionId()
        httpRequest.getSession(true).setAttribute(SESSION_KEY, account.sessionVersion)
        httpRequest.removeAttribute(ACCOUNT_KEY)
        true
    }

    void logout() {
        request()?.getSession(false)?.removeAttribute(SESSION_KEY)
        request()?.removeAttribute(ACCOUNT_KEY)
    }

}
