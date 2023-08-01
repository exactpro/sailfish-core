/*
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
 */
package com.exactpro.sf.services.fix.converter;

import static com.exactpro.sf.common.messages.structures.StructureUtils.getAttributeValue;
import static com.exactpro.sf.services.fix.FixMessageHelper.EXCEPTIONAL_DATA_LENGTH_TAGS;
import static com.exactpro.sf.services.fix.QFJDictionaryAdapter.ATTRIBUTE_FIX_TYPE;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.IMetadata;
import com.exactpro.sf.common.messages.MessageUtil;
import com.exactpro.sf.common.messages.MetadataExtensions;
import com.exactpro.sf.common.messages.MetadataProperty;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.services.fix.FixMessageHelper;
import com.exactpro.sf.services.fix.FixUtil;
import com.exactpro.sf.services.fix.ISailfishMessage;
import com.exactpro.sf.util.DateTimeUtility;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;

import quickfix.Field;
import quickfix.FieldException;
import quickfix.FieldMap;
import quickfix.FieldNotFound;
import quickfix.FieldType;
import quickfix.FixVersions;
import quickfix.Group;
import quickfix.Message;
import quickfix.Message.Header;
import quickfix.field.BeginString;
import quickfix.field.BodyLength;
import quickfix.field.CheckSum;
import quickfix.field.MsgSeqNum;
import quickfix.field.MsgType;

public class QFJIMessageConverter
{
	private static final Logger logger = LoggerFactory.getLogger(QFJIMessageConverter.class);
    private static final Map<MetadataProperty, BiConsumer<IMetadata, IMessageStructure>> REQUIRED_METADATA_PROPERTIES = ImmutableMap.<MetadataProperty, BiConsumer<IMetadata, IMessageStructure>>builder()
            .put(MetadataProperty.NAME, (data, structure) -> MetadataExtensions.setName(data, structure.getName()))
            .put(MetadataProperty.NAMESPACE, (data, structure) -> MetadataExtensions.setNamespace(data, structure.getNamespace()))
            .put(MetadataProperty.TIMESTAMP, (data, structure) -> MetadataExtensions.setTimestamp(data, new Date()))
            .put(MetadataProperty.ID, (data, structure) -> MetadataExtensions.setId(data, MessageUtil.generateId()))
            .put(MetadataProperty.SEQUENCE, (data, structure) -> MetadataExtensions.setSequence(data, MessageUtil.generateSequence()))
            .put(MetadataProperty.PRECISE_TIMESTAMP, (data, structure) -> MetadataExtensions.setPreciseTimestamp(data, Instant.now()))
            .build();

	protected static final String ATTRIBUTE_TAG = "tag";
	protected static final String ATTRIBUTE_ENTITY_TYPE = "entity_type";
	protected static final String ATTRIBUTE_MESSAGE_TYPE = "MessageType";

	protected final int[] fieldOrderHeader;
	protected final int[] fieldOrderTrailer;
	
	protected final IDictionaryStructure dictionary;
	protected final IMessageFactory factory;
	private final boolean verifyTags;
	protected final boolean includeMilliseconds;
	protected final boolean includeMicroseconds;
    protected final boolean includeNanoseconds;
	private final boolean skipTags;
	protected final boolean orderingFields;

	private final Map<String, IMessageStructure> typeToStructure = new HashMap<>();
	private final Table<Integer, String, List<IFieldStructure>> tagToPath = HashBasedTable.create();
	private final Table<Integer, String, IFieldStructure> tagToStructure = HashBasedTable.create();

	private final List<Integer> tagsToSkip = Arrays.asList(MsgSeqNum.FIELD, BodyLength.FIELD, CheckSum.FIELD);
	
    public QFJIMessageConverter(QFJIMessageConverterSettings settings) {
        this.dictionary = settings.getDictionary();
        this.factory = settings.getFactory();
        this.verifyTags = settings.isVerifyTags();
        this.includeMilliseconds = settings.isIncludeMilliseconds();
        this.includeMicroseconds = settings.isIncludeMicroseconds();
        this.includeNanoseconds = settings.isIncludeNanoseconds();
        this.skipTags = settings.isSkipTags();
        this.orderingFields = settings.isOrderingFields();
        
        if (orderingFields) {
            this.fieldOrderHeader = getFieldOrderPrimitive(dictionary.getMessages().get(FixMessageHelper.HEADER));
            this.fieldOrderTrailer = getFieldOrderPrimitive(dictionary.getMessages().get(FixMessageHelper.TRAILER));
        } else {
            this.fieldOrderHeader = null;
            this.fieldOrderTrailer = null;
        }
        
        indexMessages();
    }

