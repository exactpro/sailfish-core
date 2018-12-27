<div class='eps-table-wrapper'>
    <div style='text-align: center; padding: 5px; position: relative'>
    <div style='right: 40px; position: absolute'>
		<a class="checkpointgroups" style="display: ${table.hasCheckPoints?then('inline', 'none')}" id="togglerCollapseAllCheckPoints" onclick="expandCollapseAllCheckPoints('false');">Collapse CheckPoints</a>
		<a class="checkpointgroups" style="display: none" id="togglerExpandAllCheckPoints" onclick="expandCollapseAllCheckPoints('true');">Expand CheckPoints</a>
	</div>
	</div>
<p>
<div id="sticky-anchor"></div>
<table id="messageTable" class="intable" style="table-layout:fixed;">
    <thead class="messagesHeader">
        <tr>
        <#list table.header as column><#t>
            <#switch column?index>
            <#case 0>
            <th style="border-right-color: #ccc !important; box-sizing: border-box; width: 10%"></th>
            <#break>
            <#case 4>
            <th style="border-right-color: #ccc !important; width: 50%"><input name="${column}" onkeyup="filterMessages(this)" placeholder=" filter by ${column?lower_case}..." style="width: 95%"></th>
            <#break>
            <#case 5>
            <th style="text-align: right; box-sizing: border-box; width: 120px"><label><input name="${column}" onchange="toggleRaw(this)" type="checkbox">Show</label></th>
            <#break>
            <#case 6>
            <#break>
            <#case 7>
            <#break>
            <#case 8>
            <#break>
            <#case 9>
            <#break>
            <#default>
            <th style="border-right-color: #ccc !important; width: 10%"><input name="${column}" onkeyup="filterMessages(this)" placeholder=" filter by ${column?lower_case}..." style="width: 95%"></th>
            </#switch>
        </#list>
        </tr>
        <tr>
        <#list table.header as column>
        	<#if !(column?index >= 6 && column?index <= 9)>
            	<th style="border-right-color: #ccc !important; box-sizing: border-box;"><b>${column}</b></th>
            </#if>
        </#list>
        </tr>
    </thead>
    <tbody>
    <#list table.rows as row>
        <tr id="row_msg_${row['Id']}" class="messageRow <#if row['UnderCheckPoint']?has_content>UnderCheckPoint${row['UnderCheckPoint']}</#if>">
        <#if row['CheckPoint'] == 'true'>
        	<#list table.header as column>
        		<#switch column?index>
            	<#case 1>
            		<td class="checkPointTD" colspan="3">
                        <a name="table_${row['Id']!}" class="toggler" id="toggler${row['Id']!}" onclick="toggleCheckpoint('toggler${row['Id']!}');">
            			    <#assign checkPointDesc = row['Content']?remove_beginning('Description=')>
            				${row[column]!}<span class="node_description"><#if checkPointDesc != "null">: ${checkPointDesc}</#if></span>
            			</a>
            		</td>
            	<#break>
            	<#case 2>
            	<#break>
            	<#case 3>
            	<#break>
            	<#case 4>
            	<#break>
            	<#case 5>
            		<td class="fake" style="display: none"></td>
            		<td class="${column}" style="display: none"></td>
            	<#break>
            	<#case 8>
                <#break>
            	<#default>
            		<td class="checkPointTD"></td>
            	</#switch>
        	</#list>
        <#else>
        <#list table.header as column>
            <#switch column?index>
            <#case 0>
            <td class="${column}">${row[column]!}</td>
            <#break>
            <#case 4>
            <td class="${column}">
                <pre>${row[column]!}</pre>
                <div id="jsn_cont_${row['Id']}" style="display: none">${row['ContentJson']!}</div>
            </td>
            <#break>
            <#case 5>
            <td class="fake">...</td>
            <td class="${column}" style="display:none;">
            <#list row[column]!?split("\n") as line>
                <pre>${line?trim}</pre>
            </#list>
            </td>
            <#break>
            <#case 6>
            <#break>
            <#case 7>
            <#break>
            <#case 8>
            <#break>
            <#case 9>
            <#break>
            <#default>
            <td class="${column}">${row[column]!}</td>
            </#switch>
        </#list>
        </#if>
        </tr>
    </#list>
    </tbody>
</table>
<table id="messages-header-fixed" class="intable" style="table-layout:fixed;"></table>
</div>