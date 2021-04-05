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
import static com.exactpro.sf.services.json.JSONMessageHelper.FROM_ARRAY_ATTR;
import static com.google.common.collect.Iterables.get;
import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static org.apache.commons.lang3.BooleanUtils.toBoolean;
import static org.apache.commons.lang3.StringUtils.defaultString;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.StructureUtils;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class JSONVisitorUtility {
    public static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalEnd()
            .append(DateTimeFormatter.ofPattern("'Z'"))
            .toFormatter().withZone(ZoneOffset.UTC);
    public static final String FORMAT_ATTRIBUTE = "format";
    public static final String ISO_UTC_FORMAT = "ISO_UTC";
    public static final String ATTRIBUTE_FIELD_NAME = "FieldName";

    public static String getJsonFieldName(IFieldStructure fldStruct) {
        String jsonFieldName = getAttributeValue(fldStruct, ATTRIBUTE_FIELD_NAME);
        return ObjectUtils.defaultIfNull(jsonFieldName, fldStruct.getName());
    }

    @Nullable
    public static String checkForUnexpectedFields(@NotNull TreeNode node, @NotNull IFieldStructure structure) {
        Collection<String> expectedFields = structure.getFields().keySet();
        Collection<String> unexpectedFields = new HashSet<>();

        node.fieldNames().forEachRemaining(fieldName -> {
            if (!expectedFields.contains(fieldName)) {
                unexpectedFields.add(fieldName);
            }
        });

        return unexpectedFields.isEmpty() ? null : format("Unexpected fields in message '%s': %s", structure.getName(), unexpectedFields);
    }

    @SuppressWarnings("IfMayBeConditional")
    public static void addRejectReason(@NotNull IMessage message, String reason) {
        MsgMetaData metaData = message.getMetaData();
        String oldReason = metaData.getRejectReason();

        if (oldReason != null && reason != null) {
            metaData.setRejectReason(oldReason + lineSeparator() + reason);
        } else {
            metaData.setRejectReason(defaultString(oldReason, reason));
        }
    }

    @NotNull
    public static JsonNode preprocessMessageNode(@NotNull JsonNode node, @NotNull IFieldStructure structure) {
        boolean fromArray = toBoolean(StructureUtils.<Boolean>getAttributeValue(structure, FROM_ARRAY_ATTR));

        if (!fromArray) {
            return node;
        }

        if (!node.isArray()) {
            throw new IllegalArgumentException("Node is not an array node: " + structure.getName());
        }

        Set<String> fieldNames = structure.getFields().keySet();
        Map<String, JsonNode> fields = new LinkedHashMap<>();
        int fieldCount = Math.max(node.size(), fieldNames.size());

        for (int i = 0; i < fieldCount; i++) {
            JsonNode fieldValue = node.get(i);

            if (fieldValue == null) {
                break;
            }

            String fieldName = get(fieldNames, i, "Field" + i);
            fields.put(fieldName, fieldValue);
        }

        return new ObjectNode(JsonNodeFactory.instance, fields);
    }

    @NotNull
    public static List<JsonNode> convertToKeyValueList(String structureName, JsonNode node) {
        if (!node.isObject()) {
            throw new IllegalArgumentException("The node is not an object: " + structureName);
        }
        List<JsonNode> jsonNodes = new ArrayList<>();
        Iterator<Entry<String, JsonNode>> fields = node.fields();

        while (fields.hasNext()) {
            Entry<String, JsonNode> entry = fields.next();

            Map<String, JsonNode> newFields = new HashMap<>();
            newFields.put("key", new TextNode(entry.getKey()));
            newFields.put("value", entry.getValue());

            jsonNodes.add(new ObjectNode(JsonNodeFactory.instance, newFields));
        }
        return jsonNodes;
    }

    public static TextNode convertCharToTextNode(Character v) {
        if (v == null) {
            return null;
        }
        return new TextNode(v.toString());
    }
}
