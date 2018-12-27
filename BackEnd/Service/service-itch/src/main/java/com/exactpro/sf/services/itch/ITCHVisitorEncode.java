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
package com.exactpro.sf.services.itch;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MessageStructureReader;
import com.exactpro.sf.common.messages.MessageStructureReaderHandlerImpl;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.util.DateTimeUtility;
import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.List;

public class ITCHVisitorEncode extends ITCHVisitorBase {

    private static final Logger logger = LoggerFactory.getLogger(ITCHVisitorEncode.class);
	private final IoBuffer buffer;
	private final ByteOrder byteOrder;

	private final static Byte DEFAULT_BYTE = 0x0;

    public ITCHVisitorEncode(IoBuffer buffer, ByteOrder byteOrder) {
		this.buffer = buffer;
		this.byteOrder = byteOrder;
		buffer.setAutoExpand(true);
		buffer.order(byteOrder);
	}

	@Override
	public void visit(String fieldName, Integer value, IFieldStructure fldStruct, boolean isDefault) {

		ProtocolType type = ProtocolType.getEnum((String) fldStruct.getAttributeValueByName(TYPE_ATTRIBUTE));
		Integer length = (Integer) fldStruct.getAttributeValueByName(LENGTH_ATTRIBUTE);

		if (type != ProtocolType.STUB && value == null) {
		    writeDefaultValue(length, fieldName);
		    return;
		}

		if (type == ProtocolType.UINT16) {
			buffer.putShort(value.shortValue());
		} else if (type == ProtocolType.INT8) {
			buffer.put(value.byteValue());
		} else if (type == ProtocolType.INT16) {
			buffer.putShort(value.shortValue());
		} else if (type == ProtocolType.INT32) {
			buffer.putInt(value);
		} else if (type == ProtocolType.STUB) {
			buffer.put(new byte[length]);
		} else {
			throw new EPSCommonException("Incorrect type = " + type + " for " + fieldName + " field");
		}
	}

	@Override
	public void visit(String fieldName, Long value, IFieldStructure fldStruct, boolean isDefault) {
		ProtocolType type = ProtocolType.getEnum((String) fldStruct.getAttributeValueByName(TYPE_ATTRIBUTE));
		Integer length = (Integer) fldStruct.getAttributeValueByName(LENGTH_ATTRIBUTE);

		if (type != ProtocolType.STUB && value == null) {
		    writeDefaultValue(length, fieldName);
		    return;
        }

		if (type == ProtocolType.UINT32) {
			buffer.putInt(value.intValue());
		} else if (type == ProtocolType.UINT64) {
			buffer.putLong(value.longValue());
		} else if (type == ProtocolType.INT16) {
			buffer.putShort(value.shortValue());
		} else if (type == ProtocolType.INT32) {
			buffer.putInt(value.intValue());
		} else if (type == ProtocolType.INT64) {
			buffer.putLong(value);
		} else if (type == ProtocolType.STUB) {
			buffer.put(new byte[length]);
		} else {
			throw new EPSCommonException("Incorrect type = " + type + " for " + fieldName + " field");
		}
	}

	@Override
	public void visit(String fieldName, Short value, IFieldStructure fldStruct, boolean isDefault) {
		ProtocolType type = ProtocolType.getEnum((String) fldStruct.getAttributeValueByName(TYPE_ATTRIBUTE));
		Integer length = (Integer) fldStruct.getAttributeValueByName(LENGTH_ATTRIBUTE);

		if (type != ProtocolType.STUB && value == null) {
            writeDefaultValue(length, fieldName);
            return;
        }

		if (type == ProtocolType.UINT8) {
			buffer.putUnsigned(value);
		} else if (type == ProtocolType.BYTE) {
			buffer.put(value.byteValue());
		} else if (type == ProtocolType.INT8) {
			buffer.put(value.byteValue());
		} else if (type == ProtocolType.INT16) {
			buffer.putShort(value);
		} else if (type == ProtocolType.STUB) {
			buffer.put(new byte[length]);
		} else {
			throw new EPSCommonException("Incorrect type = " + type + " for " + fieldName + " field");
		}
	}

	@Override
	public void visit(String fieldName, Byte value, IFieldStructure fldStruct, boolean isDefault) {
		ProtocolType type = ProtocolType.getEnum((String) fldStruct.getAttributeValueByName(TYPE_ATTRIBUTE));

		if (value == null) {
            value = DEFAULT_BYTE;
        }

		if (type == ProtocolType.BYTE) {
			buffer.put(value.byteValue());
		} else if (type == ProtocolType.INT8) {
			buffer.put(value);
		} else {
			throw new EPSCommonException("Incorrect type = " + type + " for " + fieldName + " field");
		}
	}

