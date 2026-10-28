package grailsadmin

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import java.time.*
import org.grails.datastore.mapping.model.types.ToMany

@Transactional(readOnly = true)
class AdminMetadataService {

    GrailsApplication grailsApplication
    private final Map<Class, Map> cache = new java.util.concurrent.ConcurrentHashMap<>()
    private static final Set<String> EXCLUDED = ['id', 'version', 'dateCreated', 'lastUpdated', 'password', 'passwordHash',
        'salt', 'secret', 'token', 'apiKey', 'springSecurityService'] as Set

    Map inspect(Class domainClass) {
        cache.computeIfAbsent(domainClass) { Class type ->
            def artefact = grailsApplication.getArtefact('Domain', type.name)
            if (!artefact) throw new IllegalArgumentException('Unregistered domain class')
            List<Map> fields = []
            def entity = grailsApplication.mappingContext.getPersistentEntity(type.name)
            if (!entity) throw new IllegalArgumentException('Domain has no GORM mapping')
            entity.persistentProperties.each { property ->
                String name = property.name
                if (EXCLUDED.contains(name) || name ==~ /(?i).*(password|secret|token|credential|privateKey).*/ ||
                    name.startsWith('$') || name == 'class') return
                Class fieldType = property.type
                if (!fieldType || Collection.isAssignableFrom(fieldType) || Map.isAssignableFrom(fieldType) ||
                    fieldType.isArray()) return
                boolean association = grailsApplication.getArtefact('Domain', fieldType.name) != null
                boolean supported = association || fieldType.isEnum() || fieldType in [String, Integer, int, Long, long,
                    Short, short, BigDecimal, Double, double, Float, float, Boolean, boolean, Date, LocalDate, LocalDateTime]
                if (!supported) return
                def constraint = artefact.constrainedProperties?.get(name)
                String widget = association ? 'association' : fieldType.isEnum() ? 'enum' :
                    fieldType in [Boolean, boolean] ? 'checkbox' :
                    fieldType in [Date, LocalDate] ? 'date' : fieldType == LocalDateTime ? 'datetime-local' :
                    fieldType in [Integer, int, Long, long, Short, short, BigDecimal, Double, double, Float, float] ? 'number' :
                    constraint?.email == true ? 'email' : 'text'
                fields << [name: name, type: fieldType, association: association,
                    widget: widget, required: constraint?.nullable == false, maxSize: constraint?.maxSize,
                    label: name.replaceAll(/([a-z])([A-Z])/, '$1 $2').capitalize()]
            }
            fields.sort { it.name }
            Map belongsTo = [:]
            try { if (type.belongsTo instanceof Map) belongsTo = type.belongsTo } catch (MissingPropertyException ignored) { }
            List<Map> relationships = entity.associations.collect { association ->
                [name: association.name, target: association.associatedEntity?.javaClass,
                 toMany: association instanceof ToMany, belongsTo: belongsTo.containsKey(association.name),
                 owningSide: association.owningSide]
            }
            [fields: fields.asImmutable(), byName: fields.collectEntries { [(it.name): it] }.asImmutable(),
             relationships: relationships.asImmutable()].asImmutable()
        }
    }

}
