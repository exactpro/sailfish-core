/*
 * Copyright (c) 2009-2019, Exactpro Systems LLC
 * www.exactpro.com
 * Build Software to Test Software
 *
 * All rights reserved.
 * This is unpublished, licensed software, confidential and proprietary
 * information which is the property of Exactpro Systems LLC or its licensors.
 */

package com.exactpro.sf.common.messages;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.ReorderFieldComparator;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.util.EPSCommonException;

import java.util.List;

import static com.exactpro.sf.common.messages.DirtyConst.FIELD_ORDER;
import static com.exactpro.sf.common.messages.DirtyConst.EXCLUDED_FIELD;
import static com.exactpro.sf.common.messages.DirtyConst.FIELD_ORDER;

public class DirtyMessageTraverser extends MessageTraverser {


    @Override
    public void traverse(IMessageStructureVisitor msgStrVisitor, List<IFieldStructure> fields, IMessage message,
            IMessageStructureReaderHandler handler) {

        fields = combineUnknownFields(fields, message);

        if (message.isFieldSet(FIELD_ORDER)) {
            fields = handleFieldOrderMode(fields, message);
        }

        super.traverse(msgStrVisitor, fields, message, handler);
    }

    protected List<IFieldStructure> handleFieldOrderMode(List<IFieldStructure> fields, IMessage message) {
        Object fieldValue = message.getField(FIELD_ORDER);
        List<String> newFieldOrder = validateFieldOrderValue(fieldValue, String.class);

        ReorderFieldComparator comparator = new ReorderFieldComparator(newFieldOrder, fields);
        fields.sort(comparator);

        return fields;
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
    public void visitSimpleField(IFieldStructure curField, IMessageStructureVisitor msgStrVisitor, JavaType javaType, String fieldName, Object value,
            boolean isDefault) {

        if (dropMetaField(curField, value))
            return;

        super.visitSimpleField(curField, msgStrVisitor, javaType, fieldName, value, isDefault);
    }

    @Override
    protected void visitField(IFieldStructure curField, IMessageStructureVisitor msgStrVisitor, IMessageStructureReaderHandler handler,
            String fieldName, Object value) {

        if (dropMetaField(curField, value))
            return;

        super.visitField(curField, msgStrVisitor, handler, fieldName, value);
    }

    @Override
    protected void visitComplexField(IFieldStructure curField, IMessageStructureVisitor msgStrVisitor, String fieldName, Object value) {

        if (dropMetaField(curField, value))
            return;

        super.visitComplexField(curField, msgStrVisitor, fieldName, value);
    }

    private boolean dropMetaField(IFieldStructure curField, Object value) {
        if (value == EXCLUDED_FIELD)
            return true;
        if (curField.getName().equals(FIELD_ORDER))
            return true;
        return false;
    }
}
