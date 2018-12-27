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

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MessageUtil;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.services.fix.converter.MessageConvertException;
import com.exactpro.sf.util.DateTimeUtility;

import quickfix.DataDictionary;
import quickfix.DataDictionaryProvider;
import quickfix.DefaultDataDictionaryProvider;
import quickfix.Field;
import quickfix.FieldConvertError;
import quickfix.FieldException;
import quickfix.FieldMap;
import quickfix.FieldNotFound;
import quickfix.FixVersions;
import quickfix.Group;
import quickfix.InvalidMessage;
import quickfix.Message;
import quickfix.field.BeginString;
import quickfix.field.converter.BooleanConverter;
import quickfix.field.converter.UtcDateOnlyConverter;
import quickfix.field.converter.UtcTimeOnlyConverter;
import quickfix.field.converter.UtcTimestampConverter;

/**
 * Collection of utilities for manipulation with FIX messages.
 *
 * @author dmitry.guriev
 *
 */
public class FixUtil {


	private static DefaultDataDictionaryProvider provider = new DefaultDataDictionaryProvider();

	private FixUtil()
	{
		// hide constructor
	}

	/**
	 * Return quantity on tags in a field map and its children groups.
	 * @param fieldMap instance if FieldMap
	 * @return number of tags in a message
	 */
	public static int getFieldsCount(FieldMap fieldMap)
	{
		int i=0;
		Iterator<Field<?>> iterator = fieldMap.iterator();
		while (iterator.hasNext()) {
			iterator.next();
			i++;
		}

		Iterator<Integer> groupKeyIterator = fieldMap.groupKeyIterator();
		while (groupKeyIterator.hasNext())
		{
			int tag = groupKeyIterator.next();
			List<Group> groups = fieldMap.getGroups(tag);
			for (Group group : groups) {
				i += getFieldsCount(group);
			}
		}

		return i;
	}

	public static String getBeginString(Message fieldMap) throws FieldNotFound
	{
		BeginString beginString = new BeginString();
		fieldMap.getHeader().getField(beginString);
		return beginString.getObject();
	}

    public static String toString (Message fieldMap, DataDictionaryProvider dictionaryProvider) throws FieldNotFound
    {
        DataDictionary dictionary = dictionaryProvider.getSessionDataDictionary(getBeginString(fieldMap));

        return toString(fieldMap, dictionary);
    }

	public static String toString (Message fieldMap, DataDictionary dictionary) throws FieldNotFound
	{
		Message clone = (Message) fieldMap.clone();

		StringBuilder builder = new StringBuilder();

		builder.append(clone.getClass().getSimpleName());
		builder.append("=[");
		builder.append(toString((FieldMap) clone.getHeader(), dictionary));
		builder.append("] [");
		builder.append(toString((FieldMap) clone, dictionary));
		builder.append("] [");
		builder.append(toString((FieldMap) clone.getTrailer(), dictionary));
		builder.append("]");

		return MessageUtil.escapeCharacter(builder);
	}

	public static String toShortString (Message fieldMap) throws FieldNotFound
	{
		Message clone = (Message) fieldMap.clone();
		StringBuilder buffer = new StringBuilder();

		DataDictionary dictionary = getDictionary(clone);

		buffer.append(clone.getClass().getSimpleName());
		buffer.append("=[");
		buffer.append(toString((FieldMap) clone, dictionary));
		buffer.append("]");

		return MessageUtil.escapeCharacter(buffer);
	}

	public static DataDictionary getDictionary (Message message) throws FieldNotFound
	{
		String beginString = getBeginString(message);
		return getDictionary(beginString, message.isApp());
	}

	public static DataDictionary getDictionary (String beginString, boolean isApp) throws FieldNotFound
	{
		if (FixVersions.BEGINSTRING_FIXT11.equals(beginString) && isApp) {
			return provider.getSessionDataDictionary(FixVersions.FIX50);
		}
		return provider.getSessionDataDictionary(beginString);
	}

