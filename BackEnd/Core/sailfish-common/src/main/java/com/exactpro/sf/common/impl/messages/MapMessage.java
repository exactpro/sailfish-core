/******************************************************************************
 * Copyright 2009-2023 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.common.impl.messages;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.FieldMetaData;
import com.exactpro.sf.common.messages.IFieldInfo;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MetadataProperty;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.util.EPSCommonException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

public class MapMessage implements IMessage
{
	private static final Logger logger = LoggerFactory.getLogger(MapMessage.class );

    private final Map<String, Object> fieldsMap = new HashMap<>();
    private final Map<String, FieldMetaData> fieldsMetaData = new HashMap<>();
	private MsgMetaData msgMetaData;
	private String namespace;
	private String name;

	public MapMessage(MsgMetaData metaData) {
	    this.msgMetaData = metaData;
	    this.namespace = metaData.getMsgNamespace();
	    this.name = metaData.getMsgName();
    }

	@JsonCreator
	public MapMessage(@JsonProperty("namespace") String namespace, @JsonProperty("name") String name)
	{
		if ( namespace == null )
		{
			throw new IllegalArgumentException("[namespace] could not be null");
		}

		if ( name == null )
		{
			throw new IllegalArgumentException("[name] could not be null");
		}

		this.namespace = namespace;
		this.name = name;

		this.msgMetaData = new MsgMetaData(namespace, name);
	}


	@Override
	public boolean isFieldSet(String name)
	{
		return getField(name) != null;
	}

	@Override
	public void addField(String name, Object value)
	{
		if ( name == null )
		{
			throw new IllegalArgumentException("[name] could not be null");
		}

		fieldsMap.put(name, value);
	}

	public Map<String, Object> getFieldsMap(){
		Map<String, Object> resultFieldsMap = new HashMap<>();
		resultFieldsMap.putAll(fieldsMap);
		return resultFieldsMap;
	}

    @JsonSetter("fieldsMap")
    @SuppressWarnings("ControlFlowStatementWithoutBraces")
	public void addFieldsMap(Map<String, Object> fieldsMap){
		this.fieldsMap.putAll(fieldsMap);
        while (this.fieldsMap.values().remove(null));
	}

	public void removeAllFields(){
		fieldsMap.clear();
	}

	@SuppressWarnings("unchecked")
    @Override
	public <T> T getField(String name)
	{
		if ( name == null )
		{
			throw new IllegalArgumentException("[name] could not be null");
		}
        return (T)fieldsMap.get(name);
	}

	@Override
	public MsgMetaData getMetaData()
	{
		return msgMetaData ;
	}


	/**
	 * JSON and XML setter. Don't use it in your code
	 */
	@Deprecated
    public void setMetaData(MsgMetaData msgMetaData) {
        this.msgMetaData = msgMetaData;
    }

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public String getNamespace()
	{
		return namespace;
	}

	@Override
	public Object removeField(String name)
	{
        if(name == null)
		{
			throw new IllegalArgumentException("[name] could not be null");
		}
        return fieldsMap.remove(name);
	}

	@Override
	public MapMessage cloneMessage()
	{
        MapMessage cloned = new MapMessage(namespace, name);

        for(String fldName : fieldsMap.keySet()) {
            cloned.addField(fldName, clone(fieldsMap.get(fldName)));
		}

		cloned.msgMetaData = msgMetaData.clone();

		return cloned;
	}

	private Object clone (Object o)
	{
        if(o instanceof IMessage) {
            return ((IMessage)o).cloneMessage();
        }
		if (o instanceof List)
		{
			List<Object> list = new ArrayList<>();
			for (Object obj : (List<?>)o) {
				list.add(clone(obj));
			}
			return list;
		}
		return o;
	}

	@Override
	public boolean compare( IMessage message )
	{
		//return false;
		if ( message == null )
		{
			throw new IllegalArgumentException("[message] could not be null");
		}

		if ( !( message instanceof MapMessage ))
		{
			return false;
			//throw new IllegalArgumentException("[message] is not instance of MapMessage");
		}

		boolean equal = true;

        if(!namespace.equals(message.getNamespace()))
		{
			equal = false;

			logger.debug("Comparision failed. Message name [{}]. Namespaces mismatch: this [{}], other [{}].",
                    name, namespace, message.getNamespace());
        } else if(!name.equals(message.getName()))
		{
			equal = false;

			logger.debug("Comparision failed. Message name [{}]. Names mismatch: this [{}], other [{}].",
                    name, name, message.getName());
		}
		else
		{
            for(String fldName : getFieldNames())
			{
				if( ! message.getFieldNames().contains(fldName))
				{
					equal = false;

					logger.debug("Comparision failed. Message name [{}]. Other message does not contain field: [{}].",
                            name, fldName);

                    return equal;
				}
				else
				{
                    if((getField(fldName) instanceof MapMessage)
                            || (getField(fldName) instanceof MapMessage[])
						|| ( message.getField( fldName ) instanceof MapMessage)
						|| (message.getField( fldName ) instanceof MapMessage[]))
					{
                        if(!(((getField(fldName) instanceof MapMessage[]) &&
								( message.getField( fldName ) instanceof MapMessage[] ))
                                || ((getField(fldName) instanceof MapMessage) &&
									( message.getField( fldName ) instanceof MapMessage ))))
						{
							equal = false;

							logger.debug("Comparision failed. Message name [{}]. Field name: [{}]. Types mismatch. Class this: [{}], class other: [{}].",
                                    name, fldName,
                                    getField(fldName).getClass(),
									message.getField( fldName ).getClass());

                            return equal;
						}
						else
						{
                            if(getField(fldName) instanceof IMessage)
							{
                                equal = ((IMessage)getField(fldName))
									.compare((IMessage) message.getField( fldName ));

								if(! equal )
								{
                                    return equal;
								}
							}
							else
							{
                                IMessage[] thisArr = (IMessage[])getField(fldName);
								IMessage[] thatArr = (IMessage[]) message.getField( fldName );

								if(thisArr.length != thatArr.length)
								{
									logger.debug("Comparision failed. Message name [{}]. Field name: [{}]. Arrays lengths are not equal. this: [{}], other: [{}].",
                                            name, fldName, thisArr.length, thatArr.length);
                                    return false;
                                }

								for( int i = 0 ; i < thisArr.length; i++ )
								{
									equal = thisArr[i].compare( thatArr[i] );

									if(! equal )
									{
										logger.debug("Comparision failed. Message name [{}]. Field name: [{}]. Sub messages at index [{}] are not equal.",
                                                name, fldName, i);

										break;
									}
								}
								if(! equal )
								{
                                    return equal;
								}
							}
						}
					}
					else
					{
                        if(getFieldType(fldName) != ((MapMessage)message).getFieldType(fldName))
						{
							equal = false;

							logger.debug("Comparision failed. Message name [{}]. Field name: [{}]. FieldType mismatch. this: [{}], other: [{}].",
                                    name, fldName,
                                    getFieldType(fldName),
									((MapMessage)message).getFieldType(fldName));
                            return equal;
						}
						else
						{
                            Object valueThis = getField(fldName);
							Object valueThat = message.getField( fldName );

							if(! compareValues(fldName, valueThis, valueThat))

							//if( ! valueThis.equals( valueThat ))
							{
								equal = false;

                                logger.debug("Comparision failed. Message name [{}]. Field name: [{}]. Values mismatch. this: [{}], other: [{}].",
                                        name, fldName,
                                        valueThis,
                                        valueThat);

                                return equal;
							}
						}
					}
				}
			}
		}
		return equal;
	}

	private boolean compareValues(String fieldname, Object valueThis, Object valueThat)
	{

        Class<?> clazzThis = valueThis.getClass();
		Class<?> clazzThat = valueThat.getClass();

		if(clazzThis == clazzThat)
		{
			Object castedThis = clazzThis.cast(valueThis);
			Object castedThat = clazzThis.cast(valueThat);

            if(clazzThis == BigDecimal.class)
			{
				BigDecimal bigDecimalThis = (BigDecimal)castedThis;
				BigDecimal bigDecimalThat = (BigDecimal)castedThat;
				BigDecimal subtract = bigDecimalThis.subtract(bigDecimalThat);

				if(subtract.doubleValue() == 0.0 )
				{
                    return true;
				}
			}
			else if(castedThis.equals( castedThat))
			{
                return true;
			}
		}
		else
		{
            logger.debug("Comparision failed. Message name [{}]. Field name: [{}]. Types mismatch. this: [{}], other: [{}].",
                    name, fieldname,
                    clazzThis,
                    clazzThat);

		}
        return false;
	}

	@Override
	@JsonIgnore
	public Set<String> getFieldNames()
	{
		return fieldsMap.keySet();
	}

    @Override
    @JsonIgnore
    public int getFieldCount() {
        return fieldsMap.size();
    }

	public JavaType getFieldType(String name)
	{
        if(name == null)
		{
			throw new IllegalArgumentException("[name] could not be null");
		}

        if(fieldsMap.containsKey(name)) {
            Object field = fieldsMap.get(name);
            return JavaType.fromValue(field.getClass().getCanonicalName());
		}
		else
		{
			throw new EPSCommonException(
					String.format( "Field [%s] is not defined in the message. " +
							"Details: namespace [%s], message name [%s].",
                            name, namespace, this.name));
        }
    }

	@Override
    public String toString() {
        StringBuilder toString = new StringBuilder(1024);

        for(String fldName : getFieldNames()) {
            if(toString.length() > 0) {
                toString.append('|');
            }

            Object value = getField(fldName);

            if(value instanceof IMessage) {
                toString.append(value);
            } else {
                Object field = getField(fldName);

                toString.append(fldName);
                toString.append('=');

                if (field instanceof BigDecimal) {
                    toString.append(((BigDecimal)field).toPlainString());
                } else {
                    toString.append(field);
                }
            }
        }

        if (toString.length() > 0) {
            toString.append('|');
        }
        byte[] rawMessage = msgMetaData.getRawMessage();
        toString.append(MetadataProperty.RAW_MESSAGE.getPropertyName())
                .append('=')
                .append(rawMessage == null ? "" : Hex.encodeHexString(rawMessage));
        return toString.toString();
    }


	@Override
	public IFieldInfo getFieldInfo(String name)
	{
        return fieldsMap.containsKey(name) ? new MapFieldInfo(name, fieldsMap.get(name)) : null;
    }

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public void setName(String name) {
		this.name = name;
	}


	private class MapFieldInfo implements IFieldInfo
	{
        private final String fldName;
        private final Object value;


		private MapFieldInfo(String name, Object value)
		{
			this.fldName = name;
			this.value = value;
		}

		@Override
		public FieldType getFieldType()
		{
			return convert(value);
		}


		@Override
		public String getName()
		{
            return fldName;
		}


		@Override
		public Object getValue() {
			return value;
		}


		@Override
		public boolean isCollection()
		{
			if ( value instanceof List<?> )
			{
				return true;
			}
			else if ( value instanceof boolean[] ||
						value instanceof short[] ||
						value instanceof int[] ||
						value instanceof long[] ||
						value instanceof byte[] ||
						value instanceof float[] ||
						value instanceof double[] ||
						value instanceof String[] ||
                        value instanceof LocalDateTime[] ||
                        value instanceof LocalDate[] ||
                        value instanceof LocalTime[] ||
						value instanceof char[] ||
						value instanceof BigDecimal[] )
			{
				return true;
			}

			return false;
		}


		private FieldType convert(Object value)
		{
            if(value == null) {
                throw new NullPointerException("value");
            }

            if(value instanceof IMessage) {
                return FieldType.SUBMESSAGE;
            }

            if(value instanceof Boolean || value instanceof boolean[]) {
                return FieldType.BOOLEAN;
            }

            if(value instanceof Short || value instanceof short[]) {
                return FieldType.SHORT;
            }

            if(value instanceof Integer || value instanceof int[]) {
                return FieldType.INT;
            }

            if(value instanceof Long || value instanceof long[]) {
                return FieldType.LONG;
            }

            if(value instanceof Byte || value instanceof byte[]) {
                return FieldType.BYTE;
            }

            if(value instanceof Float || value instanceof float[]) {
                return FieldType.FLOAT;
            }

            if(value instanceof Double || value instanceof double[]) {
                return FieldType.DOUBLE;
            }

            if(value instanceof String || value instanceof String[]) {
                return FieldType.STRING;
            }

            if(value instanceof LocalDateTime || value instanceof LocalDateTime[]) {
                return FieldType.DATE_TIME;
            }

            if(value instanceof LocalDate || value instanceof LocalDate[]) {
                return FieldType.DATE;
            }

            if(value instanceof LocalTime || value instanceof LocalTime[]) {
                return FieldType.TIME;
            }

            if(value instanceof Character || value instanceof char[]) {
                return FieldType.CHAR;
            }

            if(value instanceof BigDecimal || value instanceof BigDecimal[]) {
                return FieldType.DECIMAL;
            }

			throw new EPSCommonException("Cannot associate  [" + value.getClass().getCanonicalName() + "] with FieldType" );
		}

	}



	@Override
	public FieldMetaData getFieldMetaData(String name) {
		if (!fieldsMetaData.containsKey(name)) {
			fieldsMetaData.put(name, new FieldMetaData());
		}
		return fieldsMetaData.get(name);
	}

	@Override
	public boolean equals(Object obj) {
	    if(obj == this) {
            return true;
        }

        if(!(obj instanceof MapMessage)) {
            return false;
        }

        MapMessage that = (MapMessage)obj;
        EqualsBuilder builder = new EqualsBuilder();

        builder.append(name, that.name);
        builder.append(namespace, that.namespace);
        builder.append(msgMetaData, that.msgMetaData);
        builder.append(fieldsMap, that.fieldsMap);
        builder.append(fieldsMetaData, that.fieldsMetaData);

        return builder.isEquals();
	}

	@Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.append(name);
        builder.append(namespace);
        builder.append(msgMetaData);
        builder.append(fieldsMap);
        builder.append(fieldsMetaData);

        return builder.toHashCode();
    }
}
