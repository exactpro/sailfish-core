/******************************************************************************
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.itch.soup;

import com.exactpro.sf.services.itch.ITCHCodecSettings;

public class MoldUdpCodecSettings extends ITCHCodecSettings {

    private boolean parseMessageLengthAsSeparateMessage;

    public MoldUdpCodecSettings(boolean parseHeaderAsSeparateMessage) {
        this.parseMessageLengthAsSeparateMessage = parseHeaderAsSeparateMessage;
    }

    public MoldUdpCodecSettings() {
        this(false);
    }

    public void setParseMessageLengthAsSeparateMessage(boolean parseMessageLengthAsSeparateMessage) {
        this.parseMessageLengthAsSeparateMessage = parseMessageLengthAsSeparateMessage;
    }

    public boolean isParseMessageLengthAsSeparateMessage() {
        return parseMessageLengthAsSeparateMessage;
    }
}
