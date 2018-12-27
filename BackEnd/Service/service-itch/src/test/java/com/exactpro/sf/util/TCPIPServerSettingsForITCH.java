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
package com.exactpro.sf.util;

import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.services.itch.ITCHCodecSettings;
import com.exactpro.sf.services.itch.ITCHMessageHelper;
import com.exactpro.sf.services.tcpip.TCPIPServerSettings;

public class TCPIPServerSettingsForITCH extends TCPIPServerSettings {
    private static final long serialVersionUID = -4453030398800797263L;

    @Override
    public ICommonSettings createCodecSettings() {
        return getCodecSettings();
    }

    private ICommonSettings getCodecSettings() {
        IDictionaryManager dictionaryManager = SFLocalContext.getDefault().getDictionaryManager();
        ITCHCodecSettings settings = new ITCHCodecSettings();
        Integer lengthSize = ITCHMessageHelper.extractLengthSize(dictionaryManager.getDictionary(getDictionaryName()));
        if (lengthSize != null) {
            settings = new ITCHCodecSettings();
            settings.setMsgLength(lengthSize);
        }
        return settings;
    }
}
