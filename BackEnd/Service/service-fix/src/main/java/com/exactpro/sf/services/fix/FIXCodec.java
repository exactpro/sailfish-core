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
package com.exactpro.sf.services.fix;

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.IMetadata;
import com.exactpro.sf.common.messages.MessageUtil;
import com.exactpro.sf.common.messages.MetadataExtensions;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.messages.service.ErrorMessage;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.fix.converter.MessageConvertException;
import com.exactpro.sf.services.fix.converter.dirty.DirtyQFJIMessageConverter;
import com.exactpro.sf.services.fix.converter.dirty.DirtyQFJIMessageConverterSettings;
import com.exactpro.sf.services.fix.converter.dirty.struct.RawMessage;
import com.exactpro.sf.services.tcpip.IFieldConverter;
import com.exactpro.sf.services.tcpip.MessageParseException;
import com.exactpro.sf.services.tcpip.TCPIPMessageHelper;
import com.exactpro.sf.services.tcpip.TCPIPSettings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.jetbrains.annotations.Nullable;
import org.quickfixj.CharsetSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.FieldException;
import quickfix.FieldMap;
import quickfix.FieldNotFound;
import quickfix.Group;
import quickfix.Message;
import quickfix.field.MsgType;

import javax.xml.parsers.DocumentBuilderFactory;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.exactpro.sf.common.messages.structures.StructureUtils.getAttributeValue;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class FIXCodec extends AbstractCodec {
	private static final Logger logger = LoggerFactory.getLogger(FIXCodec.class);
	public static final String SOH = "\001";
	public static final String SEP = "\\|";
    public static final String SUB_CONDITIONS_DELIMETER = ",";
    private static final String SOH_REPLACEMENT = SOH + "$1";

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
    private String separatorReplacePattern;
    private String fieldSeparator = SOH;
    private FIXBeginString beginStringByDictionary;
    private boolean preValidationMessage;

    @Override
    public void init(IServiceContext serviceContext, ICommonSettings settings, IMessageFactory msgFactory, IDictionaryStructure dictionary) {
        super.init(serviceContext, settings, msgFactory, dictionary);
        if (!(settings instanceof TCPIPSettings)) {
            throw new IllegalStateException("settings is not TCPIPSettings. settings is " + settings.getClass().getName());
        }

        this.settings = (TCPIPSettings)settings;
        if (this.settings.isVerifyMessageStructure() &&
                (!this.settings.isDecodeByDictionary() || this.settings.isDepersonalizationIncomingMessages())) {
            throw new IllegalArgumentException("The 'verification message structure' feature can't be enabled because "
                    + "the 'decode by dictionary' is " + this.settings.isDecodeByDictionary() + " (required 'true') and "
                    + "the 'depersonalization incoming messages' is " + this.settings.isDepersonalizationIncomingMessages() + " (required 'false')");
        }

        FixPropertiesReader.loadAndSetCharset(serviceContext);

        this.msgFactory = msgFactory;

        this.dictionary = Objects.requireNonNull(dictionary, "dictionary cannot be null");
        this.fieldConverter = new FixFieldConverter();
        fieldConverter.init(dictionary, dictionary.getNamespace());
        DirtyQFJIMessageConverterSettings dirtySettings = new DirtyQFJIMessageConverterSettings(dictionary, this.msgFactory)
                .setVerifyTags(this.settings.isVerifyMessageStructure())
                .setVerifyFields(this.settings.isVerifyMessageStructure())
                .setIncludeMilliseconds(this.settings.isIncludeMilliseconds())
                .setIncludeMicroseconds(this.settings.isIncludeMicroseconds())
                .setIncludeNanoseconds(this.settings.isIncludeNanoseconds());
        this.qfjConverter = new DirtyQFJIMessageConverter(dirtySettings);
        beginStringByDictionary = QFJDictionaryAdapter.extractFixVersion(dictionary.getNamespace());

        preValidationMessage = this.settings.isPreValidationMessage();

        this.msgStructures = new HashMap<>();

        messagesWithXmlField.clear();

        for(IMessageStructure messageStructure : dictionary.getMessages().values()) {
            String msgType = getAttributeValue(messageStructure, FixMessageHelper.MESSAGE_TYPE_ATTR_NAME);
            if(msgType != null) {
                msgStructures.put(msgType, messageStructure);

                Boolean hasXmlFields = getAttributeValue(messageStructure, FixMessageHelper.HAS_XML_FIELDS_ATTR_NAME);
                if (Boolean.TRUE.equals(hasXmlFields)) {
                    messagesWithXmlField.add(msgType);
                }
            }
        }
        this.dataDict = new QFJDictionaryAdapter(dictionary);
        dataDict.setAllowUnknownMessageFields(true);

        String filterValues = this.settings.getFilterMessages();
        messageFilter = isNotEmpty(filterValues) ? new TagValueFilter(filterValues) : new FakeFilter();

        String fieldSeparator = this.settings.getFieldSeparator();
        if (isNotEmpty(fieldSeparator) ) {
            this.fieldSeparator = fieldSeparator;
            this.separatorReplacePattern = format("\\%s+(\\d+=)|\\%s+$", fieldSeparator, fieldSeparator);
        } else {
            this.fieldSeparator = SOH;
            this.separatorReplacePattern = null;
        }
    }

    public static String getFixString(IoBuffer in) throws MessageParseException {
        return getFixString(in, SOH);
    }

	public static String getFixString(IoBuffer in, String fieldSeparator) throws MessageParseException {
	    int offset = in.position();
        byte[] buffer = new byte[in.remaining()];

        in.get(buffer);

        String out = new String(buffer, 0, buffer.length, Charset.forName(CharsetSupport.getCharset()));

        int beginStringIdx = out.indexOf("8=FIX");

        if(beginStringIdx == -1) {
            in.position(offset);
            return null;
        }

        if(beginStringIdx > 0) {
            in.position(offset + beginStringIdx);
            throw new MessageParseException("BeginString index is higher than 0", out);
        }

        int nextBeginStringIdx = out.indexOf(fieldSeparator + "8=FIX", beginStringIdx + 1);

        if(nextBeginStringIdx != -1) {
            nextBeginStringIdx += 1;  /*left last SOH in the current message*/
            out = out.substring(0, nextBeginStringIdx);
        }

        int checkSumIdx = out.indexOf(fieldSeparator + "10=");
        int tagDelimeterIdx = out.indexOf(fieldSeparator, checkSumIdx + 1);

        if(checkSumIdx == -1 || tagDelimeterIdx == -1) {
            if(nextBeginStringIdx != -1) {
                in.position(nextBeginStringIdx);
                throw new MessageParseException("CheckSum is absent or does no have SOH at the end. Next message starts at index " + nextBeginStringIdx, out);
            }

            in.position(offset);
            return null;
        }

        tagDelimeterIdx++;

        in.position(offset + tagDelimeterIdx);

        if(tagDelimeterIdx < out.length()) {
            out = out.substring(0, tagDelimeterIdx);
        }

        tagDelimeterIdx = out.indexOf(fieldSeparator);
        int lengthIdx = out.indexOf("9=");

        if(lengthIdx != tagDelimeterIdx + 1) {
            throw new MessageParseException("BodyLength is absent or not a second tag", out);
        }

        tagDelimeterIdx = out.indexOf(fieldSeparator, lengthIdx);

        try {
            int bodyLength = Integer.parseInt(out.substring(lengthIdx + 2, tagDelimeterIdx));

            if(bodyLength != checkSumIdx - tagDelimeterIdx) {
                throw new MessageParseException("BodyLength value differs from actual message length", out);
            }
        } catch(Exception e) {
            throw new MessageParseException("BodyLength value is invalid", out);
        }

        return out;
    }

    @Override
    protected boolean doDecodeInternal(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        String fixString;
        boolean decoded = false;

        while (true) {
            try {
                in.mark();
                fixString = getFixString(in, fieldSeparator);
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
                return true;
            }

            if (fixString == null) {
                return decoded;
            }

            logger.debug("doDecode: FixString = {}", fixString);
            IMessage message;

            if (!messageFilter.filterMessage(fixString)) {
                logger.info("FixString = {} ignored", fixString);
                continue;
            }

            String preprocessedRawMessage = isNotEmpty(separatorReplacePattern)
                    ? fixString.replaceAll(separatorReplacePattern, SOH_REPLACEMENT)
                    : fixString;
            try {
                message = settings.isDecodeByDictionary() ? convertToIMessageByIDictionaryStructure(preprocessedRawMessage) : convertToIMessage(preprocessedRawMessage);
                if (isNotEmpty(separatorReplacePattern)) {
                    message.getMetaData().setRawMessage(fixString.getBytes(CharsetSupport.getCharset()));
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);

                ErrorMessage errorMessage = new ErrorMessage(msgFactory);
                String exMessage = ObjectUtils.defaultIfNull(e.getMessage(), "");
                errorMessage.setCause(exMessage.replace('\001', '|'));

                message = errorMessage.getMessage();
                message.getMetaData().setRawMessage(preprocessedRawMessage.getBytes(CharsetSupport.getCharset()));
            }

            logger.debug("doDecode: IMessage = {}", message);
            out.write(message);
            decoded = true;
        }

    }

    @Override
    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
        if (message instanceof IMessage) {
            IMessage originIMessage = (IMessage)message;

            IMetadata metaData = originIMessage.getMetaData();
            IMessage encoded = settings.isEvolutionOptimize() ? originIMessage : originIMessage.cloneMessage();
            String messageName = getMessageName(encoded);
            byte[] rawMessage = settings.isVerifyMessageStructure() // that is enough to check that settings
                    // because if it is enabled other settings are enabled as well
                    ? encodeByDictionary(encoded)
                    : encodeDirty(encoded, messageName);

            MetadataExtensions.setRawMessage(metaData, rawMessage);

            IoBuffer buffer = IoBuffer.wrap(rawMessage);
            out.write(buffer);
        } else {
            out.write(message);
        }
    }

    private byte[] encodeDirty(IMessage encoded, String messageName) throws MessageConvertException {
        RawMessage rawMessage = qfjConverter.convertDirty(encoded, messageName);
        return Objects.requireNonNull(rawMessage, () -> "Raw message for " + encoded.getName() + " is `null`").getBytes();
    }

    private byte[] encodeByDictionary(IMessage encoded) throws MessageConvertException {
        return FixUtil.getRawMessage(qfjConverter.convert(encoded, beginStringByDictionary == FIXBeginString.FIXT_1_1));
    }

    protected IMessage convertToIMessageByIDictionaryStructure(String fixMessage) throws Exception {
        Message fixMessageSrc = new Message();
        fixMessageSrc.fromString(fixMessage, dataDict, true);

        if (preValidationMessage) {
            FieldException qfjException = fixMessageSrc.getException();
            if (qfjException != null) {
                throw new MessageParseException("Error parse the message data. " + qfjException.getMessage(), fixMessage);
            }
        }

        IMessage iMessage = settings.isDepersonalizationIncomingMessages() ?
                                convertMessageToIMessage(fixMessageSrc, dictionary.getNamespace()) :
                                qfjConverter.convert(fixMessageSrc);

        parseXmlFields(iMessage, new MessageFieldExtractor(fixMessageSrc),
                fixMessageSrc.getHeader().getString(MsgType.FIELD));

        if (fixMessageSrc.getException() != null) {
            iMessage.getMetaData().setRejectReason(fixMessageSrc.getException().getMessage());
        }

        if (settings.isDepersonalizationIncomingMessages()) {
            MsgMetaData metaData = iMessage.getMetaData();

            String unexpectedMessages = settings.getUnexpectedMessages();
            metaData.setAdmin(unexpectedMessages != null && !unexpectedMessages.trim().isEmpty() && isUnexpectedMessage(fixMessage, unexpectedMessages));
        }

        return iMessage;
    }

    protected IMessage convertToIMessage(String fixMessage) throws Exception {
        Map<String, String> fields = new HashMap<>();
        String[] tagValueArray = fixMessage.split(SOH);
        boolean rejected = false;

        for (String tagValue : tagValueArray) {
            if (StringUtils.isBlank(tagValue)) {
                throw new EPSCommonException("Empty tag-value pair");
            }

            int separatorIndex = tagValue.indexOf('=');

            if (separatorIndex == -1) {
                throw new EPSCommonException("No tag-value separator '=' in tag-value pair: " + tagValue);
            }

            String key = tagValue.substring(0, separatorIndex);
            String value = tagValue.substring(separatorIndex + 1);
            String convertedKey = (String)fieldConverter.convertValue(key, true);

            if (convertedKey == null) {
                logger.warn("Tag [{}] does not present in the [{}] dictionary, " +
                            "but exists in the [{}] incoming message", key, dictionary.getNamespace(), fixMessage);
                convertedKey = key;
                rejected = true;
            }

            fields.put(convertedKey, value);
        }

        String msgType = fields.get(FixMessageHelper.MSG_TYPE_FIELD);
        IMessageStructure messageStructure = msgStructures.get(msgType);

        String messageName;
        if(messageStructure == null) {
            logger.warn("MsgType {} is not exist in the dictionary", msgType);
            messageName = TCPIPMessageHelper.INCOMING_MESSAGE_NAME_AND_NAMESPACE;
        } else {
            messageName = messageStructure.getName();
        }

        IMessage iMessage =
                MessageUtil.convertToIMessage(fields, msgFactory, dictionary.getNamespace(), messageName);

        parseXmlFields(iMessage, new IMessageFieldExtractor(iMessage), iMessage.getField(FixMessageHelper.MSG_TYPE_FIELD));

        MsgMetaData metaData = iMessage.getMetaData();
        metaData.setRawMessage(fixMessage.getBytes());
        metaData.setRejected(rejected);
        String unexpectedMessages = settings.getUnexpectedMessages();
        metaData.setAdmin(unexpectedMessages != null && !unexpectedMessages.trim().isEmpty() && isUnexpectedMessage(fixMessage, unexpectedMessages));
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

    @Nullable
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
            return msgStructure == null ? null : msgStructure.getName();
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
        String messageName = msgStructures.get(msgType).getName();
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
        IMessageStructure headerStructure = dictionary.getMessages().get(FixMessageHelper.HEADER);
        IMessageStructure trailerStructure = dictionary.getMessages().get(FixMessageHelper.TRAILER);

        copyFields(message, iMessage, messageStructure);
        copyFields(message.getHeader(), header, headerStructure);
        copyFields(message.getTrailer(), trailer, trailerStructure);

        iMessage.getMetaData().setAdmin(message.isAdmin());
        iMessage.getMetaData().setRawMessage(FixUtil.getRawMessage(message));

        return iMessage;
    }

    private void copyFields(FieldMap message, IMessage iMessageTo, IFieldStructure messageStructure) throws Exception {

        for(IFieldStructure fieldStructure : messageStructure.getFields().values()) {

            if (fieldStructure.isComplex()) {
                String entType = getAttributeValue(fieldStructure, FixMessageHelper.ATTRIBUTE_ENTITY_TYPE);

                if (FixMessageHelper.GROUP_ENTITY.equals(entType)) {

                    Integer groupTag = getAttributeValue(fieldStructure, FixMessageHelper.ATTRIBUTE_TAG);
                    List<Group> groups = message.getGroups(groupTag);

                    if(!groups.isEmpty()) {
                        List<IMessage> groupList = new ArrayList<>();
                        addGroup(groups, groupList, fieldStructure);
                        if(!groupList.isEmpty()) {
                            iMessageTo.addField(fieldStructure.getName(), groupList);
                        }
                    }
                } else if(FixMessageHelper.COMPONENT_ENTITY.equals(entType)) {
                    IMessage iMessageComponent =
                            msgFactory.createMessage(fieldStructure.getName(), messageStructure.getNamespace());

                    IMessageStructure componentStructure = dictionary.getMessages().get(fieldStructure.getReferenceName());

                    copyFields(message, iMessageComponent, componentStructure);
                    if(!iMessageComponent.getFieldNames().isEmpty()) {
                        iMessageTo.addField(fieldStructure.getName(), Collections.singletonList(iMessageComponent));
                    }

                }
            } else {
                Integer tag = getAttributeValue(fieldStructure, FixMessageHelper.ATTRIBUTE_TAG);
                if(tag != null && message.isSetField(tag)) {
                    if(settings.isRemoveTrailingZeros()) {
                        try {
                            if(fieldStructure.getJavaType() == JavaType.JAVA_LANG_DOUBLE) {
                                String value = new BigDecimal(message.getString(tag)).stripTrailingZeros().toPlainString();
                                iMessageTo.addField(fieldStructure.getName(), value);
                            } else if(fieldStructure.getJavaType() == JavaType.JAVA_MATH_BIG_DECIMAL) {
                                String value = message.getDecimal(tag).stripTrailingZeros().toPlainString();
                                iMessageTo.addField(fieldStructure.getName(), value);
                            } else {
                                iMessageTo.addField(fieldStructure.getName(), message.getString(tag));
                            }
                        } catch (FieldNotFound e) {
                            throw new MessageConvertException("Field " + tag + " isn't found for copying to message " + iMessageTo.getName(), e);
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
           IMessageStructure msgStructure = msgStructures.get(msgType);
            if (msgStructure != null) {
                for(IFieldStructure fieldStructure : msgStructure.getFields().values()) {
                    String entityType = getAttributeValue(fieldStructure, FixMessageHelper.ATTRIBUTE_ENTITY_TYPE);
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
            Integer tag = getAttributeValue(fieldStructure, FixMessageHelper.FIX_TAG);
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
                    throw new EPSCommonException(format("Invalid filter [%s]." +
                            " Must have format 'tag:value' delimited by ';'", filters));
                }
                String tag = tagValuePair[0].trim();
                String value = tagValuePair[1];
                if (tag.isEmpty()) {
                    throw new EPSCommonException(format("Invalid filter [%s]. Tag is empty", filters));
                }
                String[] values = StringUtils.split(value, ",");
                filterTagValues.putAll(tag, Arrays.asList(values));
            }
        }
    }
}

