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
package com.exactpro.sf.actions;

import static org.junit.Assert.assertEquals;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.exactpro.sf.actions.data.DateComponent;
import com.exactpro.sf.util.DateTimeUtility;

/**
 * @author alexey.suknatov
 *
 */
public class TestDateUtil {

    private DateUtil dateUtil;

    LocalDateTime sub = LocalDateTime.of(2000, 1, 2, 3, 4, 5, 106_107_108);

    @Before
    public void clean() {
        this.dateUtil = new DateUtil();
    }

    @Test
    public void testGetTimeByZoneId() {
        LocalTime calculatedTime = dateUtil.getTimeByZoneId("h=12:m=30:s=10:ns=123456789", "+03:01");
        LocalTime awaitingTime = LocalTime.of(9, 29, 10, 123456789);
        Assert.assertEquals(awaitingTime, calculatedTime);
    }

    @Test
    public void testGetDateByZoneId() {
        LocalDate calculatedDate = dateUtil.getDateByZoneId("Y=2019:M=3:D=13:h+0:m+0:s+0:ns+0", "+03:01");
        LocalDate awaitingDate = LocalDate.of(2019, 3, 13);
        Assert.assertEquals(awaitingDate, calculatedDate);
    }

    @Test
    public void testModifyDateTime() {
        LocalDateTime awaitingResult = LocalDateTime.of(2019, 3, 25, 8, 59, 0, 0);
        LocalDateTime awaitingResult2 = LocalDateTime.of(2019, 3, 23, 8, 59, 0, 0);
        LocalDateTime nowDate = LocalDateTime.of(2019, 3, 18, 12, 0, 0, 0);
        LocalDateTime result = ZonedDateTime.of(nowDate, ZoneId.of("+03:01")).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime result2 = ZonedDateTime.of(nowDate, ZoneId.of("+03:01")).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        result = dateUtil.modifyDateTime(result, "D+5", true);
        result2 = dateUtil.modifyDateTime(result2, "D+5", false);

        Assert.assertEquals(awaitingResult, result);
        Assert.assertEquals(awaitingResult2, result2);
    }

    @Test
    public void testToTimeByZoneId() {
        long time1 = 1552313123000L; //2019/03/11 14:05:23.000
        long time2 = 1554041123000L; //2019/03/31 14:05:23.000
        long time3 = 1572098723000L; //2019/10/26 14:05:23.000
        long time4 = 1572185123000L; //2019/10/27 14:05:23.000

        LocalTime awaitingTime = LocalTime.of(14, 5, 23);

        Assert.assertTrue(awaitingTime.equals(dateUtil.toTimeByZoneId(time1, "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London")));
        Assert.assertTrue(awaitingTime.equals(dateUtil.toTimeByZoneId(time2, "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London")));
        Assert.assertTrue(awaitingTime.equals(dateUtil.toTimeByZoneId(time3, "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London")));
        Assert.assertTrue(awaitingTime.equals(dateUtil.toTimeByZoneId(time4, "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London")));
    }

    @Test
    public void testToTimeByZoneId2() {
        LocalTime awaitingTime = LocalTime.of(14, 5, 23);

        Assert.assertTrue(awaitingTime.equals(dateUtil.toTimeByZoneId("20190311-14:05:23.507", "yyyyMMdd-HH:mm:ss.SSS", "Y+0:M+0:D+0:h+0:m+0:s+0:ns=0", "Europe/London")));
        Assert.assertTrue(awaitingTime.equals(dateUtil.toTimeByZoneId("20190331-14:05:23.507", "yyyyMMdd-HH:mm:ss.SSS", "Y+0:M+0:D+0:h+0:m+0:s+0:ns=0", "Europe/London")));
        Assert.assertTrue(awaitingTime.equals(dateUtil.toTimeByZoneId("20191026-14:05:23.507", "yyyyMMdd-HH:mm:ss.SSS", "Y+0:M+0:D+0:h+0:m+0:s+0:ns=0", "Europe/London")));
        Assert.assertTrue(awaitingTime.equals(dateUtil.toTimeByZoneId("20191027-14:05:23.507", "yyyyMMdd-HH:mm:ss.SSS", "Y+0:M+0:D+0:h+0:m+0:s+0:ns=0", "Europe/London")));

    }

    @Test
    public void testToTimeByZoneId3() {
        LocalTime awaitingTime = LocalTime.of(14, 5, 23, 0);

        LocalDateTime localDateTime1 = LocalDateTime.of(2019, 3, 11, 14, 5, 23, 0);
        LocalDateTime localDateTime2 = LocalDateTime.of(2019, 3, 31, 14, 5, 23, 0);
        LocalDateTime localDateTime3 = LocalDateTime.of(2019, 10, 26, 14, 5, 23, 0);
        LocalDateTime localDateTime4 = LocalDateTime.of(2019, 10, 27, 14, 5, 23, 0);

        Assert.assertEquals(awaitingTime, dateUtil.toTimeByZoneId(localDateTime1, "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London"));
        Assert.assertEquals(awaitingTime, dateUtil.toTimeByZoneId(localDateTime2, "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London"));
        Assert.assertEquals(awaitingTime, dateUtil.toTimeByZoneId(localDateTime3, "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London"));
        Assert.assertEquals(awaitingTime, dateUtil.toTimeByZoneId(localDateTime4, "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London"));
    }

    @Test
    public void testToDateByZoneId() {
        long time1 = 1552313123000L; //2019/03/11 14:05:23.000
        long time2 = 1554041123000L; //2019/03/31 14:05:23.000
        long time3 = 1572098723000L; //2019/10/26 14:05:23.000
        long time4 = 1572185123000L; //2019/10/27 14:05:23.000

        LocalDate awaitingTime1 = LocalDate.of(2019, 3, 11);
        LocalDate awaitingTime2 = LocalDate.of(2019, 3, 31);
        LocalDate awaitingTime3 = LocalDate.of(2019, 10, 26);
        LocalDate awaitingTime4 = LocalDate.of(2019, 10, 27);

        Assert.assertTrue(awaitingTime1.equals(dateUtil.toDateByZoneId(time1, "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London")));
        Assert.assertTrue(awaitingTime2.equals(dateUtil.toDateByZoneId(time2, "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London")));
        Assert.assertTrue(awaitingTime3.equals(dateUtil.toDateByZoneId(time3, "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London")));
        Assert.assertTrue(awaitingTime4.equals(dateUtil.toDateByZoneId(time4, "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London")));
    }

    @Test
    public void testToDateByZoneId2() {
        LocalDate awaitingTime1 = LocalDate.of(2019, 3, 11);
        LocalDate awaitingTime2 = LocalDate.of(2019, 3, 31);
        LocalDate awaitingTime3 = LocalDate.of(2019, 10, 26);
        LocalDate awaitingTime4 = LocalDate.of(2019, 10, 27);

        Assert.assertTrue(awaitingTime1.equals(dateUtil.toDateByZoneId("20190311-14:05:23.507", "yyyyMMdd-HH:mm:ss.SSS", "Y+0:M+0:D+0:h+0:m+0:s+0:ns=0", "Europe/London")));
        Assert.assertTrue(awaitingTime2.equals(dateUtil.toDateByZoneId("20190331-14:05:23.507", "yyyyMMdd-HH:mm:ss.SSS", "Y+0:M+0:D+0:h+0:m+0:s+0:ns=0", "Europe/London")));
        Assert.assertTrue(awaitingTime3.equals(dateUtil.toDateByZoneId("20191026-14:05:23.507", "yyyyMMdd-HH:mm:ss.SSS", "Y+0:M+0:D+0:h+0:m+0:s+0:ns=0", "Europe/London")));
        Assert.assertTrue(awaitingTime4.equals(dateUtil.toDateByZoneId("20191027-14:05:23.507", "yyyyMMdd-HH:mm:ss.SSS", "Y+0:M+0:D+0:h+0:m+0:s+0:ns=0", "Europe/London")));
    }

