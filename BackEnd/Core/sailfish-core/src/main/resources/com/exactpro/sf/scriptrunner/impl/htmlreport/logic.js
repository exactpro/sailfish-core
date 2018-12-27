var mTableOffset, mHeader, mFixedHeader; // For messages table
var lTableOffset, lHeader, lFixedHeader; // For logs table
var treetable_rowstate = [];
var treetable_callbacks = [];
var checkPointsCollapsed = [];
var headStyle; // css from head

var rules = new Map();
var lastRuleIndex = -1;

var verificationSorter;


function showHideElements (checkbox) {
    let rule = rules.get(checkbox);
    if(rule === null || rule === undefined){
        return;
    }

    if (!checkbox.checked) {
        rule.index = ++lastRuleIndex;
        headStyle.insertRule(rule.rule, rule.index);
    } else {
        headStyle.deleteRule(rule.index);
        onRuleRemoved(rule.index);
        rule.index = -1;
        lastRuleIndex--;
    }
}

function showHideAll(checkbox){
    var menuItem = checkbox.parentNode;
    var submenuItems = menuItem.getElementsByClassName("submenu-items")[0].getElementsByClassName("submenu-item");

    for(let i = 0; i < submenuItems.length; i++){
        let subCheckbox = submenuItems[i].getElementsByTagName("input")[0];
        subCheckbox.checked = checkbox.checked;
        showHideElements(subCheckbox);
    }
}

function onRuleRemoved(deletedIndex) {
    rules.forEach(function(item){
        if(deletedIndex < item.index){
            item.index --;
        }
    });
}

function treetable_hideRow(rowId) {
    let el = document.getElementById(rowId);
    el.style.display = "none";
}

function treetable_showRow(rowId) {
    let el = document.getElementById(rowId);
    el.style.display = "";
}

function treetable_hasChildren(rowId) {
    let res = document.getElementById(rowId + '_0');
    return (res != null);
}

function treetable_getRowChildren(rowId) {
    let el = document.getElementById(rowId);
    var arr = new Array();
    let i = 0;
    while (true) {
        let childRowId = rowId + '_' + i;
        let childEl = document.getElementById(childRowId);
        if (childEl) {
            arr[i] = childRowId;
        } else {
            break;
        }
        i++;
    }
    return (arr);
}

function treetable_toggleRow(rowId, tableId, state, force) {

    var rowChildren;
    var i;
    force = (force == null) ? 1 : force;
    let row_state;
    if (state == null) {
        let togglerHref = document.getElementById('toggler' + rowId);
        if (togglerHref != null) {
            togglerHref.style.backgroundImage = togglerHref.style.backgroundImage == 'url("resources/2.gif")' ?
                'url("resources/1.gif")' :
                'url("resources/2.gif")';
        }
        row_state = ((treetable_rowstate[rowId]) ? (treetable_rowstate[rowId]) : 1) * -1;
    } else {
        row_state = state;
    }
    rowChildren = treetable_getRowChildren(rowId);
    if (rowChildren.length == 0) return (false);
    for (i = 0; i < rowChildren.length; i++) {
        if (row_state == -1) {
            treetable_hideRow(rowChildren[i]);
            treetable_toggleRow(rowChildren[i], tableId, row_state, -1);
        } else {
            if (force == 1 || treetable_rowstate[rowId] != -1) {
                treetable_showRow(rowChildren[i]);
                treetable_toggleRow(rowChildren[i], tableId, row_state, -1);
            }
        }
    }

    if (force == 1) {
        treetable_rowstate[rowId] = row_state;
        treetable_fireEventRowStateChanged(rowId, row_state);
    }

    if (rowId.lastIndexOf('_') - rowId.indexOf('_') == 2) {
        var allToggled = true;
        let table = document.getElementById(tableId);
        rowChildren = table.getElementsByTagName('tr');
        for (i = 0; i < rowChildren.length; i++) {
            if (index = rowChildren[i].id.indexOf('_')) {
                let togglerHref = document.getElementById('toggler' + rowChildren[i].id);
                if (togglerHref != null) {
                    if(rowChildren[i].id.lastIndexOf('_') - index == 2 &&
                        togglerHref.style.backgroundImage != 'url("resources/2.gif")') {
                        allToggled = false;
                    }
                }
            }
        }
        if (allToggled) {
            document.getElementById('togglerCollapseAll' + tableId).style.display = 'none';
            document.getElementById('togglerExpandAll' + tableId).style.display = 'inline';
        } else {
            document.getElementById('togglerExpandAll' + tableId).style.display = 'none';
            document.getElementById('togglerCollapseAll' + tableId).style.display = 'inline';
        }
    }
    return (true);
}



function treetable_fireEventRowStateChanged(rowId, state) {
    if (treetable_callbacks['eventRowStateChanged']) {
        let callback = treetable_callbacks['eventRowStateChanged'] + "('" + rowId + "', " + state + ");";
        eval(callback);
    }
}

function treetable_collapseAll(tableId) {
    let table = document.getElementById(tableId);
    document.getElementById('togglerCollapseAll' + tableId).style.display = 'none';
    document.getElementById('togglerExpandAll' + tableId).style.display = 'inline';
    let rowChildren = table.getElementsByTagName('tr');
    for (let i = 0; i < rowChildren.length; i++) {
        let togglerHref = document.getElementById('toggler' + rowChildren[i].id);
        if (togglerHref != null) {
            togglerHref.style.backgroundImage = 'url("resources/2.gif")';
        }
        if (index = rowChildren[i].id.indexOf('_')) {
            if(rowChildren[i].id.lastIndexOf('_') - index > 2) {
                rowChildren[i].style.display = 'none';
            }
            if (treetable_hasChildren(rowChildren[i].id)) {
                treetable_rowstate[rowChildren[i].id] = -1;
                treetable_fireEventRowStateChanged(rowChildren[i].id, -1);
            }
        }
    }
    return (true);
}

function treetable_expandAll(tableId) {
    let table = document.getElementById(tableId);
    document.getElementById('togglerExpandAll' + tableId).style.display = 'none';
    document.getElementById('togglerCollapseAll' + tableId).style.display = 'inline';
    let rowChildren = table.getElementsByTagName('tr');
    for (let i = 0; i < rowChildren.length; i++) {
        var togglerHref = document.getElementById('toggler' + rowChildren[i].getAttribute("id"));
        if (togglerHref != null) {
            togglerHref.style.backgroundImage = 'url("resources/1.gif")';
        }
        if (index = rowChildren[i].id.indexOf('_')) {
            rowChildren[i].style.display = '';
            if (treetable_hasChildren(rowChildren[i].id)) {
                treetable_rowstate[rowChildren[i].id] = 1;
                treetable_fireEventRowStateChanged(rowChildren[i].id, 1);
            }
        }
    }
    return (true);
}

function isHidden(el) {
    var style = window.getComputedStyle(el);
    return (style.display === 'none');
}


