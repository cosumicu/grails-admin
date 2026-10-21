package grailsadmin

import spock.lang.Specification
import java.time.LocalDate
import java.time.ZoneId

class AdminSafetySpec extends Specification {
    void 'route slugs contain only stable path characters'() {
        expect:
        AdminRegistryService.slug('Banking & Payments') == 'banking-payments'
    }

    void 'empty slug is rejected'() {
        when:
        AdminRegistryService.slug('')
        then:
        thrown(IllegalArgumentException)
    }

    void 'boolean values reject arbitrary input'() {
        when:
        AdminCrudService.convert([name: 'active', type: Boolean], 'unexpected')
        then:
        thrown(IllegalArgumentException)
    }

    void 'numeric values reject expressions'() {
        when:
        AdminCrudService.convert([name: 'balance', type: BigDecimal], '1; drop table')
        then:
        thrown(IllegalArgumentException)
    }

    void 'date form values convert to a Date at the start of the selected day'() {
        when:
        Date result = AdminCrudService.convert([name: 'receivedAt', type: Date], '2026-10-19')

        then:
        result.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.of(2026, 10, 19)
    }

    void 'saved Date values render in the format expected by a date input'() {
        given:
        Date saved = AdminCrudService.convert([name: 'receivedAt', type: Date], '2026-10-19')

        expect:
        AdminFormValues.format(saved, 'date') == '2026-10-19'
        AdminFormValues.format(null, 'date') == null
    }
}
