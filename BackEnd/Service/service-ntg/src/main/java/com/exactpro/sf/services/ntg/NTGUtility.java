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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Random;

import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.actions.DateUtil;
import com.exactpro.sf.common.util.StringUtil;
import com.exactpro.sf.services.IdleStatus;
import com.exactpro.sf.services.ntg.exceptions.UnknownMina2SessionIdleStatusException;
import com.exactpro.sf.util.DateTimeUtility;

public final class NTGUtility {
    private static final Logger logger = LoggerFactory.getLogger(NTGUtility.class);

	public static final String EOL = StringUtil.EOL;
	private static Long ordID = 0L;
    private static Long currentTimeMillis;
    private static final String ClientOrderID_FROMAT = "%s%s";
    private static final ThreadLocal<DecimalFormat> formatter1 = new ThreadLocal<DecimalFormat>() {
        @Override
        protected DecimalFormat initialValue() {
            return new DecimalFormat("0000000000000");
        }
    };
    private static final ThreadLocal<DecimalFormat> formatter2 = new ThreadLocal<DecimalFormat>() {
        @Override
        protected DecimalFormat initialValue() {
            return new DecimalFormat("0000000");
        }
    };

    private static final DateTimeFormatter SDF_FORMATTER = DateTimeUtility.createFormatter("yyyyMMdd HH:mm:ss");

    private static final DateTimeFormatter ENTRY_FORMATTER = DateTimeUtility.createFormatter("yyyyMMdd HH:mm:ss.SSS");

    private static final Random RND = new Random(System.currentTimeMillis());

	/**
	 * The high digits lookup table.
	 */
	private static final byte[] highDigits;

	/**
	 * The low digits lookup table.
	 */
	private static final byte[] lowDigits;

	/**
	 * Initialize lookup tables.
	 */
	static
	{
        byte[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
				'9', 'A', 'B', 'C', 'D', 'E', 'F' };

		int i;
		byte[] high = new byte[256];
		byte[] low = new byte[256];

		for (i = 0; i < 256; i++) {
			high[i] = digits[i >>> 4];
			low[i] = digits[i & 0x0F];
		}

		highDigits = high;
		lowDigits = low;
	}

	public static String asString(Object obj)
	{
		return obj.toString();
	}

	public static String getNewClOrdID()
	{
		String clordID = "";

		synchronized(ordID)
		{
            if(currentTimeMillis == null)
			{
				currentTimeMillis = System.currentTimeMillis();
			}

			++ordID;
			clordID = String.format( ClientOrderID_FROMAT,
					formatter1.get().format(currentTimeMillis ), formatter2.get().format(ordID));

		}

		return clordID;
	}

    public static String getNewShortClOrdID(String userKey)
	{
		String clordID = "";

		synchronized(ordID)
		{
            if(currentTimeMillis == null)
			{
				currentTimeMillis = System.currentTimeMillis();
			}

			++ordID;

			if( userKey.length() > 24  )
			{
				return userKey.substring( 0, 24 );
			}
			char[] arrToFill = new char[24 - userKey.length() ];
			Arrays.fill(arrToFill, '0');
			DecimalFormat formatter = new DecimalFormat(new String(arrToFill));
			clordID = formatter.format(ordID) + userKey ;
		}

		return clordID;
	}

    public static IdleStatus
    translateMinaIdleStatus(org.apache.mina.core.session.IdleStatus minaStatus)
	{
		if (minaStatus == org.apache.mina.core.session.IdleStatus.READER_IDLE)
		{
            return IdleStatus.READER_IDLE;
		}
		else if (minaStatus == org.apache.mina.core.session.IdleStatus.WRITER_IDLE)
		{
            return IdleStatus.WRITER_IDLE;
		}
		else if (minaStatus == org.apache.mina.core.session.IdleStatus.BOTH_IDLE)
		{
            return IdleStatus.BOTH_IDLE;
		}
		else
		{
			throw new UnknownMina2SessionIdleStatusException(
					String.format( "Unknown MINA2 session status [%s].", minaStatus.toString() ));
		}
	}

	public static String getMachineIP()
	{
		String machineIP = "127.0.0.1";

		try
		{
            return InetAddress.getLocalHost().getHostAddress();
		}
		catch( UnknownHostException e )
		{
			logger.error("Can not resolve IP address: {}", machineIP, e);
		}
		return machineIP;

	}

	public static String getRandomString( int length )
	{
		StringBuffer randomString = new StringBuffer();

		for( int i = 0 ; i < length; i++ )
		{
			randomString.append( (char)( 65 + RND.nextInt(90 - 65)));
		}
		return randomString.toString();
	}

	/**
     * Generate TransactTime according NTG specification v1.04.<br>
	 * The first 4 bytes of the TransactTime timestamp will represent
	 * the Unix (Posix) time while the next 4 bytes will specify the micro seconds.
	 * @return
	 */
	public static String getTransactTime()
	{
        return getTransactTime(getTransactTimeMillisecond());
	}
	
	/**
     * Generate TransactTime according NTG specification v1.04.<br>
     * The first 4 bytes of the TransactTime timestamp will represent
     * the Unix (Posix) time while the next 4 bytes will specify the micro seconds.
     * @return
     */
    public static LocalDateTime getTransactTimeAsDate()
    {
        return getTransactTimeAsDate(getTransactTimeMillisecond());
    }

