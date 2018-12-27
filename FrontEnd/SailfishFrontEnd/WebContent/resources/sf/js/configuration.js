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

function setSectionVisible(section) {
	
	$(".eps-config-container").css("display", "none");
	
	removeActiveClass();
	
	setActiveNavItem(section + "NavItem");
	
	$("#" + section + "Config").css("display", "block");
	
	if ("logging" == section) {
		restoreLogSettings();
	}
	
	var u = window.location.href;
	var pos = u.indexOf("?");
	if (pos != -1) {
		window.history.pushState({}, "Title", u.substring(0, pos) + "?section=" + section);
	} else {
		window.history.pushState({}, "Title", window.location.href + "?section=" + section);
	}
}

function removeActiveClass(){
	$(".eps-navigation-active-item").removeClass("eps-navigation-active-item");
}

function setActiveNavItem(itemID){
	$("#"+itemID).addClass("eps-navigation-active-item");
}

function loadServiceRecord(args) {
	var svc = args.find(function (x) { return x[0] == "servicerecords" });
	if (svc != undefined) {
		PF('ndFileFilter').jq.val(svc[1]);
	}
	PF('ndFileTable').filter();
}

function parseArgs() {
	return location.search
		.substring(1)
		.split('&')
		.map(function (x) { return x.split('='); });
}

$("document").ready(function() {
    $('.eps-config-text-area')[0].setAttribute('spellcheck', false);
    onPageLoad();
	
	var a = parseArgs();
	loadServiceRecord(a);
});