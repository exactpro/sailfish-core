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
package com.exactpro.sf.services.util;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.configuration.FieldMapping;
import com.exactpro.sf.configuration.FieldName;
import com.exactpro.sf.configuration.FieldPosition;
import com.exactpro.sf.configuration.RuleDescription;
import com.exactpro.sf.configuration.Rules;
import com.exactpro.sf.util.LRUMap;

public class RulesProcessor {

	private static final Logger logger = LoggerFactory.getLogger(RulesProcessor.class);

	private final Rules rules;
	
	private final Map<String, Object> fieldStorage = new HashMap<>();
	
	private final Map<String, Map<String, Object>> mapStorage = new HashMap<>();

	public RulesProcessor(Rules rules) {
		super();
		this.rules = rules;
	}

	public boolean processMessage(final IMessage message) {
		boolean notSend = false;

		for (RuleDescription rule : rules.getRuleDescription()) {
			if (!rule.getMsgType().equals(message.getName())) {
				continue;
			}

			for (FieldPosition whenField : rule.getWhen().getField()) {
				logger.trace("Start process for when rule with field: {}", whenField.getName());
				
				String key = whenField.getName();
				String value = String.valueOf(lookupField(message, whenField.getName()));

				if (!whenField.getValue().equals(value)) {
					continue;
				}
				if (rule.getNotSend() != null) {
					notSend = true;
					continue;
				}

				if (rule.getChange() != null) {
					for (FieldPosition changeField : rule.getChange().getField()) {
						IMessage msg = lookupParentToFieldMessage(message, changeField.getName());
						if (msg != null) {
							msg.addField(changeField.getName(), changeField.getValue());
						}
					}
				} // if CHANGE

				if (rule.getRemove() != null) {
					for (FieldName removeField : rule.getRemove().getField()) {
						IMessage msg = lookupParentToFieldMessage(message, removeField.getName());
						if (msg != null) {
							msg.removeField(removeField.getName());
						}
					}
				} // if REMOVE

				if (rule.getSave() != null) {
					for (FieldPosition saveField : rule.getSave().getField()) {
						String fieldName = saveField.getName();
						String mappingAlias = saveField.getValue();

						fieldStorage.put(mappingAlias, lookupField(message, fieldName));
					}
				} // if SAVE
				if (null != rule.getLoad()) {
					for (FieldPosition loadField : rule.getLoad().getField()) {
						String fieldName = loadField.getName();
						String mappingAlias = loadField.getValue();

						IMessage msg = lookupParentToFieldMessage(message, loadField.getName());
						if (msg != null) {
							msg.addField(fieldName, fieldStorage.get(mappingAlias));
						} else {
							logger.debug("Can't load: path not found in message");
						}
					}
				} // if LOAD

				if (null != rule.getSaveMapping()) {
					for (FieldMapping saveMappingField : rule.getSaveMapping().getField()) {
						key = saveMappingField.getKeyField();
						value = saveMappingField.getValueField();

						Map<String, Object> map = mapStorage.get(key);
						if (map == null) {
							map = new LRUMap<>(10_000);
							mapStorage.put(key, map);
						}

						map.put(String.valueOf(lookupField(message, key)), lookupField(message, value));
					}
				} // if SAVE MAPPING
				if (null != rule.getLoadMapping()) {
					for (FieldMapping loadMappingField : rule.getLoadMapping().getField()) {
						key = loadMappingField.getKeyField();
						value = loadMappingField.getValueField();

						Map<String, Object> map = mapStorage.get(key);
						if (map == null) {
							map = new LRUMap<>(10_000);
							mapStorage.put(key, map);
						}

						IMessage msg = lookupParentToFieldMessage(message, value);
						if (msg != null) {
							msg.addField(value, map.get(lookupField(msg, key)));
						}
					}
				} // if LOAD MAPPING
			}
		}
		return notSend;

	}

	private Object lookupField(IMessage message, String name) {
		IMessage parent = lookupParentToFieldMessage(message, name);
		
		if (parent == null) {
			return null;
		}
		
		String fieldName = getLastFiled(name);
		
		return parent.getField(fieldName);
	}
	
	private IMessage lookupParentToFieldMessage(IMessage message, String name) {
		if (message == null) {
			return null;
		}
		
		if (name.indexOf(".") == -1) {
			return message;
		}
		
		String group = name.substring(0, name.indexOf("."));
		String restName = name.substring(name.indexOf("."));
		
		// FIXME: collections?
		Object nested = message.getField(group);
		
		if (nested instanceof IMessage) {
			return lookupParentToFieldMessage((IMessage) nested, restName);
		}

		return null;
	}

	private static String getLastFiled(final String path) {
		if (path.indexOf('.') < 0) {
			return path;
		}
		return path.substring(path.lastIndexOf('.'));
	}
}
