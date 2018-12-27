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
import java.util.ArrayList;
import java.util.List;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.util.DateTimeUtility;

public class CreateIMessageVisitor extends DefaultMessageStructureVisitor {
    private IMessageFactory factory;
    private IMessage imessage;

    public IMessage getImessage() {
        return imessage;
    }

    public CreateIMessageVisitor(IMessageFactory factory, String name, String namespace){
        this.factory = factory;
        this.imessage = factory.createMessage(name,namespace);
    }

    @Override
    public void visit(String fieldName, BigDecimal value, IFieldStructure fldStruct, boolean isDefault) {
        imessage.addField(fieldName, new BigDecimal("0"));
    }

    @Override
    public void visitBigDecimalCollection(String fieldName, List<BigDecimal> value, IFieldStructure fldStruct, boolean isDefault) {
        List<BigDecimal> array = new ArrayList<>();
        array.add(new BigDecimal("0"));
        imessage.addField(fieldName, array);
    }

    @Override
    public void visit(String fieldName, Boolean value, IFieldStructure fldStruct, boolean isDefault) {
        imessage.addField(fieldName, true);
    }

    @Override
    public void visitBooleanCollection(String fieldName, List<Boolean> value, IFieldStructure fldStruct, boolean isDefault) {
        List<Boolean> array = new ArrayList<>();
        array.add(true);
        imessage.addField(fieldName, array);
    }

    @Override
    public void visit(String fieldName, Byte value, IFieldStructure fldStruct, boolean isDefault) {
        imessage.addField(fieldName, new Byte("1"));
    }

    @Override
    public void visitByteCollection(String fieldName, List<Byte> value, IFieldStructure fldStruct, boolean isDefault) {
        List<Byte> array = new ArrayList<>();
        array.add((byte)1);
        imessage.addField(fieldName, array);
    }

    @Override
    public void visitCharCollection(String fieldName, List<Character> value, IFieldStructure fldStruct, boolean isDefault) {
        List<Character> array = new ArrayList<>();
        array.add('c');
        imessage.addField(fieldName, array);
    }

    @Override
    public void visit(String fieldName, Character value, IFieldStructure fldStruct, boolean isDefault) {
        imessage.addField(fieldName, 'c');
    }

    @Override
    public void visit(String fieldName, LocalDateTime value, IFieldStructure fldStruct, boolean isDefault) {
        imessage.addField(fieldName, DateTimeUtility.nowLocalDateTime());
    }

    @Override
    public void visitDateTimeCollection(String fieldName, List<LocalDateTime> value, IFieldStructure fldStruct, boolean isDefault) {
        List<LocalDateTime> array = new ArrayList<>();
        array.add(DateTimeUtility.nowLocalDateTime());
        imessage.addField(fieldName, array);

    }

    @Override
    public void visit(String fieldName, LocalDate value, IFieldStructure fldStruct, boolean isDefault) {
        imessage.addField(fieldName, DateTimeUtility.nowLocalDate());
    }

    @Override
    public void visitDateCollection(String fieldName, List<LocalDate> value, IFieldStructure fldStruct, boolean isDefault) {
        List<LocalDate> array = new ArrayList<>();
        array.add(DateTimeUtility.nowLocalDate());
        imessage.addField(fieldName, array);

    }

    @Override
    public void visit(String fieldName, LocalTime value, IFieldStructure fldStruct, boolean isDefault) {
        imessage.addField(fieldName, DateTimeUtility.nowLocalTime());
    }

    @Override
    public void visitTimeCollection(String fieldName, List<LocalTime> value, IFieldStructure fldStruct, boolean isDefault) {
        List<LocalTime> array = new ArrayList<>();
        array.add(DateTimeUtility.nowLocalTime());
        imessage.addField(fieldName, array);
    }

    @Override
    public void visit(String fieldName, Double value, IFieldStructure fldStruct, boolean isDefault) {
        imessage.addField(fieldName, 1.0);
    }

    @Override
    public void visitDoubleCollection(String fieldName, List<Double> value, IFieldStructure fldStruct, boolean isDefault) {
        List<Double> array = new ArrayList<>();
        array.add(1.0d);
        imessage.addField(fieldName, array);
    }

    @Override
    public void visit(String fieldName, Float value, IFieldStructure fldStruct, boolean isDefault) {
        imessage.addField(fieldName, 2.0f);
    }

    @Override
    public void visitFloatCollection(String fieldName, List<Float> value, IFieldStructure fldStruct, boolean isDefault) {
        List<Float> array = new ArrayList<>();
        array.add(2.0f);
        imessage.addField(fieldName, array);
    }

    @Override
    public void visit(String fieldName, IMessage message, IFieldStructure complexField, boolean isDefault) {
        MessageStructureWriter msw = new MessageStructureWriter();
        CreateIMessageVisitor visitor = new CreateIMessageVisitor(factory, fieldName, complexField.getNamespace());
        msw.traverse(visitor, complexField.getFields());
        imessage.addField(fieldName, visitor.getImessage());
    }

    @Override
    public void visitIntCollection(String fieldName, List<Integer> value, IFieldStructure fldStruct, boolean isDefault) {
        List<Integer> array = new ArrayList<>();
        array.add(0);
        imessage.addField(fieldName, array);
    }

    @Override
    public void visit(String fieldName, Integer value, IFieldStructure fldStruct, boolean isDefault) {
        imessage.addField(fieldName, 0);
    }

    @Override
    public void visitMessageCollection(String fieldName, List<IMessage> message, IFieldStructure complexField, boolean isDefault) {
        List<IMessage> limessages = new ArrayList<>(1);
        CreateIMessageVisitor visitor = new CreateIMessageVisitor(factory, fieldName, complexField.getNamespace());

        MessageStructureWriter msw = new MessageStructureWriter();
        msw.traverse(visitor, complexField.getFields());

        limessages.add(visitor.getImessage());

        imessage.addField(fieldName, limessages);
    }

    @Override
    public void visit(String fieldName, Long value, IFieldStructure fldStruct, boolean isDefault) {
        imessage.addField(fieldName, 2L);
    }

    @Override
    public void visitLongCollection(String fieldName, List<Long> value, IFieldStructure fldStruct, boolean isDefault) {
        List<Long> array = new ArrayList<>();
        array.add(2L);
        imessage.addField(fieldName, array);
    }

    @Override
    public void visit(String fieldName, Short value, IFieldStructure fldStruct, boolean isDefault) {
        imessage.addField(fieldName, new Short("0"));
    }

    @Override
    public void visitShortCollection(String fieldName, List<Short> value, IFieldStructure fldStruct, boolean isDefault) {
        List<Short> array = new ArrayList<>();
        array.add(new Short("0"));
        imessage.addField(fieldName, array);
    }

    @Override
    public void visit(String fieldName, String value, IFieldStructure fldStruct, boolean isDefault) {
        imessage.addField(fieldName, fieldName);
    }

    @Override
    public void visitStringCollection(String fieldName, List<String> value, IFieldStructure fldStruct, boolean isDefault) {
        List<String> array = new ArrayList<>();
        array.add(fieldName);
        imessage.addField(fieldName, array);
    }
}
