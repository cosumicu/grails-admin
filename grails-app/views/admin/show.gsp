<%@ page defaultCodec="html" %>
<html><head><meta name="layout" content="admin"/><title>${adminModel.label} #${record.id}</title></head><body>
<div class="page-head"><div><a class="crumb" href="${createLink(uri:'/admin/' + params.group + '/' + params.model)}">${adminModel.label}</a><h1>${adminModel.label} #${record.id}</h1></div>
<div class="actions"><g:if test="${adminSecurityService.allowed(adminModel, 'change')}"><a class="button" href="${createLink(uri:'/admin/' + params.group + '/' + params.model + '/' + record.id + '/edit')}">Edit</a></g:if>
<g:if test="${adminSecurityService.allowed(adminModel, 'delete')}"><a class="button secondary" href="${createLink(uri:'/admin/' + params.group + '/' + params.model + '/' + record.id + '/delete')}">Delete</a></g:if></div></div>
<g:if test="${flash.message}"><p class="notice" role="status"><g:encodeAs codec="HTML">${flash.message}</g:encodeAs></p></g:if>
<section class="panel"><dl class="details"><g:each in="${metadata.fields}" var="field"><g:if test="${adminSecurityService.readable(adminModel, field.name)}"><dt>${field.label}</dt><dd><g:encodeAs codec="HTML">${record[field.name]?.toString()}</g:encodeAs></dd></g:if></g:each></dl></section>
</body></html>
