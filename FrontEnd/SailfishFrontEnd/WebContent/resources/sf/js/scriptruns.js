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

(function (){
    try{
        new Date().toLocaleString();
    }catch (e){
        Date.prototype.toLocaleString = function (native) {
            return this.toString();
        }
        console.warn("Date.prototype.toLocaleString was overwritten");
    }
})();

var activeMatrixId = null,
    $resultBlock;

function setActiveMatrixId(id) {
  activeMatrixId = id;
};

function invokeRemoteStart() {
    $('.eps-table-input').blur();
    remoteStartScript([{ name:'id', value: activeMatrixId }]);
};

var filesTotal = -1;
var filesUploaded = 0;
var isUploading = false;
function onUploadStart() {

    if(isUploading) {
        return;
    }
    //file upload callback should call once
    filesTotal = $(".ui-fileupload-preview").size();
    filesUploaded = 0;
    isUploading = true;

    var filesFromJS = $('.ui-fileupload-files').find('tr').length;
    showGrowlMessage([{name: "Severity", value: "INFO"},{name: "Summary", value: "Uploading started"},{name: "Detail", value: filesFromJS + " files are uploading"}]);
}

function onUploadComplete() {
    filesUploaded ++ ;

    if(filesUploaded === filesTotal) {
        showGrowlMessage([{name: "Severity", value: "INFO"},{name: "Summary", value: "Uploading finished"},{name: "Detail", value: "Uploaded " + filesUploaded + " files"}]);
        updateMatricesTable();
        filesUploaded = 0;
        filesTotal = -1;
        isUploading = false;
        PF('addLocalMatrixDialog').hide();

        ////todo This is hack made due with https://code.google.com/p/primefaces/issues/detail?id=7046
        $('body').children("[id$='_menu']").remove();

        return;
    }

}

var datetimeFilterClicked = false;
$(document).ready(function() {

	$(".fileupload-buttonbar").delegate(".start", "click", function(){
  		addLocalMatrixDialog.hide();
	});

	$(".files").delegate("tbody > .template-upload > .start > .ui-button", "hover", function(){
  		$(this).attr("title","Upload");
	});

	$(".files").delegate("tbody > .template-upload > .cancel > .ui-button", "hover", function(){
  		$(this).attr("title","Cancel");
	});

	$("body").on("click", ".eps-scripts-toolbar .ui-menu-parent > a", function(){
        showAddLocalMatrixDialog();
    });

    $("body").on("click", ".eps-scriptruns-toolbar .ui-menu-parent > a", function(){
        $('a[id$=clearAllSafe]').trigger('click')
    });

  $("body").on("mouseup", ".hasDatepicker", onCalendarPreShow);
  $("body").on("mouseup", ".eps-date-matrix-filter .ui-datepicker-trigger", onCalendarShow);

  $("body").on("click", ".eps-filter-testscript-button", function(e) {
    showResultsFilter($(".eps-filter-testscript-button"));
    e.stopPropagation();
  });

  $("body").on("click", ".eps-results-filter", function(e){e.stopPropagation();});
  $("body").on("click", "#ui-datepicker-div", function(e){e.stopPropagation();});
  $("body").on("click", "[id$='filterResultCheckboxMenu_panel']", function(e){e.stopPropagation();});

  $("body").on("click", function(event) {

      if (event.target.className === 'ui-icon ui-icon-circle-triangle-e' || event.target.className === 'ui-icon ui-icon-circle-triangle-w') {
    	  if (datetimeFilterClicked) {
    		  showClearDateBtn();
    	  }
    	  return;
      }

      var filterDiv = $('.eps-results-filter');
      filterDiv.css('display', 'none');
  });

  $("body").on("click", ".eps-clear-date-btn", clearCalendar);

  $(".eps-results-wrapper").delegate(".ui-button", "hover", function( event ) {
    if( event.type === 'mouseenter' )  
        $(this).addClass("ui-state-hover");  
    else
        $(this).removeClass("ui-state-hover");
  });

  if(typeof(Storage) !== "undefined") {
      loadFilters();
    }
  
});

