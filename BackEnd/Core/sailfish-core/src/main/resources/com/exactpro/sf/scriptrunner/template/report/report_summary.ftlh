<div id="summary-wrapper">            
    <div id="summary">
    <#setting datetime_format="yyyy-MM-dd HH:mm:ss.SSS">
    <#setting time_zone="UTC">
    <table>
        <tr>
            <td colspan="2" style="text-align: center; padding: 0 0 0.7em 0;">
                <img src="resources/SF_white.jpg" alt="SAILFISH LOGO" border="0" align="center">
            </td>
        </tr><tr>
            <td><strong>Host:</strong></td>
            <td>${host}</td>
        </tr><tr>
            <td><strong>User:</strong></td>
            <td>${user}</td>
        </tr><tr>
            <td><strong>Execution Time:</strong></td>
            <td>${(duration / 1000.0)?c}s</td>
        </tr><tr>
            <td><strong>ScriptRun Id:</strong></td>
            <td>${id?c}</td>
        </tr><tr>
            <td><strong>Date:</strong></td>
            <td>${date?datetime}</td>
        </tr><tr>
        	<td><strong>Version:</strong></td>
        	<td>${version}</td>
        </tr><tr>
            <td><strong>Comparison Precision:</strong></td>
            <td>${precision}</td>
        </tr>
<#if plugins_visible>
    	<tr>
    		<td><strong>Plugins:</strong></td>
    		<td>${plugins}</td>
    	</tr>
</#if>
        <tr>
            <td><strong>TEST CASES:</strong></td>
            <td>${tc_total?c}</td>
        </tr><tr>
            <td class="statuspassed nested-2"><strong>PASSED:</strong></td>
            <td>${tc_passed?c}</td>
        </tr><tr>
            <td class="statuscondpassed nested-2"><strong>CONDITIONALLY PASSED:</strong></td>
            <td>${tc_cp?c}</td>
        </tr><tr>
            <td class="statusfailed nested-2"><strong>FAILED:</strong></td>
            <td>${tc_failed?c}</td>
        </tr><tr>
            <td class="nested-2"><strong>N/A:</strong></td>
            <td>${tc_notapp?c}</td>
        </tr>
    </table>
    </div>
</div>