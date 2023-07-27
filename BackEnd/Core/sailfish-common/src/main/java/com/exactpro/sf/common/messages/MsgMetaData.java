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
package com.exactpro.sf.common.messages;

import com.exactpro.sf.common.messages.impl.Metadata;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

import static com.exactpro.sf.common.messages.MetadataExtensions.getDictionaryUri;
import static com.exactpro.sf.common.messages.MetadataExtensions.getName;
import static com.exactpro.sf.common.messages.MetadataExtensions.getNamespace;
import static com.exactpro.sf.common.messages.MetadataExtensions.getPreciseTimestamp;
import static com.exactpro.sf.common.messages.MetadataExtensions.getTimestamp;
import static com.exactpro.sf.common.messages.MetadataExtensions.setDictionaryUri;
import static com.exactpro.sf.common.messages.MetadataExtensions.setId;
import static com.exactpro.sf.common.messages.MetadataExtensions.setName;
import static com.exactpro.sf.common.messages.MetadataExtensions.setNamespace;
import static com.exactpro.sf.common.messages.MetadataExtensions.setPreciseTimestamp;
import static com.exactpro.sf.common.messages.MetadataExtensions.setSequence;
import static com.exactpro.sf.common.messages.MetadataExtensions.setTimestamp;
import static com.exactpro.sf.common.messages.MetadataProperty.ID;

/**
 * To be removed in the next release. Please use {@link IMetadata} instead
 */
@Deprecated
@JsonDeserialize(using = MsgMetaDataDeserializer.class)
public class MsgMetaData extends Metadata {
    public MsgMetaData(IMetadata metadata) {
        Objects.requireNonNull(metadata, "metadata is null");

        for (String key : metadata.getKeys()) {
            set(key, metadata.get(key));
        }
    }

    public MsgMetaData(String namespace, String name, Instant msgTimestamp, long id, long sequence) {
        setNamespace(this, namespace);
        setName(this, name);
        setPreciseTimestamp(this, msgTimestamp);
        setTimestamp(this, Date.from(msgTimestamp));
        setId(this, id);
        setSequence(this, sequence);
    }

    public MsgMetaData(String namespace, String name, Instant msgTimestamp) {
        this(namespace, name, msgTimestamp, MessageUtil.generateId(), MessageUtil.generateSequence());
    }

    public MsgMetaData(String namespace, String name) {
        this(namespace, name, Instant.now(), MessageUtil.generateId(), MessageUtil.generateSequence());
    }

    public MsgMetaData(String namespace, String name, long id) {
        this(namespace, name, Instant.now(), id, MessageUtil.generateSequence());
    }

    public long getId() {
        return MetadataExtensions.getId(this);
    }

    @Nullable
    public Long getSequence() {
        return MetadataExtensions.getSequence(this);
    }

    @Nullable
    public String getFromService() {
        return MetadataExtensions.getFromService(this);
    }

    public void setFromService(@Nullable String fromService) {
        MetadataExtensions.setFromService(this, fromService);
    }

    @Nullable
    public String getToService() {
        return MetadataExtensions.getToService(this);
    }

    public void setToService(@Nullable String toService) {
        MetadataExtensions.setToService(this, toService);
    }

    public boolean isAdmin() {
        return MetadataExtensions.isAdmin(this);
    }

    public void setAdmin(boolean isAdmin) {
        MetadataExtensions.setAdmin(this, isAdmin);
    }

    public boolean isRejected() {
        return MetadataExtensions.isRejected(this);
    }

    @Deprecated
    /**
     * @deprecated please use more userfriendly setRejectReason, it automatically set isRejected when reason non-null
     */
    public void setRejected(boolean isRejected) {
        MetadataExtensions.setRejected(this, isRejected);
    }

    public boolean isDirty() {
        return MetadataExtensions.isDirty(this);
    }

    public void setDirty(boolean dirty) {
        MetadataExtensions.setDirty(this, dirty);
    }

    /**
     *
     * @deprecated This method is no longer recommended to use as Date gives only milliseconds precision.
     * Use {@link #getPreciseMsgTimestamp()} instead.
     */
    @Deprecated
    public Date getMsgTimestamp() {
        return getTimestamp(this);
    }

    public Instant getPreciseMsgTimestamp() {
        return getPreciseTimestamp(this);
    }

    public String getMsgNamespace() {
        return getNamespace(this);
    }

    public String getMsgName() {
        return getName(this);
    }

    @Nullable
    public byte[] getRawMessage() {
        return MetadataExtensions.getRawMessage(this);
    }

    public void setRawMessage(byte[] value) {
        MetadataExtensions.setRawMessage(this, value);
    }

    @Nullable
    public ServiceInfo getServiceInfo() {
        return MetadataExtensions.getServiceInfo(this);
    }

    public void setServiceInfo(@Nullable ServiceInfo serviceInfo) {
        MetadataExtensions.setServiceInfo(this, serviceInfo);
    }

    @Nullable
    public SailfishURI getDictionaryURI() {
        return getDictionaryUri(this);
    }

    public void setDictionaryURI(@Nullable SailfishURI dictionaryURI) {
        setDictionaryUri(this, dictionaryURI);
    }

    @Nullable
    public String getProtocol() {
        return MetadataExtensions.getProtocol(this);
    }

    public void setProtocol(@Nullable String protocol) {
        MetadataExtensions.setProtocol(this, protocol);
    }

    @Nullable
    public String getRejectReason() {
        return MetadataExtensions.getRejectReason(this);
    }

    public void setRejectReason(@Nullable String rejectReason) {
        MetadataExtensions.setRejectReason(this, rejectReason);
    }

    // FIXME: find all the usages of MsgMetaData.clone() and copy id as well if it doesn't break anything
    @Override
    public MsgMetaData clone() {
        MsgMetaData metaData = new MsgMetaData(super.clone());

        metaData.set(ID.getPropertyName(), MessageUtil.generateId());

        byte[] rawMessage = getRawMessage();

        if (rawMessage != null) {
            metaData.setRawMessage(Arrays.copyOf(rawMessage, rawMessage.length));
        }

        return metaData;
    }
}
