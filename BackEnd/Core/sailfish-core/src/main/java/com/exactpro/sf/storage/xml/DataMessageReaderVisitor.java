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
import java.util.LinkedList;
import java.util.List;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.exactpro.sf.common.impl.messages.MapMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageStructureVisitor;
import com.exactpro.sf.common.messages.MessageStructureWriter;
import com.exactpro.sf.common.messages.structures.IFieldStructure;

class DataMessageReaderVisitor implements IMessageStructureVisitor {

	private DataMessage dataMessage;
	private IMessage mapMessage;
	private static MessageStructureWriter wtraverser = new MessageStructureWriter();

	public DataMessageReaderVisitor(DataMessage dataMessage, IMessage mapMessage) {
		this.dataMessage = dataMessage;
		this.mapMessage = mapMessage;
	}

	@Override
	public void visit(String fieldName, BigDecimal value,
			IFieldStructure fldStruct, boolean isDefault) {
		String tmp = getDataFieldValue(fieldName);
		if (tmp != null) {
			mapMessage.addField(fieldName, TypeConverter.getBigDecimal(tmp));
		}
	}

	@Override
	public void visitBigDecimalCollection(String fieldName, List<BigDecimal> value,
			IFieldStructure fldStruct, boolean isDefault) {
		String[] tmp = getDataFieldArrayValue(fieldName);
		if (tmp != null) {
			BigDecimal[] decimals = new BigDecimal[tmp.length];
			int i = 0;
			for (String tmpvalue : tmp) {
				decimals[i++] = TypeConverter.getBigDecimal(tmpvalue);
			}
			mapMessage.addField(fieldName, decimals);
		}
	}

	@Override
	public void visit(String fieldName, Boolean value, IFieldStructure fldStruct, boolean isDefault) {
		String tmp = getDataFieldValue(fieldName);
		if (tmp != null) {
			mapMessage.addField(fieldName, TypeConverter.getBoolean(tmp));
		}
	}

	@Override
	public void visitBooleanCollection(String fieldName, List<Boolean> value,
			IFieldStructure fldStruct, boolean isDefault) {
		String[] tmp = getDataFieldArrayValue(fieldName);
		if (tmp != null) {
			boolean[] booleans = new boolean[tmp.length];
			int i = 0;
			for (String tmpvalue : tmp) {
				booleans[i++] = TypeConverter.getBoolean(tmpvalue);
			}
			mapMessage.addField(fieldName, booleans);
		}
	}

	@Override
	public void visit(String fieldName, Byte value, IFieldStructure fldStruct, boolean isDefault) {
		String tmp = getDataFieldValue(fieldName);
		if (tmp != null) {
			mapMessage.addField(fieldName, TypeConverter.getByte(tmp));
		}
	}

	@Override
	public void visitByteCollection(String fieldName, List<Byte> value, IFieldStructure fldStruct, boolean isDefault) {
		String[] tmp = getDataFieldArrayValue(fieldName);
		if (tmp != null) {
			byte[] bytes = new byte[tmp.length];
			int i = 0;
			for (String tmpvalue : tmp) {
				bytes[i++] = TypeConverter.getByte(tmpvalue);
			}
			mapMessage.addField(fieldName, bytes);
		}
	}

	@Override
	public void visitCharCollection(String fieldName, List<Character> value, IFieldStructure fldStruct, boolean isDefault) {
		String[] tmp = getDataFieldArrayValue(fieldName);
		if (tmp != null) {
			char[] chars = new char[tmp.length];
			int i = 0;
			for (String tmpvalue : tmp) {
				chars[i++] = TypeConverter.getCharacter(tmpvalue);
			}
			mapMessage.addField(fieldName, chars);
		}
	}

	@Override
	public void visit(String fieldName, Character value,
			IFieldStructure fldStruct, boolean isDefault) {
		String tmp = getDataFieldValue(fieldName);
		if (tmp != null) {
			mapMessage.addField(fieldName, TypeConverter.getCharacter(tmp));
		}
	}

	@Override
	public void visit(String fieldName, LocalDateTime value, IFieldStructure fldStruct, boolean isDefault) {
		String tmp = getDataFieldValue(fieldName);
		if (tmp != null) {
			mapMessage.addField(fieldName, TypeConverter.getLocalDateTime(tmp));
		}
	}

