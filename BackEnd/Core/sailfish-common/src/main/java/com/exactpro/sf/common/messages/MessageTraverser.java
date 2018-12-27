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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ObjectUtils;

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.StructureType;
import com.exactpro.sf.common.messages.structures.impl.FieldStructure;
import com.exactpro.sf.common.messages.structures.impl.MessageStructure;

public class MessageTraverser extends MessageStructureReader {

    private final IMessageStructure emptyMessageStructure = new MessageStructure("Empty", "Empty", false, null);
    
    public MessageTraverser() {
    }

    @Override
    public void traverse(IMessageStructureVisitor msgStrVisitor,
            IMessageStructure msgStructure,
            IMessage message, IMessageStructureReaderHandler handler) {
        if (msgStructure == null) {
            msgStructure = new MessageStructure(message.getName(), message.getNamespace(), false, emptyMessageStructure);
        }
        super.traverse(msgStrVisitor, msgStructure, message, handler);
    }

    @Override
    public void traverse(IMessageStructureVisitor msgStrVisitor, List<IFieldStructure> fields, IMessage message,
            IMessageStructureReaderHandler handler) {
        Set<String> namesByDictionary = fields.stream().map(IFieldStructure::getName).collect(Collectors.toSet());

        List<IFieldStructure> combinedFields = Stream.concat(fields.stream(),
                message.getFieldNames().stream()
                        .filter(name -> !namesByDictionary.contains(name))
                        .map(name -> createFieldStructure(message, name))
                        .filter(Objects::nonNull))
                .collect(Collectors.toList());

        super.traverse(msgStrVisitor, combinedFields, message, handler);
    }

    @Override
    protected void visitField(IFieldStructure curField, IMessageStructureVisitor msgStrVisitor, IMessageStructureReaderHandler handler,
            String fieldName, Object value) {
        curField = createFieldStructure(curField, curField.getNamespace(), fieldName, value);
        // Check unsupported type
        if (!curField.isComplex() && !curField.isCollection() && curField.getJavaType() == JavaType.JAVA_LANG_STRING &&
                value != null && !(value instanceof String)) {
            value = value.toString();
        }
        super.visitField(curField, msgStrVisitor, handler, fieldName, value);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected void visitComplexField(IFieldStructure curField, IMessageStructureVisitor msgStrVisitor, String fieldName, Object value) {
        String namespace = curField.getNamespace();
        String name = curField.getName();
        
        if (value != null) {
            if (curField.isCollection()) {
                value = ((List<Object>) value).stream()
                        .map(msg -> {
                            if (msg instanceof Map) {
                                msg = MessageUtil.convertToIMessage((Map<?, ?>) msg, DefaultMessageFactory.getFactory(), namespace, name);
                            }
                            return msg;
                        }).collect(Collectors.toList());
            } else {
                if (value instanceof Map) {
                    value = MessageUtil.convertToIMessage((Map<?, ?>)value, DefaultMessageFactory.getFactory(), namespace, curField.getName());
                }
            }
        }
        
        super.visitComplexField(curField, msgStrVisitor, fieldName, value);
    }
    
    protected IFieldStructure createFieldStructure(IFieldStructure fieldStructure, String namespace, String fieldName, Object value) {
        Optional<?> optional = Optional.ofNullable(value);
        if (optional.isPresent()) {
            boolean isCollection = false;
            JavaType javaType = null;
            StructureType structureType = (fieldStructure != null && !fieldStructure.isComplex()) ? fieldStructure.getStructureType() : StructureType.SIMPLE;

            if (optional.get() instanceof List<?>) {
                isCollection = true;
                optional = ((List<?>) optional.get()).stream()
                        .filter(Objects::nonNull).findFirst();
            }

            if (optional.isPresent()) {
                if ((optional.get() instanceof IMessage) ||
                        (optional.get() instanceof HashMap)) {
                    structureType = StructureType.COMPLEX;
                } else {
                    javaType = ObjectUtils.defaultIfNull(getJavaTypeSafe(optional.get().getClass().getCanonicalName()), JavaType.JAVA_LANG_STRING);
                }
            }

            if (fieldStructure == null 
                    || fieldStructure.isCollection() != isCollection
                    || fieldStructure.getStructureType() != structureType
                    || (structureType == StructureType.COMPLEX && javaType != null)
                    || (structureType != StructureType.COMPLEX && fieldStructure.getJavaType() != javaType)) {
                if (structureType == StructureType.COMPLEX && javaType == null) {
                    fieldStructure = new MessageStructure(fieldName, namespace, isCollection, emptyMessageStructure);
                } else {
                    fieldStructure = new FieldStructure(fieldName, namespace, ObjectUtils.defaultIfNull(javaType, JavaType.JAVA_LANG_STRING), isCollection, structureType);
                }
            }
        }
        
        if (fieldStructure == null) {
            fieldStructure = new FieldStructure(fieldName, namespace, JavaType.JAVA_LANG_STRING, false, StructureType.SIMPLE);
        }
        
        return fieldStructure;
    }

    private JavaType getJavaTypeSafe(String value) {
        try {
            return JavaType.fromValue(value);
        } catch (RuntimeException e) {
            return null;
        }
    }
    
    private IFieldStructure createFieldStructure(IFieldStructure fieldStructure, IMessage message, String fieldName) {
        return createFieldStructure(fieldStructure, message.getNamespace(), fieldName, message.getField(fieldName));
    }
    
    private IFieldStructure createFieldStructure(IMessage message, String fieldName) {
        return createFieldStructure(null, message, fieldName);
    }
}