	@Override
	public void visit(String fieldName, String value, IFieldStructure fldStruct, boolean isDefault) {
		ProtocolType type = ProtocolType.getEnum((String) fldStruct.getAttributeValueByName(TYPE_ATTRIBUTE));
		Integer length = (Integer) fldStruct.getAttributeValueByName(LENGTH_ATTRIBUTE);

		if (type != ProtocolType.STUB && value == null) {
            writeDefaultValue(length, fieldName);
            return;
        }

		if (type == ProtocolType.ALPHA || type == ProtocolType.TIME || type == ProtocolType.DATE) {
			byte[] array = new byte[length];

			if (!encodeString(value, array)) {
				throw new EPSCommonException("The length of value = [" + value
						+ "] is greater than Length for fieldName [" + fieldName + "]");
			}

			buffer.put(array);
		} else if (type == ProtocolType.STUB) {
			buffer.put(new byte[length]);
		} else {
			throw new EPSCommonException("Incorrect type = " + type + " for " + fieldName + " field");
		}
	}

	@Override
	public void visit(String fieldName, Float value, IFieldStructure fldStruct, boolean isDefault) {
		ProtocolType type = ProtocolType.getEnum((String) fldStruct.getAttributeValueByName(TYPE_ATTRIBUTE));
		Integer length = (Integer) fldStruct.getAttributeValueByName(LENGTH_ATTRIBUTE);

		if (value == null) {
		    writeDefaultValue(length, fieldName);
		    return;
        }

		if (type == ProtocolType.PRICE) {
			int val = (int) (value.floatValue() * 10000);
			buffer.putInt(val);
		} else {
			throw new EPSCommonException("Incorrect type = " + type + " for " + fieldName + " field");
		}
	}

	@Override
	public void visit(String fieldName, Double value, IFieldStructure fldStruct, boolean isDefault) {
		ProtocolType type = ProtocolType.getEnum((String) fldStruct.getAttributeValueByName(TYPE_ATTRIBUTE));
		Integer length = (Integer) fldStruct.getAttributeValueByName(LENGTH_ATTRIBUTE);

		if (value == null) {
            writeDefaultValue(length, fieldName);
		    return;
        }

		if (type == ProtocolType.PRICE || type == ProtocolType.SIZE) {
			long val = (long) (value.doubleValue() * 100000000);
			buffer.putLong(val);
		} else if (type == ProtocolType.PRICE4 || type == ProtocolType.SIZE4) {
			long val = (long) (value.doubleValue() * 10000);
			buffer.putLong(val);
		} else if (type == ProtocolType.UINT16) {
			double val = value.doubleValue();
			Integer impliedDecimals = (Integer) fldStruct.getAttributeValueByName(IMPILED_DECIMALS_ATTRIBUTE);
			if (impliedDecimals != null) {
				for (int i = 0; i < impliedDecimals; i++) {
					val*=10;
				}
			}
			buffer.putLong((long)val);
		} else {
			throw new EPSCommonException("Incorrect type = " + type + " for " + fieldName + " field");
		}
	}

	@Override
    public void visit(String fieldName, LocalDateTime value, IFieldStructure fldStruct, boolean isDefault) {
        ProtocolType type = ProtocolType.getEnum((String) fldStruct.getAttributeValueByName(TYPE_ATTRIBUTE));
        Integer length = (Integer) fldStruct.getAttributeValueByName(LENGTH_ATTRIBUTE);
        
		tryToFillDefaultBytes(type, value, fieldName, length);

        if (type == ProtocolType.STUB) {
            buffer.put(new byte[length]);
        } else {
            throw new EPSCommonException("Incorrect type = " + type + " for " + fieldName + " field");
        }
    }

    @Override
    public void visit(String fieldName, LocalDate value, IFieldStructure fldStruct, boolean isDefault) {
        ProtocolType type = ProtocolType.getEnum((String) fldStruct.getAttributeValueByName(TYPE_ATTRIBUTE));
		Integer length = (Integer) fldStruct.getAttributeValueByName(LENGTH_ATTRIBUTE);

		tryToFillDefaultBytes(type, value, fieldName, length);

        if (type == ProtocolType.DAYS) {
            buffer.putShort((short) (DateTimeUtility.getMillisecond(value) / 86400_000L));
        } else if (type == ProtocolType.DATE) {
            String temp = value.format(dateFormatter);
        	buffer.put(temp.getBytes());
        } else {
            throw new EPSCommonException("Incorrect type = " + type + " for " + fieldName + " field");
        }
    }

