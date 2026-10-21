package grailsadmin

/** A trusted host bean for an explicitly configured batch operation. */
interface AdminBulkAction {
    String getLabel()
    /** Required model permission: change or delete. */
    String getPermission()
    /** Fields this action changes; empty for a delete action. */
    List<String> getFields()
    /** Runs in one transaction after every selected record has passed scope checks. */
    void execute(AdminModel model, List<Object> records)
}