window.onload = function() {
    verificationSorter = new VerificationSorter();
    checkPointsCollapsed = [];
    headStyle = document.styleSheets[0];

    var checkboxes = document.getElementsByClassName("menu-item");
    for(let i = 0; i < checkboxes.length; i++){
        let rule = checkboxes[i].getAttribute("data-rule");
        if(rule !== null && rule !== undefined){
            rules.set(checkboxes[i], {index: -1, rule : rule});
        }
    }

    disableNextBtn();

    if (document.getElementById("messageTable") == null) {
        return;
    }

    mHeader = document.getElementById("messageTable").getElementsByTagName("thead")[0].cloneNode(true);
    mFixedHeader = document.getElementById("messages-header-fixed");
    mFixedHeader.appendChild(mHeader);

    var logsPresented = document.getElementById("logsTable") != null;
    if (logsPresented) {
        lHeader = document.getElementById("logsTable").getElementsByTagName("thead")[0].cloneNode(true);
        lFixedHeader = document.getElementById("logs-header-fixed");
        lFixedHeader.appendChild(lHeader);
    }

    document.getElementById("wrapper").onscroll = function () {
        updateOffsets();

        let msgTable = document.getElementById("messageTable");
        if (!isHidden(msgTable)) {
            if (mTableOffset > 0 || (mTableOffset < 0 && Math.abs(mTableOffset) >= msgTable.scrollHeight - 100)) {
                mFixedHeader.style.display = 'none';
            }
            else if (mTableOffset <= 0) {
                mFixedHeader.style.display = 'table';
                mFixedHeader.style.width = (msgTable.clientWidth + 1) + "px";
            }
        }

        let logsTable = document.getElementById("logsTable");
        if (logsPresented && !isHidden(document.getElementById("logsTable"))) {

            if (lTableOffset > 0 || (lTableOffset < 0 && Math.abs(lTableOffset) >= logsTable.scrollHeight - 100)) {
                lFixedHeader.style.display = 'none';
            } else if (lTableOffset <= 0) {
                lFixedHeader.style.display = 'table';
                lFixedHeader.style.width = (logsTable.clientWidth + 1) + "px";
            }
        }
    };
};

window.onresize = function() {

    if (document.getElementById("messageTable") == null) {
        return;
    }

    mFixedHeader.style.width = document.getElementById("messageTable").clientWidth+"px";
    lFixedHeader.style.width = document.getElementById("logsTable").clientWidth+"px";
    updateOffsets();
};

var filters = {
    "MsgName": "",
    "From": "",
    "To": "",
    "Content": ""
};

function showhide(id) {
    let element = document.getElementById(id);
    let isDisplaying = element.style.display == 'block';

    element.style.display = isDisplaying ? 'none' : 'block';
    let nElement = document.getElementById('n' + id);
    nElement.style.backgroundImage = !isDisplaying ? 'url(resources/1.gif)' : 'url(resources/2.gif)';
    if(!isDisplaying){
        verificationSorter.preSortVerifications(element.parentNode)
    }

    if (document.getElementById("messageTable") != null) {
        updateOffsets();
    }
}

function findUpNodeWrapperTableDivTag(el) {
    while (el.parentNode) {
        var el = el.parentNode;
        if (el.className.includes('eps-table-wrapper'))
            return el.parentNode;
    }
    return null;
}

function findUpNodeWrapperDivTag(el) {
    while (el.parentNode) {
        var el = el.parentNode;
        if (el.className.includes('eps-node-wrapper'))
            return el.parentElement;
    }
    return null;
}

function findFirstNodeWrapperDivTag(el) {
    while (el.parentNode) {
        var el = el.parentNode;
        if (el.className.includes('eps-node-wrapper'))
            return el;
    }
    return null;
}

function checkMessagesTable() {
    var el = findUpNodeWrapperTableDivTag(document.getElementById("messageTable"));
    if(el.style.display == 'none'){
        showhide(el.id);
    }
}

function showhideException(id) {
    let e = document.getElementById(id);
    e.style.display = e.style.display == 'block' ? 'none' : 'block';
    let ne = document.getElementById('n'+id);
    ne.style.backgroundImage = e.style.display == 'block' ? 'url(resources/1.gif)' : 'url(resources/2.gif)';
    let shortHeader = document.getElementById('exShortHeader'+id);
    shortHeader.style.display = e.style.display == 'block' ? 'none' : 'block';
    let fullHeader = document.getElementById('exFullHeader'+id);
    fullHeader.style.display = e.style.display == 'block' ? 'block' : 'none';
    if (document.getElementById("messageTable") != null) {
        updateOffsets();
    }
}

function updateOffsets() {
    let msgTable = document.getElementById("messageTable")
    if (msgTable && !isHidden(msgTable)) {
        mTableOffset = document.getElementById("messageTable").getBoundingClientRect().top;
    }
    let logTable = document.getElementById("logsTable");
    if (logTable && !isHidden(logTable)) {
        lTableOffset = document.getElementById("logsTable").getBoundingClientRect().top;
    }
}

function expandCollapseAllCheckPoints(expand) {
    var togglers = document.getElementsByClassName("toggler");
    var expand_b = expand == 'true' ? true : false;

    for (var i = 0; i < togglers.length; i++) {
        var collapsed = checkPointsCollapsed.includes(togglers[i].id);
        if ((expand_b && collapsed) || (!expand_b && !collapsed)) {
            toggleCheckpoint(togglers[i].id);
        }
    }

    document.getElementById("togglerCollapseAllCheckPoints").style["display"] = expand_b ? "inline" : "none";
    document.getElementById("togglerExpandAllCheckPoints").style["display"] = expand_b ? "none" : "inline";
}

function changeCheckpointState(togglerId) {
    var toggler = document.getElementById(togglerId);

    if (checkPointsCollapsed.includes(togglerId)) {
        checkPointsCollapsed.splice(checkPointsCollapsed.indexOf(togglerId), 1);
    } else {
        checkPointsCollapsed.push(togglerId);
    }

    toggler.style.backgroundImage = checkPointsCollapsed.includes(togglerId) ? 'url(resources/2.gif)' : 'url(resources/1.gif)';
}

function toggleCheckpoint(togglerId) {
    changeCheckpointState(togglerId);
    var togglerName = togglerId.split("toggler")[1];
    var messageTable = document.getElementById("messageTable");
    var rows = messageTable.getElementsByClassName("UnderCheckPoint" + togglerName);

    for (var i = 0; i < rows.length; i++) {
        var row = rows[i];
        if (row.getElementsByClassName("checkPointTD").length > 0) {
            continue;
        }
        if (!checkPointsCollapsed.includes(togglerId)) {
            if (filterActive()) {
                if (row.innerHTML.indexOf("<span class=\"highlight\">") > 0) {
                    row.style["display"] = 'table-row';
                }
            } else {
                row.style["display"] = 'table-row';
            }
        } else {
            row.style["display"] = 'none';
        }
    }

    var state = allCheckPointsState();

    if (state == "allExpanded" || state == "") {
        document.getElementById("togglerCollapseAllCheckPoints").style["display"] = "inline";
        document.getElementById("togglerExpandAllCheckPoints").style["display"] = "none";
    }
    if (state == "allCollapsed") {
        document.getElementById("togglerCollapseAllCheckPoints").style["display"] = "none";
        document.getElementById("togglerExpandAllCheckPoints").style["display"] = "inline";
    }
}

