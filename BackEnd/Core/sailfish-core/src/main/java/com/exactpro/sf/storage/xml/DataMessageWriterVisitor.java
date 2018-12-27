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
package com.exactpro.sf.storage.xml;

import java.math.BigDecimal;
import java.util.List;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageStructureVisitor;
import com.exactpro.sf.common.messages.MessageStructureReader;
import com.exactpro.sf.common.messages.MessageStructureReaderHandlerImpl;
import com.exactpro.sf.common.messages.structures.IFieldStructure;

public class DataMessageWriterVisitor implements IMessageStructureVisitor {

	private DataMessage dataMessage;
	
	private static MessageStructureReader rtraverser = new MessageStructureReader(); 
	
	public DataMessageWriterVisitor(DataMessage dataMessage){
		this.dataMessage = dataMessage;
	}
	
	@Override
	public void visit(String fieldName, BigDecimal value,
			IFieldStructure fldStruct, boolean isDefault) {
		DataField dataField = new DataField();
		dataField.setName(fieldName);
		dataField.setValue(TypeConverter.getString(value));
		dataMessage.getFieldsAndFieldsAndMessages().add(dataField);                   
	}

	@Override
	public void visitBigDecimalCollection(String fieldName, List<BigDecimal> value,
			IFieldStructure fldStruct, boolean isDefault) {
		DataFieldArray dataField = new DataFieldArray();
		dataField.setName(fieldName);
		for(BigDecimal val : value){
			dataField.getValues().add(TypeConverter.getString(val));	
		}
		dataMessage.getFieldsAndFieldsAndMessages().add(dataField);
	}

	@Override
	public void visit(String fieldName, Boolean value, IFieldStructure fldStruct, boolean isDefault) {
		DataField dataField = new DataField();
		dataField.setName(fieldName);
		dataField.setValue(TypeConverter.getString(value));
		dataMessage.getFieldsAndFieldsAndMessages().add(dataField);
	}

	@Override
	public void visitBooleanCollection(String fieldName, List<Boolean> value,
			IFieldStructure fldStruct, boolean isDefault) {
		DataFieldArray dataFieldArray = new DataFieldArray();
		dataFieldArray.setName(fieldName);
		for(boolean val : value){
			dataFieldArray.getValues().add(TypeConverter.getString(val));
		}
		dataMessage.getFieldsAndFieldsAndMessages().add(dataFieldArray);
	}

	@Override
	public void visit(String fieldName, Byte value, IFieldStructure fldStruct, boolean isDefault) {
		DataField dataField = new DataField();
		dataField.setName(fieldName);
		dataField.setValue(TypeConverter.getString(value));
		dataMessage.getFieldsAndFieldsAndMessages().add(dataField);
	}

	@Override
	public void visitByteCollection(String fieldName, List<Byte> value, IFieldStructure fldStruct, boolean isDefault) {
		DataFieldArray dataFieldArray = new DataFieldArray();
		dataFieldArray.setName(fieldName);
		for(byte val : value){
			dataFieldArray.getValues().add(TypeConverter.getString(val));
		}
		dataMessage.getFieldsAndFieldsAndMessages().add(dataFieldArray);
	}

	@Override
	public void visitCharCollection(String fieldName, List<Character> value, IFieldStructure fldStruct, boolean isDefault) {
		DataFieldArray dataFieldArray = new DataFieldArray();
		dataFieldArray.setName(fieldName);
		for(char val : value){
			dataFieldArray.getValues().add(TypeConverter.getString(val));
		}
		dataMessage.getFieldsAndFieldsAndMessages().add(dataFieldArray);
	}

	@Override
	public void visit(String fieldName, Character value,
			IFieldStructure fldStruct, boolean isDefault) {
		DataField dataField = new DataField();
		dataField.setName(fieldName);
		dataField.setValue(TypeConverter.getString(value));
		dataMessage.getFieldsAndFieldsAndMessages().add(dataField);
	}