    @Test
    public void testToDateByZoneId3() {
        LocalDate awaitingTime1 = LocalDate.of(2019, 3, 11);
        LocalDate awaitingTime2 = LocalDate.of(2019, 3, 31);
        LocalDate awaitingTime3 = LocalDate.of(2019, 10, 26);
        LocalDate awaitingTime4 = LocalDate.of(2019, 10, 27);

        LocalDateTime localDateTime1 = LocalDateTime.of(2019, 3, 11, 14, 5, 23, 0);
        LocalDateTime localDateTime2 = LocalDateTime.of(2019, 3, 31, 14, 5, 23, 0);
        LocalDateTime localDateTime3 = LocalDateTime.of(2019, 10, 26, 14, 5, 23, 0);
        LocalDateTime localDateTime4 = LocalDateTime.of(2019, 10, 27, 14, 5, 23, 0);

        Assert.assertEquals(awaitingTime1, dateUtil.toDateByZoneId(localDateTime1, "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London"));
        Assert.assertEquals(awaitingTime2, dateUtil.toDateByZoneId(localDateTime2, "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London"));
        Assert.assertEquals(awaitingTime3, dateUtil.toDateByZoneId(localDateTime3, "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London"));
        Assert.assertEquals(awaitingTime4, dateUtil.toDateByZoneId(localDateTime4, "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London"));
    }

    @Test
    public void testToDateTimeByZoneId() {
        long time1 = 1552313123000L; //2019/03/11 14:05:23.000
        long time2 = 1554041123000L; //2019/03/31 14:05:23.000
        long time3 = 1572098723000L; //2019/10/26 14:05:23.000
        long time4 = 1572185123000L; //2019/10/27 14:05:23.000

        LocalDateTime awaitingTime1 = LocalDateTime.of(2019, 3, 11, 14, 5, 23);
        LocalDateTime awaitingTime2 = LocalDateTime.of(2019, 3, 31, 14, 5, 23);
        LocalDateTime awaitingTime3 = LocalDateTime.of(2019, 10, 26, 14, 5, 23);
        LocalDateTime awaitingTime4 = LocalDateTime.of(2019, 10, 27, 14, 5, 23);

        Assert.assertTrue(awaitingTime1.equals(dateUtil.toDateTimeByZoneId(time1, "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London")));
        Assert.assertTrue(awaitingTime2.equals(dateUtil.toDateTimeByZoneId(time2, "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London")));
        Assert.assertTrue(awaitingTime3.equals(dateUtil.toDateTimeByZoneId(time3, "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London")));
        Assert.assertTrue(awaitingTime4.equals(dateUtil.toDateTimeByZoneId(time4, "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London")));
    }

    @Test
    public void testToDateTimeByZoneId2() {
        LocalDateTime awaitingTime1 = LocalDateTime.of(2019, 3, 11, 14, 5, 23);
        LocalDateTime awaitingTime2 = LocalDateTime.of(2019, 3, 31, 14, 5, 23);
        LocalDateTime awaitingTime3 = LocalDateTime.of(2019, 10, 26, 14, 5, 23);
        LocalDateTime awaitingTime4 = LocalDateTime.of(2019, 10, 27, 14, 5, 23);

        Assert.assertTrue(awaitingTime1.equals(dateUtil.toDateTimeByZoneId("20190311-14:05:23.507", "yyyyMMdd-HH:mm:ss.SSS", "Y+0:M+0:D+0:h+0:m+0:s+0:ns=0", "Europe/London")));
        Assert.assertTrue(awaitingTime2.equals(dateUtil.toDateTimeByZoneId("20190331-14:05:23.507", "yyyyMMdd-HH:mm:ss.SSS", "Y+0:M+0:D+0:h+0:m+0:s+0:ns=0", "Europe/London")));
        Assert.assertTrue(awaitingTime3.equals(dateUtil.toDateTimeByZoneId("20191026-14:05:23.507", "yyyyMMdd-HH:mm:ss.SSS", "Y+0:M+0:D+0:h+0:m+0:s+0:ns=0", "Europe/London")));
        Assert.assertTrue(awaitingTime4.equals(dateUtil.toDateTimeByZoneId("20191027-14:05:23.507", "yyyyMMdd-HH:mm:ss.SSS", "Y+0:M+0:D+0:h+0:m+0:s+0:ns=0", "Europe/London")));
    }

    @Test
    public void testToDateTimeByZoneId3() {
        LocalDateTime awaitingTime = LocalDateTime.of(1970, 1, 1, 14, 5, 23);

        LocalDateTime localDateTime1 = LocalDateTime.of(2019, 3, 11, 14, 5, 23, 0);
        LocalDateTime localDateTime2 = LocalDateTime.of(2019, 3, 31, 14, 5, 23, 0);
        LocalDateTime localDateTime3 = LocalDateTime.of(2019, 10, 26, 14, 5, 23, 0);
        LocalDateTime localDateTime4 = LocalDateTime.of(2019, 10, 27, 14, 5, 23, 0);

        Assert.assertEquals(awaitingTime, dateUtil.toDateTimeByZoneId(localDateTime1.toLocalTime(), "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London"));
        Assert.assertEquals(awaitingTime, dateUtil.toDateTimeByZoneId(localDateTime2.toLocalTime(), "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London"));
        Assert.assertEquals(awaitingTime, dateUtil.toDateTimeByZoneId(localDateTime3.toLocalTime(), "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London"));
        Assert.assertEquals(awaitingTime, dateUtil.toDateTimeByZoneId(localDateTime4.toLocalTime(), "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London"));
    }

    @Test
    public void testToDateTimeByZoneId4() {
        LocalDateTime awaitingTime1 = LocalDateTime.of(2019, 3, 11, 0, 0, 0);
        LocalDateTime awaitingTime2 = LocalDateTime.of(2019, 4, 1, 0, 0, 0);
        LocalDateTime awaitingTime3 = LocalDateTime.of(2019, 10, 26, 0, 0, 0);
        LocalDateTime awaitingTime4 = LocalDateTime.of(2019, 10, 28, 0, 0, 0);

        LocalDateTime localDateTime1 = LocalDateTime.of(2019, 3, 11, 14, 5, 23, 0);
        LocalDateTime localDateTime2 = LocalDateTime.of(2019, 4, 1, 14, 5, 23, 0);
        LocalDateTime localDateTime3 = LocalDateTime.of(2019, 10, 26, 14, 5, 23, 0);
        LocalDateTime localDateTime4 = LocalDateTime.of(2019, 10, 28, 14, 5, 23, 0);

        Assert.assertEquals(awaitingTime1, dateUtil.toDateTimeByZoneId(localDateTime1.toLocalDate(), "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London"));
        Assert.assertEquals(awaitingTime2, dateUtil.toDateTimeByZoneId(localDateTime2.toLocalDate(), "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London"));
        Assert.assertEquals(awaitingTime3, dateUtil.toDateTimeByZoneId(localDateTime3.toLocalDate(), "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London"));
        Assert.assertEquals(awaitingTime4, dateUtil.toDateTimeByZoneId(localDateTime4.toLocalDate(), "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London"));
    }

