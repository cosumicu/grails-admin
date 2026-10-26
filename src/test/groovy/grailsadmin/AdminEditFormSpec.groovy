package grailsadmin

import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import grails.gorm.annotation.Entity
import org.springframework.context.support.StaticMessageSource
import spock.lang.Specification

class AdminEditFormSpec extends Specification implements ControllerUnitTest<AdminController>, DomainUnitTest<AdminEditRecord> {

    void 'edit renders the saved record and its date field'() {
        given:
        Date date = AdminCrudService.convert([name: 'receivedAt', type: Date], '2026-10-19')
        AdminEditRecord saved = new AdminEditRecord(transactionId: 'TX-2', receivedAt: date).save(flush: true)
        def adminModel = new AdminModel('transaction', 'Monitoring', 'transaction', 'Transactions', AdminEditRecord,
            ['transactionId'], [], [], [], [], ['transactionId', 'receivedAt'], true, true, false,
            null, null, [:], 100, [Main: ['transactionId', 'receivedAt']], [:], [:])
        List<Map> fields = [
            [name: 'transactionId', type: String, widget: 'text', label: 'Transaction ID', required: true],
            [name: 'receivedAt', type: Date, widget: 'date', label: 'Received At', required: false]
        ]
        params.group = 'monitoring'
        params.model = 'transaction'
        params.id = saved.id.toString()
        controller.messageSource = new StaticMessageSource()
        controller.adminRegistryService = Stub(AdminRegistryService) {
            isEnabled() >> true
            resolve('monitoring', 'transaction') >> adminModel
            all() >> [adminModel]
        }
        controller.adminSecurityService = Stub(AdminSecurityService) {
            authenticated() >> true
            allowed(adminModel, 'change') >> true
            allowed(adminModel, 'view') >> true
            fieldWritable(adminModel, _, 'change') >> true
            readable(adminModel, _) >> true
            username() >> 'admin'
        }
        controller.adminCrudService = Stub(AdminCrudService) {
            get(adminModel, saved.id.toString(), 'change') >> saved
        }
        controller.adminMetadataService = Stub(AdminMetadataService) {
            inspect(AdminEditRecord) >> [fields: fields, byName: fields.collectEntries { [(it.name): it] }]
        }

        when:
        controller.edit()
        String page = render(view: '/admin/form', model: model)

        then:
        response.status == 200
        page.contains('Edit Transactions')
        page.contains('value="2026-10-19"')
        page.contains('value="TX-2"')
    }

    void 'conversion error stays beside the field and keeps submitted values'() {
        given:
        def adminModel = new AdminModel('transaction', 'Monitoring', 'transaction', 'Transactions', AdminEditRecord,
            ['transactionId'], [], [], [], [], ['transactionId', 'receivedAt'], true, true, false,
            null, null, [:], 100, [Main: ['transactionId', 'receivedAt']], [:], [:])
        List<Map> fields = [
            [name: 'transactionId', type: String, widget: 'text', label: 'Transaction ID', required: true],
            [name: 'receivedAt', type: Date, widget: 'date', label: 'Received At', required: false]
        ]
        params.group = 'monitoring'
        params.model = 'transaction'
        controller.adminSecurityService = Stub(AdminSecurityService) {
            fieldWritable(adminModel, _, 'add') >> true
            readable(adminModel, _) >> true
        }

        when:
        String page = render(view: '/admin/form', model: [adminModel: adminModel,
            record: new AdminEditRecord(), creating: true, options: [:],
            metadata: [fields: fields, byName: fields.collectEntries { [(it.name): it] }],
            adminSecurityService: controller.adminSecurityService,
            submittedValues: [transactionId: 'MY-INPUT', receivedAt: 'not-a-date'],
            fieldErrors: [receivedAt: 'Enter a valid date for Received At (YYYY-MM-DD).'],
            generalErrors: [], saveError: 'Correct the highlighted field and try again.'])

        then:
        page.contains('value="MY-INPUT"')
        page.contains('value="not-a-date"')
        page.contains('type="text" value="not-a-date"')
        page.contains('Enter a valid date for Received At')
    }

}

@Entity
class AdminEditRecord {

    String transactionId
    Date receivedAt

    static constraints = {
        transactionId maxSize: 80
        receivedAt nullable: true
    }

}
