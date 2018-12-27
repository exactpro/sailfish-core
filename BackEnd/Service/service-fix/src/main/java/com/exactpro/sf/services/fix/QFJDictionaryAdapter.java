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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.FieldNotFoundException;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.EPSCommonException;

import quickfix.DataDictionary;
import quickfix.Field;
import quickfix.FieldConvertError;
import quickfix.FieldException;
import quickfix.FieldMap;
import quickfix.FieldNotFound;
import quickfix.FieldType;
import quickfix.FixVersions;
import quickfix.Group;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.StringField;
import quickfix.field.MsgType;
import quickfix.field.SessionRejectReason;
import quickfix.field.converter.BooleanConverter;
import quickfix.field.converter.CharConverter;
import quickfix.field.converter.DoubleConverter;
import quickfix.field.converter.IntConverter;
import quickfix.field.converter.UtcDateOnlyConverter;
import quickfix.field.converter.UtcTimeOnlyConverter;
import quickfix.field.converter.UtcTimestampConverter;

public class QFJDictionaryAdapter extends DataDictionary {

	private static final Logger logger = LoggerFactory.getLogger(QFJDictionaryAdapter.class);

	public static final String ATTRIBUTE_FIX_TYPE = "fixtype";

    private static final String MESSAGE_CATEGORY_ADMIN = "admin".intern();
	private boolean checkFieldsOutOfOrder = true;
	private boolean checkFieldsHaveValues = true;
	private boolean checkUserDefinedFields = true;
	private boolean allowUnknownMessageFields = false;
    private boolean checkUnorderedGroupFields = true;
	private IDictionaryStructure iMsgDict;
	private String version;
	private String namespace;

	private HashMap<Integer, IFieldStructure> fields = new HashMap<>();

	private HashMap<String, IMessageStructure> messages = new HashMap<>();
	private HashMap<String, HashMap<Integer, IFieldStructure>> mesgFields = new HashMap<>();
	private final Map<String, Set<Integer>> requiredFields = new HashMap<>();
	private final Map<Integer, Set<String>> fieldValues = new HashMap<>();
    private final Map<String, String> messageCategory = new HashMap<>();
    private final Map<String, Integer> names = new HashMap<>();

	private final Map<String, Set<Integer>> messageFields = new HashMap<>();
	private final Map<IntStringPair, GroupInfo> groups = new HashMap<>();

	public QFJDictionaryAdapter(IDictionaryStructure iMsgDict) {
		this.iMsgDict = iMsgDict;
		this.version = getVersionFromDictName();
		loadMsgTypes();
	}

	private void loadMsgTypes() {
		IMessageStructure header = iMsgDict.getMessageStructure("header");
		if (header != null) {
			indexFields(HEADER_ID, header.getFields(), false);
		}
		IMessageStructure trailer = iMsgDict.getMessageStructure("trailer");
		if (trailer != null) {
			indexFields(TRAILER_ID, trailer.getFields(), false);
		}
		for (IMessageStructure struct : iMsgDict.getMessageStructures()) {
			String messageType = (String) struct.getAttributeValueByName("MessageType");
			if (messageType == null) {
				continue;
			}
			messages.put(messageType, struct);
			indexFields(messageType, struct.getFields(), false);
		}
	}

    private void addFieldName(int field, String name) /*throws ConfigError*/ {
        if (names.put(name, field) != null) {
            //throw new ConfigError("Field named " + name + " defined multiple times");
        }
    }