    @Test
    public void testFormatTimeByZoneId() {
        String resultTime = "19700101-14:05:23.000";

        LocalDateTime localDateTime1 = LocalDateTime.of(2019, 3, 11, 14, 5, 23, 0);
        LocalDateTime localDateTime2 = LocalDateTime.of(2019, 4, 1, 14, 5, 23, 0);
        LocalDateTime localDateTime3 = LocalDateTime.of(2019, 10, 26, 14, 5, 23, 0);
        LocalDateTime localDateTime4 = LocalDateTime.of(2019, 10, 28, 14, 5, 23, 0);

        Assert.assertTrue(resultTime.equals(dateUtil.formatTimeByZoneId(localDateTime1.toLocalTime(), "yyyyMMdd-HH:mm:ss.SSS", "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London")));
        Assert.assertTrue(resultTime.equals(dateUtil.formatTimeByZoneId(localDateTime2.toLocalTime(), "yyyyMMdd-HH:mm:ss.SSS", "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London")));
        Assert.assertTrue(resultTime.equals(dateUtil.formatTimeByZoneId(localDateTime3.toLocalTime(), "yyyyMMdd-HH:mm:ss.SSS", "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London")));
        Assert.assertTrue(resultTime.equals(dateUtil.formatTimeByZoneId(localDateTime4.toLocalTime(), "yyyyMMdd-HH:mm:ss.SSS", "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London")));
    }

    @Test
    public void testFormatDateByZoneId() {
        String resultTime1 = "20190311-00:00:00.000";
        String resultTime2 = "20190401-00:00:00.000";
        String resultTime3 = "20191026-00:00:00.000";
        String resultTime4 = "20191028-00:00:00.000";

        LocalDateTime localDateTime1 = LocalDateTime.of(2019, 3, 11, 14, 5, 23, 0);
        LocalDateTime localDateTime2 = LocalDateTime.of(2019, 4, 1, 14, 5, 23, 0);
        LocalDateTime localDateTime3 = LocalDateTime.of(2019, 10, 26, 14, 5, 23, 0);
        LocalDateTime localDateTime4 = LocalDateTime.of(2019, 10, 28, 14, 5, 23, 0);

        Assert.assertTrue(resultTime1.equals(dateUtil.formatDateByZoneId(localDateTime1.toLocalDate(), "yyyyMMdd-HH:mm:ss.SSS", "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London")));
        Assert.assertTrue(resultTime2.equals(dateUtil.formatDateByZoneId(localDateTime2.toLocalDate(), "yyyyMMdd-HH:mm:ss.SSS", "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London")));
        Assert.assertTrue(resultTime3.equals(dateUtil.formatDateByZoneId(localDateTime3.toLocalDate(), "yyyyMMdd-HH:mm:ss.SSS", "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London")));
        Assert.assertTrue(resultTime4.equals(dateUtil.formatDateByZoneId(localDateTime4.toLocalDate(), "yyyyMMdd-HH:mm:ss.SSS", "Y+0:M+0:D+0:h+0:m+0:s+0:ns+0", "Europe/London")));
    }

    @Test
    public void testFormatDateTimeByZoneId() {
        LocalDateTime localDateTime1 = LocalDateTime.of(2019, 3, 11, 14, 5, 23, 0); // before summer time start | 2019-03-11T14:05:23 2019-03-11T14:05:23Z[Europe/London] 2019-03-11T14:05:23+03:00[Europe/Moscow]
        LocalDateTime localDateTime2 = LocalDateTime.of(2019, 3, 31, 14, 5, 23, 0); // start of summer time | 2019-03-31T14:05:23 2019-03-31T14:05:23+01:00[Europe/London] 2019-03-31T14:05:23+03:00[Europe/Moscow]
        LocalDateTime localDateTime3 = LocalDateTime.of(2019, 10, 26, 14, 5, 23, 0); // before summer time end | 2019-10-26T14:05:23 2019-10-26T14:05:23+01:00[Europe/London] 2019-10-26T14:05:23+03:00[Europe/Moscow]
        LocalDateTime localDateTime4 = LocalDateTime.of(2019, 10, 27, 14, 5, 23, 0); // end of summer time | 2019-10-27T14:05:23 2019-10-27T14:05:23Z[Europe/London] 2019-10-27T14:05:23+03:00[Europe/Moscow]

        Assert.assertEquals("20190311-14:05:23.000", dateUtil.formatDateTimeByZoneId(localDateTime1, "yyyyMMdd-HH:mm:ss.SSS", "h+0", "Europe/London"));
        Assert.assertEquals("20190331-14:05:23.000", dateUtil.formatDateTimeByZoneId(localDateTime2, "yyyyMMdd-HH:mm:ss.SSS", "h+0", "Europe/London"));
        Assert.assertEquals("20191026-14:05:23.000", dateUtil.formatDateTimeByZoneId(localDateTime3, "yyyyMMdd-HH:mm:ss.SSS", "h+0", "Europe/London"));
        Assert.assertEquals("20191027-14:05:23.000", dateUtil.formatDateTimeByZoneId(localDateTime4, "yyyyMMdd-HH:mm:ss.SSS", "h+0", "Europe/London"));
    }

    @Test
    public void testMergeDateTimeByZoneId() {
        LocalDateTime awaitingTime1 = LocalDateTime.of(2019, 3, 11, 14, 5, 23);
        LocalDateTime awaitingTime2 = LocalDateTime.of(2019, 3, 31, 14, 5, 23);
        LocalDateTime awaitingTime3 = LocalDateTime.of(2019, 10, 26, 14, 5, 23);
        LocalDateTime awaitingTime4 = LocalDateTime.of(2019, 10, 27, 14, 5, 23);

        LocalDateTime localDateTime1 = LocalDateTime.of(2019, 3, 11, 14, 5, 23, 0);
        LocalDateTime localDateTime2 = LocalDateTime.of(2019, 3, 31, 14, 5, 23, 0);
        LocalDateTime localDateTime3 = LocalDateTime.of(2019, 10, 26, 14, 5, 23, 0);
        LocalDateTime localDateTime4 = LocalDateTime.of(2019, 10, 27, 14, 5, 23, 0);

        Assert.assertTrue(awaitingTime1.equals(dateUtil.mergeDateTimeByZoneId(localDateTime1.toLocalDate(), localDateTime1.toLocalTime(), "h+0", "Europe/London")));
        Assert.assertTrue(awaitingTime2.equals(dateUtil.mergeDateTimeByZoneId(localDateTime2.toLocalDate(), localDateTime2.toLocalTime(), "h+0", "Europe/London")));
        Assert.assertTrue(awaitingTime3.equals(dateUtil.mergeDateTimeByZoneId(localDateTime3.toLocalDate(), localDateTime3.toLocalTime(), "h+0", "Europe/London")));
        Assert.assertTrue(awaitingTime4.equals(dateUtil.mergeDateTimeByZoneId(localDateTime4.toLocalDate(), localDateTime4.toLocalTime(), "h+0", "Europe/London")));
    }

    @Test
    public void testMergeDateTimeByZoneId2() {
        LocalDateTime awaitingTime1 = LocalDateTime.of(2019, 3, 11, 14, 5, 23);
        LocalDateTime awaitingTime2 = LocalDateTime.of(2019, 3, 31, 14, 5, 23);
        LocalDateTime awaitingTime3 = LocalDateTime.of(2019, 10, 26, 14, 5, 23);
        LocalDateTime awaitingTime4 = LocalDateTime.of(2019, 10, 27, 14, 5, 23);

        LocalDateTime localDateTime1 = LocalDateTime.of(2019, 3, 11, 14, 5, 23, 0);
        LocalDateTime localDateTime2 = LocalDateTime.of(2019, 3, 31, 14, 5, 23, 0);
        LocalDateTime localDateTime3 = LocalDateTime.of(2019, 10, 26, 14, 5, 23, 0);
        LocalDateTime localDateTime4 = LocalDateTime.of(2019, 10, 27, 14, 5, 23, 0);

        Assert.assertTrue(awaitingTime1.equals(dateUtil.mergeDateTimeByZoneId(localDateTime1.toLocalDate(), localDateTime1, "h+0", "Europe/London")));
        Assert.assertTrue(awaitingTime2.equals(dateUtil.mergeDateTimeByZoneId(localDateTime2.toLocalDate(), localDateTime2, "h+0", "Europe/London")));
        Assert.assertTrue(awaitingTime3.equals(dateUtil.mergeDateTimeByZoneId(localDateTime3.toLocalDate(), localDateTime3, "h+0", "Europe/London")));
        Assert.assertTrue(awaitingTime4.equals(dateUtil.mergeDateTimeByZoneId(localDateTime4.toLocalDate(), localDateTime4, "h+0", "Europe/London")));
    }