	private static String toString (FieldMap fieldMap, DataDictionary dictionary)
	{
		StringBuilder buffer = new StringBuilder();

		Iterator<Field<?>> iter = fieldMap.iterator();
		while (iter.hasNext()) {
			Field<?> field = (Field<?>) iter.next();
			int tag = field.getField();
			if (!isGroupField(fieldMap, tag)) {

				String name = dictionary.getFieldName(tag);
				if (name != null) {
					buffer.append(name);
					buffer.append("=");
				}
				buffer.append(tag);
				buffer.append("=");
				buffer.append(field.getObject());
				if (iter.hasNext()) {
					buffer.append(", ");
				}
			} else if (isGroupField(fieldMap, tag) && fieldMap.getGroupCount(tag) > 0) {
				List<Group> groups = fieldMap.getGroups(tag);
				String name = dictionary.getFieldName(tag);
				if (name != null) {
					buffer.append(name);
					buffer.append("=");
				}
				buffer.append(tag);
				buffer.append("=");
				buffer.append(field.getObject());
				buffer.append(", ");

				if (name != null) {
					buffer.append(name);
					buffer.append("=");
				}
				buffer.append(tag);
				buffer.append("={ ");
				for (int i = 0; i < groups.size(); i++) {
					FieldMap groupFields = (FieldMap) groups.get(i);
					buffer.append(i);
					buffer.append("={");
					buffer.append(toString(groupFields, dictionary));
					buffer.append("}");
					if (i < groups.size()-1) {
						buffer.append(", ");
					}
				}
				buffer.append(" }");
			}
		}

		return buffer.toString();
	}

	public static Message fromString(String message) throws FieldNotFound, InvalidMessage {

		int index1 = message.indexOf("8=");
		int index2 = message.indexOf("\001");
		String beginString = message.substring(index1+2, index2);
		index1 = message.indexOf("35=");
		index2 = message.indexOf("\001", index1);
		String msgType = message.substring(index1+3, index2);
		boolean isApp = isApp(msgType);
		DataDictionary dd = FixUtil.getDictionary(beginString, isApp);
		Message msg = new Message();
		msg.fromString(message, dd, true);
		return msg;
	}

	public static boolean isGroupField(FieldMap fieldMap, int tag) {
		Iterator<Integer> iter = fieldMap.groupKeyIterator();
		while (iter.hasNext())
		{
			if (iter.next() == tag) {
				return true;
			}
		}
		return false;
	}

	private static long nextID = 1;
	public static synchronized String generateClorID() {
		return String.valueOf(System.currentTimeMillis()+(nextID++));
	}

	public static synchronized String generateClorIDSpecLng(int length) {
		return String.valueOf(RandomStringUtils.random(length, "0123456789"));
	}

	public static String asString(Object obj) throws IllegalArgumentException, IllegalAccessException
	{
		Class<?> cls = obj.getClass();
		for (java.lang.reflect.Field constant : obj.getClass().getDeclaredFields())
		{
			int modifyers = constant.getModifiers();
			if (((modifyers & Modifier.FINAL) == Modifier.FINAL)
					&& ((modifyers & Modifier.STATIC) == Modifier.STATIC)
					&& ((modifyers & Modifier.PUBLIC) == Modifier.PUBLIC))
			{
				if (quickfix.IntField.class.isAssignableFrom(cls)
						|| quickfix.StringField.class.isAssignableFrom(cls)
						|| quickfix.CharField.class.isAssignableFrom(cls)
						|| quickfix.DoubleField.class.isAssignableFrom(cls)
						|| quickfix.BooleanField.class.isAssignableFrom(cls)) {
					if (((quickfix.Field<?>)obj).getObject().equals(constant.get(obj))) {
						return constant.getName();
					}
				}
			}
		}

		if (quickfix.Field.class.isAssignableFrom(cls))
			return String.valueOf(((quickfix.Field<?>)obj).getObject());
		return obj.toString();
	}

	public static boolean isAdmin(String msgType) {
		if (msgType != null && false == msgType.equals("")) {
			return msgType.length() == 1 && "0A12345".indexOf(msgType.charAt(0)) != -1;
		}
		return false;
	}

	public static boolean isApp(String msgType) {
		return !isAdmin(msgType);
	}

