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

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MessageStructureWriter;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.GenericConverter;
import com.exactpro.sf.services.ntg.exceptions.UnknownNTGMessageTypeException;
import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;

import java.math.BigDecimal;
import java.math.BigInteger;

final class NTGVisitorDecode extends NTGVisitorBase {
    private static final Logger logger = LoggerFactory.getLogger(NTGVisitorDecode.class);
	private IMessage message = null;
	private final IMessageFactory msgFactory ;

    public NTGVisitorDecode(final IoBuffer buffer, IMessageFactory msgFactory,
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

		int length = (Integer)complexField.getAttributeValueByName(
                NTGProtocolAttribute.Length.toString());
		int offset = (Integer)complexField.getAttributeValueByName(
                NTGProtocolAttribute.Offset.toString());

		validateOffset(fieldName, accumulatedLength, offset);
		IMessage msg = msgFactory.createMessage(complexField.getName(), complexField.getNamespace());
        NTGVisitorDecode visitorNTG = new NTGVisitorDecode(super.buffer, msgFactory, msg);
		MessageStructureWriter messageStructureWriter = new MessageStructureWriter();
        messageStructureWriter.traverse(visitorNTG, complexField.getFields());

        this.message.addField(fieldName, visitorNTG.getMessage());
		accumulatedLength += length;
	}


