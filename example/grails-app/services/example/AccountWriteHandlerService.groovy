package example

import grailsadmin.AdminModel
import grailsadmin.AdminWriteHandler

class AccountWriteHandlerService implements AdminWriteHandler {

    Object save(AdminModel model, Object record, Map<String, Object> changes, boolean creating) {
        if (!creating && changes.containsKey('accountNumber') && changes.accountNumber != record.accountNumber) {
            record.errors.rejectValue('accountNumber', 'immutable', 'Account number cannot be changed')
            return record
        }
        changes.each { field, value -> record."$field" = value }
        if (record.validate()) record.save(flush: true)
        record
    }

    void delete(AdminModel model, Object record) { record.delete(flush: true) }

}