    public static void parseXmlFields(String content, IMessage xmlMessage, IDictionaryStructure dictionary,
                                      IFieldStructure messageStructure, DocumentBuilderFactory documentFactory,
                                      boolean parseFieldsByDictionary)
            throws IOException, SAXException, ParserConfigurationException, MessageConvertException {

        DocumentBuilder docBuilder = documentFactory.newDocumentBuilder();
        Document document = docBuilder.parse(new InputSource(new StringReader(content)));

        NodeList list = document.getChildNodes();

        for (int i = 0; i < list.getLength(); i++) {
            NodeList insideList = list.item(i).getChildNodes();

            for (int j = 0; j < insideList.getLength(); j++) {
                if (!insideList.item(j).hasAttributes()) {
                    continue;
                }

                Node nameNode = insideList.item(j).getAttributes().getNamedItem("name");
                Node valueNode = insideList.item(j).getAttributes().getNamedItem("value");

                String fieldName = nameNode.getNodeValue().replaceAll(" ", "");
                String fieldValue = valueNode.getNodeValue().trim();

                if (StringUtils.isNotEmpty(fieldName) && StringUtils.isNotEmpty(fieldValue)) {
                    if (parseFieldsByDictionary) {

                        IFieldStructure fieldStructure = messageStructure.getField(fieldName);
                        if (fieldStructure == null) {
                            fieldStructure = dictionary.getFieldStructure(fieldName);
                        }

                        if (fieldStructure != null && !fieldStructure.isComplex()) {
                            try {
                                switch (fieldStructure.getJavaType()) {
                                    case JAVA_LANG_BOOLEAN:
                                        xmlMessage.addField(fieldName, BooleanConverter.convert(fieldValue));
                                        break;
                                    case JAVA_LANG_CHARACTER:
                                        xmlMessage.addField(fieldName, fieldValue.charAt(0));
                                        break;
                                    case JAVA_LANG_BYTE:
                                        xmlMessage.addField(fieldName, Byte.valueOf(fieldValue));
                                        break;
                                    case JAVA_LANG_SHORT:
                                        xmlMessage.addField(fieldName, Short.valueOf(fieldValue));
                                        break;
                                    case JAVA_LANG_INTEGER:
                                        xmlMessage.addField(fieldName, Integer.valueOf(fieldValue));
                                        break;
                                    case JAVA_LANG_LONG:
                                        xmlMessage.addField(fieldName, Long.valueOf(fieldValue));
                                        break;
                                    case JAVA_LANG_FLOAT:
                                        xmlMessage.addField(fieldName, Float.valueOf(fieldValue));
                                        break;
                                    case JAVA_LANG_DOUBLE:
                                        xmlMessage.addField(fieldName, Double.valueOf(fieldValue));
                                        break;
                                    case JAVA_MATH_BIG_DECIMAL:
                                        xmlMessage.addField(fieldName, new BigDecimal(fieldValue));
                                        break;
                                    case JAVA_LANG_STRING:
                                        xmlMessage.addField(fieldName, fieldValue);
                                        break;
                                    case JAVA_TIME_LOCAL_DATE_TIME:
                                        LocalDateTime dateTime = DateTimeUtility.toLocalDateTime(
                                                UtcTimestampConverter.convert(fieldValue));
                                        xmlMessage.addField(fieldName, dateTime);
                                        break;
                                    case JAVA_TIME_LOCAL_TIME:
                                        LocalTime time = DateTimeUtility.toLocalTime(
                                                UtcTimeOnlyConverter.convert(fieldValue));
                                        xmlMessage.addField(fieldName, time);
                                        break;
                                    case JAVA_TIME_LOCAL_DATE:
                                        LocalDate date = DateTimeUtility.toLocalDate(
                                                UtcDateOnlyConverter.convert(fieldValue));
                                        xmlMessage.addField(fieldName, date);
                                        break;
                                    default:
                                        xmlMessage.addField(fieldName, fieldValue);
                                        break;
                                }
                            } catch (FieldException | FieldConvertError e) {
                                throw new MessageConvertException(
                                        String.format("Can not parse [%s] value for [%s] field", fieldName, fieldValue), "", e);
                            }
                        }
                    } else {
                        xmlMessage.addField(fieldName, fieldValue);
                    }
                }
            }
        }
    }


    /**
     * @param fieldName TODO
     * @param obj
     * @param seqNum
     * @return
     */
    public static Number convertToNumber(String fieldName, Object obj) {
        if (obj instanceof Number) {
            return (Number)obj;
        } else {
            try {
                return Long.parseLong(String.valueOf(obj));
            } catch (NumberFormatException e) {
                throw new EPSCommonException(FixMessageHelper.MSG_SEQ_NUM_FIELD + " is not a number", e);
            }
        }
    }
}
