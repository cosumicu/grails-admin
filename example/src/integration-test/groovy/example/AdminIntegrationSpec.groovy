package example

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import grailsadmin.*
import spock.lang.Specification
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Integration
@Rollback
class AdminIntegrationSpec extends Specification {

    AdminRegistryService adminRegistryService
    AdminMetadataService adminMetadataService
    AdminCrudService adminCrudService
    AdminSecurityService adminSecurityService
    AdminAccountService adminAccountService

    def setup() {
        if (!adminAccountService.configured()) {
            adminAccountService.createInitialAccount('tester', 'example-password-123'.toCharArray(), 'example-password-123'.toCharArray())
        }
        def request = new MockHttpServletRequest()
        request.getSession(true).setAttribute(AdminAuthenticationService.SESSION_KEY,
            AdminAccount.findBySlot('primary').sessionVersion)
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request))
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
        RequestContextHolder.resetRequestAttributes()
    }

    void 'registered models have groups and safe metadata'() {
        expect:
        adminRegistryService.enabled
        adminRegistryService.resolve('banking', 'customer').domainClass == Customer
        adminRegistryService.resolve('banking', 'customer').formGroups.Identity == ['firstName', 'lastName', 'email']
        adminRegistryService.resolve('banking', 'customer').widgetTemplates.email == '/admin/widgets/emailHint'
        adminRegistryService.resolve('banking', 'customer').pageTemplates.show == '/admin/custom/customerShow'
        adminRegistryService.all().first().key == 'customer'
        adminRegistryService.resolve('authentication', 'user').domainClass == User
        adminMetadataService.inspect(Customer).byName.containsKey('email')
        adminMetadataService.inspect(Customer).byName.email.widget == 'email'
        adminMetadataService.inspect(Customer).byName.active.widget == 'checkbox'
        !adminMetadataService.inspect(Customer).byName.containsKey('passwordHash')
        !adminMetadataService.inspect(User).byName.containsKey('password')
        adminMetadataService.inspect(Customer).relationships.any { it.name == 'accounts' && it.toMany }
        adminMetadataService.inspect(Account).relationships.any { it.name == 'customer' && it.belongsTo }
        adminRegistryService.resolve('banking', 'missing') == null
    }

    void 'duplicate registration is rejected'() {
        when:
        adminRegistryService.register('customer', [domain: 'example.Customer'])
        then:
        thrown(IllegalArgumentException)
    }

    void 'invalid model display order is rejected'() {
        when:
        adminRegistryService.register('bad-order',
            [domain: 'example.Customer', group: 'Temporary', displayOrder: 'not-a-number'])
        then:
        thrown(IllegalArgumentException)
        adminRegistryService.resolve('temporary', 'bad-order') == null
    }

    void 'form groups cannot include fields outside the form allowlist'() {
        when:
        adminRegistryService.register('bad-group', [domain: 'example.Customer', group: 'Temporary',
            formFields: ['email'], formGroups: [Identity: ['firstName']]])
        then:
        thrown(IllegalArgumentException)
        adminRegistryService.resolve('temporary', 'bad-group') == null
    }

    void 'widget template paths are restricted to admin widgets'() {
        when:
        adminRegistryService.register('bad-widget', [domain: 'example.Customer', group: 'Temporary',
            widgetTemplates: [email: '../../other/template']])
        then:
        thrown(IllegalArgumentException)
        adminRegistryService.resolve('temporary', 'bad-widget') == null
    }

    void 'page template paths are restricted to admin custom views'() {
        when:
        adminRegistryService.register('bad-page', [domain: 'example.Customer', group: 'Temporary',
            pageTemplates: [show: '../../other/view']])
        then:
        thrown(IllegalArgumentException)
        adminRegistryService.resolve('temporary', 'bad-page') == null
    }

    void 'list search and pagination query registered domain'() {
        given:
        SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(
            'tester', 'unused', [new SimpleGrantedAuthority('ROLE_ADMIN')])
        def aster = new Customer(firstName: 'Aster', lastName: 'One', email: 'aster@example.com').save(failOnError: true, flush: true)
        new Customer(firstName: 'Beryl', lastName: 'Two', email: 'beryl@example.com').save(failOnError: true, flush: true)
        new Account(customer: aster, accountNumber: 'LIST-1', accountType: 'Savings', balance: 10G).save(failOnError: true, flush: true)
        def model = adminRegistryService.resolve('banking', 'customer')
        when:
        def result = adminCrudService.page(model, [q: 'Aster', sort: 'invalid', order: 'desc', page: '1', max: '1'])
        def accounts = adminCrudService.page(adminRegistryService.resolve('banking', 'account'), [sort: 'customer'])
        then:
        result.total == 1
        result.records*.firstName == ['Aster']
        result.sort != 'invalid'
        result.max == 1
        accounts.records*.customer*.firstName == ['Aster']
        accounts.sort == 'id'
    }

    void 'anonymous access is denied at service layer'() {
        given:
        RequestContextHolder.requestAttributes.request.getSession(false).removeAttribute(AdminAuthenticationService.SESSION_KEY)
        SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(
            'host-admin', 'unused', [new SimpleGrantedAuthority('ROLE_ADMIN')])
        def model = adminRegistryService.resolve('banking', 'customer')
        expect:
        !adminSecurityService.allowed(adminRegistryService.resolve('banking', 'customer'), 'change')
        when:
        adminCrudService.page(model, [:])
        then:
        thrown(SecurityException)
        when:
        adminCrudService.get(model, '1')
        then:
        thrown(SecurityException)
    }

    void 'filtering and validation use domain constraints'() {
        given:
        SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(
            'tester', 'unused', [new SimpleGrantedAuthority('ROLE_ADMIN')])
        def model = adminRegistryService.resolve('banking', 'customer')
        new Customer(firstName: 'Active', lastName: 'User', email: 'active-filter@example.com', active: true)
            .save(failOnError: true, flush: true)
        new Customer(firstName: 'Inactive', lastName: 'User', email: 'inactive-filter@example.com', active: false)
            .save(failOnError: true, flush: true)
        int before = Customer.count()
        when:
        def filtered = adminCrudService.page(model, [filter_active: 'false'])
        def unfiltered = adminCrudService.page(model, [filter_active: ''])
        def invalid = adminCrudService.save(model, new Customer(),
            [firstName: '', lastName: 'User', email: 'not-an-email'], true)
        then:
        filtered.records*.firstName == ['Inactive']
        unfiltered.total == before
        invalid.hasErrors()
        !invalid.id
        Customer.count() == before
    }

    void 'forged association ids are rejected'() {
        given:
        SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(
            'tester', 'unused', [new SimpleGrantedAuthority('ROLE_ADMIN')])
        def model = adminRegistryService.resolve('banking', 'account')
        when:
        adminCrudService.save(model, new Account(),
            [accountNumber: 'FORGED-1', accountType: 'Savings', customer: '99999999'], true)
        then:
        thrown(IllegalArgumentException)
        !Account.findByAccountNumber('FORGED-1')
    }

    void 'configured bulk action updates only selected scoped records'() {
        given:
        SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(
            'tester', 'unused', [new SimpleGrantedAuthority('ROLE_ADMIN')])
        def model = adminRegistryService.resolve('banking', 'customer')
        def selected = new Customer(firstName: 'Selected', lastName: 'User',
            email: 'selected-bulk@example.com', active: false).save(failOnError: true, flush: true)
        def other = new Customer(firstName: 'Other', lastName: 'User',
            email: 'other-bulk@example.com', active: false).save(failOnError: true, flush: true)
        expect:
        adminCrudService.availableBulkActions(model)*.key == ['activate']
        when:
        int changed = adminCrudService.bulk(model, 'activate', [selected.id.toString()])
        then:
        changed == 1
        selected.refresh().active
        !other.refresh().active
        AuditLog.findByAction("admin:customer:bulk:activate:${selected.id}")?.actor == 'tester'
        when:
        adminCrudService.bulk(model, 'missing', [other.id.toString()])
        then:
        thrown(IllegalArgumentException)
        !other.refresh().active
    }

    void 'registered domain on a second datasource supports generic CRUD'() {
        given:
        SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(
            'tester', 'unused', [new SimpleGrantedAuthority('ROLE_ADMIN')])
        def model = adminRegistryService.resolve('system', 'lookup-code')
        String code = 'LOOKUP-' + UUID.randomUUID().toString().take(8)
        when:
        def record = adminCrudService.save(model, new LookupCode(), [code: code, description: 'First'], true)
        def found = adminCrudService.get(model, record.id.toString())
        adminCrudService.save(model, record, [description: 'Updated'], false)
        def listed = adminCrudService.page(model, [q: code])
        then:
        record.id
        found.code == code
        adminCrudService.get(model, record.id.toString()).description == 'Updated'
        listed.records*.id == [record.id]
    }

    void 'query scope restricts list and record lookup'() {
        given:
        SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(
            'tester', 'unused', [new SimpleGrantedAuthority('ROLE_ADMIN')])
        def visible = new AuditLog(actor: 'demo', action: 'Allowed').save(failOnError: true, flush: true)
        def hidden = new AuditLog(actor: 'other', action: 'Hidden').save(failOnError: true, flush: true)
        def model = adminRegistryService.resolve('system', 'audit-log')
        when:
        def result = adminCrudService.page(model, [max: '10'])
        then:
        result.records*.id == [visible.id]
        adminCrudService.get(model, hidden.id.toString()) == null
    }

    void 'admin role can create while mass assignment and readonly fields stay blocked'() {
        given:
        SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(
            'tester', 'unused', [new SimpleGrantedAuthority('ROLE_ADMIN')])
        def customerModel = adminRegistryService.resolve('banking', 'customer')
        def accountModel = adminRegistryService.resolve('banking', 'account')
        when:
        def customer = adminCrudService.save(customerModel, new Customer(),
            [firstName: 'Casey', lastName: 'Test', email: 'casey@example.com', active: 'true', passwordHash: 'attack'], true)
        def account = new Account(customer: customer, accountNumber: 'TEST-123', accountType: 'Savings', balance: 100G).save(failOnError: true, flush: true)
        adminCrudService.save(accountModel, account, [balance: '9999', accountType: 'Checking'], false)
        adminCrudService.save(accountModel, account, [accountNumber: 'CHANGED'], false)
        then:
        adminSecurityService.allowed(customerModel, 'add')
        customer.id
        AuditLog.findByAction("admin:customer:add:${customer.id}")?.actor == 'tester'
        customer.passwordHash == null
        account.balance == 100G
        account.accountType == 'Checking'
        account.accountNumber == 'TEST-123'
        account.hasErrors()
        !adminSecurityService.allowed(accountModel, 'delete')
    }

}
