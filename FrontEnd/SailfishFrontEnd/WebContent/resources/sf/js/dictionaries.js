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

var uiHack = {};

(function() {

	var filesScrollPos  = {};
	var mesScrollPos	= {};
	var fieldsScrollPos = {};

	uiHack.saveFilesScrollPos  = createSaveScrollFunction("#filesTable > .ui-datatable-tablewrapper", filesScrollPos);
	uiHack.getFilesScrollPos   = createGetScrollFunction ("#filesTable > .ui-datatable-tablewrapper", filesScrollPos);

	uiHack.saveMesScrollPos	   = createSaveScrollFunction("#dictionaryTree", mesScrollPos);
	uiHack.getMesScrollPos	   = createGetScrollFunction ("#dictionaryTree", mesScrollPos);

	uiHack.saveFieldsScrollPos = createSaveScrollFunction("#fieldsTable", fieldsScrollPos);
	uiHack.getFieldsScrollPos  = createGetScrollFunction ("#fieldsTable", fieldsScrollPos);

    uiHack.hackDropCacheOverlayPanel = createHackDropCacheOverlayPanel();
    uiHack.hideDropCacheOverlayPanel = createHideDropCacheOverlayPanel();
})();

function createSaveScrollFunction(id, scrollVar) {
	function save() {
		scrollVar.scroll = $(id).scrollTop();
	}
	return save;
}

function createGetScrollFunction(id, scrollVar) {
	function get() {
		$(id).scrollTop(scrollVar.scroll);
	}
	return get;
}

function toggleMessageBody(nodeSelector) {

	// Disable opportunity to click on expander during animation
	var expanderLinksSelector; // = nodeSelector + ' > .field-info > .message-expander > .expander-links';

	toggleBlock(nodeSelector, expanderLinksSelector, nodeSelector + ' > .tree-message-body-for-slide', nodeSelector + ' > .tree-message-body-for-slide > .tree-message-body');

};

function toggleProperties(nodeSelector) {

	// Disable opportunity to click on field type during animation
	var fieldLinksSelector; // = nodeSelector.replace('#p', '#inside') + ' > .tree-node > .field-info > .field-type > .field-links';

	toggleBlock(nodeSelector, fieldLinksSelector, nodeSelector, nodeSelector + ' > .tree-properties-wrapper');

}

function toggleBlock(nodeSelector, linksSelector, slideSelector, emptySelector) {

	$(nodeSelector).toggleClass('collapsed');

	if ($(nodeSelector).hasClass('collapsed')) {

		$(slideSelector).css('display', 'none');
		$(emptySelector).empty();

	} else {
		$(slideSelector).css('display', 'block');
	}

	/** Animation **/

	/*$(linksSelector).css('display', 'none');

	$(slideSelector).slideToggle(400, function() {
		$(linksSelector).css('display', 'block');
		if ($(nodeSelector).hasClass('collapsed')) {
			$(emptySelector).empty();
		}
	});*/
}

function showPropButtons(nodeSelector) {
	if (!$(nodeSelector).hasClass('uncollapsed')) {
		$(nodeSelector).addClass('uncollapsed');
		$(nodeSelector).slideDown(400);
	}
}

function scrollToRightSelected() {
	$('#fieldsTable').scrollTop($('#fieldsTable').scrollTop() +
								$('.selected-right-node').position().top -
								$('#fieldsTable').height()/2 +
								$('.selected-right-node').height()/2);
}

function scrollToLeftSelected() {
	$('#dictionaryTree').scrollTop($('#dictionaryTree').scrollTop() +
								   $('.selected-left-node').parent().parent().position().top -
								   $('#dictionaryTree').height()/2 + 20);
}

function disableView() {
	$('#dictionaryPanels').css('pointer-events', 'none');
	$('#dictionaryPanels').css('opacity', '0.3');
	//$('#dictionaryPanels').animate({ opacity: 0.3 }, 500);
}

function enableView() {
	$('#dictionaryPanels').css('pointer-events', 'auto');
	$('#dictionaryPanels').css('opacity', '1');
}

function createHideDropCacheOverlayPanel() {
    function hide() {
        $('#dropCacheOverlayPanel').removeClass("ui-overlay-visible");
        $('#dropCacheOverlayPanel').addClass("ui-overlay-hidden");
        $('#dropCacheOverlayPanel').css("visibility", "hidden");
    }
    return hide;
}

function createHackDropCacheOverlayPanel() {
    function hack() {
        var attr = $("#dropCacheOverlayPanel").attr("onmouseleave");
        if(attr === undefined || attr === false) {
            $('#dropCacheOverlayPanel').attr("onmouseleave", "uiHack.hideDropCacheOverlayPanel()");
        }
    }
    return hack;
}