	private void indexMessages() {
        for(IMessageStructure messageStructure : dictionary.getMessages().values()) {
            String messageType = getAttributeValue(messageStructure, ATTRIBUTE_MESSAGE_TYPE);

            if(messageType != null) {
                typeToStructure.put(messageType, messageStructure);
            }

            indexMessageFields(messageStructure, new ArrayList<IFieldStructure>(), messageStructure);
        }
	}

	private void indexMessageFields(IFieldStructure messageStructure, ArrayList<IFieldStructure> fieldPath, IMessageStructure rootMessage) {
        String messageName = rootMessage.getName();

        for(IFieldStructure fieldStructure : messageStructure.getFields().values()) {
            Integer fieldTag = getAttributeValue(fieldStructure, ATTRIBUTE_TAG);

            if(fieldTag != null) {
                tagToStructure.put(fieldTag, messageName, fieldStructure);
                tagToPath.put(fieldTag, messageName, fieldPath);
            }

            if(fieldStructure.isComplex() && !fieldStructure.isCollection()) {
                ArrayList<IFieldStructure> newFieldPath = new ArrayList<>();

                newFieldPath.addAll(fieldPath);
                newFieldPath.add(fieldStructure);

                indexMessageFields(fieldStructure, newFieldPath, rootMessage);
            }
        }
	}

	//
	// QFJ to SF
	//
	@Nullable
    public IMessage convert(@Nullable Message message) throws MessageConvertException {
		return convert(message, null, null, false);
	}

    @Nullable
    public IMessage convert(@Nullable Message message, Boolean verifyTagsOverride, Boolean skipTagsOverride) throws MessageConvertException {
        return convert(message, verifyTagsOverride, skipTagsOverride, false);
    }

    @Nullable
    public IMessage convert(@Nullable Message message, @Nullable Boolean verifyTagsOverride, @Nullable Boolean skipTagsOverride,
            boolean ignoreFieldType) throws MessageConvertException {

        if (message == null) {
            return null;
        }
        try {
            IMessageStructure messageStructure = getIMessageStructure(message);

            IMessage resultMessage = createIMessage(message, messageStructure);

            IMessage messageHeader = factory.createMessage(FixMessageHelper.HEADER, messageStructure.getNamespace());
            IMessage messageTrailer = factory.createMessage(FixMessageHelper.TRAILER, messageStructure.getNamespace());

            resultMessage.addField(FixMessageHelper.HEADER, messageHeader);
            resultMessage.addField(FixMessageHelper.TRAILER, messageTrailer);

            boolean verifyTags = ObjectUtils.defaultIfNull(verifyTagsOverride, this.verifyTags);
            boolean skipTags = ObjectUtils.defaultIfNull(skipTagsOverride, this.skipTags);

            traverseMessage(resultMessage, message, factory, resultMessage, verifyTags, skipTags, ignoreFieldType);
            traverseMessage(resultMessage, message.getHeader(), factory, resultMessage, verifyTags, skipTags, ignoreFieldType);
            traverseMessage(resultMessage, message.getTrailer(), factory, resultMessage, verifyTags, skipTags, ignoreFieldType);


            // dictionaryName, environment, fromService, toService - will be filled in FixToImessageConvertingHandler
            resultMessage.getMetaData().setAdmin(message.isAdmin());
            resultMessage.getMetaData().setRawMessage(FixUtil.getRawMessage(message));

            if (message.getException() != null) {
                resultMessage.getMetaData().setRejectReason(message.getException().getMessage());
            }

            return resultMessage;
        } catch (FieldNotFound e) {
            throw new MessageConvertException("Failed to get message type, raw message: " + message, e);
        }
    }

    private IMessage createIMessage(@NotNull Message message, IMessageStructure messageStructure) {
        return message instanceof ISailfishMessage
                ? factory.createMessage(preprocessMetadata((ISailfishMessage)message, messageStructure))
                : factory.createMessage(messageStructure.getName(), messageStructure.getNamespace());
    }

