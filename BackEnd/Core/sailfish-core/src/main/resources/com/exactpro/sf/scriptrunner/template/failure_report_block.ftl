<#-- Macroses are defined here -->
<#macro processThrowsable cause showhide_num>
    <div class="eps-node-wrapper">
        <a class="node" id="n${showhide_num}" onclick="showhide(${showhide_num});">
            <span class="exceptiontype" > ${cause.description!}</span>
        </a>
        <div id="${showhide_num}" style="display: none;">
            <table>
                <#list cause.stackTrace! as element>
                    <tr><td>${element}</td></tr>
                </#list>
                <tr><td>
                    <#if cause.cause??> 
                        <@processThrowsable cause.cause!, showhide_num + 1 />
                    </#if>
                </td></tr>
            </table>
        </div>
    </div>
</#macro>

<#-- Template starts here -->
<#import "copyright.ftl" as copyright>

<#assign showhide_num = 0>

<#compress>
<tr><td>
    <div class="eps-node-wrapper">
        <a class="node" id="n${showhide_num}" onclick="showhide(${showhide_num});">
            <span class="statustype" > Status</span>
        </a>
        <div id="${showhide_num}" style="display: none;">
            <table class="intable">
                <tr><td>Status</td><td>FAILED</td></tr>
                <tr><td>Description</td><td>${exceptionType.description!}</td></tr>
                <tr><td>Exception</td><td>
                
                <@processThrowsable exceptionType!, showhide_num + 1 />
                
                </td></tr>
            </table>
        </div>
    </div>
</td></tr>
</#compress>