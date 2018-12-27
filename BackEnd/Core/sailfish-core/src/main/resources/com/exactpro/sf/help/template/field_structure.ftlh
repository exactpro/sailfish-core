<link rel="stylesheet" type="text/css" href="../../../resources/static-help.css"/>
<span class="eps-content-value">
	<h2>Field information:</h2>
<#if description?has_content>
    <p class='eps-content-description'>Description: ${description}</p>
</#if>
	<table class='eps-content-message-table'>
		<tr><td>Name</td><td>${name}</td></tr>
    <#if referenceName?? && isComplex>
        <tr><td>Reference</td><td>Message (${referenceName})</td></tr>
    </#if>
    <#if !isComplex>
        <tr><td>Type</td><td>${type}</td></tr>
        <tr><td>Required</td><td>${isRequired?c}</td></tr>
        <#if isCollection>
        <tr><td>Is collection</td><td>${isCollection?c}</td></tr>
        </#if>
        <#if defaultValue?has_content>
        <tr><td>Default value</td><td>${(defaultValue?is_boolean || defaultValue?is_number)?then(defaultValue?c, defaultValue)}</td></tr>
        </#if>
    </#if>
	</table>

<#if attributes?has_content>
    <h3> Attributes: </h3>
            <table class='eps-content-message-table'>
                <tr class='helpTableHeader'><td>Name</td><td>Value</td></tr>
                <#list attributes?values as attribute>
                    <tr><td>${attribute.getName()}</td><td>${attribute.getValue()}</td></tr>
                </#list>
            </table>
</#if>

<#if !isComplex && validValues?has_content>
    <h3> Valid Values:</h3>
    <table class='eps-content-message-table'>
                <tr class='helpTableHeader'><td>Name</td><td>Value</td></tr>
        <#list validValues?values as validValue>
            <tr><td>${validValue.getName()}</td><td>${validValue.getValue()}</td></tr>
        </#list>
            </table>
</#if>

</span>
