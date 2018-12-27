<#macro print cause id>
<div class="eps-node-wrapper">
    <a class="node" id="n${id?c}" onclick="showhideException(${id?c});">
    	<span class="exceptiontype" id="exShortHeader${id?c}" style="display: block;">${cause.message?has_content?then(cause.message, "Blank message, please contact the developers")}</span>
        <span class="exceptiontype" id="exFullHeader${id?c}" style="display: none;">${cause.class}: ${cause.message?has_content?then(cause.message, "Blank message, please contact the developers")}</span>
    </a>
    <div id="${id?c}" style="display: none;">
        <table>
            <#list cause.stackTrace! as s>
            <tr><td>${s.className}.${s.methodName}(${s.fileName!"Unknown Source"}:${s.lineNumber?c})</td></tr>
            </#list>
        </table>
    </div>    
    <#if cause.cause??> 
        <@print cause.cause, id + 1/>
    </#if>      
</div>
</#macro>