function showClearDateBtn() {
    var elem = $("#ui-datepicker-div");
    var clearBtn = "<button class='ui-button ui-widget ui-state-default eps-clear-date-btn'>clear date</button>";
    elem.prepend(clearBtn);
    return elem;
}

function onCalendarPreShow() {
	datetimeFilterClicked = false;
}

function onCalendarShow() {
	datetimeFilterClicked = true;
	
	setTimeout(function() {
		var elem = showClearDateBtn();
		elem.css('top', '+=32px');
	}, 100);
}

function clearCalendar() {
	var calendarInput = $(".ui-column-customfilter").find('.hasDatepicker');
	calendarInput.val("");
	triggerDateFilter();
	$( ".ui-datepicker-trigger" ).trigger( "click" );
}

function showResultsFilter(button) {
    var posX = button.offset().left + button.width() / 2 - 10
    var posY = button.offset().top + button.height() + 20
    var filterDiv = $('.eps-results-filter');

    filterDiv.css('display', filterDiv.css('display') === 'none' ? 'flex' : 'none');
    filterDiv.css('position', 'fixed');
    filterDiv.css('top', posY)
    filterDiv.css('left', posX)
}

PrimeFaces.widget.DataTable.prototype.showCellEditor = function(c) {
    this.incellClick = true;

    var cell = null,
        $this = this;

    if(c) {
        cell = c;

        //remove contextmenu selection highlight
        if(this.contextMenuCell) {
            this.contextMenuCell.parent().removeClass('ui-state-highlight');
        }
    }
    else {
        cell = this.contextMenuCell;
    }

    if(this.currentCell) {
        $this.saveCell(this.currentCell);
    }

    this.currentCell = cell;

    var cellEditor = cell.children('div.ui-cell-editor'),
        displayContainer = cellEditor.children('div.ui-cell-editor-output'),
        inputContainer = cellEditor.children('div.ui-cell-editor-input'),
        inputs = inputContainer.find(':input:enabled'),
        multi = inputs.length > 1;

    cell.addClass('ui-state-highlight ui-cell-editing');
    displayContainer.hide();
    inputContainer.show();
    inputs.eq(0).focus().select();

    //metadata
    if(multi) {
        var oldValues = [];
        for(var i = 0; i < inputs.length; i++) {
            oldValues.push(inputs.eq(i).val());
        }

        cell.data('multi-edit', true);
        cell.data('old-value', oldValues);
    }
    else {
        cell.data('multi-edit', false);
        cell.data('old-value', inputs.eq(0).val());
    }

    //bind events on demand
    if(!cell.data('edit-events-bound')) {
        cell.data('edit-events-bound', true);

        inputs.on('keyup.datatable-cell', function(e) {
            var keyCode = $.ui.keyCode,
                shiftKey = e.shiftKey,
                key = e.which,
                input = $(this);

            if(key != keyCode.ENTER && key != keyCode.NUMPAD_ENTER) {
                $this.saveCell(cell);
                e.preventDefault();
            }
            //else if(key === keyCode.TAB) {
            //    if(multi) {
            //        var focusIndex = shiftKey ? input.index() - 1 : input.index() + 1;
            //
            //        if(focusIndex < 0 || (focusIndex === inputs.length)) {
            //            $this.tabCell(cell, !shiftKey);
            //        } else {
            //            inputs.eq(focusIndex).focus();
            //        }
            //    }
            //    else {
            //        $this.tabCell(cell, !shiftKey);
            //    }
            //
            //    e.preventDefault();
            //}
        });
    }
};

