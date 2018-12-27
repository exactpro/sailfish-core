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
function loggerFactory(url) {
    
    var logQueue = [];
    var protocol = 'https:' === document.location.protocol ? 'https://' : 'http://';
    var http = null;
    var logInterval = 1000;
    var sendTaskId = -1;
    var normalizeUrl = /([^:]\/)\/+/g; // removes unnecessary / from url (http://localhost:8080//sfapi/editor//log >> http://localhost:8080/sfapi/editor/1720/log)

    var sendLogs = function() {

        var bin, tmp;
        if (http === null && logQueue.length > 0) {
            
            tmp = logQueue;
            logQueue = [];
            bin = JSON.stringify(tmp);
            let fullUrl = protocol + window.location.host + '/' + url;
            fullUrl = fullUrl.replace(normalizeUrl, "$1");
            http = new XMLHttpRequest();
            http.open("POST", fullUrl, true);
            http.setRequestHeader("Content-type", "application/json");
            http.timeout = 5000;
            http.onreadystatechange = function() {
                if (http.readyState === 4) {
                    if (http.status !== 200) {
                        var data, _i, _len;
                        tmp.reverse();
                        for (_i = 0, _len = tmp.length; _i < _len; _i++) {
                            data = tmp[_i];
                            logQueue.unshift(data);
                        }
                        logInterval = logInterval + 1000;
                    } else {
                        logInterval = 1000;
                    }
                    http = null;
                    sendTaskId = setTimeout(sendLogs, logInterval);
                }
            };
            
            http.send(bin);
            
        } else {
            
            sendTaskId = -1;
            
        }
        
    };
    
    var log = function(level, message, stacktrace) {
        var i, len, msg, date, dateStr;
        
        if (typeof message === 'object') {
            if (typeof message.stack === 'string') {
                stacktrace = message.stack.split('\n');
            }
            message = '' + message;
        }
    
        date = new Date();
        dateStr = ('' + date).split(' ').splice(0, 5).join(' ') + '.' + (date.getTime() % 1000);
    
        msg = dateStr + " " + message;
        if (typeof console !== 'undefined') {
            if (typeof stacktrace !== 'undefined') {
                console.error(msg);
                for (i = 0, len = stacktrace.length; i<len; i++) {
                    console.log(stacktrace[i]);
                }
            } else {
                console.log(msg)
            }
        }
        
        logQueue.push({
            message: msg,
            level: level,
            stacktrace: stacktrace || []
        });
        
        if (sendTaskId === -1) {
            sendTaskId = setTimeout(sendLogs, logInterval);
        }
    };
    
    return {
    	log: log
    }
}