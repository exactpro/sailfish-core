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
package com.exactpro.sf.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.exactpro.sf.common.util.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageStructureVisitor;
import com.exactpro.sf.common.messages.MessageStructureReader;
import com.exactpro.sf.common.messages.MessageStructureReaderHandlerImpl;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.StructureUtils;
import com.exactpro.sf.aml.scriptutil.StaticUtil;
import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;
import com.exactpro.sf.comparison.ComparisonResult;

public class EnumReplacer {
    private static final Logger logger = LoggerFactory.getLogger(EnumReplacer.class);

    /**
     * Tries to replace enum values with their aliases.
     */
    public static void replaceEnums(ComparisonResult comparisonResult, IFieldStructure fieldStructure) {
        Objects.requireNonNull(comparisonResult, "comparison result cannot be null");
        Objects.requireNonNull(fieldStructure, "field structure cannot be null");

        if(comparisonResult.hasResults()) {
            for(ComparisonResult subComparison : comparisonResult) {
                IFieldStructure subFieldStructure = fieldStructure;

                if(fieldStructure.isComplex()) {
                    subFieldStructure = fieldStructure.getField(subComparison.getName());
                }

                if (subFieldStructure != null) {
                    replaceEnums(subComparison, subFieldStructure);
                }
            }
        } else if(fieldStructure.isEnum()) {
            JavaType type = fieldStructure.getJavaType();

            String actualAlias = EnumUtils.getAlias(fieldStructure, comparisonResult.getActual());
            String expectedAlias = EnumUtils.getAlias(fieldStructure, castValue(comparisonResult.getExpected(), type));

            if(actualAlias != null) {
                comparisonResult.setActual(actualAlias);
            }

            if(expectedAlias != null) {
                comparisonResult.setExpected(expectedAlias);
            }
        }
    }

    /**
     * Tries to replace enum values with their aliases. Creates a new message.
     * This method should only be used to change visual representation of a message
     */
    public static IMessage replaceEnums(IMessage message, IMessageStructure messageStructure) {
        Objects.requireNonNull(message, "message cannot be null");
        Objects.requireNonNull(messageStructure, "message structure cannot be null");

        MessageStructureReader messageStructureReader = new MessageStructureReader();
        EnumReplacingVisitor enumReplacingVisitor = new EnumReplacingVisitor(message.cloneMessage());
        messageStructureReader.traverse(enumReplacingVisitor, messageStructure, (IMessage)StaticUtil.stripFilter(message, true), MessageStructureReaderHandlerImpl.instance());

        return enumReplacingVisitor.getMessage();
    }

    private static Object castValue(Object value, JavaType type) {
        try {
            if(value instanceof IFilter) {
                IFilter filter = (IFilter)value;

                if(!filter.hasValue()) {
                    return null;
                }

                value = filter.getCondition();
            }

            return StructureUtils.castValueToJavaType(Objects.toString(value, null), type);
        } catch(Exception e) {
            logger.debug("Failed to cast value '{}' to type: {}", value, type, e);
            return null;
        }
    }

    private static class EnumReplacingVisitor implements IMessageStructureVisitor {
        private final IMessage message;

        public EnumReplacingVisitor(IMessage message) {
            this.message = message;
        }

        @Override
        public void visit(String fieldName, Boolean value, IFieldStructure fldStruct, boolean isDefault) {
            visit(fieldName, value, fldStruct);
        }

        @Override
        public void visitBooleanCollection(String fieldName, List<Boolean> values, IFieldStructure fldStruct, boolean isDefault) {
            visit(fieldName, values, fldStruct);
        }

        @Override
        public void visit(String fieldName, Byte value, IFieldStructure fldStruct, boolean isDefault) {
            visit(fieldName, value, fldStruct);
        }

        @Override
        public void visitByteCollection(String fieldName, List<Byte> values, IFieldStructure fldStruct, boolean isDefault) {
            visit(fieldName, values, fldStruct);
        }

        @Override
        public void visit(String fieldName, Character value, IFieldStructure fldStruct, boolean isDefault) {
            visit(fieldName, value, fldStruct);
        }

        @Override
        public void visitCharCollection(String fieldName, List<Character> values, IFieldStructure fldStruct, boolean isDefault) {
            visit(fieldName, values, fldStruct);
        }

        @Override
        public void visit(String fieldName, LocalDate value, IFieldStructure fldStruct, boolean isDefault) {
            visit(fieldName, value, fldStruct);
        }

        @Override
        public void visitDateCollection(String fieldName, List<LocalDate> values, IFieldStructure fldStruct, boolean isDefault) {
            visit(fieldName, values, fldStruct);
        }

        @Override
        public void visit(String fieldName, LocalTime value, IFieldStructure fldStruct, boolean isDefault) {
            visit(fieldName, value, fldStruct);
        }

        @Override
        public void visitTimeCollection(String fieldName, List<LocalTime> values, IFieldStructure fldStruct, boolean isDefault) {
            visit(fieldName, values, fldStruct);
        }