    @Override
    public void visit(String fieldName, LocalTime value, IFieldStructure fldStruct, boolean isDefault) {
        ProtocolType type = ProtocolType.getEnum((String) fldStruct.getAttributeValueByName(TYPE_ATTRIBUTE));
		Integer length = (Integer) fldStruct.getAttributeValueByName(LENGTH_ATTRIBUTE);

		tryToFillDefaultBytes(type, value, fieldName, length);

        if (type == ProtocolType.TIME) {
            String temp = value.format(timeFormatter);
        	buffer.put(temp.getBytes());        	
        } else {
            throw new EPSCommonException("Incorrect type = " + type + " for " + fieldName + " field");
        }
    }
	
	@Override
	public void visit(String fieldName, BigDecimal value, IFieldStructure fldStruct, boolean isDefault) {
		ProtocolType type = ProtocolType.getEnum((String) fldStruct.getAttributeValueByName(TYPE_ATTRIBUTE));
		Integer impliedDecimals = (Integer) fldStruct.getAttributeValueByName(IMPILED_DECIMALS_ATTRIBUTE);
		Integer length = (Integer) fldStruct.getAttributeValueByName(LENGTH_ATTRIBUTE);

		if (value == null) {
		    writeDefaultValue(length, fieldName);
		    return;
		}

		if (type == ProtocolType.UINT64) {
			// buffer.put(value.toBigInteger().toByteArray());
			long val = value.longValue();
			if (impliedDecimals != null) {
				for (int i = 0; i < impliedDecimals; i++) {
					val*=10;
				}
			}
			buffer.putLong(val);
		} else if (type == ProtocolType.PRICE || type == ProtocolType.SIZE) {
			long val = (long) (value.doubleValue() * 100000000);
			buffer.putLong(val);
		} else if (type == ProtocolType.UDT) {
			byte[] longArray = new byte[length];
			byte[] data = value.toBigInteger().multiply(new BigInteger("1000000000")).toByteArray();

			int l = Math.min(length, data.length);
			// copy (& reverse)
			for (int i = 0; i < l; i++) {
				longArray[i] = (byteOrder == ByteOrder.LITTLE_ENDIAN) ? data[data.length - i - 1] : data[i];
			}
			buffer.put(longArray);
		} else  if (type == ProtocolType.INT32 || type == ProtocolType.UINT32) {
			int val = value.intValue();
			if (impliedDecimals != null) {
				for (int i = 0; i < impliedDecimals; i++) {
					val*=10;
				}
			}
			buffer.putInt(val);
		} else {
			throw new EPSCommonException("Incorrect type = " + type + " for " + fieldName + " field");
		}
	}

	@Override
	public void visitMessageCollection(String fieldName, List<IMessage> message, IFieldStructure fldType, boolean isDefault) {
		if (message == null) {
			throw new NullPointerException("Message is null. Field name = " + fieldName);
		}

		logger.trace("Encode - field: {}, from: List<IMessage>, value: {}", fieldName, message);

        ITCHVisitorEncode visitorEncode = new ITCHVisitorEncode(buffer, byteOrder);
		MessageStructureReader messageStuctureReader = new MessageStructureReader();

		for (IMessage msg : message) {
			messageStuctureReader.traverse(visitorEncode, fldType.getFields(), msg, MessageStructureReaderHandlerImpl.instance());
		}
	}
	
	
	@Override
	public void visit(String fieldName, IMessage message, IFieldStructure fldType, boolean isDefault) {
		if (message == null) {
			throw new NullPointerException("Message is null. Field name = " + fieldName);
		}
		
		logger.trace("Encode - field: {}, from: List<IMessage>, value: {}", fieldName, message);

        ITCHVisitorEncode visitorEncode = new ITCHVisitorEncode(buffer, byteOrder);
        MessageStructureReader messageStuctureReader = new MessageStructureReader();
        messageStuctureReader.traverse(visitorEncode, fldType.getFields(), message, MessageStructureReaderHandlerImpl.instance());
	}

    private void writeDefaultValue(int length, String fieldName) {
        for (int i = 0; i < length; i++) {
            buffer.put(DEFAULT_BYTE);
        }
        logger.warn("Using default filler for [{}] field", fieldName);
    }

	private void tryToFillDefaultBytes(ProtocolType type, Object value, String fieldName, int length) {

		if (type != ProtocolType.STUB && value == null) {
			byte[] array = new byte[length];

			if (!encodeString(new String(new byte[]{DEFAULT_BYTE}), array)) {
				throw new EPSCommonException("The length of value = [" + value
						+ "] is greater than Length for fieldName [" + fieldName + "]");
			}

			buffer.put(array);
		}
	}

}