<%@ page defaultCodec="html" %>
<div class="table-wrap"><table><thead><tr>
    <g:if test="${bulkActions}"><th scope="col">Select</th></g:if>
    <g:each in="${adminModel.listDisplay}" var="field"><th scope="col"><g:if test="${!metadata.byName[field].association && adminSecurityService.readable(adminModel, field)}"><a href="${createLink(uri:'/admin/' + params.group + '/' + params.model, params:linkParams + [sort:field,order:(sort == field && order == 'asc' ? 'desc' : 'asc')])}">${metadata.byName[field].label}</a></g:if><g:else>${metadata.byName[field].label}</g:else></th></g:each>
</tr></thead><tbody>
<g:each in="${records}" var="record"><tr>
    <g:if test="${bulkActions}"><td><input type="checkbox" name="selectedIds" value="${record.id}" aria-label="Select ${adminModel.label} #${record.id}"/></td></g:if>
    <g:each in="${adminModel.listDisplay}" var="field"><td><g:if test="${adminSecurityService.readable(adminModel, field)}"><a href="${createLink(uri:'/admin/' + params.group + '/' + params.model + '/' + record.id)}"><g:encodeAs codec="HTML">${record[field]?.toString()}</g:encodeAs></a></g:if></td></g:each>
</tr></g:each>
<g:if test="${!records}"><tr><td colspan="${adminModel.listDisplay.size() + (bulkActions ? 1 : 0)}" class="empty">No records found.</td></tr></g:if>
</tbody></table></div>
