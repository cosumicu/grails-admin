package example
class UserRole implements Serializable {
    User user
    Role role
    static constraints = { role unique: 'user' }
    static mapping = { id composite: ['user', 'role']; version false }
}
