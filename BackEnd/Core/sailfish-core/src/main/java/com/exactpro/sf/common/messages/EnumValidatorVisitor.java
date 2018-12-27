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
package com.exactpro.sf.common.messages;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;

public class EnumValidatorVisitor implements IMessageStructureVisitor {
    private final List<String> errors;
    private final Deque<String> path;

    public EnumValidatorVisitor() {
        this.errors = new ArrayList<>();
        this.path = new ArrayDeque<>();
    }

    @Override
    public void visit(String fieldName, Boolean value, IFieldStructure fldStruct, boolean isDefault) {
        checkValue(fldStruct, value, true);
    }

    @Override
    public void visitBooleanCollection(String fieldName, List<Boolean> values, IFieldStructure fldStruct, boolean isDefault) {
        checkValues(fldStruct, values);
    }

    @Override
    public void visit(String fieldName, Byte value, IFieldStructure fldStruct, boolean isDefault) {
        checkValue(fldStruct, value, true);
    }

    @Override
    public void visitByteCollection(String fieldName, List<Byte> values, IFieldStructure fldStruct, boolean isDefault) {
        checkValues(fldStruct, values);
    }

    @Override
    public void visit(String fieldName, Character value, IFieldStructure fldStruct, boolean isDefault) {
        checkValue(fldStruct, value, true);
    }

    @Override
    public void visitCharCollection(String fieldName, List<Character> values, IFieldStructure fldStruct, boolean isDefault) {
        checkValues(fldStruct, values);
    }

    @Override
    public void visit(String fieldName, LocalDate value, IFieldStructure fldStruct, boolean isDefault) {
        checkValue(fldStruct, value, true);
    }

    @Override
    public void visitDateCollection(String fieldName, List<LocalDate> values, IFieldStructure fldStruct, boolean isDefault) {
        checkValues(fldStruct, values);
    }

    @Override
    public void visit(String fieldName, LocalTime value, IFieldStructure fldStruct, boolean isDefault) {
        checkValue(fldStruct, value, true);
    }

    @Override
    public void visitTimeCollection(String fieldName, List<LocalTime> values, IFieldStructure fldStruct, boolean isDefault) {
        checkValues(fldStruct, values);
    }

    @Override
    public void visit(String fieldName, LocalDateTime value, IFieldStructure fldStruct, boolean isDefault) {
        checkValue(fldStruct, value, true);
    }

    @Override
    public void visitDateTimeCollection(String fieldName, List<LocalDateTime> values, IFieldStructure fldStruct, boolean isDefault) {
        checkValues(fldStruct, values);
    }

    @Override
    public void visit(String fieldName, Double value, IFieldStructure fldStruct, boolean isDefault) {
        checkValue(fldStruct, value, true);
    }

    @Override
    public void visitDoubleCollection(String fieldName, List<Double> values, IFieldStructure fldStruct, boolean isDefault) {
        checkValues(fldStruct, values);
    }

    @Override
    public void visit(String fieldName, Float value, IFieldStructure fldStruct, boolean isDefault) {
        checkValue(fldStruct, value, true);
    }

    @Override
    public void visitFloatCollection(String fieldName, List<Float> values, IFieldStructure fldStruct, boolean isDefault) {
        checkValues(fldStruct, values);
    }

    @Override
    public void visit(String fieldName, Integer value, IFieldStructure fldStruct, boolean isDefault) {
        checkValue(fldStruct, value, true);
    }

    @Override
    public void visitIntCollection(String fieldName, List<Integer> values, IFieldStructure fldStruct, boolean isDefault) {
        checkValues(fldStruct, values);
    }

    @Override
    public void visit(String fieldName, Long value, IFieldStructure fldStruct, boolean isDefault) {
        checkValue(fldStruct, value, true);
    }

    @Override
    public void visitLongCollection(String fieldName, List<Long> values, IFieldStructure fldStruct, boolean isDefault) {
        checkValues(fldStruct, values);
    }

    @Override
    public void visit(String fieldName, BigDecimal value, IFieldStructure fldStruct, boolean isDefault) {
        checkValue(fldStruct, value, true);
    }

    @Override
    public void visitBigDecimalCollection(String fieldName, List<BigDecimal> values, IFieldStructure fldStruct, boolean isDefault) {
        checkValues(fldStruct, values);
    }

    @Override
    public void visit(String fieldName, Short value, IFieldStructure fldStruct, boolean isDefault) {
        checkValue(fldStruct, value, true);
    }

    @Override
    public void visitShortCollection(String fieldName, List<Short> values, IFieldStructure fldStruct, boolean isDefault) {
        checkValues(fldStruct, values);
    }

    @Override
    public void visit(String fieldName, String value, IFieldStructure fldStruct, boolean isDefault) {
        checkValue(fldStruct, value, true);
    }

    @Override
    public void visitStringCollection(String fieldName, List<String> values, IFieldStructure fldStruct, boolean isDefault) {
        checkValues(fldStruct, values);
    }

    @Override
    public void visit(String fieldName, IMessage message, IFieldStructure complexField, boolean isDefault) {
        if(message == null) {
            return;
        }

        path.addLast(fieldName);
        MessageStructureReader reader = new MessageStructureReader();
        reader.traverse(this, complexField.getFields(), message, MessageStructureReaderHandlerImpl.instance());
        path.removeLast();
    }

    @Override
    public void visitMessageCollection(String fieldName, List<IMessage> messages, IFieldStructure complexField, boolean isDefault) {
        if(messages == null) {
            return;
        }

        for(int i = 0; i < messages.size(); i++) {
            IMessage message = messages.get(i);

            if(message == null) {
                continue;
            }

            path.addLast(complexField.getName() + '[' + i + ']');
            MessageStructureReader reader = new MessageStructureReader();
            reader.traverse(this, complexField.getFields(), message, MessageStructureReaderHandlerImpl.instance());
            path.removeLast();
        }
    }

    private void checkValues(IFieldStructure field, List<?> values) {
        if(values == null) {
            return;
        }

        for(int i = 0; i < values.size(); i++) {
            path.addLast(field.getName() + '[' + i + ']');
            checkValue(field, values.get(i), false);
            path.removeLast();
        }
    }

    private void checkValue(IFieldStructure field, Object value, boolean updatePath) {
        if(value == null || !field.isEnum()) {
            return;
        }

        if(updatePath) {
            path.addLast(field.getName());
        }

        List<Object> values = new ArrayList<>();

        for(IAttributeStructure attribute : field.getValues().values()) {
            Object castValue = attribute.getCastValue();

            if(castValue.equals(value)) {
                if(updatePath) {
                    path.removeLast();
                }

                return;
            }

            values.add(castValue);
        }

        errors.add(String.format("Unknown value in field '%s': %s (expected values: %s)", StringUtils.join(path, '.'), value, values));

        if(updatePath) {
            path.removeLast();
        }
    }

    public List<String> getErrors() {
        return errors;
    }
}
