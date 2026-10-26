package grailsadmin

import grails.core.GrailsApplication
import java.time.*
import org.hibernate.FetchMode

class AdminCrudService {

    GrailsApplication grailsApplication
    AdminMetadataService adminMetadataService
    AdminSecurityService adminSecurityService
    AdminRegistryService adminRegistryService

    Map page(AdminModel model, Map params) {
        model.domainClass.withTransaction([readOnly: true]) { pageInTransaction(model, params) }
    }

    private Map pageInTransaction(AdminModel model, Map params) {
        if (!adminSecurityService.allowed(model, 'view')) throw new SecurityException('Forbidden')
        Map meta = adminMetadataService.inspect(model.domainClass)
        int maxPageSize = grailsApplication.config.getProperty('grailsAdmin.maxPageSize', Integer, 100)
        int requested = positive(integer(params.max), 25)
        int max = Math.min(Math.max(1, maxPageSize), requested)
        int page = positive(integer(params.page), 1)
        int offset = (int) Math.min(Integer.MAX_VALUE - max, ((long) page - 1L) * max)
        List<String> sortable = model.listDisplay.findAll {
            !meta.byName[it].association && adminSecurityService.readable(model, it)
        }
        String sort = sortable.contains(params.sort) ? params.sort :
            (model.ordering && sortable.contains(model.ordering.first()) ? model.ordering.first() : 'id')
        String direction = params.order == 'desc' ? 'desc' : 'asc'
        String query = params.q?.toString()?.trim()?.take(200)
        List<String> searchFields = model.searchFields.findAll { adminSecurityService.readable(model, it) }
        if (query && !searchFields) throw new IllegalArgumentException('No searchable fields are available')
        Map filters = [:]
        model.listFilter.each { field ->
            String raw = params["filter_${field}"]?
            if (raw != null && !adminSecurityService.readable(model, field)) {
                throw new IllegalArgumentException("Filter is not available: $field")
            }
            if (raw != null && raw.length() > 100) throw new IllegalArgumentException("Filter is too long: $field")
            if (raw?.trim()) filters[field] = convert(meta.byName[field], raw.trim())
        }
        def result = model.domainClass.createCriteria().list(max: max, offset: offset) {
            applyScope(model, 'view', delegate)
            model.listDisplay.findAll { meta.byName[it].association }.each { field -> fetchMode(field, FetchMode.JOIN) }
            if (query && searchFields) {
                or {
                    searchFields.each { field -> ilike(field, "%${escapeLike(query)}%") }
                }
            }
            filters.each { field, value -> eq(field, value) }
            order(sort, direction)
        }
        [records: result, total: result.totalCount, page: page, max: max, sort: sort, order: direction, query: query ?: '', filters: filters]
    }

    Object get(AdminModel model, String id, String operation = 'view') {
        model.domainClass.withTransaction([readOnly: true]) { getInTransaction(model, id, operation) }
    }

    private Object getInTransaction(AdminModel model, String id, String operation) {
        if (!adminSecurityService.allowed(model, operation)) throw new SecurityException('Forbidden')
        if (!(id ==~ /\d{1,18}/)) return null
        List<String> associations = adminMetadataService.inspect(model.domainClass).fields
            .findAll { it.association }*.name
        model.domainClass.createCriteria().get {
            eq('id', id.toLong())
            associations.each { field -> fetchMode(field, FetchMode.JOIN) }
            applyScope(model, operation, delegate)
        }
    }

    Object save(AdminModel model, Object record, Map input, boolean creating) {
        model.domainClass.withTransaction { saveInTransaction(model, record, input, creating) }
    }

    private Object saveInTransaction(AdminModel model, Object record, Map input, boolean creating) {
        Map meta = adminMetadataService.inspect(model.domainClass)
        String operation = creating ? 'add' : 'change'
        if (!adminSecurityService.allowed(model, operation)) throw new SecurityException('Forbidden')
        if (!creating) {
            record = getInTransaction(model, record.id?.toString(), 'change')
            if (!record) throw new SecurityException('Record outside admin scope')
        }
        Map<String, Object> changes = [:]
        model.formFields.each { field ->
            if (model.readonlyFields.contains(field) || !adminSecurityService.fieldWritable(model, field, operation)) return
            if (!input.containsKey(field) && !(meta.byName[field].type == Boolean && input.containsKey("_$field"))) return
            Map definition = meta.byName[field]
            changes[field] = convert(definition, input.containsKey(field) ? input[field]?.toString() : 'false')
            if (definition.association && changes[field] != null) {
                AdminModel target = adminRegistryService.all().find { it.domainClass == definition.type }
                if (!target || !adminSecurityService.allowed(target, 'view') ||
                    !get(target, changes[field].id?.toString(), 'view')) {
                    throw new AdminFieldConversionException(field, definition.type,
                        new IllegalArgumentException('Association is not available'))
                    }
            }
        }
        if (model.writeHandlerBean) {
            Object saved = grailsApplication.mainContext.getBean(model.writeHandlerBean)
                .save(model, record, changes.asImmutable(), creating)
            if (!model.domainClass.isInstance(saved)) throw new IllegalStateException('Admin write handler returned another type')
            if (!saved.hasErrors() && saved.id != null) audit(model, operation, [saved.id as Long])
            return saved
        }
        changes.each { field, value -> record."$field" = value }
        if (!record.validate()) return record
        record.save(flush: true)
        if (!record.hasErrors() && record.id != null) audit(model, operation, [record.id as Long])
        record
    }

