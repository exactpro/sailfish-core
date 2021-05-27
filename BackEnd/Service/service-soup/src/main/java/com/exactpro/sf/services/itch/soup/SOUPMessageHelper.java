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
package com.exactpro.sf.services.itch.soup;

import java.util.Map;

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.comparison.conversion.MultiConverter;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.itch.ITCHCodecSettings;

public class SOUPMessageHelper extends MessageHelper {

	public static final String ASCII_TYPE = "ASCII";
	public static final String VARIABLE_TYPE = "VARIABLE";
	public static final String ROUTE_ATTRIBUTE = "Route";
	public static final String ROUTE_ATTRIBUTE_OUTGOING = "Outgoing";
    public static final String ROUTE_ATTRIBUTE_INCOMING = "Incoming";
    public static final String DEBUG_PACKET_NAME = "DebugPacket";
    public static final String UNDOCUMENTED_ERROR_MESSAGE = "UndocumentedErrorMessage";
    public static final String SEQUENCED_DATA_PACKET = "SequencedDataPacket";
    public static final String UNSEQUENCED_DATA_PACKET = "UnsequencedDataPacket";
    public static final String SEQUENCED_HEADER_MESSAGE = "SequencedDataPacketHeader";
    public static final String UNSEQUENCED_HEADER_MESSAGE = "UnsequencedDataPacketHeader";
    public static final String PACKET_LENGTH = "PacketLength";
    public static final String PACKET_TYPE = "PacketType";
    public static final String ADMIN_MESSAGE_TYPE_ATTR = "AdminMessageType";
    public static final String MESSAGE_TYPE_FIELD = "MessageType";
    public static final String PACKET_HEADER_MESSAGE = "PacketHeader";
    public static final String SIZE_FIELD = "SizeField";
    public static final String SOUP_BIN_TCP_HEADER_NAME = "SoupBinHeader";
    public static final String MOLD_UDP_MESSAGE_HEADER_NAME = "MoldUdpMessageHeader";
    public static final String MESSAGE_LENGTH_FIELD_NAME = "Length";
    public static final int MESSAGE_LENGTH_FIELD_SIZE = 2;

	@Override
	public AbstractCodec getCodec(IServiceContext serviceContext) {
		SOUPCodec codec = new SOUPCodec();
        codec.init(serviceContext, new ITCHCodecSettings(), getMessageFactory(), getDictionaryStructure());
		return codec;
	}

    public IMessage createMessage(String name, Map<String, Object> values) {
        IMessageStructure structure = getDictionaryStructure().getMessages().get(name);
        IMessage result = getMessageFactory().createMessage(name, getDictionaryStructure().getNamespace());
        values.forEach((str, obj) -> setIfPresented(structure, result, str, obj));
        return prepareMessageToEncode(result, null);
    }

    private static void setIfPresented(IMessageStructure messageStructure, IMessage message, String field, Object value) {
        if (value == null) {
            return;
        }

        IFieldStructure fieldStructure = messageStructure.getFields().get(field);
        if (fieldStructure != null) {
            try {
                Class<?> clazz = Class.forName(fieldStructure.getJavaType().value());
                value = MultiConverter.convert(value, clazz);
                message.addField(field, value);
            } catch (ClassNotFoundException e) {
                throw new EPSCommonException("Cannot associate  [" + fieldStructure.getJavaType().value() + "] with any class" );
            }
        }
    }

}
