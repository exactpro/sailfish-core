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
const $ = require('jquery');
import Base from './Base.js';

class Channels extends Base {
    constructor(...args) {
        super(...args);

        let contextPath = window.getContextPath() + '/';

        this.listenExecutionId = -1;
        this.listener = Function.prototype;
        this.channel = new Channel(contextPath + "polling", false);

        $.ajaxSetup({
            cache : false
        });

        this.channel.addHandler('onScriptrunnerUpdateResponse', (val) => {
            if (val.scriptRunId == this.listenExecutionId) {
                $('#scriptRunResult').html(val.formattedString);
                this.listener(val);
            }
        });

        // this.channel.addHandler('onMatrixUpdateResponse', (val) => {
        // });

        // this.channel.addHandler('onConnectionStatus', (val) => {
        // });

        // this.channel.addHandler('onCloseChannel', (val) => {
        // });

        this.channelConnect();
        this.sendUpdateRequest();
    }

    channelConnect() {
        this.channel.connect();
    }

    sendUpdateRequest() {
        this.channel.sendRequest(
            MessageFactory.get().create('ScriptrunnerUpdateRequest')
        );
    }

    setListenExecutionId(newId) {
        this.listenExecutionId = newId;
    }

    setListener(newListener) {
        this.listener = newListener;
    }
}

export const TheChannel = new Channels;
export default TheChannel;
