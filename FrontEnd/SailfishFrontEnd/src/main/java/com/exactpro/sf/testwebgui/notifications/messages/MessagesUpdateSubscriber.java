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
package com.exactpro.sf.testwebgui.notifications.messages;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.webchannels.IUpdateRequestListener;
import com.exactprosystems.webchannels.channel.AbstractChannel;

@SuppressWarnings("deprecation")
public class MessagesUpdateSubscriber implements IUpdateRequestListener {

    private static final Logger logger = LoggerFactory.getLogger(MessagesUpdateSubscriber.class);
    private final AbstractChannel channel;
    private String id;

    public MessagesUpdateSubscriber(String id, AbstractChannel channel) {
        this.id = id;
        this.channel = channel;
        logger.info("Create MessagesUpdateSubscriber {} for channel {}", new Object[] {this, channel});

    }

    @Override
    public void onEvent(Object event) {

        logger.debug("Event {} happend for MessagesUpdateSubscriber {}", event, this);

        if (event instanceof MessagesUpdateEvent) {

            MessagesUpdateEvent updateEvent = (MessagesUpdateEvent) event;
            MessagesUpdateResponse messagesUpdateResponse = new MessagesUpdateResponse();
            messagesUpdateResponse.setRequestId(id);
            messagesUpdateResponse.setMessagesUpdateEvent(updateEvent);
            channel.sendMessage(messagesUpdateResponse);
        }
    }


    @Override
    public void destroy() {
        //channel = null;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("id", id).toString();
    }
}
