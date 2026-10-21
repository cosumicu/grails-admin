<%@ page defaultCodec="html" %>
<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><g:layoutTitle default="Grails Administration"/></title>
    <asset:stylesheet src="admin.css"/>
    <g:layoutHead/>
</head>
<body>
<header class="topbar">
    <a class="brand" href="${createLink(uri:'/admin')}">Grails Administration</a>
    <nav aria-label="Account"><span><g:encodeAs codec="HTML">${username}</g:encodeAs></span>
        <a href="${createLink(uri:viewSiteUrl)}">View site</a>
        <g:if test="${changePasswordUrl}"><a href="${createLink(uri:changePasswordUrl)}">Change password</a></g:if>
        <g:form method="post" uri="${logoutUrl}" class="logout-form" useToken="true"><adminHost:csrfFields/><button type="submit">Log out</button></g:form>
    </nav>
</header>
<div class="shell">
    <aside class="sidebar" aria-label="Models">
        <h2>Administration</h2>
        <g:each in="${groups}" var="entry">
            <details open><summary><g:encodeAs codec="HTML">${entry.key}</g:encodeAs></summary>
                <g:each in="${entry.value}" var="item">
                    <a class="${adminModel == item ? 'active' : ''}" href="${createLink(uri:'/admin/' + grailsadmin.AdminRegistryService.slug(item.group) + '/' + item.slug)}"><g:encodeAs codec="HTML">${item.label}</g:encodeAs></a>
                </g:each>
            </details>
        </g:each>
    </aside>
    <main id="main"><g:layoutBody/></main>
</div>
</body>
</html>
