package grailsadmin

/** The only credential used by Grails Admin. This domain is never registered for CRUD. */
class AdminAccount {

    String slot = 'primary'
    String username
    String passwordHash
    String sessionVersion

    static constraints = {
        slot blank: false, unique: true
        username blank: false, maxSize: 100
        passwordHash blank: false, maxSize: 255
        sessionVersion blank: false, maxSize: 36
    }

    static mapping = {
        table 'grails_admin_account'
        passwordHash column: 'password_hash'
        sessionVersion column: 'session_version'
    }

}