    private MsgMetaData preprocessMetadata(ISailfishMessage message, IMessageStructure messageStructure) {
        MsgMetaData metadata = message.getMetadata();

        //region Set required properties. They might not be set when we send a raw message with extra metadata
        REQUIRED_METADATA_PROPERTIES.forEach((property, action) -> {
            if (!MetadataExtensions.contains(metadata, property)) {
                logger.trace("The required property {} is not set. Applying action", property);
                action.accept(metadata, messageStructure);
            }
        });
        //endregion

        return metadata;
    }

    @NotNull
    private IMessageStructure getIMessageStructure(@NotNull Message message) throws FieldNotFound, MessageConvertException {
        String messageType = message.getHeader().getString(MsgType.FIELD);

        IMessageStructure messageStructure = typeToStructure.get(messageType);

        if (messageStructure == null) {
            throw new MessageConvertException("Unknown message type: " + messageType);
        }
        return messageStructure;
    }

    @Nullable
    public IMessage convertEvolution(@Nullable Message message) throws MessageConvertException {
        try {
            if (message == null) {
                return null;
            }

            IMessageStructure messageStructure = getIMessageStructure(message);

            IMessage resultMessage = createIMessage(message, messageStructure);
            resultMessage.getMetaData().setAdmin(message.isAdmin());
            resultMessage.getMetaData().setRawMessage(FixUtil.getRawMessage(message));

            if (message.getException() != null) {
                resultMessage.getMetaData().setRejectReason(message.getException().getMessage());
            }

            return resultMessage;
        } catch (FieldNotFound e) {
            throw new MessageConvertException("Failed to get message type, raw message: " + message, e);
        }
    }

    protected void traverseMessage(IMessage resultMessage, FieldMap message, IMessageFactory factory,
            IMessage rootMessage, boolean verifyTags,
            boolean skipTags, boolean ignoreFieldType) throws MessageConvertException {
		String messageName = rootMessage.getName();
		Iterator<Field<?>> it = message.iterator();

		while(it.hasNext()) {
			Field<?> field = it.next();
			int fieldTag = field.getTag();

			if(skipTags && tagsToSkip.contains(fieldTag)) {
			    continue;
			}

            if(!tagToStructure.containsRow(fieldTag)) {
                if(verifyTags) {
                    throw new MessageConvertException("Unknown tag: " + fieldTag + ", verify tags enabled");
                }

                continue;
            }

			IFieldStructure fieldStructure = tagToStructure.get(fieldTag, messageName);

            if(fieldStructure == null) {
                if(verifyTags) {
                    throw new MessageConvertException("Message '" + messageName + "' doesn't contain tag: " + fieldTag + ", verify tags enabled");
                }

                continue;
            }

			List<IFieldStructure> fieldPath = tagToPath.get(fieldTag, messageName);

			if(fieldPath == null) {
			    if(verifyTags) {
			        //TODO Clarefy error description for user
                    throw new MessageConvertException("No field path in the message '" + messageName + "' for tag: " + fieldTag + ", verify tags enabled");
                }

			    continue;
			}

			String fieldName = fieldStructure.getName();
			IMessage messageComponent = getMessageComponent(fieldPath, resultMessage);

			if (fieldStructure.isComplex() && fieldStructure.isCollection()) {
				List<IMessage> iGroups = new ArrayList<>();

                for(Group group : message.getGroups(fieldTag)) {
                    try {
                        IMessage iGroup = factory.createMessage(fieldStructure.getReferenceName(), fieldStructure.getNamespace());
                        traverseMessage(iGroup, group, factory, iGroup, verifyTags, skipTags, ignoreFieldType);
                        iGroups.add(iGroup);
                    } catch (MessageConvertException e) {
                        throw new MessageConvertException("Group '" + group.getFieldTag() + "' can't be parsed. " + e.getMessage(), e);
                    }
                }

                if (!iGroups.isEmpty()) {
                    messageComponent.addField(fieldName, iGroups);
                } else {
                    //group is empty, lets find group wrapper and remove it
                    IMessage componentParent = resultMessage;
                    for (int i = 0; i < fieldPath.size() - 1; i++) {
                        componentParent = componentParent.getField(fieldPath.get(i).getName());
                    }

                    if (!fieldPath.isEmpty()) {
                        String fieldToRemove = fieldPath.get(fieldPath.size() - 1).getName();
                        componentParent.removeField(fieldToRemove);
                    }
                }
			} else {
				try {
					if (message.getString(fieldTag).isEmpty()) {
						// it from SETTING_VALIDATE_FIELDS_HAVE_VALUES
						// don't add it. It can break some traverse of this message.
					    continue;
					}
                    if (ignoreFieldType) {
                        messageComponent.addField(fieldName, message.getString(fieldTag));
                        continue;
                    }

					switch(fieldStructure.getJavaType()) {
					case JAVA_LANG_BOOLEAN:
					    messageComponent.addField(fieldName, message.getBoolean(fieldTag));
					    continue;
					case JAVA_LANG_CHARACTER:
					    messageComponent.addField(fieldName, message.getChar(fieldTag));
					    continue;
					case JAVA_LANG_INTEGER:
					    messageComponent.addField(fieldName, message.getInt(fieldTag));
					    continue;
					case JAVA_LANG_DOUBLE:
					    messageComponent.addField(fieldName, message.getDouble(fieldTag));
					    continue;
					case JAVA_MATH_BIG_DECIMAL:
					    messageComponent.addField(fieldName, message.getDecimal(fieldTag));
					    continue;
					case JAVA_LANG_STRING:
					    messageComponent.addField(fieldName, message.getString(fieldTag));
					    continue;
                    case JAVA_TIME_LOCAL_DATE_TIME:
                        LocalDateTime dateTime = DateTimeUtility.toLocalDateTime(message.getUtcTimeStamp(fieldTag));
                        messageComponent.addField(fieldName, dateTime);
                        continue;
                    case JAVA_TIME_LOCAL_DATE:
                        LocalDate date = DateTimeUtility.toLocalDate(message.getUtcDateOnly(fieldTag));
                        messageComponent.addField(fieldName, date);
                        continue;
                    case JAVA_TIME_LOCAL_TIME:
                        LocalTime time = DateTimeUtility.toLocalTime(message.getUtcTimeOnly(fieldTag));
                        messageComponent.addField(fieldName, time);
                        continue;
				    default:
				        messageComponent.addField(fieldName, field.getObject());
				        continue;
					}
                } catch(FieldNotFound | FieldException e) {
                    throw new MessageConvertException("Getting field " + fieldName + "problem", e);
                }
			}
		}
	}

