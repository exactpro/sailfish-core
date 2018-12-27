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

import java.nio.ByteOrder;

import org.apache.commons.lang3.StringUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.DefaultMessageStructureVisitor;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.util.EPSCommonException;

abstract class NTGVisitorBase extends DefaultMessageStructureVisitor {

    private static final Logger logger = LoggerFactory.getLogger(NTGVisitorBase.class);

	protected static final int lengthByte = 1;
	protected static final int lengthShort = 2;
	protected static final int lengthInt = 4;
	protected static final int lengthFloat = 4;
	protected static final int lengthDouble = 8;
	protected static final int lengthBigDecimal = 8;
	protected static final int lengthLong = 8;
	protected final char STRING_TERMINATOR = '\0';

	protected int accumulatedLength = 0;
	protected IoBuffer buffer = null;

    protected NTGVisitorBase()
	{
		this.buffer = IoBuffer.wrap(new byte[0]);
		this.buffer.setAutoExpand(true);
		this.buffer.order(ByteOrder.LITTLE_ENDIAN);
	}

    protected NTGVisitorBase(IoBuffer buffer)
	{
		this.buffer = buffer;
		this.buffer.order(ByteOrder.LITTLE_ENDIAN);
	}

	protected void validateAttributesMap(String fieldName, Class<?> clazz, final IFieldStructure fldStruct)
	throws EPSCommonException
	{
		StringBuffer errMessage = new StringBuffer();

		//
		// Validate presence of the required keys in the Map.
		//
        if (!fldStruct.getAttributes().containsKey(NTGProtocolAttribute.Offset
				.toString()))
		{
			errMessage.append(String.format(
				"In the protocol attributes map for the field '%s' mandatory key '%s' is missed.",
                    fieldName, NTGProtocolAttribute.Offset.toString()));
		}

        if (!fldStruct.getAttributes().containsKey(NTGProtocolAttribute.Length
				.toString()))
		{
			if (!errMessage.toString().isEmpty())
			{
				errMessage.append("\r\n");
			}

			errMessage.append(String.format(
									"In the protocol attributes map for the field '%s' mandatory key '%s' is missed.",
                    fieldName, NTGProtocolAttribute.Length.toString()));
		}

        if (!fldStruct.getAttributes().containsKey(NTGProtocolAttribute.Format.toString()))
		{
			if (!errMessage.toString().isEmpty())
			{
				errMessage.append("\r\n");
			}
			errMessage.append(String.format(
									"In the protocol attributes map for the field '%s' mandatory key '%s' is missed.",
                    fieldName, NTGProtocolAttribute.Format.toString()));
		}

		if (errMessage.length() > 0)
		{
			logger.error("{}", errMessage);
			throw new EPSCommonException(errMessage.toString());
		}

		//
		// Validate format and type correlation
		//

		// If it is alphanumeric
		if (clazz == String.class || clazz == String[].class)
		{
            // If the value in the protocol attributes is not FieldFormat.A
			String format = fldStruct.getAttributeValueByName(
                    NTGProtocolAttribute.Format.toString()).toString();
            if (!NTGFieldFormat.A.toString().equals(format) &&
                    !NTGFieldFormat.D.toString().equals(format))
			{
				errMessage.append("Incorrect format protocol attribute value for the field ["+fieldName+"].\r\n");
				errMessage.append("For the field type [String] or [Array of String] protocol attribute [format] must have value [A] or [D]."
										+ " Actual value is ["+format+"].");
			}
		}

		if (errMessage.length() > 0)
		{
			logger.error("{}", errMessage);
			throw new EPSCommonException(errMessage.toString());
		}
	}

	protected void validateLength(String fieldName, int lengthExpected, int length)
	{
		if( lengthExpected != length)
		{
			String errMsg = String.format(
					"Protocol attribute length value [%d] does not match with the expected one [%d]."
					+ " Details: FieldName = [%s]", length,
					lengthExpected, fieldName);

			logger.error( errMsg );
			throw new EPSCommonException( errMsg );
		}
	}

	protected void validateOffset(String fieldName, int currentOffset, int offset)
	{
		if (currentOffset != offset)
		{
			String errMsg = String.format(
					"Protocol attribute offset value [%d] does not match with the accumulated one [%d]."
					+ " Details: FieldName = [%s]", offset,
			currentOffset, fieldName);

			logger.error( errMsg );
			throw new EPSCommonException( errMsg );
		}
	}

    public enum ProtocolType {

        UINT32("UInt32");

        private final String type;

        private ProtocolType(String type) {
            this.type = type;
        }

        public static ProtocolType parse(String type) {
            if (StringUtils.isNotBlank(type)) {
                for (ProtocolType protocolType : ProtocolType.values()) {
                    if (protocolType.type.equals(type)) {
                        return protocolType;
                    }
                }
            }
            throw new EPSCommonException("Unknown type = [" + type + "]");
        }
    }
}
