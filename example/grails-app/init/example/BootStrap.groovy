package example

class BootStrap {
    def springSecurityService
    def init = { servletContext ->
        if (grails.util.Environment.current != grails.util.Environment.DEVELOPMENT) return
        Customer.withTransaction {
            if (!Customer.count()) {
                def customer = new Customer(firstName: 'Maya', lastName: 'Lee', email: 'maya@example.com', active: true).save(failOnError: true)
                def account = new Account(customer: customer, accountNumber: 'DEMO-1001', accountType: 'Savings', balance: 1250G).save(failOnError: true)
                new Transaction(account: account, referenceNumber: 'TX-DEMO-1', amount: 25G, transactionDate: new Date()).save(failOnError: true)
                new AuditLog(actor: 'demo', action: 'Synthetic data created').save(failOnError: true)
            }
            String password = System.getenv('DEMO_ADMIN_PASSWORD')
            if (password && !User.findByUsername('demo-admin')) {
                def role = Role.findByAuthority('ROLE_ADMIN') ?: new Role(authority: 'ROLE_ADMIN').save(failOnError: true, flush: true)
                def user = new User(username: 'demo-admin', password: springSecurityService.encodePassword(password)).save(failOnError: true, flush: true)
                new UserRole(user: user, role: role).save(failOnError: true, flush: true)
            }
        }
        LookupCode.withTransaction {
            if (!LookupCode.count())
                new LookupCode(code: 'DEMO-LOOKUP', description: 'Synthetic secondary-datasource value')
                    .save(failOnError: true, flush: true)
        }
    }
    def destroy = {
    }
}