    private IMessage getMessageComponent(List<IFieldStructure> fieldPath, IMessage message) {
        for(IFieldStructure fieldStructure : fieldPath) {
            IMessage messageComponent = message.getField(fieldStructure.getName());

            if(messageComponent == null) {
                messageComponent = factory.createMessage(fieldStructure.getName(), fieldStructure.getNamespace());
                message.addField(fieldStructure.getName(), messageComponent);
            }

            message = messageComponent;
        }

        return message;
    }

    public Message convert(IMessage message, boolean fixt) throws MessageConvertException {
        return convert(message, fixt, Message.class);
    }

	public Message convert(IMessage message, boolean fixt, Class<? extends Message> messageClass) throws MessageConvertException {
		if(message == null) {
		    return null;
		}

        try {
            Message resultMessage = createInstance(message.getName(), messageClass, message.getMetaData());

            traverseIMessage(resultMessage, message);

            if(fixt) {
                Header header = resultMessage.getHeader();
                IMessage iheader = message.getField(FixMessageHelper.HEADER);
                String messageType = iheader.getField("MsgType");

                header.setString(BeginString.FIELD, FixVersions.BEGINSTRING_FIXT11);
                header.setString(MsgType.FIELD, messageType);
            }

            return resultMessage;
        } catch(InstantiationException | IllegalAccessException e) {
            throw new MessageConvertException("Failed to create result message", e);
        }
    }

    protected Message createInstance(String messageName, Class<? extends Message> messageClass, MsgMetaData metadata) throws InstantiationException, IllegalAccessException {

        int[] fieldOrder = orderingFields
                ? getFieldOrderPrimitive(dictionary.getMessages().get(messageName))
                : null;

        return (messageClass == Message.class)
                ? new SailfishQuickfixMessage(fieldOrder, fieldOrderHeader, fieldOrderTrailer, metadata)
                : messageClass.newInstance();
    }