	private void indexFields(String messageType, List<IFieldStructure> fieldStructures, boolean onlyFields) {
		HashMap<Integer, IFieldStructure> msgFields = mesgFields.get(messageType);
		if (msgFields == null && !onlyFields) {
			msgFields = new HashMap<>();
			mesgFields.put(messageType, msgFields);
		}
		Set<Integer> rqFlds = requiredFields.get(messageType);
		if (rqFlds == null && !onlyFields) {
			rqFlds = new HashSet<>();
			requiredFields.put(messageType, rqFlds);
		}

		for (IFieldStructure fs : fieldStructures) {
			Integer tag = (Integer) fs.getAttributeValueByName("tag");
			if (tag == null) {
				if (fs.isComplex()) {
					indexFields(messageType, fs.getFields(), onlyFields || fs.isCollection());
				}
				continue;
			}
			if (fs.isComplex() && fs.isCollection()) {
				indexFields(null, fs.getFields(), true);
			}
			if (!onlyFields) {
				msgFields.put(tag, fs);
			}
			fields.put(tag, fs);
			addFieldName(tag.intValue(), fs.getName());
			if (fs.isRequired() && !onlyFields) {
				try {
					if (
							messageType == HEADER_ID ||
							messageType == TRAILER_ID ||
							(!isHeaderField(tag) && !isTrailerField(tag))
						) {
						rqFlds.add(tag);
					}
				} catch (NullPointerException e) {
					logger.debug("No header or trailer in a message");
					rqFlds.add(tag);
				}
			}

			if (fs.isEnum()) {
			    Set<String> fldValues = new HashSet<>();

			    Set<String> curretnValues = fieldValues.get(tag);
			    if (curretnValues != null) {
                    fldValues.addAll(curretnValues);
                }

			    for (String enumElement : fs.getValues().keySet()) {
			        if (JavaType.JAVA_LANG_BOOLEAN.equals(fs.getJavaType())) {
			            fldValues.add(Boolean.TRUE.equals(fs.getValues().get(enumElement).getCastValue()) ? "Y" : "N");
			        } else {
			            fldValues.add(fs.getValues().get(enumElement).getValue());
			        }
                }

                Object allowOtherValues = fs.getAttributeValueByName(ALLOW_OTHER_VALUES_ATTRIBUTE);
				if (allowOtherValues instanceof Boolean && (Boolean)allowOtherValues) {
				    fldValues.add(ANY_VALUE);
				}
				fieldValues.put(tag, fldValues);

				for (String element : fs.getValues().keySet()) {
					fldValues.add(fs.getValues().get(element).getValue());
				}
			}
		}
	}

	private String getVersionFromDictName() {
		namespace = iMsgDict.getNamespace();
		return getVersionFromNs(namespace);
	}