function allCheckPointsState() {
    var togglers = document.getElementsByClassName("toggler");

    if (checkPointsCollapsed.length == togglers.length) {
        return "allCollapsed";
    } else if (checkPointsCollapsed.length == 0) {
        return "allExpanded";
    } else {
        return "";
    }
}

function filterActive() {
    var messageTable = document.getElementById("messageTable");

    if (messageTable.innerHTML.indexOf("<span class=\"highlight\">") > 0) {
        return true;
    }

    return false;
}

function filterMessages(e) {
    filters[e.name] = e.value;

    removeHighlight();

    var messageTable = document.getElementById("messageTable");
    var rows = messageTable.getElementsByClassName("messageRow");

    for (var i = 0; i < rows.length; i++) {
        var row = rows[i];
        if (row.getElementsByClassName("checkPointTD").length > 0) {
            continue;
        }
        var showRowMap = new Map();
        Object.keys(filters).forEach(function (key) {
            if (filters[key] != "") {
                showRowMap.set(key, row.getElementsByClassName(key)[0].innerHTML.indexOf(filters[key]) >= 0);
            } else {
                showRowMap.set(key, true);
            }
        });
        if (showRow(showRowMap)) {
            row.style["display"] = "table-row";
            Object.keys(filters).forEach(function (key) {
                if (filters[key] != "") {
                    var rowElement = row.getElementsByClassName(key)[0];

                    if(key === "Content") {
                        rowElement = rowElement.getElementsByTagName('pre')[0];
                    }

                    highlight(rowElement, filters[key]);
                }
            });
        } else {
            row.style["display"] = "none";
        }
    }

    showHideCheckPointRaws(rows);
    showHideExpandCollapseCheckPoints(rows);
}

function showHideCheckPointRaws(rows) {
    for (var j = 0; j < rows.length; j++) {
        var row = rows[j];
        if (row.getElementsByClassName("checkPointTD").length > 0) {
            var unders = document.getElementsByClassName("UnderCheckPoint");
            var visible = false;
            for (var i = 0; i < unders.length; i++) {
                var under = unders[i];
                if (under.innerHTML == row.getElementsByClassName("toggler")[0].id.substring(7) && under.parentElement.style["display"] != "none") {
                    visible = true;
                }
            }
            if (visible) {
                row.style["display"] = "table-row";
                if (checkPointsCollapsed.includes(row.getElementsByClassName("toggler")[0].id)) {
                    changeCheckpointState(row.getElementsByClassName("toggler")[0].id);
                }
            } else {
                row.style["display"] = "none";
            }
        }
    }
}

function showHideExpandCollapseCheckPoints(rows) {
    var hasCheckPoints = false;

    for (var j = 0; j < rows.length; j++) {
        var row = rows[j];
        if (row.getElementsByClassName("checkPointTD").length > 0 && row.style["display"] != "none") {
            hasCheckPoints = true;
            break;
        }
    }

    var control1 = document.getElementById("togglerCollapseAllCheckPoints");
    var control2 = document.getElementById("togglerExpandAllCheckPoints");

    control1.style["display"] = control1.style["display"] == 'none' ? 'none' : hasCheckPoints ? 'inline' : 'none';
    control2.style["display"] = control2.style["display"] == 'none' ? 'none' : hasCheckPoints ? 'inline' : 'none';
}

function showHideCheckPoints(checkbox) {
    var show_b = checkbox.checked;
    var togglers = document.getElementsByClassName("toggler");
    var displayStyle = show_b ? 'table-row' : 'none';

    for (var i = 0; i < togglers.length; i++) {
        var toggler = togglers[i];
        if (checkPointsCollapsed.includes(toggler.id)) {
            toggleCheckpoint(toggler.id);
        }
        toggler.parentElement.parentElement.style["display"] = displayStyle;
    }

    document.getElementById("togglerCollapseAllCheckPoints").style["display"] = show_b ? 'inline' : 'none';
    document.getElementById("togglerExpandAllCheckPoints").style["display"] = 'none';
}

function highlight(element, phrase) {
    var elementInner = element.innerHTML;
    var regExpPhrase = new RegExp(escapeRegExp(phrase), 'g');
    var replacePhrase = "<span class=\"highlight\">" + phrase + "</span>";
    var newInner = elementInner.replace(regExpPhrase, replacePhrase);
    element.innerHTML = newInner;
}

