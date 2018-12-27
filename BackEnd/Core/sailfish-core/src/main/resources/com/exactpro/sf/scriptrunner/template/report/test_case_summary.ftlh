<div style="display:table-header-group;">
    <#setting datetime_format="yyyy-MM-dd HH:mm:ss.SSS">
    <#setting time_zone="UTC">
    <table id="casedesc">
        <tr>
            <td><strong>Start time:</strong></td>
            <td>${start_time?datetime}</td>
        </tr>
        <tr>
            <td><strong>Finish time:</strong></td>
            <td>${finish_time?datetime}</td>
        </tr>
        <#if id??>
        	<tr>
	            <td><strong>Id:</strong></td>
	            <td>${id}</td>
	        </tr>
        </#if>
        <tr>
            <td><strong>Hash:</strong></td>
            <td>${hash?c}</td>
        </tr>
    </table>
</div>