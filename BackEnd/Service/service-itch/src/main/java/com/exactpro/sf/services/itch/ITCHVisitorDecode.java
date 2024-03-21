/******************************************************************************
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
 ******************************************************************************/
package com.exactpro.sf.services.itch;

import static com.exactpro.sf.common.messages.structures.StructureUtils.getAttributeValue;
import static com.exactpro.sf.services.util.ServiceUtil.divide;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MessageStructureWriter;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.services.util.ServiceUtil;
import com.exactpro.sf.util.DateTimeUtility;
import com.google.common.primitives.UnsignedLong;

public class ITCHVisitorDecode extends ITCHVisitorBase {

    private static final Logger logger = LoggerFactory.getLogger(ITCHVisitorDecode.class);
    private static final int INT_MASK = 0x7FFFFFFF;
    private static final long LONG_MASK = 0x7FFFFFFFFFFFFFFFL;
	private static final ITCHVisitorSettings DEFAULT_VISITOR_SETTINGS = new ITCHVisitorSettings();

    private final IoBuffer buffer;
	private final ByteOrder byteOrder;
	private final IMessage msg;
	private final IMessageFactory msgFactory;
	private final ITCHVisitorSettings visitorSettings;

    public static final String DEFAULT_ZONE_ID = "UTC";

    public ITCHVisitorDecode(IoBuffer buffer, ByteOrder byteOrder, IMessage msg, IMessageFactory msgFactory) {
		this(buffer, byteOrder, msg, msgFactory, DEFAULT_VISITOR_SETTINGS);
	}

    public ITCHVisitorDecode(IoBuffer buffer, ByteOrder byteOrder, IMessage msg, IMessageFactory msgFactory, ITCHVisitorSettings visitorSettings) {
		this.buffer = buffer.order(byteOrder);
		this.byteOrder = byteOrder;
		this.msg = msg;
		this.msgFactory = msgFactory;
		this.visitorSettings = (visitorSettings != null)? visitorSettings: DEFAULT_VISITOR_SETTINGS;
	}

	@Override
	public void visit(String fieldName, Integer value, IFieldStructure fldStruct, boolean isDefault) {
		int length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);
    	int pos1 = buffer.position();

		Integer val = extractInteger(fldStruct);
		if(val != null) {
			msg.addField(fieldName, val);
		}

