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
import java.util.List;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.exactpro.sf.common.messages.structures.IFieldStructure;

public interface IMessageStructureVisitor 
{
	void visit(String fieldName, Boolean value, IFieldStructure fldStruct, boolean isDefault);
	
	void visitBooleanCollection(String fieldName, List<Boolean> value, IFieldStructure fldStruct, boolean isDefault);
	
	void visit(String fieldName, Byte value, IFieldStructure fldStruct, boolean isDefault);
	
	void visitByteCollection(String fieldName, List<Byte> value, IFieldStructure fldStruct, boolean isDefault);
	
	void visit(String fieldName, Character value, IFieldStructure fldStruct, boolean isDefault);
	
	void visitCharCollection(String fieldName, List<Character> value, IFieldStructure fldStruct, boolean isDefault);
	
	void visit(String fieldName, LocalDate value, IFieldStructure fldStruct, boolean isDefault);
	
	void visitDateCollection(String fieldName, List<LocalDate> value, IFieldStructure fldStruct, boolean isDefault);

	void visit(String fieldName, LocalTime value, IFieldStructure fldStruct, boolean isDefault);

	void visitTimeCollection(String fieldName, List<LocalTime> value, IFieldStructure fldStruct, boolean isDefault);

	void visit(String fieldName, LocalDateTime value, IFieldStructure fldStruct, boolean isDefault);

	void visitDateTimeCollection(String fieldName, List<LocalDateTime> value, IFieldStructure fldStruct, boolean isDefault);

	void visit(String fieldName, Double value, IFieldStructure fldStruct, boolean isDefault);
	
	void visitDoubleCollection(String fieldName, List<Double> value, IFieldStructure fldStruct, boolean isDefault);
	
	void visit(String fieldName, Float value, IFieldStructure fldStruct, boolean isDefault);
	
	void visitFloatCollection(String fieldName, List<Float> value, IFieldStructure fldStruct, boolean isDefault);

	void visit(String fieldName, Integer value, IFieldStructure fldStruct, boolean isDefault);
	
	void visitIntCollection(String fieldName, List<Integer> value, IFieldStructure fldStruct, boolean isDefault);

	void visit(String fieldName, Long value, IFieldStructure fldStruct, boolean isDefault);
	
	void visitLongCollection(String fieldName, List<Long> value, IFieldStructure fldStruct, boolean isDefault);
	
	void visit(String fieldName, BigDecimal value, IFieldStructure fldStruct, boolean isDefault);
	
	void visitBigDecimalCollection(String fieldName, List<BigDecimal> value, IFieldStructure fldStruct, boolean isDefault);
	
	void visit(String fieldName, Short value, IFieldStructure fldStruct, boolean isDefault);
	
	void visitShortCollection(String fieldName, List<Short> value, IFieldStructure fldStruct, boolean isDefault);
	
	void visit(String fieldName, String value, IFieldStructure fldStruct, boolean isDefault);
	
	void visitStringCollection(String fieldName, List<String> value, IFieldStructure fldStruct, boolean isDefault);
	
	void visit(String fieldName, IMessage message, IFieldStructure complexField, boolean isDefault);
	
	void visitMessageCollection(String fieldName, List<IMessage> message, IFieldStructure complexField, boolean isDefault);
		
}
