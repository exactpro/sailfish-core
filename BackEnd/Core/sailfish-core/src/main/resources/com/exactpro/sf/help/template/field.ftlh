<link rel="stylesheet" type="text/css" href="../../../resources/static-help.css"/>
<span class="eps-content-value" >
	<h2>Field information:</h2>
	<table class='eps-content-message-table'>
		<#if tag??>
			<tr><td>Tag</td><td>${tag}</td></tr>
		</#if>
		<tr><td>Name</td><td>${name}</td></tr>
		<tr><td>Type</td><td>${type}</td></tr>
	</table>
	<#if valValues?has_content>
		<h3> Valid values:</h3>
		<table class='eps-content-message-table'>
			<#list valValues as valValue>
			<tr>
				<#list valValue?split("=") as val>
					<td>${val}</td>
				</#list>
			</tr>
			</#list>
		</table>
	</#if>
</span>
