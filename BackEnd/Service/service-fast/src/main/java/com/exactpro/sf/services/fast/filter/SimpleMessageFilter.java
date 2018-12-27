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
package com.exactpro.sf.services.fast.filter;

import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.template.Field;
import org.openfast.template.Group;

import java.util.HashMap;
import java.util.HashSet;

public class SimpleMessageFilter implements IFastMessageFilter {

	//ignore messages that do not have required fields,
	//if no required fields defined - accept messages
	private HashSet<String> requiredFields = new HashSet<String>();

	//if a field is presents in a message and is a key in requiredFieldValues
	//it must have the value equal to the value in requiredFieldValues be accepted by filter
	private HashMap<String, String> requiredValues = new HashMap<String, String>();

	//if a field is presents in a message and is a key in ignoreFieldValues
	//it must have the value not equal to the value in ignoreFieldValues to be accepted by filter
	private HashMap<String, String> ignoreValues = new HashMap<String, String>();

	public SimpleMessageFilter() {};



	public SimpleMessageFilter(String filterString) {
		HashMap<String, String> requiredValuesMap = new HashMap<String, String>();
		HashMap<String, String> ignoreValuesMap = new HashMap<String, String>();
		if (filterString == null || filterString.equals("")) {
			setRequiredValues(requiredValuesMap);
			setIgnoreValues(ignoreValuesMap);
			return;
		}
		String[] strvals = filterString.split(";");

		for(String requiredValue : strvals) {
			String[] valuePair = requiredValue.split("=", 2);
			String key = valuePair[0].trim();
			String value = valuePair[1].trim();
			if (value.matches("\\[.*\\]")) {
				value = value.substring(1, value.length()-2);
				requiredValuesMap.put(key, value);
			} else if (value.matches("!\\[.*\\]")) {
				value = value.substring(2, value.length()-3);
				ignoreValuesMap.put(key, value);
			} else {
				requiredValuesMap.put(key, value);
			}
		}
		setRequiredValues(requiredValuesMap);
		setIgnoreValues(ignoreValuesMap);
	}



	/* (non-Javadoc)
     * @see com.exactpro.sf.services.fast.filter.IFastMessageFilter#isMessageAcceptable(org.openfast.Message)
	 */
	@Override
	public boolean isMessageAcceptable(Message fastMsg) {
		if (fastMsg == null) {
			return false;
		}
		fastMsg.getTemplate().getName();
		if (!checkRequiredFields(fastMsg, fastMsg.getTemplate()))  {
			return false;
		}

		if (!checkFieldValues(fastMsg)) {
			return false;
		}
		return true;
	}

	private boolean checkFieldValues(GroupValue fastMsg) {
		for(String fieldName: getRequiredValues().keySet()) {
			Group group = fastMsg.getGroup();
			if (!group.hasField(fieldName)) {
				continue;
			}
			if (!fastMsg.isDefined(fieldName)) {
				continue;
			}
			Field field = group.getField(fieldName);

			if (field.getTypeName().equals("scalar")) {
				String value = fastMsg.getString(field.getName());
				String requiredValue = getRequiredValues().get(fieldName);
				if (!requiredValue.equals(value)) {
					return false;
				}
			}
		}

		for(String fieldName: getIgnoreValues().keySet()) {
			Group group = fastMsg.getGroup();
			if (!group.hasField(fieldName)) {
				continue;
			}
			if (!fastMsg.isDefined(fieldName)) {
				continue;
			}
			Field field = group.getField(fieldName);

			if (field.getTypeName().equals("scalar")) {
				String value = fastMsg.getString(field.getName());
				String ignoreValue = getIgnoreValues().get(fieldName);
				if (ignoreValue.equals(value)) {
					return false;
				}
			}
		}

		return true;
	}

	private boolean checkRequiredFields(GroupValue fastMsg, Group template) {
		for(String fieldName: getRequiredFields()) {
			if (!fastMsg.isDefined(fieldName)) {
				return false;
			}
		}
		return true;
	}

	public void setRequiredFields(HashSet<String> requiredFields) {
		if (requiredFields == null) {
			throw new NullPointerException();
		}
		this.requiredFields = requiredFields;
	}

	public HashSet<String> getRequiredFields() {
		return requiredFields;
	}

	public void setRequiredValues(HashMap<String, String> requiredValues) {
		if (requiredValues == null) {
			throw new NullPointerException();
		}
		this.requiredValues = requiredValues;
	}

	public HashMap<String, String> getRequiredValues() {
		return requiredValues;
	}

	public void setIgnoreValues(HashMap<String, String> ignoreValues) {
		if (ignoreValues == null) {
			throw new NullPointerException();
		}
		this.ignoreValues = ignoreValues;
	}

	public HashMap<String, String> getIgnoreValues() {
		return ignoreValues;
	}
}
