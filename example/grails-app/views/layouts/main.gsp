<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title><g:layoutTitle default="Grails Admin example"/></title>
    <style>
        body { margin: 0; font: 16px/1.5 system-ui, sans-serif; color: #1f2937; background: #f8fafc; }
        header { background: #1e293b; color: white; padding: 1rem max(1rem, calc((100vw - 960px) / 2)); }
        header a { color: white; text-decoration: none; }
        main { max-width: 960px; margin: 2rem auto; padding: 0 1rem; }
        a { color: #1d4ed8; }
        details { margin: 0.75rem 0; padding: 0.75rem 1rem; background: white; border: 1px solid #cbd5e1; border-radius: 0.4rem; }
        summary { cursor: pointer; font-weight: 600; }
        ul { padding-left: 1.5rem; }
    </style>
    <g:layoutHead/>
</head>
<body>
<header><a href="${createLink(uri: '/')}">Grails Admin example</a></header>
<main><g:layoutBody/></main>
</body>
</html>