	@Override
	public void visit(String fieldName, LocalDateTime value, IFieldStructure fldStruct, boolean isDefault) {
		DataField dataField = new DataField();
		dataField.setName(fieldName);
		dataField.setValue(TypeConverter.getString(value));
		dataMessage.getFieldsAndFieldsAndMessages().add(dataField);
	}

	@Override
	public void visitDateTimeCollection(String fieldName, List<LocalDateTime> value, IFieldStructure fldStruct, boolean isDefault) {
		DataFieldArray dataFieldArray = new DataFieldArray();
		dataFieldArray.setName(fieldName);
		for(LocalDateTime val : value){
			dataFieldArray.getValues().add(TypeConverter.getString(val));
		}
		dataMessage.getFieldsAndFieldsAndMessages().add(dataFieldArray);
	}

    @Override
    public void visit(String fieldName, LocalDate value, IFieldStructure fldStruct, boolean isDefault) {
        DataField dataField = new DataField();
        dataField.setName(fieldName);
        dataField.setValue(TypeConverter.getString(value));
        dataMessage.getFieldsAndFieldsAndMessages().add(dataField);
    }

    @Override
    public void visitDateCollection(String fieldName, List<LocalDate> value, IFieldStructure fldStruct, boolean isDefault) {
        DataFieldArray dataFieldArray = new DataFieldArray();
        dataFieldArray.setName(fieldName);
        for(LocalDate val : value){
            dataFieldArray.getValues().add(TypeConverter.getString(val));
        }
        dataMessage.getFieldsAndFieldsAndMessages().add(dataFieldArray);
    }

    @Override
    public void visit(String fieldName, LocalTime value, IFieldStructure fldStruct, boolean isDefault) {
        DataField dataField = new DataField();
        dataField.setName(fieldName);
        dataField.setValue(TypeConverter.getString(value));
        dataMessage.getFieldsAndFieldsAndMessages().add(dataField);
    }

    @Override
    public void visitTimeCollection(String fieldName, List<LocalTime> value, IFieldStructure fldStruct, boolean isDefault) {
        DataFieldArray dataFieldArray = new DataFieldArray();
        dataFieldArray.setName(fieldName);
        for(LocalTime val : value){
            dataFieldArray.getValues().add(TypeConverter.getString(val));
        }
        dataMessage.getFieldsAndFieldsAndMessages().add(dataFieldArray);
    }

	@Override
	public void visit(String fieldName, Double value, IFieldStructure fldStruct, boolean isDefault) {
		DataField dataField = new DataField();
		dataField.setName(fieldName);
		dataField.setValue(TypeConverter.getString(value));
		dataMessage.getFieldsAndFieldsAndMessages().add(dataField);
	}

	@Override
	public void visitDoubleCollection(String fieldName, List<Double> value,
			IFieldStructure fldStruct, boolean isDefault) {
		DataFieldArray dataFieldArray = new DataFieldArray();
		dataFieldArray.setName(fieldName);
		for(double val : value){
			dataFieldArray.getValues().add(TypeConverter.getString(val));
		}
		dataMessage.getFieldsAndFieldsAndMessages().add(dataFieldArray);
	}

	@Override
	public void visit(String fieldName, Float value, IFieldStructure fldStruct, boolean isDefault) {
		DataField dataField = new DataField();
		dataField.setName(fieldName);
		dataField.setValue(TypeConverter.getString(value));
		dataMessage.getFieldsAndFieldsAndMessages().add(dataField);
	}

	@Override
	public void visitFloatCollection(String fieldName, List<Float> value, IFieldStructure fldStruct, boolean isDefault) {
		DataFieldArray dataFieldArray = new DataFieldArray();
		dataFieldArray.setName(fieldName);
		for(float val : value){
			dataFieldArray.getValues().add(TypeConverter.getString(val));
		}
	}

