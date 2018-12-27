<link rel="stylesheet" type="text/css" href="../../../resources/static-help.css"/>
<span class="eps-content-value" >
	<h2>Message information:</h2>
	<table class='eps-content-message-table'>
		<tr><td>Name</td><td>${name}</td></tr>
		<tr><td>Type</td><td>${type}</td></tr>
	</table>
	<h3>Fields information:</h3>
	<table>
		<tr class="helpTableHeader">
			<#if hasTag><td align="center"> Tag </td></#if>
			<td align="center"> Name </td>
			<td align="center"> Type </td>
		</tr>
		<#list fieldMess as abstrFM>
			<tr>
				<#if hasTag><td>${abstrFM.getTag()!}</td></#if>
				<td>${abstrFM.getName()}</td>
				<td>${abstrFM.getType()!}</td>
			</tr>
		</#list>
	</table>
</span>