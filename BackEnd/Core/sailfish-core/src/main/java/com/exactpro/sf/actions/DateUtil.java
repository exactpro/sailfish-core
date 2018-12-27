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


import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.actions.data.DateComponent;
import com.exactpro.sf.actions.data.DateModificator;
import com.exactpro.sf.aml.Description;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityMethod;
import com.exactpro.sf.util.DateTimeUtility;

/**
 *
 * @author dmitry.guriev
 *
 */
@MatrixUtils
@ResourceAliases({"DateUtil"})
public class DateUtil extends AbstractCaller {
    private static Logger logger = LoggerFactory.getLogger(DateUtil.class);

    private static final String DATE_COMPONENTS =
        "    <tr bgcolor=\"#eeeeff\"><td><code>Y</code><td>Year" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>M</code><td>Month" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>D</code><td>Day";

    private static final String TIME_COMPONENTS =
        "    <tr bgcolor=\"#eeeeff\"><td><code>h</code><td>Hour" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>m</code><td>Minute" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>s</code><td>Second" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>ms</code><td>Millisecond" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>mc</code><td>Microsecond" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>ns</code><td>Nanosecond";

    public static final String DATE_COMPONENTS_TABLE =
        "<blockquote>" +
        "<table border=0 cellspacing=3 cellpadding=0>" +
            "    <tr bgcolor=\"#ccccff\" align=\"left\"><th>Field<th>Component" +
            DATE_COMPONENTS +
        "</table>" +
        "</blockquote>";

    public static final String TIME_COMPONENTS_TABLE =
        "<blockquote>" +
        "<table border=0 cellspacing=3 cellpadding=0>" +
            "    <tr bgcolor=\"#ccccff\" align=\"left\"><th>Field<th>Component" +
            TIME_COMPONENTS +
        "</table>" +
        "</blockquote>";

    public static final String DATE_TIME_COMPONENTS_TABLE =
        "<blockquote>" +
        "<table border=0 cellspacing=3 cellpadding=0>" +
            "    <tr bgcolor=\"#ccccff\" align=\"left\"><th>Field<th>Component" +
            DATE_COMPONENTS +
            TIME_COMPONENTS +
        "</table>" +
        "</blockquote>";

    public static final String FORMAT_HELP =
        "<br/><h4>Date and Time format pattern</h4>" +
        "The following pattern symbols are defined:" +
        "<blockquote>" +
        "<table border=0 cellspacing=3 cellpadding=0>" +
        "    <tr bgcolor=\"#ccccff\" align=\"left\"><th>Symbol<th>Meaning<th>Examples" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>G</code><td>era<td>1; 01; AD; Anno Domini" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>y</code><td>year<td>2004; 04" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>D</code><td>day-of-year<td>189" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>M</code><td>month-of-year<td>7; 07; Jul; July; J" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>d</code><td>day-of-month<td>10" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>Q</code><td>quarter-of-year<td>3; 03; Q3" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>Y</code><td>week-based-year<td>1996; 96" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>w</code><td>week-of-year<td>27" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>W</code><td>week-of-month<td>27" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>e</code><td>localized day-of-week<td>2; Tue; Tuesday; T" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>E</code><td>day-of-week<td>2; Tue; Tuesday; T" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>F</code><td>week-of-month<td>3" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>a</code><td>am-pm-of-day<td>PM" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>h</code><td>clock-hour-of-am-pm (1-12)<td>12" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>K</code><td>hour-of-am-pm (0-11)<td>0" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>k</code><td>clock-hour-of-am-pm (1-24)<td>0" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>H</code><td>hour-of-day (0-23)<td>0" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>m</code><td>minute-of-hour<td>30" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>s</code><td>second-of-minute<td>55" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>S</code><td>fraction-of-second<td>978" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>A</code><td>milli-of-day<td>1234" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>n</code><td>nano-of-second<td>987654321" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>N</code><td>nano-of-day<td>1234000000" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>V</code><td>time-zone ID<td>America/Los_Angeles; Z; -08:30" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>z</code><td>time-zone name<td>Pacific Standard Time; PST" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>X</code><td>zone-offset 'Z' for zero<td>Z; -08; -0830; -08:30; -083015; -08:30:15;" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>x</code><td>zone-offset<td>+0000; -08; -0830; -08:30; -083015; -08:30:15;" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>Z</code><td>zone-offset<td>+0000; -0800; -08:00;" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>p</code><td>pad next<td>1" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>'</code><td>escape for text<td>" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>''</code><td>single quote<td>'" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>[</code><td>optional section start" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>]</code><td>optional section end" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>{}</code><td>reserved for future use" +
        "</table>" +
        "</blockquote>" +
        "For example, <code>yyyyMMdd-HH:mm:ss</code> will format <code>2017-05-30T14:05:13.801</code> as <code>20170530-14:05:13</code><br/>";

