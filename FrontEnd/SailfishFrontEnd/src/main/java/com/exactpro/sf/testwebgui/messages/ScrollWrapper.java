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
package com.exactpro.sf.testwebgui.messages;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import com.exactpro.sf.common.util.HexDumper;
import com.exactpro.sf.services.ITaskExecutor;
import com.exactpro.sf.storage.MessageLoader;
import com.exactpro.sf.storage.MessageRow;
import com.exactpro.sf.storage.entities.StoredMessage;
import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.SFWebApplication;
import com.exactpro.sf.testwebgui.notifications.messages.MessagesUpdateEvent;
import com.exactpro.sf.testwebgui.notifications.messages.MessagesUpdateRetriever;

/**
 * Created by alexey.zarovny on 12/12/14.
 */
public class ScrollWrapper implements Runnable {
    private MessageLoader messageLoader;
    private List<MessageAdapter> messages;
    private MessagesUpdateRetriever messagesUpdateRetriever;
    private String guiSessionId;
    private final String formatString;
    private ITaskExecutor taskExecutor;
    private Future<?> loadFuture;
    private boolean isNotNotifyGUI;

    public ScrollWrapper(String guiSessionId, String format) {
        messages = new ArrayList<>();
        this.guiSessionId = guiSessionId;
        this.formatString = format;
        messagesUpdateRetriever = (MessagesUpdateRetriever) SFWebApplication.getInstance().getMessagesUpdateRetriever();
        taskExecutor = BeanUtil.getSfContext().getTaskExecutor();
    }

    public void load(MessageLoader messageLoader) {
        if(loadFuture != null && !loadFuture.isDone()){
            loadFuture.cancel(true);
            this.messageLoader.close();
        }
        this.messageLoader = messageLoader;
        messages.clear();
        loadFuture = taskExecutor.addTask(this);
    }

    public void load(MessageLoader messageLoader, boolean isNotNotifyGUI) {
    	load(messageLoader);
    	this.isNotNotifyGUI = isNotNotifyGUI;
    }

    public boolean isLoaded(){
        if(loadFuture == null){
            return true;
        } else{
            return loadFuture.isDone();
        }
    }

    @Override
    public void run() {

        DateFormat format = new SimpleDateFormat(formatString);

        try {
            while (messageLoader.hasNext()) {
                StoredMessage msg = messageLoader.next();
                MessageRow row = new MessageRow();

                row.setID(String.valueOf(msg.getId()));
                row.setMsgName(msg.getName());
                row.setMsgNamespace(msg.getNamespace());
                row.setTimestamp(format.format(msg.getArrived()));
                row.setFrom(msg.getFrom());
                row.setTo(msg.getTo());
                row.setJson(msg.getJsonMessage());
                row.setContent(msg.getHumanMessage());

                if (msg.getRawMessage() != null) {
                    HexDumper dumper = new HexDumper(msg.getRawMessage());
                    row.setRawMessage(dumper.getHexdump());
                    row.setPrintableMessage(dumper.getPrintableString());
                } else {
                    row.setRawMessage("null");
                }
                messages.add(new MessageAdapter(row));
            }
        } finally {
            messageLoader.close();
            if(!isNotNotifyGUI) {
                notifyGui();
                isNotNotifyGUI = false;
            }
        }
    }

    protected void notifyGui(){
        MessagesUpdateEvent messagesUpdateEvent = new MessagesUpdateEvent(guiSessionId);
        messagesUpdateRetriever.onEvent(messagesUpdateEvent);
    }

    public List<MessageAdapter> getMessages() {
        return messages;
    }

    @Override
    public String toString() {
        return ScrollWrapper.class.getSimpleName();
    }
}
