/*******************************************************************************
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CoderResult;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.mina.core.buffer.IoBuffer;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.services.itch.soup.SOUPMessageHelper;

public class SOUPVisitorEncode extends ITCHVisitorEncode {

    private static final BigDecimal PRICE4_MULTIPLIER = new BigDecimal(10_000);
    private static final BigDecimal MAX_UNSIGNED_INT = new BigDecimal(0xffff_ffffL);

    private final IoBuffer buffer;

    public SOUPVisitorEncode(IoBuffer buffer, ByteOrder byteOrder) {
        super(buffer, byteOrder);
        this.buffer = buffer;
    }

    @Override
    public void visit(String fieldName, Integer value, IFieldStructure fldStruct, boolean isDefault) {
        byte[] encodedASCIIvalue = encodeASCII(value != null ? value.longValue() : null, fieldName, fldStruct);
        if (encodedASCIIvalue != null) {
            buffer.put(encodedASCIIvalue);
        } else {
            super.visit(fieldName, value, fldStruct, isDefault);
        }
    }

    @Override
    public void visit(String fieldName, IMessage message, IFieldStructure fldType, boolean isDefault) {
        String typeValue = getAttributeValue(fldType, TYPE_ATTRIBUTE);
        if (typeValue != null) {
            ProtocolType type = ProtocolType.getEnum(typeValue);
            if (type == ProtocolType.STUB) {
                return;
            }
        }
        super.visit(fieldName, message, fldType, isDefault);
    }

    @Override
    public void visit(String fieldName, Long value, IFieldStructure fldStruct, boolean isDefault) {
        byte[] encodedASCIIvalue = encodeASCII(value, fieldName, fldStruct);
        if (encodedASCIIvalue != null) {
            buffer.put(encodedASCIIvalue);
        } else {
            super.visit(fieldName, value, fldStruct, isDefault);
        }
    }

    @Override
    public void visit(String fieldName, String value, IFieldStructure fldStruct, boolean isDefault) {
        byte[] encodedNullTerninated = encodeNullTerminated(value, fieldName, fldStruct);
        if (encodedNullTerninated!= null) {
            buffer.put(encodedNullTerninated);
        } else {
            super.visit(fieldName, value, fldStruct, isDefault);
        }
    }

    @Override
    public void visitStringCollection(String fieldName, List<String> values, IFieldStructure fldStruct, boolean isDefault) {
        if(values != null) {
            values.forEach(value -> {
                visit(fieldName, value, fldStruct, isDefault);
            });
        }
    }

    @Override
    public void visit(String fieldName, BigDecimal value, IFieldStructure fldStruct, boolean isDefault) {
        ProtocolType type = ProtocolType.getEnum(getAttributeValue(fldStruct, TYPE_ATTRIBUTE));
        if (value != null && type == ProtocolType.PRICE4) {
            if (value.signum() == -1) {
                throw new EPSCommonException(String.format("Field '%s' cannot be negative: %s", fldStruct.getName(), value));
            }

            try {
                BigDecimal multiplied = value.multiply(PRICE4_MULTIPLIER);
                byte[] raw = multiplied.toBigIntegerExact().toByteArray();
                int length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);

                if (raw.length > length) {
                    int firstNonZeroIndex = 0;

                    while (firstNonZeroIndex < raw.length && raw[firstNonZeroIndex] == 0) {
                        firstNonZeroIndex++;
                    }

                    int lengthDiff = raw.length - length;

                    if (firstNonZeroIndex < lengthDiff) {
                        throw new EPSCommonException(String.format("Field '%s' does not fit into %s bytes: %s", fldStruct.getName(), length, value));
                    }

                    raw = ArrayUtils.subarray(raw, lengthDiff, raw.length);
                }

                if (raw.length < length) {
                    byte[] filler = new byte[length - raw.length];
                    raw = ArrayUtils.insert(0, raw, filler);
                }

                if (buffer.order() == ByteOrder.LITTLE_ENDIAN) {
                    ArrayUtils.reverse(raw);
                }

                buffer.put(raw);
            } catch (ArithmeticException e) {
                throw new EPSCommonException(String.format("Field '%s' has more than 4 decimal places: %s", fldStruct.getName(), value), e);
            }
        } else {
            super.visit(fieldName, value, fldStruct, isDefault);
        }
    }

    private byte[] encodeNullTerminated(String value, String fieldName, IFieldStructure fldStruct) {
        String type = getAttributeValue(fldStruct, TYPE_ATTRIBUTE);
        if (SOUPMessageHelper.VARIABLE_TYPE.equals(type)) {
            Integer length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);
            String stringValue = StringUtils.EMPTY;

            if (value != null) {
                stringValue = value;
            }

            if (stringValue.length() > length - 1) {
                throw new EPSCommonException("The length of value = [" + stringValue
                        + "] is greater than Length for fieldName [" + fieldName + "]");
            }
            if (!stringValue.endsWith("\u0000")) {
                stringValue = stringValue + "\u0000";
            }
            byte[] array = new byte[stringValue.length()];

            if (!encodeNullString(stringValue, array)) {
                throw new EPSCommonException("The length of value = [" + value
                        + "] is greater than Length for fieldName [" + fieldName + "]");
            }

            return array;
        }
        return null;
    }

    private boolean encodeNullString(String value, byte[] array) {
        ByteBuffer buffer = ByteBuffer.wrap(array);
        CharBuffer charBuffer = CharBuffer.wrap(value);

        CoderResult result = encoder.get().encode(charBuffer, buffer, true);

        return !result.isOverflow();
    }

    private byte[] encodeASCII(Long value, String fieldName, IFieldStructure fldStruct) {
        String type = getAttributeValue(fldStruct, TYPE_ATTRIBUTE);
        if (SOUPMessageHelper.ASCII_TYPE.equals(type)) {
            Integer length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);
            String stringValue = StringUtils.EMPTY; 

            if (value != null) {
                stringValue = String.valueOf(value);
            }
            
            if (stringValue.length() > length) {
                throw new EPSCommonException("The length of value = [" + stringValue
                        + "] is greater than Length for fieldName [" + fieldName + "]");
            }
            stringValue =  StringUtils.leftPad(stringValue, length);
            byte[] array = new byte[length];
    
            if (!encodeString(stringValue, array)) {
                throw new EPSCommonException("The length of value = [" + value
                        + "] is greater than Length for fieldName [" + fieldName + "]");
            }
    
            return array;
        }
        return null;
    }
}
