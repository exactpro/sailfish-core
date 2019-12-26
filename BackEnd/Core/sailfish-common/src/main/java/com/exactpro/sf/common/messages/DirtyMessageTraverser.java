/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.sf.common.messages;

import static com.exactpro.sf.common.messages.DirtyConst.EXCLUDED_FIELD;
import static com.exactpro.sf.common.messages.DirtyConst.FIELD_ORDER;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.util.EPSCommonException;

public class DirtyMessageTraverser extends MessageTraverser {


    @Override
    public void traverse(IMessageStructureVisitor msgStrVisitor, Map<String, IFieldStructure> fields, IMessage message,
                         IMessageStructureReaderHandler handler) {

        fields = combineUnknownFields(fields, message);

        if (message.isFieldSet(FIELD_ORDER)) {
            fields = handleFieldOrderMode(fields, message);
        }

        super.traverse(msgStrVisitor, fields, message, handler);
    }

    protected Map<String, IFieldStructure> handleFieldOrderMode(Map<String, IFieldStructure> fields, IMessage message) {
        Object fieldValue = message.getField(FIELD_ORDER);
        List<String> newFieldOrder = validateFieldOrderValue(fieldValue, String.class);

        ReorderFieldComparator comparator = new ReorderFieldComparator(newFieldOrder, fields.keySet());
        TreeMap<String, IFieldStructure> result = new TreeMap<>(comparator);
        result.putAll(fields);

        return result;
    }

    protected boolean ensureListType(List<?> list, Class<?> type) {
        for (Object e : list) {
            if (e != null && !type.isAssignableFrom(e.getClass())) {
                return false;
            }
        }

        return true;
    }

    protected List<String> validateFieldOrderValue(Object fieldValue, Class<?> type) {
        if (!(fieldValue instanceof List<?>)) {
            throw new EPSCommonException(FIELD_ORDER + " field is not a list");
        }
        if (!ensureListType((List<?>) fieldValue, String.class)) {
            throw new EPSCommonException(FIELD_ORDER + " field is not a list of " + type.getName());
        }

        return (List<String>) fieldValue;
    }

    @Override
    protected void visitSimpleField(IFieldStructure curField, IMessageStructureVisitor msgStrVisitor, IMessage message, JavaType javaType, String fieldName, Object value, boolean isDefault) {

        if(dropMetaField(curField, value)) {
            return;
        }
        
        super.visitSimpleField(curField, msgStrVisitor, message, javaType, fieldName, value, isDefault);
    }

    @Override
    protected void visitField(IFieldStructure curField, IMessageStructureVisitor msgStrVisitor, IMessageStructureReaderHandler handler, IMessage message, String fieldName, Object value) {

        if(dropMetaField(curField, value)) {
            return;
        }

        super.visitField(curField, msgStrVisitor, handler, message, fieldName, value);
    }

    @Override
    protected void visitComplexField(IFieldStructure curField, IMessageStructureVisitor msgStrVisitor, String fieldName, Object value) {

        if(dropMetaField(curField, value)) {
            return;
        }

        super.visitComplexField(curField, msgStrVisitor, fieldName, value);
    }

    private boolean dropMetaField(IFieldStructure curField, Object value) {
        return value == EXCLUDED_FIELD || curField.getName().equals(FIELD_ORDER);
    }
}
