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

import java.sql.Timestamp;
import java.util.Date;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;

public class DateTimeUtility {

    public static final LocalDate MIN_DATE = LocalDate.ofEpochDay(0);
    public static final LocalTime MIN_TIME = LocalTime.MIN;
    public static final LocalDateTime MIN_DATE_TIME = LocalDateTime.of(MIN_DATE, MIN_TIME);

    public static ZonedDateTime toZonedDateTime(TemporalAccessor temporalAccessor) {
        int year = getOrDefault(temporalAccessor, ChronoField.YEAR, MIN_DATE.getYear());
        int month = getOrDefault(temporalAccessor, ChronoField.MONTH_OF_YEAR, 1);
        int dayOfMonth = getOrDefault(temporalAccessor, ChronoField.DAY_OF_MONTH, 1);
        int hour = getOrDefault(temporalAccessor, ChronoField.HOUR_OF_DAY, 0);
        int minute = getOrDefault(temporalAccessor, ChronoField.MINUTE_OF_HOUR, 0);
        int second = getOrDefault(temporalAccessor, ChronoField.SECOND_OF_MINUTE, 0);
        int nanoOfSecond = getOrDefault(temporalAccessor, ChronoField.NANO_OF_SECOND, 0);

        ZoneId zoneId = getZoneId(temporalAccessor);

        ZonedDateTime zonedDateTime = ZonedDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond, zoneId);
        return zonedDateTime.withZoneSameInstant(ZoneOffset.UTC);
    }

    public static ZonedDateTime toZonedDateTime(LocalDate date, LocalTime time) {
        int year = getOrDefault(date, ChronoField.YEAR, MIN_DATE.getYear());
        int month = getOrDefault(date, ChronoField.MONTH_OF_YEAR, 1);
        int dayOfMonth = getOrDefault(date, ChronoField.DAY_OF_MONTH, 1);
        int hour = getOrDefault(time, ChronoField.HOUR_OF_DAY, 0);
        int minute = getOrDefault(time, ChronoField.MINUTE_OF_HOUR, 0);
        int second = getOrDefault(time, ChronoField.SECOND_OF_MINUTE, 0);
        int nanoOfSecond = getOrDefault(time, ChronoField.NANO_OF_SECOND, 0);

        ZoneId zoneId = getZoneId(time);

        ZonedDateTime zonedDateTime = ZonedDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond, zoneId);
        return zonedDateTime.withZoneSameInstant(ZoneOffset.UTC);
    }

    /**
     * Convert TemporalAccessor to LocalDateTime
     * 
     * @param temporal
     * @return LocalDateTime (UTC)
     */
    public static LocalDateTime toLocalDateTime(TemporalAccessor temporal) {
        ZonedDateTime zonedDateTime = toZonedDateTime(temporal);
        return zonedDateTime.toLocalDateTime();
    }

    /**
     * Convert java.util.Date to LocalDateTime in UTC time zone
     * 
     * @param date
     * @return LocalDateTime (UTC)
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        Instant instant = Instant.ofEpochMilli(date.getTime());
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    /**
     * Convert java.sql.Timestamp to LocalDateTime in UTC time zone
     * 
     * @param date
     * @return LocalDateTime (UTC)
     */
    public static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        long second = timestamp.getTime() / 1000;
        return LocalDateTime.ofEpochSecond(second, timestamp.getNanos(), ZoneOffset.UTC);
    }

    /**
     * Convert milliseconds to LocalDateTime in UTC time zone
     * @param millisecond
     * @return LocalDateTime (UTC)
     */
    public static LocalDateTime toLocalDateTime(long millisecond) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millisecond), ZoneOffset.UTC);
    }

    /**
     * Convert milliseconds and nanosecond to LocalDateTime in UTC time zone
     * 
     * @param millisecond
     * @param nanosecond
     * @return LocalDateTime (UTC)
     */
    public static LocalDateTime toLocalDateTime(long millisecond, int nanosecond) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millisecond), ZoneOffset.UTC).withNano(nanosecond);
    }

    /**
     * Merge a LocalTime and a LocalDate to a LocalDateTime in UTC time zone
     *
     * @param millisecond
     * @param nanosecond
     * @return LocalDateTime (UTC)
     */
    public static LocalDateTime toLocalDateTime(LocalDate date, LocalTime time) {
        ZonedDateTime zonedDateTime = toZonedDateTime(date, time);
        return zonedDateTime.toLocalDateTime();
    }

    /**
     * Convert TemporalAccessor to LocalDate
     * 
     * @param temporalAccessor
     * @return LocalDate (UTC)
     */
    public static LocalDate toLocalDate(TemporalAccessor temporal) {
        ZonedDateTime zonedDateTime = toZonedDateTime(temporal);
        return zonedDateTime.toLocalDate();
    }

    /**
     * Convert java.util.Date to LocalDate in UTC time zone
     * 
     * @param date
     * @return LocalDate (UTC)
     */
    public static LocalDate toLocalDate(Date date) {
        return toLocalDateTime(date).toLocalDate();
    }

    /**
     * Convert java.sql.Timestamp to LocalDate in UTC time zone
     * 
     * @param date
     * @return LocalDate (UTC)
     */
    public static LocalDate toLocalDate(Timestamp timestamp) {
        return toLocalDateTime(timestamp).toLocalDate();
    }

    /**
     * Convert milliseconds to LocalDate in UTC time zone
     * 
     * @param millisecond
     * @return LocalDate (UTC)
     */
    public static LocalDate toLocalDate(long millisecond) {
        return toLocalDateTime(millisecond).toLocalDate();
    }

    /**
     * Convert TemporalAccessor to LocalTime
     * 
     * @param date
     * @return LocalTime (UTC)
     */
    public static LocalTime toLocalTime(TemporalAccessor temporal) {
        ZonedDateTime zonedDateTime = toZonedDateTime(temporal);
        return zonedDateTime.toLocalTime();
    }

    /**
     * Convert java.util.Date to LocalTime in UTC time zone
     * 
     * @param date
     * @return LocalTime (UTC)
     */
    public static LocalTime toLocalTime(Date date) {
        return toLocalDateTime(date).toLocalTime();
    }

    /**
     * Convert java.sql.Timestamp to LocalTime in UTC time zone
     * 
     * @param date
     * @return LocalTime (UTC)
     */
    public static LocalTime toLocalTime(Timestamp timestamp) {
        return toLocalDateTime(timestamp).toLocalTime();
    }

    /**
     * Convert milliseconds to LocalTime in UTC time zone
     * 
     * @param millisecond
     * @return LocalTime (UTC)
     */
    public static LocalTime toLocalTime(long millisecond) {
        return toLocalDateTime(millisecond).toLocalTime();
    }

    /**
     * Convert milliseconds and nanosecond to LocalTime in UTC time zone
     * 
     * @param millisecond
     * @param nanosecond
     * @return LocalTime (UTC)
     */
    public static LocalTime toLocalTime(long millisecond, int nanosecond) {
        return toLocalDateTime(millisecond, nanosecond).toLocalTime();
    }

    /**
     * Convert TemporalAccessor to java.util.Date
     * 
     * @param localDateTime
     * @return java.util.Date
     */
    public static Date toDate(TemporalAccessor temporalAccessor) {
        return toDate(toLocalDateTime(temporalAccessor));
    }

    /**
     * Convert LocalDateTime to java.util.Date
     * 
     * @param localDateTime
     * @return java.util.Date
     */
    public static Date toDate(LocalDateTime localDateTime) {
        Instant instant = localDateTime.atZone(ZoneOffset.UTC).toInstant();
        return new Date(instant.toEpochMilli());
    }

    /**
     * Convert LocalDate to java.util.Date
     * 
     * @param localDate
     * @return java.util.Date
     */
    public static Date toDate(LocalDate localDate) {
        Instant instant = localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        return new Date(instant.toEpochMilli());
    }

    /**
     * Convert LocalTime to java.util.Date
     * 
     * @param localTime
     * @return java.util.Date
     */
    public static Date toDate(LocalTime localTime) {
        return toDate(localTime.atDate(MIN_DATE));
    }

    /**
     * Convert TemporalAccessor to java.sql.Timestamp
     * 
     * @param localDateTime
     * @return java.sql.Timestamp
     */
    public static Timestamp toTimestamp(TemporalAccessor temporalAccessor) {
        return toTimestamp(toLocalDateTime(temporalAccessor));
    }

    /**
     * Convert LocalDateTime to java.sql.Timestamp
     * 
     * @param localDateTime
     * @return java.sql.Timestamp
     */
    public static Timestamp toTimestamp(LocalDateTime localDateTime) {
        Instant instant = localDateTime.atZone(ZoneOffset.UTC).toInstant();
        return Timestamp.from(instant);
    }

    /**
     * Convert LocalDate to java.sql.Timestamp
     * 
     * @param localDate
     * @return java.sql.Timestamp
     */
    public static Timestamp toTimestamp(LocalDate localDate) {
        Instant instant = localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        return Timestamp.from(instant);
    }

    /**
     * Convert LocalTime to java.sql.Timestamp
     * 
     * @param localTime
     * @return java.sql.Timestamp
     */
    public static Timestamp toTimestamp(LocalTime localTime) {
        return toTimestamp(localTime.atDate(MIN_DATE));
    }

    /**
     * Create ZonedDateTime in specified zone id 
     * 
     * @param zoneId
     * @return
     */
    public static ZonedDateTime nowZonedDateTime(String zoneId) {
        return ZonedDateTime.now(ZoneId.of(zoneId));
    }

    /**
     * Create new ZonedDateTime in UTC time zone
     * 
     * @return ZonedDateTime (UTC)
     */
    public static ZonedDateTime nowZonedDateTime() {
        return ZonedDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Create new LocalDateTime in UTC time zone
     * 
     * @return LocalDateTime (UTC)
     */
    public static LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Create new LocalDate in UTC time zone
     * 
     * @return LocalDate (UTC)
     */
    public static LocalDate nowLocalDate() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    /**
     * Create new LocalTime in UTC time zone
     * 
     * @return LocalTime (UTC)
     */
    public static LocalTime nowLocalTime() {
        return LocalTime.now(ZoneOffset.UTC);
    }

    /**
     * Get millisecond from LocalDateTime in UTC time zone
     * 
     * @param localDateTime
     * @return the number of milliseconds since the epoch of
     *         1970-01-01T00:00:00Z
     */
    public static long getMillisecond(LocalDateTime localDateTime) {
        return localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    /**
     * Get millisecond from LocalDate in UTC time zone
     * 
     * @param localDate
     * @return the number of milliseconds since the epoch of
     *         1970-01-01T00:00:00Z
     */
    public static long getMillisecond(LocalDate localDate) {
        return localDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond() * 1_000;
    }

    /**
     * Create DateTimeFormatter with UTC time zone
     * 
     * @param pattern
     * @return
     */
    public static DateTimeFormatter createFormatter(String pattern) {
        return DateTimeFormatter.ofPattern(pattern).withZone(ZoneOffset.UTC);
    }

    private static int getOrDefault(TemporalAccessor temporalAccessor, TemporalField field, int defaultValue) {
        if (temporalAccessor.isSupported(field)) {
            return temporalAccessor.get(field);
        }
        return defaultValue;
    }

    private static ZoneId getZoneId(TemporalAccessor temporalAccessor) {
        ZoneId zoneId = temporalAccessor.query(TemporalQueries.offset());
        return zoneId != null ? zoneId : ZoneOffset.UTC;
    }
}
