function setScrollableBodyHeightImpl(tableSelector, bodySelector) {
	
	var $body = $(tableSelector + ' ' + bodySelector);
	var bodyOffset		  = $body.offset().top;
	var windowHeight	  = $(window).height();
	var footerHeight	  = $('.eps-footer-wrapper').height();
	var tableFooterHeight = $('.ui-datatable-footer').height();
	var newBodyHeight = windowHeight - bodyOffset - footerHeight - tableFooterHeight - 30;

	$body.css('height', newBodyHeight);

};

function setScrollableBodyHeight(tableSelector) {
	
	setScrollableBodyHeightImpl(tableSelector, '.ui-datatable-scrollable-body');

};

function setTreeTableScrollableBodyHeight(tableSelector) {
	
	setScrollableBodyHeightImpl(tableSelector, '.ui-treetable-scrollable-body');

};

var scrollPos;

function saveScrollPos(scrollableContainerSelector) {
	scrollPos = $(scrollableContainerSelector).scrollTop();
}

function restoreScrollPos(scrollableContainerSelector) {
	$(scrollableContainerSelector).scrollTop(scrollPos);
}

$(document).ready(function() {
	   $('.ui-menuitem-link').each(function(){
	       if(window.location.pathname.indexOf($(this).attr('href')) != -1) {
	           $(this).css('background', '#779DB7');
	       }
	   });  
	})