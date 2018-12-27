<div <#if action> class="eps-node-wrapper ${status_type?lower_case}-action" <#else>class="eps-node-wrapper"</#if> <#if verificationsOrder?has_content>data-verificationsOrder="${verificationsOrder}"</#if>>
    <#if checkPoint??><a name="${checkPoint.getId()?c}" href="#table_${checkPoint.getId()?c}" class="action_checkpoint" onclick="checkMessagesTable()"
                         title="reference to checkpoint"></a></#if>
    <table>
        <tr>
            <td style="padding-left: 0px;">
                 <a id="n${id?c}" <#if hasChild>class="<#if action>action_node<#else >node</#if>" onclick="showhide(${id?c});"</#if> >
                    <span class="${node_class}" <#if msgId?has_content>data-msgId="${msgId?c}"</#if> <#if jsonVerificationResult?has_content>data-jsonVerificationResult="${jsonVerificationResult}"</#if>>${title?no_esc}</span>
                </a>
            </td>
        <#if description??>
            <td style="padding-left: 0px;">
                <div title="${description}" class="node_description">: ${description}</div>
            </td>
        </#if>
        </tr>
    </table>
    <div id="${id?c}" style="display: none;">
