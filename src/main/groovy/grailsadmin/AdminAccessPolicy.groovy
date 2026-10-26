package grailsadmin

/** Host applications may register a bean named adminAccessPolicy. All methods must be enforced server side. */
interface AdminAccessPolicy {

    boolean authenticated()
    String username()
    boolean allowed(AdminModel model, String operation)
    boolean readable(AdminModel model, String field)
    boolean writable(AdminModel model, String field)

}