	@Override
	public void visitDateTimeCollection(String fieldName, List<LocalDateTime> value, IFieldStructure fldStruct, boolean isDefault) {
		String[] tmp = getDataFieldArrayValue(fieldName);
		if (tmp != null) {
            LocalDateTime[] dates = new LocalDateTime[tmp.length];
			int i = 0;
			for (String tmpvalue : tmp) {
				dates[i++] = TypeConverter.getLocalDateTime(tmpvalue);
			}
			mapMessage.addField(fieldName, dates);
		}
    }

    @Override
    public void visit(String fieldName, LocalDate value, IFieldStructure fldStruct, boolean isDefault) {
        String tmp = getDataFieldValue(fieldName);
        if (tmp != null) {
            mapMessage.addField(fieldName, TypeConverter.getLocalDate(tmp));
        }
    }

    @Override
    public void visitDateCollection(String fieldName, List<LocalDate> value, IFieldStructure fldStruct, boolean isDefault) {
        String[] tmp = getDataFieldArrayValue(fieldName);
        if (tmp != null) {
            LocalDate[] dates = new LocalDate[tmp.length];
            int i = 0;
            for (String tmpvalue : tmp) {
                dates[i++] = TypeConverter.getLocalDate(tmpvalue);
            }
            mapMessage.addField(fieldName, dates);
        }
    }

    @Override
    public void visit(String fieldName, LocalTime value, IFieldStructure fldStruct, boolean isDefault) {
        String tmp = getDataFieldValue(fieldName);
        if (tmp != null) {
            mapMessage.addField(fieldName, TypeConverter.getLocalDate(tmp));
        }
    }

    @Override
    public void visitTimeCollection(String fieldName, List<LocalTime> value, IFieldStructure fldStruct, boolean isDefault) {
        String[] tmp = getDataFieldArrayValue(fieldName);
        if (tmp != null) {
            LocalTime[] dates = new LocalTime[tmp.length];
            int i = 0;
            for (String tmpvalue : tmp) {
                dates[i++] = TypeConverter.getLocalTime(tmpvalue);
            }
            mapMessage.addField(fieldName, dates);
        }
    }

	@Override
	public void visit(String fieldName, Double value, IFieldStructure fldStruct, boolean isDefault) {
		String tmp = getDataFieldValue(fieldName);
		if (tmp != null) {
			mapMessage.addField(fieldName, TypeConverter.getDouble(tmp));
		}
	}

	@Override
	public void visitDoubleCollection(String fieldName, List<Double> value,
			IFieldStructure fldStruct, boolean isDefault) {
		String[] tmp = getDataFieldArrayValue(fieldName);
		if (tmp != null) {
			double[] doubles = new double[tmp.length];
			int i = 0;
			for (String tmpvalue : tmp) {
				doubles[i++] = TypeConverter.getDouble(tmpvalue);
			}
			mapMessage.addField(fieldName, doubles);
		}
	}

	@Override
	public void visit(String fieldName, Float value, IFieldStructure fldStruct, boolean isDefault) {
		String tmp = getDataFieldValue(fieldName);
		if (tmp != null) {
			mapMessage.addField(fieldName, TypeConverter.getFloat(tmp));
		}
	}

	@Override
	public void visitFloatCollection(String fieldName, List<Float> value, IFieldStructure fldStruct, boolean isDefault) {
		String[] tmp = getDataFieldArrayValue(fieldName);
		if (tmp != null) {
			float[] floats = new float[tmp.length];
			int i = 0;
			for (String tmpvalue : tmp) {
				floats[i++] = TypeConverter.getFloat(tmpvalue);
			}
			mapMessage.addField(fieldName, floats);
		}
	}

	@Override
	public void visit(String fieldName, IMessage message, IFieldStructure complexField, boolean isDefault) {
		DataMessage localDataMessage = (DataMessage)getDataEntityByName(fieldName);
		IMessage imessage = new MapMessage(dataMessage.getNamespace(), fieldName);
		DataMessageReaderVisitor dataMessageVisitor = new DataMessageReaderVisitor(
				localDataMessage, imessage);
		wtraverser.traverse(dataMessageVisitor, complexField.getFields());
		mapMessage.addField(fieldName, imessage);
	}

