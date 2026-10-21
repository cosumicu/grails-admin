<%@ page defaultCodec="html" %>
<input id="field_${name}" name="${name}" type="email" value="${record[name]}" ${field.required ? 'required' : ''}/>
<small class="muted">Used only in this synthetic example.</small>
