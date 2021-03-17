/*******************************************************************************
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
 ******************************************************************************/
package com.exactpro.sf.common.messages.structures.loaders;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.impl.messages.all.configuration.IAttribute;
import com.exactpro.sf.common.impl.messages.all.configuration.IDictionary;
import com.exactpro.sf.common.impl.messages.all.configuration.IField;
import com.exactpro.sf.common.impl.messages.all.configuration.IMessage;
import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.StructureBuilder;
import com.exactpro.sf.common.messages.structures.StructureUtils;
import com.exactpro.sf.common.messages.structures.impl.AttributeStructure;
import com.exactpro.sf.common.messages.structures.impl.DictionaryStructure;
import com.exactpro.sf.common.messages.structures.impl.FieldStructure;
import com.exactpro.sf.common.messages.structures.impl.MessageStructure;
import com.exactpro.sf.common.messages.structures.impl.MessageStructurePromise;
import com.exactpro.sf.common.util.EPSCommonException;
import com.sun.istack.NotNull;

/**
 * Java class for implements {@link IDictionaryStructureLoader} with special rules and features.
 */
public abstract class AbstractDictionaryStructureLoader implements IDictionaryStructureLoader{

    protected final boolean aggregate;
    protected final Set<String> pendingMessages = new HashSet<>();
    protected final Map<String, MessageStructurePromise> promiseMap = new HashMap<>();

    protected final Logger logger;

    private boolean isRecursion;
    private IField lastMessage;
    private IFieldStructure messageFinalRecursionField;
    private String startRecursionMessageName;
    private boolean isCircledAttribute;

    public AbstractDictionaryStructureLoader(boolean aggregate) {
        this.aggregate = aggregate;
         logger = LoggerFactory.getLogger(this.getClass());
    }

    public AbstractDictionaryStructureLoader(){
        this(true);
    }

    /**
     * Get IDictionary structure from specific input format.
     * @param inputStream {@link InputStream}
     * @return {@link IDictionary}
     */
    public abstract IDictionary getDictionary(InputStream inputStream) throws EPSCommonException;

    /**
     * Create IDictionary from specific input format
     * @param input {@link InputStream}
     * @return
     * @throws EPSCommonException
     */
    @Override
    public IDictionaryStructure load(InputStream input) throws EPSCommonException {
        return convert(getDictionary(input));
    }

    /**
     * Get IDictionary namespace from specific input format
     * @param input {@link InputStream}
     * @return
     * @throws EPSCommonException
     */
    @Override
    public String extractNamespace(InputStream input) throws EPSCommonException {
        return getDictionary(input).getName();
    }


    protected StructureBuilder initStructureBuilder(String namespace) {
        return new StructureBuilder(namespace);
    }

    protected <K,V> Map<K,V> initMap() {
        return new HashMap<>();
    }

    public IDictionaryStructure convert(IDictionary dictionary) throws EPSCommonException {

        StructureBuilder builder = initStructureBuilder(dictionary.getName());

        for (IField field : dictionary.getFields()) {

            if (getFieldType(field, true) == null) {
                throw new EPSCommonException("A field " + field.getName()
                        + " with an id " + field.getId()
                        + " has neither a type nor a reference");
            }

            if (field.getName() == null) {
                throw new EPSCommonException("Field id='" + field.getId() + "' name='" + field.getName() + "' type='"+ field.getType() +"' does not contain name");
            }

            if (isComplex(field)) {
                throw new EPSCommonException(String.format("It is impossible to keep message '%s' in fields", field.getName()));
            } else {

                JavaType javaType = getFieldType(field, true);
                String defVal = getDefaultValue(field, true);

                IFieldStructure fieldStructure = createFieldStructure(
                        field, true,
                        field.getId(),
                        field.getName(),
                        dictionary.getName(),
                        field.getDescription(),
                        javaType,
                        field.isRequired(),
                        field.isIsCollection(),
                        field.isIsServiceName(),
                        defVal
                );

                builder.addFieldStructure(fieldStructure);
            }
        }

        List<? extends IMessage> messages = dictionary.getMessages();
        int i = 0;
        int size = messages.size();

        while (builder.getMsgStructures().size() != size) {

            //Check index need if dictionary contains messages with same names
            //Submessages load while loads message contains it
            if (i >= size) {

                Set<String> dup = new HashSet<>();
                StringBuilder duplicatedNames = new StringBuilder();

                for (IMessage m : messages) {

                    String messageName = m.getName();

                    if (!dup.add(messageName)) {
                        duplicatedNames.append(messageName);
                        duplicatedNames.append("; ");
                    }
                }

                throw new EPSCommonException(
                        "Messages with same names has been detected! Check names of your messages. Message names: " + duplicatedNames);
            }

            IMessage msg = messages.get(i++);

            if (builder.getMessageStructure(msg.getName()) == null) {
                convertMessage(dictionary.getName(), builder, msg);
            }

            startRecursionMessageName = null;
            pendingMessages.clear();
            promiseMap.clear();
        }

        Map<String, IAttributeStructure> dictAttributes = getDictionaryAttributes(dictionary);

        return createDictionaryStructure(dictionary.getName(), dictionary.getDescription(), dictAttributes,
                builder.getMsgStructureMap(), builder.getFieldStructureMap());
    }

