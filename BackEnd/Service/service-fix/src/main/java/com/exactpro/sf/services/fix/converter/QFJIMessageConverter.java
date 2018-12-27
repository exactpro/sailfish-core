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
package com.exactpro.sf.services.fix.converter;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.services.fix.FixMessageHelper;
import com.exactpro.sf.services.fix.QFJDictionaryAdapter;
import com.exactpro.sf.util.DateTimeUtility;
import com.google.common.collect.HashBasedTable;
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
	private final boolean skipTags;
	protected final boolean orderingFields;

	private final Map<String, IMessageStructure> typeToStructure = new HashMap<>();
	private final Table<Integer, String, List<IFieldStructure>> tagToPath = HashBasedTable.create();
	private final Table<Integer, String, IFieldStructure> tagToStructure = HashBasedTable.create();

	private final List<Integer> tagsToSkip = Arrays.asList(MsgSeqNum.FIELD, BodyLength.FIELD, CheckSum.FIELD);

	public QFJIMessageConverter(IDictionaryStructure dictionary, IMessageFactory factory, boolean verifyTags, boolean includeMilliseconds, boolean skipTags) {
	    this(dictionary, factory, verifyTags, includeMilliseconds, false, skipTags);
	}

	public QFJIMessageConverter(IDictionaryStructure dictionary, IMessageFactory factory,
            boolean verifyTags, boolean includeMilliseconds, boolean includeMicroseconds, boolean skipTags) {
	    this(dictionary, factory, verifyTags, includeMilliseconds, includeMicroseconds, skipTags, false);
	}
	
    public QFJIMessageConverter(IDictionaryStructure dictionary, IMessageFactory factory,
                                boolean verifyTags, boolean includeMilliseconds, boolean includeMicroseconds, boolean skipTags, boolean orderingFields) {
        this.dictionary = dictionary;
        this.factory = factory;
        this.verifyTags = verifyTags;
        this.includeMilliseconds = includeMilliseconds;
        this.includeMicroseconds = includeMicroseconds;
        this.skipTags = skipTags;
        this.orderingFields = orderingFields;
        
        if (this.orderingFields) {
            this.fieldOrderHeader = getFieldOrderPrimitive(this.dictionary.getMessageStructure(FixMessageHelper.HEADER));
            this.fieldOrderTrailer = getFieldOrderPrimitive(this.dictionary.getMessageStructure(FixMessageHelper.TRAILER));
        } else {
            this.fieldOrderHeader = null;
            this.fieldOrderTrailer = null;
        }
        
        indexMessages();
    }

	private void indexMessages() {
        for(IMessageStructure messageStructure : dictionary.getMessageStructures()) {
            String messageType = (String)messageStructure.getAttributeValueByName(ATTRIBUTE_MESSAGE_TYPE);

            if(messageType != null) {
                typeToStructure.put(messageType, messageStructure);
            }

            indexMessageFields(messageStructure, new ArrayList<IFieldStructure>(), messageStructure);
        }
	}

	private void indexMessageFields(IFieldStructure messageStructure, ArrayList<IFieldStructure> fieldPath, IMessageStructure rootMessage) {
        String messageName = rootMessage.getName();

        for(IFieldStructure fieldStructure : messageStructure.getFields()) {
            Integer fieldTag = (Integer)fieldStructure.getAttributeValueByName(ATTRIBUTE_TAG);

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
	public IMessage convert(Message message) throws MessageConvertException {
		return convert(message, null, null, false);
	}

    public IMessage convert(final Message message, final Boolean verifyTagsOverride, final Boolean skipTagsOverride) throws MessageConvertException {
        return convert(message, verifyTagsOverride, skipTagsOverride, false);
    }

    public IMessage convert(final Message message, final Boolean verifyTagsOverride, final Boolean skipTagsOverride,
                            final boolean ignoreFieldType) throws MessageConvertException {
		if(message == null) {
		    return null;
		}

		String messageType = null;

		try {
			messageType = message.getHeader().getString(MsgType.FIELD);
		} catch (FieldNotFound e) {
			throw new MessageConvertException(message, "Failed to get message type", e);
		}

		IMessageStructure messageStructure = typeToStructure.get(messageType);

		if(messageStructure == null) {
			throw new MessageConvertException(message, "Unknown message type: " + messageType);
		}

		IMessage resultMessage = factory.createMessage(messageStructure.getName(), messageStructure.getNamespace());
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
		resultMessage.getMetaData().setRawMessage(message.toString().getBytes());

		return resultMessage;
	}

    protected void traverseMessage(final IMessage resultMessage, final FieldMap message, final IMessageFactory factory,
                                   final IMessage rootMessage, final boolean verifyTags,
                                   final boolean skipTags, final boolean ignoreFieldType) throws MessageConvertException {
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
                    throw new MessageConvertException(message, "Unknown tag: " + fieldTag);
                }

                continue;
            }

			IFieldStructure fieldStructure = tagToStructure.get(fieldTag, messageName);

            if(fieldStructure == null) {
                if(verifyTags) {
                    throw new MessageConvertException(message, String.format("Message '%s' doesn't contain tag: %s", messageName, fieldTag));
                }

                continue;
            }

			List<IFieldStructure> fieldPath = tagToPath.get(fieldTag, messageName);

			if(fieldPath == null) {
			    if(verifyTags) {
                    throw new MessageConvertException(message, String.format("No field path in message '%s' for tag: %s", messageName, fieldTag));
                }

			    continue;
			}

			String fieldName = fieldStructure.getName();
			IMessage messageComponent = getMessageComponent(fieldPath, resultMessage);

			if (fieldStructure.isComplex() && fieldStructure.isCollection()) {
				List<IMessage> iGroups = new ArrayList<>();

                for(Group group : message.getGroups(fieldTag)) {
                    IMessage iGroup = factory.createMessage(fieldStructure.getReferenceName(), fieldStructure.getNamespace());
                    iGroups.add(iGroup);
                    traverseMessage(iGroup, group, factory, iGroup, verifyTags, skipTags, ignoreFieldType);
                }

				messageComponent.addField(fieldName, iGroups);
			} else {
				try {
					if (message.getString(fieldTag).isEmpty()) {
						// it from SETTING_VALIDATE_FIELDS_HAVE_VALUES
						// don't add it. It can break some traverse of this message
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
                    throw new MessageConvertException(message, e.getMessage(), e);
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
            Message resultMessage = createInstance(message.getName(), messageClass);

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

    protected Message createInstance(String messageName, Class<? extends Message> messageClass) throws InstantiationException, IllegalAccessException {
        if (this.orderingFields && Message.class == messageClass) {
            IMessageStructure messageStructure = dictionary.getMessageStructure(messageName);
            int[] fieldOrder = getFieldOrderPrimitive(messageStructure);
            return new SailfishQuickfixMessage(fieldOrder, this.fieldOrderHeader, this.fieldOrderTrailer);
        }
        return messageClass.newInstance();
    }
	
    protected void traverseIMessage(final FieldMap resultMessage, final IMessage message) throws MessageConvertException {
		IMessageStructure messageStructure = dictionary.getMessageStructure(message.getName());

        for(IFieldStructure fieldStructure : messageStructure.getFields()) {
			String fieldName = fieldStructure.getName();
			Object fieldValue = message.getField(fieldName);

            if(fieldValue == null) {
                continue;
            }

            if(fieldName.equals(FixMessageHelper.HEADER)) {
                FieldMap header = ((quickfix.Message)resultMessage).getHeader();
                IMessage iHeader = message.getField(fieldName);

                traverseIMessage(header, iHeader);

                continue;
            }

            if(fieldName.equals(FixMessageHelper.TRAILER)) {
                FieldMap trailer = ((quickfix.Message)resultMessage).getTrailer();
                IMessage iTrailer = message.getField(fieldName);

                traverseIMessage(trailer, iTrailer);

                continue;
            }

			Integer fieldTag = (Integer)fieldStructure.getAttributeValueByName(ATTRIBUTE_TAG);

			if(fieldStructure.isComplex()) {
				String entityType = (String)fieldStructure.getAttributeValueByName(ATTRIBUTE_ENTITY_TYPE);

				switch(entityType) {
				case "Group":
				    traverseGroups(resultMessage, (List<?>)message.getField(fieldName), fieldStructure, fieldTag);
                    continue;
				case "Component":
                    Object messageComponent = message.getField(fieldName);

                    if(messageComponent instanceof List<?>) {
                        for(Object element : (List<?>)messageComponent) {
                            traverseIMessage(resultMessage, (IMessage)element);
                        }
                    } else if(messageComponent instanceof IMessage) {
                        traverseIMessage(resultMessage, (IMessage)messageComponent);
                    }

                    continue;
				default:
				    throw new MessageConvertException(message,
                            String.format("Field '%s' has unknown entity type: %s", fieldName, entityType));
				}
			}

			JavaType fieldType = fieldStructure.getJavaType();

			try {
                switch(fieldType) {
                case JAVA_LANG_BOOLEAN:
                    resultMessage.setBoolean(fieldTag, (Boolean)fieldValue);
                    continue;
                case JAVA_LANG_CHARACTER:
                    resultMessage.setString(fieldTag, (fieldValue.toString()));
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

                    Object fixType = fieldStructure.getAttributeValueByName(QFJDictionaryAdapter.ATTRIBUTE_FIX_TYPE);
                    if (FieldType.UtcTimeStampSecondPresicion.getName().equals(fixType)) {
                        includeMillis = includeMicros = false;
                    }

                    Timestamp dateTime = DateTimeUtility.toTimestamp((LocalDateTime)fieldValue);
                    resultMessage.setUtcTimeStamp(fieldTag, dateTime, includeMillis, includeMicros);
                    continue;
                case JAVA_TIME_LOCAL_DATE:
                    Timestamp date = DateTimeUtility.toTimestamp((LocalDate)fieldValue);
                    resultMessage.setUtcDateOnly(fieldTag, date);
                    break;
                case JAVA_TIME_LOCAL_TIME:
                    Timestamp time = DateTimeUtility.toTimestamp((LocalTime)fieldValue);
                    resultMessage.setUtcTimeOnly(fieldTag, time, includeMilliseconds, includeMicroseconds);
                    break;
                default:
                    throw new MessageConvertException("Unknown field type: " + fieldType);
			    }
            } catch(ClassCastException e) {
                throw new MessageConvertException(message, String.format("Failed to convert field '%s' in message: %s", fieldName, messageStructure.getName()), e);
            }
		}
	}

    private void traverseGroups(FieldMap resultMessage, List<?> groups, IFieldStructure fieldStructure, Integer groupTag) throws MessageConvertException {
        if(groupTag == null) {
            throw new MessageConvertException(groups, "Group tag is missing for field: " + fieldStructure.getName());
        }

        Integer delimiterTag = getGroupDelimiter(fieldStructure);

        if(delimiterTag == null) {
            throw new MessageConvertException(groups, "Group delimiter is missing for field: " + fieldStructure.getName());
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

        IFieldStructure firstFieldStructure = fieldStructure.getFields().get(0);

        if(firstFieldStructure.isCollection()) {
            return (Integer)firstFieldStructure.getAttributeValueByName(ATTRIBUTE_TAG);
        }

        if(firstFieldStructure.isComplex()) {
            return getGroupDelimiter(firstFieldStructure);
        }

        return (Integer)firstFieldStructure.getAttributeValueByName(ATTRIBUTE_TAG);
	}

	private List<Integer> getFieldOrder(IFieldStructure fieldStructure) {
        List<Integer> fieldOrder = new ArrayList<>();

        for(IFieldStructure field : fieldStructure.getFields()) {
            if(field.isComplex() && !field.isCollection()) {
                fieldOrder.addAll(getFieldOrder(field));
            } else {
                Integer fieldTag = (Integer)field.getAttributeValueByName(ATTRIBUTE_TAG);

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