	private String getVersionFromNs(String ns) {
		Pattern p = Pattern.compile("(.*)(FIXT?_[0-9]+_[0-9]+)");
		Matcher m = p.matcher(ns);
		m.find();
		return m.group(2).replace('_', '.');
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
    public boolean isMsgType(String msgType) {
		return messages.containsKey(msgType);
	}

	@Override
	public GroupInfo getGroup(String msg, int field) {
		IFieldStructure fs = mesgFields.get(msg).get(field);
		Integer delim = getFirstFieldWithTag(fs);
		if (delim == null) {
			logger.warn("Can not get separator field for group {}", field);
			return null;
		}
		return new GroupInfo(delim, new GroupDictionary(msg, field, fs));
	}

	private Integer getFirstFieldWithTag(IFieldStructure subMsgType) {
		if (subMsgType.getFields().isEmpty()) {
			return null;
		}
		IFieldStructure firstFld = subMsgType.getFields().get(0);
		if (!firstFld.isCollection()) {
    		if (firstFld.isComplex()) {
    			return getFirstFieldWithTag(firstFld);
    		}
		}
		return (Integer) firstFld.getAttributeValueByName("tag");
	}

	@Override
	public void checkHasRequired(FieldMap header, FieldMap body, FieldMap trailer, String msgType,
			boolean bodyOnly) {

		if (!bodyOnly) {
			checkHasRequired(HEADER_ID, header, bodyOnly);
			checkHasRequired(TRAILER_ID, trailer, bodyOnly);
		}

		checkHasRequired(msgType, body, bodyOnly);
	}


	private void checkHasRequired(String msgType, FieldMap fields, boolean bodyOnly) {
		final Set<Integer> requiredFieldsForMessage = requiredFields.get(msgType);
		if (requiredFieldsForMessage == null || requiredFieldsForMessage.size() == 0) {
			return;
		}

		final Iterator<Integer> fieldItr = requiredFieldsForMessage.iterator();
		while (fieldItr.hasNext()) {
			final int field = (fieldItr.next()).intValue();
			if (!fields.isSetField(field)) {
				throw new FieldException(SessionRejectReason.REQUIRED_TAG_MISSING, field);
			}
		}

		final Map<Integer, List<Group>> groups = fields.getGroups();
		if (groups.size() > 0) {
			final Iterator<Map.Entry<Integer, List<Group>>> groupIter = groups.entrySet()
			.iterator();
			while (groupIter.hasNext()) {
				final Map.Entry<Integer, List<Group>> entry = groupIter.next();
				final GroupInfo p = getGroup(msgType, (entry.getKey()).intValue());
				final List<Group> groupInstances = entry.getValue();
				for (int i = 0; i < groupInstances.size(); i++) {
					final FieldMap groupFields = groupInstances.get(i);
					p.getDataDictionary().checkHasRequired(null, groupFields, null,
							msgType, bodyOnly);
				}
			}
		}
	}

	@Override
	public void iterate(FieldMap map, String msgType, DataDictionary dd)
	throws IncorrectTagValue, IncorrectDataFormat {
		final Iterator<Field<?>> iterator = map.iterator();
		while (iterator.hasNext()) {
			final StringField field = (StringField) iterator.next();

			checkHasValue(field);

			if (hasVersion()) {
				checkValidFormat(field);
				checkValue(field);
			}

			if (getVersion() != null && shouldCheckTag(field)) {
				//dd.checkValidTagNumber(field);
				if (map instanceof Message) {
					checkIsInMessage(field, msgType);
				}
				dd.checkGroupCount(field, map, msgType);
			}
		}

		for (final List<Group> groups : map.getGroups().values()) {
			for (final Group group : groups) {
				iterate(group, msgType, dd.getGroup(msgType, group.getFieldTag())
						.getDataDictionary());
			}
		}

	}

	// / If we need to check for the tag in the dictionary
	private boolean shouldCheckTag(Field<?> field) {
		if (!checkUserDefinedFields && field.getField() >= USER_DEFINED_TAG_MIN) {
			return false;
		} else {
			return true;
		}
	}

	private void checkValue(StringField field) throws IncorrectTagValue {
		final int tag = field.getField();
		if (!hasFieldValue(tag)) {
			return;
		}

		final String value = field.getValue();
		if (!isFieldValue(tag, value)) {
			throw new IncorrectTagValue(tag);
		}
	}

	// / Check if a field is in this message type.
	private void checkIsInMessage(Field<?> field, String msgType) {
		if (!isMsgField(msgType, field.getField()) && !allowUnknownMessageFields) {
			throw new FieldException(SessionRejectReason.TAG_NOT_DEFINED_FOR_THIS_MESSAGE_TYPE,
					field.getField());
		}
	}

	@Override
    public boolean isFieldValue(int field, String value) {
		final Set<String> validValues = fieldValues.get(field);

		if (validValues.contains(ANY_VALUE)) {
			return true;
		}

		if (validValues == null || validValues.size() == 0) {
			return false;
		}

		if (!isMultipleValueStringField(field)) {
			return validValues.contains(value);
		}

		// MultipleValueString
		final String[] values = value.split(" ");
		for (int i = 0; i < values.length; i++) {
			if (!validValues.contains(values[i])) {
				return false;
			}
		}

		return true;
	}

	private boolean isMultipleValueStringField(int field) {
		return getFieldTypeEnum(field) == FieldType.MultipleValueString;
	}

	private void checkValidFormat(StringField field) throws IncorrectDataFormat {
		if (!checkFieldsHaveValues && (field.getValue() == null || field.getValue().isEmpty())) {
			return;
		}

		try {
			final FieldType fieldType = getFieldTypeEnum(field.getTag());
			if (fieldType == FieldType.String) {
				// String
			} else if (fieldType == FieldType.Char) {
				if (getVersion().compareTo(FixVersions.BEGINSTRING_FIX41) > 0) {
					CharConverter.convert(field.getValue());
				} else {
					// String, for older FIX versions
				}
			} else if (fieldType == FieldType.Price) {
				DoubleConverter.convert(field.getValue());
			} else if (fieldType == FieldType.Int) {
				IntConverter.convert(field.getValue());
			} else if (fieldType == FieldType.Amt) {
				DoubleConverter.convert(field.getValue());
			} else if (fieldType == FieldType.Qty) {
				DoubleConverter.convert(field.getValue());
			} else if (fieldType == FieldType.Qty) {
				// String
			} else if (fieldType == FieldType.MultipleValueString) {
				// String
			} else if (fieldType == FieldType.Exchange) {
				// String
			} else if (fieldType == FieldType.Boolean) {
				BooleanConverter.convert(field.getValue());
			} else if (fieldType == FieldType.LocalMktDate) {
				// String
			} else if (fieldType == FieldType.Data) {
				// String
			} else if (fieldType == FieldType.Float) {
				DoubleConverter.convert(field.getValue());
			} else if (fieldType == FieldType.PriceOffset) {
				DoubleConverter.convert(field.getValue());
			} else if (fieldType == FieldType.MonthYear) {
				// String
			} else if (fieldType == FieldType.DayOfMonth) {
				// String
			} else if (fieldType == FieldType.UtcDate) {
				UtcDateOnlyConverter.convert(field.getValue());
			} else if (fieldType == FieldType.UtcTimeOnly) {
				UtcTimeOnlyConverter.convert(field.getValue());
			} else if (fieldType == FieldType.UtcTimeStamp || fieldType == FieldType.UtcTimeStampSecondPresicion || fieldType == FieldType.Time) {
				UtcTimestampConverter.convert(field.getValue());
			} else if (fieldType == FieldType.NumInGroup) {
				IntConverter.convert(field.getValue());
			} else if (fieldType == FieldType.Percentage) {
				DoubleConverter.convert(field.getValue());
			} else if (fieldType == FieldType.SeqNum) {
				IntConverter.convert(field.getValue());
			} else if (fieldType == FieldType.Length) {
				IntConverter.convert(field.getValue());
			} else if (fieldType == FieldType.Country) {
				// String
			}
		} catch (final FieldConvertError e) {
			throw new IncorrectDataFormat(field.getTag(), field.getValue());
		}
	}

	// / Check if a field has a value.
	private void checkHasValue(StringField field) {
		if (checkFieldsHaveValues && field.getValue().length() == 0) {
			throw new FieldException(SessionRejectReason.TAG_SPECIFIED_WITHOUT_A_VALUE, field
					.getField());
		}
	}

	// / Check if group count matches number of groups in
	@Override
	public void checkGroupCount(StringField field, FieldMap fieldMap, String msgType) {
		final int fieldNum = field.getField();
		if (isGroup(msgType, fieldNum)) {
			if (fieldMap.getGroupCount(fieldNum) != Integer.parseInt(field.getValue())) {
				throw new FieldException(
						SessionRejectReason.INCORRECT_NUMINGROUP_COUNT_FOR_REPEATING_GROUP,
						fieldNum);
			}
		}
	}

    @Override
    public boolean isAdminMessage(String msgType) {
        // Categories are interned
        return messageCategory.get(msgType) == MESSAGE_CATEGORY_ADMIN;
    }

	@Override
	public void addMsgField(String msgType, int field) {
		Set<Integer> fields = messageFields.get(msgType);
		if (fields == null) {
			fields = new HashSet<>();
			messageFields.put(msgType, fields);
		}
		fields.add(field);
	}

	@Override
	public void addGroup(String msg, int field, int delim, DataDictionary dataDictionary) {
		groups.put(new IntStringPair(field, msg), new GroupInfo(delim, dataDictionary));
	}

	@Override
	public boolean hasVersion() {
		return true;
	}

	@Override
	public String getBeginString() {
		return getVersion();
	}

	@Override
	public boolean isCheckFieldsOutOfOrder() {
		return checkFieldsOutOfOrder;
	}

    @Override
    public boolean isCheckUnorderedGroupFields() {
        return checkUnorderedGroupFields;
    }

	@Override
	public boolean isCheckFieldsHaveValues() {
		return checkFieldsHaveValues;
	}

	@Override
	public boolean isCheckUserDefinedFields() {
		return checkUserDefinedFields;
	}

	@Override
	public Map<String, Set<Integer>> getMessageFields() {
		Map<String, Set<Integer>> msgFields = new HashMap<>();
		for (Map.Entry<String, HashMap<Integer, IFieldStructure>> mesgFld : mesgFields.entrySet()) {
			String msgType = mesgFld.getKey();
			msgFields.put(msgType, new HashSet<>(mesgFld.getValue().keySet()));
		}

		return msgFields;
	}

	@Override
	public Map<String, Set<Integer>> getRequiredFields() {
		return requiredFields;
	}

	@Override
	public Set<String> getMessages() {
		return messages.keySet();
	}

	@Override
	public Set<Integer> getFields() {
		return fields.keySet();
	}

	@Override
	public Map<Integer, Set<String>> getFieldValues() {
		return fieldValues;
	}


	@Override
	public void setCheckFieldsOutOfOrder(boolean checkFieldsOutOfOrder) {
		this.checkFieldsOutOfOrder = checkFieldsOutOfOrder;

	}

	@Override
	public void setCheckFieldsHaveValues(boolean checkFieldsHaveValues) {
		this.checkFieldsHaveValues = checkFieldsHaveValues;
	}

	@Override
	public void setCheckUserDefinedFields(boolean checkUserDefinedFields) {
		this.checkUserDefinedFields = checkUserDefinedFields;
	}

	@Override
	public void setAllowUnknownMessageFields(boolean allowUnknownMessageFields) {
		this.allowUnknownMessageFields = allowUnknownMessageFields;
	}

	@Override
	public String getValueName(int tag, String value) {
	    if (tag == MsgType.FIELD)
            return getMessageName(value);
        String result = null;
        IFieldStructure fieldType = iMsgDict.getFieldStructure(fields.get(tag).getName());
        if (fieldType != null && fieldType.isEnum()) {
            for (String el : fieldType.getValues().keySet()) {
                if (fieldType.getValues().get(el).getValue().equals(value)) {
                    result = el;
                    break;
                }
            }
        }
		return result;
	}

	@Override
	public String getFieldName(int tag) {
    	IFieldStructure field = fields.get(tag);
		return field != null ? field.getName() : null;
	}

	@Override
	public boolean isGroup(String msgId, int field) {
		HashMap<Integer, IFieldStructure> msgFld = mesgFields.get(msgId);
		if (msgFld == null) {
			return false;
		}
		IFieldStructure fld = msgFld.get(field);
		if (fld == null) {
			return false;
		}
		return fld.isCollection();
	}

	@Override
	public int[] getOrderedFields() {
		throw new EPSCommonException("Not implemented");
	}

	@Override
	public boolean isField(int tag) {

		return getFields().contains(tag);
	}

	@Override
	public boolean isHeaderField(int field) {
		return mesgFields.get(HEADER_ID).containsKey(field);
	}

	@Override
	public boolean isTrailerField(int field) {
		return mesgFields.get(TRAILER_ID).containsKey(field);
	}

	@Override
	public boolean isDataField(int tag) {
		IFieldStructure fs = fields.get(tag);
		if (fs == null) {
			return false;
		}
		String fixType = (String) fs.getAttributeValueByName(ATTRIBUTE_FIX_TYPE);
		if (fixType == null) {
			//group field
			return false;
		}
		return fixType.equals("DATA");
	}

	@Override
	public int getFieldTag(String name) {
        final Integer tag = names.get(name);
        return tag != null ? tag.intValue() : -1;
	}

	@Override
	public int getFieldType(int tag) {
		return getFieldTypeEnum(tag).getOrdinal();
	}

	@Override
	public boolean hasFieldValue(int iTagNum) {
		IFieldStructure fld = fields.get(iTagNum);
		return fld.isEnum();
	}

	@Override
	public FieldType getFieldTypeEnum(int tag) {
		String bs = getBeginString();
		IFieldStructure fldType = fields.get(tag);
		if (fldType == null) {
			throw new FieldNotFoundException("tag:" + tag);
		}
		String fixType = (String) fldType.getAttributeValueByName(ATTRIBUTE_FIX_TYPE);
		if (bs == null || fixType == null)	{
			throw new EPSCommonException("Can not get field type");
		}
		return FieldType.fromName(bs, fixType);
//		return null;
	}

	@Override
	public boolean isHeaderGroup(int tag) {

		return isGroup(HEADER_ID, tag);
	}

	@Override
	public boolean isMsgField(String msgType, int tag) {
		return mesgFields.get(msgType).containsKey(tag);
	}

	@Override
	public void validate(Message message, boolean bodyOnly) throws IncorrectTagValue,
			FieldNotFound, IncorrectDataFormat {
		validate(message, bodyOnly ? null : this, this);
	}

    @Override
    public void setCheckUnorderedGroupFields(boolean checkUnorderedGroupFields) {
        this.checkUnorderedGroupFields = checkUnorderedGroupFields;
        for (GroupInfo gi : groups.values()) {
            gi.getDataDictionary().setCheckUnorderedGroupFields(this.checkUnorderedGroupFields);
        }
    }

    @Override
    public void checkValidTagNumber(Field<?> field) {
        if (!fields.containsKey(Integer.valueOf(field.getTag()))) {
            throw new FieldException(SessionRejectReason.INVALID_TAG_NUMBER, field.getField());
        }
    }

    @Override
    public String getMessageName(String msgType) {
        return messages.get(msgType).getName();
    }

    @Override
	public Map<IntStringPair, GroupInfo> getGroups() {
		return groups;
	}

	class GroupDictionary extends DataDictionary {
		private int[] orderedFields = null;
		private HashSet<Integer> fields = null;
		private Set<Integer> grpRequiredFields;
		private IFieldStructure fieldStructure;

		public GroupDictionary(String msg, int field, IFieldStructure fieldStructure) {
			this.fieldStructure = fieldStructure;
		}

		@Override
		public String getVersion() {
			return getDd().getVersion();
		}

		@Override
		public GroupInfo getGroup(String msg, int field) {

			IFieldStructure grpFld = null;
			for (IFieldStructure fld : fieldStructure.getFields()) {
				if (fld.isComplex() && !fld.isCollection()) {
					GroupDictionary dd1 = new GroupDictionary(msg, field, fld);
					GroupInfo grp = dd1.getGroup(msg, field);
					if (grp != null) {
						return grp;
					}
				}
				Integer tag = getFieldTag(fld);
				if (tag == null) {
					continue;
				}
				if (tag == field) {
					if (!fld.isCollection()) {
						return null;
					}
					grpFld = fld;
				}
			}
			if (grpFld == null) {
				return null;
			}
			Integer delim = getFirstFieldWithTag(grpFld);
			return new GroupInfo(delim, new GroupDictionary(msg, field, grpFld));
		}

		@Override
		public void checkHasRequired(FieldMap header, FieldMap body,
				FieldMap trailer, String msgType, boolean bodyOnly) {
			checkHasRequired(msgType, body);
		}

		public void checkHasRequired(String msgType, FieldMap subGroup) {
			final Set<Integer> requiredFieldsForMessage = getRequiredFieldsForGroup() ;
			if (requiredFieldsForMessage == null || requiredFieldsForMessage.size() == 0) {
				return;
			}

			final Iterator<Integer> fieldItr = requiredFieldsForMessage.iterator();
			while (fieldItr.hasNext()) {
				final int field = (fieldItr.next()).intValue();
				if (!subGroup.isSetField(field)) {
					throw new FieldException(SessionRejectReason.REQUIRED_TAG_MISSING, field);
				}
			}

			final Map<Integer, List<Group>> groups = subGroup.getGroups();
			if (groups.size() > 0) {
				final Iterator<Map.Entry<Integer, List<Group>>> groupIter = groups.entrySet()
				.iterator();
				while (groupIter.hasNext()) {
					final Map.Entry<Integer, List<Group>> entry = groupIter.next();
					final GroupInfo p = getGroup(msgType, (entry.getKey()).intValue());
					final List<Group> groupInstances = entry.getValue();
					for (int i = 0; i < groupInstances.size(); i++) {
						final FieldMap groupFields = groupInstances.get(i);
						p.getDataDictionary().checkHasRequired(null, groupFields, null,
								msgType, true);
					}
				}
			}
		}

		private Set<Integer> getRequiredFieldsForGroup() {
			if (grpRequiredFields == null) {
				grpRequiredFields = new HashSet<>();
				addSubmessageRequiredFields(grpRequiredFields, fieldStructure);
			}
			return grpRequiredFields;
		}

		private void addSubmessageRequiredFields(
				Set<Integer> requiredFields, IFieldStructure msgStruct) {
			for(IFieldStructure fldstrct: msgStruct.getFields()) {
				Integer tag = getFieldTag(fldstrct);
				if (fldstrct.isRequired()) {
					requiredFields.add(tag);
				}
				if (tag == null) {
					if (fldstrct.isComplex() && !fldstrct.isCollection()) {
						addSubmessageRequiredFields(requiredFields , fldstrct);
					}
					continue;
				}
			}
		}

		@Override
		public boolean isGroup(String headerId, int field) {
			//FIXME : rehash all submessages of submessages
			for (IFieldStructure fld : fieldStructure.getFields()) {
				if (fld.isComplex() && !fld.isCollection()) {
					GroupDictionary dd1 = new GroupDictionary(headerId, field, fld);
					if (dd1.isGroup(headerId, field)) {
						return true;
					}
				}
				Integer tag = getFieldTag(fld);
				if (tag == null) {
					continue;
				}
				if (tag == field) {
					return fld.isCollection();
				}
			}
			return false;
		}

		@Override
		public int[] getOrderedFields() {
			if (orderedFields == null) {
				indexFields();
			}
			return orderedFields;
		}

		private void indexFields() {
			ArrayList<Integer> fields = new ArrayList<>();
			addSubmessageFields(fields, fieldStructure);
			this.fields = new HashSet<>();
			this.fields.addAll(fields);

			orderedFields = new int[fields.size()];
			for (int i = 0; i<fields.size(); ++i) {
				orderedFields[i] = fields.get(i);
			}
		}

		private void addSubmessageFields(ArrayList<Integer> fields,
				IFieldStructure msgStruct) {
			for(IFieldStructure fldstrct: msgStruct.getFields()) {
				Integer tag = getFieldTag(fldstrct);
				if (tag == null) {
					if (fldstrct.isComplex() && !fldstrct.isCollection()) {
						addSubmessageFields(fields , fldstrct);
					}
					continue;
				}
				fields.add(tag);
			}
		}

		private Integer getFieldTag(IFieldStructure fldstrct) {
			Integer tag = (Integer) fldstrct.getAttributeValueByName("tag");
			return tag;
		}

		@Override
		public boolean isField(int tag) {
			if (this.fields == null) {
				indexFields();
			}
			return this.fields.contains(tag);
		}

        @Override
        public void setCheckUnorderedGroupFields(boolean flag) {
            setCheckUnorderedGroupFields(flag);
            for (GroupInfo gi : groups.values()) {
                gi.getDataDictionary().setCheckUnorderedGroupFields(flag);
            }
        }

        @Override
        public void checkValidTagNumber(Field<?> field) {
            if (!fields.contains(Integer.valueOf(field.getTag()))) {
                throw new FieldException(SessionRejectReason.INVALID_TAG_NUMBER, field.getField());
            }
        }

        DataDictionary getDd() {
			return QFJDictionaryAdapter.this;
		}
	}
}
