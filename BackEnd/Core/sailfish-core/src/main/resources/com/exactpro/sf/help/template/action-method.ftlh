<link rel="stylesheet" type="text/css" href="../../../resources/static-help.css"/>
<div class="eps-content-value">
    <h2>Method information:</h2>
    <table class='eps-content-message-table'>
	<tr><td>Return type</td><td>${returnType}</td></tr>
	<tr><td>Method name</td><td>${name}</td></tr>
	<tr><td>Parameters</td><td>${parameters}</td></tr>
	<tr><td>URI</td><td>${uri}</td></tr>
	</table>
	<#if description??>
	<br/>
	${description?no_esc}
	</#if>
    <br/>
		<#if commonColumns?has_content>
		<table class='eps-content-message-table'>
			<tr><td>Common columns:</td></tr>
			<#list commonColumns as commonColumn>
				<tr><td>${commonColumn.value().getName()}<#if commonColumn.required() || commonColumn.deprecated()>(<#if commonColumn.required()>required</#if><#if commonColumn.deprecated()>${commonColumn.required()?then(", deprecated","deprecated")}</#if>)</#if></td></tr>
			</#list>
		</#if>
		</table>
   		 <br/>
		<#if customColumns?has_content>
        <table class='eps-content-message-table'>
        <tr><td>Custom columns:</td></tr>
			<#list customColumns as customColumn>
				<tr><td>${customColumn.value()}
					<#if customColumn.required() || customColumn.deprecated()>
						(<#if customColumn.required()>required</#if><#if customColumn.deprecated()>${customColumn.required()?then(", deprecated","deprecated")}</#if>)
					</#if>
                </td></tr>
			</#list>
        </table>
		</#if>
</div>