	@Override
	public void visitIntCollection(String fieldName, List<Integer> value, IFieldStructure fldStruct, boolean isDefault) {
		String[] tmp = getDataFieldArrayValue(fieldName);
		if (tmp != null) {
			int[] ints = new int[tmp.length];
			int i = 0;
			for (String tmpvalue : tmp) {
				ints[i++] = TypeConverter.getInteger(tmpvalue);
			}
			mapMessage.addField(fieldName, ints);
		}
	}

	@Override
	public void visit(String fieldName, Integer value, IFieldStructure fldStruct, boolean isDefault) {
		String tmp = getDataFieldValue(fieldName);
		if (tmp != null) {
			mapMessage.addField(fieldName, TypeConverter.getInteger(tmp));
		}
	}

	@Override
	public void visitMessageCollection(String fieldName, List<IMessage> message, IFieldStructure complexField, boolean isDefault) {
		
		DataMessageArray array = (DataMessageArray)getDataEntityByName(fieldName);
		List<IMessage> limessages = new LinkedList<IMessage>();
		for (DataMessage dmessage : array.getMessages()) {
			IMessage imessage = new MapMessage(dmessage.getNamespace(), fieldName);
			wtraverser.traverse(new DataMessageReaderVisitor(dmessage, imessage), complexField.getFields());
			limessages.add(imessage);
		}
		mapMessage.addField(fieldName, limessages);
	}

	@Override
	public void visit(String fieldName, Long value, IFieldStructure fldStruct, boolean isDefault) {
		String tmp = getDataFieldValue(fieldName);
		if (tmp != null) {
			mapMessage.addField(fieldName, TypeConverter.getLong(tmp));
		}
	}

	@Override
	public void visitLongCollection(String fieldName, List<Long> value, IFieldStructure fldStruct, boolean isDefault) {
		String[] tmp = getDataFieldArrayValue(fieldName);
		if (tmp != null) {
			long[] longes = new long[tmp.length];
			int i = 0;
			for (String tmpvalue : tmp) {
				longes[i++] = TypeConverter.getLong(tmpvalue);
			}
			mapMessage.addField(fieldName, longes);
		}
	}

	@Override
	public void visit(String fieldName, Short value, IFieldStructure fldStruct, boolean isDefault) {
		String tmp = getDataFieldValue(fieldName);
		if (tmp != null) {
			mapMessage.addField(fieldName, TypeConverter.getShort(tmp));
		}
	}

	@Override
	public void visitShortCollection(String fieldName, List<Short> value, IFieldStructure fldStruct, boolean isDefault) {
		String[] tmp = getDataFieldArrayValue(fieldName);
		if (tmp != null) {
			short[] shorts = new short[tmp.length];
			int i = 0;
			for (String tmpvalue : tmp) {
				shorts[i++] = TypeConverter.getShort(tmpvalue);
			}
			mapMessage.addField(fieldName, shorts);
		}
	}

	@Override
	public void visit(String fieldName, String value, IFieldStructure fldStruct, boolean isDefault) {
		String tmp = getDataFieldValue(fieldName);
		if (tmp != null) {
			mapMessage.addField(fieldName, tmp);
		}
	}

	@Override
	public void visitStringCollection(String fieldName, List<String> value,
			IFieldStructure fldStruct, boolean isDefault) {
		String[] tmp = getDataFieldArrayValue(fieldName);
		if (tmp != null) {
			mapMessage.addField(fieldName, tmp);
		}
	}

	
	private String getDataFieldValue(String name){
		return ((DataField)getDataEntityByName(name)).getValue();
	}
	
	private String[] getDataFieldArrayValue(String name){
		return ((DataFieldArray)getDataEntityByName(name)).getValues().toArray(new String[1]);	
	}

	private DataEntity getDataEntityByName(String name){
		for (DataEntity object : dataMessage.getFieldsAndFieldsAndMessages()) {
			if (object.getName().equals(name)) {
				return object;
			}
		}
		return null;
		
	}
	
}