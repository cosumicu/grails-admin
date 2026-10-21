<%@ page defaultCodec="html" %>
<html><head><meta name="layout" content="admin"/><title>Delete ${adminModel.label}</title></head><body>
<h1>Delete ${adminModel.label} #${record.id}?</h1><section class="panel form-panel"><p>This action cannot be undone.</p>
<g:form method="post" uri="/admin/${params.group}/${params.model}/${record.id}/remove" useToken="true"><adminHost:csrfFields/><button class="danger" type="submit">Delete</button> <a href="${createLink(uri:'/admin/' + params.group + '/' + params.model + '/' + record.id)}">Cancel</a></g:form></section>
</body></html>
