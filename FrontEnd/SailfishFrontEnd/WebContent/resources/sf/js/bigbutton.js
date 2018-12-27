$(document).ready(function () {
    setHeight();
});

function setHeight() {
    var $progressView = $('#progress-view');
    var progressViewOffset = $progressView.offset().top;
    var windowHeight = $(window).height();
    var footerHeight = $('.eps-footer-wrapper').height();
    var newProgressViewHeight = windowHeight - progressViewOffset - footerHeight - 30;
    $progressView.css('height', newProgressViewHeight);

    var $statusColumn = $('#status-column');
    var statusColumnOffset = $statusColumn.offset().top;
    var newStatusColumnHeight = windowHeight - statusColumnOffset - footerHeight;
    $statusColumn.css('height', newStatusColumnHeight);
}

$(window).resize(function () {
    setHeight();
});