    @Test
    public void testMergeDateTimeByZoneId3() {
        LocalDateTime awaitingTime1 = LocalDateTime.of(2019, 3, 11, 14, 5, 23);
        LocalDateTime awaitingTime2 = LocalDateTime.of(2019, 3, 31, 14, 5, 23);
        LocalDateTime awaitingTime3 = LocalDateTime.of(2019, 10, 26, 14, 5, 23);
        LocalDateTime awaitingTime4 = LocalDateTime.of(2019, 10, 27, 14, 5, 23);

        LocalDateTime localDateTime1 = LocalDateTime.of(2019, 3, 11, 14, 5, 23, 0);
        LocalDateTime localDateTime2 = LocalDateTime.of(2019, 3, 31, 14, 5, 23, 0);
        LocalDateTime localDateTime3 = LocalDateTime.of(2019, 10, 26, 14, 5, 23, 0);
        LocalDateTime localDateTime4 = LocalDateTime.of(2019, 10, 27, 14, 5, 23, 0);

        Assert.assertTrue(awaitingTime1.equals(dateUtil.mergeDateTimeByZoneId(localDateTime1, localDateTime1.toLocalTime(), "h+0", "Europe/London")));
        Assert.assertTrue(awaitingTime2.equals(dateUtil.mergeDateTimeByZoneId(localDateTime2, localDateTime2.toLocalTime(), "h+0", "Europe/London")));
        Assert.assertTrue(awaitingTime3.equals(dateUtil.mergeDateTimeByZoneId(localDateTime3, localDateTime3.toLocalTime(), "h+0", "Europe/London")));
        Assert.assertTrue(awaitingTime4.equals(dateUtil.mergeDateTimeByZoneId(localDateTime4, localDateTime4.toLocalTime(), "h+0", "Europe/London")));
    }

    @Test
    public void testMergeDateTimeByZoneId4() {
        LocalDateTime awaitingTime1 = LocalDateTime.of(2019, 3, 11, 14, 5, 23);
        LocalDateTime awaitingTime2 = LocalDateTime.of(2019, 3, 31, 14, 5, 23);
        LocalDateTime awaitingTime3 = LocalDateTime.of(2019, 10, 26, 14, 5, 23);
        LocalDateTime awaitingTime4 = LocalDateTime.of(2019, 10, 27, 14, 5, 23);

        LocalDateTime localDateTime1 = LocalDateTime.of(2019, 3, 11, 14, 5, 23, 0);
        LocalDateTime localDateTime2 = LocalDateTime.of(2019, 3, 31, 14, 5, 23, 0);
        LocalDateTime localDateTime3 = LocalDateTime.of(2019, 10, 26, 14, 5, 23, 0);
        LocalDateTime localDateTime4 = LocalDateTime.of(2019, 10, 27, 14, 5, 23, 0);

        Assert.assertTrue(awaitingTime1.equals(dateUtil.mergeDateTimeByZoneId(localDateTime1, localDateTime1, "h+0", "Europe/London")));
        Assert.assertTrue(awaitingTime2.equals(dateUtil.mergeDateTimeByZoneId(localDateTime2, localDateTime2, "h+0", "Europe/London")));
        Assert.assertTrue(awaitingTime3.equals(dateUtil.mergeDateTimeByZoneId(localDateTime3, localDateTime3, "h+0", "Europe/London")));
        Assert.assertTrue(awaitingTime4.equals(dateUtil.mergeDateTimeByZoneId(localDateTime4, localDateTime4, "h+0", "Europe/London")));
    }

    @Test
    public void testModifyDateTimeByZoneId() {
        Assert.assertTrue("20190311-14:05:23".equals(dateUtil.modifyDateTimeByZoneId("2019-03-11 14:05:23.000", "yyyyMMdd-HH:mm:ss", "h+0", "Europe/London")));
        Assert.assertTrue("20190331-14:05:23".equals(dateUtil.modifyDateTimeByZoneId("2019-03-31 14:05:23.000", "yyyyMMdd-HH:mm:ss", "h+0", "Europe/London")));
        Assert.assertTrue("20191026-14:05:23".equals(dateUtil.modifyDateTimeByZoneId("2019-10-26 14:05:23.000", "yyyyMMdd-HH:mm:ss", "h+0", "Europe/London")));
        Assert.assertTrue("20191027-14:05:23".equals(dateUtil.modifyDateTimeByZoneId("2019-10-27 14:05:23.000", "yyyyMMdd-HH:mm:ss", "h+0", "Europe/London")));
    }

    @Test
    public void testModifyTimeByZoneId() {
        LocalDateTime awaitingTime = LocalDateTime.of(1970, 1, 1, 14, 5, 23);

        LocalDateTime localDateTime1 = LocalDateTime.of(2019, 3, 11, 14, 5, 23, 0);
        LocalDateTime localDateTime2 = LocalDateTime.of(2019, 3, 31, 14, 5, 23, 0);
        LocalDateTime localDateTime3 = LocalDateTime.of(2019, 10, 26, 14, 5, 23, 0);
        LocalDateTime localDateTime4 = LocalDateTime.of(2019, 10, 27, 14, 5, 23, 0);

        Assert.assertTrue(awaitingTime.toLocalTime().equals(dateUtil.modifyTimeByZoneId(localDateTime1.toLocalTime(), "h+0", "Europe/London")));
        Assert.assertTrue(awaitingTime.toLocalTime().equals(dateUtil.modifyTimeByZoneId(localDateTime2.toLocalTime(), "h+0", "Europe/London")));
        Assert.assertTrue(awaitingTime.toLocalTime().equals(dateUtil.modifyTimeByZoneId(localDateTime3.toLocalTime(), "h+0", "Europe/London")));
        Assert.assertTrue(awaitingTime.toLocalTime().equals(dateUtil.modifyTimeByZoneId(localDateTime4.toLocalTime(), "h+0", "Europe/London")));
    }

    @Test
    public void testModifyDateByZoneId() {
        LocalDateTime awaitingTime1 = LocalDateTime.of(2019, 3, 11, 14, 5, 23);
        LocalDateTime awaitingTime2 = LocalDateTime.of(2019, 3, 31, 14, 5, 23);
        LocalDateTime awaitingTime3 = LocalDateTime.of(2019, 10, 26, 14, 5, 23);
        LocalDateTime awaitingTime4 = LocalDateTime.of(2019, 10, 27, 14, 5, 23);

        LocalDateTime localDateTime1 = LocalDateTime.of(2019, 3, 11, 14, 5, 23, 0);
        LocalDateTime localDateTime2 = LocalDateTime.of(2019, 3, 31, 14, 5, 23, 0);
        LocalDateTime localDateTime3 = LocalDateTime.of(2019, 10, 26, 14, 5, 23, 0);
        LocalDateTime localDateTime4 = LocalDateTime.of(2019, 10, 27, 14, 5, 23, 0);

        Assert.assertTrue(awaitingTime1.toLocalDate().equals(dateUtil.modifyDateByZoneId(localDateTime1.toLocalDate(), "h+0", "Europe/London")));
        Assert.assertTrue(awaitingTime2.toLocalDate().equals(dateUtil.modifyDateByZoneId(localDateTime2.toLocalDate(), "h+0", "Europe/London")));
        Assert.assertTrue(awaitingTime3.toLocalDate().equals(dateUtil.modifyDateByZoneId(localDateTime3.toLocalDate(), "h+0", "Europe/London")));
        Assert.assertTrue(awaitingTime4.toLocalDate().equals(dateUtil.modifyDateByZoneId(localDateTime4.toLocalDate(), "h+0", "Europe/London")));
    }

