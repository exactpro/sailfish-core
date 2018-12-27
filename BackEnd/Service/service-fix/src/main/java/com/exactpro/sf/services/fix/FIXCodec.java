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
package com.exactpro.sf.services.fix;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import com.exactpro.sf.messages.service.ErrorMessage;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MessageUtil;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.fix.converter.MessageConvertException;
import com.exactpro.sf.services.fix.converter.dirty.DirtyQFJIMessageConverter;
import com.exactpro.sf.services.fix.converter.dirty.struct.RawMessage;
import com.exactpro.sf.services.tcpip.IFieldConverter;
import com.exactpro.sf.services.tcpip.MessageParseException;
import com.exactpro.sf.services.tcpip.TCPIPMessageHelper;
import com.exactpro.sf.services.tcpip.TCPIPSettings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import quickfix.FieldMap;
import quickfix.FieldNotFound;
import quickfix.Group;
import quickfix.Message;
import quickfix.field.MsgType;

public class FIXCodec extends AbstractCodec {
	private static final Logger logger = LoggerFactory.getLogger(FIXCodec.class);
	public static final String SOH = "\001";
	public static final String SEP = "\\|";
	public static final String SUB_CONDITIONS_DELIMETER = ",";

	private TCPIPSettings settings;
	private IMessageFactory msgFactory;
	private IFieldConverter fieldConverter;
    private IDictionaryStructure dictionary;
    private DirtyQFJIMessageConverter qfjConverter;
    private Map<String, IMessageStructure> msgStructures;
    private QFJDictionaryAdapter dataDict;
    private IMessageFilter<String> messageFilter;
    private final Set<String> messagesWithXmlField = new HashSet<>();
    private final DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();

    @Override
	public void init(IServiceContext serviceContext, ICommonSettings settings,
			IMessageFactory msgFactory, IDictionaryStructure dictionary) {
        if (!(settings instanceof TCPIPSettings)) {
            throw new IllegalStateException("settings is not TCPIPSettings. settings is " + settings.getClass().getName());
        }
		if (msgFactory == null) {
		    throw new IllegalStateException("MessageFactory can not be null");
		}

		this.settings = (TCPIPSettings)settings;

        this.msgFactory = msgFactory;
		this.dictionary = Objects.requireNonNull(dictionary, "dictionary cannot be null");
		this.fieldConverter = new FixFieldConverter();
		this.fieldConverter.init(dictionary, dictionary.getNamespace());
		this.qfjConverter = new DirtyQFJIMessageConverter(dictionary, msgFactory, false, false, false);
		this.msgStructures = new HashMap<>();

        messagesWithXmlField.clear();

		for(IMessageStructure messageStructure : dictionary.getMessageStructures()) {
            String msgType = (String)messageStructure.getAttributeValueByName(FixMessageHelper.MESSAGE_TYPE_ATTR_NAME);
            if(msgType != null) {
                msgStructures.put(msgType, messageStructure);

                Boolean hasXmlFields = (Boolean) messageStructure
                        .getAttributeValueByName(FixMessageHelper.HAS_XML_FIELDS_ATTR_NAME);
                if (Boolean.TRUE.equals(hasXmlFields)) {
                    this.messagesWithXmlField.add(msgType);
                }
            }
        }
        this.dataDict = new QFJDictionaryAdapter(dictionary);
        this.dataDict.setAllowUnknownMessageFields(true);

        String filterValues = this.settings.getFilterMessages();
        messageFilter = StringUtils.isNotEmpty(filterValues) ? new TagValueFilter(filterValues) : new FakeFilter();
	}

