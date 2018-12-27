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

import java.math.BigDecimal;

import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MessageStructureReader;
import com.exactpro.sf.common.messages.MessageStructureReaderHandlerImpl;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.GenericConverter;
import com.exactpro.sf.services.ntg.exceptions.NullFieldValue;
import com.exactpro.sf.services.ntg.exceptions.TooLongStringValueException;
import com.exactpro.sf.services.ntg.exceptions.UnknownNTGMessageTypeException;

public final class NTGVisitorEncode extends NTGVisitorBase {
    private static final Logger logger = LoggerFactory.getLogger(NTGVisitorEncode.class);

    public NTGVisitorEncode() {
		super();
	}


	@Override
	public void visit(String fieldName, IMessage message, IFieldStructure complexField, boolean isDefault)
	{
		if (message == null) {
			throw new NullPointerException("Message is null. Field name = "+fieldName);
		}

		logger.debug("   Encode visiting IMessage field [{}] , value = [{}]", fieldName, message);

		int	length = (Integer)complexField.getAttributeValueByName(
                NTGProtocolAttribute.Length.toString());

		int	offset = (Integer)complexField.getAttributeValueByName(
                NTGProtocolAttribute.Offset.toString());

		validateOffset(fieldName, accumulatedLength, offset);

        NTGVisitorEncode visitorNTG = new NTGVisitorEncode();

		MessageStructureReader messageStructureReader = new MessageStructureReader();
        messageStructureReader.traverse(visitorNTG, complexField.getFields(), message,
					MessageStructureReaderHandlerImpl.instance());

        this.buffer.put(visitorNTG.getBuffer().flip());
		accumulatedLength += length;
	}

