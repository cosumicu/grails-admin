package example
class User implements Serializable {
    String username
    String password
    boolean enabled = true
    boolean accountExpired
    boolean accountLocked
    boolean passwordExpired
    static constraints = { username blank: false, unique: true; password blank: false }
    static mapping = { table 'admin_demo_user' }
    Set<Role> getAuthorities() { UserRole.findAllByUser(this)*.role as Set<Role> }
    String toString() { username }
}
