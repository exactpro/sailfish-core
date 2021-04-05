/******************************************************************************
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
package com.exactpro.sf.services.json;

import static com.exactpro.sf.common.messages.structures.StructureUtils.getAttributeValue;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.Nullable;

import com.exactpro.sf.common.messages.DefaultMessageStructureVisitor;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.IMessageStructureVisitor;
import com.exactpro.sf.common.messages.MessageStructureWriter;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.StructureUtils;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.util.DateTimeUtility;
import com.fasterxml.jackson.databind.JsonNode;

public class JSONVisitorDecode extends DefaultMessageStructureVisitor {

    private final JsonNode rootNode;
    private final IMessageFactory msgFactory;
    private final IMessage result;
    private final JsonSettings jsonSettings;


    public JSONVisitorDecode(JsonNode rootNode, IMessageFactory msgFactory, IMessage message, JsonSettings jsonSettings) {
        this.rootNode = rootNode;
        this.msgFactory = msgFactory;
        this.result = message;
        this.jsonSettings = jsonSettings;
    }

    @Override
    public void visit(String fieldName, Byte value, IFieldStructure fldStruct, boolean isDefault) {

        JsonNode valueNode = getJsonNode(fldStruct, rootNode);
        if (valueNode != null) {
            //TODO overflow check
            value = (byte)valueNode.asInt();
            value = jsonSettings.isTreatSimpleValuesAsStrings() ? (byte)Integer.parseInt(valueNode.asText()): (byte)valueNode.asInt();
            result.addField(fieldName, value);
        }
    }

    @Override
    public void visit(String fieldName, Short value, IFieldStructure fldStruct, boolean isDefault) {

        JsonNode valueNode = getJsonNode(fldStruct, rootNode);
        if (valueNode != null) {
            //TODO overflow check
            value = jsonSettings.isTreatSimpleValuesAsStrings() ? (short)Integer.parseInt(valueNode.asText()): (short)valueNode.asInt();
            result.addField(fieldName, value);
        }
    }

    @Override
    public void visit(String fieldName, Integer value, IFieldStructure fldStruct, boolean isDefault) {

        JsonNode valueNode = getJsonNode(fldStruct, rootNode);
        if (valueNode != null) {
            //TODO overflow check
            value = jsonSettings.isTreatSimpleValuesAsStrings() ? Integer.parseInt(valueNode.asText()): valueNode.asInt();
            result.addField(fieldName, value);
        }
    }

    @Override
    public void visit(String fieldName, Long value, IFieldStructure fldStruct, boolean isDefault) {

        JsonNode valueNode = getJsonNode(fldStruct, rootNode);
        if (valueNode != null) {
            //TODO overflow check
            value = jsonSettings.isTreatSimpleValuesAsStrings() ? Long.parseLong(valueNode.asText()): valueNode.asLong();
            result.addField(fieldName, value);
        }
    }

    @Override
    public void visit(String fieldName, BigDecimal value, IFieldStructure fldStruct, boolean isDefault) {

        JsonNode valueNode = getJsonNode(fldStruct, rootNode);
        if (valueNode != null) {
            //TODO overflow check
            value = jsonSettings.isTreatSimpleValuesAsStrings() ? new BigDecimal(valueNode.asText()): valueNode.decimalValue();
            result.addField(fieldName, value);
        }
    }

    @Override
    public void visit(String fieldName, Boolean value, IFieldStructure fldStruct, boolean isDefault) {

        JsonNode valueNode = getJsonNode(fldStruct, rootNode);
        if (valueNode != null) {
            //TODO overflow check
            value = jsonSettings.isTreatSimpleValuesAsStrings() ? Boolean.parseBoolean(valueNode.asText()): valueNode.asBoolean();
            result.addField(fieldName, value);
        }
    }

    @Override
    public void visit(String fieldName, Character value, IFieldStructure fldStruct, boolean isDefault) {

        JsonNode valueNode = getJsonNode(fldStruct, rootNode);
        if (valueNode != null) {
            if (valueNode.asText().length() > 1) {
                //TODO edit ex message
                throw new EPSCommonException("overflow");
            }
            value = valueNode.asText().charAt(0);
            result.addField(fieldName, value);
        }
    }

    @Override
    public void visit(String fieldName, LocalDateTime value, IFieldStructure fldStruct, boolean isDefault) {

        JsonNode valueNode = getJsonNode(fldStruct, rootNode);
        if (valueNode != null) {
            if(JSONVisitorUtility.ISO_UTC_FORMAT.equals(getAttributeValue(fldStruct, JSONVisitorUtility.FORMAT_ATTRIBUTE))) {
                value = LocalDateTime.parse(valueNode.asText(), JSONVisitorUtility.FORMATTER);
            } else {
                value = DateTimeUtility.toLocalDateTime(ZonedDateTime.parse(valueNode.asText()));

            }
            result.addField(fieldName, value);
        }
    }

    @Override
    public void visit(String fieldName, String value, IFieldStructure fldStruct, boolean isDefault) {

        JsonNode valueNode = getJsonNode(fldStruct, rootNode);
        if (valueNode != null) {
            value = valueNode.asText();
            result.addField(fieldName, value);
        }
    }

    @Override
    public void visit(String fieldName, Double value, IFieldStructure fldStruct, boolean isDefault) {

        JsonNode valueNode = getJsonNode(fldStruct, rootNode);
        if (valueNode != null) {
            //TODO overflow check
            value = jsonSettings.isTreatSimpleValuesAsStrings() ? Double.parseDouble(valueNode.asText()): valueNode.asDouble();
            result.addField(fieldName, value);
        }
    }

    @Override
    public void visit(String fieldName, Float value, IFieldStructure fldStruct, boolean isDefault) {

        JsonNode valueNode = getJsonNode(fldStruct, rootNode);
        if (valueNode != null) {
            //TODO overflow check
            value = jsonSettings.isTreatSimpleValuesAsStrings() ? Float.parseFloat(valueNode.asText()): (float)valueNode.asDouble();
            result.addField(fieldName, value);
        }
    }

    @Override
    public void visit(String fieldName, IMessage message, IFieldStructure fldStruct, boolean isDefault) {
        String jsonFieldName = JSONVisitorUtility.getJsonFieldName(fldStruct);
        if (rootNode.hasNonNull(jsonFieldName)) {
            JsonNode node = JSONVisitorUtility.preprocessMessageNode(this.rootNode.get(jsonFieldName), fldStruct);
            IMessage nested = msgFactory.createMessage(fldStruct.getReferenceName(), fldStruct.getNamespace());
            IMessageStructureVisitor jsonVisitorDecode = new JSONVisitorDecode(node, msgFactory, nested, jsonSettings);
            MessageStructureWriter.WRITER.traverse(jsonVisitorDecode, fldStruct.getFields());

            if (jsonSettings.isRejectUnexpectedFields()) {
                String rejectReason = JSONVisitorUtility.checkForUnexpectedFields(node, fldStruct);
                JSONVisitorUtility.addRejectReason(nested, rejectReason);
                JSONVisitorUtility.addRejectReason(result, nested.getMetaData().getRejectReason());
            }

            result.addField(fieldName, nested);
        }
    }

    @Override
    public void visitByteCollection(String fieldName, List<Byte> value, IFieldStructure fldStruct, boolean isDefault) {
        Iterator<JsonNode> valueNodes = getJsonNodes(fldStruct, rootNode);
        if (valueNodes != null) {
            List<Byte> list = new ArrayList<>();
            while (valueNodes.hasNext()) {
                JsonNode element = valueNodes.next();
                list.add(jsonSettings.isTreatSimpleValuesAsStrings() ? (byte)Integer.parseInt(element.asText()) : (byte)element.asInt());
            }
            result.addField(fieldName, list);
        }
    }

    @Override
    public void visitShortCollection(String fieldName, List<Short> value, IFieldStructure fldStruct, boolean isDefault) {
        Iterator<JsonNode> valueNodes = getJsonNodes(fldStruct, rootNode);
        if (valueNodes != null) {
            List<Short> list = new ArrayList<>();
            while (valueNodes.hasNext()) {
                JsonNode element = valueNodes.next();
                list.add(jsonSettings.isTreatSimpleValuesAsStrings() ? (short)Integer.parseInt(element.asText()) : (short)element.asInt());
            }
            result.addField(fieldName, list);
        }
    }

    @Override
    public void visitIntCollection(String fieldName, List<Integer> value, IFieldStructure fldStruct, boolean isDefault) {
        Iterator<JsonNode> valueNodes = getJsonNodes(fldStruct, rootNode);
        if (valueNodes != null) {
            List<Integer> list = new ArrayList<>();
            while (valueNodes.hasNext()) {
                JsonNode element = valueNodes.next();
                list.add(jsonSettings.isTreatSimpleValuesAsStrings() ? Integer.parseInt(element.asText()) : element.asInt());
            }
            result.addField(fieldName, list);
        }
    }

    @Override
    public void visitLongCollection(String fieldName, List<Long> value, IFieldStructure fldStruct, boolean isDefault) {
        Iterator<JsonNode> valueNodes = getJsonNodes(fldStruct, rootNode);
        if (valueNodes != null) {
            List<Long> list = new ArrayList<>();
            while (valueNodes.hasNext()) {
                JsonNode element = valueNodes.next();
                list.add(jsonSettings.isTreatSimpleValuesAsStrings() ? Long.parseLong(element.asText()) : element.asLong());
            }
            result.addField(fieldName, list);
        }
    }

    @Override
    public void visitBigDecimalCollection(String fieldName, List<BigDecimal> value, IFieldStructure fldStruct, boolean isDefault) {
        Iterator<JsonNode> valueNodes = getJsonNodes(fldStruct, rootNode);
        if (valueNodes != null) {
            List<BigDecimal> list = new ArrayList<>();
            while (valueNodes.hasNext()) {
                JsonNode element = valueNodes.next();
                list.add(jsonSettings.isTreatSimpleValuesAsStrings() ? new BigDecimal(element.asText()) : element.decimalValue());
            }
            result.addField(fieldName, list);
        }
    }

    @Override
    public void visitCharCollection(String fieldName, List<Character> value, IFieldStructure fldStruct, boolean isDefault) {
        Iterator<JsonNode> valueNodes = getJsonNodes(fldStruct, rootNode);
        if (valueNodes != null) {
            List<Character> list = new ArrayList<>();
            while (valueNodes.hasNext()) {
                JsonNode element = valueNodes.next();
                if (element.asText().length() == 1) {
                    list.add(element.asText().charAt(0));
                }
            }
            result.addField(fieldName, list);
        }
    }

    @Override
    public void visitStringCollection(String fieldName, List<String> value, IFieldStructure fldStruct, boolean isDefault) {
        Iterator<JsonNode> valueNodes = getJsonNodes(fldStruct, rootNode);
        if (valueNodes != null) {
            List<String> list = new ArrayList<>();
            while (valueNodes.hasNext()) {
                JsonNode element = valueNodes.next();
                list.add(element.asText());
            }
            result.addField(fieldName, list);
        }
    }

    @Override
    public void visitBooleanCollection(String fieldName, List<Boolean> value, IFieldStructure fldStruct, boolean isDefault) {
        Iterator<JsonNode> valueNodes = getJsonNodes(fldStruct, rootNode);
        if (valueNodes != null) {
            List<Boolean> list = new ArrayList<>();
            while (valueNodes.hasNext()) {
                JsonNode element = valueNodes.next();
                list.add(jsonSettings.isTreatSimpleValuesAsStrings() ? Boolean.parseBoolean(element.asText()) : element.asBoolean());
            }
            result.addField(fieldName, list);
        }
    }

    @Override
    public void visitDateTimeCollection(String fieldName, List<LocalDateTime> value, IFieldStructure fldStruct, boolean isDefault) {
        Iterator<JsonNode> valueNodes = getJsonNodes(fldStruct, rootNode);
        if (valueNodes != null) {
            List<LocalDateTime> list = new ArrayList<>();
            SimpleDateFormat dateFormat = new SimpleDateFormat();
            while (valueNodes.hasNext()) {
                JsonNode element = valueNodes.next();
                try {
                    list.add(DateTimeUtility.toLocalDateTime(dateFormat.parse(element.asText())));
                } catch (ParseException e) {
                    //TODO throw ex normally
                    throw new EPSCommonException(e);
                }
            }
            result.addField(fieldName, list);
        }
    }

    @Override
    public void visitDoubleCollection(String fieldName, List<Double> value, IFieldStructure fldStruct, boolean isDefault) {
        Iterator<JsonNode> valueNodes = getJsonNodes(fldStruct, rootNode);
        if (valueNodes != null) {
            List<Double> list = new ArrayList<>();
            while (valueNodes.hasNext()) {
                JsonNode element = valueNodes.next();
                list.add(jsonSettings.isTreatSimpleValuesAsStrings() ? Double.parseDouble(element.asText()) : element.asDouble());
            }
            result.addField(fieldName, list);
        }
    }

    @Override
    public void visitFloatCollection(String fieldName, List<Float> value, IFieldStructure fldStruct, boolean isDefault) {
        Iterator<JsonNode> valueNodes = getJsonNodes(fldStruct, rootNode);
        if (valueNodes != null) {
            List<Float> list = new ArrayList<>();
            while (valueNodes.hasNext()) {
                JsonNode element = valueNodes.next();
                list.add(jsonSettings.isTreatSimpleValuesAsStrings() ? Float.parseFloat(element.asText()) : (float)element.asDouble());
            }
            result.addField(fieldName, list);
        }
    }

    @Override
    public void visitMessageCollection(String fieldName, List<IMessage> message, IFieldStructure fldStruct, boolean isDefault) {
        boolean keyValueList = BooleanUtils.toBoolean(StructureUtils.<Boolean>getAttributeValue(fldStruct, JSONMessageHelper.KEY_VALUE_LIST_ATTR));

        Iterator<JsonNode> valueNodes = keyValueList
                ? getKeyValueList(fldStruct, rootNode)
                : getJsonNodes(fldStruct, rootNode);

        if (valueNodes != null) {
            List<IMessage> list = new ArrayList<>();
            while (valueNodes.hasNext()) {
                JsonNode element = JSONVisitorUtility.preprocessMessageNode(valueNodes.next(), fldStruct);
                IMessage nested = msgFactory.createMessage(fldStruct.getReferenceName(), fldStruct.getNamespace());
                IMessageStructureVisitor jsonVisitorDecode = new JSONVisitorDecode(element, msgFactory, nested, jsonSettings);
                MessageStructureWriter.WRITER.traverse(jsonVisitorDecode, fldStruct.getFields());

                if (jsonSettings.isRejectUnexpectedFields()) {
                    String rejectReason = JSONVisitorUtility.checkForUnexpectedFields(element, fldStruct);
                    JSONVisitorUtility.addRejectReason(nested, rejectReason);
                    JSONVisitorUtility.addRejectReason(result, nested.getMetaData().getRejectReason());
                }

                list.add(nested);
            }
            result.addField(fieldName, list);
        }
    }
    
    public static JsonNode getJsonNode(IFieldStructure fldStruct, JsonNode rootNode) {
        String jsonFieldName = JSONVisitorUtility.getJsonFieldName(fldStruct);
        return rootNode.hasNonNull(jsonFieldName) ? rootNode.path(jsonFieldName) : null;
    }

    @Nullable
    public static Iterator<JsonNode> getJsonNodes(IFieldStructure fldStruct, JsonNode rootNode) {
        boolean isNoName = BooleanUtils.toBoolean(StructureUtils.<Boolean>getAttributeValue(fldStruct, JSONMessageHelper.IS_NO_NAME_ATTR));

        if (isNoName) {
            return rootNode.isArray() ? rootNode.elements() : null;
        }

        String jsonFieldName = JSONVisitorUtility.getJsonFieldName(fldStruct);

        if (rootNode.hasNonNull(jsonFieldName)) {
            JsonNode node = rootNode.path(jsonFieldName);
            return node.isArray() ? node.elements() : null;
        }

        return null;
    }

    @Nullable
    public static Iterator<JsonNode> getKeyValueList(IFieldStructure fldStruct, JsonNode rootNode) {
        boolean isNoName = BooleanUtils.toBoolean(StructureUtils.<Boolean>getAttributeValue(fldStruct, JSONMessageHelper.IS_NO_NAME_ATTR));

        String jsonFieldName = JSONVisitorUtility.getJsonFieldName(fldStruct);
        if (rootNode.hasNonNull(jsonFieldName)) {
            JsonNode node = rootNode.path(jsonFieldName);
            return JSONVisitorUtility.convertToKeyValueList(fldStruct.getName(), node).iterator();
        }

        return null;
    }
}
