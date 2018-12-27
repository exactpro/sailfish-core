<table class="intable">
    <tr>
    <#list table.header as column><#t>
        <th><b>${column}</b></th>
    </#list>
    </tr>
    <#list table.rows as row>
    <tr>
        <#list table.header as column>
        <td>${row[column]!}</td>
        </#list>
    </tr>
    </#list>
</table>