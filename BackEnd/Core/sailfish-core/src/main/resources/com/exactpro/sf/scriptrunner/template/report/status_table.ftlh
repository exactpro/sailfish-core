<#import "exception_printer.ftlh" as ep>
<table class="intable">
    <tr>
        <td>Status</td>
        <td>${status}</td>
    </tr>
    <#if description?has_content>
    <tr>
        <td>Description</td>
        <td>${description!}</td>
    </tr>
    </#if>
    <#if exception??>
    <tr>
        <td>Exception</td>
        <td><@ep.print exception id/></td>
    </tr>
    </#if>
</table>