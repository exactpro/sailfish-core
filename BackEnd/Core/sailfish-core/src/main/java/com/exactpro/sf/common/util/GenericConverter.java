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
package com.exactpro.sf.common.util;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;

public final class GenericConverter
{
	private static Charset charsetISO_8859 = Charset.forName("ISO-8859-1");
	//private static CharsetDecoder charsetDecoder = charsetISO_8859.newDecoder();
	//private static CharsetEncoder charsetEncoder = charsetISO_8859.newEncoder();

	public GenericConverter()
	{
	}


	/**
	 * Convert String to byte array.
	 * Characters in result byte array will be <b>left aligned</b>.
	 * Missed characters will be filled by ' '(space).
	 * Redundant characters will be truncated.
	 * @param length - length of return array
	 * @param value
	 * @return byte array
	 */
	public static byte[] convertStringToArray(int length, String value)
	{
		byte[] byteArray = String.format( "%-" + length + "s", value == null ? "" : value ).getBytes(charsetISO_8859);

		if (byteArray.length != length)
			return Arrays.copyOf(byteArray, length); // truncate array
		else
			return byteArray;
	}

	public static <T extends Number> byte[] convertSignedNumericToArray(int length, T value )
	{
		return convertSignedNumericToArray( length, value, 0 );
	}


	public static <T extends Number> byte[] convertSignedNumericToArray(int length, T value, int precision )
	{
		byte[] byteArray = new byte[length];

		// Pattern for fractional part
		char[] arrFractional = new char[precision];
		Arrays.fill(arrFractional, '0');

		// Pattern for mantissa
		char[] arrMantissa = new char[length - 1 - precision ];
		Arrays.fill(arrMantissa, '0');

		String patternDesired = String.format( "%s.%s", new String(arrMantissa), new String(arrFractional));
		DecimalFormat formatter = new DecimalFormat(patternDesired );
		formatter.setPositivePrefix("+");
		formatter.setNegativePrefix("-");
		formatter.setGroupingUsed(false);
		String formattedValue = formatter.format(value);
		DecimalFormatSymbols decimalFormatSymbol = formatter.getDecimalFormatSymbols();
		formattedValue = formattedValue.replace(String.valueOf(decimalFormatSymbol.getDecimalSeparator()), "");
		byteArray = formattedValue.getBytes(charsetISO_8859);
		return byteArray;
	}


	public static <T extends Number> byte[] convertUnsignedNumericToArray(int length, T value )
	{
		return convertUnsignedNumericToArray(length, value, 0 );
	}


	public static <T extends Number> byte[] convertUnsignedNumericToArray(int length, T value, int precision )
	{
		byte[] byteArray = new byte[length];

		// Pattern for fractional part
		char[] arrFractional = new char[precision];
		Arrays.fill(arrFractional, '0');

		// Pattern for mantissa
		char[] arrMantissa = new char[length - precision ];
		Arrays.fill(arrMantissa, '0');

		String patternDesired = String.format( "%s.%s", new String(arrMantissa), new String(arrFractional));
		DecimalFormat formatter = new DecimalFormat(patternDesired );
		formatter.setPositivePrefix("");
		formatter.setNegativePrefix("");
		formatter.setGroupingUsed(false);
		String formattedValue = formatter.format(value);
		DecimalFormatSymbols decimalFormatSymbol = formatter.getDecimalFormatSymbols();
		formattedValue = formattedValue.replace(String.valueOf(decimalFormatSymbol.getDecimalSeparator()), "");
		byteArray = formattedValue.getBytes(charsetISO_8859);
		return byteArray;
	}


	public static String convertByteArrayToString( int length, byte[] byteArray )
	{
		return new String(byteArray, charsetISO_8859);
		//		// DG: this loop is to correctly convert bytes >= 128 to chars
		//		// try byte[]{-128} compare with char[]{128}
		//		char[] charArray = new char[byteArray.length];
		//		for (int i=0; i<byteArray.length; i++) {
		//			charArray[i] = (char)((0x000000FF & (int)byteArray[i]));
		//		}
		//
		//		String stringValue = new String( charArray );
		//
		//		if(length != stringValue.length())
		//		{
		//			String errMsg =
		//				String.format("Length [%d] of the string, created from " +
		//						"the bytes array, does not equal to the supplied length [%d].",
		//						stringValue.length(), length);
		//
		//			logger.error(errMsg);
		//			throw new EPSCommonException( errMsg );
		//		}
		//		return stringValue;
	}

	public static Integer convertByteArrayToAnyInteger( int length, byte[] byteArray )
	{
		String stringValue = convertByteArrayToString( length, byteArray );
		Double parsedDouble = Double.parseDouble( stringValue );
		return parsedDouble.intValue();
	}

	public static Long convertByteArrayToAnyLong( int length, byte[] byteArray )
	{
		String stringValue = convertByteArrayToString( length, byteArray );
		return new BigDecimal( stringValue ).longValue();
	}

	public static Double convertByteArrayToAnyDouble( int length, byte[] byteArray, int precision )
	{
		String stringValue = convertByteArrayToString( length, byteArray );

		if(0 != precision)
		{
			stringValue = ((new StringBuffer(stringValue )).insert(length - precision, ".")).toString();
		}
		return Double.parseDouble( stringValue );
	}

	public static BigDecimal convertByteArrayToAnyDecimal( int length, byte[] byteArray, int precision )
	{
		String stringValue = convertByteArrayToString( length, byteArray );

		if(0 != precision)
		{
			stringValue = ((new StringBuffer(stringValue )).insert(length - precision, ".")).toString();
		}
		return new BigDecimal( stringValue );
	}
}