        @Override
        public void visit(String fieldName, LocalDateTime value, IFieldStructure fldStruct, boolean isDefault) {
            visit(fieldName, value, fldStruct);
        }

        @Override
        public void visitDateTimeCollection(String fieldName, List<LocalDateTime> values, IFieldStructure fldStruct, boolean isDefault) {
            visit(fieldName, values, fldStruct);
        }

        @Override
        public void visit(String fieldName, Double value, IFieldStructure fldStruct, boolean isDefault) {
            visit(fieldName, value, fldStruct);
        }

        @Override
        public void visitDoubleCollection(String fieldName, List<Double> values, IFieldStructure fldStruct, boolean isDefault) {
            visit(fieldName, values, fldStruct);
        }

        @Override
        public void visit(String fieldName, Float value, IFieldStructure fldStruct, boolean isDefault) {
            visit(fieldName, value, fldStruct);
        }

        @Override
        public void visitFloatCollection(String fieldName, List<Float> values, IFieldStructure fldStruct, boolean isDefault) {
            visit(fieldName, values, fldStruct);
        }

        @Override
        public void visit(String fieldName, Integer value, IFieldStructure fldStruct, boolean isDefault) {
            visit(fieldName, value, fldStruct);
        }

        @Override
        public void visitIntCollection(String fieldName, List<Integer> values, IFieldStructure fldStruct, boolean isDefault) {
            visit(fieldName, values, fldStruct);
        }

        @Override
        public void visit(String fieldName, Long value, IFieldStructure fldStruct, boolean isDefault) {
            visit(fieldName, value, fldStruct);
        }

        @Override
        public void visitLongCollection(String fieldName, List<Long> values, IFieldStructure fldStruct, boolean isDefault) {
            visit(fieldName, values, fldStruct);
        }

        @Override
        public void visit(String fieldName, BigDecimal value, IFieldStructure fldStruct, boolean isDefault) {
            visit(fieldName, value, fldStruct);
        }

        @Override
        public void visitBigDecimalCollection(String fieldName, List<BigDecimal> values, IFieldStructure fldStruct, boolean isDefault) {
            visit(fieldName, values, fldStruct);
        }

        @Override
        public void visit(String fieldName, Short value, IFieldStructure fldStruct, boolean isDefault) {
            visit(fieldName, value, fldStruct);
        }

        @Override
        public void visitShortCollection(String fieldName, List<Short> values, IFieldStructure fldStruct, boolean isDefault) {
            visit(fieldName, values, fldStruct);
        }

        @Override
        public void visit(String fieldName, String value, IFieldStructure fldStruct, boolean isDefault) {
            visit(fieldName, value, fldStruct);
        }

        @Override
        public void visitStringCollection(String fieldName, List<String> values, IFieldStructure fldStruct, boolean isDefault) {
            visit(fieldName, values, fldStruct);
        }

        @Override
        public void visit(String fieldName, IMessage value, IFieldStructure complexField, boolean isDefault) {
            if(value == null) {
                return;
            }

            MessageStructureReader messageStructureReader = new MessageStructureReader();
            EnumReplacingVisitor visitor = new EnumReplacingVisitor(value);

            messageStructureReader.traverse(visitor, complexField.getFields(), value, MessageStructureReaderHandlerImpl.instance());
            this.message.addField(fieldName, visitor.getMessage());
        }

        @Override
        public void visitMessageCollection(String fieldName, List<IMessage> values, IFieldStructure complexField, boolean isDefault) {
            if(values == null) {
                return;
            }

            List<IMessage> list = new ArrayList<>();

            for(IMessage value : values) {
                if(value == null) {
                    list.add(null);
                    continue;
                }

                MessageStructureReader messageStructureReader = new MessageStructureReader();
                EnumReplacingVisitor visitor = new EnumReplacingVisitor(value);

                messageStructureReader.traverse(visitor, complexField.getFields(), value, MessageStructureReaderHandlerImpl.instance());
                list.add(visitor.getMessage());
            }

            this.message.addField(fieldName, list);
        }

        private void visit(String fieldName, List<?> values, IFieldStructure fldStruct) {
            if(values == null) {
                return;
            }

            List<Object> newValues = new ArrayList<>();

            for(Object value : values) {
                if(fldStruct.isEnum()) {
                    String alias = EnumUtils.getAlias(fldStruct, value);
                    value = alias != null ? alias : value;
                }

                newValues.add(value);
            }

            message.addField(fieldName, newValues);
        }

        private void visit(String fieldName, Object value, IFieldStructure fldStruct) {
            if(value == null) {
                return;
            }

            if(fldStruct.isEnum()) {
                String alias = EnumUtils.getAlias(fldStruct, value);
                value = alias != null ? alias : value;
            }

            message.addField(fieldName, value);
        }

        public IMessage getMessage() {
            return message;
        }
    }
}
