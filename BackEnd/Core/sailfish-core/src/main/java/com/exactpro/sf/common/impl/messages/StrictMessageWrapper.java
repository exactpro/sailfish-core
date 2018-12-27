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
package com.exactpro.sf.common.impl.messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.exactpro.sf.common.messages.FieldMetaData;
import com.exactpro.sf.common.messages.IFieldInfo;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.comparison.conversion.MultiConverter;

/**
 * This implementation of IMessage wraps up standard IMessage and checks value
 * type by dictionary during its adding to message
 * 
 * @author oleg.smirnov
 *
 */
public class StrictMessageWrapper implements IMessage {

    private final IMessageStructure messageStructure;
    private final IMessage message;

    /**
     * 
     * @param message
     * @param messageStructure
     */
    private StrictMessageWrapper(IMessage message, IMessageStructure messageStructure) {
        this.message = Objects.requireNonNull(message, "'Message' parameter");
        this.messageStructure = Objects.requireNonNull(messageStructure, "'Message structure' parameter");
        if (!message.getName().equals(messageStructure.getName())) {
            throw new IllegalArgumentException("Message name and message structure name not are same");
        }
    }

    /**
     *
     * @param messageStructure
     */
    public StrictMessageWrapper(IMessageStructure messageStructure) {
        this.messageStructure = Objects.requireNonNull(messageStructure, "'Message structure' parameter");
        message = new MapMessage(messageStructure.getName(), messageStructure.getNamespace());
    }

    /**
     *
     */
    public StrictMessageWrapper(IMessageFactory messageFactory, IMessageStructure messageStructure) {
        this.messageStructure = Objects.requireNonNull(messageStructure, "'Message structure' parameter");
        message = messageFactory.createMessage(messageStructure.getName(), messageStructure.getNamespace());
    }

    /**
     * 
     * @param value
     * @param elementClass
     * @return {@code CheckedList<T>} which check opportunity to cast the
     *         {@code value} elements to {@code elementClass} during its adding
     */
    protected <T> List<T> castValueToCollection(Object value, Class<T> elementClass) {
        List<T> list = new ArrayList<>();

        if (value instanceof List<?>) {
            for (Object element : (List<?>) value) {
                list.add(checkElement(element, elementClass));
            }
        } else {
            list.add(checkElement(value, elementClass));
        }
        return Collections.checkedList(list, elementClass);
    }

    /**
     * Adding field in message with trying to cast field's value to type that set in
     * dictionary
     */
    @Override
    public void addField(String name, Object value) {
        IFieldStructure fieldStructure = messageStructure.getField(name);
        if (fieldStructure == null) {
            message.addField(name, value);
            return;
        }

        if (value == null) {
            message.addField(name, requiedNotNull(value, fieldStructure));
            return;
        }

        if (fieldStructure.isComplex()) {
            if (fieldStructure.isCollection()) {
                this.message.addField(name, castValueToCollection(value, StrictMessageWrapper.class));
            } else {
                Object element = value;
                if (value instanceof List<?>) {
                    element = extractValueFromCollection((List<?>)value);
                }
                this.message.addField(name, checkElement(requiedNotNull(element, fieldStructure), StrictMessageWrapper.class));
            }
        } else {
            try {
                Class<?> clazz = this.getClass().getClassLoader().loadClass(fieldStructure.getJavaType().value());

                if (fieldStructure.isCollection()) {
                    this.message.addField(name, castValueToCollection(value, clazz));
                } else {
                    Object element = value;
                    if (value instanceof List<?>) {
                        element = extractValueFromCollection((List<?>)value);
                    }
                    this.message.addField(name, checkElement(requiedNotNull(element, fieldStructure), clazz));
                }
            } catch (ClassNotFoundException e) {
                throw new EPSCommonException("Unknown field type: " + fieldStructure.getJavaType().value(), e);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.exactpro.sf.common.messages.IMessage#getName()
     */
    @Override
    public String getName() {
        return this.message.getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.exactpro.sf.common.messages.IMessage#getNamespace()
     */
    @Override
    public String getNamespace() {
        return this.message.getNamespace();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.exactpro.sf.common.messages.IMessage#getMetaData()
     */
    @Override
    public MsgMetaData getMetaData() {
        return this.message.getMetaData();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.exactpro.sf.common.messages.IMessage#removeField(java.lang.String)
     */
    @Override
    public Object removeField(String name) {
        return this.message.removeField(name);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.exactpro.sf.common.messages.IMessage#getField(java.lang.String)
     */
    @Override
    public <T> T getField(String name) {
        return this.message.getField(name);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.exactpro.sf.common.messages.IMessage#getFieldMetaData(java.lang.
     * String)
     */
    @Override
    public FieldMetaData getFieldMetaData(String name) {
        return this.message.getFieldMetaData(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.exactpro.sf.common.messages.IMessage#isFieldSet(java.lang.String)
     */
    @Override
    public boolean isFieldSet(String name) {
        return this.message.isFieldSet(name);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.exactpro.sf.common.messages.IMessage#getFieldNames()
     */
    @Override
    public Set<String> getFieldNames() {
        return this.message.getFieldNames();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.exactpro.sf.common.messages.IMessage#getFieldInfo(java.lang.String)
     */
    @Override
    public IFieldInfo getFieldInfo(String name) {
        return this.message.getFieldInfo(name);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.exactpro.sf.common.messages.IMessage#cloneMessage()
     */
    @Override
    public IMessage cloneMessage() {
        return new StrictMessageWrapper(message.cloneMessage(), this.messageStructure);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.exactpro.sf.common.messages.IMessage#compare(com.exactpro.
     * common.messages.IMessage)
     */
    @Override
    public boolean compare(IMessage message) {
        return (message instanceof StrictMessageWrapper) && this.message.compare(message);
    }

    /* (non-Javadoc)
     * @see com.exactpro.sf.common.messages.IMessage#getFieldCount()
     */
    @Override
    public int getFieldCount() {
        return this.message.getFieldCount();
    }

    @Override
    public String toString() {
        StringBuilder toString = new StringBuilder(1024);

        for(String fldName : this.getFieldNames()) {
            if(toString.length() > 0) {
                toString.append('|');
            }

            if(this.getField(fldName) instanceof IMessage) {
                toString.append(((StrictMessageWrapper)this.getField(fldName)).toString());
            } else {
                toString.append(fldName);
                toString.append('=');
                toString.append(this.<Object>getField(fldName));
            }
        }

        return toString.toString();
    }
    
    /**
     * 
     * @param value
     * @param fieldStructure
     * @return value if it can be NULL, else throw Exception
     */
    private Object requiedNotNull(Object value, IFieldStructure fieldStructure) {
        if (value == null && fieldStructure.isRequired() && fieldStructure.getDefaultValue() == null) {
            throw new EPSCommonException(
                    "Requied field [" + fieldStructure.getName() + "] must have NOT NULL value or default value");
        }
        return value;
    }

    private Object extractValueFromCollection(List<?> collection) {
        switch (collection.size()) {
        case 0:
            return null;
        case 1:
            return collection.get(0);
        default:
            throw new EPSCommonException("Can't extract single value from collection cause it have more than 1 element");
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> T checkElement(Object element, Class<T> elementClass) {
        if (IMessage.class.isAssignableFrom(elementClass)) {
            if (!StrictMessageWrapper.class.isAssignableFrom(element.getClass())) {
                throw new ClassCastException(String.format("%s is not instance or subclass of %s",
                        element.getClass().getName(), StrictMessageWrapper.class.getName()));
            }
            return (T) element;
        } else {
            return MultiConverter.convert(element, elementClass);
        }
    }
}
