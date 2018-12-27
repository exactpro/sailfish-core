/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
(function() {
	var context = getContextPath() + "/",
		channel = new Channel(context + "polling", false),	
		enabledCurrency = [], 
		tooltip = null, 
		warning = null, 
		disabled = null,
		resultsContainer = null;
	
	function channelConnect() {
		channel.connect();
	};
	
	function sendUpdateRequest() {
		channel.sendRequest(MessageFactory.get().create("ScriptrunnerUpdateRequest"));
		channel.sendRequest(MessageFactory.get().create("MatrixUpdateRequest"));
	};
	
	$(document).ready(function() {
		
		$("#dataTable").attr("wrap","off");
		
		resizeExcel();	
		
		$(window).resize(function() {
			resizeExcel();
		});
		
		$.ajaxSetup({
			cache : false
		});
		
		channel.addHandler("onScriptrunnerUpdateResponse", function(val) {
			console.log(val);
			if (val["scriptRunId"] == $("#lastScriptRun").val()) {
				var $resultById = $("#scriptRunResult");
				$resultById.html(val["formattedString"]);
			};
		});
	
		channel.addHandler("onMatrixUpdateResponse", function(val) {
			console.log(val);
			updateMatricesTable();
		});
	
		channel.addHandler("onConnectionStatus", function(val) {
			console.log(val);
		});
	
		channel.addHandler("onCloseChannel", function(val) {
			console.log(val);
		});	
	
		channel.addHandler("onNotifyResponse", function(val) {
			console.log(val);
		});
	
		channelConnect();
		sendUpdateRequest();
		
	});
})();

function toggleDetails(block_id) {
	$("#" + block_id).toggle();
};

function resizeExcel() {
	var $excel = $("#dataTable");	
	//$excel.css("overflow", "scroll");
	$excel.css("width", $(".eps-matrix-text-container").width() - 5);
	$excel.css("height", $(window).height() - 160);
	try {
		$excel.handsontable('render');
	} catch (e) {
		console.log(printStackTrace(e));
	}
};

function handleComplete(xhr, status, args) { 
	var str =  window.atob(args.text);
	$("#dataTable").val(str);
};

function returnData() {
	return $("#dataTable").val(); 
};