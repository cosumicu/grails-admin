package grailsadmin

/** Trusted host bean returning GORM criteria restrictions for one operation. */
interface AdminQueryScope {
    Closure criteria(AdminModel model, String operation)
}
