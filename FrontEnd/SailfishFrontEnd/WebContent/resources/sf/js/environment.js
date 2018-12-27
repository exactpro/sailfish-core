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
$(document).ready(function() {

  $(".fileupload-buttonbar").delegate(".start", "click", function(){
      uploadDialog.hide();
  });

  $(".files").delegate("tbody > .template-upload > .start > .ui-button", "hover", function(){
      $(this).attr("title","Upload");
  });

  $(".files").delegate("tbody > .template-upload > .cancel > .ui-button", "hover", function(){
      $(this).attr("title","Cancel");
  });

  removeOverlays();

  //saving filters
    var timeFilterValue = document.getElementById('typeFilter').innerHTML;
    $('.eps-service-type-filter').val(timeFilterValue);
    $('body').on('keyup', '.eps-service-type-filter', function(e){
      var t = document.getElementsByClassName('eps-service-type-filter')[0].value;
      document.getElementById('typeFilter').innerHTML = t;
      saveFilter([{name: 'filterColumn', value: 'type'}, {name: 'filterValue', value: t}]);
    });
    var nameFilterValue = document.getElementById('nameFilter').innerHTML;
    $('.eps-service-name-filter').val(nameFilterValue);
    $('body').on('keyup', '.eps-service-name-filter', function(e){
     var t = document.getElementsByClassName('eps-service-name-filter')[0].value;
     document.getElementById('nameFilter').innerHTML = t;
     saveFilter([{name: 'filterColumn', value: 'name'}, {name: 'filterValue', value: t}]);
    });

    loadSort();

    $('body').on('click', '.eps-service-start-button, .eps-service-stop-button, #startButton, #stopButton', saveServiceTableScroll);
  
});

var serviceTableOffset = 0;

function removeFilters() {
    $('.eps-service-name-filter').val("");
    $('.eps-service-type-filter').val("");
}

var serviceTableOffset = 0;

function saveServiceTableScroll() {
    serviceTableOffset = $('#table_data').scrollTop();
}

function restoreTableScroll() {
    if(serviceTableOffset !== 0) {
        $('#table_data').scrollTop(serviceTableOffset);
    }
}

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

}

function onUploadComplete() {
    filesUploaded ++ ;

    if(filesUploaded === filesTotal) {
        filesUploaded = 0;
        filesTotal = -1;
        isUploading = false;
        PF('uploadDialog').hide();
        PF('environmentsDialogWidget').hide();
        setRespectFileName([{name:'respectFileName', value:'false'}]);

        return;
    }

}

function removeOverlays() {
    $('.ui-overlaypanel').remove();
}

var selectedServiceName;

function displayServiceOptionsOverlay(event, serviceName, me) {
	
	selectedServiceName = serviceName;
	
	var me = $(me);
	var offset = me.offset();
	offset.top = offset.top + me.height();
	
	$('#service-options-overlay').show();
	$('#service-options-overlay').offset(offset);
	
	event.stopPropagation();
	
};

$(document).click(function(event) {
	if(! $(event.target).hasClass('eps-options-button') ) {
		$('#service-options-overlay').hide();
	}
});


