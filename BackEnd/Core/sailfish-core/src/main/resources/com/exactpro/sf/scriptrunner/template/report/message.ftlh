<#import "exception_printer.ftlh" as ep>
<table>
    <#list message.submessages as msg>
    <tr>
        <td>${msg?no_esc}
        <td>
    </tr>
    </#list>
    <#if message.cause??>
    <tr>         
        <td><@ep.print message.cause id/><td>
    </tr>
    </#if>
</table>