    @Test
	public void dateDiffTest() {

        LocalDateTime min = LocalDateTime.of(2002, 1, 2, 3, 4, 5, 106_107_108);
        checkDiff(sub, min, "Y", 2l);

        min = LocalDateTime.of(2000, 3, 2, 3, 4, 5, 106_107_108);
        checkDiff(sub, min, "M", 2l);

        min = LocalDateTime.of(2000, 1, 4, 3, 4, 5, 106_107_108);
        checkDiff(sub, min, "D", 2l);

        min = LocalDateTime.of(2000, 1, 2, 5, 4, 5, 106_107_108);
        checkDiff(sub, min, "h", 2l);

        min = LocalDateTime.of(2000, 1, 2, 3, 6, 5, 106_107_108);
        checkDiff(sub, min, "m", 2l);

        min = LocalDateTime.of(2000, 1, 2, 3, 4, 7, 106_107_108);
        checkDiff(sub, min, "s", 2l);

        min = LocalDateTime.of(2000, 1, 2, 3, 4, 5, 108_107_108);
        checkDiff(sub, min, "ms", 2l);
	}

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

	@Test
    public void testIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unsupported date component " + "e");
        dateUtil.diffDateTime(sub, sub, "e");
	}

	@Test
    public void testNullPointerExceptionOnSecondArgement() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("The second argument is null");
        dateUtil.diffDateTime(sub, null, "D");
	}

    @Test
    public void testNullPointerExceptionOnFirstArgument() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("The first argument is null");
        dateUtil.diffDateTime(null, sub, "D");
    }

    @Test
    public void name() {
        DateTimeFormatter formatter = DateTimeUtility.createFormatter("yyyyMMdd-HH:mm:ss.SSS");
        LocalDateTime localDateTime = DateTimeUtility.nowLocalDateTime();
        System.out.println(localDateTime);
        ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
        System.out.println(LocalDateTime.parse("20170215-15:47:22.928", formatter));
        System.out.println(ZonedDateTime.parse("20170215-15:47:22.928", formatter));
        System.out.println(zonedDateTime);
        System.out.println(zonedDateTime.format(formatter));
        System.out.println(zonedDateTime.withZoneSameInstant(ZoneId.of("UTC")));
        System.out.println(zonedDateTime.format(formatter));

    }

    @Test
    public void testModifyCalendar() {
        LocalDateTime localDateTime = LocalDateTime.of(2005, 6, 7, 8, 9, 10, 11_000_000);

        Map<String, LocalDateTime> modificator = new HashMap<>();
        modificator.put("Y+1", LocalDateTime.of(2006, 6, 7, 8, 9, 10, 11_000_000));
        modificator.put("Y-1", LocalDateTime.of(2004, 6, 7, 8, 9, 10, 11_000_000));
        modificator.put("Y=2000", LocalDateTime.of(2000, 6, 7, 8, 9, 10, 11_000_000));
        modificator.put("M+1", LocalDateTime.of(2005, 7, 7, 8, 9, 10, 11_000_000));
        modificator.put("M-1", LocalDateTime.of(2005, 5, 7, 8, 9, 10, 11_000_000));
        modificator.put("M=8", LocalDateTime.of(2005, 8, 7, 8, 9, 10, 11_000_000));
        modificator.put("D+1", LocalDateTime.of(2005, 6, 8, 8, 9, 10, 11_000_000));
        modificator.put("D-1", LocalDateTime.of(2005, 6, 6, 8, 9, 10, 11_000_000));
        modificator.put("D=5", LocalDateTime.of(2005, 6, 5, 8, 9, 10, 11_000_000));
        modificator.put("h+1", LocalDateTime.of(2005, 6, 7, 9, 9, 10, 11_000_000));
        modificator.put("h-1", LocalDateTime.of(2005, 6, 7, 7, 9, 10, 11_000_000));
        modificator.put("h=6", LocalDateTime.of(2005, 6, 7, 6, 9, 10, 11_000_000));
        modificator.put("m+1", LocalDateTime.of(2005, 6, 7, 8, 10, 10, 11_000_000));
        modificator.put("m-1", LocalDateTime.of(2005, 6, 7, 8, 8, 10, 11_000_000));
        modificator.put("m=7", LocalDateTime.of(2005, 6, 7, 8, 7, 10, 11_000_000));
        modificator.put("s+1", LocalDateTime.of(2005, 6, 7, 8, 9, 11, 11_000_000));
        modificator.put("s-1", LocalDateTime.of(2005, 6, 7, 8, 9, 9, 11_000_000));
        modificator.put("s=7", LocalDateTime.of(2005, 6, 7, 8, 9, 7, 11_000_000));
        modificator.put("ms+1", LocalDateTime.of(2005, 6, 7, 8, 9, 10, 12_000_000));
        modificator.put("ms-1", LocalDateTime.of(2005, 6, 7, 8, 9, 10, 10_000_000));
        modificator.put("ms=7", LocalDateTime.of(2005, 6, 7, 8, 9, 10, 7_000_000));
        modificator.put("mc+1", LocalDateTime.of(2005, 6, 7, 8, 9, 10, 11_001_000));
        modificator.put("mc-1", LocalDateTime.of(2005, 6, 7, 8, 9, 10, 10_999_000));
        modificator.put("mc=1", LocalDateTime.of(2005, 6, 7, 8, 9, 10, 1_000));
        modificator.put("ns+1", LocalDateTime.of(2005, 6, 7, 8, 9, 10, 11_000_001));
        modificator.put("ns-1", LocalDateTime.of(2005, 6, 7, 8, 9, 10, 10_999_999));
        modificator.put("ns=1", LocalDateTime.of(2005, 6, 7, 8, 9, 10, 1));

        for (Entry<String, LocalDateTime> pattern : modificator.entrySet()) {
            assertEquals("ModifyCalendar " + pattern.getKey(), DateUtil.modifyTemporal(localDateTime, pattern.getKey()), pattern.getValue());
        }
    }

    @Test
    public void testModifyDate() {
        LocalDate localDate = LocalDate.of(2005, 6, 7);

        Map<String, LocalDate> modificator = new HashMap<>();
        modificator.put("Y+1", LocalDate.of(2006, 6, 7));
        modificator.put("Y-1", LocalDate.of(2004, 6, 7));
        modificator.put("Y=2000", LocalDate.of(2000, 6, 7));
        modificator.put("M+1", LocalDate.of(2005, 7, 7));
        modificator.put("M-1", LocalDate.of(2005, 5, 7));
        modificator.put("M=8", LocalDate.of(2005, 8, 7));
        modificator.put("D+1", LocalDate.of(2005, 6, 8));
        modificator.put("D-1", LocalDate.of(2005, 6, 6));
        modificator.put("D=5", LocalDate.of(2005, 6, 5));

        for (Entry<String, LocalDate> pattern : modificator.entrySet()) {
            assertEquals("ModifyDate " + pattern.getKey(), DateUtil.modifyTemporal(localDate, pattern.getKey()), pattern.getValue());
        }
    }

    @Test
    public void testModifyTime() {
        LocalTime localTime = LocalTime.of(8, 9, 10, 11_000_000);

        Map<String, LocalTime> modificator = new HashMap<>();
        modificator.put("h+1", LocalTime.of(9, 9, 10, 11_000_000));
        modificator.put("h-1", LocalTime.of(7, 9, 10, 11_000_000));
        modificator.put("h=6", LocalTime.of(6, 9, 10, 11_000_000));
        modificator.put("m+1", LocalTime.of(8, 10, 10, 11_000_000));
        modificator.put("m-1", LocalTime.of(8, 8, 10, 11_000_000));
        modificator.put("m=7", LocalTime.of(8, 7, 10, 11_000_000));
        modificator.put("s+1", LocalTime.of(8, 9, 11, 11_000_000));
        modificator.put("s-1", LocalTime.of(8, 9, 9, 11_000_000));
        modificator.put("s=7", LocalTime.of(8, 9, 7, 11_000_000));
        modificator.put("ms+1", LocalTime.of(8, 9, 10, 12_000_000));
        modificator.put("ms-1", LocalTime.of(8, 9, 10, 10_000_000));
        modificator.put("ms=7", LocalTime.of(8, 9, 10, 7_000_000));
        modificator.put("mc+1", LocalTime.of(8, 9, 10, 11_001_000));
        modificator.put("mc-1", LocalTime.of(8, 9, 10, 10_999_000));
        modificator.put("mc=1", LocalTime.of(8, 9, 10, 1_000));
        modificator.put("ns+1", LocalTime.of(8, 9, 10, 11_000_001));
        modificator.put("ns-1", LocalTime.of(8, 9, 10, 10_999_999));
        modificator.put("ns=1", LocalTime.of(8, 9, 10, 1));

        for (Entry<String, LocalTime> pattern : modificator.entrySet()) {
            assertEquals("ModifyTime " + pattern.getKey(), DateUtil.modifyTemporal(localTime, pattern.getKey()), pattern.getValue());
        }
    }

    @Test
    public void getDateTimeByZoneId() {
        for (int hoursDiff = -1; hoursDiff <= 1; hoursDiff += 1) {
            for (int minutsDiff = -10; minutsDiff <= 10; minutsDiff += 10) {
                
                int defaultOffsetSeconds = ZonedDateTime.now().getOffset().getTotalSeconds();
                String unsignedOffset = LocalTime.ofSecondOfDay(Math.abs(defaultOffsetSeconds))
                        .plusHours(hoursDiff)
                        .plusMinutes(minutsDiff)
                        .format(DateTimeFormatter.ofPattern("HH:mm"));
                String signedOffset = (defaultOffsetSeconds < 0 ? '-' : '+') +  unsignedOffset;
                
                // Create date type using modify pattern for UTC time zone
                LocalDateTime dateTime = dateUtil.getDateTime("Y=2018:M=3:D=7:s=10:ns=123456789" +
                        // Apply shift between time zone 
                        ":h=" + (12 - hoursDiff) + 
                        ":m=" + (30 - minutsDiff))
                        .minusSeconds(defaultOffsetSeconds);
                LocalDateTime dateTimeByZoneId = dateUtil.getDateTimeByZoneId("Y=2018:M=3:D=7:h=12:m=30:s=10:ns=123456789", signedOffset);

                assertEquals("Diff hours = " + hoursDiff + ", minuts = " + minutsDiff, dateTime, dateTimeByZoneId);
            }
        }
        
    }
    
    @Test(expected=DateTimeException.class)
    public void getDateTimeByZoneIdUpperBound() {
        System.out.println(dateUtil.getDateTimeByZoneId("Y=2018:M=3:D=7:h=12:m=30:s=10:ns=123456789", "+18:01"));
    }
    
    @Test(expected=DateTimeException.class)
    public void getDateTimeByZoneIdDownBound() {
        System.out.println(dateUtil.getDateTimeByZoneId("Y=2018:M=3:D=7:h=12:m=30:s=10:ns=123456789", "-18:01"));
    }
    
    @Test
    public void testGetDate() {
        LocalDateTime currentDate = DateTimeUtility.nowLocalDateTime();

        String timePattern = String.format(":h=%s:m=%s:s=%s:ns=%s",
                currentDate.getHour(), currentDate.getMinute(), currentDate.getSecond(), currentDate.getNano());

        int dayOfWeek = currentDate.getDayOfWeek().getValue();
        int toSaturday = DayOfWeek.SATURDAY.getValue() < dayOfWeek
                ? DayOfWeek.SATURDAY.getValue()
                : DayOfWeek.SATURDAY.getValue() - dayOfWeek;
        int toSunday = DayOfWeek.SUNDAY.getValue() - dayOfWeek;

        if (toSaturday == 0 || toSunday == 0) {
            timePattern = timePattern + ":ms+5000";
        }

        // add with skipping
        LocalDateTime modified = dateUtil.getDateTime("D+" + toSaturday + timePattern, true);
        assertEquals(modified.getDayOfWeek(), DayOfWeek.MONDAY);
        assertEquals(toSaturday + 2, ChronoUnit.DAYS.between(currentDate, modified));

        modified = dateUtil.getDateTime("D+" + toSunday + timePattern, true);
        assertEquals(modified.getDayOfWeek(), DayOfWeek.MONDAY);
        assertEquals(toSunday + 1, ChronoUnit.DAYS.between(currentDate, modified));

        // add without skipping
        modified = dateUtil.getDateTime("D+" + toSaturday + timePattern, false);
        assertEquals(modified.getDayOfWeek(), DayOfWeek.SATURDAY);
        assertEquals(toSaturday, ChronoUnit.DAYS.between(currentDate, modified));

        modified = dateUtil.getDateTime("D+" + toSunday + timePattern, false);
        assertEquals(modified.getDayOfWeek(), DayOfWeek.SUNDAY);
        assertEquals(toSunday, ChronoUnit.DAYS.between(currentDate, modified));

        // subtract with skipping
        int toSaturdayDown = (toSaturday == DayOfWeek.SATURDAY.getValue())
                ? 1
                : DayOfWeek.SUNDAY.getValue() - toSaturday;
        int toSundayDown = DayOfWeek.SUNDAY.getValue() - toSunday;

        if (toSaturday == 0) {
            toSaturdayDown = toSaturday;
            timePattern = timePattern.substring(0, timePattern.length() - ":ms+5000".length());
        }
        if (toSunday == 0) {
            toSundayDown = toSunday;
            timePattern = timePattern.substring(0, timePattern.length() - ":ms+5000".length());
        }

        modified = dateUtil.getDateTime("D-" + toSaturdayDown + timePattern, true);
        assertEquals(modified.getDayOfWeek(), DayOfWeek.FRIDAY);
        assertEquals(toSaturdayDown + 1, ChronoUnit.DAYS.between(modified, currentDate));

        modified = dateUtil.getDateTime("D-" + toSundayDown + timePattern, true);
        assertEquals(modified.getDayOfWeek(), DayOfWeek.FRIDAY);
        assertEquals(toSundayDown + 2, ChronoUnit.DAYS.between(modified, currentDate));

        // subtract without skipping
        modified = dateUtil.getDateTime("D-" + toSaturdayDown + timePattern, false);
        assertEquals(modified.getDayOfWeek(), DayOfWeek.SATURDAY);
        assertEquals(toSaturdayDown, ChronoUnit.DAYS.between(modified, currentDate));

        modified = dateUtil.getDateTime("D-" + toSundayDown + timePattern, false);
        assertEquals(modified.getDayOfWeek(), DayOfWeek.SUNDAY);
        assertEquals(toSundayDown, ChronoUnit.DAYS.between(modified, currentDate));
    }

    @Test
    public void testModifyWithTimezone() {
	    //no DST adjusting
        TimeZone withoutDst = Arrays.stream(TimeZone.getAvailableIDs()).map(TimeZone::getTimeZone)
                .filter(z -> !z.useDaylightTime() && z.getRawOffset() != 0)
                .findFirst().orElseThrow(() -> new IllegalArgumentException("no time zone without dst support was found"));

        LocalDateTime base = LocalDate.now().atTime(12, 0);
        LocalTime compareTo = LocalTime.of(12, 0, 0, 0);
        for (Month month : Month.values()) {
            LocalTime currentNoDst = dateUtil.modifyDateTimeByZoneId(base, String.format("M=%d", month.getValue()), withoutDst.getID()).toLocalTime();
            assertEquals(currentNoDst, compareTo);
        }

        //with DST adjusting
        TimeZone withDst = Arrays.stream(TimeZone.getAvailableIDs()).map(TimeZone::getTimeZone)
                .filter(z -> z.useDaylightTime() && z.getRawOffset() != 0)
                .findFirst().orElseThrow(() -> new IllegalArgumentException("no time zone with dst support was found"));

        compareTo = dateUtil.modifyDateTimeByZoneId(base, "M=1", withDst.getID()).toLocalTime();

        int timeChangeCount = 0;
        for (Month month : Month.values()) {
            LocalTime currentWithDst = dateUtil.modifyDateTimeByZoneId(base, String.format("M=%d", month.getValue()), withDst.getID()).toLocalTime();
            if (!compareTo.equals(currentWithDst)) {
                timeChangeCount++;
                compareTo = currentWithDst;
            }
        }
        assertEquals(timeChangeCount, 2);

    }

    @Test
    public void testBusinessGetDate() {

        LocalDateTime currentDate = DateTimeUtility.nowLocalDateTime();

        String timePattern = String.format(":h=%s:m=%s:s=%s:ns=%s",
                currentDate.getHour(), currentDate.getMinute(), currentDate.getSecond(), currentDate.getNano());

        int dayOfWeek = currentDate.getDayOfWeek().getValue();
        int toSaturday = DayOfWeek.SATURDAY.getValue() < dayOfWeek
                ? DayOfWeek.SATURDAY.getValue()
                : DayOfWeek.SATURDAY.getValue() - dayOfWeek;
        int toSunday = DayOfWeek.SUNDAY.getValue() - dayOfWeek;

        if (toSaturday == 0) {
            timePattern = timePattern + ":ms+5000";
        }

        // subtract with skipping
        int toSaturdayDown = (toSaturday == DayOfWeek.SATURDAY.getValue())
                ? 1
                : DayOfWeek.SUNDAY.getValue() - toSaturday;
        int toSundayDown = DayOfWeek.SUNDAY.getValue() - toSunday;

        DayOfWeek expected = toSunday == 0 ? DayOfWeek.TUESDAY : DayOfWeek.MONDAY;
        // add with skipping
        LocalDateTime modified = dateUtil.getBusinessDateTime("D+" + toSaturday + timePattern);
        assertEquals(modified.getDayOfWeek(), expected);

        expected = toSunday == 0 ? DayOfWeek.FRIDAY : DayOfWeek.TUESDAY;
        modified = dateUtil.getBusinessDateTime("D+" + toSunday + timePattern);
        assertEquals(modified.getDayOfWeek(), expected);

        // if saturday or sunday
        expected = DayOfWeek.THURSDAY;
        if (toSaturday == 0) {
            expected = DayOfWeek.WEDNESDAY;
            timePattern = timePattern.substring(timePattern.length() - ":ms+5000".length());
        }

        // subtract with skipping
        modified = dateUtil.getBusinessDateTime("D-" + toSaturdayDown + timePattern);
        assertEquals(modified.getDayOfWeek(), expected);

        expected = DayOfWeek.FRIDAY;
        if (toSaturday == 0) {
            expected = DayOfWeek.THURSDAY;
        } else if (toSunday == 0) {
            expected = DayOfWeek.WEDNESDAY;
        }

        modified = dateUtil.getBusinessDateTime("D-" + toSundayDown + timePattern);
        assertEquals(modified.getDayOfWeek(), expected);
    }

    @Test
    public void testCreateAndModifyAndFormat() {

        LocalDateTime dateTime = dateUtil.getDateTime("Y=2025:M=2:D=13:h=8:m=24:s=5:ns=111222333");

        // getLocalDate
        LocalDate date = dateUtil.getDate("Y=2025:M=2:D=13:h=8:m=24:s=5:ns=111222333");
        String dateString = dateUtil.formatDate(date, "yyyyMMdd");
        assertEquals("20250213", dateString);

        // getLocalTime
        LocalTime time = dateUtil.getTime("Y=2025:M=2:D=13:h=8:m=24:s=5:ns=111222333");
        dateString = dateUtil.formatTime(time, "HH:mm:ss.SSSSSSSSS");
        assertEquals("08:24:05.111222333", dateString);

        // toLocalDate with modification
        date = dateUtil.toDate(dateTime, "Y+1:M-1:D+3:h-1:m=30:s=11:ms=777:mc=666:ns+222");
        dateString = dateUtil.formatDate(date, "yyyyMMdd");
        assertEquals("20260116", dateString);

        // toLocalDate without modification
        date = dateUtil.toDate(dateTime);
        dateString = dateUtil.formatDate(date, "yyyyMMdd");
        assertEquals("20250213", dateString);

        // toLocalTime with modification
        time = dateUtil.toTime(dateTime, "Y+1:M-1:D+3:h-1:m=30:s=11:ns=777666555");
        dateString = dateUtil.formatTime(time, "HH:mm:ss.SSSSSSSSS");
        assertEquals("07:30:11.777666555", dateString);

        // toLocalTime without modification
        time = dateUtil.toTime(dateTime);
        dateString = dateUtil.formatTime(time, "HH:mm:ss.SSSSSSSSS");
        assertEquals("08:24:05.111222333", dateString);

        // toLocalDateTime by merging a LocalDate and LocalTime (with modification)
        LocalDateTime modifiedDateTime =
                dateUtil.mergeDateTime(date, time, "Y+1:M-1:D+3:h-1:m=30:s=11:ns=777666555");
        dateString =
                dateUtil.formatDateTime(modifiedDateTime, "yyyyMMdd-HH:mm:ss.SSSSSSSSS");
        assertEquals("20260116-07:30:11.777666555", dateString);

        // toLocalDateTime by merging a LocalDate and LocalTime (without modification)
        modifiedDateTime = dateUtil.mergeDateTime(date, time);
        dateString = dateUtil.formatDateTime(modifiedDateTime, "yyyyMMdd-HH:mm:ss.SSSSSSSSS");
        assertEquals("20250213-08:24:05.111222333", dateString);

        // toLocalDateTime from LocalDate with modification
        modifiedDateTime = dateUtil.toDateTime(date, "Y+1:M-1:D+3:h+1:m=30:s=11:ns=777666555");
        dateString = dateUtil.formatDateTime(modifiedDateTime, "yyyyMMdd-HH:mm:ss.SSSSSSSSS");
        assertEquals("20260116-01:30:11.777666555", dateString);

        // toLocalDateTime from LocalDate without modification
        modifiedDateTime = dateUtil.toDateTime(date);
        dateString = dateUtil.formatDateTime(modifiedDateTime, "yyyyMMdd-HH:mm:ss.SSSSSSSSS");
        assertEquals("20250213-00:00:00.000000000", dateString);

        // toLocalDateTime from LocalTime with modification
        modifiedDateTime = dateUtil.toDateTime(time, "Y+1:M+1:D+3:h+1:m=30:s=11:ns=777666555");
        dateString = dateUtil.formatDateTime(modifiedDateTime, "yyyyMMdd-HH:mm:ss.SSSSSSSSS");
        assertEquals("19710204-09:30:11.777666555", dateString);

        // toLocalDateTime from LocalTime without modification
        modifiedDateTime = dateUtil.toDateTime(time);
        dateString = dateUtil.formatDateTime(modifiedDateTime, "yyyyMMdd-HH:mm:ss.SSSSSSSSS");
        assertEquals("19700101-08:24:05.111222333", dateString);
    }

    @Test
    public void testDifferenceBetweenTwoDates() {
        LocalDateTime dateTime = dateUtil.getDateTime("Y=2025:M=2:D=13:h=8:m=24:s=5:ns=111222333");

        LocalDateTime modifiedDateTime = dateUtil.modifyDateTime(dateTime, "D+3" );
        assertEquals(3, dateUtil.diffDateTime(modifiedDateTime, dateTime, "D"));

        modifiedDateTime = dateUtil.modifyDateTime(dateTime, "h+3" );
        assertEquals(3, dateUtil.diffDateTime(modifiedDateTime, dateTime, "h"));

        modifiedDateTime = dateUtil.modifyDateTime(dateTime, "h+1:m+1" );
        assertEquals(61, dateUtil.diffDateTime(modifiedDateTime, dateTime, "m"));

        modifiedDateTime = dateUtil.modifyDateTime(dateTime, "m+1:s+1" );
        assertEquals(61, dateUtil.diffDateTime(modifiedDateTime, dateTime, "s"));

        modifiedDateTime = dateUtil.modifyDateTime(dateTime, "s+1:ms+1" );
        assertEquals(1001, dateUtil.diffDateTime(modifiedDateTime, dateTime, "ms"));
    }

    @Test
    public void testDifferenceBetweenTwoZonedDates() {
        String[] dates = {
                "2011-12-03T10:15:30.123456789+01:00[Europe/Paris]",
                "2011-12-03T11:15:30.123456789+02:00",
                "2011-12-03T11:45:30.123456789+02:30",
                "2011-12-03T09:15:30.123456789Z"
        };

        for (String minuend : dates) {
            for (String subtrahend : dates) {
                if ( minuend != subtrahend) {
                    for (DateComponent dateComponent : DateComponent.values()) {
                        assertEquals("Minuend = " + minuend + " subtrahend = " + subtrahend + " date component = " + dateComponent,
                                0, dateUtil.diffDateTimeISO(minuend, subtrahend, dateComponent.toString()));
                    }
                }
            }
        }

        assertEquals(1001, dateUtil.diffDateTimeISO("2011-12-03T11:45:30.123456789+02:30", "2011-12-03T10:15:30.122455789+01:00[Europe/Paris]", "mc"));
    }

    @Test
    public void testMergeDateTimes() {
        LocalDateTime dateTime =
                dateUtil.getDateTime("Y=2025:M=5:D=1:h=8:m=24:s=5:ns=111222333");
        LocalDate date =
                dateUtil.getDate("Y=2010:M=3:D=14:h=8:m=24:s=5:ns=111222333");
        LocalTime time =
                dateUtil.getTime("Y=2025:M=5:D=1:h=15:m=20:s=5:ns=777666555");

        LocalDateTime expectedFromDateTime = dateUtil.getDateTime("Y=2025:M=5:D=1:h=15:m=20:s=5:ns=777666555");
        LocalDateTime expectedFromDate = dateUtil.getDateTime("Y=2010:M=3:D=14:h=15:m=20:s=5:ns=777666555");

        // LocalDateTime + LocalDate
        LocalDateTime modifiedDateTime = dateUtil.mergeDateTime(dateTime, time);
        assertEquals(expectedFromDateTime, modifiedDateTime);

        // LocalDateTime + LocalDateTime
        modifiedDateTime = dateUtil.mergeDateTime(dateTime, dateUtil.toDateTime(time));
        assertEquals(expectedFromDateTime, modifiedDateTime);

        // LocalDate + LocalTime
        modifiedDateTime = dateUtil.mergeDateTime(date, time);
        assertEquals(expectedFromDate, modifiedDateTime);

        // LocalDate + LocalDateTime
        modifiedDateTime = dateUtil.mergeDateTime(date, dateUtil.toDateTime(time));
        assertEquals(expectedFromDate, modifiedDateTime);
    }

    @Test
    public void testToDate() throws ParseException {
        List<String> patterns = new ArrayList<>();
        patterns.add("yyyy-MM-dd HH:mm:ss.SSS Z");
        patterns.add("yyyy-MM-dd HH:mm:ss.SSS");
        patterns.add("yyyy-MM-dd HH:mm:ss");
        patterns.add("yyyy-MM-dd HH:mm");
        patterns.add("yyyy-MM-dd HH");
        patterns.add("yyyy-MM-dd");
        patterns.add("yyyy-MM");
        patterns.add("yyyy");

        Date originDate = new Date();
        // origin date without truncating
        String stringOriginDate = new SimpleDateFormat(patterns.get(0)).format(originDate);

        for (String pattern : patterns) {
            DateFormat dateFormat = new SimpleDateFormat(pattern);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date dateToCompare = dateFormat.parse(stringOriginDate);
            String stringDateToCompare = new SimpleDateFormat(pattern).format(originDate);

            assertEquals(stringOriginDate, DateTimeUtility.toDate(dateUtil.toDateTime(stringDateToCompare)), dateToCompare);
        }

        String partDate = "2017-02-24 12:22:34.156";
        LocalDateTime localDateTime = LocalDateTime.of(2017, 2, 24, 12, 22, 34, 156_000_000);

        for (int i = -300; i <= 300; i += 100) {
            String value = String.format("%s %+05d", partDate, i);
            assertEquals(value, localDateTime.plusHours(i / -100), dateUtil.toDateTime(value));
        }

        String dateValue = "2000-90-90 90:00:00 -0700";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unsupported date format " + dateValue);
        dateUtil.toDateTime(dateValue);
    }

    @Test
    public void testToDateWithoutDelimiters() throws ParseException {
        List<String> patterns = new ArrayList<>();
        patterns.add("yyyy MMddHHmmssSSSSSS");
        patterns.add("yyyyMMddHHmmssSSSSSS");
        patterns.add("yyyyMMddHH'T'mmssSSSSSS");
        patterns.add("MMddyyyyHHmmssSSSSSS");
        patterns.add("MMddHHmmssSSSSSSyyyy");

        LocalDateTime current = DateTimeUtility.nowLocalDateTime();

        for (String pattern : patterns) {
            DateTimeFormatter formatter = DateTimeUtility.createFormatter(pattern);
            LocalDateTime dateTime = dateUtil.toDateTime(formatter.format(current), pattern);
            assertEquals(pattern, current, dateTime);
        }
    }

    @Test
    public void testNullPointerException() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Date argument is null");
        dateUtil.toDateTime((String)null);
    }

    @Test
    public void testToDateTimeIllegalArgumentException() {
        String dateValue = "2000-10-10 ss:00:00.sss -0700";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Incorrect date value " + dateValue);
        dateUtil.toDateTime(dateValue);
    }

    private void checkDiff(LocalDateTime subtrahend, LocalDateTime minuend, String dateComponent, long diff) {
        assertEquals(dateUtil.diffDateTime(minuend, subtrahend, dateComponent), diff);
        assertEquals(dateUtil.diffDateTime(subtrahend, minuend, dateComponent), -diff);
    }
}
