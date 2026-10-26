package grailsadmin

import org.springframework.context.MessageSource
import org.springframework.context.NoSuchMessageException
import org.springframework.validation.FieldError

import java.time.LocalDate
import java.time.LocalDateTime

class AdminFormErrors {

    static Map collect(Object record, MessageSource messageSource, Locale locale, Map definitions = [:]) {
        Map<String, String> fields = [:]
        List<String> general = []
        record.errors?.allErrors?.each { error ->
            String message
            if (error instanceof FieldError && error.code?.toLowerCase()?.contains('maxsize') &&
                    definitions[error.field]?.maxSize) {
                message = "${definitions[error.field].label ?: error.field} must be at most ${definitions[error.field].maxSize} characters."
            } else {
                try { message = messageSource.getMessage(error, locale) }
                catch (NoSuchMessageException ignored) {
                    message = error instanceof FieldError ?
                        "${definitions[error.field]?.label ?: error.field} is invalid." : 'The record is invalid.'
                }
                    }
            if (error instanceof FieldError) fields[error.field] = message
            else general << message
        }
        [fields: fields, general: general]
    }

    static String conversionMessage(AdminFieldConversionException error, Map definition) {
        Class type = error.fieldType
        String label = definition?.label ?: error.fieldName
        if (type == Date || type == LocalDate) return "Enter a valid date for ${label} (YYYY-MM-DD)."
        if (type == LocalDateTime) return "Enter a valid date and time for ${label}."
        if (definition?.association) return "Select a valid ${label}."
        if (type?.isEnum()) return "Select a valid ${label}."
        "Enter a valid value for ${label}."
    }

    static String logSummary(Object record) {
        record.errors?.allErrors?.collect { error ->
            error instanceof FieldError ? "${error.field} (${error.code})" : "${error.objectName} (${error.code})"
        }?.join('; ') ?: 'Validation failed'
    }

}
