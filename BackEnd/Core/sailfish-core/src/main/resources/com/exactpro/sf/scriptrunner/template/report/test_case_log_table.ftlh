<#import "exception_printer.ftlh" as ep>
<table class="intable" width="95%">
	<thead>
    	<tr>
	    <#list headers as header><#t>
	    	<#switch header?index>
	            <#case 0>
	            <th style="width: 12%"></th>
	            <#break>
	            <#case 1>
	            <th style="width: 5%"></th>
	            <#break>
	            <#case 2>
	            <th style="width: 10%"></th>
	            <#break>
	            <#case 3>
	            <th style="width: 30%"></th>
	            <#break>
	            <#case 4>
	            <th style="width: 23%"></th>
	            <#break>
	            <#case 5>
	            <th style="width: 20%"></th>
	            <#break>
	        </#switch>
	    </#list>
	    </tr>
    </thead>
    <tbody>
	    <#setting datetime_format="yyyy-MM-dd HH:mm:ss.SSS">
	    <#setting time_zone="UTC">
	    <#list rows as row>
	    <tr>
	        <td>${row.timestamp?number_to_datetime}</td>
	        <td>${row.level}</td>
	        <td>${row.thread}</td>
	        <td>${row.clazz}</td>
	        <td>${row.message!"null"}</td>
	        <td>
	        <#if row.ex??>
	            <#assign new_id = (id + row?index * 10)/>
	            <@ep.print row.ex new_id/>
	        </#if>
	        </td>
	    </tr>
	    </#list>
    </tbody>
</table>
<table id="logs-header-fixed" class="intable" style="table-layout:fixed;"></table>