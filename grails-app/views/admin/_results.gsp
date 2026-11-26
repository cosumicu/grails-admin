<%@ page defaultCodec="html" %>
<div id="admin-list-results">
<g:if test="${bulkActions}"><g:form method="post" uri="${adminPath}/${params.group}/${params.model}/bulk" useToken="true" class="bulk-results">
    <adminHost:csrfFields/>
    <g:render template="table" model="${[adminModel:adminModel, metadata:metadata, records:records, sort:sort, order:order, linkParams:linkParams, bulkActions:bulkActions, adminSecurityService:adminSecurityService, adminPath:adminPath]}"/>
    <div class="bulk-bar"><label for="bulkAction">Action</label><g:select id="bulkAction" name="bulkAction" from="${bulkActions}" optionKey="key" optionValue="label" noSelection="${['':'Choose action']}" required="true"/>
    <button type="submit">Apply to selected</button></div>
</g:form></g:if><g:else>
    <g:render template="table" model="${[adminModel:adminModel, metadata:metadata, records:records, sort:sort, order:order, linkParams:linkParams, bulkActions:bulkActions, adminSecurityService:adminSecurityService, adminPath:adminPath]}"/>
</g:else>
<div class="pager"><g:if test="${total != null}"><span>${total} records</span></g:if><g:if test="${page > 1}"><a href="${createLink(uri:adminPath + '/' + params.group + '/' + params.model, params:linkParams + [page:page-1, sort:sort, order:order])}">Previous</a></g:if>
<span>Page ${page}</span><g:if test="${hasNext}"><a href="${createLink(uri:adminPath + '/' + params.group + '/' + params.model, params:linkParams + [page:page+1, sort:sort, order:order])}">Next</a></g:if></div>
</div>
