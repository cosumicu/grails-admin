<%@ page defaultCodec="html" %>
<html><head><meta name="layout" content="admin"/><title>Dashboard</title></head><body>
<h1>Site administration</h1>
<p class="muted">Choose a registered model to view its records.</p>
<g:if test="${!groups}"><div class="empty">No models are available to your account.</div></g:if>
<g:each in="${groups}" var="entry">
    <section class="panel"><h2><g:encodeAs codec="HTML">${entry.key}</g:encodeAs></h2>
        <div class="model-grid"><g:each in="${entry.value}" var="item">
            <div class="model-row"><a href="${createLink(uri:'/admin/' + grailsadmin.AdminRegistryService.slug(item.group) + '/' + item.slug)}"><g:encodeAs codec="HTML">${item.label}</g:encodeAs></a>
                <g:if test="${adminSecurityService.allowed(item, 'add')}"><a class="minor" href="${createLink(uri:'/admin/' + grailsadmin.AdminRegistryService.slug(item.group) + '/' + item.slug + '/create')}">Add</a></g:if>
            </div>
        </g:each></div>
    </section>
</g:each>
</body></html>