function removeHighlight() {
    var messageTable = document.getElementById("messageTable");
    var messageRows = messageTable.getElementsByClassName("messageRow");
    for (var i = 0; i < messageRows.length; i++) {
        if (messageRows[i].innerHTML.indexOf("<span class=\"highlight\">") >= 0) {
            var newInner = messageRows[i].innerHTML.replace(/<span class="highlight">/g, "");
            newInner = newInner.replace(/"<\\\/span>/g, "");
            messageRows[i].innerHTML = newInner;
        }
    }
}

function showRow(showRowMap) {
    var result = true;
    showRowMap.forEach(function (value, key, map) {
        if (value == false) {
            result = false;
        }
    });
    return result;
}

function escapeRegExp(str) {
    return str.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&");
}

function toggleRaw(e) {
    var t = e.checked;
    var curTable = document.getElementById("messageTable");
    var width = curTable.getElementsByTagName('th')[5].getBoundingClientRect().width;
    var fakeWidth = 70;
    var cWidth = curTable.getElementsByTagName('th')[4].getBoundingClientRect().width;
    var n = curTable.getElementsByClassName("RawMessage");
    var r = t ? "table-cell" : "none";
    for (var i = 0; i < n.length; i++) {
        var currentRow = curTable.getElementsByClassName('messageRow')[i];
        if (currentRow.getElementsByClassName("checkPointTD").length > 0) {
            continue;
        }
        var s = n[i];
        s.style.display = r;
        if (t) {
            var fakeNode = currentRow.getElementsByClassName('fake')[0];
            fakeNode.parentNode.removeChild(fakeNode);
        } else {
            var elem = document.createElement('td');
            elem.className = 'fake';
            elem.style.width = fakeWidth;
            elem.appendChild(document.createTextNode('...'));
            currentRow.insertBefore(elem, s);
        }
    }
    if (!t) {
        curTable.getElementsByTagName('th')[5].style.width = '120px'; // <th>
        curTable.getElementsByTagName('th')[4].style.width = '50%'; // <th>
        curTable.getElementsByClassName("RawMessage")[1].style.width = '120px'; // <td>
    } else {
        var newRawWidth = cWidth / 2 + 120;
        curTable.getElementsByTagName('th')[5].style.width = newRawWidth + 'px'; // <th>
        curTable.getElementsByTagName('th')[4].style.width = '25%'; // <th>
        curTable.getElementsByClassName("RawMessage")[1].style.width = newRawWidth + 'px'; // <td>
    }

    if (document.getElementById("messageTable") != null) {
        mHeader = document.getElementById("messageTable").getElementsByTagName("thead")[0].cloneNode(true);
        mFixedHeader = document.getElementById("messages-header-fixed");
        mFixedHeader.innerHTML = "";
        mFixedHeader.appendChild(mHeader);

        let checkboxes = document.getElementsByName("RawMessage");

        for (i = 0; i < checkboxes.length; i++) {
            checkboxes[i].checked = t;
        }
    }
}

function getTotalCount() {
    var container = document.getElementById('content');
    if (!container) {
        return;
    }


    var links = container.getElementsByTagName('a');
    var hrefs = [];
    sessionStorage.setItem('totalTestCasesCount', links.length);
    var len = links.length;
    for (var i = 0; i < len; i++) {
        hrefs.push(links[i].href);
    }
    sessionStorage.setItem('hrefs', JSON.stringify(hrefs));
}

function disableNextBtn() {
    getTotalCount();
    loadRefs();

    try {
        var isLast = sessionStorage.getItem('totalTestCasesCount') == document.getElementById('nextTCLink').getAttribute('data-order');
    }
    catch (e) {
        return;
    }


    var btn = document.getElementById('nextBtn');
    if (btn == null || !isLast) {
        return;
    }
    if (document.getElementById('nextTCLink').href.localeCompare('#')) {
        btn.disabled = true;
        btn.classList.add('ui-state-disabled');
        document.getElementById('nextTCLink').href = '#';
    }
}

function onKeyPress(event) {
    if (event.target.tagName === 'INPUT') {
        return true;
    }
    var code = event.which || event.keyCode;
    var wrapper = document.getElementById('wrapper');
    if (code === 32) {
        wrapper.scrollTop += wrapper.clientHeight;
    }
    event.preventDefault();
    return false;
}

function loadRefs() {
    var hrefs = JSON.parse(sessionStorage.getItem('hrefs'));
    var currentLink = document.URL;
    if ((currentLink.indexOf('report.html') >= 0) || (hrefs.length == 1)) {
        return;
    }
    var prevObj = document.getElementById('prevTCLink');
    var nextObj = document.getElementById('nextTCLink');

    var isCurrentLinkContainsEndSharp = currentLink.lastIndexOf('#') == (currentLink.length - 1);

    var index = 0;
    if (isCurrentLinkContainsEndSharp) {
        index = hrefs.indexOf(currentLink.substring(0, (currentLink.length - 1)));
    } else {
        index = hrefs.indexOf(currentLink);
    }

    if (index == 0) {

        nextObj.href = hrefs[index + 1]
    } else if (index == hrefs.length - 1) {
        prevObj.href = hrefs[index - 1];

    } else {
        nextObj.href = hrefs[index + 1];
        prevObj.href = hrefs[index - 1];

    }
}

function expandToIFrame(event, element) {
    event.preventDefault();
    var topNcontainer = element.parentElement;
    var action = topNcontainer.parentElement;

    // iframe
    var iframe = document.createElement("iframe");
    iframe.src = element.getAttribute('href');
    // hide button
    var hideButton = document.createElement("button");
    hideButton.innerHTML = "hide";
    hideButton.setAttribute('class', 'ui-button ui-big-button eps-hide-btn');
    hideButton.src = element.getAttribute('href');
    hideButton.onclick = function(event) {
        event.preventDefault();
        action.replaceChild(topNcontainer, iframe);
        action.removeChild(hideButton);
    };

    // replace:
    action.replaceChild(iframe, topNcontainer);
    action.insertBefore(hideButton, iframe);
}

class VerificationSorter {

    constructor() {
        this.verificationMessagesStatus = new Map();
        this.autocompleteData = new Map();
        this.statuses = ["NA", "PASSED", "CONDITIONALLY_PASSED", "FAILED"];
    }

    preSortVerifications(element) {
        let input = element.getElementsByClassName('sort-verifications-input')[0];
        if (input !== undefined && input !== null && !input.value) {
            input.value = element.getAttribute("data-verificationsorder");
            let data = this.autocompleteData.get(input);
            if (data === null || data === undefined) {
                this.extractAutocompleteData(input);
                this.autocompleteData.set(input, "generated");
                this.parse(input);
            }
        }
    }

    parse(input) {
        let unparsedOrder = input.value;
        let preOrders = unparsedOrder.split(",");
        let sortContainer = input.parentNode;

        for (let preOrder of preOrders) {
            let preOrderSplitted = preOrder.split(":");
            let fieldName = preOrderSplitted[0];
            let fieldStatus = preOrderSplitted[1];

            if (!fieldName || !fieldStatus) {
                continue;
            }

            fieldName = fieldName.trim();
            fieldStatus = fieldStatus.trim().toUpperCase();

            if (this.statuses.includes(fieldStatus)) {
                this.createChip(sortContainer, fieldName, fieldStatus);
                this.sortVerifications(sortContainer);
                input.value = "";
            }
        }
    }

    createChip(parent, fieldName, fieldStatus) {
        let chips = parent.getElementsByClassName("chips")[0];
        let thisObject = this;

        for (let chip of chips.getElementsByClassName("chip")) {
            if ($(chip).text().includes(fieldName)) {
                showStatusDialog("Error", `Duplicate field - ${fieldName}`);
                return;
            }
        }

        $(chips).append($('<div/>', {
            class: 'chip',
            append: [
                $('<span/>', { class: 'ui-icon ui-icon-arrow-4-diag' }),
                $(document.createTextNode(`${fieldName}:${fieldStatus}`)),
                $('<span/>', { class: 'chip-remove-btn', click: function () { verificationSorter.removeChip(this) }, html: '&times;' })
            ]
        }));

        $(chips).sortable({
            handle: '.ui-icon-arrow-4-diag',
            deactivate: function (event) {
                let chip = event.target;
                let sortContainer = $(chip).parents('.sort-container').get(0);
                thisObject.sortVerifications(sortContainer);
            }
        });
    }

    removeChip(chip){
        let sortContainer = $(chip).parents('.sort-container').get(0);
        $(chip).parent().remove();
        this.sortVerifications(sortContainer);
    }

    extractVerificationsOrder(sortContainer) {
        let order = {};

        $(sortContainer).find(".chip").each(function () {
            let text = $(this).text();
            text = text.substring(0, text.length - 1); // remove ×
            text = text.split(":");
            order[text[0]] = text[1];
        });
        return order;
    }

    sortVerifications(sortContainer) {
        let parent = sortContainer.parentNode;
        let filter = this.extractVerificationsOrder(sortContainer);
        let verificationsSpans = [].slice.call(parent.getElementsByClassName('verification_failed'));
        let lastElement = findFirstNodeWrapperDivTag(verificationsSpans[verificationsSpans.length - 1]);

        if (filter.length === 0) {
            return;
        }

        let messagesStatus = this.extractMessagesStatus(verificationsSpans);

        let newOrder = this.sortVerificationsSpans(filter, verificationsSpans, messagesStatus, []);

        let verificationsDivs = [];

        for (let i = 0; i < newOrder.length; i++) {
            verificationsDivs.push(findFirstNodeWrapperDivTag(newOrder[i]));
        }

        verificationsDivs.reverse();

        for (let i = 0; i < verificationsDivs.length; i++) {
            parent.insertBefore(verificationsDivs[i], lastElement);
            lastElement = verificationsDivs[i];
        }
    }

    sortVerificationsSpans(filter, verificationsSpans, messagesStatus, newOrders) {
        let invalidVerificationSpan = verificationsSpans.slice();
        while (Object.keys(filter).length !== 0) {
            for (let i = 0; i < verificationsSpans.length; i++) {
                let filterResult = {
                    fieldsCompare: false,
                    groupCompare: true
                };

                let verificationSpan = verificationsSpans[i];

                let messageStatus = messagesStatus.get(verificationSpan);

                this.iterateJson(messageStatus, filter, filterResult);

                if (filterResult.fieldsCompare && filterResult.groupCompare) {
                    newOrders.push(verificationSpan);
                    invalidVerificationSpan.splice(invalidVerificationSpan.indexOf(verificationSpan), 1);
                }
            }
            this.removeLastProperty(filter);
        }
        return newOrders.concat(invalidVerificationSpan);
    }

    removeLastProperty(obj) {
        let i = 0;
        let last = Object.keys(obj).length - 1;
        for (let prop in obj) {
            if (prop) {
                if (i === last) {
                    delete obj[prop];
                    return;
                }
                i++;
            }
        }
    }

    iterateJson(json, filter, result) {
        return this.__iterateJson(json, Object.assign({}, filter), 0, result);
    }

    __iterateJson(json, filter, level, filterResult) {
        for (let property in json) {
            if (property) {
                if (Object.keys(filter).length === 0) {
                    return; // no more properties in the filter
                }

                if (json.hasOwnProperty(property)) {
                    if (typeof json[property] === 'object') {
                        this.__iterateJson(json[property], filter, level + 1, filterResult);
                    } else {
                        if (filter.hasOwnProperty(property)) {
                            if (filter[property] === json[property]) {
                                if (level >= 2) { // level 2+ - groups
                                    filterResult.groupCompare = true;
                                } else {
                                    filterResult.fieldsCompare = true;
                                    delete filter[property];
                                }
                            } else if (level >= 2) {
                                filterResult.groupCompare = false;
                            } else {
                                filterResult.fieldsCompare = false;
                                return;
                            }
                        }
                    }
                }
            }
        }

        if (Object.keys(filter).length > 0) { // message does not contain filter property
            filterResult.fieldsCompare = false;
            return;
        }
    }

    extractMessagesStatus(verificationsSpans) {
        let parentDiv = findUpNodeWrapperDivTag(verificationsSpans[0]);
        let messagesStatus = this.verificationMessagesStatus.get(parentDiv);
        if (messagesStatus === undefined || messagesStatus === null) {
            messagesStatus = new Map();
            for (let i = 0; i < verificationsSpans.length; i++) {
                let verificationSpan = verificationsSpans[i];
                messagesStatus.set(verificationSpan, JSON.parse(verificationSpan.getAttribute("data-jsonverificationresult")));
            }
            this.verificationMessagesStatus.set(parentDiv, messagesStatus);
        }
        return messagesStatus;
    }

    extractAutocompleteData(input) {
        let parent = $(input).parents(".sort-container").get(0).parentNode;
        let datalist = input.getAttribute("list");
        let verificationsSpans = [].slice.call(parent.getElementsByClassName('verification_failed'));
        let messagesStatus = this.extractMessagesStatus(verificationsSpans);
        let values = new Set();
        for (let messageStatus of messagesStatus.values()) {
            this.__extractAutocompleteData(messageStatus, $(`#${datalist}`), values);
        }
    }

    __extractAutocompleteData(messageStatus, datalist, values){
        for (let property in messageStatus) {
            if (messageStatus.hasOwnProperty(property)) {
                if (typeof messageStatus[property] === 'object') {
                    this.__extractAutocompleteData(messageStatus[property], datalist, values);
                } else {
                    let value = `${property}:${messageStatus[property]}`;
                    if (!values.has(value)) {
                        values.add(value);
                        datalist.append(`<option value='${value}'/>`);
                    }
                }
            }
        }
    }
}


//Machine learning data collect part

var reportState = 'usual';
var collectingActionID;
var collectingActionJson;
var problemExplanations = [];
var createdElements = [];
var explanationCheckboxes = [];
var explanationCheckboxesState = new Map();
var messagesJsons = [];
var activeActionCheckboxId;
var targetSF;
var user;

document.addEventListener('DOMContentLoaded', function() {
    generateActionMarkCheckboxes();
    window.parent.postMessage("get", "*");
});

window.addEventListener('message', handleMessage, false);

function handleMessage(event) {
    let data = event.data;
    setUpTargetSF(data.url);
    setUpUser(data.username);
}

function setUpTargetSF(tSF) {
    targetSF = tSF;
}

function setUpUser(tUser) {
    user = (tUser == null || tUser === '' ? null : tUser);
}

function generateActionMarkCheckboxes() {
    let actionNodes = document.getElementsByClassName("action_node");

    for (let i = 0; i < actionNodes.length; i++) {
        let actionBodyDivID = actionNodes[i].id.replace('n', '');
        let actionBodyDiv = document.getElementById(actionBodyDivID);
        let tr = actionNodes[i].parentElement.parentElement;
        let parentDiv = tr.parentElement.parentElement.parentElement;
        let intable = parentDiv.getElementsByClassName("intable")[0];
        let actionID = intable == null ? null : intable.id;

        if (!machineLearningData.hasOwnProperty(actionID)) {
            continue;
        }

        $(actionNodes[i]).click(function() {
            if (!isHidden($('#'+actionBodyDivID).get(0))) {
                handlePrediction(actionID, actionBodyDivID);
            }
        });

        var div = document.createElement('div');
        div.style.paddingLeft = '1em';
        div.innerHTML = '<label><input type="checkbox" id="actionCheckbox' + actionID + '" class="MLActionCheckbox" onchange="toggleMLAction(' + actionID + ', ' + actionBodyDivID + ')"/>Collect Machine Learning data for this action</label>';
        actionBodyDiv.insertBefore(div, actionBodyDiv.firstChild);
        var buttonDiv = document.createElement('div');
        buttonDiv.innerHTML = '<button class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-icon-left" style="height: 24px; width: 100px;" onclick="clearSelection()"><span class="ui-button-text ui-c">Clear selection</span></button>';
        var submitDiv = document.createElement('div');
        submitDiv.innerHTML = '<button class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-icon-left" style="height: 24px; width: 100px;" onclick="preSubmitSelection()"><span class="ui-button-text ui-c">Submit data</span></button>'
        var buttonsTr = document.createElement('tr');
        buttonsTr.id = 'button' + actionID;
        buttonsTr.style.display = 'none';
        var buttonTd1 = document.createElement('td');
        var buttonTd2 = document.createElement('td');
        buttonTd1.appendChild(buttonDiv);
        buttonTd2.appendChild(submitDiv);
        buttonsTr.appendChild(buttonTd1);
        buttonsTr.appendChild(buttonTd2);
        div.appendChild(buttonsTr);
    }
}

function toggleMLAction(actionID, actionBodyDivID) {
    if (reportState == 'collecting') {
        if (actionID == collectingActionID) {
            document.getElementById('button' + actionID).style.display = 'none';
            clean();
            return;
        } else {
            document.getElementById('button' + collectingActionID).style.display = 'none';
            clean();
        }
    }

    reportState = 'collecting';
    activeActionCheckboxId = 'actionCheckbox' + actionID;
    collectingActionID = actionID;
    collectingActionJson = machineLearningData[collectingActionID];
    var actionBodyDiv = document.getElementById(actionBodyDivID);
    var verificationSpans = [];
    var spans = actionBodyDiv.getElementsByTagName('span');

    for (let i = 0; i < spans.length; i++) {
        if (spans[i].className.includes('verification')) {
            verificationSpans.push(spans[i]);
        }
    }

    for (let i = 0; i < verificationSpans.length; i++) {
        var tr = verificationSpans[i].parentElement.parentElement.parentElement;
        var div = document.createElement('div');
        var msgId = parseInt(verificationSpans[i].getAttribute("data-msgid"));
        div.innerHTML = '<label><input type="checkbox" id="similarCheckbox' + msgId + '" class="MLSimilarCheckbox" onchange="setPEsimilarIndex(' + msgId + ')"/>Problem explanation</label>';
        var newTd = document.createElement('td');
        newTd.id = 'td' + i;
        createdElements.push(newTd.id);
        explanationCheckboxes.push('similarCheckbox' + msgId);
        explanationCheckboxesState.set('similarCheckbox' + msgId, false);
        newTd.appendChild(div);
        tr.appendChild(newTd);
    }

    var messageRows = document.getElementsByClassName('messageRow');

    for (var i = 0; i < messageRows.length; i++) {
        var contentTd = messageRows[i].getElementsByClassName('Content')[0];
        if (contentTd == null) {
            continue;
        }
        var jsonString = contentTd.getElementsByTagName('div')[0].innerText;
        try {
            var messageJson = JSON.parse(jsonString);
        } catch (e) {
            continue;
        }
        var messageId = messageJson.id;
        var messageProtocol = messageJson.protocol;
        var messageTimestamp = messageJson.timestamp;
        var inPeriod = messageTimestamp >= collectingActionJson.periodStart && messageTimestamp <= collectingActionJson.periodEnd;

        if (messageProtocol == collectingActionJson.protocol) {
            let div = document.createElement('div');
            div.id = 'div' + i;
            div.innerHTML = '<label><input type="checkbox" id="MTCheckbox' + messageId + '" class="MLMTCheckbox" onchange="setPEmessageTableId(' + messageId + ')"/>Problem explanation</label>';
            explanationCheckboxes.push('MTCheckbox' + messageId);
            explanationCheckboxesState.set('MTCheckbox' + messageId, false);
            createdElements.push(div.id);
            contentTd.appendChild(div);
            messagesJsons.push(messageJson);
        }
    }
}

function setPEsimilarIndex(msgId) {
    if (explanationCheckboxesState.get('similarCheckbox' + msgId) == true) {
        explanationCheckboxesState.set('similarCheckbox' + msgId, false);
        explanationCheckboxesState.set('MTCheckbox' + msgId, false);
        document.getElementById('MTCheckbox' + msgId).checked = false;
        problemExplanations.splice(problemExplanations.indexOf(msgId), 1);
        updateButtons();
        return;
    }
    explanationCheckboxesState.set('similarCheckbox' + msgId, true);
    explanationCheckboxesState.set('MTCheckbox' + msgId, true);
    document.getElementById('MTCheckbox' + msgId).checked = true;
    problemExplanations.push(msgId);
    updateButtons();
}

function setPEmessageTableId(id) {
    if (explanationCheckboxesState.get('MTCheckbox' + id) == true) {
        explanationCheckboxesState.set('MTCheckbox' + id, false);
        explanationCheckboxesState.set('similarCheckbox' + id, false);
        if (document.getElementById('similarCheckbox' + id) != undefined) {
            document.getElementById('similarCheckbox' + id).checked = false;
        }
        problemExplanations.splice(problemExplanations.indexOf(id), 1);
        updateButtons();
        return;
    }
    explanationCheckboxesState.set('MTCheckbox' + id, true);
    explanationCheckboxesState.set('similarCheckbox' + id, true);
    if (document.getElementById('similarCheckbox' + id) != undefined) {
        document.getElementById('similarCheckbox' + id).checked = true;
    }
    problemExplanations.push(id);
    updateButtons();
}

function updateButtons() {
    if (problemExplanations.length == 0) {
        document.getElementById('button' + collectingActionID).style.display = 'none';
    } else {
        document.getElementById('button' + collectingActionID).style.display = 'block';
    }
}

function preSubmitSelection() {
    var dialog = document.createElement('div');
    dialog.id = 'submitDialog';
    dialog.className = 'modal';
    createdElements.push(dialog.id);
    var content = document.createElement('div');
    content.className = 'modal-content';
    var header = document.createElement('div');
    header.className = 'modal-header';
    header.innerHTML = '<span class="dialog-title">Machine Learning: send training data</span><span class="close" onclick="closeDialog(\'submitDialog\')">&times;</span>';
    content.appendChild(header);
    var body = document.createElement('div');
    body.className = 'modal-body';
    var buttonsDiv = document.createElement('div');
    buttonsDiv.align = 'center';
    buttonsDiv.style = 'margin: 0 auto; padding-bottom: 0.7em;';
    buttonsDiv.appendChild(document.createElement('p'));
    var bTr = document.createElement('tr');
    var sendTd = document.createElement('td');
    var cancelTd = document.createElement('td');
    sendTd.align = 'left';
    cancelTd.align = 'left';
    sendTd.innerHTML = '<button class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-icon-left" style="height: 24px; width: 100px;" onclick="submitSelection()"><span class="ui-button-text ui-c">Send</span></button>';
    cancelTd.innerHTML = '<button class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-icon-left" style="height: 24px; width: 100px;" onclick="closeDialog(' + '\'submitDialog\'' + ')"><span class="ui-button-text ui-c">Cancel</span></button>';
    bTr.appendChild(sendTd);
    bTr.appendChild(cancelTd);
    buttonsDiv.appendChild(bTr);
    body.appendChild(submitDialogInfo());
    body.appendChild(buttonsDiv);
    content.appendChild(body);
    dialog.appendChild(content);
    document.getElementsByTagName('body')[0].appendChild(dialog);
    dialog.style.display = 'block';
}

function submitDialogInfo() {
    var checkedMap = new Map();

    for (var i = 0; i < messagesJsons.length; i++) {
        var message = messagesJsons[i];

        if (problemExplanations.includes(message.id)) {
            var mString = message.dictionaryURI + '#separator#' + message.name;
            if (checkedMap.has(mString)) {
                checkedMap.set(mString, checkedMap.get(mString) + 1);
            } else {
                checkedMap.set(mString, 1);
            }
        }
    }

    var infoDiv = document.createElement('div');
    infoDiv.align = 'center';
    infoDiv.style.width = '100%';
    var inner = '<table style="width: 100%; border-spacing: 1px 10px;"><thead class="mlDialogTableHeader"><tr><td><span class="tableNameElPadding">Title</span></td><td><span>Name</span></td><td><span>Count</span></td></tr></thead>';
    inner += '<tr><td colspan="3"><span>Failed message:</span></td></tr>';
    inner += '<tr><td class="tableSmallText"><span class="tableNameElPadding">' + collectingActionJson.dictionaryURI + '</span></td><td class="tableSmallText"><span>' + collectingActionJson.name + '</span></td></tr>';
    inner += '<tr><td colspan="3"><span>Explanation messages:</span></td></tr>';

    checkedMap.forEach(function (value, key,  map) {
        var splitArr = key.split('#separator#');
        inner += '<tr><td class="tableSmallText"><span class="tableNameElPadding">' + splitArr[0] + '</span></td><td class="tableSmallText"><span>' + splitArr[1] + '</span></td><td><span>' + value + '</span></td></tr>';
    });

    inner += '<tr><td colspan="2"><span>Total messages:</span></td><td><span>' + messagesJsons.length + '</span></td></tr>';
    inner += '<tr><td colspan="2"><span>Time interval:</span></td><td><span>' + (collectingActionJson.periodEnd - collectingActionJson.periodStart) + ' ms</span></td></tr>';
    inner += '<tr><td><span>User name:</span></td><td colspan="2"><span>' + (user == null ? 'not authorized' : user) + '</span></td></tr>';
    var inputValue = targetSF != null ? targetSF : '';
    inner += '<tr><td><span>Sailfish URL:</span></td><td colspan="2"><input id="targetInp" style="width: 90%" name="targetInput" placeholder="Please enter target SailFish url" value="' + inputValue + '" onkeyup="targetKeyUp()"/></td></tr></table>';

    infoDiv.innerHTML = inner;
    return infoDiv;
}

function targetKeyUp() {
    targetSF = document.getElementById('targetInp').value;

    if (e.keyCode == 13) {
        submitSelection();
    }
}

function closeDialog(id) {
    var dialog = document.getElementById(id);
    dialog.parentNode.removeChild(dialog);
    createdElements.splice(createdElements.indexOf(id), 1);
}

function handlePrediction(actionID, actionBodyDivID) {

    //remove old div with predictions
    $(`#predictions_node_wrapper${actionID}`).remove();

    $.ajax({
        url: targetSF + '/sfapi/machinelearning/predictions_ready',
        type: 'GET',
        async: true,
        dataType: 'text',
        success: function() {
            submitForPrediction(actionID, actionBodyDivID);
        }
    });
}

/**
 * submit collected machine learning data for prediction
 * and parse/render prediction result
 *
 * @param actionID - id of predicting action
 * @param actionBodyDivID - id of div contained target action
 */

function submitForPrediction(actionID, actionBodyDivID) {

    let mlDataEntityJson = machineLearningData[actionID];

    let messageJsons = [];
    let idToNameMap = new Map();

    for (let messageId of mlDataEntityJson.actuals) {
        let contentDiv = document.getElementById('jsn_cont_' + messageId);
        if (contentDiv == null) {
            continue;
        }

        let jsonString = contentDiv.innerText;
        let messageJson;
        try {
            messageJson = JSON.parse(jsonString);
        } catch (e) {
            continue;
        }

        idToNameMap.set(messageId, messageJson.name);

        let messageProtocol = messageJson.protocol;
        let messageTimestamp = messageJson.timestamp;
        let inPeriod = messageTimestamp >= mlDataEntityJson.periodStart && messageTimestamp <= mlDataEntityJson.periodEnd;

        if (inPeriod && messageProtocol == mlDataEntityJson.protocol) {
            messageJsons.push(messageJson);
        }
    }

    //not enough data
    if (messageJsons.length == 0) return;

    let toSubmit = JSON.parse(JSON.stringify(mlDataEntityJson));
    toSubmit.actuals = messageJsons;
    toSubmit.user = user == null ? 'null' : user;

    let predictionsSpan = $('<span>');

    let wrapper = $('<div>', {
        class: 'eps-node-wrapper',
        id: `predictions_node_wrapper${actionID}`,
        append: $('<a>', {
            id: `npredicted_${actionID}`,
            class: 'action_node',
            on: {
                click: function () {
                    showhide(`predicted_${actionID}`);
                }
            },
            append: predictionsSpan
        }),
    });

    let predicitionsDiv = $('<div>', {
        id: `predicted_${actionID}`,
        style: 'display:none; margin:20px;'
    }).appendTo($(wrapper));

    $.ajax({
        url: targetSF + '/sfapi/machinelearning/predict_training_data',
        type: 'POST',
        async: true,
        dataType: 'text',
        data: JSON.stringify(toSubmit),
        contentType: 'application/json; charset=utf-8',
        beforeSend: function (jqXHR, settings) {
            //remove old wrpapper
            $(`#predictions_node_wrapper${actionID}`).remove();
            wrapper.prependTo('#' + actionBodyDivID);

            setTimeout(function () {
                if (jqXHR.state() != 'pending') return;

                $('<div/>', {
                    class: 'ml_loader'
                }).prependTo('#' + actionBodyDivID);
            }, 250);
        },
        success: function (data, textStatus, jqXHR) {
            parseAjaxResponse(JSON.parse(data), idToNameMap, actionID, predicitionsDiv, predictionsSpan);
        },
        error: function (jqXHR, textStatus, errorThrown) {

            $('<pre>', {
                text: jqXHR.responseText || 'Target SF is unreachable or unknown error appear'
            }).appendTo(predicitionsDiv);

            $(predictionsSpan).text('Predictions: ' + textStatus).addClass('action_failed');
        }
    }).always(function () {
        //finally
        $(`#${actionBodyDivID}`).find('div.ml_loader').remove();
    });
}

/**
 * Parses JSON response, sort and redner it on wrapper
 * @param {*} response
 * @param {*} idToNameMap
 * @param {*} actionID
 * @param {*} predicitionsDiv jquery element for render responses
 */
function parseAjaxResponse(response, idToNameMap, actionID, predicitionsDiv, predictionsSpan) {

    let count = 10; //max show result

    class Predict {
        constructor(id, messageName, classValue, percentage) {
            this.id = id; this.messageName = messageName;
            this.classValue = classValue; this.percentage = percentage;
        }
    }

    let sortable = [];
    for (let prediction in response) {
        
        let predictionObj = response[prediction];
        let percentage = parseFloat(predictionObj['true']) * 100;
        let messageName = idToNameMap.get(parseInt(prediction));
        /*if (predictionObj.classValue == 'false' || percentage < 50) {
            continue;
        }*/

        sortable.push(new Predict(prediction, messageName, predictionObj.classValue, percentage));
    }

    $(predictionsSpan).text(`Predictions: ${sortable.length} PCs`).addClass('action_passed');

    sortable.sort(function (a, b) {
        return b.percentage - a.percentage;
    });

    if (sortable.length > count) {
        sortable =  sortable.slice(0, count)
    }

    let rgbColors = {
        100: 'FF8080',
        95: 'FF8787',
        90: 'FF8F8F',
        85: 'FF9696',
        80: 'FF9E9E',
        75: 'FFA6A6',
        70: 'FFADAD',
        65: 'FFB5B5',
        60: 'FFBDBD',
        55: 'FFC4C4',
        50: 'FFCCCC'
    }

    let predicitionsTable = $('<table>', {
        class: 'intable',
        append: $('<thead/>', {
            style: 'background: #ccc;',
            append: $('<tr/>', { style: 'font-weight: bold;' }).append(
                $('<th/>', { text: 'Probability', style: 'width: 150px'}),
                $('<th/>', { text: 'Human readable message' }),
                $('<th/>', { text: 'Links', style: 'width: 180px' })
            )
        })
    }).appendTo($(predicitionsDiv));

    let predicitionsTableBody = $('<tbody/>').appendTo($(predicitionsTable));

    for (let item of sortable) {

        /*if (item.classValue === 'false') {
            continue;
        }*/

        let humanMessage = $(`#row_msg_${item.id}`).find('.Content').find('pre').text();
        let verifSpan = $(`span[data-msgid='${item.id}']`);
        let verificationExists = false;
        if (verifSpan.length) {
            verificationExists = true;
        }

        let targetBackgroundColor = rgbColors[item.percentage - item.percentage % 5] || 'CBD8D0';
        let rgbBackground = `#${targetBackgroundColor}`

        if (item.percentage < 50) {
            item.percentage += ' (out of scope)';
        }

        let verifDivId;
        if (verificationExists) {
            verifDivId = verifSpan.parent('a').attr('id').slice(1);
            let td = verifSpan.parent('a').parent('td');
            $(td).children(`#prediction_percentage_${verifDivId}`).remove()
            let proba = $('<a/>', {
                id: `prediction_percentage_${verifDivId}`,
                text: item.percentage,
                style: `background-color: ${rgbBackground}; padding: 10px; color: white; font-weight: bold;`
            }).append($('<span/>', {text: '%', style: 'opacity: 0.5'}))
            td.append(proba)
        }

        $('<tr/>').append(
            $('<td/>', {
                text: item.percentage,
                style: `background-color: ${rgbBackground}; text-align: center; color: white; font-weight: bold;`,
                append: $('<span/>', {text: '%', style: 'opacity: 0.5'})
            }),
            $('<td/>', {
                append: $('<pre/>', { text: humanMessage || 'debug text', style: 'padding: 5px' })
            }),
            $('<td/>', {
                style: 'text-align: center',
                append: $('<a>', {
                    class: 'prediction_result',
                    on: {
                        click: function () {
                            if (!verificationExists) { return; }

                            let verifDiv = $('#' + verifDivId);
                            if (isHidden(verifDiv.get(0))) {
                                showhide(verifDivId);
                            }
                            verifDiv.get(0).scrollIntoView();
                            verifDiv.addClass('highlight');
                            setTimeout(function () {
                                verifDiv.removeClass('highlight');
                            }, 1000);
                        }
                    },
                    text: `️Verification`
                })
            }).append($('<a>', {
                class: 'prediction_result',
                href: `#row_msg_${item.id}`,
                on: {
                    click: function () {
                        highligtMessage(item.id);
                    }
                },
                text: `Table messages`
            }))
        ).appendTo($(predicitionsTableBody));
    }
}

function highligtMessage(messageId) {

    checkMessagesTable();
    $('#row_msg_' + messageId).addClass('highlight');
    setTimeout(function() {
        $('#row_msg_' + messageId).removeClass('highlight');
    }, 1000);
}

function submitSelection() {

    for (var i = 0; i < messagesJsons.length; i++) {
        var message = messagesJsons[i];

        if (problemExplanations.includes(message.id)) {
            message.problemExplanation = 'true';
        }
    }

    var toSubmit = JSON.parse(JSON.stringify(collectingActionJson));
    toSubmit['actuals'] = messagesJsons;
    toSubmit.user = user == null ? 'null' : user;
    var xmlhttp = new XMLHttpRequest();
    xmlhttp.open('POST', targetSF + '/sfapi/machinelearning/store_training_data', true);

    xmlhttp.onreadystatechange = function (oEvent) {
        if (xmlhttp.readyState === 4) {
            var message;
            if (xmlhttp.status != 200) {
                message = xmlhttp.responseText === '' ? 'Target SF is unreachable or unknown error appear' : xmlhttp.responseText;
                showStatusDialog("Data send error", message);
            } else {
                message = xmlhttp.responseText === '' ? 'Data was successfully sent' : xmlhttp.responseText;
                showStatusDialog("Data send successfully", message);
            }
        }
    };

    xmlhttp.setRequestHeader('Content-type', 'application/json; charset=utf-8');
    xmlhttp.send(JSON.stringify(toSubmit));

    clean();
}

function showStatusDialog(title, message) {

    let msgBox = $('<div>', {
        title: title,
        id: 'dialog-message'
    })

    try {
        let jsonMessage = JSON.parse(message)
        $('<p>', {text : jsonMessage.message}).appendTo(msgBox)
        for (let err of jsonMessage.errors) {
            $('<p>', {text : err}).appendTo(msgBox)
        }
    } catch (error) {
        msgBox.append($('<p>', { text: message }))
    }

    msgBox.dialog({
        modal: true,
        autoOpen: true,
        buttons: {
            Ok: function () {
                $(this).dialog("close");
            }
        },
        width:'auto',
        height: 'auto'
    });
}


function clearSelection() {
    problemExplanations = new Array();

    explanationCheckboxesState.forEach( (value, key, map) => {
        document.getElementById(key).checked = false;
        explanationCheckboxesState.set(key, false);
    });

    updateButtons();
}

function clean() {
    document.getElementById(activeActionCheckboxId).checked = false;
    problemExplanations = new Array();
    updateButtons();
    reportState = 'usual';
    collectingActionID = null;
    collectingActionJson = null;

    for (var i = 0; i < createdElements.length; i++) {
        var toDelete = document.getElementById(createdElements[i]);
        toDelete.parentNode.removeChild(toDelete);
    }

    createdElements = new Array();
    explanationCheckboxes = new Array();
    explanationCheckboxesState = new Map();
    messagesJsons = new Array();
    activeActionCheckboxId = null;
}