	@Override
	public void visit(String fieldName, IMessage message, IFieldStructure complexField, boolean isDefault) {
		DataMessage dMessage = new DataMessage();
		dMessage.setName(message.getName());
		dMessage.setNamespace(message.getNamespace());
		
		DataMessageWriterVisitor visitor  = new DataMessageWriterVisitor(dMessage);
		rtraverser.traverse(visitor, complexField.getFields(), message, MessageStructureReaderHandlerImpl.instance());
		dataMessage.getFieldsAndFieldsAndMessages().add(dMessage);
	}

	@Override
	public void visitIntCollection(String fieldName, List<Integer> value, IFieldStructure fldStruct, boolean isDefault) {
		DataFieldArray dataFieldArray = new DataFieldArray();
		dataFieldArray.setName(fieldName);
		for(int val : value){
			dataFieldArray.getValues().add(TypeConverter.getString(val));
		}
		dataMessage.getFieldsAndFieldsAndMessages().add(dataFieldArray);
	}

	@Override
	public void visit(String fieldName, Integer value, IFieldStructure fldStruct, boolean isDefault) {
		DataField dataField = new DataField();
		dataField.setName(fieldName);
		dataField.setValue(TypeConverter.getString(value));
		dataMessage.getFieldsAndFieldsAndMessages() .add(dataField);
	}

	@Override
	public void visitMessageCollection(String fieldName, List<IMessage> message, IFieldStructure complexField, boolean isDefault) {
		DataMessageArray dmessages = new DataMessageArray();
		dmessages.setName(fieldName);
		
		for(IMessage imessage : message){
			DataMessage dMessage = new DataMessage();
			dMessage.setName(imessage.getName());
			dMessage.setNamespace(imessage.getNamespace());
			
			rtraverser.traverse(new DataMessageWriterVisitor(dMessage), complexField.getFields(), imessage, MessageStructureReaderHandlerImpl.instance());
			dmessages.getMessages().add(dMessage);
		}
		dataMessage.getFieldsAndFieldsAndMessages().add(dmessages);
	}

	@Override
	public void visit(String fieldName, Long value, IFieldStructure fldStruct, boolean isDefault) {
		DataField dataField = new DataField();
		dataField.setName(fieldName);
		dataField.setValue(TypeConverter.getString(value));
		dataMessage.getFieldsAndFieldsAndMessages().add(dataField);
	}

	@Override
	public void visitLongCollection(String fieldName, List<Long> value, IFieldStructure fldStruct, boolean isDefault) {
		DataFieldArray dataFieldArray = new DataFieldArray();
		dataFieldArray.setName(fieldName);
		for(long val : value){
			dataFieldArray.getValues().add(TypeConverter.getString(val));
		}
		dataMessage.getFieldsAndFieldsAndMessages().add(dataFieldArray);
	}

	@Override
	public void visit(String fieldName, Short value, IFieldStructure fldStruct, boolean isDefault) {
		DataField dataField = new DataField();
		dataField.setName(fieldName);
		dataField.setValue(TypeConverter.getString(value));
		dataMessage.getFieldsAndFieldsAndMessages().add(dataField);
	}

	@Override
	public void visitShortCollection(String fieldName, List<Short> value, IFieldStructure fldStruct, boolean isDefault) {
		DataFieldArray dataFieldArray = new DataFieldArray();
		dataFieldArray.setName(fieldName);
		for(short val : value){
			dataFieldArray.getValues().add(TypeConverter.getString(val));
		}
		dataMessage.getFieldsAndFieldsAndMessages().add(dataFieldArray);
	}

	@Override
	public void visit(String fieldName, String value, IFieldStructure fldStruct, boolean isDefault) {
		DataField dataField = new DataField();
		dataField.setName(fieldName);
		dataField.setValue(value);
		dataMessage.getFieldsAndFieldsAndMessages().add(dataField);
	}

	@Override
	public void visitStringCollection(String fieldName, List<String> value,
			IFieldStructure fldStruct, boolean isDefault) {
		DataFieldArray dataFieldArray = new DataFieldArray();
		dataFieldArray.setName(fieldName);
		dataFieldArray.getValues().addAll(value);
		dataMessage.getFieldsAndFieldsAndMessages().add(dataFieldArray);
	}
}
