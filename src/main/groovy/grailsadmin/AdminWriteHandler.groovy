package grailsadmin

/** Trusted host bean for models whose writes must pass through a business service. */
interface AdminWriteHandler {

    /** Validate and persist the record using only the supplied allowlisted changes. */
    Object save(AdminModel model, Object record, Map<String, Object> changes, boolean creating)
    void delete(AdminModel model, Object record)

}
