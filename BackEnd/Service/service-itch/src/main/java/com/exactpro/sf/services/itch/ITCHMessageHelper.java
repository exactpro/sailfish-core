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
package com.exactpro.sf.services.itch;


import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.StructureUtils;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.MessageHelper;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ITCHMessageHelper extends MessageHelper {

    //Codec constants
    public static final String FIELD_SECONDS = "Seconds";
    public static final String FIELD_NANOSECOND = "Nanosecond";

    public static final String ATTRIBUTE_MESSAGE_TYPE = "MessageType";
    public static final Short MESSAGE_TYPE_TIME = 84;

    public static final String SUBMESSAGES_FIELD_NAME = "IncludedMessages";
    public static final String MESSAGELIST_NAME = "MessagesList";
    public static final String MESSAGELIST_NAMESPACE = "ITCH";
    public static final Short  UNITHEADERMSGTYPE = new Short((short)256);
    public static final String FIELD_MARKET_DATA_GROUP_NAME = "MarketDataGroup";
    public static final String FIELD_LENGTH_NAME = "Length";
    public static final String ATTRIBUTE_LENGTH_NAME = FIELD_LENGTH_NAME;
    public static final String FIELD_MESSAGE_COUNT_NAME = "MessageCount";
    public static final String FIELD_SEQUENCE_NUMBER_NAME = "SequenceNumber";
    public static final String MESSAGE_UNIT_HEADER_NAME = "UnitHeader";
    public static final String MESSAGE_LENGTH_PARAM = "MsgLength";
    public static final String FAKE_FIELD_MESSAGE_TIME = "MessageTime";
    public static final String FAKE_FIELD_UH_MARKET_DATA_GROUP = "UHMarketDataGroup";
    public static final String FAKE_FIELD_UH_SEQUENCE_NUMBER = "UHSequenceNumber";
    public static final String FAKE_FIELD_MESSAGE_SEQUENCE_NUMBER = "MessageSequenceNumber";

    private IMessageStructure headerStructure;
    private AbstractCodec codec;

    @Override
    public IMessage prepareMessageToEncode(IMessage message, Map<String, String> params) {
        if(params == null){
            params = new HashMap<>();
        }
        if(!params.containsKey(FIELD_MARKET_DATA_GROUP_NAME)){
            params.put(FIELD_MARKET_DATA_GROUP_NAME, String.valueOf(0));
        }
        if(!params.containsKey(FIELD_SEQUENCE_NUMBER_NAME)){
            params.put(FIELD_SEQUENCE_NUMBER_NAME, String.valueOf(0));
        }
        if(!params.containsKey(FIELD_MESSAGE_COUNT_NAME)){
            params.put(FIELD_MESSAGE_COUNT_NAME, String.valueOf(1));
        }
        if(!params.containsKey(FIELD_LENGTH_NAME)){
            params.put(FIELD_LENGTH_NAME, String.valueOf(0));
        }

        String messageNamespace = message.getNamespace();
        IMessage header = getFilledHeader(getMessageFactory(), params, messageNamespace);
        List<IMessage> messages = new LinkedList<>();
        if (!MESSAGE_UNIT_HEADER_NAME.equals(message.getName())) {
            messages.add(header);
        }
        messages.add(message);

        IMessage listMessage = getMessageFactory().createMessage(MESSAGELIST_NAME, messageNamespace);

        listMessage.addField(SUBMESSAGES_FIELD_NAME, messages);
        return listMessage;
    }

    @Override
    public synchronized AbstractCodec getCodec(IServiceContext serviceContext) {
        if(codec == null) {
            codec = new ITCHCodec();
            ITCHCodecSettings settings = null;
            Integer lengthSize = extractLengthSize(getDictionaryStructure());
            if (lengthSize != null) {
                settings = new ITCHCodecSettings();
                settings.setMsgLength(lengthSize);
            }
            codec.init(serviceContext, settings, getMessageFactory(), getDictionaryStructure());
        }
        return codec;
    }

    public static Integer extractLengthSize(IDictionaryStructure dictionaryStructure) {
        List<IMessageStructure> messages = dictionaryStructure.getMessageStructures();
        for (IMessageStructure struct : messages) {
        	if (MESSAGE_UNIT_HEADER_NAME.equals(struct.getName())) {
        		// Ignore UnitHeader
        		continue;
        	}
    		// We look for message with MessageType attribute and with field 'Length'
        	if (struct.getAttributeValueByName(ATTRIBUTE_MESSAGE_TYPE) != null
        			&& struct.getField(FIELD_LENGTH_NAME) != null) {
                IFieldStructure lengthField = struct.getField(FIELD_LENGTH_NAME);
                return (Integer) lengthField.getAttributeValueByName(ATTRIBUTE_LENGTH_NAME);
        	}
        }
        return null;
    }

    protected IMessage getFilledHeader(IMessageFactory msgFactory, Map<String, String> params, String messageNamespace) {
        if(headerStructure == null){
            headerStructure = getHeaderStructure(messageNamespace);
        }
        IMessage headerMessage = msgFactory.createMessage(headerStructure.getName(), messageNamespace);
        for(IFieldStructure fieldStructure : headerStructure.getFields()) {
            for(String fieldName: params.keySet()){
                if(fieldName.equals(fieldStructure.getName())) {
                    Object value = StructureUtils.castValueToJavaType(params.get(fieldName), fieldStructure.getJavaType());
                    headerMessage.addField(fieldStructure.getName(), value);
                    break;
                }
            }
        }
        return headerMessage;
    }

    protected IMessageStructure getHeaderStructure(String messageNamespace){
        List<IMessageStructure> messageStructures = getDictionaryStructure().getMessageStructures();
        IMessageStructure header = null;
        for(IMessageStructure messageStructure: messageStructures){
            if(messageStructure.getName().equals(MESSAGE_UNIT_HEADER_NAME)){
                header = messageStructure;
                break;
            }
        }
        if(header == null){
            throw new IllegalStateException("Header '" + MESSAGE_UNIT_HEADER_NAME + "' has not been found in dictionary with message namespace '"
                    + messageNamespace + "', title '" + getDictionaryStructure().getNamespace() + "'");
        }
        return header;
    }

    public static List<IMessage> extractSubmessages(Object message) {
        if(message instanceof IMessage) {
            return ((IMessage) message).<List<IMessage>>getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
        }

        throw new EPSCommonException("Sent message is not an " + IMessage.class.getSimpleName());
    }
}
