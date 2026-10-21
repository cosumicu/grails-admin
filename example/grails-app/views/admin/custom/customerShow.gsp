<%@ page defaultCodec="html" %>
<html><head><meta name="layout" content="admin"/><title>${adminModel.label} #${record.id}</title></head><body>
<div class="page-head"><div><a class="crumb" href="${createLink(uri:'/admin/' + params.group + '/' + params.model)}">${adminModel.label}</a><h1>Customer #${record.id}</h1></div>
<g:if test="${adminSecurityService.allowed(adminModel, 'change')}"><a class="button" href="${createLink(uri:'/admin/' + params.group + '/' + params.model + '/' + record.id + '/edit')}">Edit</a></g:if></div>
<section class="panel"><p class="muted">Synthetic customer profile rendered by the host application's custom GSP.</p>
<dl class="details"><g:each in="${metadata.fields}" var="field"><g:if test="${adminSecurityService.readable(adminModel, field.name)}"><dt>${field.label}</dt><dd>${record[field.name]?.toString()}</dd></g:if></g:each></dl></section>
</body></html>
