package example

class Customer {

    static hasMany = [accounts: Account]
    String firstName
    String lastName
    String email
    Boolean active = true
    String passwordHash
    static constraints = {
        firstName blank: false
        lastName blank: false
        email blank: false, email: true, unique: true
        passwordHash nullable: true
    }
    String toString() { "$firstName $lastName" }

}
