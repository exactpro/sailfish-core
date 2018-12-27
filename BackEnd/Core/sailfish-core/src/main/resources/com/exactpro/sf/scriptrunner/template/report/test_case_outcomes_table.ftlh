<#list outcomes.groupOrder as group>
<table class="intable" style="margin: 1.5em 0;">
    <tr>
        <td class="outcome_head" colspan="4">Outcome: ${group}</td>
    </tr>
    <tr style="text-align:center;">
        <td>Case</td>
        <td>Passed count</td>
        <td>Conditionally passed count</td>
        <td>Failed count</td>
    </tr>
    <#list outcomes.definedOutcomes[group]! as name>
    <tr>
        <td>${name}</td>
        <td>${outcomes.getPassedCount(group, name)}</td>
        <td>${outcomes.getConditionallyPassedCount(group, name)}</td>
        <td>${outcomes.getFailedCount(group, name)}</td>
    </tr>
    </#list>
</table>
</#list>