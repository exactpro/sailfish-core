<table class="intable" style="width=95%">
    <thead>
        <tr>
        <#list table.header as column><#t>
            <th style="border-right-color: #ccc !important; box-sizing: border-box;"><b>${column}</b></th>
        </#list>
        </tr>
    </thead>
    <tbody>
    <#list table.rows as row>
        <tr>
        <#list table.header as column>
            <td class="eps-alert eps-alert-${column}">${row[column]!}</td>
        </#list>
        </tr>
    </#list>
    </tbody>
</table>