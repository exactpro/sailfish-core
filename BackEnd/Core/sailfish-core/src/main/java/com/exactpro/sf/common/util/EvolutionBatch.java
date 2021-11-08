/*******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.sf.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MetadataProperty;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.google.common.collect.ImmutableSet;

public class EvolutionBatch {

    public static final String MESSAGE_NAME = "EvolutionBatch";
    private static final String BATCH_FIELD = "Batch";
    private static final ImmutableSet<MetadataProperty> MERGE_PROPERTIES = ImmutableSet.of(MetadataProperty.TO_SERVICE, MetadataProperty.SERVICE_INFO);
    private final List<IMessage> batch;

    public EvolutionBatch(int size) {
        this(new ArrayList<>(size));
    }

    public EvolutionBatch(IMessage message) {
        this(extractBatch(message));
    }

    EvolutionBatch(List<IMessage> batch) {
        this.batch = ObjectUtils.defaultIfNull(batch, new ArrayList<>());
    }

    public void addMessage(IMessage message) {
        batch.add(message);
    }

    public void addAllMessages(Collection<IMessage> messages) {
        batch.addAll(messages);
    }

    public List<IMessage> getBatch() {
        return Collections.unmodifiableList(batch);
    }

    public boolean isEmpty() {
        return batch.isEmpty();
    }

    public int size() {
        return batch.size();
    }

    public IMessage toMessage(IMessageFactory messageFactory) {
        IMessage message = messageFactory.createMessage(MESSAGE_NAME, getNamespace(messageFactory));
        MsgMetaData metadata = message.getMetaData();
        for (IMessage mes : batch) {
            mergeMetadata(metadata, mes.getMetaData());
        }
        message.addField(BATCH_FIELD, batch);
        metadata.setAdmin(true); // it is definitely not a business message
        return message;
    }

    private static void mergeMetadata(MsgMetaData firstMetadata, MsgMetaData secondMetadata) {
        for(MetadataProperty property : MERGE_PROPERTIES) {
            String propertyName = property.getPropertyName();
            Object value = secondMetadata.get(propertyName);
            if(firstMetadata.get(propertyName) == null && value != null) {
                firstMetadata.set(propertyName, value);
            }
        }
    }

    private static List<IMessage> extractBatch(IMessage message) {
        if (!MESSAGE_NAME.equals(message.getName())) {
            throw new IllegalArgumentException(String.format("Expects %s message but get %s", MESSAGE_NAME, message.getName()));
        }
        return message.getField(BATCH_FIELD);
    }

    private static String getNamespace(IMessageFactory messageFactory) {
        String namespace = messageFactory.getNamespace();
        return namespace == null ? "Service" : namespace;
    }
}
