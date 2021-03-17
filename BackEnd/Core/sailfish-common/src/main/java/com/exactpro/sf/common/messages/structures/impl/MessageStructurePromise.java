/*
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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
 */
package com.exactpro.sf.common.messages.structures.impl;

import static com.exactpro.sf.common.messages.structures.StructureType.COMPLEX;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.jetbrains.annotations.NotNull;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.StructureType;

public class MessageStructurePromise implements IMessageStructure {
    private final AtomicReference<IMessageStructure> origin = new AtomicReference<>();
    private final String name;

    public MessageStructurePromise(@NotNull String name) {
        this.name = Objects.requireNonNull(name, "Name can't be null");
    }

    public void setOrigin(@NotNull IMessageStructure messageStructure) {
        if (!name.equals(messageStructure.getName())) {
            throw new IllegalArgumentException("Origin message structure has incorrect name, expected '" + name + "', actual '" + messageStructure.getName() + '\'');
        }
        if (!origin.compareAndSet(null, Objects.requireNonNull(messageStructure, "Origin message strucure can not be null"))) {
            throw new IllegalStateException("Origin message for promise '" + getName() + "' already set");
        }
    }

    private IMessageStructure getOrigin() {
        IMessageStructure msgStrucuture = origin.get();
        if (msgStrucuture == null) {
            throw new IllegalStateException("Origin message for promise '" + getName() + "' isn't set yet");
        }
        return msgStrucuture;
    }

    @Override
    public boolean isComplex() {
        return true;
    }

    @Override
    public boolean isEnum() {
        return false;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return getOrigin().getDescription();
    }

    @Override
    public Map<String, IFieldStructure> getFields() {
        return getOrigin().getFields();
    }

    @Override
    public Map<String, IAttributeStructure> getAttributes() {
        return getOrigin().getAttributes();
    }

    @Override
    public Map<String, IAttributeStructure> getValues() {
        throw new UnsupportedOperationException("Messages don't have values. Message: " + getName());
    }

    @Override
    public StructureType getStructureType() {
        return COMPLEX;
    }

    @Override
    public JavaType getJavaType() {
        throw new UnsupportedOperationException("Messages don't have a java type. Message: " + getName());
    }

    @Override
    public boolean isRequired() {
        return getOrigin().isRequired();
    }

    @Override
    public boolean isCollection() {
        return getOrigin().isCollection();
    }

    @Override
    public boolean isServiceName() {
        throw new UnsupportedOperationException("Messages don't have a 'serviceName' parameter. Message: " + getName());
    }

    @Override
    public <T> T getDefaultValue() {
        throw new UnsupportedOperationException("Messages don't have a default value. Message: " + getName());
    }

    @Override
    public String getNamespace() {
        return getOrigin().getNamespace();
    }

    @Override
    public String getReferenceName() {
        return getOrigin().getReferenceName();
    }
}