    private IMessageStructure convertMessage(String namespace, StructureBuilder builder, IMessage message) {
        if (!pendingMessages.add(message.getId())) {
            if (message.getReference() instanceof IMessage
                && message.getReference().getReference() instanceof IMessage) {
                    throw new EPSCommonException(String.format("Recursion at message id: '%s' has been detected!", message.getId()));
            }
            return promiseMap.computeIfAbsent(message.getId(), (key) -> new MessageStructurePromise(message.getName()));
        }

        IField reference = message.getReference();

        if (reference instanceof IMessage) {
            if (builder.getMessageStructure(reference.getName()) == null) {
                convertMessage(namespace, builder, (IMessage)reference);
            }
        } else if (reference != null) {
            throw new EPSCommonException(String.format("Message '%s' has field '%s' as a reference", message.getName(), reference.getName()));
        }

        try {
            IMessageStructure messageStructure = convertMessageInternal(namespace, builder, message, reference);
            builder.addMessageStructure(messageStructure);

            MessageStructurePromise promise = promiseMap.get(message.getId());
            if (promise != null) {
                promise.setOrigin(messageStructure);
            }

            return messageStructure;
        } catch (RuntimeException e) {
            throw new EPSCommonException("Message '" + message.getName() + "', problem with content", e);
        }
    }

    private IMessageStructure convertMessageInternal(String namespace, StructureBuilder builder, IMessage message, IField reference) {
        Map<String, IFieldStructure> fields = createFieldStructures(builder, message, namespace);
        Map<String, ? extends IAttributeStructure> attributes = getAttributes(message, false, null);
        IMessageStructure referencedMessage = null;

        if (reference != null) {
            referencedMessage = builder.getMessageStructure(reference.getName());

            if (aggregate) {
                Map<String, IFieldStructure> parentFields = new LinkedHashMap<>(referencedMessage.getFields());
                Map<String, IAttributeStructure> parentAttributes = new HashMap<>(referencedMessage.getAttributes());

                parentFields.putAll(fields);
                fields = parentFields;

                if (attributes != null) {
                    parentAttributes.putAll(attributes);
                }

                attributes = parentAttributes;
            }
        }

        return createMessageStructure(
                message,
                message.getId(),
                message.getName(),
                namespace,
                message.getDescription(),
                message.isRequired(),
                message.isIsCollection(),
                fields,
                attributes,
                referencedMessage
        );
    }

    protected IFieldStructure createFieldStructure(IField field, boolean isTemplate, String id, String name, String namespace,
                                                   String description, JavaType javaType, Boolean isRequired, Boolean isCollection, Boolean isServiceName, String defaultValue) {

        String referenceName = field.getReference() != null ? field.getReference().getName() : null;

        try {
            Map<String, ? extends IAttributeStructure> attributes = getAttributes(field, false, null);
            Map<String, ? extends IAttributeStructure> values = (!isTemplate && referenceName == null) ? null : getAttributes(field, true, javaType);

            return new FieldStructure(name, namespace, description, referenceName, attributes, values,
                    defaultIfNull(javaType, JavaType.JAVA_LANG_STRING), isRequired, isCollection, isServiceName, defaultValue);
        } catch (EPSCommonException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new EPSCommonException("Field '" + name + "', problem with attributes or values", e);
        }
    }

