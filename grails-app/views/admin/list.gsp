<%@ page defaultCodec="html" %>
<html><head><meta name="layout" content="admin"/><title>${adminModel.label}</title></head><body>
<div class="page-head"><div><a class="crumb" href="${createLink(uri:'/admin')}">Home</a><h1><g:encodeAs codec="HTML">${adminModel.label}</g:encodeAs></h1></div>
<g:if test="${adminSecurityService.allowed(adminModel, 'add')}"><a class="button" href="${createLink(uri:'/admin/' + params.group + '/' + params.model + '/create')}">Add ${adminModel.label}</a></g:if></div>
<section class="panel">
<g:if test="${flash.message}"><p class="notice" role="status"><g:encodeAs codec="HTML">${flash.message}</g:encodeAs></p></g:if>
<div class="list-layout ${visibleFilters ? '' : 'no-filters'}"><div class="list-main">
<g:if test="${searchable}"><g:form method="get" uri="/admin/${params.group}/${params.model}" class="tools">
    <label for="q">Search</label><input id="q" name="q" value="${query}" type="search" maxlength="200"/>
    <g:each in="${visibleFilters}" var="field"><input type="hidden" name="filter_${field}" value="${params['filter_' + field]}"/></g:each>
    <button type="submit">Search</button>
</g:form></g:if>
<g:render template="results" model="${[adminModel:adminModel, metadata:metadata, records:records, total:total, page:page, max:max, sort:sort, order:order, query:query, linkParams:linkParams, bulkActions:bulkActions, adminSecurityService:adminSecurityService]}"/>
</div>
<g:if test="${visibleFilters}"><aside class="list-filters" aria-label="List filters"><h2>Filter</h2>
<g:form method="get" uri="/admin/${params.group}/${params.model}">
    <input type="hidden" name="q" value="${query}"/>
    <g:each in="${visibleFilters}" var="field"><div class="filter-field"><label for="filter_${field}">${metadata.byName[field].label}</label>
        <g:if test="${metadata.byName[field].widget == 'checkbox'}"><select id="filter_${field}" name="filter_${field}"><option value="">All</option><option value="true" ${params['filter_' + field] == 'true' ? 'selected' : ''}>Yes</option><option value="false" ${params['filter_' + field] == 'false' ? 'selected' : ''}>No</option></select></g:if>
        <g:else><input id="filter_${field}" name="filter_${field}" value="${params['filter_' + field]}"/></g:else>
    </div></g:each>
    <button type="submit">Apply filters</button>
</g:form></aside></g:if>
</div></section></body></html>
