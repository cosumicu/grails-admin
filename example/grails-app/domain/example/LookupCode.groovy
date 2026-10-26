package example

/** Synthetic record stored on a second host-managed GORM datasource. */
class LookupCode {

    String code
    String description

    static mapping = { datasource 'lookup' }
    static constraints = {
        code blank: false, unique: true
        description blank: false
    }

    String toString() { code }

}
