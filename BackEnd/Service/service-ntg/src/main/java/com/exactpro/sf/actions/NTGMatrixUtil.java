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

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityMethod;
import com.exactpro.sf.services.ntg.NTGUtility;
import com.exactpro.sf.util.DateTimeUtility;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

/**
 * Collection of matrix utilities for NTG protocol.
 * @author dmitry.guriev
 *
 */
@MatrixUtils
@ResourceAliases({"NTGMatrixUtil"})
public class NTGMatrixUtil extends AbstractCaller {

    private static final Logger logger = LoggerFactory.getLogger(NTGMatrixUtil.class);

    /**
	 * Generate unique ClOrID for new order.
	 * @return
	 */
	@Description("Generate unique ClOrdID for new order<br>"
			+ " Example:"
			+ "#{ShortClOrdID(\"test\")}<br>"
			+ " will return:<br>"
			+ "00000000000000000001test<br>"
			+ "next call:"
			+ "#{ShortClOrdID(\"_xxxxx\")}<br>"
			+ " will return:<br>"
			+ "000000000000000002_xxxxx<br>")
	@UtilityMethod
	public final String ShortClOrdID(final String userKey)
	{
        return NTGUtility.getNewShortClOrdID(userKey);
	}

	/**
	 * Generate unique ClOrID for new order.
	 * @return
	 */
	@Description("Generate unique ClOrdID for new order<br>"
			+ "Example:<br>"
			+ "#{ClOrdID()}<br>"
			+ "will return:<br>"
			+ "12768827218610000001<br>"
			+ "next call will return:<br>"
			+ "12768827218610000002")
	@UtilityMethod
	public final String ClOrdID()
	{
        return NTGUtility.getNewClOrdID();
	}

	/**
	 * Generate unique ClOrID for new order.
	 * @param length
	 * @return
	 */
	@Description(
			"Generate unique ClOrID for new order.<br>"
	)
	@UtilityMethod
	public final String ClOrdID(int length)
	{
		return String.valueOf(RandomStringUtils.random(length, "0123456789"));
	}

	@Description("Generate TransactTime according specification.<br>" +
			"The first 4 bytes of the TransactTime timestamp will represent the Unix (Posix) time while the next 4 bytes will specify the micro seconds.<br>"
			+" Example:<br>"
			+ "#{TransactTime()}<br>"
			+ "will return:<br>"
			+ "5453303938323494736<br>"
			+ "in Hex format it will be:<br>"
			+ "4BAE070A0002B750<br>"
			+ "human readable string will representation:<br>"
			+ "1269696266:178000")
	@UtilityMethod
	public final String TransactTime()
	{
        return NTGUtility.getTransactTime();
	}

	@Description("Transact Time - return time in GMT time zone<br>"
			+ DateUtil.MODIFY_HELP
			+ "Example:<br>"
            + "#{TransactTime(\"Y+2:M-6:D=4:h+1:m-2:s=39\")}<br>"
			+ "will return:<br>"
			+ "<br>"
			+ "Date object with date: 20110904-08:58:39<br>"
	)
	@UtilityMethod
    public LocalDateTime TransactTime(String dateFormat)
	{
        return DateUtil.modifyLocalDateTime(dateFormat);
	}

	@Description("Return current date in GMT time zone in format: yyyyMMdd hh:mm:ss.SSS<br>"
			+ "Example:<br>"
			+ "#{EntryTime()}<br>"
			+ "will return:<br>"
			+ "20100618 19:29:43.178")
	@UtilityMethod
	public final String EntryTime()
	{
        return NTGUtility.getEntryTime();
	}

	/**
	 * Return expire date time one day in future from the current date
	 *
	 * @return formatted date time one day apart from current
	 */
	@Description("Return expire date time one day in future from the current date.<br>"
			+ "Example:<br>"
			+ "#{ExpireDateTime()}<br>"
			+ "will return:<br>"
			+ "1276872448")
	@UtilityMethod
	public final int ExpireDateTime()
	{
        return NTGUtility.getExpireDateTime();
	}

	/**
	 *  Return expire date time as specified in input string
	 *
	 * @return formatted date time
	 */
	@Description("Return expire date time.<br>"
			+  DateUtil.MODIFY_HELP)
	@UtilityMethod
	public final int ExpireDateTime(String dateFormat)
	{
        return NTGUtility.getExpireDateTime(dateFormat);
	}

	/**
     *  Return expire date time as specified in input string
     *
     * @return formatted date time
     */
    @Description("Return expire date time.<br>"
            + " Example:<br>"
            + " For the <b>25 Mar 2010, 08:00:00</b>"
            + " where input date parameter <b>#{getDate(\"Y+2:m-1:D=4\")}</b>"
            + " this method will return the following:<br>"
            + " 1330833600<br>"
            + " All fields are optional. If you do not specify any fields"
            + " than current time will be returned."
            + " All specified date modification operations will be applied"
            + " in the same order as they present in input string.<br>")
    @UtilityMethod
    public final int ExpireDateTime(LocalDateTime date)
    {
        return NTGUtility.getExpireDateTime(date, "");
    }

