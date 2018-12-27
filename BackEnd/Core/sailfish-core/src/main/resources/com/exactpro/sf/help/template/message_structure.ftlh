<link rel="stylesheet" type="text/css" href="../../../resources/static-help.css"/>
<span class="eps-content-value" >
	<h2>Message information:</h2>
	<table class='eps-content-message-table'>
		<tr><td>Name</td><td>${name}</td></tr>
		<tr><td>NameSpace</td><td>${namespace}</td></tr>
		<tr><td>Type</td><td>${type}</td></tr>
	</table>
	<#if attributes?has_content>
		<h3> Attributes: </h3>
		<table>
			<tr class='helpTableHeader'><td>Name</td><td>Value</td></tr>
            <#list attributes?values as attribute>
				<tr><td>${attribute.getName()}</td><td>${attribute.getValue()}</td></tr>
			</#list>
		</table>
	</#if>
</span>