    protected IMessageStructure createMessageStructure(IMessage message, String id, String name, String namespace, String description,
                                                       boolean isRequired, boolean isCollection,
                                                       Map<String, IFieldStructure> fields, Map<String, ? extends IAttributeStructure> attributes, IMessageStructure reference) {

        return new MessageStructure(name, namespace, description, fields, isRequired, isCollection, attributes, reference);
    }

    protected IMessageStructure createMessageStructure(IMessage message, IField field, String id, String name, String namespace,
                                                       String description, Boolean isRequired, Boolean isCollection, IMessageStructure reference) {

        try {
            Map<String, ? extends IAttributeStructure> attributes = getAttributes(field, false, null);

            return new MessageStructure(name, namespace, description, isRequired, isCollection, attributes, reference);
        } catch (RuntimeException e) {
            throw new EPSCommonException("Message '" + name + "', problem with attributes", e);
        }
    }

    protected IDictionaryStructure createDictionaryStructure(String namespace, String description, Map<String, IAttributeStructure> dictAttributes,
                                                             Map<String, IMessageStructure> msgStructures, Map<String, IFieldStructure> fieldStructures) {


        return new DictionaryStructure(namespace, description, dictAttributes, msgStructures, fieldStructures);
    }

    protected IAttributeStructure createAttributeStructure(String name, String value, Object castValue, JavaType type) {
        return new AttributeStructure(name, value, castValue, defaultIfNull(type, JavaType.JAVA_LANG_STRING));
    }

    private String getDefaultValue(IField field, boolean search) {
        if(!search || field.getDefaultValue() != null) {
            return field.getDefaultValue();
        }

        return field.getReference() != null ? getDefaultValue(field.getReference(), true) : null;
    }

    protected Map<String, IFieldStructure> createFieldStructures(StructureBuilder builder, IMessage message, String namespace) {
        Map<String, IFieldStructure> result = new LinkedHashMap<>();

        for (IField field : message.getFields()) {

            IFieldStructure fieldStructure;

            if (field instanceof IMessage || field.getReference() instanceof IMessage) {

                IMessageStructure struct = null;

                if (field.getReference() != null) {
                    struct = builder.getMessageStructure(field.getReference().getName());

                    if (struct == null) {
                        struct = convertMessage(namespace, builder, (IMessage)field.getReference());
                    }
                }

                if (field instanceof IMessage && !((IMessage)field).getFields().isEmpty()) {
                    fieldStructure = convertMessageInternal(namespace, builder, (IMessage)field, null);
                } else {
                    fieldStructure = createMessageStructure((IMessage)field.getReference(), field, field.getId(),
                            field.getName(), namespace, field.getDescription(), field.isRequired(), field.isIsCollection(), struct);
                }

            } else {

                if (getFieldType(field, true) == null) {
                    throw new EPSCommonException(
                            String.format("Field [%s] in message [%s] has neither a type nor a reference",
                                    field.getName(), message.getName()));
                }

                JavaType javaType = getFieldType(field, aggregate);
                String defVal = getDefaultValue(field, aggregate);

                fieldStructure = createFieldStructure(
                        field, false,
                        field.getId(),
                        field.getName(),
                        namespace,
                        field.getDescription(),
                        javaType,
                        field.isRequired(),
                        field.isIsCollection(),
                        field.isIsServiceName(),
                        defVal
                );

            }

            if(result.put(fieldStructure.getName(), fieldStructure) != null) {
                throw new EPSCommonException(String.format("Duplicate field '%s' in message '%s'", field.getName(), message.getName()));
            }
        }

        return result;
    }

    private JavaType getFieldType(IField field, boolean search, @NotNull Set<String> pendingFields) {
        if (!search) {
            return field.getType();
        }

        if (field.getReference() != null) {
            if (pendingFields.add(field.getReference().getId())) {
                try {
                    return getFieldType(field.getReference(), true, pendingFields);
                } finally {
                    pendingFields.remove(field.getReference().getId());
                }
            } else {

                StringBuilder builder = new StringBuilder("Recursive reference for fields with ids: [");
                pendingFields.stream().sorted(String::compareTo).forEach(id -> {
                    builder.append(StringUtils.join(id, ","));
                });
                builder.setCharAt(builder.length() - 1, ']');
                pendingFields.clear();
                throw new EPSCommonException(builder.toString());
            }
        } else
            return field.getType();
    }

