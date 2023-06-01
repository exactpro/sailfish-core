/******************************************************************************
 * Copyright 2009-2023 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.json;

import static com.exactpro.sf.extensions.IMessageExtensionsKt.isFieldPresent;
import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.BooleanUtils;

import com.exactpro.sf.common.messages.DefaultMessageStructureVisitor;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MessageStructureReader;
import com.exactpro.sf.common.messages.MessageStructureReaderHandlerImpl;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.StructureUtils;
import com.exactpro.sf.comparison.conversion.MultiConverter;
import com.fasterxml.jackson.databind.JsonNode;

public class JSONVisitorEncode extends DefaultMessageStructureVisitor {

    protected IJsonNodeWrapper root;
    protected final IDictionaryStructure dictionary;
    protected final Supplier<MessageStructureReader> structureReaderSupplier;

    private final IMessage message;

    private final Map<String, IFieldStructure> dynamicStructures;
    private final JsonSettings jsonSettings;

    public JSONVisitorEncode(IJsonNodeWrapper root, IDictionaryStructure dictionary, IMessage message) {
        this(root, dictionary, Collections.emptyMap(), message);
    }

    public JSONVisitorEncode(IJsonNodeWrapper root, IDictionaryStructure dictionary, Map<String, IFieldStructure> dynamicStructures, IMessage message) {
        this(root, dictionary, dynamicStructures, new JsonSettings(), message);
    }
    public JSONVisitorEncode(IJsonNodeWrapper root, IDictionaryStructure dictionary, Map<String, IFieldStructure> dynamicStructures, JsonSettings jsonSettings, IMessage message) {
        this(root, dictionary, () -> MessageStructureReader.READER, dynamicStructures, jsonSettings, message);
    }

    public JSONVisitorEncode(IJsonNodeWrapper root, IDictionaryStructure dictionary, Supplier<MessageStructureReader> structureReaderSupplier, Map<String, IFieldStructure> dynamicStructures, JsonSettings jsonSettings, IMessage message) {
        this.root = root;
        this.dictionary = dictionary;
        this.structureReaderSupplier = structureReaderSupplier;
        this.dynamicStructures = dynamicStructures;
        this.jsonSettings = jsonSettings;
        this.message = message;
    }
    public JSONVisitorEncode(IJsonNodeWrapper root, IDictionaryStructure dictionary, Supplier<MessageStructureReader> structureReaderSupplier, Map<String, IFieldStructure> dynamicStructures, IMessage message) {
        this(root,dictionary,structureReaderSupplier, dynamicStructures, new JsonSettings(), message);
    }

    @Override
    public void visit(String fieldName, String value, IFieldStructure fldStruct, boolean isDefault) {
        visitSimple(value, fldStruct, TextNode::new);
    }

    @Override
    public void visit(String fieldName, Character value, IFieldStructure fldStruct, boolean isDefault) {
        visitSimple(value, fldStruct, JSONVisitorUtility::convertCharToTextNode);
    }

    @Override
    public void visit(String fieldName, BigDecimal value, IFieldStructure fldStruct, boolean isDefault) {
        visitSimple(value, fldStruct, DecimalNode::new);
    }

    @Override
    public void visit(String fieldName, Long value, IFieldStructure fldStruct, boolean isDefault) {
        visitSimple(value, fldStruct, LongNode::new);
    }

    @Override
    public void visit(String fieldName, Integer value, IFieldStructure fldStruct, boolean isDefault) {
        visitSimple(value, fldStruct, IntNode::new);
    }

    @Override
    public void visit(String fieldName, Byte value, IFieldStructure fldStruct, boolean isDefault) {
        visitSimple(value, fldStruct, (Function<Byte, JsonNode>)IntNode::new);
    }

    @Override
    public void visit(String fieldName, Boolean value, IFieldStructure fldStruct, boolean isDefault) {
        visitSimple(value, fldStruct, BooleanNode::valueOf);
    }

    @Override
    public void visit(String fieldName, IMessage message, IFieldStructure fldStruct, boolean isDefault) {
        visitSimple(message, fldStruct, msg -> writeMessage(msg, (IMessageStructure)fldStruct));
    }

    @Override
    public void visitMessageCollection(String fieldName, List<IMessage> message, IFieldStructure fldStruct, boolean isDefault) {
        visitList(message, fldStruct, msg -> writeMessageList(msg, (IMessageStructure)fldStruct));
    }

    @Override
    public void visitStringCollection(String fieldName, List<String> value, IFieldStructure fldStruct, boolean isDefault) {
        visitList(value, fldStruct, TextNode::new);
    }

    @Override
    public void visitCharCollection(String fieldName, List<Character> value, IFieldStructure fldStruct, boolean isDefault) {
        visitList(value, fldStruct, JSONVisitorUtility::convertCharToTextNode);
    }

    @Override
    public void visitLongCollection(String fieldName, List<Long> value, IFieldStructure fldStruct, boolean isDefault) {
        visitList(value, fldStruct, LongNode::new);
    }

    @Override
    public void visitIntCollection(String fieldName, List<Integer> value, IFieldStructure fldStruct, boolean isDefault) {
        visitList(value, fldStruct, IntNode::new);
    }

    @Override
    public void visitBigDecimalCollection(String fieldName, List<BigDecimal> value, IFieldStructure fldStruct, boolean isDefault) {
        visitList(value, fldStruct, DecimalNode::new);
    }

    @Override
    public void visitBooleanCollection(String fieldName, List<Boolean> value, IFieldStructure fldStruct, boolean isDefault) {
        visitList(value, fldStruct, BooleanNode::valueOf);
    }

    @Override
    public void visit(String fieldName, LocalDateTime value, IFieldStructure fldStruct, boolean isDefault) {
        String formatted = null;
        if(value != null) formatted = JSONVisitorUtility.FORMATTER.format(value);
        visitSimple(formatted, fldStruct, TextNode::new);
    }

    protected <T> void visitSimple(T value, IFieldStructure fldStruct, Function<T, JsonNode> converter) {
        if (!isWriteable(value, fldStruct)) {
            return;
        }

        String jsonFieldName = JSONVisitorUtility.getJsonFieldName(fldStruct);


        if(value == null && isFieldPresent(message, fldStruct.getName())) {
            if(root.isArray()) {
                root.add(NullNode.instance);
            } else {
                root.set(jsonFieldName, NullNode.instance);
            }
            return;
        }

        JsonNode node = jsonSettings.isTreatSimpleValuesAsStrings() && !(value instanceof IMessage) ? new TextNode(MultiConverter.convert(value, String.class)) : converter.apply(value);

        if (root.isArray()) {
            root.add(node);
        } else {
            root.set(jsonFieldName, node);
        }
    }

    protected <T> void visitList(List<T> value, IFieldStructure fldStruct, Function<T, JsonNode> converter) {
        if (!isWriteable(value, fldStruct)) {
            return;
        }

        String jsonFieldName = JSONVisitorUtility.getJsonFieldName(fldStruct);
        boolean isNoName = BooleanUtils.toBoolean(StructureUtils.<Boolean>getAttributeValue(fldStruct, JSONMessageHelper.IS_NO_NAME_ATTR));

        if(value == null && isFieldPresent(message, fldStruct.getName())) {
            if(isNoName) return;
            if(root.isArray()) {
                root.add(NullNode.instance);
            } else {
                root.set(jsonFieldName, NullNode.instance);
            }
            return;
        }

        ArrayNode array = instance.arrayNode(value.size());

        for (T item : value) {
            if(item == null) {
                array.add(NullNode.instance);
                continue;
            }
            array.add(jsonSettings.isTreatSimpleValuesAsStrings() && !(item instanceof IMessage) ? new TextNode(item.toString()) : converter.apply(item));
        }

        if (isNoName) {
            root = new ArrayNodeWrapper(array);
        } else if (root.isArray()) {
            root.add(array);
        } else {
            root.set(jsonFieldName, array);
        }
    }

    protected JsonNode writeMessageList(IMessage message, IMessageStructure structure) {
        IMessageStructure messageStructure = dictionary.getMessages().getOrDefault(structure.getReferenceName(), (IMessageStructure)dynamicStructures.get(structure.getReferenceName()));
        boolean isObject = !BooleanUtils.toBoolean(StructureUtils.<Boolean>getAttributeValue(messageStructure, JSONMessageHelper.IS_NO_OBJECT_ATTR));
        boolean fromArray = BooleanUtils.toBoolean(StructureUtils.<Boolean>getAttributeValue(structure, JSONMessageHelper.FROM_ARRAY_ATTR));
        IJsonNodeWrapper node = null;

        if (fromArray) {
            node = new ArrayNodeWrapper(instance.arrayNode(messageStructure.getFields().size()));
        } else if (isObject) {
            node = new ObjectNodeWrapper(instance.objectNode());
        }

        JSONVisitorEncode visitor = createVisitor(node, message);
        MessageStructureReader structureReader = structureReaderSupplier.get();

        structureReader.traverse(visitor, messageStructure, message, MessageStructureReaderHandlerImpl.instance());

        return visitor.getRoot();
    }

    protected JsonNode writeMessage(IMessage message, IMessageStructure messageStructure) {
        boolean isObject = !BooleanUtils.toBoolean(StructureUtils.<Boolean>getAttributeValue(messageStructure, JSONMessageHelper.IS_NO_NAME_ATTR));
        boolean fromArray = BooleanUtils.toBoolean(StructureUtils.<Boolean>getAttributeValue(messageStructure, JSONMessageHelper.FROM_ARRAY_ATTR));
        IJsonNodeWrapper node = null;

        if (fromArray) {
            node = new ArrayNodeWrapper(instance.arrayNode(messageStructure.getFields().size()));
        } else if (isObject) {
            node = new ObjectNodeWrapper(instance.objectNode());
        }

        JSONVisitorEncode visitor = createVisitor(node, message);
        MessageStructureReader structureReader = structureReaderSupplier.get();

        structureReader.traverse(visitor, messageStructure, message, MessageStructureReaderHandlerImpl.instance());

        return visitor.getRoot();
    }

    protected JSONVisitorEncode createVisitor(IJsonNodeWrapper root, IMessage message) {
        return new JSONVisitorEncode(root, dictionary, dynamicStructures, jsonSettings, message);
    }

    protected boolean isWriteable(Object value, IFieldStructure fldStruct) {
        return (value != null || isFieldPresent(message, fldStruct.getName())) && JSONVisitorUtility.isWritable(fldStruct);
    }

    public JsonNode getRoot() {
        return root.getNode();
    }
}
