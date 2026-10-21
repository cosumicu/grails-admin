package grailsadmin

import groovy.transform.Immutable

@Immutable
class AdminModel {
    String key
    String group
    String slug
    String label
    Class domainClass
    List<String> listDisplay
    List<String> searchFields
    List<String> listFilter
    List<String> ordering
    List<String> readonlyFields
    List<String> formFields
    boolean allowCreate
    boolean allowChange
    boolean allowDelete
    String writeHandlerBean
    String scopeBean
    Map<String, String> bulkActions
    int displayOrder
    Map<String, List<String>> formGroups
    Map<String, String> widgetTemplates
    Map<String, String> pageTemplates
}
