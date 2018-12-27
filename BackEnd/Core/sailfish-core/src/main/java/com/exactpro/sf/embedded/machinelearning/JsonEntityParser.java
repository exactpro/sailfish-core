/*******************************************************************************
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

package com.exactpro.sf.embedded.machinelearning;

import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.embedded.machinelearning.entities.EntryType;
import com.exactpro.sf.embedded.machinelearning.entities.FailedAction;
import com.exactpro.sf.embedded.machinelearning.entities.Message;
import com.exactpro.sf.embedded.machinelearning.entities.MessageEntry;
import com.exactpro.sf.embedded.machinelearning.entities.MessageParticipant;
import com.exactpro.sf.embedded.machinelearning.entities.MessageType;
import com.exactpro.sf.embedded.machinelearning.entities.SimpleValue;
import com.exactpro.sf.storage.util.JsonMessageConverter;
import com.exactpro.sf.storage.util.JsonMessageDecoder;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonEntityParser {

    public static FailedAction parse(IDictionaryManager dictionaryManager, InputStream requestBody) throws
            JsonParseException, IOException {
            return parse(dictionaryManager, requestBody, false);
    }

    public static FailedAction parse(IDictionaryManager dictionaryManager, InputStream requestBody, boolean storeId) throws
            JsonParseException, IOException {
        JsonFactory factory = new JsonFactory();
        JsonParser parser = factory.createParser(requestBody);

        FailedAction failedAction = new FailedAction();
        checkToken(parser, JsonToken.START_OBJECT, parser.nextToken());
        Map<String, String> rootFields = new HashMap<>();
        while (JsonToken.END_OBJECT != parser.nextToken()) {
            switch (parser.getCurrentToken()) {
            case FIELD_NAME:
                if(!collectField(parser, rootFields, "expected", "actuals", "user")) {
                    switch (parser.getCurrentName()) {
                    case "expected":
                        failedAction.setExpectedMessage(parseMessage(dictionaryManager, parser, rootFields, false));
                        break;
                    case "actuals":
                        checkToken(parser, JsonToken.START_ARRAY, parser.nextToken());
                        parser.nextToken();
                        do {
                            checkToken(parser, JsonToken.START_OBJECT, parser.getCurrentToken());
                            Map<String, String> messageFields = new HashMap<>();
                            Message message = null;
                            while (JsonToken.END_OBJECT != parser.nextToken()) {
                                if (!collectField(parser, messageFields, JsonMessageConverter.JSON_MESSAGE)) {
                                    message = parseMessage(dictionaryManager, parser, messageFields, true);
                                }
                            }
                            boolean explanation = BooleanUtils.toBoolean(messageFields.get("problemExplanation"));
                            long participantId = ObjectUtils.defaultIfNull(Long.parseLong(messageFields.get("id")), 0L);
                            MessageParticipant messageParticipant = new MessageParticipant(explanation, message);
                            if (storeId) {
                                messageParticipant.setId(participantId);
                            }
                            failedAction.addParticipant(messageParticipant);
                        } while (JsonToken.END_ARRAY != parser.nextToken());
                        break;
                    case "user":
                        parser.nextToken();
                        failedAction.setSubmitter(parser.getValueAsString());
                        break;
                    default:
                        throw new JsonParseException("Unsupported root field " + parser.getCurrentName(), parser.getCurrentLocation());
                    }
                }
                break;
            default:
                throw new JsonParseException("Unsupported type " + parser.getCurrentToken(), parser.getCurrentLocation());
            }
        }
        checkToken(parser, JsonToken.END_OBJECT, parser.getCurrentToken());

        return failedAction;
    }

    private static boolean collectField(JsonParser parser, Map<String, String> fields, String ... stopFields) throws JsonParseException, IOException {
        checkToken(parser, JsonToken.FIELD_NAME, parser.getCurrentToken());
        if (!ArrayUtils.contains(stopFields, parser.getCurrentName())) {
            parser.nextToken();
            fields.put(parser.getCurrentName(), parser.getValueAsString());
            return true;
        }
        return false;
    }

    private static Message parseMessage(IDictionaryManager dictionaryManager, JsonParser parser, Map<String, String> rootFields, boolean compact) throws JsonParseException, IOException {
        try {
            parser.nextToken();

            String protocol = rootFields.get(JsonMessageConverter.JSON_MESSAGE_PROTOCOL);
            String dictionaryURI = rootFields.get(JsonMessageConverter.JSON_MESSAGE_DICTIONARY_URI);
            String dirty = ObjectUtils.defaultIfNull(rootFields.get(JsonMessageConverter.JSON_MESSAGE_DIRTY), "");

            JsonMLMessageDecoder decoder = new JsonMLMessageDecoder(dictionaryManager, dictionaryURI, protocol);
            boolean isDirty = !dirty.isEmpty() && Boolean.parseBoolean(dirty);

            Message message = decoder.parse(parser,
                    protocol,
                    SailfishURI.parse(dictionaryURI),
                    rootFields.get(JsonMessageConverter.JSON_MESSAGE_NAMESPACE),
                    rootFields.get(JsonMessageConverter.JSON_MESSAGE_NAME),
                    compact, isDirty);

            message.setDirty(isDirty);

            return message;

        } catch (RuntimeException | SailfishURIException e) {
            throw new JsonParseException("Incorrect data for parsing message", parser.getCurrentLocation(), e);
        }
    }

    private static void checkToken(JsonParser parser, JsonToken expected, JsonToken actual) throws JsonParseException, IOException {
        if (expected != actual) {
            throw new JsonParseException("Incorrect structure, expectd: " + expected + ", actual: " + actual, parser.getCurrentLocation());
        }
    }

    private static class JsonMLMessageDecoder extends JsonMessageDecoder<Message> {

        private final String dictionaryURI;
        private final String protocol;

        public JsonMLMessageDecoder(IDictionaryManager dictionaryManager, String dictionaryURI, String protocol) {
            super(dictionaryManager);
            this.dictionaryURI = dictionaryURI.toLowerCase();
            this.protocol = protocol.toLowerCase();
        }

        @Override
        protected Message createMessage(String messageName) {
            MessageType messageType = new MessageType(messageName, dictionaryURI, protocol);
            return new Message(messageType);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void handleField(Message message, String fieldName, FieldInfo fieldInfo) {
            boolean isMessage = fieldInfo.getType() == Type.IMESSAGE;

            MessageEntry entry = null;
            if (fieldInfo.isCollection()) {
                if (isMessage) {
                    entry = new MessageEntry(fieldName, EntryType.MESSAGE_ARRAY, null, null, null, (List<Message>) fieldInfo.getValue());
                } else {
                    List<?> list = (List<?>) fieldInfo.getValue();
                    List<SimpleValue> targetList = new ArrayList<>(list.size());
                    for (Object object : list) {
                        targetList.add(new SimpleValue(fieldInfo.getType().toString(), String.valueOf(object)));
                    }
                    entry = new MessageEntry(fieldName, EntryType.SIMPLE_ARRAY, null, null, targetList, null);
                }
            } else {
                if (isMessage) {
                    entry = new MessageEntry(fieldName, (Message)fieldInfo.getValue());
                } else {
                    entry = new MessageEntry(fieldName, new SimpleValue(fieldInfo.getType().toString(), String.valueOf(fieldInfo.getValue())));
                }
            }

            message.addEntry(entry);
        }

    }
}
