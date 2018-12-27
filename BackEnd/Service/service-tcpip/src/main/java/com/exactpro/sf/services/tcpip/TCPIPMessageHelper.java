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
package com.exactpro.sf.services.tcpip;

import com.exactpro.sf.common.messages.AttributeNotFoundException;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MessageNotFoundException;
import com.exactpro.sf.services.MessageHelper;

public class TCPIPMessageHelper extends MessageHelper {
    public static final String INCOMING_MESSAGE_NAME_AND_NAMESPACE = "incoming";
    public static final String OUTGOING_MESSAGE_NAME_AND_NAMESPACE = "outgoing";
    public static final String REJECTED_MESSAGE_NAME_AND_NAMESPACE = "rejected";
    
    private final boolean depersonalizationIncomingMessages;
    
    public TCPIPMessageHelper(boolean depersonalizationIncomingMessages) {
        this.depersonalizationIncomingMessages = depersonalizationIncomingMessages;
    }
    
    public TCPIPMessageHelper() {
        this(true);
    }
    
    @Override
    public boolean isAdmin(IMessage message) throws MessageNotFoundException, AttributeNotFoundException {
        if (!depersonalizationIncomingMessages) {
            return super.isAdmin(message);
        }
        return false;
    }
}
