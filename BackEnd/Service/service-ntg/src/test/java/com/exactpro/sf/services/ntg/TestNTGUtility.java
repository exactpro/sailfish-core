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

import com.exactpro.sf.util.DateTimeUtility;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TestNTGUtility {

    private final DateTimeFormatter formatter = DateTimeUtility.createFormatter("yyyyMMdd HH:mm:ss.nnnnnnnnn");

    private final long ntgDateTime = 1487331930790L;
    private final String dateTime = "20100507 15:32:54.000346";
    private final LocalDateTime localDateTime = LocalDateTime.parse(dateTime + "000", formatter);

    /**
     * Test method for {@link NTGUtility#getTransactTimeAsDate(long)}.
     */
    @Test
    public void testGetTransactTimeAsDateLong() {
        assertEquals(localDateTime, NTGUtility.getTransactTimeAsDate(ntgDateTime));

    }

    /**
     * Test method for {@link NTGUtility#getTransactTime(long)}.
     */
    @Test
    public void testGetTransactTimeLong() {
        assertEquals(dateTime, NTGUtility.getTransactTime(ntgDateTime));
    }

    /**
     * Test method for {@link NTGUtility#getTransactTime(java.lang.String)}.
     */
    @Test
    public void testGetTransactTimeString() {
        assertEquals(ntgDateTime, NTGUtility.getTransactTime(dateTime));
        assertEquals(ntgDateTime, NTGUtility.getTransactTime(localDateTime));
    }

    /**
     * Test method for {@link NTGUtility#getExpireDateTime(java.lang.String)}.
     */
    @Test
    public void testGetExpireDateTimeString() {
        long currentMillisecond = System.currentTimeMillis();

        int expected = (int) (currentMillisecond / 1000) + (60 * 60) - 20;

        assertEquals(expected, NTGUtility.getExpireDateTime(DateTimeUtility.toLocalDateTime(currentMillisecond), "h+1:s-20"));
    }

    @Test
    public void testGetSpecificExpireDateTime() {
        long currentMillisecond = System.currentTimeMillis();

        int expected = (int) (currentMillisecond / 1000);

        LocalDateTime localDateTime = DateTimeUtility.toLocalDateTime(currentMillisecond);

        assertEquals(expected, NTGUtility.getSpecificExpireDateTime(localDateTime.getYear(), localDateTime.getMonth().getValue(),
                localDateTime.getDayOfMonth(), localDateTime.getHour(), localDateTime.getMinute(), localDateTime.getSecond()));
    }
}