		int pos2 = buffer.position();
		int read = pos2 - pos1;
		if (read != length) {
			throw new EPSCommonException("Read " + read + " bytes, but length = " + length + " for " + fieldName + " field");
		}
	}

	@Override
	public void visit(String fieldName, Long value, IFieldStructure fldStruct, boolean isDefault) {
        int length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);
		int pos1 = buffer.position();

		Long val = extractLong(fldStruct);
		if(val != null) {
			msg.addField(fieldName, val);
		}
		int pos2 = buffer.position();
		int read = pos2 - pos1;
		if (read != length) {
			throw new EPSCommonException("Read " + read + " bytes, but length = " + length + " for " + fieldName + " field");
		}
	}

	@Override
	public void visit(String fieldName, Short value, IFieldStructure fldStruct, boolean isDefault) {
        ProtocolType type = ProtocolType.getEnum(getAttributeValue(fldStruct, TYPE_ATTRIBUTE));
		logger.trace("Visit fieldname = [{}]; fieldType [{}]", fieldName, type);

        int length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);
		int pos1 = buffer.position();

		if (type == ProtocolType.UINT8 || type == ProtocolType.BYTE) {
			msg.addField(fieldName, buffer.getUnsigned());
		} else if (type == ProtocolType.INT8) {
			msg.addField(fieldName, (short) buffer.get());
		} else if (type == ProtocolType.INT16) {
			msg.addField(fieldName, buffer.getShort());
		} else if (type == ProtocolType.STUB) {
			buffer.skip(length);
		} else {
			throw new EPSCommonException("Incorrect type = " + type + " for " + fieldName + " field");
		}

		int pos2 = buffer.position();
		int read = pos2 - pos1;
		if (read != length) {
			throw new EPSCommonException("Read " + read + " bytes, but length = " + length + " for " + fieldName + " field");
		}
	}

	@Override
	public void visit(String fieldName, Byte value, IFieldStructure fldStruct, boolean isDefault) {
        ProtocolType type = ProtocolType.getEnum(getAttributeValue(fldStruct, TYPE_ATTRIBUTE));
		logger.trace("Visit fieldname = [{}]; fieldType [{}]", fieldName, type);

        int length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);
		int pos1 = buffer.position();

		if (type == ProtocolType.BYTE) {
			msg.addField(fieldName, buffer.getUnsigned());
		} else if (type == ProtocolType.INT8) {
			msg.addField(fieldName, buffer.get());
		} else {
			throw new EPSCommonException("Incorrect type = " + type + " for " + fieldName + " field");
		}

		int pos2 = buffer.position();
		int read = pos2 - pos1;
		if (read != length) {
			throw new EPSCommonException("Read " + read + " bytes, but length = " + length + " for " + fieldName + " field");
		}
	}

	@Override
	public void visit(String fieldName, String value, IFieldStructure fldStruct, boolean isDefault) {
		String decodedString = decodeString(fieldName,fldStruct);
		if(decodedString != null) {
			msg.addField(fieldName, decodedString);
		}
	}

	protected String decodeString(String fieldName, IFieldStructure fldStruct) {
		ProtocolType type = ProtocolType.getEnum(getAttributeValue(fldStruct, TYPE_ATTRIBUTE));
		logger.trace("Visit fieldname = [{}]; fieldType [{}]", fieldName, type);

		int length = getStringLength(fldStruct);
		int pos1 = buffer.position();
		String result = null;

		if (type == ProtocolType.ALPHA || type == ProtocolType.DATE || type == ProtocolType.TIME || type == ProtocolType.DATE_TIME) {
			// if you edit this lines, please edit ALPHA_NOTRIM too
			byte[] array = new byte[length];

			buffer.get(array); // FIXME: slice?

			try {
				String decoded = decoder.get().decode(ByteBuffer.wrap(array)).toString();
				result = (visitorSettings.isTrimLeftPaddingEnabled())? decoded.trim(): trimRight(decoded);
			} catch (CharacterCodingException e) {
				throw new EPSCommonException(e);
			}

		} else if (type == ProtocolType.ALPHA_NOTRIM) {
			byte[] array = new byte[length];
			buffer.get(array);
			try {
				result = decoder.get().decode(ByteBuffer.wrap(array)).toString();
			} catch (CharacterCodingException e) {
				throw new EPSCommonException(e);
			}
		} else if (type == ProtocolType.STUB) {
			buffer.skip(length);
		} else {
			throw new EPSCommonException("Incorrect type = " + type + " for " +  fieldName + " field");
		}

		int pos2 = buffer.position();
		int read = pos2 - pos1;
		if (read != length) {
			throw new EPSCommonException("Read " + read + " bytes, but length = " + length + " for " + fieldName + " field");
		}

		return result;
	}

	@Override
	public void visit(String fieldName, Double value, IFieldStructure fldStruct, boolean isDefault) {
        ProtocolType type = ProtocolType.getEnum(getAttributeValue(fldStruct, TYPE_ATTRIBUTE));
		logger.trace("Visit fieldname = [{}]; fieldType [{}]", fieldName, type);

        int length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);
		int pos1 = buffer.position();

		if (type == ProtocolType.SIZE || type == ProtocolType.SIZE4) {
			byte[] rawArray = new byte[length + 1];

			rawArray[length] = 0;

			buffer.get(rawArray, 0, length);

			if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
				reverseBytes(rawArray);
			}

			UnsignedLong divider = (type == ProtocolType.SIZE) ? SIZE_DEVIDER : SIZE4_DEVIDER;

			double result = ServiceUtil.convertFromUint64(rawArray, divider);

			msg.addField(fieldName, result);
        } else if (type == ProtocolType.PRICE && length == 4) {
            int val = correctIfNegative(buffer.getInt());
            msg.addField(fieldName, divide(val, 10000L));
		} else if (type == ProtocolType.PRICE || type == ProtocolType.PRICE4) {
			long val = buffer.getLong();

			boolean positive = (byte) (val >> 63) == 0;
			
			if (!positive) {
                val &= LONG_MASK;
                val *= -1L;
			}

            double valDouble = divide(val, type == ProtocolType.PRICE ? 100_000_000L : 10_000L);

            msg.addField(fieldName, valDouble);
		} else if (type == ProtocolType.UINT16) {
			BigDecimal val = new BigDecimal(buffer.getUnsignedShort());

            Integer impliedDecimals = getAttributeValue(fldStruct, IMPILED_DECIMALS_ATTRIBUTE);
			if (impliedDecimals != null) {
				for (int i = 0; i < impliedDecimals; i++) {
					val=val.divide(BigDecimal.TEN);
				}
			}
			msg.addField(fieldName, val.doubleValue());
		} else {
			throw new EPSCommonException("Incorrect type = " + type + " for " + fieldName + " field");
		}

		int pos2 = buffer.position();
		int read = pos2 - pos1;
		if (read != length) {
			throw new EPSCommonException("Read " + read + " bytes, but length = " + length + " for " + fieldName + " field");
		}
	}

    @Override
	public void visit(String fieldName, Float value, IFieldStructure fldStruct, boolean isDefault) {
        ProtocolType type = ProtocolType.getEnum(getAttributeValue(fldStruct, TYPE_ATTRIBUTE));
		logger.trace("Visit fieldname = [{}]; fieldType [{}]", fieldName, type);

        int length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);
		int pos1 = buffer.position();

		if (type == ProtocolType.PRICE) {
			int val = correctIfNegative(buffer.getInt());
			msg.addField(fieldName, (float) (val / 10000.0));
		} else {
			throw new EPSCommonException("Incorrect type = " + type + " for " + fieldName + " field");
		}

		int pos2 = buffer.position();
		int read = pos2 - pos1;
		if (read != length) {
			throw new EPSCommonException("Read " + read + " bytes, but length = " + length + " for " + fieldName + " field");
		}
	}

    @Override
	public void visit(String fieldName, BigDecimal value, IFieldStructure fldStruct, boolean isDefault) {
        ProtocolType type = ProtocolType.getEnum(getAttributeValue(fldStruct, TYPE_ATTRIBUTE));
		logger.trace("Visit fieldname = [{}]; fieldType [{}]", fieldName, type);

        Integer impliedDecimals = getAttributeValue(fldStruct, IMPILED_DECIMALS_ATTRIBUTE);
        int length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);
		int pos1 = buffer.position();

		if (type == ProtocolType.UINT64) {
			// we will add 0x00 before most significant byte (to parse this number as unsigned)
			byte[] rawArray = new byte[length + 1];

			buffer.get(rawArray, 0, length);

			if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
				rawArray[length] = 0; // set most significant byte to '0x00'
				reverseBytes(rawArray);
			} else {
				System.arraycopy(rawArray, 0, rawArray, 1, length);
				rawArray[0] = 0x00; // set most significant byte to '0x00'
			}

			BigDecimal bigDec = ServiceUtil.convertFromUint64(rawArray);

			if (impliedDecimals != null) {
				for (int i = 0; i < impliedDecimals; i++) {
					bigDec = bigDec.divide(BigDecimal.TEN);
				}
			}

			msg.addField(fieldName, bigDec);
		} else  if (type == ProtocolType.INT32 || type == ProtocolType.UINT32) {
			long val = 0;

			switch (type) {
			case INT32:
				val = buffer.getInt();
				break;
			case UINT32:
				val = buffer.getUnsignedInt();
				break;
			default:
				throw new IllegalStateException("Type is " + type);
			}

			BigDecimal bigDec = new BigDecimal(val);

			if (impliedDecimals != null) {
				for (int i = 0; i < impliedDecimals; i++) {
					bigDec = bigDec.divide(BigDecimal.TEN);
				}
			}
			msg.addField(fieldName, bigDec);

		} else if (type == ProtocolType.SIZE) {
			byte[] rawArray = new byte[length + 1];

			rawArray[length] = 0;

			buffer.get(rawArray, 0, length);

			// reverse
			if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
				reverseBytes(rawArray);
			}

			BigInteger bigInt = new BigInteger(rawArray);
			BigDecimal bigDec = new BigDecimal(bigInt);

			msg.addField(fieldName, bigDec.divide(BD_SIZE_DEVIDER));
		} else if (type == ProtocolType.PRICE) {
            BigDecimal valBD;
            if (length == 4) {
                int intVal = buffer.getInt();
                valBD = BigDecimal.valueOf(intVal);
                msg.addField(fieldName, valBD.divide(BD_PRICE4_DEVIDER));
            } else {
                long longVal = buffer.getLong();

                boolean positive = (byte)(longVal >> 63) == 0;

                if (!positive) {
                    long mask = LONG_MASK;
                    longVal = longVal & mask;
                    longVal = longVal * -1L;
                }

                valBD = new BigDecimal(longVal);

                msg.addField(fieldName, valBD.divide(BD_PRICE_DEVIDER));
            }
		} else if (type == ProtocolType.UDT) {
            byte[] longArray = new byte[length];

            buffer.get(longArray);

            BigDecimal pvalue = new BigDecimal(ByteBuffer.wrap(longArray).order(byteOrder).getLong());
            msg.addField(fieldName, pvalue.divide(BD_UDT_DEVIDER));
        } else if (type == ProtocolType.UINTXX) {
            byte[] parts = new byte[length];
            buffer.get(parts);
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                reverseBytes(parts);
            }
            msg.addField(fieldName, new BigDecimal(new BigInteger(1, parts)));
        } else {
			throw new EPSCommonException("Incorrect type = " + type + " for " + fieldName + " field");
		}

		int pos2 = buffer.position();
		int read = pos2 - pos1;
		if (read != length) {
			throw new EPSCommonException("Read " + read + " bytes, but length = " + length + " for " + fieldName + " field");
		}
	}

	@Override
	public void visit(String fieldName, LocalDateTime value, IFieldStructure fldStruct, boolean isDefault) {
        ProtocolType type = ProtocolType.getEnum(getAttributeValue(fldStruct, TYPE_ATTRIBUTE));
		logger.trace("Visit fieldname = [{}]; fieldType [{}]", fieldName, type);

        int length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);
		int pos1 = buffer.position();


		if (type == ProtocolType.STUB) {
            buffer.skip(length);
		} else if (type == ProtocolType.UDT) {
		    byte[] longArray = new byte[length];
            buffer.get(longArray);
            BigDecimal pvalue = new BigDecimal(ByteBuffer.wrap(longArray).order(byteOrder).getLong());

            long ldt = pvalue.divide(UDT_DEVIDER).longValue();

            msg.addField(fieldName, DateTimeUtility.toLocalDateTime(ldt));
		} else if (type == ProtocolType.DATE_TIME) {
            String dateTimePattern = getAttributeValue(fldStruct, DATE_TIME_FORMAT);
            DateTimeFormatter dateTimeFormatter = DateTimeUtility.createFormatter(dateTimePattern);

            byte[] array = new byte[length];
            buffer.get(array);
            try {
                String asciiDate = decoder.get().decode(ByteBuffer.wrap(array)).toString().trim();

                TemporalAccessor dateTime = dateTimeFormatter.parse(asciiDate);

                msg.addField(fieldName, DateTimeUtility.toLocalDateTime(dateTime));
            } catch (CharacterCodingException | DateTimeParseException e) {
                throw new EPSCommonException(e);
            }
        } else {
            throw new EPSCommonException("Incorrect type = " + type + " for " + fieldName + " field");
        }

		int pos2 = buffer.position();
		int read = pos2 - pos1;
		if (read != length) {
			throw new EPSCommonException("Read " + read + " bytes, but length = " + length + " for " + fieldName + " field");
		}
	}

    @Override
    public void visit(String fieldName, LocalDate value, IFieldStructure fldStruct, boolean isDefault) {
        ProtocolType type = ProtocolType.getEnum(getAttributeValue(fldStruct, TYPE_ATTRIBUTE));
        logger.trace("Visit fieldname = [{}]; fieldType [{}]", fieldName, type);

        int length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);
        int pos1 = buffer.position();

        if (type == ProtocolType.DAYS) {
            if (length != 2) {
                throw new EPSCommonException("Incorrect field lenth = " + length + " for " + fieldName + " field");
            }
            int days = buffer.getShort();
            msg.addField(fieldName, DateTimeUtility.toLocalDate(86_400_000L * days));
        } else if (type == ProtocolType.DATE) {
            String dateTimePattern = getAttributeValue(fldStruct, DATE_TIME_FORMAT);
            DateTimeFormatter dateTimeFormatter = DateTimeUtility.createFormatter(dateTimePattern);

            byte[] array = new byte[length];
            buffer.get(array);
            try {
                String asciiDate = decoder.get().decode(ByteBuffer.wrap(array)).toString().trim();

                TemporalAccessor date = dateTimeFormatter.parse(asciiDate);

                msg.addField(fieldName, DateTimeUtility.toLocalDate(date));
            } catch (CharacterCodingException | DateTimeParseException e) {
                throw new EPSCommonException(e);
            }
        } else if (type == ProtocolType.UINT32) {
            if (length != 4) {
                throw new EPSCommonException("Cannot read date from " + type + ". Expected length: 4; Actual: " + length);
            }
            long dateValue = buffer.getUnsignedInt();
            if (dateValue != 0) { /*0 means not value*/
                msg.addField(fieldName, LocalDate.parse(String.valueOf(dateValue), DATE_AS_INT));
            }
        } else {
            throw new EPSCommonException("Incorrect type = " + type + " for " + fieldName + " field");
        }

        int pos2 = buffer.position();
        int read = pos2 - pos1;
        if (read != length) {
            throw new EPSCommonException("Read " + read + " bytes, but length = " + length + " for " + fieldName + " field");
        }
    }

    @Override
    public void visit(String fieldName, LocalTime value, IFieldStructure fldStruct, boolean isDefault) {
        ProtocolType type = ProtocolType.getEnum(getAttributeValue(fldStruct, TYPE_ATTRIBUTE));
        logger.trace("Visit fieldname = [{}]; fieldType [{}]", fieldName, type);

        int length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);
        int pos1 = buffer.position();

        if (type == ProtocolType.TIME) {
            String dateTimePattern = getAttributeValue(fldStruct, DATE_TIME_FORMAT);
            DateTimeFormatter dateTimeFormatter = DateTimeUtility.createFormatter(dateTimePattern);

            byte[] array = new byte[length];
            buffer.get(array);
            try {
                String asciiDate = decoder.get().decode(ByteBuffer.wrap(array)).toString().trim();

                TemporalAccessor time = dateTimeFormatter.parse(asciiDate);

                msg.addField(fieldName, DateTimeUtility.toLocalTime(time));
            } catch (CharacterCodingException | DateTimeParseException e) {
                throw new EPSCommonException(e);
            }
        } else {
            throw new EPSCommonException("Incorrect type = " + type + " for " + fieldName + " field");
        }

        int pos2 = buffer.position();
        int read = pos2 - pos1;
        if (read != length) {
            throw new EPSCommonException("Read " + read + " bytes, but length = " + length + " for " + fieldName + " field");
        }
    }

	@Override
	public void visitMessageCollection(String fieldName, List<IMessage> message, IFieldStructure complexField, boolean isDefault) {
		int legsCount = 0;
        Object countField = getAttributeValue(complexField, COUNT_ATTRIBUTE);
		if (countField instanceof Number) {
			// hardcoded length
			legsCount = ((Number) countField).intValue();
		} else if (countField instanceof String){
			// reference to field
			legsCount = Integer.parseInt(msg.getField((String) countField).toString());
		}

		logger.trace("Decode - field: {}, count: {}", fieldName, legsCount);

		List<IMessage> list = new LinkedList<IMessage>();

		for (int i = 0; i < legsCount; i++) {
			IMessage msg = msgFactory.createMessage(complexField.getReferenceName(), complexField.getNamespace());
            ITCHVisitorDecode visitor = new ITCHVisitorDecode(buffer, byteOrder, msg, msgFactory, visitorSettings);
            MessageStructureWriter.WRITER.traverse(visitor, complexField.getFields());
			list.add(msg);
		}
		msg.addField(fieldName, list);
	}

	@Override
	public void visit(String fieldName, IMessage message, IFieldStructure complexField, boolean isDefault) {
		logger.trace("Decode - field: {}", fieldName);

		IMessage subMessage = msgFactory.createMessage(complexField.getReferenceName(), complexField.getNamespace());
        ITCHVisitorDecode visitor = new ITCHVisitorDecode(buffer, byteOrder, subMessage, msgFactory, visitorSettings);
        MessageStructureWriter.WRITER.traverse(visitor, complexField.getFields());
        msg.addField(fieldName, subMessage);
	}

    private static int correctIfNegative(int val) {
        boolean isNegative = (byte)(val >> 31) != 0;
        if (isNegative) {
            val &= INT_MASK;
            val *= -1;
        }
        return val;
    }

	private final void reverseBytes(byte[] data) {
		int length = data.length;
		for (int i = 0; i < length / 2; i++) {
			byte temp = data[length - i - 1];
			data[length - i - 1] = data[i];
			data[i] = temp;
		}
	}

	protected Integer extractInteger(IFieldStructure fldStruct) {
		ProtocolType type = ProtocolType.getEnum(getAttributeValue(fldStruct, TYPE_ATTRIBUTE));
		logger.trace("Visit fieldname = [{}]; fieldType [{}]", fldStruct.getName(), type);
		int length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);

		if (type == ProtocolType.UINT16) {
			return buffer.getUnsignedShort();
		} else if (type == ProtocolType.INT8) {
			return (int) buffer.get();
		} else if (type == ProtocolType.INT16) {
			return (int) buffer.getShort();
		} else if (type == ProtocolType.INT32) {
			return  buffer.getInt();
		} else if (type == ProtocolType.STUB) {
			buffer.skip(length);
			return null;
		} else {
			throw new EPSCommonException("Incorrect type = " + type + " for " + fldStruct.getName() + " field");
		}
	}

	protected Long extractLong(IFieldStructure fldStruct) {
		ProtocolType type = ProtocolType.getEnum(getAttributeValue(fldStruct, TYPE_ATTRIBUTE));
		logger.trace("Visit fieldname = [{}]; fieldType [{}]", fldStruct.getName(), type);
		int length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);

		if (type == ProtocolType.UINT32) {
			return buffer.getUnsignedInt();
		} else if (type == ProtocolType.UINT64) {
			return buffer.getLong();
		} else if (type == ProtocolType.INT16) {
			return (long) buffer.getShort();
		} else if (type == ProtocolType.INT32) {
			return (long) buffer.getInt();
		} else if (type == ProtocolType.INT64) {
			return buffer.getLong();
		} else if (type == ProtocolType.STUB) {
			buffer.skip(length);
		} else {
			throw new EPSCommonException("Incorrect type = " + type + " for " + fldStruct.getName() + " field");
		}

		return null;
	}

	protected int getStringLength(IFieldStructure fldStruct) {
    	return getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);
	}

	private static String trimRight(String str) {
		// This is copy of javadoc for trim method
		//
		// Returns a string whose value is this string, with all leading and trailing space removed,
		// where space is defined as any character whose codepoint is less than or equal to 'U+0020' (the space character)
		//
		// In ITCH 0x00 char is used as filling character
		return StringUtils.stripEnd(str, " \u0000");
	}
}