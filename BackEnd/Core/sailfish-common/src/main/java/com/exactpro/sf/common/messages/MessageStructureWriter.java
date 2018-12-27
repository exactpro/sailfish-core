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

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.EPSCommonException;

public class MessageStructureWriter 
{
	
	public MessageStructureWriter()
	{
	}
	
	
	public void traverse(IMessageStructureVisitor msgStrVisitor, 
						IMessageStructure msgStructure)
	{
		try
		{
			this.traverse(msgStrVisitor, msgStructure.getFields());
		}
		catch ( Exception e )
		{
			throw new EPSCommonException(e.getMessage() + ". in MessageStructure Name = [" + 
					msgStructure.getName() + "]", e); 
		}
	}
	
	public void traverse(IMessageStructureVisitor msgStrVisitor, 
			List<IFieldStructure> fields)
	{
		
		for ( IFieldStructure curField : fields )
		{
			
		    String fieldName = curField.getName();
			
			try {
				if ( !curField.isComplex() )
				{
					JavaType javaType = curField.getJavaType(); 
					
					switch ( javaType )
					{
						case JAVA_LANG_BOOLEAN :
							if ( !curField.isCollection() )
								msgStrVisitor.visit(fieldName, (Boolean)null, curField, false);
							else
								msgStrVisitor.visitBooleanCollection(fieldName, null, curField, false);
							break;
						case JAVA_LANG_SHORT :
							if ( !curField.isCollection() )
								msgStrVisitor.visit(fieldName, (Short) null, curField, false);
							else
								msgStrVisitor.visitShortCollection(fieldName, null, curField, false);
							break;
						case JAVA_LANG_INTEGER :
							if ( !curField.isCollection() )
								msgStrVisitor.visit(fieldName, (Integer)null, curField, false);
							else
								msgStrVisitor.visitIntCollection(fieldName, null, curField, false);
							break;
						case JAVA_LANG_LONG :
							if ( !curField.isCollection() )
								msgStrVisitor.visit(fieldName, (Long) null, curField, false);
							else
								msgStrVisitor.visitLongCollection(fieldName, null, curField, false);
							break;
						case JAVA_LANG_BYTE :
							if ( !curField.isCollection() )
								msgStrVisitor.visit(curField.getName(), (Byte) null, curField, false);
							else
								msgStrVisitor.visitByteCollection(fieldName, null, curField, false);
							break;
						case JAVA_LANG_FLOAT :
							if ( !curField.isCollection() )
								msgStrVisitor.visit(fieldName, (Float) null, curField, false);
							else
								msgStrVisitor.visitFloatCollection(fieldName, null, curField, false);
							break;
						case JAVA_LANG_DOUBLE :
							if ( !curField.isCollection() )
								msgStrVisitor.visit(fieldName, (Double) null, curField, false);
							else
								msgStrVisitor.visitDoubleCollection(fieldName, null, curField, false);
							break;
						case JAVA_LANG_STRING :
							if ( !curField.isCollection() )
								msgStrVisitor.visit(fieldName, (String) null, curField, false);
							else
								msgStrVisitor.visitStringCollection(fieldName, null, curField, false);
							break;
						case JAVA_TIME_LOCAL_DATE_TIME :
							if ( !curField.isCollection() )
								msgStrVisitor.visit(fieldName, (LocalDateTime) null, curField, false);
							else
								msgStrVisitor.visitDateTimeCollection(fieldName, null, curField, false);
							break;
                        case JAVA_TIME_LOCAL_DATE :
                            if ( !curField.isCollection() )
                                msgStrVisitor.visit(fieldName, (LocalDate) null, curField, false);
                            else
                                msgStrVisitor.visitDateCollection(fieldName, null, curField, false);
                            break;
                        case JAVA_TIME_LOCAL_TIME :
                            if ( !curField.isCollection() )
                                msgStrVisitor.visit(fieldName, (LocalTime) null, curField, false);
                            else
                                msgStrVisitor.visitTimeCollection(fieldName, null, curField, false);
                            break;
						case JAVA_LANG_CHARACTER:
							if ( !curField.isCollection() )
								msgStrVisitor.visit(fieldName, (Character) null, curField, false);
							else
								msgStrVisitor.visitCharCollection(fieldName, null, curField, false);
							break;
						case JAVA_MATH_BIG_DECIMAL:
							if ( !curField.isCollection() )
								msgStrVisitor.visit(fieldName, (BigDecimal) null, curField, false);
							else
								msgStrVisitor.visitBigDecimalCollection(fieldName, null, curField, false);
							break;
						default:
							throw new EPSCommonException("Unknown FieldType = [" + 
									javaType + "] for FieldName = [" + 
									curField.getName() + "]" );
					}
				} else {
					if (!curField.isCollection()) {
						msgStrVisitor.visit(fieldName, (IMessage) null, curField, false);
					} else {
						msgStrVisitor.visitMessageCollection(fieldName, (List<IMessage>) null, curField, false);
					}
				}
			} catch (RuntimeException e) {
                StringBuilder builder;
                if(e.getMessage() != null) {
                    builder = new StringBuilder(e.getMessage());
                } else {
                    builder = new StringBuilder();
                }
				builder.append(". in field name = [").append(fieldName).append("]");
				throw new EPSCommonException(builder.toString(), e);
			}
						
		}
		
	}
}
