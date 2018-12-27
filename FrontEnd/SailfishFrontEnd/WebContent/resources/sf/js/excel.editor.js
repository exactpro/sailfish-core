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
var data = null;

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
		
		resizeExcel();
		
		$("#matrixContent").attr("wrap","off");	
		
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
	var $excel = $(".eps-editor-table");
	$excel.css("width", $(".eps-matrix-text-container").width());
	$excel.css("height", $(window).height() - 160);
	try {
		$excel.handsontable('render');
	} catch (e) {
		console.log(printStackTrace(e));
	}
};

function handleComplete(xhr, status, args) { 
	var container = $(".eps-editor-table");

	if(args.text === undefined) {
        return;
    }

	var str =  window.atob(args.text);
	data = $.parseJSON(str);

	if (data === null || str === "[]" || str === "[null]") {
		var empty = [];
		for (var i=0; i < 1; i++) {
			var child = [];
			for (var j=0; j < 10; j++) {
				child.push("");
			}
			empty.push(child);
		}
		data = empty;
	}

	var headersRowNumber = 0;
	for(var i=0;i<data.length;i++) {
	    var isLineHeader = data[i][0]!= null && data[i][0].trim().indexOf('#') === 0 && data[i][0].trim().length > 2;
	    if(isLineHeader) {
	        headersRowNumber = i+1;
	        break;
	    }
	}

	var settings = {
		data: data,
	    startRows: data.length,
	    startCols: data[0].length,
	    rowHeaders: true,
	    colHeaders: true,
	    currentRowClassName: 'currentRow',
        currentColClassName: 'currentCol',
	    minSpareRows: 1,
	    contextMenu: true,
	    fixedRowsTop: headersRowNumber,
	    manualColumnResize: true,
	};

	matrix = container.handsontable(settings);	
		

};

function returnData() {
	return JSON.stringify(data);
};
