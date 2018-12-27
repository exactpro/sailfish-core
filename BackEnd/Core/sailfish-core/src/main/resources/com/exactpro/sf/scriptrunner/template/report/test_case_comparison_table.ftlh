    <a class="groups" style="display: ${hasHeaders?then('inline', 'none')}" id="togglerCollapseAll${tableId?c}" onclick="treetable_collapseAll('${tableId?c}');">Collapse Groups</a>
    <a class="groups" style="display: none" id="togglerExpandAll${tableId?c}" onclick="treetable_expandAll('${tableId?c}');">Expand Groups</a>
</div>

<table id="${tableId?c}" class="intable">

    <colgroup>
      <col width="200">
      <col width="0*">
    </colgroup>
    
    <thead>
        <tr>
            <th><b>Name</b></th>
            <th><b>Expected</b></th>
            <th><b>Actual</b></th>
            <th><b>Status</b></th>
        </tr>
    </thead>
    
    <#list parameters as parameter>
        
        <#if parameter.header>
        
            <#if parameter.id != "${tableId?c}_0">
        
                <tr id="${parameter.id}">
                    <td class="nested-${parameter.level?c}-group_head">
                        <a class="toggler" id="toggler${parameter.id}" onclick="treetable_toggleRow('${parameter.id}', '${tableId?c}');">${parameter.name!"null"}</a>
                    </td>
                    <td>
                        <pre>${parameter.status???then(parameter.expected!"null","")}</pre>
                    </td>
                    <td>
                        <pre>${parameter.status???then(parameter.actual!"null","")}</pre>
                    </td>
                    <td>
                        <a class="${parameter.statusClass!}" title="${parameter.failReason!}">${parameter.status!}</a>
                    </td>
                </tr>
        
            </#if>
        
        <#else>
        
        <tr id="${parameter.id}" class="${parameter.status?lower_case}-field" >
            <td class="nested-${parameter.level?c}">
                ${parameter.name!"null"}
            </td>
            <td>
                <pre>${parameter.expected!"null"}<#if parameter.precision??> ± ${parameter.precision}</#if><#if parameter.systemPrecision??> % ${parameter.systemPrecision}</#if></pre>
            </td>
            <td>
                <pre>${parameter.actual!"null"}</pre>
            </td>
            <td>
                <a class="${parameter.statusClass!}" title="${parameter.failReason!}">${parameter.status!}</a>
            </td>
        </tr>
        
        </#if>      
    
    </#list>
    
</table>