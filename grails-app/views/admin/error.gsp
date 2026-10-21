<%@ page defaultCodec="html" %>
<html><head><meta name="layout" content="adminLogin"/><title>${title ?: 'Grails Admin error'}</title></head><body>
<main class="admin-login"><h1>Grails Administration</h1><section class="panel form-panel" role="alert">
<h2>${title}</h2><p>${message}</p>
<g:if test="${errorId}"><p>Error ID: <strong>${errorId}</strong></p></g:if>
<p><a href="${createLink(uri:actionUrl ?: '/admin')}">${actionLabel ?: 'Admin home'}</a></p>
</section></main></body></html>