    public static final String MODIFY_HELP =
        "<br/><h4>Date and Time modify pattern</h4>" +
        "Format: <code>&lt;field&gt;&lt;operator&gt;&lt;value&gt;[:&lt;field&gt;&lt;operator&gt;&lt;value&gt;]</code><br/><br/>" +
        "The following pattern fields are defined:" +
                DATE_TIME_COMPONENTS_TABLE +
        "The following pattern operators are defined:" +
        "<blockquote>" +
        "<table border=0 cellspacing=3 cellpadding=0>" +
        "    <tr bgcolor=\"#ccccff\" align=\"left\"><th>Operator<th>Action" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>+</code><td>Add value to time field" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>-</code><td>Substract value from time field" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>=</code><td>Set time field to value" +
        "</table>" +
        "</blockquote>" +
        "Value should be an unsigned integer amount of time<br/><br/>" +
        "For example, <code>Y+1:M-2:D=3:h+4:m-5:s=6:ms=7</code> will modify <code>2017-05-30T14:00:23.439</code> to <code>2018-03-03T17:55:06.007</code><br/>";

    public static final String OFFSET_ID_HELP =
        "<br/><h4>Date and Time offset id</h4>" +
        "The following pattern formats are defined:" +
        "<blockquote>" +
        "<table border=0 cellspacing=2 cellpadding=0>" +
        "    <tr bgcolor=\"#ccccff\" align=\"left\"><th>Format</th></tr>" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>+h</code><td></tr>" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>+hh</code><td></tr>" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>+hh:mm</code><td></tr>" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>-hh:mm</code><td></tr>" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>+hhmm</code><td></tr>" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>-hhmm</code><td></tr>" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>+hh:mm:ss</code><td></tr>" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>-hh:mm:ss</code><td></tr>" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>+hhmmss</code><td></tr>" +
        "    <tr bgcolor=\"#eeeeff\"><td><code>-hhmmss</code><td></tr>" +
        "</table>" +
        "</blockquote>" +
        "Thе &plusmn; means either a plus or a minus symbol.<br/>" +
        "The maximum supported range is from +18:00 to -18:00 inclusive.<br/><br/>" +
        "For example, <code>+03:30</code><br/>";
    
    @Description("Returns current time in UTC time zone<br/>Example: #{getTime()}")
    @UtilityMethod
    public LocalTime getTime() {
        return DateTimeUtility.nowLocalTime();
    }

    @Description("Returns current time in UTC time zone modified by pattern." + MODIFY_HELP + "Example: #{getTime(modifyPattern)}")
    @UtilityMethod
    public LocalTime getTime(String modifyPattern) {
        return toTime(getDateTime(modifyPattern));
    }

    @Description("Returns current date in UTC time zone<br/>Example: #{getDate()}")
    @UtilityMethod
    public LocalDate getDate() {
        return DateTimeUtility.nowLocalDate();
    }

    @Description("Returns current date in UTC time zone modified by pattern." + MODIFY_HELP + "Example: #{getDate(modifyPattern)}")
    @UtilityMethod
    public LocalDate getDate(String modifyPattern) {
        return toDate(getDateTime(modifyPattern));
    }

    @Description("Returns current date time in UTC time zone<br/>Example: #{getDateTime()}")
    @UtilityMethod
    public final LocalDateTime getDateTime() {
        return DateTimeUtility.nowLocalDateTime();
    }

    @Description("Returns current date time in UTC time zone modified by pattern." + MODIFY_HELP + "Example: #{getDateTime(modifyPattern)}")
    @UtilityMethod
    public final LocalDateTime getDateTime(String modifyPattern) {
        return modifyDateTime(getDateTime(), modifyPattern);
    }