	@Override
    public void visit(String fieldName, String value, IFieldStructure fldStruct, boolean isDefault)
    {
        validateAttributesMap(fieldName, String.class, fldStruct);

        int length = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Length.toString());
        int offset = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Offset.toString());
        String format = (String)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Format.toString());

        validateOffset(fieldName, accumulatedLength, offset);

        String decodedValue;
        if ("D".equals(format) || "DATE".equals(format))
        {
            long time = super.buffer.getLong();
            decodedValue = NTGUtility.getTransactTime(time);
        }
        else
        {
            byte[] fieldBytes = new byte[length];
            super.buffer.get( fieldBytes );
            decodedValue = GenericConverter.convertByteArrayToString( length, fieldBytes );
            int index = decodedValue.indexOf( '\0' );

            if( -1 != index )
            {
                decodedValue = decodedValue.substring( 0 , index );
            }
        }
        if( logger.isDebugEnabled()) {
            logger.debug("   Decode visiting String field [{}], decoded value [{}].", fieldName, decodedValue);
        }

        this.message.addField( fieldName, decodedValue.toString() );
        accumulatedLength += length;
    }
	
	@Override
	public void visit(String fieldName, LocalDateTime value, IFieldStructure fldStruct, boolean isDefault) {
	    validateAttributesMap(fieldName, LocalDateTime.class, fldStruct);

        int length = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Length.toString());
        int offset = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Offset.toString());

        validateLength(fieldName, lengthLong, length);
        validateOffset(fieldName, accumulatedLength, offset);

		LocalDateTime decodedValue = null;
        
        long time = super.buffer.getLong();

        decodedValue = NTGUtility.getTransactTimeAsDate(time);
        
        if (logger.isDebugEnabled()) {
            logger.debug("   Decode visiting Double field [{}], decoded value [{}].", fieldName, decodedValue);
        }

        this.message.addField( fieldName, decodedValue );
        accumulatedLength += length;
	}


	@Override
	public void visit(String fieldName, Double value, IFieldStructure fldStruct, boolean isDefault)
	{
		validateAttributesMap(fieldName, Double.class, fldStruct);

		int length = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Length.toString());
		int offset = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Offset.toString());

		String type = (String)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Type.toString());

		double divisor = "Price4".equals(type) ? 10000.0D : 100000000.0D;

		validateLength(fieldName, lengthLong, length);
		validateOffset(fieldName, accumulatedLength, offset);

		long longValue = super.buffer.getLong();
		Double decodedValue = longValue / divisor;

		if (logger.isDebugEnabled()) {
			logger.debug("   Decode visiting Double field [{}], decoded value [{}].", fieldName, decodedValue);
		}

		this.message.addField( fieldName, decodedValue );
		accumulatedLength += length;
	}

	@Override
	public void visit(String fieldName, Float value, IFieldStructure fldStruct, boolean isDefault)
	{
		validateAttributesMap(fieldName, Float.class, fldStruct);

		int length = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Length.toString());
		int offset = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Offset.toString());

		validateLength(fieldName, lengthFloat, length);
		validateOffset(fieldName, accumulatedLength, offset);

		int intValue = super.buffer.getInt();
		Float decodedValue = intValue / 10000.0f;

		if( logger.isDebugEnabled()) {
			logger.debug("   Decode visiting Float field [{}], decoded value [{}].", fieldName, decodedValue);
		}

		this.message.addField( fieldName, decodedValue );
		accumulatedLength += length;
	}

	@Override
	public void visit(String fieldName, Long value, IFieldStructure fldStruct, boolean isDefault)
	{
		validateAttributesMap(fieldName, Long.class, fldStruct);

		int length = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Length.toString());
		int offset = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Offset.toString());
        String type = (String)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Type.toString());

        ProtocolType protocolType = type != null ? ProtocolType.parse(type) : null;

		validateOffset(fieldName, accumulatedLength, offset);

		Long decodedValue;
        if (ProtocolType.UINT32 == protocolType) {
            validateLength(fieldName, lengthInt, length);
            decodedValue = super.buffer.getUnsignedInt();
        } else {
            validateLength(fieldName, lengthLong, length);
            decodedValue = super.buffer.getLong();
        }

		if (logger.isDebugEnabled()) {
			logger.debug("   Decode visiting Long field [{}], decoded value [{}].", fieldName, decodedValue);
		}

		this.message.addField( fieldName, decodedValue );
		accumulatedLength += length;
	}

	@Override
	public void visit(String fieldName, Integer value, IFieldStructure fldStruct, boolean isDefault)
	{
		validateAttributesMap(fieldName, Integer.class, fldStruct);

		int length = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Length.toString());
		int offset = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Offset.toString());

		validateOffset(fieldName, accumulatedLength, offset);

		Integer decodedValue = null;

		switch(length)
		{
			case lengthByte:
				decodedValue = new Integer( super.buffer.get());
				break;

			case lengthShort:
				decodedValue = new Integer( super.buffer.getShort());
				break;

			case lengthInt:
				decodedValue = super.buffer.getInt();
				break;
			default:
				throw new EPSCommonException(
						String.format("Unsupported length [%d] for field [%s].", length, fieldName));
		}

		if (logger.isDebugEnabled()) {
			logger.debug("   Decode visiting String field [{}], decoded value [{}].", fieldName, decodedValue);
		}

		this.message.addField( fieldName, decodedValue );
		accumulatedLength += length;
	}


	@Override
	public void visit(String fieldName, Byte value, IFieldStructure fldStruct, boolean isDefault)
	{
		validateAttributesMap(fieldName, Byte.class, fldStruct);

		int length = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Length.toString());
		int offset = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Offset.toString());

		validateLength(fieldName, lengthByte, length);
		validateOffset(fieldName, accumulatedLength, offset);
		Byte decodedValue = super.buffer.get();

		if (logger.isDebugEnabled()) {
			logger.debug("   Decode visiting String field [{}], decoded value [{}].", fieldName, decodedValue);
		}

		this.message.addField( fieldName, decodedValue );
		accumulatedLength += length;
	}

	@Override
	public void visit(String fieldName, BigDecimal value, IFieldStructure fldStruct, boolean isDefault)
	{
		validateAttributesMap(fieldName, BigDecimal.class, fldStruct);

		int length = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Length.toString());
		int offset = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Offset.toString());
		String type = (String)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Type.toString());

		validateLength(fieldName, lengthBigDecimal, length);
		validateOffset(fieldName, accumulatedLength, offset);

		if (logger.isDebugEnabled()) {
			logger.debug("Visit fieldname = [{}]; fieldType [BigDecimal]", fieldName);
		}

		if(type.equals("Uint64")) {

			byte[] rawArray = new byte[length + 1];

			rawArray[length] = 0;

			super.buffer.get(rawArray, 0, length);

			// reverse
			for (int i = 0; i < (length + 1)/2; ++i )
			{
				byte temp = 0;
				temp = rawArray[length - i];
				rawArray[length - i] = rawArray[i];
				rawArray[i] = temp;
			}

			BigInteger bigInt = new BigInteger(rawArray);
			BigDecimal bigDec = new BigDecimal(bigInt);
			this.message.addField( fieldName, bigDec );

		} else if(type.equals("Price")) {
			long val = buffer.getLong();

			boolean positive = ((byte)(val >> 63)) == 0;

			if ( !positive )
			{
				long mask = 0x7FFFFFFFFFFFFFFFL;

				val = val & mask;

				val = val * -1L;
			}

			BigDecimal valBD = new BigDecimal(val);

			this.message.addField(fieldName, valBD.divide(new BigDecimal(100000000L)));
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
