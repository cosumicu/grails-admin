package grailsadmin

class AdminFieldConversionException extends IllegalArgumentException {
    final String fieldName
    final Class fieldType

    AdminFieldConversionException(String fieldName, Class fieldType, Throwable cause) {
        super("Invalid value for ${fieldName}", cause)
        this.fieldName = fieldName
        this.fieldType = fieldType
    }
}
