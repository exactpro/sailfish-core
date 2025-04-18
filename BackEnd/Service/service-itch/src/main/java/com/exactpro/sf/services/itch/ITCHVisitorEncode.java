/*
 * Copyright 2009-2024 Exactpro (Exactpro Systems Limited)
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
 */
package com.exactpro.sf.services.itch;

import static com.exactpro.sf.common.messages.structures.StructureUtils.getAttributeValue;

import java.math.BigDecimal;
import java.nio.ByteOrder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MessageStructureReader;
import com.exactpro.sf.common.messages.MessageStructureReaderHandlerImpl;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.util.DateTimeUtility;

public class ITCHVisitorEncode extends ITCHVisitorBase {

	private static final Logger logger = LoggerFactory.getLogger(ITCHVisitorEncode.class);
    public static final int INT_MASK = 0x80000000;
    public static final long LONG_MASK = 0x8000000000000000L;
	private final IoBuffer buffer;
	private final ByteOrder byteOrder;

    private static final Byte DEFAULT_BYTE = 0x0;

    public ITCHVisitorEncode(IoBuffer buffer, ByteOrder byteOrder) {
		this.buffer = buffer;
		this.byteOrder = byteOrder;
		buffer.setAutoExpand(true);
		buffer.order(byteOrder);
	}

