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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.comparison.conversion.MultiConverter;

/**
 *  Using on encode to native view
 *  Call IMessageStructureVisitor for each IMessage`s field described in IMessageStructure.
 *  Value with simple type convert in right type before call IMessageStructureVisitor
 */
@ThreadSafe
public class MessageStructureReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageStructureReader.class);

    public static MessageStructureReader READER = new MessageStructureReader();

    protected MessageStructureReader(){}

    public void traverse(IMessageStructureVisitor msgStrVisitor,
                         IMessageStructure msgStructure,
                         IMessage message,
                         IMessageStructureReaderHandler handler)
	{
		try
		{
            traverse(msgStrVisitor, msgStructure.getFields(), message, handler);
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
            Map<String, IFieldStructure> fields,
			IMessage message,
			IMessageStructureReaderHandler handler)
	{

        fields.forEach((fieldName, curField) -> {
            if(message == null) {
                throw new NullPointerException("message is null for field " + fieldName);
            }

			Object value = message.getField(fieldName);

			try {
    			visitField(curField, msgStrVisitor, handler, message, fieldName, value);
			} catch (RuntimeException e) {
			    throw new EPSCommonException("Travers problem for FieldName = " + fieldName + ", FieldValue = " + message.getField(fieldName), e);
			}
        });
	}


    protected void visitField(IFieldStructure curField, IMessageStructureVisitor msgStrVisitor, IMessageStructureReaderHandler handler,
            IMessage message, String fieldName, Object value) {
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
                    LOGGER.warn("Incorrect default value for [{}] field", fieldName);
                }
            }

        	visitSimpleField(curField, msgStrVisitor, message, javaType, fieldName, value, isDefault);

        } else {
        	visitComplexField(curField, msgStrVisitor, fieldName, value);
        }
    }


    @SuppressWarnings("unchecked")
    protected void visitSimpleField(IFieldStructure curField, IMessageStructureVisitor msgStrVisitor, IMessage message,
                                        JavaType javaType, String fieldName, Object value, boolean isDefault) {
        try {
            switch ( javaType )
            {
                case JAVA_LANG_BOOLEAN :
                    if (!curField.isCollection()) {
                        msgStrVisitor.visit(
                                fieldName,
                                castByDictionary(message, fieldName, value, Boolean.class),
                                curField, isDefault);
                    } else {
                        msgStrVisitor.visitBooleanCollection(
                                fieldName,
                                castCollectionByDictionary(message, fieldName, value, Boolean.class),
                                curField, isDefault);
                    }
                    break;
                case JAVA_LANG_SHORT :
                    if (!curField.isCollection()) {
                        msgStrVisitor.visit(
                                fieldName,
                                castByDictionary(message, fieldName, value, Short.class),
                                curField, isDefault);
                    } else {
                        msgStrVisitor.visitShortCollection(
                                fieldName,
                                castCollectionByDictionary(message, fieldName, value, Short.class),
                                curField, isDefault);
                    }
                    break;
                case JAVA_LANG_INTEGER :
                    if (!curField.isCollection()) {
                        msgStrVisitor.visit(
                                fieldName,
                                castByDictionary(message, fieldName, value, Integer.class),
                                curField, isDefault);
                    } else {
                        msgStrVisitor.visitIntCollection(
                                fieldName,
                                castCollectionByDictionary(message, fieldName, value, Integer.class),
                                curField, isDefault);
                    }
                    break;
                case JAVA_LANG_LONG :
                    if (!curField.isCollection()) {
                        msgStrVisitor.visit(
                                fieldName,
                                castByDictionary(message, fieldName, value, Long.class),
                                curField, isDefault);
                    } else {
                        msgStrVisitor.visitLongCollection(
                                fieldName,
                                castCollectionByDictionary(message, fieldName, value, Long.class),
                                curField, isDefault);
                    }
                    break;
                case JAVA_LANG_BYTE :
                    if (!curField.isCollection()) {
                        msgStrVisitor.visit(
                                curField.getName(),
                                castByDictionary(message, fieldName, value, Byte.class),
                                curField, isDefault);
                    } else {
                        msgStrVisitor.visitByteCollection(
                                fieldName,
                                castCollectionByDictionary(message, fieldName, value, Byte.class),
                                curField, isDefault);
                    }
                    break;
                case JAVA_LANG_FLOAT :
                    if (!curField.isCollection()) {
                        msgStrVisitor.visit(
                                fieldName,
                                castByDictionary(message, fieldName, value, Float.class),
                                curField, isDefault);
                    } else {
                        msgStrVisitor.visitFloatCollection(
                                fieldName,
                                castCollectionByDictionary(message, fieldName, value, Float.class),
                                curField, isDefault);
                    }
                    break;
                case JAVA_LANG_DOUBLE :
                    if (!curField.isCollection()) {
                        msgStrVisitor.visit(
                                fieldName,
                                castByDictionary(message, fieldName, value, Double.class),
                                curField, isDefault);
                    } else {
                        msgStrVisitor.visitDoubleCollection(
                                fieldName,
                                castCollectionByDictionary(message, fieldName, value, Double.class),
                                curField, isDefault);
                    }
                    break;
                case JAVA_LANG_STRING :
                    if (!curField.isCollection()) {
                        msgStrVisitor.visit(
                                fieldName,
                                castByDictionary(message, fieldName, value, String.class),
                                curField, isDefault);
                    } else {
                        msgStrVisitor.visitStringCollection(
                                fieldName,
                                castCollectionByDictionary(message, fieldName, value, String.class),
                                curField, isDefault);
                    }
                    break;
                case JAVA_TIME_LOCAL_DATE_TIME:
                    if (!curField.isCollection()) {
                        msgStrVisitor.visit(
                                fieldName,
                                castByDictionary(message, fieldName, value, LocalDateTime.class),
                                curField, isDefault);
                    } else {
                        msgStrVisitor.visitDateTimeCollection(
                                fieldName,
                                castCollectionByDictionary(message, fieldName, value, LocalDateTime.class),
                                curField, isDefault);
                    }
                    break;
                case JAVA_TIME_LOCAL_DATE:
                    if (!curField.isCollection()) {
                        msgStrVisitor.visit(
                                fieldName,
                                castByDictionary(message, fieldName, value, LocalDate.class),
                                curField, isDefault);
                    } else {
                        msgStrVisitor.visitDateCollection(
                                fieldName,
                                castCollectionByDictionary(message, fieldName, value, LocalDate.class),
                                curField, isDefault);
                    }
                    break;
                case JAVA_TIME_LOCAL_TIME:
                    if (!curField.isCollection()) {
                        msgStrVisitor.visit(
                                fieldName,
                                castByDictionary(message, fieldName, value, LocalTime.class),
                                curField, isDefault);
                    } else {
                        msgStrVisitor.visitTimeCollection(
                                fieldName,
                                castCollectionByDictionary(message, fieldName, value, LocalTime.class),
                                curField, isDefault);
                    }
                    break;
                case JAVA_LANG_CHARACTER:
                    if (!curField.isCollection()) {
                        msgStrVisitor.visit(
                                fieldName,
                                castByDictionary(message, fieldName, value, Character.class),
                                curField, isDefault);
                    } else {
                        msgStrVisitor.visitCharCollection(
                                fieldName,
                                castCollectionByDictionary(message, fieldName, value, Character.class),
                                curField, isDefault);
                    }
                    break;
                case JAVA_MATH_BIG_DECIMAL:
                    if (!curField.isCollection()) {
                        msgStrVisitor.visit(
                                fieldName,
                                castByDictionary(message, fieldName, value, BigDecimal.class),
                                curField, isDefault);
                    } else {
                        msgStrVisitor.visitBigDecimalCollection(
                                fieldName,
                                castCollectionByDictionary(message, fieldName, value, BigDecimal.class),
                                curField, isDefault);
                    }
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
    
    private <T> T castByDictionary(IMessage message, String fieldName, Object value, Class<T> clazz) {
        T convertedValue = MultiConverter.convert(value, clazz);
        message.addField(fieldName, convertedValue);
        return convertedValue;
    }
    
    private <T> List<T> castCollectionByDictionary(IMessage message, String fieldName, Object value, Class<T> clazz) {
        List<T> convertedCollection = MultiConverter.convert((Collection<?>)value, clazz, ArrayList::new);
        message.addField(fieldName, convertedCollection);
        return convertedCollection;
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
