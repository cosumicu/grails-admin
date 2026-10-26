package example

import grailsadmin.AdminAuditSink
import grailsadmin.AdminModel

/** Example-only audit sink using the host app's AuditLog domain. */
class DemoAdminAuditSinkService implements AdminAuditSink {

    void record(AdminModel model, String operation, List<Long> recordIds, String actor) {
        AuditLog.withTransaction {
            new AuditLog(actor: actor ?: 'unknown',
                action: "admin:${model.key}:${operation}:${recordIds.join(',')}").save(failOnError: true, flush: true)
        }
    }

}
