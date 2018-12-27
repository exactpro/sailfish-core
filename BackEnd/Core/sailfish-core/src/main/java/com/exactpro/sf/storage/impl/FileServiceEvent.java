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
import java.util.Date;
import java.util.Objects;

import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.services.ServiceEvent;
import com.exactpro.sf.storage.ISerializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FileServiceEvent extends ServiceEvent {
    private final transient ISerializer<FileServiceEvent> serializer;
    private final transient File file;
    private final transient long lastModified;

    private transient boolean loaded = true;

    public FileServiceEvent(ServiceEvent event) {
        this(event.getServiceName(), event.getLevel(), event.getType(), event.getOccurred(), event.getMessage(), event.getDetails());
    }

    public FileServiceEvent(ISerializer<FileServiceEvent> serializer, File file) {
        super(null, null, null, null, null, null);
        this.serializer = Objects.requireNonNull(serializer, "serializer cannot be null");
        this.file = Objects.requireNonNull(file, "file cannot be null");
        this.lastModified = file.lastModified();
        this.loaded = false;
    }

    @JsonCreator
    public FileServiceEvent(@JsonProperty("serviceName") ServiceName serviceName,
            @JsonProperty("level") Level level,
            @JsonProperty("type") Type type,
            @JsonProperty("occurred") Date occurred,
            @JsonProperty("message") String message,
            @JsonProperty("details") String details) {
        super(serviceName, level, type, occurred, message, details);
        this.serializer = null;
        this.file = null;
        this.lastModified = System.currentTimeMillis();
        this.loaded = true;
    }

    @Override
    public Type getType() {
        load();
        return type;
    }

    @Override
    public String getMessage() {
        load();
        return message;
    }

    @Override
    public Date getOccurred() {
        load();
        return occurred;
    }

    @Override
    public String getDetails() {
        load();
        return details;
    }

    @Override
    public Level getLevel() {
        load();
        return level;
    }

    @Override
    public ServiceName getServiceName() {
        load();
        return serviceName;
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
            FileServiceEvent event = serializer.deserialize(file);

            this.serviceName = event.serviceName;
            this.type = event.type;
            this.level = event.level;
            this.occurred = event.occurred;
            this.message = event.message;
            this.details = event.details;
        } catch(Exception e) {
            throw new EPSCommonException("Failed to load file service event: " + file.getAbsolutePath(), e);
        }

        loaded = true;
    }
}
