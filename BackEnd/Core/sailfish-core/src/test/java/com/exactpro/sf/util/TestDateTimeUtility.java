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
package com.exactpro.sf.util;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Random;

import org.junit.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAccessor;

public class TestDateTimeUtility {

    private final long millisecondInDay = 24 * 60 * 60 * 1000;
    private final Random rnd = new Random();

    private final long millisecond = System.currentTimeMillis();
    private final int nanosecond = (int) ((millisecond % 1_000) * 1_000_000) + rnd.nextInt(999_999);

    private final Date date = new Date(millisecond);
    private final Date dateOnly = getDateOnly(millisecond);
    private final Date timeOnly = getTimeOnly(millisecond);
    private final LocalDateTime localDateTime = DateTimeUtility.toLocalDateTime(millisecond);
    private final LocalDate localDate = LocalDate.from(localDateTime);
    private final LocalTime localTime = LocalTime.from(localDateTime);

    private final Timestamp timestamp;
    private final Timestamp timestampDateOnly = getDateOnly(millisecond, nanosecond);
    private final Timestamp timestampTimeOnly = getTimeOnly(millisecond, nanosecond);
    private final LocalDateTime localDateTimeNano = DateTimeUtility.toLocalDateTime(millisecond, nanosecond);
    private final LocalDate localDateNano = LocalDate.from(localDateTimeNano);
    private final LocalTime localTimeNano = LocalTime.from(localDateTimeNano);

    {
        timestamp = new Timestamp(millisecond);
        timestamp.setNanos(nanosecond);
    }

    @Test
    public void testConvert() {
        assertEquals(DateTimeUtility.toLocalDateTime(localDateTime), localDateTime);
        assertEquals(DateTimeUtility.toLocalDateTime(millisecond, nanosecond), localDateTimeNano);
        assertEquals(DateTimeUtility.toLocalDateTime(millisecond), localDateTime);
        assertEquals(DateTimeUtility.toLocalDateTime(localDate), LocalDateTime.of(localDate, DateTimeUtility.MIN_TIME));
        assertEquals(DateTimeUtility.toLocalDateTime(localTime), LocalDateTime.of(DateTimeUtility.MIN_DATE, localTime));

        assertEquals(DateTimeUtility.toLocalDate(localDateTime), localDate);
        assertEquals(DateTimeUtility.toLocalDate(millisecond), localDate);
        assertEquals(DateTimeUtility.toLocalDate(localDate), localDate);
        assertEquals(DateTimeUtility.toLocalDate(localTime), DateTimeUtility.MIN_DATE);

        assertEquals(DateTimeUtility.toLocalTime(localDateTime), localTime);
        assertEquals(DateTimeUtility.toLocalTime(millisecond, nanosecond), localTimeNano);
        assertEquals(DateTimeUtility.toLocalTime(millisecond), localTime);
        assertEquals(DateTimeUtility.toLocalTime(localDate), DateTimeUtility.MIN_TIME);
        assertEquals(DateTimeUtility.toLocalTime(localTime), localTime);

        assertEquals(DateTimeUtility.toLocalDateTime(date), localDateTime);
        assertEquals(DateTimeUtility.toLocalDate(date), localDate);
        assertEquals(DateTimeUtility.toLocalTime(date), localTime);

        assertEquals(DateTimeUtility.toDate(localDateTime), date);
        assertEquals(DateTimeUtility.toDate((TemporalAccessor) localDateTime), date);
        assertEquals(DateTimeUtility.toDate(localDate), dateOnly);
        assertEquals(DateTimeUtility.toDate((TemporalAccessor) localDate), dateOnly);
        assertEquals(DateTimeUtility.toDate(localTime), timeOnly);
        assertEquals(DateTimeUtility.toDate((TemporalAccessor) localTime), timeOnly);

        assertEquals(DateTimeUtility.toLocalDateTime(timestamp), localDateTimeNano);
        assertEquals(DateTimeUtility.toLocalDate(timestamp), localDateNano);
        assertEquals(DateTimeUtility.toLocalTime(timestamp), localTimeNano);

        assertEquals(DateTimeUtility.toTimestamp(localDateTimeNano), timestamp);
        assertEquals(DateTimeUtility.toTimestamp((TemporalAccessor) localDateTimeNano), timestamp);
        assertEquals(DateTimeUtility.toTimestamp(localDateNano), timestampDateOnly);
        assertEquals(DateTimeUtility.toTimestamp((TemporalAccessor) localDateNano), timestampDateOnly);
        assertEquals(DateTimeUtility.toTimestamp(localTimeNano), timestampTimeOnly);
        assertEquals(DateTimeUtility.toTimestamp((TemporalAccessor) localTimeNano), timestampTimeOnly);

        assertEquals(DateTimeUtility.getMillisecond(localDateTime), millisecond);
        assertEquals(DateTimeUtility.getMillisecond(localDateTimeNano), millisecond);
        assertEquals(DateTimeUtility.getMillisecond(localDate), millisecond / (24 * 60 * 60 * 1_000) * (24 * 60 * 60 * 1_000));
    }

    private Date getDateOnly(long millisecond) {
        return new Date(extractDate(millisecond));
    }

    private Date getTimeOnly(long millisecond) {
        return new Date(extractTime(millisecond));
    }

    private Timestamp getDateOnly(long millisecond, int nanoseconds) {
        return new Timestamp(extractDate(millisecond));
    }

    private Timestamp getTimeOnly(long millisecond, int nanoseconds) {
        Timestamp result = new Timestamp(extractTime(millisecond));
        result.setNanos(nanoseconds);
        return result;
    }

    private long extractDate(long millisecond) {
        return millisecond / millisecondInDay * millisecondInDay;
    }

    private long extractTime(long millisecond) {
        return millisecond = millisecond % millisecondInDay;
    }
}
