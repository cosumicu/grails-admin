<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>Grails Admin example</title>
</head>
<body>
<h1>Grails Admin example</h1>
<p>This application hosts the Grails Admin plugin and its sample domains. <a href="${createLink(uri: '/admin')}">Open Grails Admin</a>.</p>

<details>
    <summary>Application status</summary>
    <ul>
        <li>Environment: ${grails.util.Environment.current.name}</li>
        <li>App version: <g:meta name="info.app.version"/></li>
        <li>Grails version: <g:meta name="info.app.grailsVersion"/></li>
        <li>Groovy version: ${GroovySystem.getVersion()}</li>
        <li>JVM version: ${System.getProperty('java.version')}</li>
        <li>Reloading active: ${grails.util.Environment.reloadingAgentEnabled}</li>
    </ul>
</details>
<details>
    <summary>Artefacts</summary>
    <ul>
        <li>Controllers: ${grailsApplication.controllerClasses.size()}</li>
        <li>Domains: ${grailsApplication.domainClasses.size()}</li>
        <li>Services: ${grailsApplication.serviceClasses.size()}</li>
        <li>Tag Libraries: ${grailsApplication.tagLibClasses.size()}</li>
    </ul>
</details>
<details>
    <summary>Installed plugins</summary>
    <ul>
        <g:each var="plugin" in="${applicationContext.getBean('pluginManager').allPlugins}">
            <li>${plugin.name} - ${plugin.version}</li>
        </g:each>
    </ul>
</details>

<h2>Available controllers</h2>
<ul>
    <g:each var="c" in="${grailsApplication.controllerClasses.sort { it.fullName }}">
        <li><g:link controller="${c.logicalPropertyName}">${c.fullName}</g:link></li>
    </g:each>
</ul>
</body>
</html>