	@Override
    public void visit(String fieldName, String value, IFieldStructure fldStruct, boolean isDefault)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("   Encode visiting String field [{}] , value = [{}]", fieldName, (null == value ? "" : value));
        }

        validateAttributesMap(fieldName, String.class, fldStruct);

        int length = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Length.toString());
        int offset = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Offset.toString());
        String format = (String)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Format.toString());

        validateOffset(fieldName, accumulatedLength, offset);

        if ("D".equals(format) || "DATE".equals(format))
        {
            if (writeFiller(value, length, fieldName)) {
                return;
            }
            long time = NTGUtility.getTransactTime(value);
            this.buffer.putLong( time );
        }
        else
        {
            StringBuffer terminatedString  = new StringBuffer();

            if(null == value)
            {
                terminatedString.append(super.STRING_TERMINATOR);
            }
            else
            {
                if( value.length() > length )
                {
                    throw new TooLongStringValueException(String.format(
                            "Length [%d] of provided value exceeds maximum field length [%d]. " +
                            "Field name=[%s], value=[%s].", value.length(), length,
                            fieldName, value));
                }

                terminatedString.append( value );

                if( value.length() < length )
                {
                    terminatedString.append(super.STRING_TERMINATOR);
                }
            }

            this.buffer.put( GenericConverter.convertStringToArray( length, terminatedString.toString() ));
        }
        accumulatedLength += length;
    }
	
	@Override
	public void visit(String fieldName, LocalDateTime value, IFieldStructure fldStruct, boolean isDefault) {
	    
	    if (logger.isDebugEnabled()) {
            logger.debug("   Encode visiting String field [{}] , value = [{}]", fieldName, (null == value ? "" : value));
        }

        validateAttributesMap(fieldName, LocalDateTime.class, fldStruct);

        int length = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Length.toString());
        int offset = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Offset.toString());

        validateOffset(fieldName, accumulatedLength, offset);

        long time = NTGUtility.getTransactTime(value);
        this.buffer.putLong( time );
        
        accumulatedLength += length;
	    
	}

	@Override
	public void visit(String fieldName, Double value, IFieldStructure fldStruct, boolean isDefault)
	{
		if(fldStruct.getAttributeNames().size() == 0 )
		{
			throw new NullFieldValue(String.format( "Field name = [%s] has null value" , fieldName ));
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("   Encode visiting Double field [{}] , value = [{}]", fieldName, value);			
			logger.debug("         float value [{}] will be encoded as integer [{}]", value,
					(int) (value * 100000000.0d));
		}
		validateAttributesMap(fieldName, Double.class, fldStruct);

		int length = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Length.toString());

		int offset = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Offset.toString());

		String type = (String)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Type.toString());

		int precision = "Price4".equals(type) ? 4 : 8;

		if (writeFiller(value, length, fieldName)) {
            return;
        }

		validateLength(fieldName, lengthDouble, length);
		validateOffset(fieldName, accumulatedLength, offset);

		BigDecimal baseValue = new BigDecimal( value );
		BigDecimal baseScaled  = baseValue.setScale( precision, BigDecimal.ROUND_HALF_UP );
		BigDecimal multiplied = baseScaled.multiply( new BigDecimal(Math.pow(10, precision))) ;

		buffer.putLong(multiplied.longValue());

		accumulatedLength += length;
	}

	@Override
	public void visit(String fieldName, Float value, IFieldStructure fldStruct, boolean isDefault)
	{
		if(fldStruct.getAttributeNames().size() == 0 )
		{
			throw new NullFieldValue(String.format( "Field name = [%s] has null value" , fieldName ));
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("   Encode visiting Float field [{}] , value = [{}]", fieldName, value);			
			logger.debug("         float value [{}] will be encoded as integer [{}]", value, (int) (value * 10000.0f));
		}
		validateAttributesMap(fieldName, Float.class, fldStruct);

		int length = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Length.toString());

		int offset = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Offset.toString());

		if (writeFiller(value, length, fieldName)) {
            return;
        }

		validateLength(fieldName, lengthFloat, length);
		validateOffset(fieldName, accumulatedLength, offset);

		BigDecimal baseValue = new BigDecimal( value );
		BigDecimal baseScaled  = baseValue.setScale( 4, BigDecimal.ROUND_HALF_UP );
		BigDecimal multiplied = baseScaled.multiply( new BigDecimal(10000.0f)) ;

		buffer.putInt(multiplied.intValue());

		accumulatedLength += length;
	}

	@Override
	public void visit(String fieldName, Long value, IFieldStructure fldStruct, boolean isDefault)
	{
		if (logger.isDebugEnabled()) {
			logger.debug("   Encode visiting Long field [{}] , value = [{}]", fieldName, value);
		}

		if(fldStruct.getAttributeNames().size() == 0 )
		{
			throw new NullFieldValue(String.format( "Field name = [%s] has null value" , fieldName ));
		}

		validateAttributesMap(fieldName, Long.class, fldStruct);

		int length = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Length.toString());
		int offset = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Offset.toString());
        String type = (String) fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Type.toString());

        ProtocolType protocolType = type != null ? ProtocolType.parse(type) : null;

		validateOffset(fieldName, accumulatedLength, offset);

		if (writeFiller(value, length, fieldName)) {
            return;
        }
        if (ProtocolType.UINT32 == protocolType) {
            validateLength(fieldName, lengthInt, length);
            buffer.putUnsignedInt(value);
        } else {
            validateLength(fieldName, lengthLong, length);
            buffer.putLong(value);
        }

		accumulatedLength += length;
	}

	@Override
	public void visit(String fieldName, Integer value, IFieldStructure fldStruct, boolean isDefault)
	{
		if (logger.isDebugEnabled()) {
			logger.debug("   Encode visiting Integer field [{}] , value = [{}]", fieldName, value);
		}

		if (fldStruct.getAttributeNames().size() == 0) {
			throw new NullFieldValue(String.format( "Field name = [%s] has null value" , fieldName ));
		}

		int length = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Length.toString());
		int offset = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Offset.toString());

		logger.debug("   length = {}", length);
		logger.debug("   offset = {}", offset);

        validateAttributesMap(fieldName, Integer.class, fldStruct);

        if (writeFiller(value, length, fieldName)) {
            return;
        }

		validateOffset(fieldName, accumulatedLength, offset);

		switch( length )
		{
			case lengthByte:
				buffer.put(value.byteValue());
				break;

			case lengthShort:
				buffer.putShort(value.shortValue());
				break;

			case lengthInt:
				buffer.putInt(value);
				break;

			default:
				throw new EPSCommonException(
						String.format("Unsupported length [%d] for field [%s].", length, fieldName));
		}

		accumulatedLength += length;
	}

	@Override
	public void visit(String fieldName, Byte value, IFieldStructure fldStruct, boolean isDefault)
	{
		if (logger.isDebugEnabled()) {
			logger.debug("   Encode visiting Byte field [{}] , value = [{}]", fieldName, value);
		}

		if(fldStruct.getAttributeNames().size() == 0 )
		{
			throw new NullFieldValue(String.format( "Field name = [%s] has null value" , fieldName ));
		}

		validateAttributesMap(fieldName, Byte.class, fldStruct);

		int length = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Length.toString());
		int offset = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Offset.toString());

		validateLength(fieldName, lengthByte, length);
		validateOffset(fieldName, accumulatedLength, offset);

		if (writeFiller(value, length, fieldName)) {
            return;
        }

		buffer.put(value);
		accumulatedLength += length;
	}

	@Override
	public void visit(String fieldName, BigDecimal value, IFieldStructure fldStruct, boolean isDefault)
	{
		if (logger.isDebugEnabled()) {
			logger.debug("   Encode visiting BigDecimal field [{}] , value = [{}]", fieldName, value);
		}

		if(null == value )
		{
			throw new NullFieldValue(String.format( "Field name = [%s] has null value" , fieldName ));
		}
		validateAttributesMap(fieldName, BigDecimal.class, fldStruct);

		int length = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Length.toString());

		int offset = (Integer)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Offset.toString());

		String type = (String)fldStruct.getAttributeValueByName(
                NTGProtocolAttribute.Type.toString());

		validateLength(fieldName, lengthBigDecimal, length);
		validateOffset(fieldName, accumulatedLength, offset);

		if(type.equals("Uint64")) {
			buffer.putLong(value.longValue());
		} else if(type.equals("Price")) {

			BigDecimal baseScaled  = value.setScale( 8, BigDecimal.ROUND_HALF_UP );
			BigDecimal multiplied = baseScaled.multiply( new BigDecimal(100000000.0d)) ;

			buffer.putLong(multiplied.longValue());
		} else {
            throw new UnknownNTGMessageTypeException("Unknown protocol atribute Type: " + type);
		}

		accumulatedLength += length;
	}


	public IoBuffer getBuffer() {
		return buffer;
	}

	public int getAccumulatedLength() {
		return super.accumulatedLength;
	}

    private boolean writeFiller(Object value, int length, String fieldName) {
        if (null == value) {
            for (int i = 0; i < length; i++) {
                buffer.put((byte) 0x0);
            }
            accumulatedLength += length;
            logger.warn("Using default filler for [{}] field", fieldName);
            return true;
        } else {
            return false;
        }
	}
}
