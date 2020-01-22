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
package com.exactpro.sf.services.ntg;

import static com.exactpro.sf.common.messages.structures.StructureUtils.getAttributeValue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;

import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MessageStructureWriter;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.services.ntg.exceptions.UnknownNTGMessageTypeException;
import com.exactpro.sf.util.DateTimeUtility;

final class NTGVisitorDecode extends NTGVisitorBase {
    private static final Logger logger = LoggerFactory.getLogger(NTGVisitorDecode.class);
    private final IMessage message;
    private final IMessageFactory msgFactory;

    public NTGVisitorDecode(IoBuffer buffer, IMessageFactory msgFactory,
                            IMessage message)
	{
		super(buffer);
		this.msgFactory = msgFactory;
		this.message = message;
	}

	@Override
	public void visit(String fieldName, IMessage message, IFieldStructure complexField, boolean isDefault)
	{
		if( logger.isDebugEnabled()) {
			logger.debug("   Decode visiting IMessage field [{}]" , fieldName);
		}

        int length = getAttributeValue(complexField, NTGProtocolAttribute.Length.toString());
        int offset = getAttributeValue(complexField, NTGProtocolAttribute.Offset.toString());

		validateOffset(fieldName, accumulatedLength, offset);
		IMessage msg = msgFactory.createMessage(complexField.getName(), complexField.getNamespace());
        NTGVisitorDecode visitorNTG = new NTGVisitorDecode(buffer, msgFactory, msg);
        MessageStructureWriter.WRITER.traverse(visitorNTG, complexField.getFields());

        this.message.addField(fieldName, visitorNTG.getMessage());
		accumulatedLength += length;
	}


	@Override
    public void visit(String fieldName, String value, IFieldStructure fldStruct, boolean isDefault)
    {
        validateAttributesMap(fieldName, String.class, fldStruct);

        int length = getAttributeValue(fldStruct, NTGProtocolAttribute.Length.toString());
        int offset = getAttributeValue(fldStruct, NTGProtocolAttribute.Offset.toString());
        String format = getAttributeValue(fldStruct, NTGProtocolAttribute.Format.toString());

        validateOffset(fieldName, accumulatedLength, offset);

        String decodedValue;
        if ("D".equals(format) || "DATE".equals(format))
        {
            long time = buffer.getLong();
            decodedValue = NTGUtility.getTransactTime(time);
        }
        else {
            byte[] fieldBytes = new byte[length];
            buffer.get(fieldBytes);
            try {
                decodedValue = DECODER.get().decode(ByteBuffer.wrap(fieldBytes)).toString().trim();
            } catch (CharacterCodingException e) {
                throw new EPSCommonException("Problem with decoding the fieldBytes = \"" + Arrays.toString(fieldBytes) + "\"", e);
            }
        }
        if( logger.isDebugEnabled()) {
            logger.debug("   Decode visiting String field [{}], decoded value [{}].", fieldName, decodedValue);
        }

        message.addField(fieldName, decodedValue.toString());
        accumulatedLength += length;
    }
    
    @Override
    public void visit(String fieldName, LocalDateTime value, IFieldStructure fldStruct, boolean isDefault) {
        validateAttributesMap(fieldName, LocalDateTime.class, fldStruct);
        
        int length = getAttributeValue(fldStruct, NTGProtocolAttribute.Length.toString());
        int offset = getAttributeValue(fldStruct, NTGProtocolAttribute.Offset.toString());
        String dateTimeFormat = getAttributeValue(fldStruct, NTGProtocolAttribute.DateTimeFormat.toString());
        
        validateOffset(fieldName, accumulatedLength, offset);
        
        DateTimeFormatter dateTimeFormatter = DateTimeUtility.createFormatter(dateTimeFormat);
        byte[] array = new byte[length];
        buffer.get(array);
        try {
            String asciiDate = DECODER.get().decode(ByteBuffer.wrap(array)).toString().trim();
            TemporalAccessor dateTime = dateTimeFormatter.parse(asciiDate);
            message.addField(fieldName, DateTimeUtility.toLocalDateTime(dateTime));
            accumulatedLength += length;
        } catch (CharacterCodingException e) {
            throw new EPSCommonException("Problem with decoding the asciiDate = \"" + Arrays.toString(array) + "\"", e);
        }
    }

    @Override
	public void visit(String fieldName, Double value, IFieldStructure fldStruct, boolean isDefault)
	{
		validateAttributesMap(fieldName, Double.class, fldStruct);

        int length = getAttributeValue(fldStruct, NTGProtocolAttribute.Length.toString());
        int offset = getAttributeValue(fldStruct, NTGProtocolAttribute.Offset.toString());

        String type = getAttributeValue(fldStruct, NTGProtocolAttribute.Type.toString());

        double divisor = "Price4".equals(type) ? 10_000 : 100_000_000;

		validateLength(fieldName, lengthLong, length);
		validateOffset(fieldName, accumulatedLength, offset);

        long longValue = buffer.getLong();
		Double decodedValue = longValue / divisor;

		if (logger.isDebugEnabled()) {
			logger.debug("   Decode visiting Double field [{}], decoded value [{}].", fieldName, decodedValue);
		}

        message.addField(fieldName, decodedValue);
		accumulatedLength += length;
	}

