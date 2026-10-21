package example
class AuditLog {
    String actor
    String action
    Date dateCreated
    static constraints = { actor blank: false; action blank: false }
    String toString() { "$actor: $action" }
}
