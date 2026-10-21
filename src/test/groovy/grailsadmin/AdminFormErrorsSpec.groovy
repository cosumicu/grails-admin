package grailsadmin

import grails.testing.gorm.DomainUnitTest
import org.springframework.context.support.StaticMessageSource
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import spock.lang.Specification

class AdminFormErrorsSpec extends Specification implements DomainUnitTest<AdminAccount> {
    void 'an overlong value identifies its field and limit without logging the value'() {
        given:
        String submitted = 'x' * 101
        def account = new AdminAccount(slot: 'primary', username: submitted,
            passwordHash: 'hash', sessionVersion: UUID.randomUUID().toString())

        when:
        boolean valid = account.validate()
        Map errors = AdminFormErrors.collect(account, new StaticMessageSource(), Locale.ENGLISH,
            [username: [label: 'Username', maxSize: 100]])

        then:
        !valid
        errors.fields.username == 'Username must be at most 100 characters.'
        !AdminFormErrors.logSummary(account).contains(submitted)
        AdminFormErrors.logSummary(account).contains('username')
    }

    void 'invalid date conversion identifies the field without echoing the submitted value'() {
        when:
        AdminCrudService.convert([name: 'receivedAt', type: Date], 'not-a-date')

        then:
        AdminFieldConversionException error = thrown()
        error.fieldName == 'receivedAt'
        AdminFormErrors.conversionMessage(error, [label: 'Received At']) ==
            'Enter a valid date for Received At (YYYY-MM-DD).'
        !error.message.contains('not-a-date')
    }

    void 'message-source validation text resolves argument placeholders'() {
        given:
        def record = new Expando()
        def errors = new BeanPropertyBindingResult(record, 'record')
        errors.addError(new FieldError('record', 'amount', 'private-value', false,
            ['record.amount.invalid'] as String[], ['Amount'] as Object[], 'Property [{0}] is invalid'))
        record.errors = errors
        def messages = new StaticMessageSource()
        messages.addMessage('record.amount.invalid', Locale.ENGLISH, '{0} is invalid.')

        expect:
        AdminFormErrors.collect(record, messages, Locale.ENGLISH).fields.amount == 'Amount is invalid.'
        !AdminFormErrors.logSummary(record).contains('private-value')
    }
}
