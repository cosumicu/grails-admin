package grailsadmin

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional

@Transactional(readOnly = true)
class AdminRegistryService {

    GrailsApplication grailsApplication
    AdminMetadataService adminMetadataService
    private final Map<String, AdminModel> models = new LinkedHashMap<>()
    private volatile boolean initialized = false

    boolean isEnabled() {
        grailsApplication.config.getProperty('grailsAdmin.enabled', Boolean, false)
    }

    synchronized void initialize() {
        if (initialized) return
        if (!enabled) { initialized = true; return }
        String auditSinkBean = grailsApplication.config.getProperty('grailsAdmin.auditSinkBean', String, '')
        if (auditSinkBean && (!grailsApplication.mainContext.containsBean(auditSinkBean) ||
            !(grailsApplication.mainContext.getBean(auditSinkBean) instanceof AdminAuditSink))) {
            throw new IllegalArgumentException("Invalid admin audit sink: $auditSinkBean")
        }
        Map configured = grailsApplication.config.getProperty('grailsAdmin.models', Map, [:]) ?: [:]
        configured.each { key, value -> register(key.toString(), value as Map) }
        initialized = true
    }

    synchronized AdminModel register(String key, Map options) {
        if (!key || !(key ==~ /[a-z][a-z0-9-]*/)) throw new IllegalArgumentException('Invalid model key')
        if (models.values().any { it.key == key }) throw new IllegalArgumentException("Duplicate admin model: $key")
        String className = options.domain?.toString()
        def artefact = className ? grailsApplication.getArtefact('Domain', className) : null
        if (!artefact) throw new IllegalArgumentException("Not a GORM domain: $className")
        Class domainClass = artefact.clazz
        Map metadata = adminMetadataService.inspect(domainClass)
        String group = options.group?.toString() ?: domainClass.package?.name?.tokenize('.')?.last() ?: 'General'
        String groupSlug = slug(group)
        String modelSlug = slug(options.slug?.toString() ?: key)
        String route = "$groupSlug/$modelSlug"
        if (models.containsKey(route)) throw new IllegalArgumentException("Duplicate admin route: $route")
        List<String> display = fields(options.listDisplay, metadata) ?: metadata.fields.take(4)*.name
        List<String> search = fields(options.searchFields, metadata)
        search.each { if (!(metadata.byName[it].type in [String, CharSequence])) throw new IllegalArgumentException("Search field must be text: $it") }
        List<String> filters = fields(options.listFilter, metadata)
        List<String> ordering = fields(options.ordering, metadata)
        List<String> readonly = options.readonlyFields == ['*'] ? metadata.fields*.name : fields(options.readonlyFields, metadata)
        List<String> form = fields(options.formFields, metadata) ?: metadata.fields*.name
        Map<String, List<String>> formGroups = [:]
        if (options.formGroups != null && !(options.formGroups instanceof Map)) {
            throw new IllegalArgumentException('Form groups must be a map')
        }
        if (options.formGroups) {
            Set<String> grouped = [] as Set
            Map<String, List> normalizedGroups = [:]
            options.formGroups.each { label, names ->
                String rawLabel = label?.toString() ?: ''
                def indexed = rawLabel =~ /^(.*)\[(\d+)\]$/
                if (indexed.matches()) {
                    String groupName = indexed.group(1)
                    if (!normalizedGroups.containsKey(groupName)) normalizedGroups[groupName] = []
                    normalizedGroups[groupName] << [index: indexed.group(2).toInteger(), value: names]
                } else normalizedGroups[rawLabel] = names instanceof Collection ? names.toList() : [names]
            }
            normalizedGroups.each { label, values ->
                String title = label?.toString()?.trim()
                def names = values.sort { it instanceof Map && it.containsKey('index') ? it.index : 0 }
                    .collect { it instanceof Map && it.containsKey('value') ? it.value : it }
                List<String> groupFields = fields(names, metadata)
                if (!title || !groupFields || groupFields.any { !form.contains(it) || !grouped.add(it) })
                    throw new IllegalArgumentException("Invalid admin form group: $title")
                formGroups[title] = groupFields.asImmutable()
            }
            List<String> remaining = form.findAll { !grouped.contains(it) }
            if (remaining) formGroups['Other'] = remaining.asImmutable()
        } else formGroups['Details'] = form.asImmutable()
        Map<String, String> widgetTemplates = [:]
        if (options.widgetTemplates != null && !(options.widgetTemplates instanceof Map)) {
            throw new IllegalArgumentException('Widget templates must be a map')
        }
        (options.widgetTemplates ?: [:]).each { field, path ->
            String name = field
            String templatePath = path?.toString()
            if (!form.contains(name) || !(templatePath ==~ /\/admin\/widgets\/[A-Za-z][A-Za-z0-9\/_-]*/)) {
                throw new IllegalArgumentException("Invalid admin widget template: $name")
            }
            widgetTemplates[name] = templatePath
        }
        Map<String, String> pageTemplates = [:]
        if (options.pageTemplates != null && !(options.pageTemplates instanceof Map)) {
            throw new IllegalArgumentException('Page templates must be a map')
        }
        (options.pageTemplates ?: [:]).each { page, path ->
            String name = page
            String templatePath = path?.toString()
            if (!(name in ['list', 'show', 'form', 'delete']) ||
                !(templatePath ==~ /\/admin\/custom\/[A-Za-z][A-Za-z0-9\/_-]*/)) {
                throw new IllegalArgumentException("Invalid admin page template: $name")
            }
            pageTemplates[name] = templatePath
        }
        String writeHandlerBean = options.writeHandlerBean?.toString()
        if (writeHandlerBean && (!grailsApplication.mainContext.containsBean(writeHandlerBean) ||
            !(grailsApplication.mainContext.getBean(writeHandlerBean) instanceof AdminWriteHandler))) {
            throw new IllegalArgumentException("Invalid admin write handler: $writeHandlerBean")
            }
        String scopeBean = options.scopeBean?.toString()
        if (scopeBean && (!grailsApplication.mainContext.containsBean(scopeBean) ||
            !(grailsApplication.mainContext.getBean(scopeBean) instanceof AdminQueryScope))) {
            throw new IllegalArgumentException("Invalid admin query scope: $scopeBean")
            }
        Map<String, String> bulkActions = [:]
        if (options.bulkActions != null && !(options.bulkActions instanceof Map)) {
            throw new IllegalArgumentException('Bulk actions must be a map')
        }
        (options.bulkActions ?: [:]).each { actionKey, beanName ->
            String action = actionKey
            String bean = beanName?.toString()
            if (!(action ==~ /[a-z][a-z0-9-]*/) || !bean ||
                !grailsApplication.mainContext.containsBean(bean) ||
                !(grailsApplication.mainContext.getBean(bean) instanceof AdminBulkAction)) {
                throw new IllegalArgumentException("Invalid admin bulk action: $action")
                }
            AdminBulkAction implementation = grailsApplication.mainContext.getBean(bean)
            if (!(implementation.permission in ['change', 'delete']) || !implementation.label) {
                throw new IllegalArgumentException("Invalid admin bulk action contract: $action")
            }
            if (implementation.permission == 'change' && (!implementation.fields ||
                implementation.fields.any { !metadata.byName.containsKey(it) || readonly.contains(it) })) {
                throw new IllegalArgumentException("Invalid admin bulk action fields: $action")
            }
            bulkActions[action] = bean
            }
        int displayOrder = 100
        if (options.displayOrder != null) {
            try { displayOrder = Integer.parseInt(options.displayOrder.toString()) }
            catch (NumberFormatException ignored) { throw new IllegalArgumentException('Invalid display order') }
        }
        AdminModel model = new AdminModel(key, group, modelSlug,
            options.label?.toString() ?: domainClass.simpleName.replaceAll(/([a-z])([A-Z])/, '$1 $2'),
            domainClass, display.asImmutable(), search.asImmutable(), filters.asImmutable(),
            ordering.asImmutable(), readonly.asImmutable(), form.asImmutable(),
            options.allowCreate != false, options.allowChange != false, options.allowDelete == true,
            writeHandlerBean, scopeBean, bulkActions.asImmutable(), displayOrder, formGroups.asImmutable(),
            widgetTemplates.asImmutable(), pageTemplates.asImmutable())
        models[route] = model
        model
        }

    synchronized AdminModel resolve(String group, String model) { models["$group/$model"] }
    synchronized List<AdminModel> all() {
        models.values().toList().sort { a, b -> a.displayOrder <=> b.displayOrder ?: a.label <=> b.label }.asImmutable()
    }

    private static List<String> fields(Object value, Map metadata) {
        List<String> names = value == null ? [] : (value instanceof Collection ? value : [value])*.toString()
        names.each { if (!metadata.byName.containsKey(it)) throw new IllegalArgumentException("Unknown admin field: $it") }
        names.unique()
    }

    static String slug(String value) {
        String result = value.toLowerCase(Locale.ROOT).replaceAll(/[^a-z0-9]+/, '-').replaceAll(/^-|-$/, '')
        if (!result) throw new IllegalArgumentException('Empty admin slug')
        result
    }

    }