    protected void traverseIMessage(FieldMap resultMessage, IMessage message) throws MessageConvertException {
        IMessageStructure messageStructure = dictionary.getMessages().get(message.getName());
        if (messageStructure == null) {
            throw new MessageConvertException("Message " + message.getName() + " doesn't exist in the dictionary " + dictionary.getNamespace());
        }

        Set<Integer> definedTags = getDefinedTags(message, messageStructure);

        for(IFieldStructure fieldStructure : messageStructure.getFields().values()) {
			String fieldName = fieldStructure.getName();
			Object fieldValue = message.getField(fieldName);

            if(fieldValue == null) {
                continue;
            }

            if(fieldName.equals(FixMessageHelper.HEADER)) {
                FieldMap header = ((Message)resultMessage).getHeader();
                IMessage iHeader = message.getField(fieldName);

                traverseIMessage(header, iHeader);

                continue;
            }

            if(fieldName.equals(FixMessageHelper.TRAILER)) {
                FieldMap trailer = ((Message)resultMessage).getTrailer();
                IMessage iTrailer = message.getField(fieldName);

                traverseIMessage(trailer, iTrailer);

                continue;
            }

            Integer fieldTag = getAttributeValue(fieldStructure, ATTRIBUTE_TAG);

			if(fieldStructure.isComplex()) {
                String entityType = getAttributeValue(fieldStructure, ATTRIBUTE_ENTITY_TYPE);

				switch(entityType) {
				case "Group":
				    traverseGroups(resultMessage, message.getField(fieldName), fieldStructure, fieldTag);
                    continue;
				case "Component":
                    Object messageComponent = message.getField(fieldName);

                    if(messageComponent instanceof Iterable<?>) {
                        for(Object element : (Iterable<?>)messageComponent) {
                            traverseIMessage(resultMessage, (IMessage)element);
                        }
                    } else if(messageComponent instanceof IMessage) {
                        traverseIMessage(resultMessage, (IMessage)messageComponent);
                    }

                    continue;
				default:
                    throw new MessageConvertException("Field '" + fieldName + "' in  message '" + message.getName() + "' has unknown entity type: " + entityType);
				}
			}

			JavaType fieldType = fieldStructure.getJavaType();

			try {
                switch(fieldType) {
                case JAVA_LANG_BOOLEAN:
                    resultMessage.setBoolean(fieldTag, (Boolean)fieldValue);
                    continue;
                case JAVA_LANG_CHARACTER:
                    resultMessage.setString(fieldTag, fieldValue.toString());
                    continue;
                case JAVA_LANG_DOUBLE:
                    if(fieldValue instanceof BigDecimal) {
                        resultMessage.setDecimal(fieldTag, (BigDecimal)fieldValue);
                    } else {
                        resultMessage.setDouble(fieldTag, (Double)fieldValue);
                    }

                    continue;
                case JAVA_LANG_INTEGER:
                    resultMessage.setInt(fieldTag, (Integer)fieldValue);
                    continue;
                case JAVA_LANG_STRING:
                    if(fieldValue instanceof Character) {
                        resultMessage.setChar(fieldTag, (Character)fieldValue);
                    } else {
                        resultMessage.setString(fieldTag, (String)fieldValue);
                    }
                    //In FIX if a field has type DATA it also has a paired field that has a tag value equals `dataTag - 1`.
                    // The value for that field should be set to the content length in the data field
                    String fixtype = getAttributeValue(fieldStructure, ATTRIBUTE_FIX_TYPE);
                    if (FieldType.Data.getName().equals(fixtype)) {
                        Integer lengthTag = EXCEPTIONAL_DATA_LENGTH_TAGS.get(fieldTag);
                        if (lengthTag == null) {
                            lengthTag = fieldTag - 1;
                        }
                        if (!definedTags.contains(lengthTag)) {
                            resultMessage.setInt(lengthTag, ((String)fieldValue).length());
                        }
                    }
                    continue;
                case JAVA_MATH_BIG_DECIMAL:
                    if(fieldValue instanceof BigDecimal) {
                        resultMessage.setDecimal(fieldTag, (BigDecimal)fieldValue);
                    } else {
                        resultMessage.setDouble(fieldTag, (Double)fieldValue);
                    }

                    continue;
                case JAVA_TIME_LOCAL_DATE_TIME:
                    boolean includeMillis = includeMilliseconds;
                    boolean includeMicros = includeMicroseconds;
                    boolean includeNanos = includeNanoseconds;

                    Object fixType = getAttributeValue(fieldStructure, ATTRIBUTE_FIX_TYPE);
                    if (FieldType.UtcTimeStampSecondPresicion.getName().equals(fixType)) {
                        includeMillis = includeMicros = includeNanos = false;
                    }

                    Timestamp dateTime = DateTimeUtility.toTimestamp((LocalDateTime)fieldValue);
                    resultMessage.setUtcTimeStamp(fieldTag, dateTime, includeMillis, includeMicros, includeNanos);
                    continue;
                case JAVA_TIME_LOCAL_DATE:
                    Timestamp date = DateTimeUtility.toTimestamp((LocalDate)fieldValue);
                    resultMessage.setUtcDateOnly(fieldTag, date);
                    break;
                case JAVA_TIME_LOCAL_TIME:
                    Timestamp time = DateTimeUtility.toTimestamp((LocalTime)fieldValue);
                    resultMessage.setUtcTimeOnly(fieldTag, time, includeMilliseconds, includeMicroseconds, includeNanoseconds);
                    break;
                default:
                    throw new MessageConvertException("Unknown field type: " + fieldType);
			    }
            } catch(ClassCastException e) {
                throw new MessageConvertException("Failed to convert field '" + fieldName + "' in the message: " + messageStructure.getName(), e);
            }
		}
	}