	@Override
	public void visit(String fieldName, Float value, IFieldStructure fldStruct, boolean isDefault)
	{
		validateAttributesMap(fieldName, Float.class, fldStruct);

        int length = getAttributeValue(fldStruct, NTGProtocolAttribute.Length.toString());
        int offset = getAttributeValue(fldStruct, NTGProtocolAttribute.Offset.toString());

		validateLength(fieldName, lengthFloat, length);
		validateOffset(fieldName, accumulatedLength, offset);

        int intValue = buffer.getInt();
        Float decodedValue = intValue / 10_000.0f;

		if( logger.isDebugEnabled()) {
			logger.debug("   Decode visiting Float field [{}], decoded value [{}].", fieldName, decodedValue);
		}

        message.addField(fieldName, decodedValue);
		accumulatedLength += length;
	}

	@Override
	public void visit(String fieldName, Long value, IFieldStructure fldStruct, boolean isDefault)
	{
		validateAttributesMap(fieldName, Long.class, fldStruct);

        int length = getAttributeValue(fldStruct, NTGProtocolAttribute.Length.toString());
        int offset = getAttributeValue(fldStruct, NTGProtocolAttribute.Offset.toString());
        String type = getAttributeValue(fldStruct, NTGProtocolAttribute.Type.toString());

        ProtocolType protocolType = type != null ? ProtocolType.parse(type) : null;

		validateOffset(fieldName, accumulatedLength, offset);

		Long decodedValue;
        if(protocolType == ProtocolType.UINT32) {
            validateLength(fieldName, lengthInt, length);
            decodedValue = buffer.getUnsignedInt();
        } else {
            validateLength(fieldName, lengthLong, length);
            decodedValue = buffer.getLong();
        }

		if (logger.isDebugEnabled()) {
			logger.debug("   Decode visiting Long field [{}], decoded value [{}].", fieldName, decodedValue);
		}

        message.addField(fieldName, decodedValue);
		accumulatedLength += length;
	}

	@Override
	public void visit(String fieldName, Integer value, IFieldStructure fldStruct, boolean isDefault)
	{
		validateAttributesMap(fieldName, Integer.class, fldStruct);

        int length = getAttributeValue(fldStruct, NTGProtocolAttribute.Length.toString());
        int offset = getAttributeValue(fldStruct, NTGProtocolAttribute.Offset.toString());

		validateOffset(fieldName, accumulatedLength, offset);

		Integer decodedValue = null;

		switch(length)
		{
			case lengthByte:
                decodedValue = new Integer(buffer.get());
				break;

			case lengthShort:
                decodedValue = new Integer(buffer.getShort());
				break;

			case lengthInt:
                decodedValue = buffer.getInt();
				break;
			default:
				throw new EPSCommonException(
						String.format("Unsupported length [%d] for field [%s].", length, fieldName));
		}

		if (logger.isDebugEnabled()) {
			logger.debug("   Decode visiting String field [{}], decoded value [{}].", fieldName, decodedValue);
		}

        message.addField(fieldName, decodedValue);
		accumulatedLength += length;
	}


	@Override
	public void visit(String fieldName, Byte value, IFieldStructure fldStruct, boolean isDefault)
	{
		validateAttributesMap(fieldName, Byte.class, fldStruct);

        int length = getAttributeValue(fldStruct, NTGProtocolAttribute.Length.toString());
        int offset = getAttributeValue(fldStruct, NTGProtocolAttribute.Offset.toString());

		validateLength(fieldName, lengthByte, length);
		validateOffset(fieldName, accumulatedLength, offset);
        Byte decodedValue = buffer.get();

		if (logger.isDebugEnabled()) {
			logger.debug("   Decode visiting String field [{}], decoded value [{}].", fieldName, decodedValue);
		}

        message.addField(fieldName, decodedValue);
		accumulatedLength += length;
	}

	@Override
	public void visit(String fieldName, BigDecimal value, IFieldStructure fldStruct, boolean isDefault)
	{
		validateAttributesMap(fieldName, BigDecimal.class, fldStruct);

        int length = getAttributeValue(fldStruct, NTGProtocolAttribute.Length.toString());
        int offset = getAttributeValue(fldStruct, NTGProtocolAttribute.Offset.toString());
        String type = getAttributeValue(fldStruct, NTGProtocolAttribute.Type.toString());

		validateLength(fieldName, lengthBigDecimal, length);
		validateOffset(fieldName, accumulatedLength, offset);

		if (logger.isDebugEnabled()) {
			logger.debug("Visit fieldname = [{}]; fieldType [BigDecimal]", fieldName);
		}

        if("Uint64".equals(type)) {

			byte[] rawArray = new byte[length + 1];

			rawArray[length] = 0;

            buffer.get(rawArray, 0, length);

			// reverse
			for (int i = 0; i < (length + 1)/2; ++i )
			{
                byte temp = rawArray[length - i];
				rawArray[length - i] = rawArray[i];
				rawArray[i] = temp;
			}

			BigInteger bigInt = new BigInteger(rawArray);
			BigDecimal bigDec = new BigDecimal(bigInt);
            message.addField(fieldName, bigDec);

        } else if("Price".equals(type)) {
			long val = buffer.getLong();

            boolean positive = (byte)(val >> 63) == 0;

			if ( !positive )
			{
				long mask = 0x7FFFFFFFFFFFFFFFL;

				val = val & mask;

				val = val * -1L;
			}

			BigDecimal valBD = new BigDecimal(val);

            message.addField(fieldName, valBD.divide(new BigDecimal(100_000_000L)));
		} else {
            throw new UnknownNTGMessageTypeException("Unknown protocol atribute Type: " + type);
		}

		accumulatedLength += length;
	}

	public IMessage getMessage()
	{
		return message;
	}
}
