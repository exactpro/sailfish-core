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
(function () {
    var context = getContextPath() + "/",

        channel = new Channel(context + "polling", false);

    function channelConnect() {
        channel.connect();
    };

    function sendServiceUpdateRequest() {
        if (channel !== null) {
            channel.sendRequest(MessageFactory.get().create('com.exactpro.sf.testwebgui.notifications.messages.ServiceUpdateRequest'));
        }
    };

    function registerHandlers() {

        channel.addHandler("com.exactpro.sf.testwebgui.notifications.messages.ServiceUpdateResponse", function (val) {
            var curEnvName = $(selectedEnvironment_label).text();
            var envName = val.envName;
            if (curEnvName == envName || (curEnvName == 'Default Environment' && envName == 'default')) {
                updateTable();
            }
        });

        channel.addHandler("com.exactpro.sf.testwebgui.notifications.messages.EnvironmentUpdateResponse", function (val) {
            var status = val.status;
            var envName = val.envName;
            var newEnvName = val.newEnvName;
            var curEnvName = $(selectedEnvironment_label).text();

            if (status == "DELETED" && curEnvName == envName) {
                changeCurrentEnvironment([{name: "currentEnvironment", value: "default"}]);
            }

            if (status == "RENAMED" && curEnvName == envName) {
                changeCurrentEnvironment([{name: "currentEnvironment", value: newEnvName}]);
            }

            updateRightEnvMenuBar();
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
        });
    };

    $(document).ready(function () {
        registerHandlers();
        channelConnect();
        sendServiceUpdateRequest();
    });
})();