	public static String getFixString(IoBuffer in) throws MessageParseException {
	    int offset = in.position();
        byte[] buffer = new byte[in.remaining()];

        in.get(buffer);

        String out = new String(buffer, 0, buffer.length, Charset.forName("ISO-8859-1"));

        int beginStringIdx = out.indexOf("8=FIX");

        if(beginStringIdx == -1) {
            in.position(offset);
            return null;
        }

        if(beginStringIdx > 0) {
            in.position(offset + beginStringIdx);
            throw new MessageParseException("BeginString index is higher than 0", out);
        }

        int nextBeginStringIdx = out.indexOf("8=FIX", beginStringIdx + 1);

        if(nextBeginStringIdx != -1) {
            out = out.substring(0, nextBeginStringIdx);
        }

        int checkSumIdx = out.indexOf(SOH + "10=");
        int sohIdx = out.indexOf(SOH, checkSumIdx + 1);

        if(checkSumIdx == -1 || sohIdx == -1) {
            if(nextBeginStringIdx != -1) {
                in.position(nextBeginStringIdx);
                throw new MessageParseException("CheckSum is absent or invalid", out);
            }

            in.position(offset);
            return null;
        }

        sohIdx++;

        in.position(offset + sohIdx);

        if(sohIdx < out.length()) {
            out = out.substring(0, sohIdx);
        }

        sohIdx = out.indexOf(SOH);
        int lengthIdx = out.indexOf("9=");

        if(lengthIdx != sohIdx + 1) {
            throw new MessageParseException("BodyLength is absent or not a second tag", out);
        }

        sohIdx = out.indexOf(SOH, lengthIdx);

        try {
            int bodyLength = Integer.parseInt(out.substring(lengthIdx + 2, sohIdx));

            if(bodyLength != checkSumIdx - sohIdx) {
                throw new MessageParseException("BodyLength value differs from actual message length", out);
            }
        } catch(Exception e) {
            throw new MessageParseException("BodyLength value is invalid", out);
        }

        return out;
    }

    @Override
    protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        String fixString;
        boolean decoded = false;

        while (true) {
            try {
                in.mark();
                fixString = getFixString(in);
            } catch (MessageParseException e) {
                logger.error(e.getMessage(), e);
                int endPosition = in.position();
                in.reset();
                byte[] raw = new byte[endPosition - in.position()];
                in.get(raw);

                ErrorMessage errorMessage = new ErrorMessage(msgFactory);
                errorMessage.setCause("Can not slice input: " + e.getMessage());

                IMessage message = errorMessage.getMessage();
                message.getMetaData().setRawMessage(raw);

                out.write(message);
                decoded = true;
                break;
            }

            if (fixString == null) {
                break;
            }

            logger.debug("doDecode: FixString = {}", fixString);
            IMessage message;

            if (!messageFilter.filterMessage(fixString)) {
                logger.info("FixString = {} ignored", fixString);
                continue;
            }

            try {
                if (settings.isDecodeByDictionary()) {
                    message = convertToIMessageByIDictionaryStructure(fixString);
                } else {
                    message = convertToIMessage(fixString);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);

                ErrorMessage errorMessage = new ErrorMessage(msgFactory);
                String exMessage = ObjectUtils.defaultIfNull(e.getMessage(), "");
                errorMessage.setCause(exMessage.replace('\001', '|'));

                message = errorMessage.getMessage();
                message.getMetaData().setRawMessage(fixString.getBytes(StandardCharsets.ISO_8859_1));
            }

            logger.debug("doDecode: IMessage = {}", message);
            out.write(message);
            decoded = true;
        }

