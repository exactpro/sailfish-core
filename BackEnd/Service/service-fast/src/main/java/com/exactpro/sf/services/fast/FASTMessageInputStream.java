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
package com.exactpro.sf.services.fast;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openfast.Context;
import org.openfast.Message;
import org.openfast.MessageBlockReader;
import org.openfast.MessageHandler;
import org.openfast.MessageStream;
import org.openfast.codec.FastDecoder;
import org.openfast.template.MessageTemplate;
import org.openfast.template.TemplateRegisteredListener;
import org.openfast.template.TemplateRegistry;

public class FASTMessageInputStream implements MessageStream {
    private InputStream in;
    private FastDecoder decoder;
    private Context context;
    private Map<MessageTemplate, MessageHandler> templateHandlers = Collections.emptyMap();
    private List<MessageHandler> handlers = Collections.emptyList();
    private MessageBlockReader blockReader = MessageBlockReader.NULL;

    public FASTMessageInputStream(InputStream inputStream) {
        this(inputStream, new Context());
    }

    public FASTMessageInputStream(InputStream inputStream, Context context) {
        this.in = inputStream;
        this.context = context;
        this.decoder = new FastDecoder(context, in);
    }

    /**
     * @throws java.io.EOFException
     * @return the next message in the stream
     */
    public Message readMessage(int skipInitialByteAmount) {
        if (context.isTraceEnabled())
            context.startTrace();
        boolean keepReading = blockReader.readBlock(in);
        if (!keepReading)
            return null;
        skipInitialByte(in, skipInitialByteAmount);
        Message message = decoder.readMessage();
        if (message == null) {
            return null;
        }
        blockReader.messageRead(in, message);
        if (!handlers.isEmpty()) {
            for (int i = 0; i < handlers.size(); i++) {
                ((MessageHandler) handlers.get(i)).handleMessage(message, context, decoder);
            }
        }
        if (templateHandlers.containsKey(message.getTemplate())) {
            MessageHandler handler = (MessageHandler) templateHandlers.get(message.getTemplate());
            handler.handleMessage(message, context, decoder);
            return readMessage(skipInitialByteAmount);
        }
        return message;
    }

    public void registerTemplate(int templateId, MessageTemplate template) {
        context.registerTemplate(templateId, template);
    }

    @Override
    public void close() {
        try {
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream getUnderlyingStream() {
        return in;
    }

    @Override
    public void addMessageHandler(MessageTemplate template, MessageHandler handler) {
        if (templateHandlers == Collections.EMPTY_MAP) {
            templateHandlers = new HashMap<>();
        }
        templateHandlers.put(template, handler);
    }

    @Override
    public void addMessageHandler(MessageHandler handler) {
        if (handlers == Collections.EMPTY_LIST) {
            handlers = new ArrayList<>(4);
        }
        handlers.add(handler);
    }

    public void setTemplateRegistry(TemplateRegistry registry) {
        context.setTemplateRegistry(registry);
    }

    @Override
    public TemplateRegistry getTemplateRegistry() {
        return context.getTemplateRegistry();
    }

    public void addTemplateRegisteredListener(TemplateRegisteredListener templateRegisteredListener) {}

    public void reset() {
        decoder.reset();
    }

    public Context getContext() {
        return context;
    }

    public void setBlockReader(MessageBlockReader messageBlockReader) {
        this.blockReader = messageBlockReader;
    }

    protected void skipInitialByte(InputStream in, int amount) {
        try {
            for(int i = 0; i < amount; i++) {
                in.read();
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
