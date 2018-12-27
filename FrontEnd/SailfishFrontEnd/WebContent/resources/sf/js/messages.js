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
function onBlur() {
    var area = $('.eps-messages-query-input');
    $(area).addClass("eps-height32");
}

function onFocus() {
    var area = $('.eps-messages-query-input');
    $(area).removeClass("eps-height32");
}

function showTogglerTooltip(){
	var toggler = $('.ui-row-toggler');
	$(toggler).each(function(index, elem){
		$(elem).prop('title', 'Show/hide raw message');
	});
}

function removeFilters() {
    $('.eps-message-timestamp-filter').val("");
    $('.eps-message-name-filter').val("");
    $('.eps-message-from-filter').val("");
    $('.eps-message-to-filter').val("");
    $('.eps-message-content-filter').val("");
}

function wrapContent() {

    var noHighlight = !$('.eps-highlight-content input').is(":checked");
    if(noHighlight) {
        return;
    }

    $('td.eps-message-content-column').each(function(rowIndex, contentRow){

        if($(contentRow).find('.eps-content-wrapper').length) {
            return;
        }

        let mainContent = contentRow.childNodes[0]
        var trimContent = mainContent.textContent.trim();
        //TODO split content at this place
        var spans = parseMessages(trimContent).join('');
        var fullHtml = "<div class=\"eps-content-wrapper\">" + spans + "</div>";
        $(mainContent).replaceWith(fullHtml);
    });
}

function parseMessages(rawContent) {
    var messages = [];

    var tags = rawContent.split(";");

    tags.forEach(function (tag) {
        var equalIndex = tag.indexOf('=');
        var bracketIndex = tag.indexOf('(');
        var index = bracketIndex;
        if (equalIndex < bracketIndex || bracketIndex === -1) {
            index = equalIndex;
        }
        var tagName = $("<div>").text(tag.substring(0, index)).html();
        var htmlTag = "<span class=\"eps-msg-tag eps-" + tagName.toLowerCase().trim() + "\">" + tag + "; </span>";
        messages.push(htmlTag);
    });

    if (!messages.length) {
        messages.push(rawContent);
    }

    return messages;
}

function onHoverContent(event) {
    $('.eps-msg-tag').removeClass('eps-highlighted-tag');
    var className = event.target.classList[1];
    var elems = $('.' + className);
    $(elems).addClass('eps-highlighted-tag');
}

function onMouseOutContent(event) {
    $('.eps-msg-tag').removeClass('eps-highlighted-tag');
}

$(document).ready(function () {

    //unknowing resize problem
    window.onresize = refresh;
  //called when key is pressed in limit textbox
  $("#countInput").keypress(function (e) {
     //if the letter is not digit then don't type anything
     if (e.which != 8 && e.which != 0 && (e.which < 48 || e.which > 57)) {
        return false;
     }
   });

   $('body').on('click','.eps-msg-tag', function(e) {
        onHoverContent(e);
        return false;
   });
   $('body').on('click', function(e) {
        onMouseOutContent(e);
   });

   //saving rowsPerPage value
   var rowsPerPage = document.getElementById('rowsPerPage').innerHTML;
   $('#table_rppDD').val(rowsPerPage);
   $('body').on('change', '#table_rppDD', function(e){
        var rows = $('#table_rppDD').val();
        document.getElementById('rowsPerPage').innerHTML = rows;
        setRowsPerPage([{name: 'rowsPerPage', value: rows}]);
   });

   //saving filters
    var timeFilterValue = document.getElementById('timeFilter').innerHTML;
    $('.eps-message-timestamp-filter').val(timeFilterValue);
    $('body').on('keyup', '.eps-message-timestamp-filter', function(e){
        var t = document.getElementsByClassName('eps-message-timestamp-filter')[0].value;
        document.getElementById('timeFilter').innerHTML = t;
        saveFilter([{name: 'filterColumn', value: 'time'}, {name: 'filterValue', value: t}]);
    });
    var nameFilterValue = document.getElementById('nameFilter').innerHTML;
    $('.eps-message-name-filter').val(nameFilterValue);
    $('body').on('keyup', '.eps-message-name-filter', function(e){
       var t = document.getElementsByClassName('eps-message-name-filter')[0].value;
       document.getElementById('nameFilter').innerHTML = t;
       saveFilter([{name: 'filterColumn', value: 'name'}, {name: 'filterValue', value: t}]);
    });
    var fromFilterValue = document.getElementById('fromFilter').innerHTML;
    $('.eps-message-from-filter').val(fromFilterValue);
    $('body').on('keyup', '.eps-message-from-filter', function(e){
       var t = document.getElementsByClassName('eps-message-from-filter')[0].value;
       document.getElementById('fromFilter').innerHTML = t;
       saveFilter([{name: 'filterColumn', value: 'from'}, {name: 'filterValue', value: t}]);
    });
    var toFilterValue = document.getElementById('toFilter').innerHTML;
    $('.eps-message-to-filter').val(toFilterValue);
    $('body').on('keyup', '.eps-message-to-filter', function(e){
       var t = document.getElementsByClassName('eps-message-to-filter')[0].value;
       document.getElementById('toFilter').innerHTML = t;
       saveFilter([{name: 'filterColumn', value: 'to'}, {name: 'filterValue', value: t}]);
    });
    var contentFilterValue = document.getElementById('contentFilter').innerHTML;
    $('.eps-message-content-filter').val(contentFilterValue);
    $('body').on('keyup', '.eps-message-content-filter', function(e){
       var t = document.getElementsByClassName('eps-message-content-filter')[0].value;
       document.getElementById('contentFilter').innerHTML = t;
       saveFilter([{name: 'filterColumn', value: 'content'}, {name: 'filterValue', value: t}]);
    });

    refresh();
    setTimeout(wrapContent, 500);
});

function copyPrintableMessageToClipboard(toCopy) {

    const el = document.createElement('textarea');
    el.value = toCopy;
    el.setAttribute('readonly', '');
    el.style.position = 'absolute';
    document.body.appendChild(el);
    el.select();
    document.execCommand('copy');
    document.body.removeChild(el);
}
