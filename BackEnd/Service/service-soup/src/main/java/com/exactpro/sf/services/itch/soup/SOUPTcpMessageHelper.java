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

import static com.exactpro.sf.common.messages.structures.StructureUtils.getAttributeValue;

import java.util.Map;
import java.util.Objects;

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.itch.ITCHCodecSettings;
import com.exactpro.sf.services.itch.ITCHVisitorBase;

public class SOUPTcpMessageHelper extends SOUPMessageHelper {

    //FIXME: Create a dictionary with common items and configure Bean code generation
    public static final String MESSAGE_LOGIN_REQUEST_PACKET = "LoginRequestPacket";
    public static final String MESSAGE_LOGOUT_REQUEST_PACKET = "LogoutRequestPacket";
    public static final String MESSAGE_ACCEPTED_LOGIN_PACKET = "LoginAcceptedPacket";
    public static final String MESSAGE_END_OF_SESSION_PACKET = "EndOfSessionPacket";
    public static final String MESSAGE_CLIENT_HEARTBEAT_NAME = "SubscriberHeartbeatPackets";
    public static final String MESSAGE_SERVER_HEARTBEAT_NAME = "ServerHeartbeatPackets";
    public static final String MESSAGE_LOGIN_REJECT_PACKET = "LoginRejectPacket";

    public static final String FIELD_REQUESTED_SEQUENCE_NUMBER = "RequestedSequenceNumber";
    public static final String FIELD_REQUESTED_SESSION = "RequestedSession";
    public static final String FIELD_REJECT_REASON_CODE = "RejectReasonCode";
    public static final String FIELD_SEQUENCE_NUMBER = "SequenceNumber";
    public static final String FIELD_USERNAME = "Username";
    public static final String FIELD_PASSWORD = "Password";
    public static final String FIELD_SESSION = "Session";

	@Override
	public AbstractCodec getCodec(IServiceContext serviceContext) {
		SOUPTcpCodec codec = new SOUPTcpCodec();
        codec.init(serviceContext, new ITCHCodecSettings(), getMessageFactory(), getDictionaryStructure());
		return codec;
	}


    @Override
    public IMessage prepareMessageToEncode(IMessage message, Map<String, String> params) {
        IDictionaryStructure dict = getDictionaryStructure();
        IMessageStructure struct = Objects.requireNonNull(dict.getMessages().get(message.getName()),
                () -> "Unknown message: " + message.getName());

        Boolean isAdmin = getAttributeValue(struct, ITCHVisitorBase.ISADMIN);

        if (Boolean.TRUE.equals(isAdmin)) {
            // FIXME: calculate full length for Unsequenced Message
            if (!message.isFieldSet("PacketLength")) {
                if(struct.getFields().get("PacketLength") != null && struct.getFields().get("PacketLength").getDefaultValue() == null) {
                    // calculate and fill length:
                    int length = -2; // don't count 'Length' fields size
                    for(IFieldStructure fld : struct.getFields().values()) {
                        Integer size = getAttributeValue(fld, ITCHVisitorBase.LENGTH_ATTRIBUTE);
                        if (size != null) {
                            length += size;
                        }
                    }
                    message.addField("PacketLength", length);
                }
            }
            if (!message.isFieldSet("PacketType")) {
                if(struct.getFields().get("PacketType") != null) {
                    if(struct.getFields().get("PacketType").getDefaultValue() == null) {
                        message.addField("PacketType", getAttributeValue(struct, "AdminMessageType"));
                    }
                }
            }
        } else {
            if (!message.isFieldSet("Length")) {
                if(struct.getFields().get("Length") != null && struct.getFields().get("Length").getDefaultValue() == null) {
                    // calculate and fill length:
                    int length = -2; // don't count 'Length' fields size
                    for(IFieldStructure fld : struct.getFields().values()) {
                        Integer size = getAttributeValue(fld, ITCHVisitorBase.LENGTH_ATTRIBUTE);
                        if (size != null) {
                            length += size;
                        }
                    }
                    message.addField("Length", length);
                }
            }
            if (!message.isFieldSet(MESSAGE_TYPE_FIELD)) {
                if(struct.getFields().get(MESSAGE_TYPE_FIELD) != null && struct.getFields().get(MESSAGE_TYPE_FIELD).getDefaultValue() == null) {
                    message.addField(MESSAGE_TYPE_FIELD, getAttributeValue(struct, ITCHVisitorBase.MESSAGE_TYPE_ATTRIBUTE));
                }
            }
        }

        return message;
    }

}
