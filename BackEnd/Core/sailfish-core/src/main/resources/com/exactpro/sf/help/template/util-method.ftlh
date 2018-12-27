<link rel="stylesheet" type="text/css" href="../../resources/static-help.css"/>
<div class="eps-content-value">
    <h2>Method information:</h2>
    <table class='eps-content-message-table'>
        <tr><td>Return type</td><td>${returnType}</td></tr>
        <tr><td>Method name</td><td>${name}</td></tr>
        <tr><td>Parameters</td><td>${parameters}</td></tr>
        <tr><td>URI</td><td>${uri}</td></tr>
    </table>
        <br/>
    <#if description??>
    ${description?no_esc}
    <#else>
        This utility not described yet. Please kick developers to add description for this utility.
    </#if>
        <br/>
</div>