        return decoded;
    }

    @Override
    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
        if (message instanceof IMessage) {
            IMessage originIMessage = ((IMessage) message);

            MsgMetaData metaData = originIMessage.getMetaData();
            IMessage encoded = originIMessage.cloneMessage();
            String messageName = getMessageName(encoded);
            RawMessage rawMessage = qfjConverter.convertDirty(encoded, messageName);

            metaData.setRawMessage(rawMessage.getBytes());

            IoBuffer buffer = IoBuffer.wrap(metaData.getRawMessage());
            out.write(buffer);
        } else {
            out.write(message);
        }
    }

    protected IMessage convertToIMessageByIDictionaryStructure(String fixMessage) throws Exception {
        Message fixMessageSrc = new Message();
        fixMessageSrc.fromString(fixMessage, dataDict, true);

        IMessage iMessage = settings.isDepersonalizationIncomingMessages() ?
                                convertMessageToIMessage(fixMessageSrc, dictionary.getNamespace()) :
                                qfjConverter.convert(fixMessageSrc, false, false);

        parseXmlFields(iMessage, new MessageFieldExtractor(fixMessageSrc),
                fixMessageSrc.getHeader().getString(MsgType.FIELD));

        if (fixMessageSrc.getException() != null) {
            iMessage.getMetaData().setRejectReason(fixMessageSrc.getException().getMessage());
        }

        if (settings.isDepersonalizationIncomingMessages()) {
            MsgMetaData metaData = iMessage.getMetaData();

            String unexpectedMessages = settings.getUnexpectedMessages();
            if (unexpectedMessages != null && !unexpectedMessages.trim().isEmpty()) {
                metaData.setAdmin(isUnexpectedMessage(fixMessage, unexpectedMessages));
            } else {
                metaData.setAdmin(false);
            }
        }

        return iMessage;
    }

    protected IMessage convertToIMessage(String fixMessage) throws Exception {
        Map<String, String> fields = new HashMap<>();
        String[] tagValueArray = fixMessage.split(SOH);
        boolean rejected = false;
        for (int i = 0; i < tagValueArray.length; i++) {
            int separatorIndex = tagValueArray[i].indexOf("=");
            String key = tagValueArray[i].substring(0, separatorIndex);
            String value = tagValueArray[i].substring(separatorIndex + 1);
            String convertedKey = (String) this.fieldConverter.convertValue(key, true);
            if (convertedKey == null) {
                logger.warn("Tag [{}] does not present in the [{}] dictionary, " +
                            "but exists in the [{}] incoming message", key, dictionary.getNamespace(), fixMessage);
                convertedKey = key;
                rejected = true;
            }
            fields.put(convertedKey, value);
        }

        String msgType = fields.get(FixMessageHelper.MSG_TYPE_FIELD);
        IMessageStructure messageStructure = this.msgStructures.get(msgType);

        String messageName;
        if(messageStructure == null) {
            logger.warn("MsgType {} is not exist in the dictionary", msgType);
            messageName = TCPIPMessageHelper.INCOMING_MESSAGE_NAME_AND_NAMESPACE;
        } else {
            messageName = messageStructure.getName();
        }

        IMessage iMessage =
                MessageUtil.convertToIMessage(fields, this.msgFactory, this.dictionary.getNamespace(), messageName);

        parseXmlFields(iMessage, new IMessageFieldExtractor(iMessage), iMessage.getField(FixMessageHelper.MSG_TYPE_FIELD));

        MsgMetaData metaData = iMessage.getMetaData();
        metaData.setRawMessage(fixMessage.getBytes());
        metaData.setRejected(rejected);
        String unexpectedMessages = settings.getUnexpectedMessages();
        if (unexpectedMessages != null && !unexpectedMessages.trim().isEmpty()) {
            metaData.setAdmin(isUnexpectedMessage(fixMessage, unexpectedMessages));
        } else {
            metaData.setAdmin(false);
        }
        return iMessage;
    }

	protected boolean isUnexpectedMessage(String fixMessage, String unexpectedMessages){
		String[] conditions = unexpectedMessages.split(SEP);
		for(String condition:conditions){
			boolean check = false;
			String[] subConditions = condition.split(SUB_CONDITIONS_DELIMETER);
			for(String subCondition:subConditions){
				if(fixMessage.contains(subCondition.trim())){
					check = true;
				} else {
					break;
				}
			}
			if(check){
				return true;
			}
		}
		return false;
	}

    protected String getMessageName(IMessage message) {
        String targetMsgType = message.getField(FixMessageHelper.MSG_TYPE_FIELD);

        if(targetMsgType == null) {
            Object headerField = message.getField(FixMessageHelper.HEADER);
            IMessage header = MessageUtil.extractMessage(headerField);

            if(header != null) {
                targetMsgType = header.getField(FixMessageHelper.MSG_TYPE_FIELD);
            }
        }

        if(targetMsgType != null) {
            IMessageStructure msgStructure = msgStructures.get(targetMsgType);
            return msgStructure != null ? msgStructure.getName() : null;
        }

        return null;
    }

    protected IMessageStructure getMessageStructure(String msgTypeValue) {
        if(msgStructures.containsKey(msgTypeValue)) {
            return msgStructures.get(msgTypeValue);
        }

        throw new RuntimeException("no message structure for msgType: " + msgTypeValue);
    }
    private IMessage convertMessageToIMessage(Message message, String namespace) throws Exception {
        String msgType = message.getHeader().getString(MsgType.FIELD);
        String messageName = this.msgStructures.get(msgType).getName();
        IMessage iMessage = msgFactory.createMessage(messageName, namespace);

        // create header / trailer if not exist
        IMessage header = (IMessage) iMessage.getField(FixMessageHelper.HEADER);
        if (header == null) {
            header = msgFactory.createMessage(FixMessageHelper.HEADER, namespace);
            iMessage.addField(FixMessageHelper.HEADER, header);
        }
        IMessage trailer = (IMessage) iMessage.getField(FixMessageHelper.TRAILER);
        if (trailer == null) {
            trailer = msgFactory.createMessage(FixMessageHelper.TRAILER, namespace);
            iMessage.addField(FixMessageHelper.TRAILER, trailer);
        }

        IMessageStructure messageStructure =
                getMessageStructure(message.getHeader().getString(MsgType.FIELD));
        IMessageStructure headerStructure = dictionary.getMessageStructure(FixMessageHelper.HEADER);
        IMessageStructure trailerStructure = dictionary.getMessageStructure(FixMessageHelper.TRAILER);

        copyFields(message, iMessage, messageStructure);
        copyFields(message.getHeader(), header, headerStructure);
        copyFields(message.getTrailer(), trailer, trailerStructure);

        iMessage.getMetaData().setAdmin(message.isAdmin());
        iMessage.getMetaData().setRawMessage(message.toString().getBytes());

        return iMessage;
    }

    private void copyFields(FieldMap message, IMessage iMessageTo, IFieldStructure messageStructure) throws Exception {

        for (IFieldStructure fieldStructure : messageStructure.getFields()) {

            if (fieldStructure.isComplex()) {
                String entType = (String) fieldStructure.getAttributeValueByName(FixMessageHelper.ATTRIBUTE_ENTITY_TYPE);

                if (entType.equals(FixMessageHelper.GROUP_ENTITY)) {

                    Integer groupTag = (Integer)fieldStructure.getAttributeValueByName(FixMessageHelper.ATTRIBUTE_TAG);
                    List<Group> groups = message.getGroups(groupTag);

                    if(groups.size() != 0) {
                        List<IMessage> groupList = new ArrayList<>();
                        addGroup(groups, groupList, fieldStructure);
                        if(groupList.size() != 0) {
                            iMessageTo.addField(fieldStructure.getName(), groupList);
                        }
                    }
                } else if(entType.equals(FixMessageHelper.COMPONENT_ENTITY)) {
                    IMessage iMessageComponent =
                            msgFactory.createMessage(fieldStructure.getName(), messageStructure.getNamespace());

                    IMessageStructure componentStructure = dictionary.getMessageStructure(fieldStructure.getReferenceName());

                    copyFields(message, iMessageComponent, componentStructure);
                    if(iMessageComponent.getFieldNames().size() != 0) {
                        iMessageTo.addField(fieldStructure.getName(), Arrays.asList(iMessageComponent));
                    }

                }
            } else {
                Integer tag = (Integer)fieldStructure.getAttributeValueByName(FixMessageHelper.ATTRIBUTE_TAG);
                if(tag != null && message.isSetField(tag)) {
                    if(this.settings.isRemoveTrailingZeros()) {
                        try {
                            if (JavaType.JAVA_LANG_DOUBLE.equals(fieldStructure.getJavaType())) {
                                String value = new BigDecimal(message.getString(tag)).stripTrailingZeros().toPlainString();
                                iMessageTo.addField(fieldStructure.getName(), value);
                            } else if (JavaType.JAVA_MATH_BIG_DECIMAL.equals(fieldStructure.getJavaType())) {
                                String value = message.getDecimal(tag).stripTrailingZeros().toPlainString();
                                iMessageTo.addField(fieldStructure.getName(), value);
                            } else {
                                iMessageTo.addField(fieldStructure.getName(), message.getString(tag));
                            }
                        } catch (FieldNotFound e) {
                            throw new MessageConvertException(message, "", e);
                        }
                    } else {
                        iMessageTo.addField(fieldStructure.getName(), message.getString(tag));
                    }
                }
            }
        }
    }

    private void addGroup(List<Group> groups, List<IMessage> groupList, IFieldStructure groupStructure) throws Exception {
        for(Group group : groups) {
            IMessage groupMessage = msgFactory.createMessage(groupStructure.getName(), groupStructure.getNamespace());
            copyFields(group, groupMessage, groupStructure);
            groupList.add(groupMessage);
        }
    }

    private IMessage parseXmlFields(IMessage targetMessage, IFieldExtractor fieldExtractor, String msgType)
            throws Exception {
       if (msgType != null && messagesWithXmlField.contains(msgType)) {
            IMessageStructure msgStructure = this.msgStructures.get(msgType);
            if (msgStructure != null) {
                for (IFieldStructure fieldStructure : msgStructure.getFields()) {
                    String entityType = (String) fieldStructure
                            .getAttributeValueByName(FixMessageHelper.ATTRIBUTE_ENTITY_TYPE);
                    if (FixMessageHelper.XML_FIELD_TYPE.equals(entityType)) {
                        IMessage xmlSubMessage = msgFactory.createMessage(
                                fieldStructure.getName(), fieldStructure.getNamespace());
                        FixUtil.parseXmlFields(fieldExtractor.extractXmlField(fieldStructure), xmlSubMessage,
                                dictionary, fieldStructure, documentFactory,
                                settings.isDecodeByDictionary() && !settings.isDepersonalizationIncomingMessages());
                        targetMessage.addField(fieldStructure.getName(), xmlSubMessage);
                    }
                }
            }
        }
        return targetMessage;
    }

    private interface IFieldExtractor {
      String extractXmlField(IFieldStructure fieldStructure) throws Exception;
    }

    private class MessageFieldExtractor implements IFieldExtractor {
      private final Message message;

      MessageFieldExtractor(Message message) {
            this.message = message;
        }

        @Override
        public String extractXmlField(IFieldStructure fieldStructure) throws Exception {
            Integer tag = (Integer) fieldStructure.getAttributeValueByName(FixMessageHelper.FIX_TAG);
            return message.getString(tag);
        }
    }

    private class IMessageFieldExtractor implements IFieldExtractor {
        private final IMessage message;

        public IMessageFieldExtractor(IMessage message) {
            this.message = message;
        }

        @Override
        public String extractXmlField(IFieldStructure fieldStructure) throws Exception {
            return message.getField(fieldStructure.getName()).toString();
        }
    }

    private interface IMessageFilter<T> {
        boolean filterMessage(T message);
    }

    private class FakeFilter implements IMessageFilter<String> {
        @Override
        public boolean filterMessage(String message) {
            return true;
        }
    }

    private class TagValueFilter implements IMessageFilter<String> {

        private final SetMultimap<String, String> filterTagValues = HashMultimap.create();

        TagValueFilter(String filters) {
            parseFilter(filters);
        }

        @Override
        public boolean filterMessage(String message) {
            for (String tag : filterTagValues.keySet()) {
                int startIdx = message.indexOf(tag + "=");
                int endIdx = message.indexOf(SOH, startIdx);
                if (startIdx == -1 || endIdx == -1) {
                    return false;
                }
                String tagValue = message.substring(startIdx + tag.length() + 1,  endIdx);
                if (!filterTagValues.get(tag).contains(tagValue)) {
                    return false;
                }
            }
            return true;
        }

        private void parseFilter(String filters) {
            String[] tagValues = StringUtils.split(filters, ";");
            for (String tagValue : tagValues) {
                String[] tagValuePair = StringUtils.split(tagValue, ":");
                if (tagValuePair.length != 2) {
                    throw new EPSCommonException(String.format("Invalid filter [%s]." +
                            " Must have format 'tag:value' delimited by ';'", filters));
                }
                String tag = tagValuePair[0].trim();
                String value = tagValuePair[1];
                if (tag.isEmpty()) {
                    throw new EPSCommonException(String.format("Invalid filter [%s]. Tag is empty", filters));
                }
                String[] values = StringUtils.split(value, ",");
                filterTagValues.putAll(tag, Arrays.asList(values));
            }
        }
    }
}

