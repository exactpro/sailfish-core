/******************************************************************************
 * Copyright 2009-2024 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.itch;

import com.exactpro.sf.services.itch.soup.SoupTcpCodecSettings;

public class SOUPVisitorSettings extends ITCHVisitorSettings {
    public static SOUPVisitorSettings from(SoupTcpCodecSettings codecSettings) {
        SOUPVisitorSettings settings = new SOUPVisitorSettings();
        settings.setTrimLeftPaddingEnabled(codecSettings.isTrimLeftPaddingEnabled());
        return settings;
    }
}