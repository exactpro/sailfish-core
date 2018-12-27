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
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MessageStructureWriter;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.services.util.ServiceUtil;
import com.exactpro.sf.util.DateTimeUtility;
import com.google.common.primitives.UnsignedLong;
import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.util.LinkedList;
import java.util.List;

public class ITCHVisitorDecode extends ITCHVisitorBase {

    private static final Logger logger = LoggerFactory.getLogger(ITCHVisitorDecode.class);

	private final IoBuffer buffer;
	private final ByteOrder byteOrder;
	private final IMessage msg;
	private final IMessageFactory msgFactory;

    public ITCHVisitorDecode(IoBuffer buffer, ByteOrder byteOrder, IMessage msg, IMessageFactory msgFactory) {
		this.buffer = buffer.order(byteOrder);
		this.byteOrder = byteOrder;
		this.msg = msg;
		this.msgFactory = msgFactory;
	}

	@Override
	public void visit(String fieldName, Integer value, IFieldStructure fldStruct, boolean isDefault) {
		ProtocolType type = ProtocolType.getEnum((String) fldStruct.getAttributeValueByName(TYPE_ATTRIBUTE));
		logger.trace("Visit fieldname = [{}]; fieldType [{}]", fieldName, type);
		
		int length = (Integer) fldStruct.getAttributeValueByName(LENGTH_ATTRIBUTE);
		int pos1 = buffer.position();

		if (type == ProtocolType.UINT16) {
			msg.addField(fieldName, buffer.getUnsignedShort());
		} else if (type == ProtocolType.INT8) {
			msg.addField(fieldName, (int) buffer.get());
		} else if (type == ProtocolType.INT16) {
			msg.addField(fieldName, (int) buffer.getShort());
		} else if (type == ProtocolType.INT32) {
			msg.addField(fieldName, buffer.getInt());
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
	public void visit(String fieldName, Long value, IFieldStructure fldStruct, boolean isDefault) {
		ProtocolType type = ProtocolType.getEnum((String) fldStruct.getAttributeValueByName(TYPE_ATTRIBUTE));
		logger.trace("Visit fieldname = [{}]; fieldType [{}]", fieldName, type);

		int length = (Integer) fldStruct.getAttributeValueByName(LENGTH_ATTRIBUTE);
		int pos1 = buffer.position();

		if (type == ProtocolType.UINT32) {
			msg.addField(fieldName, buffer.getUnsignedInt());
		} else if (type == ProtocolType.UINT64) {
			msg.addField(fieldName, buffer.getLong());
		} else if (type == ProtocolType.INT16) {
			msg.addField(fieldName, (long) buffer.getShort());
		} else if (type == ProtocolType.INT32) {
			msg.addField(fieldName, (long) buffer.getInt());
		} else if (type == ProtocolType.INT64) {
			msg.addField(fieldName, buffer.getLong());
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
	public void visit(String fieldName, Short value, IFieldStructure fldStruct, boolean isDefault) {
		ProtocolType type = ProtocolType.getEnum((String) fldStruct.getAttributeValueByName(TYPE_ATTRIBUTE));
		logger.trace("Visit fieldname = [{}]; fieldType [{}]", fieldName, type);

		int length = (Integer) fldStruct.getAttributeValueByName(LENGTH_ATTRIBUTE);
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
		ProtocolType type = ProtocolType.getEnum((String) fldStruct.getAttributeValueByName(TYPE_ATTRIBUTE));
		logger.trace("Visit fieldname = [{}]; fieldType [{}]", fieldName, type);

		int length = (Integer) fldStruct.getAttributeValueByName(LENGTH_ATTRIBUTE);
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
		ProtocolType type = ProtocolType.getEnum((String) fldStruct.getAttributeValueByName(TYPE_ATTRIBUTE));
		logger.trace("Visit fieldname = [{}]; fieldType [{}]", fieldName, type);

		int length = (Integer) fldStruct.getAttributeValueByName(LENGTH_ATTRIBUTE);
		int pos1 = buffer.position();

		if (type == ProtocolType.ALPHA || type == ProtocolType.DATE || type == ProtocolType.TIME) { 
			// if you edit this lines, please edit ALPHA_NOTRIM too
			byte[] array = new byte[(Integer) fldStruct.getAttributeValueByName(LENGTH_ATTRIBUTE)];

			buffer.get(array); // FIXME: slice?

			try {
				msg.addField(fieldName, decoder.get().decode(ByteBuffer.wrap(array)).toString().trim());
			} catch (CharacterCodingException e) {
				throw new EPSCommonException(e);
			}

        } else if (type == ProtocolType.ALPHA_NOTRIM) {
			byte[] array = new byte[(Integer) fldStruct.getAttributeValueByName(LENGTH_ATTRIBUTE)];
			buffer.get(array);
			try {
				msg.addField(fieldName, decoder.get().decode(ByteBuffer.wrap(array)).toString());
			} catch (CharacterCodingException e) {
				throw new EPSCommonException(e);
			}
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
	public void visit(String fieldName, Double value, IFieldStructure fldStruct, boolean isDefault) {
		ProtocolType type = ProtocolType.getEnum((String) fldStruct.getAttributeValueByName(TYPE_ATTRIBUTE));
		logger.trace("Visit fieldname = [{}]; fieldType [{}]", fieldName, type);

		int length = (Integer) fldStruct.getAttributeValueByName(LENGTH_ATTRIBUTE);
		int pos1 = buffer.position();

		if (type == ProtocolType.SIZE || type == ProtocolType.SIZE4) {
			byte[] rawArray = new byte[length + 1];

			rawArray[length] = 0;

			buffer.get(rawArray, 0, length);

			if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
				reverseBytes(rawArray);
			}

			final UnsignedLong divider = (type == ProtocolType.SIZE) ? SIZE_DEVIDER : SIZE4_DEVIDER;

			double result = ServiceUtil.convertFromUint64(rawArray, divider);

			msg.addField(fieldName, result);
		} else if (type == ProtocolType.PRICE || type == ProtocolType.PRICE4) {
			long val = buffer.getLong();

			boolean positive = ((byte) (val >> 63)) == 0;
			
			if (!positive) {
				long mask = 0x7FFFFFFFFFFFFFFFL;

				val = val & mask;

				val = val * -1L;
			}
			double valDouble;

			if (type == ProtocolType.PRICE) {
				valDouble = ServiceUtil.divide(val, 100000000L);
			} else {
				valDouble = ServiceUtil.divide(val, 10000L);
			}

			msg.addField(fieldName, valDouble);
		} else if (type == ProtocolType.UINT16) {
			BigDecimal val = new BigDecimal(buffer.getUnsignedShort());

			Integer impliedDecimals = (Integer) fldStruct.getAttributeValueByName(IMPILED_DECIMALS_ATTRIBUTE);
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
		ProtocolType type = ProtocolType.getEnum((String) fldStruct.getAttributeValueByName(TYPE_ATTRIBUTE));
		logger.trace("Visit fieldname = [{}]; fieldType [{}]", fieldName, type);

		int length = (Integer) fldStruct.getAttributeValueByName(LENGTH_ATTRIBUTE);
		int pos1 = buffer.position();

		if (type == ProtocolType.PRICE) {
			int val = buffer.getInt();
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
		ProtocolType type = ProtocolType.getEnum((String) fldStruct.getAttributeValueByName(TYPE_ATTRIBUTE));
		logger.trace("Visit fieldname = [{}]; fieldType [{}]", fieldName, type);

		Integer impliedDecimals = (Integer) fldStruct.getAttributeValueByName(IMPILED_DECIMALS_ATTRIBUTE);
		int length = (Integer) fldStruct.getAttributeValueByName(LENGTH_ATTRIBUTE);
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
			long val = buffer.getLong();

			boolean positive = ((byte) (val >> 63)) == 0;

			if (!positive) {
				long mask = 0x7FFFFFFFFFFFFFFFL;
				val = val & mask;
				val = val * -1L;
			}

			BigDecimal valBD = new BigDecimal(val);

			msg.addField(fieldName, valBD.divide(BD_PRICE_DEVIDER));
		} else if (type == ProtocolType.UDT) {
			byte[] longArray = new byte[length];

			buffer.get(longArray);

			BigDecimal pvalue = new BigDecimal(ByteBuffer.wrap(longArray).order(byteOrder).getLong());
			msg.addField(fieldName, pvalue.divide(BD_UDT_DEVIDER));
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
		ProtocolType type = ProtocolType.getEnum((String) fldStruct.getAttributeValueByName(TYPE_ATTRIBUTE));
		logger.trace("Visit fieldname = [{}]; fieldType [{}]", fieldName, type);
		
		int length = (Integer) fldStruct.getAttributeValueByName(LENGTH_ATTRIBUTE);
		int pos1 = buffer.position();

		
		if (type == ProtocolType.STUB) {
            buffer.skip(length);
		} else if (type == ProtocolType.UDT) {
		    byte[] longArray = new byte[length];
            buffer.get(longArray);
            BigDecimal pvalue = new BigDecimal(ByteBuffer.wrap(longArray).order(byteOrder).getLong());

            long ldt = pvalue.divide(UDT_DEVIDER).longValue();
            
            msg.addField(fieldName, DateTimeUtility.toLocalDateTime(ldt));
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
        ProtocolType type = ProtocolType.getEnum((String) fldStruct.getAttributeValueByName(TYPE_ATTRIBUTE));
        logger.trace("Visit fieldname = [{}]; fieldType [{}]", fieldName, type);

        int length = (Integer) fldStruct.getAttributeValueByName(LENGTH_ATTRIBUTE);
        int pos1 = buffer.position();

        if (type == ProtocolType.DAYS) {
            if (length != 2) {
                throw new EPSCommonException("Incorrect field lenth = " + length + " for " + fieldName + " field");
            }
            int days = buffer.getShort();
            msg.addField(fieldName, DateTimeUtility.toLocalDate(86400_000L * days));
        } else if (type == ProtocolType.DATE) {
            byte[] array = new byte[length];

            buffer.get(array);

            try {
                String asciiDate = decoder.get().decode(ByteBuffer.wrap(array)).toString().trim();

                LocalDate date = LocalDate.parse(asciiDate, dateFormatter);

                msg.addField(fieldName, date);
            } catch (CharacterCodingException e) {
                throw new EPSCommonException(e);
            } catch (DateTimeParseException e) {
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
    public void visit(String fieldName, LocalTime value, IFieldStructure fldStruct, boolean isDefault) {
        ProtocolType type = ProtocolType.getEnum((String) fldStruct.getAttributeValueByName(TYPE_ATTRIBUTE));
        logger.trace("Visit fieldname = [{}]; fieldType [{}]", fieldName, type);

        int length = (Integer) fldStruct.getAttributeValueByName(LENGTH_ATTRIBUTE);
        int pos1 = buffer.position();

        if (type == ProtocolType.TIME) {
            byte[] array = new byte[length];

            buffer.get(array);

            try {
                String asciiDate = decoder.get().decode(ByteBuffer.wrap(array)).toString().trim();

                LocalTime time = LocalTime.parse(asciiDate, timeFormatter);

                msg.addField(fieldName, time);
            } catch (CharacterCodingException e) {
                throw new EPSCommonException(e);
            } catch (DateTimeParseException e) {
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
		Object countField = complexField.getAttributeValueByName(COUNT_ATTRIBUTE);
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
            ITCHVisitorDecode visitor = new ITCHVisitorDecode(buffer, byteOrder, msg, msgFactory);
			MessageStructureWriter messageStructureWriter = new MessageStructureWriter();
			messageStructureWriter.traverse(visitor, complexField.getFields());
			list.add(msg);
		}
		msg.addField(fieldName, list);
	}

	@Override
	public void visit(String fieldName, IMessage message, IFieldStructure complexField, boolean isDefault) {
		logger.trace("Decode - field: {}", fieldName);

		IMessage msg = msgFactory.createMessage(complexField.getReferenceName(), complexField.getNamespace());
        ITCHVisitorDecode visitor = new ITCHVisitorDecode(buffer, byteOrder, msg, msgFactory);
		MessageStructureWriter messageStructureWriter = new MessageStructureWriter();
		messageStructureWriter.traverse(visitor, complexField.getFields());
		msg.addField(fieldName, msg);
	}

	private final void reverseBytes(byte[] data) {
		int length = data.length;
		for (int i = 0; i < length / 2; i++) {
			byte temp = data[length - i - 1];
			data[length - i - 1] = data[i];
			data[i] = temp;
		}
	}
}