package example

import grailsadmin.AdminBulkAction
import grailsadmin.AdminModel

/** Demonstrates an explicit, permission-checked bulk action on synthetic data. */
class CustomerActivateActionService implements AdminBulkAction {

    String getLabel() { 'Activate selected customers' }
    String getPermission() { 'change' }
    List<String> getFields() { ['active'] }

    void execute(AdminModel model, List<Object> records) {
        records.each { customer ->
            customer.active = true
            customer.save(failOnError: true, flush: true)
        }
    }

}
