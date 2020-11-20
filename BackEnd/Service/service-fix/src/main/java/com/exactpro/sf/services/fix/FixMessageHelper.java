/******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.fix;

import static com.exactpro.sf.common.messages.structures.StructureUtils.getAttributeValue;
import static com.exactpro.sf.util.DateTimeUtility.getMillisecond;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.fix.converter.dirty.FieldConst;
import com.google.common.collect.ImmutableMap;

/**
 * @author nikita.smirnov
 *
 */
public class FixMessageHelper extends MessageHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(FixMessageHelper.class);
    
    public static final String ATTRIBUTE_TAG = "tag";
    public static final String HEADER = FieldConst.HEADER;
    public static final String TRAILER = FieldConst.TRAILER;
    
    public static final String BEGIN_STRING_FIELD = "BeginString";
    public static final String BODY_LENGTH_FIELD = "BodyLength";
    public static final String SENDER_COMP_ID_FIELD = "SenderCompID";
    public static final String TARGET_COMP_ID_FIELD = "TargetCompID";
    public static final String MSG_TYPE_FIELD = "MsgType";
    public static final String MSG_SEQ_NUM_FIELD = "MsgSeqNum";
    public static final String POSS_DUP_FLAG_FIELD = "PossDupFlag";
    public static final String POSS_RESEND_FIELD = "PossResend";
    public static final String SENDING_TIME_FIELD = "SendingTime";
    public static final String ORIG_SENDING_TIME_FIELD = "OrigSendingTime";
    public static final String APPL_VER_ID_FIELD = "ApplVerID";
    public static final String ON_BEHALF_OF_COMP_ID_FIELD = "OnBehalfOfCompID";
    public static final String DELIVER_TO_COMP_ID_FIELD = "DeliverToCompID";
    public static final String BEGIN_SEQ_NUM = "BeginSeqNo";
    public static final String END_SEQ_NUM = "EndSeqNo";

    public static final String MESSAGE_TYPE_ATTR_NAME = "MessageType";
    public static final String HAS_XML_FIELDS_ATTR_NAME = "HasXmlFields";
    public static final String XML_FIELD_TYPE = "XML";
    public static final String FIX_TAG = "fix_tag";

    public static final String CHECK_SUM = "CheckSum"; //TODO: find by tag


    public static final String HEARTBEAT_MESSAGE = "Heartbeat";
    public static final String TESTREQUEST_MESSAGE = "TestRequest";
    public static final String RESENDREQUEST_MESSAGE = "ResendRequest";
    public static final String SEQUENCERESET_MESSAGE = "SequenceReset";
    public static final String REJECT_MESSAGE = "Reject";
    public static final String LOGOUT_MESSAGE = "Logout";
    public static final String LOGON_MESSAGE = "Logon";

    public static final String ATTRIBUTE_ENTITY_TYPE = "entity_type";
    public static final String MESSAGE_ENTITY = "Message";
    public static final String COMPONENT_ENTITY = "Component";
    public static final String GROUP_ENTITY = "Group";
    public static final String HEADER_ENTITY = "Header";
    public static final String TRAILER_ENTITY = "Trailer";

    public static final String MULTIPLECHARVALUE = "MULTIPLECHARVALUE";
    public static final String MULTIPLESTRINGVALUE = "MULTIPLESTRINGVALUE";
    public static final String TZTIMESTAMP = "TZTIMESTAMP";
    public static final String TZTIMEONLY = "TZTIMEONLY";
    public static final String MONTH_YEAR = "MONTH-YEAR";
    public static final String DAY_OF_MONTH = "DAY-OF-MONTH";
    public static final String UTCTIMESTAMP_SECOND_PRECISION = "UTCTIMESTAMPSECONDPRECISION";
    public static final String PROTOCOL = "FIX";
    public static final String LANGUAGE = "LANGUAGE";
    public static final String XMLDATA = "XMLDATA";
    public static final List<String> HEADER_FIELDS_ORDER = ImmutableList.of(BEGIN_STRING_FIELD, BODY_LENGTH_FIELD, MSG_TYPE_FIELD);

    //    A field with tagNumber and type DATA must have a paired field with tagNumber-1 and type LENGTH.
    //    But there are exceptions, for example field Signature with tag 89 and type DATA has a paired field with tag 93 and type LENGTH.
    //    Fields with such tags should be excluded from validation.
    public static final Map<Integer, Integer> EXCEPTIONAL_DATA_LENGTH_TAGS = ImmutableMap.of(89, 93);

    @Override
    public AbstractCodec getCodec(IServiceContext serviceContext) {
        return null;
    }

    public IMessage prepareMessageToEncode(IMessage message, Map<String, String> params) {
        IMessage subMessage = null;
        
        if (message.isFieldSet(HEADER)) {
            subMessage = ((IMessage)message.getField(HEADER)).cloneMessage();
        } else {
            subMessage = getMessageFactory().createMessage(HEADER, getNamespace());
        }
        message.addField(HEADER, subMessage);
        
        if (!subMessage.isFieldSet(MSG_TYPE_FIELD)) {
            IMessageStructure structure = getDictionaryStructure().getMessages().get(message.getName());
            if (structure == null) {
                throw new EPSCommonException("Message " + message.getName() + " not found in dictionary " + getNamespace());
            }
            subMessage.addField(MSG_TYPE_FIELD, getAttributeValue(structure, MESSAGE_TYPE_ATTR_NAME));
        }
        
        if (message.isFieldSet(TRAILER)) {
            subMessage = ((IMessage)message.getField(TRAILER)).cloneMessage();
        } else {
            subMessage = getMessageFactory().createMessage(TRAILER, getNamespace());
        }
        message.addField(TRAILER, subMessage);

        return message;
    }

    @Override
    public long getSenderTime(IMessage message) {
        try {
            LocalDateTime sendingTime = message.<IMessage>getField(HEADER).getField(SENDING_TIME_FIELD);
            return getMillisecond(sendingTime);
        } catch(Exception e) {
            LOGGER.error("Failed to retrieve timestamp from message: {}", message, e);
        }

        return super.getSenderTime(message);
    }
}
