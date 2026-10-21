package grailsadmin

/** Optional trusted host bean called within the admin write transaction. */
interface AdminAuditSink {
    void record(AdminModel model, String operation, List<Long> recordIds, String actor)
}
