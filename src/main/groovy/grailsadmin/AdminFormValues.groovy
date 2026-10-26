package grailsadmin

import java.text.SimpleDateFormat

class AdminFormValues {

    static Object format(Object value, String widget) {
        if (value instanceof Date) return new SimpleDateFormat('yyyy-MM-dd').format(value)
        if (widget == 'datetime-local') return value?.toString()?.take(16)
        value
    }

}
