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
package com.exactpro.sf.common.impl.messages.xml.converter;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exactpro.sf.common.impl.messages.xml.configuration.Dictionary;
import com.exactpro.sf.common.impl.messages.xml.configuration.Field;
import com.exactpro.sf.common.impl.messages.xml.configuration.Message;
import com.exactpro.sf.common.messages.structures.IFieldStructure;

public class XmlDictionaryUtils {

	public static Field getDictionaryField(Dictionary dictionary, String name){
		for(Field field : dictionary.getFields().getFields()){
			if(field.getName().equals(name)){
				return field;
			}
		}
		return null;
	}

    public static List<Map<String, Object>> getGroupFieldsName(IFieldStructure fieldStructure){
        List<Map<String, Object>> fieldsName = new ArrayList<Map<String, Object>>();
        List<IFieldStructure> fields = fieldStructure.getFields();
        for(IFieldStructure field : fields) {
            if(field.isComplex()) {
                Map<String, Object> groupFieldsName = new HashMap<String, Object>();
                groupFieldsName.put(field.getName(), getGroupFieldsName(field));
                fieldsName.add(groupFieldsName);
            } else if (field.getReferenceName() != null) {
                Map<String, Object> groupFieldsName = new HashMap<String, Object>();
                groupFieldsName.put(field.getReferenceName(), null);
                fieldsName.add(groupFieldsName);
            }
        }
        return fieldsName;
    }

    protected static List<Map.Entry<String, Object>> getGroupFieldsName(Message field){
        if(field == null){
            return null;
        }
        List<Map.Entry<String, Object>> groupFieldsName = new ArrayList<Map.Entry<String, Object>>();
        Message subField = (Message)field.getReference();
        if(subField == null){
            for(Field fieldName: field.getFields()){
                SimpleEntry<String, Object> fieldMap = new HashMap.SimpleEntry<String, Object>(fieldName.getName(), null);
                groupFieldsName.add(fieldMap);
            }
        } else {
            SimpleEntry<String, Object> fieldMap = new HashMap.SimpleEntry<String, Object>(subField.getName(), getGroupFieldsName(subField));
            groupFieldsName.add(fieldMap);
        }
        return groupFieldsName;
    }
	
}
