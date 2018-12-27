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
package com.exactpro.sf.comparison.conversion;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import com.exactpro.sf.util.DateTimeUtility;

import junit.framework.Assert;

public class TestMultiConverter {
    private static final Object[][] VALUES = new Object[][] {
            { null, null, null, null, null, null, null, null, null, true, null, null, null },
            { null, null, (byte)65, (byte)66, (byte)67, (byte)68, (byte)69, (byte)70, (byte)71, (byte)72, null, null, null },
            { null, (short)97, null, (short)98, (short)99, (short)100, (short)101, (short)102, (short)103, (short)104, null, null, null },
            { null, 48, 49, null, 50, 51, 52, 53, 54, 55, null, null, null },
            { null, 1L, 2L, 3L, null, 4L, 5L, 6L, null, 7L, 1513123200000L, 44350228L, 1513167550229L },
            { null, 8f, 9f, 10f, 11f, null, 12.5f, 13.5f, null, 14.5f, null, null, null },
            { null, 15d, 16d, 17d, 18d, 19.5d, null, 20.5d, null, 21.5d, null, null, null },
            { null, new BigDecimal("22"), new BigDecimal("23"), new BigDecimal("24"), new BigDecimal("25"), new BigDecimal("26.5"), new BigDecimal("27.5"), null, null, new BigDecimal("28.5"), null, null, null },
            { null, 'x', 'y', 'z', null, null, null, null, null, 'w', null, null, null },
            { "true", "29", "30", "31", "32", "33.5", "34.5", "35.5", "v", null, "2017-12-13", "15:34:07.279", "2017-12-13T15:34:07.281" },
            { null, null, null, null, LocalDate.now(), null, null, null, null, LocalDate.now(), null, null, LocalDate.now() },
            { null, null, null, null, LocalTime.now(), null, null, null, null, LocalTime.now(), null, null, LocalTime.now() },
            { null, null, null, null, LocalDateTime.now(), null, null, null, null, LocalDateTime.now(), DateTimeUtility.toLocalDateTime(LocalDate.now()), DateTimeUtility.toLocalDateTime(LocalTime.now()), null }
    };

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testMultiConversion() {
        for(int i = 0; i < VALUES.length; i++) {
            for(int j = 0; j < VALUES.length; j++) {
                Object originalValue = VALUES[i][j];

                if(originalValue == null) {
                    continue;
                }

                Object convertedValue = MultiConverter.convert(originalValue, VALUES[j][i].getClass());
                Object convertedBack = MultiConverter.convert(convertedValue, originalValue.getClass());

                Assert.assertEquals(originalValue, convertedBack);
            }
        }
    }

    @Test
    public void testOutOfRangeValueNumber() {
        thrown.expect(ConversionException.class);
        thrown.expectMessage(CoreMatchers.is("Cannot convert from Short to Byte due to data loss - original: 32767, converted: -1"));
        MultiConverter.convert(Short.MAX_VALUE, Byte.class);
    }

    @Test
    public void testOutOfRangeValueCharacter() {
        thrown.expect(ConversionException.class);
        thrown.expectMessage(CoreMatchers.is("Cannot convert from Short to Character because value is out range - value: -32768, range: [0..65535]"));
        MultiConverter.convert(Short.MIN_VALUE, Character.class);
    }

    @Test
    public void testDecimalToIntegerDataLoss() {
        thrown.expect(ConversionException.class);
        thrown.expectMessage(CoreMatchers.is("Cannot convert from Double to Integer due to data loss - original: 35.9, converted: 35"));
        MultiConverter.convert(35.9, Integer.class);
    }
    
    @Test
    public void testFloatToDoubleDataLoss() {
        thrown.expect(ConversionException.class);
        thrown.expectMessage(CoreMatchers.is("Cannot convert from Float to Double due to data loss - original: 35.9, converted: 35.900001525878906"));
        MultiConverter.convert(35.9f, Double.class);
    }

    @Test
    public void testIncorrectLengthStringToCharacter() {
        thrown.expect(ConversionException.class);
        thrown.expectMessage(CoreMatchers.is("Cannot convert from String to Character due to incorrect length - value: abc, length: 3"));
        MultiConverter.convert("abc", Character.class);
    }

    @Test
    public void testIncorrectStringToDate() {
        thrown.expect(DateTimeParseException.class);
        thrown.expectMessage(CoreMatchers.is("Text 'qwerty' could not be parsed at index 0"));
        MultiConverter.convert("qwerty", LocalDate.class);
    }

    @Test
    public void testIncorrectStringToNumber() {
        thrown.expect(ConversionException.class);
        thrown.expectMessage(CoreMatchers.is("Cannot convert from String to Double - value: qwerty, reason: For input string: \"qwerty\""));
        MultiConverter.convert("qwerty", Double.class);
    }

    @Test
    public void testUnknownType() {
        thrown.expect(ConversionException.class);
        thrown.expectMessage(CoreMatchers.is("No converter for type: BigInteger"));
        MultiConverter.convert("125", BigInteger.class);
    }

}
