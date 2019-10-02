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

// FIXME: This whole file should be refactored

let isCtrlPressed;
const selectColor = '#b2dfe5';
const lastBackgroundColor = 'last-background-color';
const background = 'background-color';
const resultBlock = '.eps-result-block';
const ctrlCode = 17;
const testScriptBlockPrefix = 'eps-result-';
const checked = 'checked';
const checkboxType = 'checkbox';
const labelTag = 'LABEL';
const linkTag = 'A';
const selectModes = Object.freeze({"all":1, "none":2, "some":3});
const scriptsOmittedCardId = "omitted-scripts-card";


$(document).ready(function() {
    $('body').on("keydown", function(e){
        if (e.keyCode == ctrlCode) {
            isCtrlPressed = true;
        }
    });

    $('body').on("click", '.eps-result-block',  function(e){
        if(isNotRunning(e.currentTarget)) {
            if (e.target.type == checkboxType) {
                var checkBox = $('#' + e.target.id);
                var testScript = $(e.currentTarget);
                if (isNotEmptyLastColor(testScript)) {
                    revertColor(testScript);
                    setCheckBox(checkBox, false);
                } else {
                    setColor(testScript);
                    setCheckBox(checkBox, true);
                }
            } else if (isTestScriptBlock(e.target)) {
                selectTestscripts(getId(e.target));
            } else if (isTestScriptBlock(e.currentTarget) && e.target.tagName != labelTag
                && e.target.tagName != linkTag) {
                selectTestscripts(getId(e.currentTarget));
            }
        }

        updateCommonControls();
    });

    $('.eps-res-checkbox').on("click", function(e){});

    $('body').on("keyup", function(e){
            isCtrlPressed = false;
    });

});

function getSelectStatus() {
    const visibleElements = getVisibleChildrenFiltered($(resultBlock));
    const totalCount = visibleElements.filter(item => isNotRunning($(item))).length;
    const selectedCount = visibleElements.filter(item => isNotEmptyLastColor($(item))).length;

    if (totalCount === selectedCount && selectedCount > 0) return selectModes.all;
    if (selectedCount > 0) return selectModes.some;
    return selectModes.none;
}

function getSelectedResultIds() {
    const regex = /eps-result-([\d]+)/;
    return getVisibleChildrenFiltered($(resultBlock))
        .filter(item => item.id.match(regex)[1] && isNotEmptyLastColor($(item)))
        .map(item => item.id.match(regex)[1]);
}

function selectTestscripts(id) {
    if (isCtrlPressed) {
        var testScript = $('#' + testScriptBlockPrefix + id);
        if (isEmptyLastColor(testScript)) {
            setColor(testScript);
            setCheckBox(id);
        } else {
            revertColor(testScript);
            resetCheckBox(id);
        }
    } else {
        var selectedTestScripts = $(resultBlock);
        for(var i = 0; i< selectedTestScripts.length; i++) {
            var testScript = $(selectedTestScripts[i]);
            if (isNotEmptyLastColor(testScript)) {
                revertColor(testScript);
                resetCheckBox(getId(selectedTestScripts[i]));
            }
        }
        var testScript = $('#' + testScriptBlockPrefix + id);
        setColor(testScript);
        setCheckBox(id);
    }
}

function getSelectTestscripts(fromDisk, confirmRemove) {
    var arr = getSelectedResultIds();

    if(arr.length === 0) {
        var errorMessage = "You must select some result before deleting";
        showGrowlMessage([{name: "Severity", value: "WARN"},{name: "Summary", value: "No result deleted"},{name: "Detail", value: errorMessage}]);
        return null;
    }

    if(confirmRemove) {
        clearSelectedTestScript([{name:'selectedTestScripts', value:arr}, {name:'fromDiskBool', value:fromDisk}]);
    }
}

function selectAllTestscripts() {
    const resultBlockWrapper = $(resultBlock);

    const testScripts = getChildrenFiltered(resultBlockWrapper);
    const allSelected = getSelectStatus() === selectModes.all;

    testScripts.filter(item => isNotRunning(item)).forEach(item => {
        const testScript = $(item);

        if (allSelected) {
            revertColor(testScript);
            resetCheckBox(getId(item));
        } else {
            if (isEmptyLastColor(testScript) && !item.hidden) {
                setColor(testScript);
                setCheckBox(getId(item));
            }
        }
    });

    updateCommonControls();
}

function updateCommonControls() {
    const selectMode = getSelectStatus();
    setCommonCheckBox(selectMode === selectModes.all);
    setClearResultsButtonVisibility(selectMode === selectModes.all || selectMode === selectModes.some);
}

function getVisibleChildrenFiltered(jqueryObject) {
    return getChildrenFiltered(jqueryObject).filter(item => !item.hidden)
}

function isEmptyLastColor(testScript){
    return testScript.prop(lastBackgroundColor) == null || testScript.prop(lastBackgroundColor) == '';
}

function isNotEmptyLastColor(testScript){
    return testScript.prop(lastBackgroundColor) != null && testScript.prop(lastBackgroundColor) != '';
}

function revertColor(testScript){
    testScript.css(background, testScript.prop(lastBackgroundColor));
    testScript.prop(lastBackgroundColor, '');
}

function setColor(testScript){
    testScript.prop(lastBackgroundColor, testScript.css(background));
    testScript.css(background, selectColor);
}

function resetCheckBox(id){
    var checkBox = $('#check-' + id);
    checkBox.prop(checked, false);
}

function setCheckBox(id, check){
    if(check == null){
        var checkBox = $('#check-' + id);
        checkBox.prop(checked, true);
    } else{
        id.prop(checked, check);
    }
}

function setCommonCheckBox(value) {
    var commonCheckBoxIcon = $('#selectAllTestScriptButton .ui-menuitem-icon')[0];

    if (value) {
        $(commonCheckBoxIcon).removeClass('eps-custom-icon-checkbox-round-unchecked');
        $(commonCheckBoxIcon).addClass('eps-custom-icon-checkbox-round-checked');
    } else {
        $(commonCheckBoxIcon).addClass('eps-custom-icon-checkbox-round-unchecked');
        $(commonCheckBoxIcon).removeClass('eps-custom-icon-checkbox-round-checked');
    }
}

function setClearResultsButtonVisibility(value) {
    var clearResultsButton = $('.eps-clear-test-results-dropdown')[0];

    if (value) {
        $(clearResultsButton).removeClass('eps-clear-results-button-hidden');
    } else {
        $(clearResultsButton).addClass('eps-clear-results-button-hidden');
    }
}

function isSelected(checkBox){
    return checkBox.prop(checked);
}

function getId(resultBlock){
    return resultBlock.id.substring('eps-result-'.length);
}

function isTestScriptBlock(target){
    return target.id.indexOf(testScriptBlockPrefix) > -1;
}

function isNotRunning(target){
    var status = $(target).find('.eps-result-status').text().trim();
    return status != 'RUNNING' && status != 'READY' && status != 'PREPARING' && status != 'INITIAL' && status != 'PAUSED (∞)';
}

function getChildrenFiltered(jqueryObject) {
    const result = [];
    for (let i = 0; i < jqueryObject.length; i++) {
        if (jqueryObject[i].id !== scriptsOmittedCardId) {
            result.push(jqueryObject[i]);
        }
    }
    return result;
}
