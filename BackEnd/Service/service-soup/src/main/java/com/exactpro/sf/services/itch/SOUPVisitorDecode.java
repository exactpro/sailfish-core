/*******************************************************************************
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.List;

import com.exactpro.sf.common.messages.structures.StructureUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.services.itch.soup.SOUPMessageHelper;

public class SOUPVisitorDecode extends ITCHVisitorDecode {

    private static final Logger logger = LoggerFactory.getLogger(SOUPVisitorDecode.class);
    private static final BigDecimal PRICE4_DIVISOR = new BigDecimal(10_000);
    private static final SOUPVisitorSettings DEFAULT_VISITOR_SETTINGS = new SOUPVisitorSettings();

    private final IoBuffer buffer;
    private final IMessage msg;

    public SOUPVisitorDecode(IoBuffer buffer, ByteOrder byteOrder, IMessage msg, IMessageFactory msgFactory) {
        this(buffer, byteOrder, msg, msgFactory, DEFAULT_VISITOR_SETTINGS);
    }

    public SOUPVisitorDecode(IoBuffer buffer, ByteOrder byteOrder, IMessage msg, IMessageFactory msgFactory, SOUPVisitorSettings visitorSettings) {
        super(buffer, byteOrder, msg, msgFactory, visitorSettings);
        this.buffer = buffer.order(byteOrder);
        this.msg = msg;
    }

    @Override
    public void visit(String fieldName, Integer value, IFieldStructure fldStruct, boolean isDefault) {
        String asciiValue = decodeASCIIField(fieldName, fldStruct, isDefault); 
        if (asciiValue != null) {
            msg.addField(fieldName, Integer.valueOf(asciiValue));
        } else {
            super.visit(fieldName, value, fldStruct, isDefault);
        }
    }

    @Override
    public void visit(String fieldName, Long value, IFieldStructure fldStruct, boolean isDefault) {
        String asciiValue = decodeASCIIField(fieldName, fldStruct, isDefault); 
        if (asciiValue != null) {
            msg.addField(fieldName, Long.valueOf(asciiValue));
        } else {
            super.visit(fieldName, value, fldStruct, isDefault);
        }
    }

    @Override
    public void visit(String fieldName, String value, IFieldStructure fldStruct, boolean isDefault) {
        String decoded = decodeNullTerminated(fieldName, fldStruct);
        if (decoded != null) {
            msg.addField(fieldName, decoded);
        } else {
            super.visit(fieldName, value, fldStruct, isDefault);
        }
    }

    @Override
    public void visitStringCollection(String fieldName, List<String> value, IFieldStructure fldStruct, boolean isDefault) {
        String sizeField = getAttributeValue(fldStruct,SOUPMessageHelper.SIZE_FIELD);

        if(sizeField != null) {
            if (!msg.isFieldSet(sizeField)) {
                throw new EPSCommonException("Size field is null for " +  fieldName + " field");
            }
            int size = msg.<Number>getField(sizeField).intValue();

            List<String> list = new ArrayList<>(size);
            for(int i = 0; i < size; i++){
                list.add(decodeString(fieldName, fldStruct));
            }
            msg.addField(fieldName, list);
        } else {
            throw new EPSCommonException("No found " + SOUPMessageHelper.SIZE_FIELD + " attribute for string collection for " +  fieldName + " field");
        }
    }

    @Override
    public void visit(String fieldName, IMessage message, IFieldStructure complexField, boolean isDefault) {
        String typeValue = getAttributeValue(complexField, TYPE_ATTRIBUTE);
        if (typeValue != null) {
            ProtocolType type = ProtocolType.getEnum(typeValue);
            if (type == ProtocolType.STUB) {
                return;
            }
        }
        super.visit(fieldName, message, complexField, isDefault);
    }

    @Override
    public void visit(String fieldName, BigDecimal value, IFieldStructure fldStruct, boolean isDefault) {
        ProtocolType type = ProtocolType.getEnum(getAttributeValue(fldStruct, TYPE_ATTRIBUTE));

        if (type == ProtocolType.PRICE4) {
            int pos1 = buffer.position();
            int length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);
            byte[] raw = new byte[length];

            buffer.get(raw);

            if (buffer.order() == ByteOrder.LITTLE_ENDIAN) {
                ArrayUtils.reverse(raw);
            }

            BigDecimal valueField = new BigDecimal(new BigInteger(1, raw)).divide(PRICE4_DIVISOR);
            msg.addField(fieldName, valueField);

            int pos2 = buffer.position();
            int read = pos2 - pos1;
            logger.info("Read " + read + " bytes for " + fieldName + " field");
        } else {
            super.visit(fieldName, value, fldStruct, isDefault);
        }
    }

    private String decodeNullTerminated(String fieldName, IFieldStructure fldStruct) {
        String type = getAttributeValue(fldStruct, TYPE_ATTRIBUTE);
        if (SOUPMessageHelper.VARIABLE_TYPE.equals(type)) {
            logger.trace("Visit fieldname = [{}]; fieldType [{}]", fieldName, type);

            int length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);
            int possibleLength = Math.min(length, buffer.remaining());
            byte[] array = new byte[possibleLength];

            int startPosition = buffer.position();
            buffer.get(array);
            array = getNullTerminated(array, fieldName, possibleLength);
            buffer.position(startPosition + array.length + 1);
            try {
                return decoder.get().decode(ByteBuffer.wrap(array)).toString().trim();
            } catch (CharacterCodingException | NumberFormatException e) {
                throw new EPSCommonException(e);
            }
        }
        return null;
    }

    private String decodeASCIIField(String fieldName, IFieldStructure fldStruct, boolean isDefault) {
        String type = getAttributeValue(fldStruct, TYPE_ATTRIBUTE);
        if (SOUPMessageHelper.ASCII_TYPE.equals(type)) {
            logger.trace("Visit fieldname = [{}]; fieldType [{}]", fieldName, type);

            int length = getAttributeValue(fldStruct, LENGTH_ATTRIBUTE);
            byte[] array = new byte[length];

            buffer.get(array);
            try {
                return decoder.get().decode(ByteBuffer.wrap(array)).toString().trim();
            } catch (CharacterCodingException | NumberFormatException e) {
                throw new EPSCommonException(e);
            }
        }
        return null;
    }

    private byte[] getNullTerminated(byte[] array, String fieldName, int possibleLength) {
        int index = ArrayUtils.indexOf(array, (byte)0);
        if (index == -1) {
            throw new EPSCommonException(String.format(
                    "Not found NULL (0x00) symbol for [%s] field on [%s] length", fieldName, possibleLength));
        }
        byte[] result = new byte[index];
        System.arraycopy(array, 0, result, 0, index);
        return result;
    }
}
