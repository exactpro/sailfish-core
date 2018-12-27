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
package com.exactpro.sf.common.impl.messages.xml;

import com.exactpro.sf.common.impl.messages.xml.configuration.Attribute;
import com.exactpro.sf.common.impl.messages.xml.configuration.Field;

/**
 * @author nikita.smirnov
 *
 */
public class DictionaryUtils {

    public static Field copy(Field field) {
        Field result = new Field();
        
        result.setDefaultvalue(field.getDefaultvalue());
        result.setDescription(field.getDescription());
        result.setId(field.getId());
        result.setIsCollection(field.isIsCollection());
        result.setName(field.getName());
        result.setReference(field.getReference());
        result.setType(field.getType());
        
        for (Attribute attribute : field.getAttributes()) {
            result.getAttributes().add(copy(attribute));
        }
        for (Attribute attribute : field.getValues()) {
            result.getValues().add(copy(attribute));
        }
        
        return result;
    }
    
    public static Attribute copy(Attribute attribute) {
        Attribute result = new Attribute();
        
        result.setName(attribute.getName());
        result.setType(attribute.getType());
        result.setValue(attribute.getValue());
        
        return result;
    }
}