    void delete(AdminModel model, Object record) {
        model.domainClass.withTransaction { deleteInTransaction(model, record) }
    }

    private void deleteInTransaction(AdminModel model, Object record) {
        if (!adminSecurityService.allowed(model, 'delete')) throw new SecurityException('Forbidden')
        record = getInTransaction(model, record.id?.toString(), 'delete')
        if (!record) throw new SecurityException('Record outside admin scope')
        if (model.writeHandlerBean) grailsApplication.mainContext.getBean(model.writeHandlerBean).delete(model, record)
        else record.delete(flush: true)
        audit(model, 'delete', [record.id as Long])
    }

    List options(Map field) {
        if (!field.association) return []
        AdminModel target = adminRegistryService.all().find { it.domainClass == field.type }
        if (!target || !adminSecurityService.allowed(target, 'view')) return []
        field.type.withTransaction([readOnly: true]) {
            if (!target.scopeBean) return field.type.list(max: 100, sort: 'id', order: 'asc')
            field.type.createCriteria().list(max: 100) {
                applyScope(target, 'view', delegate)
                order('id', 'asc')
            }
        }
    }

    List<Map> availableBulkActions(AdminModel model) {
        model.bulkActions.collect { key, beanName ->
            AdminBulkAction action = grailsApplication.mainContext.getBean(beanName)
            boolean allowed = adminSecurityService.allowed(model, action.permission) &&
                (action.permission != 'change' || action.fields.every {
                    adminSecurityService.fieldWritable(model, it, 'change')
                })
            allowed ? [key: key, label: action.label] : null
        }.findAll().asImmutable()
    }

    int bulk(AdminModel model, String actionKey, List selectedIds) {
        model.domainClass.withTransaction { bulkInTransaction(model, actionKey, selectedIds) } as int
    }

    private int bulkInTransaction(AdminModel model, String actionKey, List selectedIds) {
        String beanName = model.bulkActions[actionKey]
        if (!beanName) throw new IllegalArgumentException('Unknown bulk action')
        AdminBulkAction action = grailsApplication.mainContext.getBean(beanName)
        if (!adminSecurityService.allowed(model, action.permission)) throw new SecurityException('Forbidden')
        if (action.permission == 'change' && action.fields.any {
            !adminSecurityService.fieldWritable(model, it, 'change')
        }) throw new SecurityException('Forbidden field')
        List<String> ids = (selectedIds ?: []).collect { it?.toString() }.unique()
        if (!ids || ids.size() > 100 || ids.any { !(it ==~ /\d{1,18}/) })
            throw new IllegalArgumentException('Select 1 to 100 valid records')
        List<Object> records = ids.collect { get(model, it, action.permission) }
        if (records.any { it == null }) throw new IllegalArgumentException('Selected record is unavailable')
        action.execute(model, records.asImmutable())
        audit(model, "bulk:$actionKey", records.collect { it.id as Long })
        records.size()
    }

    private void audit(AdminModel model, String operation, List<Long> ids) {
        String beanName = grailsApplication.config.getProperty('grailsAdmin.auditSinkBean', String, '')
        if (!beanName) return
        AdminAuditSink sink = grailsApplication.mainContext.getBean(beanName)
        sink.record(model, operation, ids.asImmutable(), adminSecurityService.username())
    }

    static Object convert(Map field, String raw) {
        if (raw == null || raw == '') return null
        Class type = field.type
        if (field.association) {
            if (!(raw ==~ /\d{1,18}/))
                throw new AdminFieldConversionException(field.name?.toString(), type,
                    new IllegalArgumentException('Invalid association'))
            def target = type.get(raw.toLong())
            if (!target) {
                throw new AdminFieldConversionException(field.name?.toString(), type,
            }
                    new IllegalArgumentException('Unknown association'))
            return target
        }
        try {
            if (type == String) return raw.take(10000)
            if (type in [Integer, int]) return Integer.valueOf(raw)
            if (type in [Long, long]) return Long.valueOf(raw)
            if (type == Short) return Short.valueOf(raw)
            if (type == BigDecimal) return new BigDecimal(raw)
            if (type == Double) return Double.valueOf(raw)
            if (type == Float) return Float.valueOf(raw)
            if (type in [Boolean, boolean]) {
                if (!(raw in ['true', 'false', 'on', 'off', '1', '0'])) throw new IllegalArgumentException('Invalid boolean')
                return raw in ['true', 'on', '1']
            }
            if (type == LocalDate) return LocalDate.parse(raw)
            if (type == LocalDateTime) return LocalDateTime.parse(raw)
            if (type == Date) return Date.from(LocalDate.parse(raw).atStartOfDay(ZoneId.systemDefault()).toInstant())
            if (type.isEnum()) return Enum.valueOf(type, raw)
        } catch (IllegalArgumentException | DateTimeException error) {
            throw new AdminFieldConversionException(field.name?.toString(), type, error)
        }
        throw new IllegalArgumentException("Unsupported field ${field.name}")
    }

    private static int positive(Integer value, int fallback) { value != null && value > 0 ? value : fallback }
    private static Integer integer(Object value) {
        try { value?.toString()?.toInteger() } catch (NumberFormatException ignored) { null }
    }
    private static String escapeLike(String value) { value.replace('\\', '\\\\').replace('%', '\\%').replace('_', '\\_') }

    private void applyScope(AdminModel model, String operation, Object builder) {
        if (!model.scopeBean) return
        Closure criteria = grailsApplication.mainContext.getBean(model.scopeBean).criteria(model, operation)
        if (criteria == null) return
        criteria.call(builder)
    }

}
