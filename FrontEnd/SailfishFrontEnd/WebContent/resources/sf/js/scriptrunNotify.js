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
//(function() {
var context = getContextPath() + "/",
    channel;
$resultsContainer = null,
    $log = null,
    $content = null,
    $items = null,
    $warningObject = null;

var $resultsWrapper = $(".eps-test-script-results-wrapper");
var $noResultDiv = $('.eps-result-system-message');

function channelConnect() {
    channel.connect();
};

function sendMatrixUpdateRequest() {
    if (channel !== null) {
        channel.sendRequest(MessageFactory.get().create('com.exactpro.sf.testwebgui.notifications.messages.MatrixUpdateRequest'));
    }
};

function sendScriptrunUpdateRequest() {
    if (channel !== null) {
        channel.sendRequest(MessageFactory.get().create('com.exactpro.sf.testwebgui.notifications.messages.ScriptrunnerUpdateRequest'));
    }
};

function sendLogRequest() {
    if (channel !== null) {
        channel.sendRequest(MessageFactory.get().create('com.exactpro.sf.testwebgui.notifications.messages.EventUpdateRequest'));
    }
};

function toggleDetails(block_id) {
    $("#" + block_id).toggle();
};

function toogleLog() {
    if ($log.css("display") === "none") {
        sendLogRequest();
        $log.css("display", "block");
        $log.find(".ui-log-error").trigger("click");
    } else {
        $log.css("display", "none");
    }
    ;
};

function onDownloadStart() {
    channel.disconnect();
    channel = null;
};

function onDownloadFinish() {
    channel = new Channel(context + "polling", false, null, null, null);
    registerHandlers();
    channelConnect();
    sendScriptrunUpdateRequest();
    sendMatrixUpdateRequest();
};

function registerHandlers() {

    channel.addHandler("com.exactpro.sf.testwebgui.notifications.messages.ScriptrunnerUpdateResponse", function (val) {
        var id = val.divID;

        if (id === 'empty') {
            reloadTestScriptForm();
            return;
        }

        if ($noResultDiv.length) {
            $noResultDiv.remove();
        }

        var html = val.formattedString;

        var $div = $('#' + id);

        //check existing element
        if ($div.length) {
            $div.replaceWith(html);
        } else {
            $resultsWrapper.prepend(html);
        }

    });

    var interval = -1;
    var counterMax = 5;
    var counter = counterMax;

    function downCount() {
        interval = setInterval(function () {
            counter--;
            if (counter <= 0) {
                clearInterval(interval);
                interval = -1;
                counter = counterMax;
                updateMatricesTable();

                ////todo This is hack made due with https://code.google.com/p/primefaces/issues/detail?id=7046
                $("[id$='_menu']").remove();
            }
        }, 100);
    }

    channel.addHandler("com.exactpro.sf.testwebgui.notifications.messages.MatrixUpdateResponse", function (val) {
        counter = counterMax;

        if (interval === -1) {
            downCount();
        }

    });

    channel.addHandler("com.exactpro.sf.testwebgui.notifications.messages.CloseChannel", function (val) {
        console.log(val);
    });

    channel.addHandler("com.exactpro.sf.testwebgui.notifications.messages.NotifyResponse", function (val) {
        console.log(val);
    });

    channel.addHandler("com.exactpro.sf.testwebgui.notifications.messages.EventUpdateResponse", function (val) {
        console.log(val);
        switch (val["level"]) {
            case "ERROR":
                PrimeFaces.error(val["message"]);
                break;
            case "WARN":
                PrimeFaces.warn(val["message"]);
                break;
            case "INFO":
                PrimeFaces.info(val["message"]);
                break;
            case "DEBUG":
                PrimeFaces.debug(val["message"]);
                break;
            default:
        }
        ;

        $content.scrollTop($items.height());

    });

};

function createChannel() {
    channel = new Channel(context + "polling", false);
    $resultsContainer = $(".eps-results-wrapper");
    $log = $("#log");
    $content = $log.find(".ui-log-content");
    $items = $content.find(".ui-log-items");
    $warningObject = $("div.eps-channel-warning");

    $.ajaxSetup({
                    cache: false
                });

    channel.addHandler("error", function () {
        $warningObject.css("display", "block");
        $warningObject.html("Connection problems (Please Press F5 or click Refresh)");
    });

    channel.addHandler("success", function () {
        $warningObject.css("display", "none");
    });

    registerHandlers();

    channelConnect();
    sendScriptrunUpdateRequest();
    sendMatrixUpdateRequest();
}

$(document).ready(function () {
    createChannel();
});

//TODO Hack for rm44309. The problem is deeper, channel loses connection after some idle time
$(window).focus(function () {
    if (channel !== null) {
        channel.disconnect();
    }
    createChannel();
});

$(window).bind("beforeunload", function () {
    $warningObject.remove();
    if (channel !== null) {
        channel.disconnect();
        channel = null;
    }
});

//})();
