<div class='eps-table-wrapper'>
    <div style='text-align: center; padding: 5px; position: relative'>
        <a class="groups" style="display: ${hasHeaders?then('inline', 'none')}" id="togglerCollapseAll${tableId?c}" onclick="treetable_collapseAll('${tableId?c}');">Collapse Groups</a>
        <a class="groups" style="display: none" id="togglerExpandAll${tableId?c}" onclick="treetable_expandAll('${tableId?c}');">Expand Groups</a>
    </div>
<p>
<table id="${tableId?c}" class="intable">

    <colgroup>
      <col width="200">
      <col width="0*">
    </colgroup>
    
    <tr>
        <td colspan="2" align="center">
               <pre>${message_name}</pre>
        </td>
    </tr>
    

    <#list parameters as parameter>
    <#if parameter.header>
    
        <#if parameter.id != "${tableId?c}_0">
        
            <tr id="${parameter.id}">
                <td class="nested-${parameter.level?c}-group_head">
                    <a class="toggler" id="toggler${parameter.id}" onclick="treetable_toggleRow('${parameter.id}', '${tableId?c}');"><pre>${parameter.name}</pre></a>
                </td>
                <td>
                </td>
            </tr>
        </#if>
    
    <#else>
    
        <tr id="${parameter.id}">
            <td class="nested-${parameter.level?c}">
                <pre>${parameter.name}</pre>
            </td>
            <td>
                <#if parameter.value??><pre>${parameter.value}</pre></#if>
            </td>
        </tr>
        
    </#if>
    
    </#list>
</table>
</div>