	@Override
	public void visit(String fieldName, Integer value, IFieldStructure fldStruct, boolean isDefault) {

        ProtocolType type = ProtocolType.getEnum(getAttributeValue(fldStruct, TYPE_ATTRIBUTE));
        Integer length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);

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
        ProtocolType type = ProtocolType.getEnum(getAttributeValue(fldStruct, TYPE_ATTRIBUTE));
        Integer length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);

		if (type != ProtocolType.STUB && value == null) {
		    writeDefaultValue(length, fieldName);
		    return;
        }

		if (type == ProtocolType.UINT32) {
			buffer.putInt(value.intValue());
		} else if (type == ProtocolType.UINT64) {
			buffer.putLong(value);
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
        ProtocolType type = ProtocolType.getEnum(getAttributeValue(fldStruct, TYPE_ATTRIBUTE));
        Integer length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);

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
        ProtocolType type = ProtocolType.getEnum(getAttributeValue(fldStruct, TYPE_ATTRIBUTE));

		if (value == null) {
            value = DEFAULT_BYTE;
        }

		if (type == ProtocolType.BYTE) {
			buffer.put(value);
		} else if (type == ProtocolType.INT8) {
			buffer.put(value);
		} else {
			throw new EPSCommonException("Incorrect type = " + type + " for " + fieldName + " field");
		}
	}

	@Override
	public void visit(String fieldName, String value, IFieldStructure fldStruct, boolean isDefault) {
        ProtocolType type = ProtocolType.getEnum(getAttributeValue(fldStruct, TYPE_ATTRIBUTE));
        Integer length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);

		if (type != ProtocolType.STUB && value == null) {
            writeDefaultValue(length, fieldName);
            return;
        }

		if (type == ProtocolType.ALPHA || type == ProtocolType.TIME || type == ProtocolType.DATE || type == ProtocolType.DATE_TIME) {
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
        ProtocolType type = ProtocolType.getEnum(getAttributeValue(fldStruct, TYPE_ATTRIBUTE));
        Integer length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);

		if (value == null) {
		    writeDefaultValue(length, fieldName);
		    return;
        }

		if (type == ProtocolType.PRICE) {
            int intVal = convertSignedFloatToInt(value, 10_000);
            buffer.putInt(intVal);
		} else {
			throw new EPSCommonException("Incorrect type = " + type + " for " + fieldName + " field");
		}
	}

    @Override
	public void visit(String fieldName, Double value, IFieldStructure fldStruct, boolean isDefault) {
        ProtocolType type = ProtocolType.getEnum(getAttributeValue(fldStruct, TYPE_ATTRIBUTE));
        Integer length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);

		if (value == null) {
            writeDefaultValue(length, fieldName);
		    return;
        }
        if (type == ProtocolType.PRICE && length == 4) {
            buffer.putInt(convertSignedDoubleToInt(value, PRICE4_DEVIDER));
        } else if (type == ProtocolType.PRICE || type == ProtocolType.SIZE) {
			buffer.putLong(convertSignedDoubleToLong(value, PRICE_DEVIDER));
		} else if (type == ProtocolType.PRICE4 || type == ProtocolType.SIZE4) {
            buffer.putLong(convertSignedDoubleToLong(value, PRICE4_DEVIDER));
		} else if (type == ProtocolType.UINT16) {
			double val = value;
            Integer impliedDecimals = getAttributeValue(fldStruct, IMPILED_DECIMALS_ATTRIBUTE);
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
        ProtocolType type = ProtocolType.getEnum(getAttributeValue(fldStruct, TYPE_ATTRIBUTE));
        Integer length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);

		tryToFillDefaultBytes(type, value, fieldName, length);

        if (type == ProtocolType.STUB) {
            buffer.put(new byte[length]);
        } else if (type == ProtocolType.DATE_TIME) {
            String dateTimePattern = getAttributeValue(fldStruct, DATE_TIME_FORMAT);
            DateTimeFormatter dateTimeFormatter = DateTimeUtility.createFormatter(dateTimePattern);
            ZonedDateTime zonedDateTime = DateTimeUtility.toZonedDateTime(value);
            String dateTimeStr = zonedDateTime.format(dateTimeFormatter);
            byte[] dateTimeBytes = dateTimeStr.getBytes();
            checkLength(dateTimeStr, dateTimeBytes.length, length);
            buffer.put(dateTimeBytes);
        } else {
            throw new EPSCommonException("Incorrect type = " + type + " for " + fieldName + " field");
        }
    }

    @Override
    public void visit(String fieldName, LocalDate value, IFieldStructure fldStruct, boolean isDefault) {
        ProtocolType type = ProtocolType.getEnum(getAttributeValue(fldStruct, TYPE_ATTRIBUTE));
        Integer length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);

		tryToFillDefaultBytes(type, value, fieldName, length);

        if (type == ProtocolType.DAYS) {
            buffer.putShort((short)(DateTimeUtility.getMillisecond(value) / 86_400_000L));
        } else if (type == ProtocolType.DATE) {
            String dateTimePattern = getAttributeValue(fldStruct, DATE_TIME_FORMAT);
            DateTimeFormatter dateTimeFormatter = DateTimeUtility.createFormatter(dateTimePattern);
            ZonedDateTime zonedDateTime = DateTimeUtility.toZonedDateTime(value);
            String dateStr = zonedDateTime.format(dateTimeFormatter);
            byte[] dateBytes = dateStr.getBytes();
            checkLength(dateStr, dateBytes.length, length);
            buffer.put(dateBytes);
        } else if (type == ProtocolType.UINT32) {
            String formatted = value == null ? null : value.format(DATE_AS_INT);
            buffer.putUnsignedInt(formatted == null ? 0 : Long.parseLong(formatted));
        } else {
            throw new EPSCommonException("Incorrect type = " + type + " for " + fieldName + " field");
        }
    }

    @Override
    public void visit(String fieldName, LocalTime value, IFieldStructure fldStruct, boolean isDefault) {
        ProtocolType type = ProtocolType.getEnum(getAttributeValue(fldStruct, TYPE_ATTRIBUTE));
        Integer length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);

		tryToFillDefaultBytes(type, value, fieldName, length);

        if (type == ProtocolType.TIME) {
            String dateTimePattern = getAttributeValue(fldStruct, DATE_TIME_FORMAT);
            DateTimeFormatter dateTimeFormatter = DateTimeUtility.createFormatter(dateTimePattern);
            ZonedDateTime zonedDateTime = DateTimeUtility.toZonedDateTime(value);
            String timeStr = zonedDateTime.format(dateTimeFormatter);
            byte[] timeBytes = timeStr.getBytes();
            checkLength(timeStr, timeBytes.length, length);
            buffer.put(timeBytes);
        } else {
            throw new EPSCommonException("Incorrect type = " + type + " for " + fieldName + " field");
        }
    }

	@Override
	public void visit(String fieldName, BigDecimal value, IFieldStructure fldStruct, boolean isDefault) {
        ProtocolType type = ProtocolType.getEnum(getAttributeValue(fldStruct, TYPE_ATTRIBUTE));
        Integer impliedDecimals = getAttributeValue(fldStruct, IMPILED_DECIMALS_ATTRIBUTE);
        Integer length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);

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
        } else if (type == ProtocolType.PRICE && length == 4) {
			buffer.putInt(convertSignedDoubleToInt(value.doubleValue(), PRICE4_DEVIDER));
		} else if (type == ProtocolType.PRICE || type == ProtocolType.SIZE) {
			buffer.putLong(convertSignedDoubleToLong(value.doubleValue(), PRICE_DEVIDER));
		} else if (type == ProtocolType.PRICE4 || type == ProtocolType.SIZE4) {
			buffer.putLong(convertSignedDoubleToLong(value.doubleValue(), PRICE4_DEVIDER));
		} else if (type == ProtocolType.UDT) {
			byte[] longArray = new byte[length];
			byte[] data = value.toBigInteger().multiply(BI_UDT_MULTIPLIER).toByteArray();

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

		for (IMessage msg : message) {
            MessageStructureReader.READER.traverse(visitorEncode, fldType.getFields(), msg, MessageStructureReaderHandlerImpl.instance());
		}
	}


	@Override
	public void visit(String fieldName, IMessage message, IFieldStructure fldType, boolean isDefault) {
		if (message == null) {
			throw new NullPointerException("Message is null. Field name = " + fieldName);
		}

		logger.trace("Encode - field: {}, from: List<IMessage>, value: {}", fieldName, message);

        ITCHVisitorEncode visitorEncode = new ITCHVisitorEncode(buffer, byteOrder);
        MessageStructureReader.READER.traverse(visitorEncode, fldType.getFields(), message, MessageStructureReaderHandlerImpl.instance());
	}

    private static long convertSignedDoubleToLong(double value, int multiplier) {
        double dbVal = value < 0 ? -value : value;
        long val = (long)(dbVal * multiplier);
        return value < 0 ? val | LONG_MASK : val;
    }

    private static int convertSignedDoubleToInt(double value, int multiplier) {
        double dbVal = value < 0 ? -value : value;
        int val = (int)(dbVal * multiplier);
        return value < 0 ? val | INT_MASK : val;
    }

    private static int convertSignedFloatToInt(float value, int multiplier) {
        float flVal = value < 0 ? -value : value;
        int val = (int)(flVal * multiplier);
        return value < 0 ? val | INT_MASK : val;
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
    
    private void checkLength(String str, int actualLength, int expectedLength) {
        if (actualLength != expectedLength) {
            throw new EPSCommonException("The length of the encoded value exceeds the length specified in the dictionary."
                    + " Encoded value: \"" + str + "\". Length in dictionary: \"" + expectedLength + "\".");
        }
    }
}