package example
class Role implements Serializable {
    String authority
    static constraints = { authority blank: false, unique: true }
    String toString() { authority }
}