    @Description("Returns current date time in UTC time zone modified by pattern<br/>"
            + "If modified date time is on weekend and <code>skipWeekends</code> is <code>true</code> then the next working in direction of modification will be returned"
            + MODIFY_HELP
            + "Example: #{getDateTime(modifyPattern, skipWeekends)}"
    )
    @UtilityMethod
    public final LocalDateTime getDateTime(String modifyPattern, boolean skipWeekends) {
        LocalDateTime nowDate = DateTimeUtility.nowLocalDateTime();
        LocalDateTime modifiedDate = modifyTemporal(nowDate, modifyPattern);
        if (skipWeekends) {
            DayOfWeek dayOfWeek = modifiedDate.getDayOfWeek();
            long currentMillis = DateTimeUtility.getMillisecond(nowDate);
            long modifiedMillis = DateTimeUtility.getMillisecond(modifiedDate);
            if (DayOfWeek.SATURDAY.equals(dayOfWeek)) {
                int shift = modifiedMillis - currentMillis >= 0 ? 2 : -1;
                return  modifiedDate.plusDays(shift);
            } else if (DayOfWeek.SUNDAY.equals(dayOfWeek)) {
                int shift = modifiedMillis - currentMillis >= 0 ? 1 : -2;
                return modifiedDate.plusDays(shift);
            }
        }
        return modifiedDate;
    }

    @Description("Returns the date/time in UTC applying a time offset pattern to the current date/time in the specified time zone." + MODIFY_HELP + OFFSET_ID_HELP + "Example: #{getDateTimeByZoneId(modifyPattern, timeZoneId)}")
    @UtilityMethod
    public final LocalDateTime getDateTimeByZoneId(String modifyPattern, String timeZoneId) {
        return DateTimeUtility.toLocalDateTime(modifyTemporal(DateTimeUtility.nowZonedDateTime(timeZoneId), modifyPattern));
    }

    @Description("Returns current date time in UTC time zone modified by pattern (weekend days are skipped during modification)." + MODIFY_HELP + "Example: #{getBusinessDateTime(modifyPattern)}")
    @UtilityMethod
    public final LocalDateTime getBusinessDateTime(String modifyPattern) {
        return modifyBusinessDateTime(getDateTime(), modifyPattern);
    }

    @Description("Returns current date time in UTC time zone modified by pattern after applying time zone offset (DST aware). Weekend days are skipped during modification." + MODIFY_HELP + "Example: #{getBusinessDateTimeByZoneId(modifyPattern, timeZoneId)}")
    @UtilityMethod
    public final LocalDateTime getBusinessDateTimeByZoneId(String modifyPattern, String timeZoneId) {
        return modifyBusinessDateTimeByZoneId(getDateTime(), modifyPattern, timeZoneId);
    }

    @Description("Modifies provided date time in UTC time zone modified by pattern (weekend days are skipped during modification)" + MODIFY_HELP + "Usage: #{modifyBusinessDateTime(dateTime, modifyPattern)}")
    @UtilityMethod
    public final LocalDateTime modifyBusinessDateTime(LocalDateTime dateTime, String modifyPattern) {
        return getBusinessDateTime(dateTime, modifyDateTime(dateTime, modifyPattern));
    }

