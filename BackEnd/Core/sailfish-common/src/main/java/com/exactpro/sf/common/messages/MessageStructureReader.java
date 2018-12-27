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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.EPSCommonException;

public class MessageStructureReader {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName() + "@" + Integer.toHexString(hashCode()));

    public MessageStructureReader() {
    }


	public void traverse(IMessageStructureVisitor msgStrVisitor,
			IMessageStructure msgStructure,
			IMessage message,
			IMessageStructureReaderHandler handler)
	{
		try
		{
			this.traverse(msgStrVisitor, msgStructure.getFields(), message, handler);
		}
		catch ( Exception e )
		{
            StringBuilder builder = new StringBuilder();
            builder.append(e.getMessage()).append(". in MessageStructure Name = [");
            if(msgStructure != null && msgStructure.getName() != null){
                builder.append(msgStructure.getName());
            }
            builder.append("]");
			throw new EPSCommonException(builder.toString(), e);
		}
	}


	public void traverse(IMessageStructureVisitor msgStrVisitor,
			List<IFieldStructure> fields,
			IMessage message,
			IMessageStructureReaderHandler handler)
	{

		for (IFieldStructure curField :  fields )
		{

			String fieldName = curField.getName();

			if (message == null) throw new NullPointerException("message is null for field "+fieldName);

			Object value = message.getField(fieldName);

			try {
    			visitField(curField, msgStrVisitor, handler, fieldName, value);
			} catch (RuntimeException e) {
			    throw new EPSCommonException("Travers problem for FieldName = " + fieldName + ", FieldValue = " + message.getField(fieldName), e);
			}
		}

	}


    protected void visitField(IFieldStructure curField, IMessageStructureVisitor msgStrVisitor, IMessageStructureReaderHandler handler,
            String fieldName, Object value) {
        if ( !curField.isComplex() )
        {
        	JavaType javaType = curField.getJavaType();

            Object curFieldDefaultValue = curField.getDefaultValue();
        	if ( value == null && curField.getDefaultValue() == null &&
        			curField.isRequired() )
        	{
        		handler.onRequiredFieldAbsence(curField);
        	}

            boolean isDefault = false;
            if (value == null && curFieldDefaultValue != null) {
                boolean invalid = curField.isCollection() && !(curFieldDefaultValue instanceof List<?>);
                if (!invalid) {
                    value = curFieldDefaultValue;
                    isDefault = true;
                } else {
                    logger.warn("Incorrect default value for [{}] field", fieldName);
                }
            }

        	visitSimpleField(curField, msgStrVisitor, javaType, fieldName, value, isDefault);

        } else {
        	visitComplexField(curField, msgStrVisitor, fieldName, value);
        }
    }


    @SuppressWarnings("unchecked")
    public void visitSimpleField(IFieldStructure curField, IMessageStructureVisitor msgStrVisitor,
                                        JavaType javaType, String fieldName, Object value, boolean isDefault) {
        try {
            switch ( javaType )
            {
                case JAVA_LANG_BOOLEAN :
                    if ( !curField.isCollection() )
                        msgStrVisitor.visit(fieldName, (Boolean)value, curField, isDefault);
                    else
                        msgStrVisitor.visitBooleanCollection(fieldName, (List<Boolean>)value, curField, isDefault);
                    break;
                case JAVA_LANG_SHORT :
                    if ( !curField.isCollection() )
                        msgStrVisitor.visit(
                                fieldName,
                                (Short)value,
                                curField, isDefault);
                    else
                        msgStrVisitor.visitShortCollection(
                                fieldName,
                                (List<Short>)value,
                                curField, isDefault);
                    break;
                case JAVA_LANG_INTEGER :
                    if ( !curField.isCollection() )
                        msgStrVisitor.visit(
                                fieldName,
                                (Integer)value,
                                curField, isDefault);
                    else
                        msgStrVisitor.visitIntCollection(
                                fieldName,
                                (List<Integer>)value,
                                curField, isDefault);
                    break;
                case JAVA_LANG_LONG :
                    if ( !curField.isCollection() )
                        msgStrVisitor.visit(
                                fieldName,
                                (Long)value,
                                curField, isDefault);
                    else
                        msgStrVisitor.visitLongCollection(
                                fieldName,
                                (List<Long>)value,
                                curField, isDefault);
                    break;
                case JAVA_LANG_BYTE :
                    if ( !curField.isCollection() )
                        msgStrVisitor.visit(
                                curField.getName(),
                                (Byte)value,
                                curField, isDefault);
                    else
                        msgStrVisitor.visitByteCollection(
                                fieldName,
                                (List<Byte>)value,
                                curField, isDefault);
                    break;
                case JAVA_LANG_FLOAT :
                    if ( !curField.isCollection() )
                        msgStrVisitor.visit(
                                fieldName,
                                (Float)value,
                                curField, isDefault);
                    else
                        msgStrVisitor.visitFloatCollection(
                                fieldName,
                                (List<Float>)value,
                                curField, isDefault);
                    break;
                case JAVA_LANG_DOUBLE :
                    if ( !curField.isCollection() )
                        msgStrVisitor.visit(
                                fieldName,
                                (Double)value,
                                curField, isDefault);
                    else
                        msgStrVisitor.visitDoubleCollection(
                                fieldName,
                                (List<Double>)value,
                                curField, isDefault);
                    break;
                case JAVA_LANG_STRING :
                    if ( !curField.isCollection() )
                        msgStrVisitor.visit(
                                fieldName,
                                (String)value,
                                curField, isDefault);
                    else
                        msgStrVisitor.visitStringCollection(
                                fieldName,
                                (List<String>)value,
                                curField, isDefault);
                    break;
                case JAVA_TIME_LOCAL_DATE_TIME:
                    if ( !curField.isCollection() )
                        msgStrVisitor.visit(
                                fieldName,
                                (LocalDateTime) value,
                                curField, isDefault);
                    else
                        msgStrVisitor.visitDateTimeCollection(
                                fieldName,
                                (List<LocalDateTime>)value,
                                curField, isDefault);
                    break;
                case JAVA_TIME_LOCAL_DATE:
                    if ( !curField.isCollection() )
                        msgStrVisitor.visit(
                                fieldName,
                                (LocalDate) value,
                                curField, isDefault);
                    else
                        msgStrVisitor.visitDateCollection(
                                fieldName,
                                (List<LocalDate>)value,
                                curField, isDefault);
                    break;
                case JAVA_TIME_LOCAL_TIME:
                    if ( !curField.isCollection() )
                        msgStrVisitor.visit(
                                fieldName,
                                (LocalTime) value,
                                curField, isDefault);
                    else
                        msgStrVisitor.visitTimeCollection(
                                fieldName,
                                (List<LocalTime>)value,
                                curField, isDefault);
                    break;
                case JAVA_LANG_CHARACTER:
                    if ( !curField.isCollection() )
                        msgStrVisitor.visit(
                                fieldName,
                                (Character)value,
                                curField, isDefault);
                    else
                        msgStrVisitor.visitCharCollection(
                                fieldName,
                                (List<Character>)value,
                                curField, isDefault);
                    break;
                case JAVA_MATH_BIG_DECIMAL:
                    if ( !curField.isCollection() )
                        msgStrVisitor.visit(
                                fieldName,
                                (BigDecimal)value,
                                curField, isDefault);
                    else
                        msgStrVisitor.visitBigDecimalCollection(
                                fieldName,
                                (List<BigDecimal>)value,
                                curField, isDefault);
                    break;
                default:
                    throw new EPSCommonException("Unknown FieldType = [" +
                            javaType + "] for FieldName = [" +
                            curField.getName() + "]" );
            }
        }
        catch ( ClassCastException e )
        {
            throw new EPSCommonException(e.getMessage()+ " fieldName = " + fieldName + ", javaType = " + javaType, e);
        }
    }


    @SuppressWarnings("unchecked")
    protected void visitComplexField(IFieldStructure curField, IMessageStructureVisitor msgStrVisitor, String fieldName, Object value) {
        if (!curField.isCollection()) {
        	msgStrVisitor.visit(fieldName, (IMessage) value, curField, false);
        } else {
        	msgStrVisitor.visitMessageCollection(fieldName, (List<IMessage>) value, curField, false);
        }
    }

}
