<tr class="<#if status?has_content>${status?lower_case}-tc<#else>passed-tc</#if>">
    <td>
        <p>
            <a href="${fileName}.html"
                class="
                    <#if status?has_content>${status?lower_case}<#else>passed</#if>
                ">

                ${name}
                <#if status?has_content>(${status})</#if>
                <#if duration??>[${(duration / 1000.0)?c}s]</#if>
                <#if description??> : ${description}</#if>
            </a>
        </p>
    </td>
</tr>