PrimeFaces.widget.DataTable.prototype.selectRowsInRange = function(row) {

    
   var rows = this.tbody.children(),
    _self = this;
    
   //unselect previously selected rows with shift
    if(this.cursorIndex) {
       var oldCursorIndex = this.cursorIndex,
        
        rowsToUnselect = oldCursorIndex > this.originRowIndex 
           ? rows.slice(this.originRowIndex, oldCursorIndex + 1) 
           : rows.slice(oldCursorIndex, this.originRowIndex + 1);

        rowsToUnselect.each(function(i, item) {
            _self.unselectRow($(item), true);
        });
    }

    //select rows between cursor and origin
    this.cursorIndex = row.index();

    var rowsToSelect = this.cursorIndex > this.originRowIndex 
       ? rows.slice(this.originRowIndex, this.cursorIndex + 1) 
       : rows.slice(this.cursorIndex, this.originRowIndex + 1);
    
    //var i=0;
    rowsToSelect.each(
       function(i, item) {
           if (!(i >= rowsToSelect.length-1)) {
               _self.selectRow($(item), true);
           } else {
               _self.selectRow($(item), false);
           }
           i++;
        }
    );
};

function updateFilter(args) {
    var filterButton = $('#filterTestScriptButton');
    if (applyFilter(args)) {
        filterButton.addClass('ui-state-default');
    } else {
        filterButton.removeClass('ui-state-default');
    }
}

function applyFilter(args) {
    var originalSize = $('.eps-result-block').size();
    var nameFilter = $('.eps-filter-name-result').val();

    var fromArray = $("[id$='dateTimeResultFilterFrom_input']").val().split('/');
    var dateFrom = fromArray.length === 3 ? new Date(fromArray[2], fromArray[1] - 1, fromArray[0]) : new Date(1990, 1, 1) ;

    var toArray = $("[id$='dateTimeResultFilterTo_input']").val().split('/');
    var dateTo = toArray.length === 3 ? new Date(toArray[2], toArray[1] - 1, toArray[0]) : new Date(2100, 1, 1);
    var underscoreCleanupRegex = /_/g;
    var statusCleanupRegex = /\(\S*\)/g;

    $('.eps-result-block').each(function(index, element) {

        var curDateArray = $(element).find('.eps-result-date-time').text().split(" ")[0].split('/');
        var curDate = new Date(curDateArray[2], curDateArray[0] - 1, curDateArray[1]);

        if (!($(element).find('.eps-script-name').text().toLowerCase().includes(nameFilter.toLowerCase()))) {
            $(element).remove();
        }
        else if ((curDate.getTime() < dateFrom.getTime()) || (curDate.getTime() > dateTo.getTime())) {
            $(element).remove();
        }
        else if (!args.statuses.replace(underscoreCleanupRegex, " ").includes('"' + $(element).find('.eps-result-status').text().trim().replace(underscoreCleanupRegex, " ").replace(statusCleanupRegex,"") + '"') &&
        !hasValidExecutionStatus($(element), args.statuses)) {
            $(element).remove();
        }
    });
    return originalSize !== $('.eps-result-block').size()
}

function hasValidExecutionStatus(element, statuses) {
    return (element.hasClass('eps-result-good') && statuses.includes("EXECUTED SUCCESS")
        || ((element.hasClass('eps-result-normal') || element.hasClass('eps-result-cpnormal')) && element.find('.eps-result-status').text().trim() === "EXECUTED" && statuses.includes("EXECUTED WITH ERRORS")));
}

function clearCBPanels() {
    $('.ui-selectcheckboxmenu-panel').remove();
}

function saveFilterText() {

  if(typeof(Storage) === "undefined") {
    // Storage doesn't supported
    return;
  }

  sessionStorage.matrixNameFilter = $('.eps-matrix-name-filter').val();
}

function loadFilters() {
  $('.eps-matrix-name-filter').val(sessionStorage.matrixNameFilter);
  PF('mainTable').filter();
}

function phantomClick() {
    $('#confirmConvertMatrixForm').trigger('click');
}