	/**
	 * Return expire date time as specified in input input values
	 *
	 * @return formatted date time
	 */
	@Description("Return expire date time as specified in input values.<br>"
			+ "Function parameters is year, month, day, hour, minute, second<br>"
			+ "Example:<br>"
			+ "#{ExpireDateTime(2013,8,6,18,25,31)}<br>"
			+ "will return:<br>"
			+ "1276872448")
	@UtilityMethod
	public final int ExpireDateTime(int year, int month, int day, int hour, int minute, int second)
	{
        return NTGUtility.getSpecificExpireDateTime(year, month, day, hour, minute, second);
	}

	/**
	 * Return expire date time according with input parameters
	 * @param datePart - one of the values:  SECOND , MINUTE, HOUR , DAY.
	 * @param value string representating of signed integer value.
	 * 			Absence of the sign sybmol is evaluated as positive number.
	 *
	 * @return formatted date time apart from current according to the provided
	 * parameters:
	 * 	SECOND - (+/-) seconds from the current date,
	 *  MINUTE - (+/-) minutes from the current date,
	 *  HOUR   - (+/-) hours from the current date,
	 *  DAY    - (+/-) days from the current date.
	 * @throws Exception
	 */
	@Description("<b>Same as ExpireDateTimePOSIX</b><BR>"
			+ " Return expire date in GMT time zone time according with input parameters.<br>"
			+ " First parameter is one of the values:  SECOND , MINUTE, HOUR, DAY, MONTH, YEAR.<br>"
			+ " Second parameter is integer amount of time. Absence of the sign sybmol is evaluated as positive number.<br>"
			+ " Example:<br>"
			+ " #{ExpireDateTime(\"MINUTE\", \"5\")}<br>"
			+ " will return:<br>"
			+" 1276883032")
	@UtilityMethod
	public final int ExpireDateTime(String datePart, String value) throws Exception
	{
		long millis = modifyDateTime(datePart, value);
		logger.debug("(int) (millis / 1000) = {}", (int) (millis / 1000));
		return (int) (millis / 1000);

//		Date date = new Date(millis);
//		String dateTime = (new SimpleDateFormat("yyyyMMdd-HH:mm:ss")).format(date);
//		return dateTime;
	}

	@Description(" Return expire date time in GMT time zone according with input parameters.<br>"
			+ " First parameter is one of the values:  SECOND , MINUTE, HOUR, DAY, MONTH, YEAR.<br>"
			+ " Second parameter is integer amount of time. Absence of the sign sybmol is evaluated as positive number.<br>"
			+ " Example:<br>"
			+ " #{ExpireDateTimePOSIX(\"MINUTE\", \"5\")}<br>"
			+ " will return:<br>"
			+" 1276883032")
	@UtilityMethod
	public final int ExpireDateTimePOSIX(String datePart, String value) throws Exception
	{
		return (int) (modifyDateTime(datePart, value)/1000);
	}
	
	private static long modifyDateTime(String timeUnit, String value) throws Exception {
	    return modifyDateTime(DateTimeUtility.nowLocalDateTime(), timeUnit, value);
	}
	
	protected static long modifyDateTime(LocalDateTime localDateTime, String timeUnit, String value) throws Exception {

		if(null == timeUnit || timeUnit.isEmpty())
		{
			logger.error("datePart is empty. Return default expire date time value.");
            return System.currentTimeMillis();
		}

		if(null == value || value.isEmpty())
		{
			logger.error("datePart is empty. Return default expire date time value.");
            return System.currentTimeMillis();
		}

		long offset = 0;

		try
		{
			offset = Long.parseLong(value, 10);
		}
		catch(Exception e)
		{
			logger.error(e.getMessage(), e);
			throw e;
		}

		try
		{
            TimeUnit part = Enum.valueOf(TimeUnit.class, timeUnit);

            localDateTime = localDateTime.plus(offset, part.temporalUnit);

	        return DateTimeUtility.getMillisecond(localDateTime);
		}
		catch(Exception e)
		{
			logger.error(e.getMessage(), e);
			throw e;
		}
	}

	public enum TimeUnit
	{
		SECOND (ChronoUnit.SECONDS),
		MINUTE (ChronoUnit.MINUTES),
		HOUR (ChronoUnit.HOURS),
		DAY (ChronoUnit.DAYS),
		MONTH (ChronoUnit.MONTHS),
		YEAR (ChronoUnit.YEARS);

        private final TemporalUnit temporalUnit;

        private TimeUnit(TemporalUnit temporalUnit) {
            this.temporalUnit = temporalUnit;
        }
	}
}