    @NotNull
    private Set<Integer> getDefinedTags(IMessage message, IMessageStructure messageStructure) {
        Set<Integer> tagsSet = new TreeSet<>();
        for (IFieldStructure fieldStructure : messageStructure.getFields().values()) {
            String fieldName = fieldStructure.getName();
            Object fieldValue = message.getField(fieldName);

            if (fieldValue == null) {
                continue;
            }
            Integer tag = getAttributeValue(fieldStructure, ATTRIBUTE_TAG);
            if(tag != null) {
                tagsSet.add(tag);
            }
        }
        return tagsSet;
    }

    private void traverseGroups(FieldMap resultMessage, List<?> groups, IFieldStructure fieldStructure, Integer groupTag) throws MessageConvertException {
        if(groupTag == null) {
            throw new MessageConvertException("Group tag is missing for the field: " + fieldStructure.getName());
        }

        Integer delimiterTag = getGroupDelimiter(fieldStructure);

        if(delimiterTag == null) {
            throw new MessageConvertException("Group delimiter is missing for the field: " + fieldStructure.getName());
        }

        int[] fieldOrder = getFieldOrderPrimitive(fieldStructure);

        for(Object o : groups) {
            IMessage iGroup = (IMessage)o;
            Group group = new Group(groupTag, delimiterTag, fieldOrder);

            traverseIMessage(group, iGroup);
            resultMessage.addGroup(group);
        }
	}

	private Integer getGroupDelimiter(IFieldStructure fieldStructure) {
        if(fieldStructure.getFields().isEmpty()) {
            return null;
        }

        IFieldStructure firstFieldStructure = fieldStructure.getFields().values().iterator().next();

        if(firstFieldStructure.isCollection()) {
            return getAttributeValue(firstFieldStructure, ATTRIBUTE_TAG);
        }

        if(firstFieldStructure.isComplex()) {
            return getGroupDelimiter(firstFieldStructure);
        }

        return getAttributeValue(firstFieldStructure, ATTRIBUTE_TAG);
	}

	private List<Integer> getFieldOrder(IFieldStructure fieldStructure) {
        List<Integer> fieldOrder = new ArrayList<>();

        for(IFieldStructure field : fieldStructure.getFields().values()) {
            if(field.isComplex() && !field.isCollection()) {
                fieldOrder.addAll(getFieldOrder(field));
            } else {
                Integer fieldTag = getAttributeValue(field, ATTRIBUTE_TAG);

                if(fieldTag == null) {
                    logger.error("Field {} in dictionary {} does not contain {}", field.getName(), field.getNamespace(), ATTRIBUTE_TAG);
                }

                fieldOrder.add(fieldTag);
            }
        }

        return fieldOrder;
	}

	private int[] getFieldOrderPrimitive(IFieldStructure fieldStructure) {
	    return ArrayUtils.toPrimitive(getFieldOrder(fieldStructure).toArray(ArrayUtils.EMPTY_INTEGER_OBJECT_ARRAY));
	}


    public IDictionaryStructure getDictionary() {
        return dictionary;
    }


}
