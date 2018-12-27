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
function hideAjax() {
    $('#loadingTreeStatus').hide();
}

function showAjax() {
    $('#loadingTreeStatus').show();
}

function hideBar() {
    if (PF('pluginsProgress') != undefined) {
        PF('pluginsProgress').cancel();
    }
    $('#pluginsProgress').hide();
}

function showBar() {
    $('#pluginsProgress').show();
    if (PF('pluginsProgress') != undefined) {
        PF('pluginsProgress').start();
    }
}

function scrollActionsTree() {
    var selected = $('.eps-help-tree').find('li.ui-treenode-selected');
    if (selected.length) {
        var div = $('.eps-forest-container');
        var offset = $(selected).position().top;
        $(div).scrollTop(offset);
    }
}

function createHiddenTarget(targetId, toCopy) {
    
    exTargetId = "#"+targetId;
    
    if (!$(exTargetId).length) {
        $("<textarea />").css("position", "absolute").css("left", "-9999px").attr("id", targetId).appendTo(".eps-container");
    }
    
    $(exTargetId).val(toCopy);
}

function copyToClipboard(toCopy1, toCopy2, toCopy3, toCopy4) {
    createHiddenTarget("_hiddenCopyText1", toCopy1);
    createHiddenTarget("_hiddenCopyText2", toCopy2);
    createHiddenTarget("_hiddenCopyText3", toCopy3);
    createHiddenTarget("_hiddenCopyText4", toCopy4);
}

function copyProcess(targetId) {
    $(targetId).focus();
    $(targetId)[0].setSelectionRange(0, $(targetId).val().length);
    document.execCommand("copy");
    $(targetId).val("");
    showCopiedMessage();
}

$(document).ready(function() {

    documentReady();
    
    $('body').on("click", "#copyNewColumns", function (e) {
        copyProcess("#_hiddenCopyText1");
    });
    
    $('body').on("click", "#copyAllHeader", function (e) {
        copyProcess("#_hiddenCopyText2");
    });
    
    $('body').on("click", "#copyIncompleteStructure", function (e) {
        copyProcess("#_hiddenCopyText3");
    });
    
    $('body').on("click", "#copyAllStructure", function (e) {
        copyProcess("#_hiddenCopyText4");
    });
    
    $('body').on("click", ".eps-guide-panel a:not(.eps-redmine-attachment-link)", function(e){
        var link = e.target.getAttribute('href');

        loadRedmineWikiPage([{name: "pageName", value: link}]);

        e.preventDefault();
        return false;
    });

    $('body').on("click", ".eps-wiki-content a:not(.eps-redmine-attachment-link)", function(e){
        var link = e.target.getAttribute('href');

        loadRedmineWikiPage([{name: "pageName", value: link}]);

        e.preventDefault();
        return false;
    });
    
    $("body").on("click", "#headerProblemMenuBar .copyHeaderSubmenu > a", function(){
        $('.copyHeaderSubmenu > ul a').first().trigger('click')
    });
    
    $("body").on("click", "#headerProblemMenuBar .copyStructureSubmenu > a", function(){
        $('.copyStructureSubmenu > ul a').first().trigger('click')
    });

    initTree();
});
