<link rel="stylesheet" type="text/css" href="../resources/static-help.css"/>
<span class="eps-content-value" >
	<h2>Service information:</h2>
	<table class='eps-content-message-table'>
		<#if description?has_content>
			<tr><td>Description</td><td>${description}</td></tr>
		</#if>	
		<#if dictionaryAlias?has_content>
		<tr><td>Dictionary alias</td><td>${dictionaryAlias}</td></tr>
		</#if>
		<tr><td>URI</td><td>${uri}</td></tr>
		<#if validatorFactoryName?has_content>
			<tr><td>Dictionary validator</td><td><span class='ui-icon ui-icon-circle-check' style='margin: auto'></span></td></tr>
		</#if>
	</table>
</span>