    @Description("Modifies provided date time in UTC time zone modified by pattern after applying time zone offset (DST aware). Weekend days are skipped during modification." + MODIFY_HELP + "Usage: #{modifyBusinessDateTimeByZoneId(dateTime, modifyPattern, timeZoneId)}")
    @UtilityMethod
    public final LocalDateTime modifyBusinessDateTimeByZoneId(LocalDateTime dateTime, String modifyPattern, String timeZoneId) {
        LocalDateTime originalConverted = ZonedDateTime.of(dateTime, ZoneOffset.UTC).withZoneSameInstant(ZoneId.of(timeZoneId)).toLocalDateTime();
        LocalDateTime targetTimezoneZoneResult = getBusinessDateTime(originalConverted, modifyTemporal(originalConverted, modifyPattern));
        return ZonedDateTime.of(targetTimezoneZoneResult, ZoneId.of(timeZoneId)).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    @Description("Converts epoch milliseconds to time in UTC time zone<br/>Example: #{toTime(epochMillis)}")
    @UtilityMethod
    public LocalTime toTime(long epochMillis) {
        return DateTimeUtility.toLocalTime(epochMillis);
    }

    @Description("Converts epoch milliseconds to time in UTC time zone modified by pattern." + MODIFY_HELP + "Example: #{toTime(epochMillis, modifyPattern)}")
    @UtilityMethod
    public LocalTime toTime(long epochMillis, String modifyPattern) {
        return toTime(toDateTime(epochMillis, modifyPattern));
    }

    @Description("Converts string using format pattern to time in UTC time zone." + FORMAT_HELP + "Example: #{toTime(source, formatPattern)}")
    @UtilityMethod
    public LocalTime toTime(String source, String formatPattern) {
        return toTime(toDateTime(source, formatPattern));
    }

    @Description("Converts string using format pattern to time in UTC time zone modified by pattern." + FORMAT_HELP + MODIFY_HELP + "Example: #{toTime(source, formatPattern, modifyPattern)}")
    @UtilityMethod
    public LocalTime toTime(String source, String formatPattern, String modifyPattern) {
        return toTime(toDateTime(source, formatPattern, modifyPattern));
    }

    @Description("Converts date time to time in UTC time zone<br/>Example: #{toTime(dateTime)}")
    @UtilityMethod
    public LocalTime toTime(LocalDateTime dateTime) {
        return DateTimeUtility.toLocalTime(dateTime);
    }

    @Description("Converts date time to time in UTC time zone modified by pattern." + MODIFY_HELP + "Example: #{toTime(dateTime, modifyPattern)}")
    @UtilityMethod
    public LocalTime toTime(LocalDateTime dateTime, String modifyPattern) {
        return toTime(modifyDateTime(dateTime, modifyPattern));
    }

    @Description("Converts epoch milliseconds to date in UTC time zone<br/>Example: #{toDate(epochMillis)}")
    @UtilityMethod
    public LocalDate toDate(long epochMillis) {
        return DateTimeUtility.toLocalDate(epochMillis);
    }

    @Description("Converts epoch milliseconds to date in UTC time zone modified by pattern." + MODIFY_HELP + "Example: #{toDate(epochMillis, modifyPattern)}")
    @UtilityMethod
    public LocalDate toDate(long epochMillis, String modifyPattern) {
        return toDate(toDateTime(epochMillis, modifyPattern));
    }

    @Description("Converts string using format pattern to date in UTC time zone." + FORMAT_HELP + "Example: #{toDate(source, formatPattern)}")
    @UtilityMethod
    public LocalDate toDate(String source, String formatPattern) {
        return toDate(toDateTime(source, formatPattern));
    }

    @Description("Converts string using format pattern to date in UTC time zone modified by pattern." + FORMAT_HELP + MODIFY_HELP + "Example: #{toDate(source, formatPattern, modifyPattern)}")
    @UtilityMethod
    public LocalDate toDate(String source, String formatPattern, String modifyPattern) {
        return toDate(toDateTime(source, formatPattern, modifyPattern));
    }

    @Description("Converts date time to date in UTC time zone<br/>Example: #{toDate(dateTime)}")
    @UtilityMethod
    public LocalDate toDate(LocalDateTime dateTime) {
        return DateTimeUtility.toLocalDate(dateTime);
    }

    @Description("Converts date time to date in UTC time zone modified by pattern." + MODIFY_HELP + "Example: #{toDate(dateTime, modifyPattern)}")
    @UtilityMethod
    public LocalDate toDate(LocalDateTime dateTime, String modifyPattern) {
        return toDate(modifyDateTime(dateTime, modifyPattern));
    }

    @Description("Converts epoch milliseconds to date time in UTC time zone<br/>Example: #{toDateTime(epochMillis)}")
    @UtilityMethod
    public LocalDateTime toDateTime(long epochMillis) {
        return DateTimeUtility.toLocalDateTime(epochMillis);
    }

    @Description("Converts epoch milliseconds to date time in UTC time zone modified by pattern." + MODIFY_HELP + "Example: #{toDateTime(epochMillis, modifyPattern)}")
    @UtilityMethod
    public LocalDateTime toDateTime(long epochMillis, String modifyPattern) {
        return modifyDateTime(toDateTime(epochMillis), modifyPattern);
    }

    @Description("Converts string using automatic format pattern detection to date time in UTC time zone<br/>"
            + "One of the following date/time pattern is defined:"
            + " <br/><blockquote>"
            + " <table border=0 cellspacing=3 cellpadding=0 summary=\"Chart shows date/time patterns, and date/time values.\">"
            + "     <tr bgcolor=\"#ccccff\">"
            + "         <th align=left>Pattern"
            + "         <th align=left>Date/time value"
            + "     <tr bgcolor=\"#eeeeff\">"
            + "         <td><code>yyyy-MM-dd HH:mm:ss.SSS Z</code>"
            + "         <td>2000-01-01 00:00:00.000 -0700"
            + "     <tr bgcolor=\"#eeeeff\">"
            + "         <td><code>yyyy-MM-dd HH:mm:ss.SSS</code>"
            + "         <td>2000-01-01 00:00:00.000"
            + "     <tr bgcolor=\"#eeeeff\">"
            + "         <td><code>yyyy-MM-dd HH:mm:ss</code>"
            + "         <td>2000-01-01 00:00:00"
            + "     <tr bgcolor=\"#eeeeff\">"
            + "         <td><code>yyyy-MM-dd HH:mm</code>"
            + "         <td>2000-01-01 00:00"
            + "     <tr bgcolor=\"#eeeeff\">"
            + "         <td><code>yyyy-MM-dd HH</code>"
            + "         <td>2000-01-01 00"
            + "     <tr bgcolor=\"#eeeeff\">"
            + "         <td><code>yyyy-MM-dd</code>"
            + "         <td>2000-01-01"
            + "     <tr bgcolor=\"#eeeeff\">"
            + "         <td><code>yyyy-MM</code>"
            + "         <td>2000-01"
            + "     <tr bgcolor=\"#eeeeff\">"
            + "         <td><code>yyyy</code>"
            + "         <td>2000"
            + " </table>"
            + " </blockquote><br/>"
            + "Example: #{toDateTime(source)}")
    @UtilityMethod
    public LocalDateTime toDateTime(String source) {
        Objects.requireNonNull(source, "Date argument is null");
        StringBuilder builder = new StringBuilder();

        switch (source.length()) {
        case (29):
            builder.insert(0, " Z");
        case (23):
            builder.insert(0, ".SSS");
        case (19):
            builder.insert(0, ":ss");
        case (16):
            builder.insert(0, ":mm");
        case (13):
            builder.insert(0, " HH");
        case (10):
            builder.insert(0, "-dd");
        case (7):
            builder.insert(0, "-MM");
        case (4):
            builder.insert(0, "yyyy");
            break;
        default:
            throw new IllegalArgumentException("Unsupported date format " + source);
        }
        try {
            return toDateTime(source, builder.toString());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Incorrect date value " + source, e);
        }
    }

    @Description("Converts string using format pattern to date time in UTC time zone." + FORMAT_HELP + "Example: #{toDateTime(source, formatPattern)}")
    @UtilityMethod
    public LocalDateTime toDateTime(String source, String formatPattern) {

        int yearPos = formatPattern.lastIndexOf('y') + 1;
        if (yearPos > 0 && formatPattern.length() > yearPos && !Character.isDigit(formatPattern.charAt(yearPos))) {
            formatPattern = new StringBuilder(formatPattern).insert(yearPos, ' ').toString();
            source = new StringBuilder(source).insert(yearPos, ' ').toString();
        }

        return DateTimeUtility.toLocalDateTime(DateTimeUtility.createFormatter(formatPattern).parse(source));
    }

    @Description("Converts string using format pattern to date time in UTC time zone modified by pattern." + FORMAT_HELP + MODIFY_HELP + "Example: #{toDateTime(source, formatPattern, modifyPattern)}")
    @UtilityMethod
    public LocalDateTime toDateTime(String source, String formatPattern, String modifyPattern) {
        return modifyDateTime(toDateTime(source, formatPattern), modifyPattern);
    }

    @Description("Converts time to date time in UTC time zone<br/>Example: #{toDateTime(time)}")
    @UtilityMethod
    public LocalDateTime toDateTime(LocalTime time) {
        return DateTimeUtility.toLocalDateTime(time);
    }

    @Description("Converts time to date time in UTC time zone modified by pattern." + MODIFY_HELP + "Example: #{toDateTime(time, modifyPattern)}")
    @UtilityMethod
    public LocalDateTime toDateTime(LocalTime time, String modifyPattern) {
        return modifyDateTime(toDateTime(time), modifyPattern);
    }

    @Description("Converts date to date time in UTC time zone<br/>Example: #{toDateTime(date)}")
    @UtilityMethod
    public LocalDateTime toDateTime(LocalDate date) {
        return DateTimeUtility.toLocalDateTime(date);
    }

    @Description("Converts date to date time in UTC time zone modified by pattern." + MODIFY_HELP + "Example: #{toDateTime(date, modifyPattern)}")
    @UtilityMethod
    public LocalDateTime toDateTime(LocalDate date, String modifyPattern) {
        return modifyDateTime(toDateTime(date), modifyPattern);
    }

    @Description("Formats time in UTC time zone into string using format pattern." + FORMAT_HELP + "Example: #{formatTime(time, formatPattern)}")
    @UtilityMethod
    public String formatTime(LocalTime time, String formatPattern) {
        return formatDateTime(toDateTime(time), formatPattern);
    }

    @Description("Formats time in UTC time zone modified by pattern into string using format pattern." + FORMAT_HELP + MODIFY_HELP + "Example: #{formatTime(time, formatPattern, modifyPattern)}")
    @UtilityMethod
    public String formatTime(LocalTime time, String formatPattern, String modifyPattern) {
        return formatDateTime(toDateTime(time), formatPattern, modifyPattern);
    }

    @Description("Formats date in UTC time zone into string using format pattern." + FORMAT_HELP + "Example: #{formatDate(date, formatPattern)}")
    @UtilityMethod
    public String formatDate(LocalDate date, String formatPattern) {
        return formatDateTime(toDateTime(date), formatPattern);
    }

    @Description("Formats date in UTC time zone modified by pattern into string using format pattern." + FORMAT_HELP + MODIFY_HELP + "Example: #{formatDate(date, formatPattern, modifyPattern)}")
    @UtilityMethod
    public String formatDate(LocalDate date, String formatPattern, String modifyPattern) {
        return formatDateTime(toDateTime(date), formatPattern, modifyPattern);
    }

    @Description("Formats date time in UTC time zone into string using format pattern." + FORMAT_HELP + "Example: #{formatDateTime(dateTime, formatPattern)}")
    @UtilityMethod
    public String formatDateTime(LocalDateTime dateTime, String formatPattern) {
        return formatTemporal(dateTime, formatPattern);
    }

    @Description("Formats date time in UTC time zone modified by pattern into string using format pattern." + FORMAT_HELP + MODIFY_HELP + "Example: #{formatDateTime(dateTime, formatPattern, modifyPattern)}")
    @UtilityMethod
    public String formatDateTime(LocalDateTime dateTime, String formatPattern, String modifyPattern) {
        return formatTemporal(modifyDateTime(dateTime, modifyPattern), formatPattern);
    }

    @Description("Merges date and time into date time in UTC time zone<br/>Example: #{mergeDateTime(date, time)}")
    @UtilityMethod
    public LocalDateTime mergeDateTime(LocalDate date, LocalTime time) {
        return DateTimeUtility.toLocalDateTime(date, time);
    }

    @Description("Merges date and time into date time in UTC time zone modified by pattern." + MODIFY_HELP + "Example: #{mergeDateTime(date, time, modifyPattern)}")
    @UtilityMethod
    public LocalDateTime mergeDateTime(LocalDate date, LocalTime time, String modifyPattern) {
        return modifyDateTime(mergeDateTime(date, time), modifyPattern);
    }

    @Description("Merges date and date time (only time is used) into date time in UTC time zone<br/>Example: #{mergeDateTime(date, dateTime)}")
    @UtilityMethod
    public LocalDateTime mergeDateTime(LocalDate date, LocalDateTime dateTime) {
        return mergeDateTime(date, toTime(dateTime));
    }

    @Description("Merges date and date time (only time is used) into date time in UTC time zone modified by pattern." + MODIFY_HELP + "Example: #{mergeDateTime(date, dateTime, modifyPattern)}")
    @UtilityMethod
    public LocalDateTime mergeDateTime(LocalDate date, LocalDateTime dateTime, String modifyPattern) {
        return mergeDateTime(date, toTime(dateTime), modifyPattern);
    }

    @Description("Merges date time (only date is used) and time into date time in UTC time zone<br/>Example: #{mergeDateTime(dateTime, time)}")
    @UtilityMethod
    public LocalDateTime mergeDateTime(LocalDateTime dateTime, LocalTime time) {
        return mergeDateTime(toDate(dateTime), time);
    }

    @Description("Merges date time (only date is used) and time into date time in UTC time zone modified by pattern." + MODIFY_HELP + "Example: #{mergeDateTime(dateTime, time, modifyPattern)}")
    @UtilityMethod
    public LocalDateTime mergeDateTime(LocalDateTime dateTime, LocalTime time, String modifyPattern) {
        return mergeDateTime(toDate(dateTime), time, modifyPattern);
    }

    @Description("Merges date time (only date is used) and date time (only time is used) into date time in UTC time zone<br/>Example: #{mergeDateTime(firstDateTime, secondDateTime)}")
    @UtilityMethod
    public LocalDateTime mergeDateTime(LocalDateTime firstDateTime, LocalDateTime secondDateTime) {
        return mergeDateTime(toDate(firstDateTime), toTime(secondDateTime));
    }

    @Description("Merges date time (only date is used) and date time (only time is used) into date time in UTC time zone modified by pattern." + MODIFY_HELP + "Example: #{mergeDateTime(firstDateTime, secondDateTime, modifyPattern)}")
    @UtilityMethod
    public LocalDateTime mergeDateTime(LocalDateTime firstDateTime, LocalDateTime secondDateTime, String modifyPattern) {
        return mergeDateTime(toDate(firstDateTime), toTime(secondDateTime), modifyPattern);
    }

    @Description("Formats current date time in UTC time zone into string using format pattern." + FORMAT_HELP + "Example: #{formatDateTime(formatPattern)}")
    @UtilityMethod
    public String formatDateTime(String formatPattern) {
        return formatDateTime(getDateTime(), formatPattern);
    }

    @Description("Formats current date time in UTC time zone modified by pattern into string using format pattern." + FORMAT_HELP + MODIFY_HELP + "Example: #{formatDateTime(formatPattern, modifyPattern)}")
    @UtilityMethod
    public String formatDateTime(String formatPattern, String modifyPattern) {
        return formatDateTime(getDateTime(), formatPattern, modifyPattern);
    }

    @Description("Modifies date time string using format and modify patterns." + FORMAT_HELP + MODIFY_HELP + "Example; #{modifyDateTime(source, formatPattern, modifyPattern)}")
    @UtilityMethod
    public String modifyDateTime(String source, String formatPattern, String modifyPattern) {
        return formatDateTime(toDateTime(source, formatPattern, modifyPattern), formatPattern);
    }

    @Description("Modifies time in UTC time zone by pattern." + MODIFY_HELP + "Example: #{modifyTime(time, modifyPattern)}")
    @UtilityMethod
    public LocalTime modifyTime(LocalTime time, String modifyPattern) {
        return toTime(toDateTime(time, modifyPattern));
    }

    @Description("Modifies date in UTC time zone by pattern." + MODIFY_HELP + "Example: #{modifyDate(date, modifyPattern)}")
    @UtilityMethod
    public LocalDate modifyDate(LocalDate date, String modifyPattern) {
        return toDate(toDateTime(date, modifyPattern));
    }

    @Description("Modifies date time in UTC time zone by pattern." + MODIFY_HELP + "Example: #{modifyDateTime(dateTime, modifyPattern)}")
    @UtilityMethod
    public LocalDateTime modifyDateTime(LocalDateTime dateTime, String modifyPattern) {
        return modifyTemporal(dateTime, modifyPattern);
    }

    @Description("Modifies date time in UTC time zone by pattern after applying time zone offset (DST aware)." + MODIFY_HELP + "Example: #{modifyDateTimeByZoneId(dateTime, modifyPattern, timeZoneId)}")
    @UtilityMethod
    public LocalDateTime modifyDateTimeByZoneId(LocalDateTime dateTime, String modifyPattern, String timeZoneId) {
        LocalDateTime convertedToTargetZone = ZonedDateTime.of(dateTime, ZoneOffset.UTC)
                .withZoneSameInstant(ZoneId.of(timeZoneId)).toLocalDateTime();
        return ZonedDateTime.of(modifyTemporal(convertedToTargetZone, modifyPattern), ZoneId.of(timeZoneId))
                .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    @Description("Returns difference between minuend and subtrahend date time in specified date component<br/>"
            + "The following date components are defined:"
            + DATE_TIME_COMPONENTS_TABLE
            + "Example: #{diffDateTime(minuend, subtrahend, dateComponent)}")
    @UtilityMethod
    public long diffDateTime(LocalDateTime minuend, LocalDateTime subtrahend, String dateComponent) {
        Objects.requireNonNull(dateComponent, "datePart argument is null");
        Objects.requireNonNull(minuend, "The first argument is null");
        Objects.requireNonNull(subtrahend, "The second argument is null");
        DateComponent enumDateComponent = DateComponent.parse(dateComponent);
        if (enumDateComponent != null) {
            return enumDateComponent.diff(minuend, subtrahend);
        } else {
            throw new IllegalArgumentException("Unsupported date component " + dateComponent);
        }
    }

    @Description("Returns difference between ISO string minuend and subtrahend date time in specified date component<br/>"
            + "The following date components are defined:"
            + DATE_TIME_COMPONENTS_TABLE
            + "Example: #{diffDateTimeISO(minuend, subtrahend, dateComponent)}")
    @UtilityMethod
    public long diffDateTimeISO(String minuend, String subtrahend, String dateComponent) {
        Objects.requireNonNull(dateComponent, "datePart argument is null");
        Objects.requireNonNull(minuend, "The first argument is null");
        Objects.requireNonNull(subtrahend, "The second argument is null");
        return diffDateTime(DateTimeUtility.toLocalDateTime(ZonedDateTime.parse(minuend)),
                DateTimeUtility.toLocalDateTime(ZonedDateTime.parse(subtrahend)), dateComponent);
    }

    @Description("Returns value of choose date component from LocalDate<br/>"
            + "The following date components are defined:"
            + DATE_COMPONENTS_TABLE
            + "Usage: #{getDateComponent(source, dateComponent)}<br/>"
            + "Example: #{getDateComponent(#{toDate(\"2018-08-15\", \"yyyy-MM-dd\")}, \"M\")} returns <b>8</b>")
    @UtilityMethod
    public int getDateComponent(LocalDate source, String dateComponent) {
        return getComponent(source, dateComponent);
    }

    @Description("Returns value of choose date component from LocalTime<br/>"
            + "The following date components are defined:"
            + TIME_COMPONENTS_TABLE
            + "Usage: #{getDateComponent(source, dateComponent)}<br/>"
            + "Example: #{getDateComponent(#{toTime(\"14:35:48.456\", \"HH:mm:ss.SSS\")}, \"ms\")} returns <b>456</b>")
    @UtilityMethod
    public int getDateComponent(LocalTime source, String dateComponent) {
        return getComponent(source, dateComponent);
    }

    @Description("Returns value of choose date component from LocalDateTime<br/>"
            + "The following date components are defined:"
            + DATE_TIME_COMPONENTS_TABLE
            + "Usage: #{getDateComponent(source, dateComponent)}<br/>"
            + "Example: #{getDateComponent(#{toDateTime(\"2018-08-15 14:35:48.456\")}, \"s\")} returns <b>48</b>")
    @UtilityMethod
    public int getDateComponent(LocalDateTime source, String dateComponent) {
        return getComponent(source, dateComponent);
    }

    private int getComponent(Temporal source, String dateComponent) {
        Objects.requireNonNull(source, "source argument is null");
        Objects.requireNonNull(dateComponent, "dataComponent parameter is null");

        DateComponent component = DateComponent.parse(dateComponent);
        if (component == null) {
            throw new IllegalArgumentException("Unknown date component: " + dateComponent);
        }

        try {
            return component.extract(source);
        } catch (Exception ex) {
            throw new EPSCommonException(String.format("Can't extract %s from %s [%s]", dateComponent, source, source.getClass().getSimpleName()), ex);
        }
    }

    private String formatTemporal(Temporal source, String pattern) {
        DateTimeFormatter formatter = DateTimeUtility.createFormatter(pattern);
        return formatter.format(source);
    }

    private LocalDateTime getBusinessDateTime(LocalDateTime original, LocalDateTime modified) {
        LocalDateTime after = DateTimeUtility.toLocalDateTime(modified);
        LocalDateTime iter = DateTimeUtility.toLocalDateTime(original);

        boolean past = false;

        if (after.isBefore(iter)) {
            past = true;
        }

        int counter = past ? -1 : 1;

        while (past ? !iter.toLocalDate().isBefore(after.toLocalDate()) : !iter.toLocalDate().isAfter(after.toLocalDate())) {

            DayOfWeek dayOfWeek = iter.getDayOfWeek();
            if (dayOfWeek.equals(DayOfWeek.SATURDAY) || dayOfWeek.equals(DayOfWeek.SUNDAY)) {
                after = after.plusDays(counter);
            }

            iter = iter.plusDays(counter);
            if (logger.isDebugEnabled()) {
                logger.debug("{} : {}  {}", iter.isAfter(after), iter, after);
            }
        }

        return after;
    }

    public static <T extends Temporal> T modifyTemporal(T source, String modifyPattern) {
        List<DateModificator> modificators = DateModificator.parse(modifyPattern);

        for (DateModificator dateModificator : modificators) {
            source = dateModificator.modify(source);
        }

        return source;
    }

    public static LocalDateTime modifyLocalDateTime(String modifyPattern) {
        return modifyTemporal(DateTimeUtility.nowLocalDateTime(), modifyPattern);
    }

    public static long getMilliseconds(LocalDateTime date) {
        return DateTimeUtility.getMillisecond(date);
    }

    /**
     *
     * @param modifyPattern
     * @param format
     * @param timeZoneId
     * @return
     */
    public static final String formatDateTime(String modifyPattern, String format, String timeZoneId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format).withZone(ZoneId.of(timeZoneId));
        return modifyZonedDateTime(modifyPattern).format(formatter);
    }

    public static final ZonedDateTime modifyZonedDateTime(String modifyPattern) {
        ZonedDateTime zonedDateTime = ZonedDateTime.of(modifyTemporal(DateTimeUtility.nowLocalDateTime(), modifyPattern), ZoneOffset.UTC);
        return zonedDateTime;
    }
}