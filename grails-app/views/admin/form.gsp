<%@ page defaultCodec="html" %>
<html><head><meta name="layout" content="admin"/><title>${creating ? 'Add' : 'Edit'} ${adminModel.label}</title></head><body>
<div class="page-head"><div><a class="crumb" href="${createLink(uri:'/admin/' + params.group + '/' + params.model)}">${adminModel.label}</a><h1>${creating ? 'Add' : 'Edit'} ${adminModel.label}</h1></div></div>
<section class="panel form-panel"><g:if test="${saveError || fieldErrors || generalErrors}"><div class="form-error" role="alert">
<p>${saveError ?: (creating ? 'The record was not saved. Please correct the following errors:' : 'The changes were not saved. Please correct the following errors:')}</p>
<g:if test="${generalErrors}"><ul><g:each in="${generalErrors}" var="error"><li>${error}</li></g:each></ul></g:if>
</div></g:if><g:form method="post" uri="${creating ? '/admin/' + params.group + '/' + params.model + '/save' : '/admin/' + params.group + '/' + params.model + '/' + record.id + '/update'}" useToken="true">
<adminHost:csrfFields/>
<g:each in="${adminModel.formGroups}" var="formGroup"><fieldset class="form-group"><legend>${formGroup.key}</legend>
<g:each in="${formGroup.value}" var="name"><g:set var="field" value="${metadata.byName[name]}"/><g:set var="editable" value="${!adminModel.readonlyFields.contains(name) && adminSecurityService.fieldWritable(adminModel, name, creating ? 'add' : 'change')}"/><g:set var="submitted" value="${submittedValues?.containsKey(name)}"/><g:set var="inputValue" value="${submitted ? submittedValues[name] : record[name]}"/>
<g:if test="${adminSecurityService.readable(adminModel, name) || editable}"><div class="field"><label for="field_${name}">${field.label}<g:if test="${field.required}"><span aria-label="required"> *</span></g:if></label>
<g:if test="${editable}">
    <g:if test="${adminModel.widgetTemplates[name]}"><g:render template="${adminModel.widgetTemplates[name]}" model="${[name:name, field:field, record:record, creating:creating, inputValue:inputValue, fieldError:fieldErrors[name]]}"/></g:if>
    <g:elseif test="${field.widget == 'association'}"><g:select id="field_${name}" name="${name}" from="${options[name]}" optionKey="id" value="${submitted ? inputValue : record[name]?.id}" noSelection="${['':'— None —']}"/></g:elseif>
    <g:elseif test="${field.widget == 'enum'}"><g:select id="field_${name}" name="${name}" from="${field.type.enumConstants}" value="${inputValue?.toString()}" noSelection="${['':'— Select —']}"/></g:elseif>
    <g:elseif test="${field.widget == 'checkbox'}"><g:checkBox id="field_${name}" name="${name}" value="${inputValue in [true, 'true', 'on', '1']}"/></g:elseif>
    <g:else><input id="field_${name}" name="${name}" type="${submitted && fieldErrors[name] && field.widget in ['date', 'datetime-local', 'number'] ? 'text' : field.widget}" value="${grailsadmin.AdminFormValues.format(inputValue, field.widget)}" ${fieldErrors[name] ? 'aria-invalid="true" aria-describedby="error_' + name + '"' : ''} ${field.required ? 'required' : ''} ${field.widget == 'number' && !fieldErrors[name] ? 'step="any"' : ''} ${field.maxSize ? 'maxlength="' + field.maxSize + '"' : ''}/></g:else>
</g:if><g:else><div class="readonly"><g:encodeAs codec="HTML">${record[name]?.toString()}</g:encodeAs></div></g:else>
<g:if test="${fieldErrors[name]}"><div class="error" id="error_${name}">${fieldErrors[name]}</div></g:if></div></g:if>
</g:each></fieldset></g:each>
<div class="form-actions"><button type="submit">Save</button><a href="${createLink(uri:'/admin/' + params.group + '/' + params.model)}">Cancel</a></div>
</g:form></section></body></html>
