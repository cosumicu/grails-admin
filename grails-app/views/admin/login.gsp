<%@ page defaultCodec="html" %>
<html lang="en">
<head>
    <meta name="layout" content="adminLogin"/>
    <title>Grails Admin sign in</title>
</head>
<body>
<main class="admin-login">
    <h1>Grails Admin</h1>
    <g:if test="${error}"><p role="alert"><g:encodeAs codec="HTML">${error}</g:encodeAs></p></g:if>
    <g:form method="post" uri="/admin/authenticate" useToken="true">
        <adminHost:csrfFields/>
        <label for="username">Username</label>
        <input id="username" name="username" autocomplete="username" required>
        <label for="password">Password</label>
        <input id="password" name="password" type="password" autocomplete="current-password" required>
        <button type="submit">Sign in</button>
    </g:form>
</main>
</body>
</html>
