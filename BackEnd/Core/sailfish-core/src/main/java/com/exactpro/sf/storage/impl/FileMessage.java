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
package com.exactpro.sf.storage.impl;

import java.io.File;
import java.sql.Timestamp;
import java.util.Objects;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.storage.ISerializer;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class FileMessage {
    private String namespace;
    private String name;
    private String from;
    private String to;
    private Timestamp timestamp;
    private boolean admin;
    private String humanMessage;
    private String jsonMessage;
    private byte[] rawMessage;
    private String rejectReason;
    private String serviceID;
    private long id;
    private long metaDataID;

    private final transient ISerializer<FileMessage> serializer;
    private final transient File file;
    private final transient long lastModified;
    private transient boolean loaded = true;

    public FileMessage() {
        this.serializer = null;
        this.file = null;
        this.lastModified = System.currentTimeMillis();
        this.loaded = true;
    }

    public FileMessage(ISerializer<FileMessage> serializer, File file) {
        this.serializer = Objects.requireNonNull(serializer, "serializer cannot be null");
        this.file = Objects.requireNonNull(file, "file cannot be null");
        this.lastModified = file.lastModified();
        this.loaded = false;
    }

    public String getNamespace() {
        load();
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getName() {
        load();
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFrom() {
        load();
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        load();
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public Timestamp getTimestamp() {
        load();
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isAdmin() {
        load();
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public String getHumanMessage() {
        load();
        return humanMessage;
    }

    public void setHumanMessage(String humanMessage) {
        this.humanMessage = humanMessage;
    }

    public String getJsonMessage() {
        load();
        return jsonMessage;
    }

    public void setJsonMessage(String jsonMessage) {
        this.jsonMessage = jsonMessage;
    }

    public byte[] getRawMessage() {
        load();
        return rawMessage;
    }

    public void setRawMessage(byte[] rawMessage) {
        this.rawMessage = rawMessage;
    }

    public String getServiceID() {
        load();
        return serviceID;
    }

    public void setServiceID(String serviceID) {
        this.serviceID = serviceID;
    }

    public long getID() {
        load();
        return id;
    }

    public void setID(long id) {
        this.id = id;
    }

    public long getMetaDataID() {
        load();
        return metaDataID;
    }

    public void setMetaDataID(long metaDataID) {
        this.metaDataID = metaDataID;
    }

    @JsonIgnore
    public long getLastModified() {
        return lastModified;
    }

    private void load() {
        if(loaded) {
            return;
        }

        try {
            FileMessage message = serializer.deserialize(file);

            this.namespace = message.namespace;
            this.name = message.name;
            this.from = message.from;
            this.to = message.to;
            this.timestamp = message.timestamp;
            this.admin = message.admin;
            this.humanMessage = message.humanMessage;
            this.jsonMessage = message.jsonMessage;
            this.rawMessage = message.rawMessage;
            this.serviceID = message.serviceID;
            this.id = message.id;
            this.metaDataID = message.metaDataID;
            this.rejectReason = message.rejectReason;
        } catch(Exception e) {
            throw new EPSCommonException("Failed to load file message: " + file.getAbsolutePath(), e);
        }

        loaded = true;
    }

    public String getRejectReason() {
        load();
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
}