    /**
     * Generate TransactTime in millisecond according NTG specification
     * v1.04.<br>
     * The first 4 bytes of the TransactTime timestamp will represent the Unix
     * (Posix) time while the next 4 bytes will specify the micro seconds.
     * 
     * @return
     */
    private static long getTransactTimeMillisecond() {
        long current = System.currentTimeMillis();
        long seconds = current / 1000;
        long microseconds = (current % 1000) * 1000L;
        return (microseconds << 32) + seconds;

    }

    private static final ThreadLocal<DecimalFormat> millisFormat = new ThreadLocal<DecimalFormat>() {
        @Override
        protected DecimalFormat initialValue() {
            return new DecimalFormat("000000");
        }
    };
    
    public static LocalDateTime getTransactTimeAsDate(long time)
	{
        long microseconds = extarctMicroseconds(time);
        long seconds = extarctSeconds(time);
		
        return DateTimeUtility.toLocalDateTime(seconds * 1000, (int) microseconds * 1_000);
	}

    public static LocalDateTime getTransactTimeAsDate(String time) {
        int year = Integer.parseInt(time.substring(0, 4));
        int month = Integer.parseInt(time.substring(4, 6));
        int day = Integer.parseInt(time.substring(6, 8));
        int hour = Integer.parseInt(time.substring(9, 11));
        int minute = Integer.parseInt(time.substring(12, 14));
        int second = Integer.parseInt(time.substring(15, 17));
        int microseconds = Integer.parseInt(time.substring(18, 24));

        return LocalDateTime.of(year, month, day, hour, minute, second).withNano(microseconds * 1_000);
    }

	public static String getTransactTime(long time)
    {
        long microseconds = extarctMicroseconds(time);
        long seconds = extarctSeconds(time);
        LocalDateTime date = DateTimeUtility.toLocalDateTime(seconds * 1000);
        return String.format( "%s.%s", SDF_FORMATTER.format(date)
                , millisFormat.get().format(microseconds));
    }

    /**
     * Extract microseconds from ntg time
     *
     * @return
     */
    private static long extarctMicroseconds(long ntgTime) {
        return ntgTime >> 32;
    }

    /**
     * Extract seconds from ntg time
     *
     * @return
     */
    private static long extarctSeconds(long ntgTime) {
        return ntgTime & 0x7FFFFFFF;
    }

	public static long getTransactTime(String time)
	{
		if (time == null) {
			throw new NullPointerException("TransactTime argument is null.");
		}

        return getTransactTime(getTransactTimeAsDate(time));
    }

    public static long getTransactTime(LocalDateTime localDateTime) {
        long microseconds = localDateTime.getNano() / 1_000;
        long seconds = DateTimeUtility.getMillisecond(localDateTime) / 1000;
        return (microseconds << 32) + seconds;
    }

	public static String getEntryTime()
	{
        return DateTimeUtility.nowLocalDateTime().format(ENTRY_FORMATTER);
	}

    private static LocalDateTime getTime()
	{
        return DateTimeUtility.nowLocalDateTime();
	}
	
	/**
	 * Return formatted date set to one day apart from current time
	 * @return formatted as yyyyMMdd-HH:mm:ss date
	 */
	public static int getExpireDateTime()
	{
        long time = System.currentTimeMillis();
		return (int)(time / 1000);
	}
	
	/**
     * Return formatted date time
     * @return formatted as yyyyMMdd-HH:mm:ss date
     */
    public static int getExpireDateTime(LocalDateTime date, String dateFormat)
    {
        LocalDateTime localDateTime = DateUtil.modifyTemporal(date, dateFormat);
        return (int) (DateTimeUtility.getMillisecond(localDateTime) / 1000);
    }
	
	/**
	 * Return formatted date time
	 * @return formatted as yyyyMMdd-HH:mm:ss date
	 */
	public static int getExpireDateTime(String dateFormat)
	{
	    return getExpireDateTime(getTime(), dateFormat);
	}
	
	public static int getSpecificExpireDateTime(int year, int month, int day, int hour, int minute, int second) {
        LocalDateTime localDateTime = LocalDateTime.of(year, month, day, hour, minute, second);
        return (int) (DateTimeUtility.getMillisecond(localDateTime) / 1000);
	}

	/**
	 * Dumps an IoBuffer to a hex formatted string.
	 *
	 * @param in the buffer to dump
	 * @param lengthLimit the limit at which hex dumping will stop
	 * @return a hex formatted string representation of the in IoBuffer.
	 */
    public static String getHexdump(IoBuffer in, int lengthLimit)
	{
		if (lengthLimit == 0)
		{
			throw new IllegalArgumentException("lengthLimit: " + lengthLimit
					+ " (expected: 1+)");
		}

		boolean truncate = in.remaining() > lengthLimit;
        int size = truncate ? lengthLimit : in.remaining();

        if(size == 0)
		{
			return "empty";
		}

		StringBuilder out = new StringBuilder(size * 2 + 2);

		int mark = in.position();

		// fill the first
		int byteValue = in.get() & 0xFF;
		out.append((char) highDigits[byteValue]);
		out.append((char) lowDigits[byteValue]);
		size--;

		// and the others, too
		for (; size > 0; size--)
		{
			//out.append(' ');
			byteValue = in.get() & 0xFF;
			out.append((char) highDigits[byteValue]);
			out.append((char) lowDigits[byteValue]);
		}

		in.position(mark);

		if (truncate)
		{
			out.append("...");
		}

		return out.toString();
	}
}
