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
package com.exactpro.sf.services.fix.converter;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

import org.junit.Assert;
import org.junit.Test;
import org.quickfixj.CharsetSupport;

import com.exactpro.sf.services.fix.converter.dirty.FieldConst;
import com.exactpro.sf.services.fix.converter.dirty.struct.Field;
import com.exactpro.sf.services.fix.converter.dirty.struct.FieldList;
import com.exactpro.sf.services.fix.converter.dirty.struct.RawMessage;

public class TestRawMessage {

    private final String[] bodyLangthPath = new String[] { FieldConst.HEADER, FieldConst.BODY_LENGTH };
    private final String[] checkSumPath = new String[] { FieldConst.TRAILER, FieldConst.CHECKSUM };

    @Test
    public void testCalculation() throws UnsupportedEncodingException {
        RawMessage utf8Message = checkCalculation(createRawMessage(), StandardCharsets.UTF_8);
        RawMessage iso88591Message = checkCalculation(createRawMessage(), StandardCharsets.ISO_8859_1);

        String origin = CharsetSupport.getCharset();
        try {
            CharsetSupport.setCharset(StandardCharsets.ISO_8859_1.name());
            RawMessage defaultMessage = createRawMessage();
            defaultMessage.calculateBodyLength();
            defaultMessage.calculateCheckSum();

            Assert.assertTrue("Compare length default and ISO-8859-1",
                    Objects.equals(getField(defaultMessage, bodyLangthPath), getField(iso88591Message, bodyLangthPath)));
            Assert.assertTrue("Compare ckeckSum default and ISO-8859-1", Objects.equals(getField(defaultMessage, checkSumPath), getField(iso88591Message, checkSumPath)));
            Assert.assertTrue("Compare default and ISO-8859-1", Arrays.equals(defaultMessage.getBytes(), iso88591Message.getBytes(StandardCharsets.ISO_8859_1)));

        } finally {
            CharsetSupport.setCharset(origin);
        }

        Assert.assertTrue("Compare length UTF-8 and ISO-8859-1",
                Integer.valueOf(getField(utf8Message, bodyLangthPath)) > Integer.valueOf(getField(iso88591Message, bodyLangthPath)));
        Assert.assertFalse("Compare ckeckSum UTF-8 and ISO-8859-1", Objects.equals(getField(utf8Message, checkSumPath), getField(iso88591Message, checkSumPath)));
        Assert.assertFalse("Compare UTF-8 and ISO-8859-1",
                Arrays.equals(utf8Message.getBytes(StandardCharsets.UTF_8), iso88591Message.getBytes(StandardCharsets.ISO_8859_1)));
    }

    private RawMessage createRawMessage() {
        RawMessage rawMessage = new RawMessage();

        rawMessage.addField(new Field(FieldConst.HEADER, new FieldList()));

        rawMessage.addField(new Field("1", "\u0001"));
        rawMessage.addField(new Field("2", "\u001F"));
        rawMessage.addField(new Field("3", "\u0020"));
        rawMessage.addField(new Field("4", "\u007F"));
        rawMessage.addField(new Field("5", "\u0080"));
        rawMessage.addField(new Field("6", "\u00FF"));
        rawMessage.addField(new Field("7", "\u0100"));

        rawMessage.addField(new Field(FieldConst.TRAILER, new FieldList()));

        return rawMessage;
    }

    private RawMessage checkCalculation(RawMessage rawMessage, Charset charset) {
        rawMessage.calculateBodyLength(charset);
        rawMessage.calculateCheckSum(charset);
        Assert.assertEquals(Integer.toString(length(rawMessage, charset)), getField(rawMessage, bodyLangthPath));
        Assert.assertEquals(String.format("%03d", checkSum(rawMessage, charset)), getField(rawMessage, checkSumPath));
        return rawMessage;
    }

    private String getField(FieldList fieldList, String... path) {
        String result = null;

        for (int i = 0; i < path.length; i++) {
            if (i == path.length - 1) {
                result = fieldList.getField(path[i]).getValue();
                break;
            } else {
                fieldList = fieldList.getField(path[i]).getFields();
            }
        }

        return result;
    }

    private int checkSum(RawMessage rawMessage, Charset charset) {
        byte[] message = rawMessage.getBytes(charset);

        int end = message.length - rawMessage.getField(FieldConst.TRAILER).getBytes(charset).length;

        byte[] body = Arrays.copyOfRange(message, 0, end);

        int result = 0;
        for (byte b : body) {
            result += b;
        }

        return result & 0xFF;
    }
    
    private int length(RawMessage rawMessage, Charset charset) {
        int length = rawMessage.getBytes(charset).length;

        // FIXME: Calculate after 9=...SOH
        length -= rawMessage.getField(FieldConst.HEADER).getBytes(charset).length;
        // FIXME: Calculate before 10=...SOH
        length -= rawMessage.getField(FieldConst.TRAILER).getBytes(charset).length;

        return length;
    }
}