    private JavaType getFieldType(IField field, boolean search) {
        return getFieldType(field, search, new HashSet<>());
    }

    private boolean isComplex(IField field) {
        if(field instanceof IMessage) {
            return true;
        }

        IField reference = field.getReference();
        return reference != null && isComplex(reference);
    }

    protected Map<String, ? extends IAttributeStructure> getAttributes(IField field, boolean isValues, JavaType javaType) {
        List<IAttribute> attributes = new ArrayList<>();

        if (isValues) {
            collectFieldValues(field, attributes);
        } else {
            collectFieldAttributes(field, attributes);
        }

        Map<String, IAttributeStructure> result = collectAttributeStructures(attributes, javaType, isValues);

        return result.isEmpty() ? null : result;
    }

    private Map<String, IAttributeStructure> getDictionaryAttributes(IDictionary dictionary) {

        List<IAttribute> filteredAttr = new ArrayList<>();
        for (IAttribute attr : dictionary.getAttributes()) {
            replaceOrAdd(filteredAttr, attr);
        }
        Map<String, IAttributeStructure> result = collectAttributeStructures(filteredAttr, null, false);
        return result.isEmpty() ? null : result;
    }

    protected Map<String, IAttributeStructure> collectAttributeStructures(List<IAttribute> attributes, JavaType javaType, boolean isValues) {
        Map<String, IAttributeStructure> result = initMap();
        for (IAttribute attribute : attributes) {
            Object value = getAttributeValue(attribute, javaType);

            result.put(attribute.getName(),
                    createAttributeStructure(attribute.getName(), attribute.getValue(), value, isValues ? javaType : defaultIfNull(attribute.getType(), JavaType.JAVA_LANG_STRING)));
        }
        return result;
    }

    protected void collectFieldAttributes(IField field, Collection<IAttribute> attributes) {
        collectFieldAttributesOrValues(field, attributes, false);
    }

    protected void collectFieldValues(IField field, Collection<IAttribute> values) {
        collectFieldAttributesOrValues(field, values, true);
    }

    private void collectFieldAttributesOrValues(IField field, Collection<IAttribute> target, boolean isValue) {
        List<? extends IAttribute> source = isValue ? field.getValues() : field.getAttributes();

        if (aggregate) {
            IField reference = field.getReference();
            if (reference != null) {
                if (isRecursion && reference.getName().equals(startRecursionMessageName)) {
                    if (isCircledAttribute) {
                        return;
                    } else {
                        collectFieldAttributesOrValues(reference, target, isValue);
                    }
                    isCircledAttribute = !isCircledAttribute;
                }
                collectFieldAttributesOrValues(reference, target, isValue);
            }
        }

        for (IAttribute attribute : source) {
            boolean corruptValues = replaceOrAdd(target, attribute);

            if (isValue && corruptValues) {
                throw new EPSCommonException(String.format("Duplicated values at %s, attribute name is %s", field.getName(), attribute.getName()));
            }
        }
    }

    private static boolean replaceOrAdd(Collection<IAttribute> attributes, IAttribute attribute) {
        boolean removed = false;
        Iterator<IAttribute> iterator = attributes.iterator();
        while (iterator.hasNext()) {
            IAttribute element = iterator.next();
            if (attribute.getName().equals(element.getName())) {
                iterator.remove();
                removed = true;
                break;
            }
        }

        attributes.add(attribute);
        return removed;
    }

    private static Object getAttributeValue(IAttribute attribute, JavaType javaType) {

        javaType = defaultIfNull(defaultIfNull(javaType, attribute.getType()), JavaType.JAVA_LANG_STRING);

        if (javaType == JavaType.JAVA_LANG_STRING) {
            return attribute.getValue();
        } else if (StringUtils.isEmpty(attribute.getValue())) {
            return null;
        }

        return StructureUtils.castValueToJavaType(attribute.getValue(), javaType);
    }

}
