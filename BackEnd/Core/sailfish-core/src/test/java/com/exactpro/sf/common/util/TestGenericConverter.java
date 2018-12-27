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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.util.EPSTestCase;

import junit.framework.AssertionFailedError;


public final class TestGenericConverter extends  EPSTestCase
{
	private static final Logger logger = LoggerFactory.getLogger(TestGenericConverter.class);
	private static Charset charsetISO_8859 = Charset.forName("ISO-8859-1");

	@SuppressWarnings("unused")
	private static CharsetDecoder charsetDecoder = charsetISO_8859.newDecoder();
	private static CharsetEncoder charsetEncoder = charsetISO_8859.newEncoder();

	@Test
	public void testConvertStringToArray() throws Exception
	{
		int length = 150;
		String testString = "This is a String to test FromStringToArray() method of GenericConverter class";

		logger.info("Test of GenericConverter.FromStringToArray() - with string [{}] for length [{}].", testString, length);

		try
		{
			byte[] encoded = GenericConverter.convertStringToArray(length, testString);
			DumpAsString(encoded);

			String model = String.format( "%-" + length + "s", testString );
			ByteBuffer byteBuffer = charsetEncoder.encode( CharBuffer.wrap(model.toCharArray()) ) ;

			// compare lengths
			Assert.assertTrue( encoded.length == length );

			// compare each byte
			for(int i = 0 ; i < length; i++ )
			{
				String msg = String.format( "Character at index [%d]. ", i );
				Assert.assertTrue(msg, byteBuffer.array()[i] == encoded[i]);
			}

			logger.info("Test of GenericConverter.FromStringToArray(): PASSED.");
		}
		catch(AssertionFailedError aseEx)
		{
			logger.error("Test of GenericConverter.FromStringToArray(): FAILED.");
			logger.error(aseEx.getMessage(), aseEx);
			throw aseEx;
		}
		catch ( Exception ex )
		{
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
			throw ex;
		}
	}

	@Test
	public void testConvertUnsignedIntegerToArray() throws Exception
	{
		int value = 1234567;
		int length = 10;

		try
		{
			byte[] encoded = GenericConverter.convertUnsignedNumericToArray(length, value);
			DumpAsString(encoded);

			char[] arrFiller = new char[length - String.format( "%d", value ).length()];
			Arrays.fill(arrFiller, '0');

			String model = new String( arrFiller ) + String.format( "%d", value );
			ByteBuffer byteBuffer = charsetEncoder.encode(  CharBuffer.wrap(model.toCharArray()) ) ;
			logger.info("testConvertUnsignedIntegerToArray model = {}", model );

			// compare lengths
			Assert.assertTrue( byteBuffer.array().length == encoded.length );

			int index = 0 ;

			// compare each byte
			for(byte byteModel : byteBuffer.array())
			{
				String msg = String.format( "Character at index [%d]. ", index );
				Assert.assertTrue(msg, byteModel == encoded[index]);
				++index;
			}
			logger.info("GenericConverter.FromUnsignedIntegerToArray() - test passed.");
		}
		catch(AssertionFailedError aseEx)
		{
			logger.error("Test of GenericConverter.FromUnsignedIntegerToArray(): FAILED.");
			logger.error(aseEx.getMessage(), aseEx);
			throw aseEx;
		}
		catch ( Exception ex )
		{
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
			throw ex;
		}
	}

	@Test
	public void testConvertUnsignedLongToArray() throws Exception
	{
		Long value = 1234567L;
		int length = 12;

		try
		{
			byte[] encoded = GenericConverter.convertUnsignedNumericToArray( length, value );
			DumpAsString(encoded);

			char[] arrFiller = new char[length - String.format( "%d", value ).length()];
			Arrays.fill(arrFiller, '0');

			String model = new String( arrFiller ) + String.format( "%d", value );

			logger.info("testConvertUnsignedIntegerToArray() model = {}", model );

			ByteBuffer byteBuffer = charsetEncoder.encode(  CharBuffer.wrap(model.toCharArray()) ) ;

			int index = 0 ;

			// compare lengths
			Assert.assertTrue( byteBuffer.array().length == encoded.length );

			// compare each byte
			for(byte byteModel : byteBuffer.array())
			{
				String msg = String.format( "Character at index [%d]. ", index );
				Assert.assertTrue(msg, byteModel == encoded[index]);
				++index;
			}
			logger.info("GenericConverter.FromUnsignedLongToArray() - test passed.");
		}
		catch(AssertionFailedError aseEx)
		{
			logger.error("Test of GenericConverter.FromUnsignedLongToArray(): FAILED.");
			logger.error(aseEx.getMessage(), aseEx);
			throw aseEx;
		}
		catch ( Exception ex )
		{
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
			throw ex;
		}
	}

	@Test
	public void testConvertUnsignedDoubleToArray() throws Exception
	{
		double value = 1234567.88;
		int length = 16;
		int precision = 5;

		try
		{
			byte[] encoded = GenericConverter.convertUnsignedNumericToArray( length, value, precision );
			DumpAsString(encoded);
			String model = "0000123456788000";

			logger.info("testConvertUnsignedIntegerToArray model = {}", model );

			ByteBuffer byteBuffer = charsetEncoder.encode(  CharBuffer.wrap(model.toCharArray()) ) ;
			int index = 0 ;

			// compare lengths
			Assert.assertTrue( byteBuffer.array().length == encoded.length );

			// compare each byte
			for(byte byteModel : byteBuffer.array())
			{
				String msg = String.format( "Character at index [%d]. ", index );
				Assert.assertTrue(msg, byteModel == encoded[index]);
				++index;
			}
			logger.info("GenericConverter.FromUnsignedDoubleToArray() - test passed.");
		}
		catch(AssertionFailedError aseEx)
		{
			logger.error("Test of GenericConverter.FromUnsignedDoubleToArray(): FAILED.");
			logger.error(aseEx.getMessage(), aseEx);
			throw aseEx;
		}
		catch ( Exception ex )
		{
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
			throw ex;
		}
	}

	@Test
	public void testConvertSignedIntegerToArray() throws Exception
	{
		int value = -1234567;
		int length = 10;

		try
		{
			byte[] encoded = GenericConverter.convertSignedNumericToArray(length, value);
			DumpAsString(encoded);
			String model = "-001234567";

			logger.info("testConvertSignedIntegerToArray model = {}", model );

			ByteBuffer byteBuffer = charsetEncoder.encode(  CharBuffer.wrap(model.toCharArray()) ) ;
			int index = 0 ;

			// compare lengths
			Assert.assertTrue( byteBuffer.array().length == encoded.length );

			// compare each byte
			for(byte byteModel : byteBuffer.array())
			{
				String msg = String.format( "Character at index [%d]. ", index );
				Assert.assertTrue(msg, byteModel == encoded[index]);
				++index;
			}

			value = 1234567;
			encoded = GenericConverter.convertSignedNumericToArray(length, value);
			DumpAsString(encoded);
			model = "+001234567";

			logger.info("testConvertSignedIntegerToArray model = {}", model );

			byteBuffer = charsetEncoder.encode(  CharBuffer.wrap(model.toCharArray()) ) ;
			index = 0 ;

			// compare lengths
			Assert.assertTrue( byteBuffer.array().length == encoded.length );

			// compare each byte
			for(byte byteModel : byteBuffer.array())
			{
				String msg = String.format( "Character at index [%d]. ", index );
				Assert.assertTrue(msg, byteModel == encoded[index]);
				++index;
			}
			logger.info("GenericConverter.FromSignedIntegerToArray() - test passed.");
		}
		catch(AssertionFailedError aseEx)
		{
			logger.error("GenericConverter.FromSignedIntegerToArray() - test FAILED.");
			logger.error(aseEx.getMessage(), aseEx);
			throw aseEx;
		}
		catch ( Exception ex )
		{
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
			throw ex;
		}
	}

	@Test
	public void testConvertSignedLongToArray() throws Exception
	{
		long value = 1234567L;
		int length = 12;

		try
		{
			byte[] encoded = GenericConverter.convertSignedNumericToArray( length, value );
			DumpAsString(encoded);

			String model = "+00001234567";
			logger.info("testConvertSignedLongToArray model = {}", model );

			ByteBuffer byteBuffer = charsetEncoder.encode(  CharBuffer.wrap(model.toCharArray()) ) ;

			int index = 0 ;

			// compare lengths
			Assert.assertTrue( byteBuffer.array().length == encoded.length );

			// compare each byte
			for(byte byteModel : byteBuffer.array())
			{
				String msg = String.format( "Character at index [%d]. ", index );
				Assert.assertTrue(msg, byteModel == encoded[index]);
				++index;
			}

			value = -1234567L;
			encoded = GenericConverter.convertSignedNumericToArray( length, value );
			DumpAsString(encoded);

			model = "-00001234567";
			logger.info("testConvertSignedLongToArray model = {}", model );

			byteBuffer = charsetEncoder.encode(  CharBuffer.wrap(model.toCharArray()) ) ;
			index = 0 ;

			// compare lengths
			Assert.assertTrue( byteBuffer.array().length == encoded.length );

			// compare each byte
			for(byte byteModel : byteBuffer.array())
			{
				String msg = String.format( "Character at index [%d]. ", index );
				Assert.assertTrue(msg, byteModel == encoded[index]);
				++index;
			}
			logger.info("GenericConverter.FromSignedLongToArray() - test passed.");
		}
		catch(AssertionFailedError aseEx)
		{
			logger.error("GenericConverter.FromSignedLongToArray() - test FAILED.");
			logger.error(aseEx.getMessage(), aseEx);
			throw aseEx;
		}
		catch ( Exception ex )
		{
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
			throw ex;
		}
	}

	@Test
	public void testConvertSignedDoubleToArray() throws Exception
	{
		double value = 1234567.88;
		int length = 16;
		int precision = 5;

		try
		{
			byte[] encoded = GenericConverter.convertSignedNumericToArray( length, value, precision );
			DumpAsString(encoded);
			String model = "+000123456788000";

			logger.info("testConvertSignedDoubleToArray model = {}", model );

			ByteBuffer byteBuffer = charsetEncoder.encode(  CharBuffer.wrap(model.toCharArray()) ) ;
			int index = 0 ;

			// compare lengths
			Assert.assertTrue( byteBuffer.array().length == encoded.length );

			// compare each byte
			for(byte byteModel : byteBuffer.array())
			{
				String msg = String.format( "Character at index [%d]. ", index );
				Assert.assertTrue(msg, byteModel == encoded[index]);
				++index;
			}

			value = -1234567.88;
			encoded = GenericConverter.convertSignedNumericToArray( length, value, precision );
			DumpAsString(encoded);
			model = "-000123456788000";

			logger.info("testConvertSignedDoubleToArray model = {}", model );

			byteBuffer = charsetEncoder.encode(  CharBuffer.wrap(model.toCharArray()) ) ;
			index = 0 ;

			// compare lengths
			Assert.assertTrue( "Lengths do not equal.", byteBuffer.array().length == encoded.length );

			// compare each byte
			for(byte byteModel : byteBuffer.array())
			{
				String msg = String.format( "Character at index [%d]. ", index );
				Assert.assertTrue(msg, byteModel == encoded[index]);
				++index;
			}
			logger.info("GenericConverter.FromSignedDoubleToArray() - test passed.");
		}
		catch(AssertionFailedError aseEx)
		{
			logger.error("GenericConverter.FromSignedDoubleToArray() - test FAILED.");
			logger.error(aseEx.getMessage(), aseEx);
			throw aseEx;
		}
		catch ( Exception ex )
		{
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
			throw ex;
		}
	}

	@Test
	public void testConvertUnsignedDecimalToArray() throws Exception
	{
		BigDecimal value = new BigDecimal( 123456790.54321 );
		int length = 20;
		int precision = 7;

		try
		{
			byte[] encoded = GenericConverter.convertUnsignedNumericToArray( length, value, precision );
			DumpAsString(encoded);
			String model = "00001234567905432100";

			logger.info("testConvertUnsignedDecimalToArray model = {}", model );

			ByteBuffer byteBuffer = charsetEncoder.encode(  CharBuffer.wrap(model.toCharArray()) ) ;
			int index = 0 ;

			// compare lengths
			Assert.assertTrue( byteBuffer.array().length == encoded.length );

			// compare each byte
			for(byte byteModel : byteBuffer.array())
			{
				String msg = String.format( "Character at index [%d]. ", index );
				Assert.assertTrue(msg, byteModel == encoded[index]);
				++index;
			}

			logger.info("GenericConverter.FromUnsignedBigDecimalToArray() - test passed.");
		}
		catch(AssertionFailedError aseEx)
		{
			logger.error("GenericConverter.FromUnsignedBigDecimalToArray() - test FAILED.");
			logger.error(aseEx.getMessage(), aseEx);
			throw aseEx;
		}
		catch ( Exception ex )
		{
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
			throw ex;
		}
	}

	@Test
	public void testConvertSignedDecimalToArray() throws Exception
	{
		BigDecimal value = new BigDecimal( 123456790.54321 );
		int length = 20;
		int precision = 7;

		try
		{
			byte[] encoded = GenericConverter.convertSignedNumericToArray( length, value, precision );
			String model = "+0001234567905432100";

			DumpAsString(encoded);
			logger.info("FromSignedBigDecimalToArray model = {}", model );

			ByteBuffer byteBuffer = charsetEncoder.encode(  CharBuffer.wrap(model.toCharArray()) ) ;
			int index = 0 ;

			// compare lengths
			Assert.assertTrue( byteBuffer.array().length == encoded.length );

			// compare each byte
			for(byte byteModel : byteBuffer.array())
			{
				String msg = String.format( "Character at index [%d]. ", index );
				Assert.assertTrue(msg, byteModel == encoded[index]);
				++index;
			}

			value = new BigDecimal( -123456790.54321 );
			model = "-0001234567905432100";

			encoded = GenericConverter.convertSignedNumericToArray( length, value, precision );
			DumpAsString(encoded);

			logger.info("FromSignedBigDecimalToArray model = {}", model );

			byteBuffer = charsetEncoder.encode(  CharBuffer.wrap(model.toCharArray()) ) ;

			// compare lengths
			Assert.assertTrue( byteBuffer.array().length == encoded.length );

			index = 0 ;
			// compare each byte
			for(byte byteModel : byteBuffer.array())
			{
				String msg = String.format( "Character at index [%d]. ", index );
				Assert.assertTrue(msg,  byteModel == encoded[index]);
				++index;
			}

			logger.info("GenericConverter.FromSignedBigDecimalToArray() - test passed.");
		}
		catch(AssertionFailedError aseEx)
		{
			logger.error("GenericConverter.FromSignedBigDecimalToArray() - test FAILED.");
			logger.error(aseEx.getMessage(), aseEx);
			throw aseEx;
		}
		catch ( Exception ex )
		{
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
			throw ex;
		}
	}

	@Test
	public void testConvertByteBufferToString() throws Exception
	{
		int length = 150;
		String testString = "This is a String to test FromByteArrayToString() method of GenericConverter class";

		logger.info("Test of FromByteArrayToString() - with string [{}] for length [{}].", testString, length);

		try
		{
			byte[] encoded = GenericConverter.convertStringToArray(length, testString);
			DumpAsString(encoded);

			String model = String.format( "%-" + length + "s", testString );
			String fromByteArray = GenericConverter.convertByteArrayToString( length, encoded);

			// compare lengths
			Assert.assertTrue( "Lengths do not equal", fromByteArray.length() == model.length() );

			Assert.assertTrue("Model string does not match with restored from bytes array.",
					model.equals( fromByteArray ));

			logger.info("Test of GenericConverter.FromStringToArray(): PASSED.");
		}
		catch(AssertionFailedError aseEx)
		{
			logger.error("Test of GenericConverter.FromStringToArray() - test FAILED.");
			logger.error(aseEx.getMessage(), aseEx);
			throw aseEx;
		}
		catch ( Exception ex )
		{
			logger.error("Test of GenericConverter.FromStringToArray(): FAILED.");
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
			throw ex;
		}
	}

	@Test
	public void testConvertByteBufferToAnyInteger() throws Exception
	{
		int length = 9;
		Integer model = 12344321;

		logger.info("Test of GenericConverter.FromByteArrayToAnyInteger(),  value [{}] for length [{}].", model, length);

		try
		{
			byte[] encoded = GenericConverter.convertUnsignedNumericToArray(length, model);

			Integer fromByteArray = GenericConverter.convertByteArrayToAnyInteger(length, encoded );
			Assert.assertEquals( "Unsigned Integer values does not equal.", model, fromByteArray );

			encoded = GenericConverter.convertSignedNumericToArray(length, model);
			fromByteArray = GenericConverter.convertByteArrayToAnyInteger(length, encoded );
			Assert.assertEquals( "Positive Signed Integer values does not equal.", model, fromByteArray );

			model = -123321;
			encoded = GenericConverter.convertSignedNumericToArray(length, model);
			fromByteArray = GenericConverter.convertByteArrayToAnyInteger(length, encoded );
			Assert.assertEquals( "Negative Signed Integer values does not equal.", model, fromByteArray );

			logger.info("Test of GenericConverter.FromByteArrayToAnyInteger(): PASSED.");
		}
		catch(AssertionFailedError aseEx)
		{
			logger.error("Test of GenericConverter.FromByteArrayToAnyInteger() - test FAILED.");
			logger.error(aseEx.getMessage(), aseEx);
			throw aseEx;
		}
		catch ( Exception ex )
		{
			logger.error("Test of GenericConverter.FromByteArrayToAnyInteger(): FAILED.");
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
			throw ex;
		}
	}

	@Test
	public void testConvertByteBufferToAnyLong() throws Exception
	{
		int length = 9;
		Long model = 12344321L;

		logger.info("Test of GenericConverter.FromByteArrayToAnyLong(),  value [{}] for length [{}].", model, length);

		try
		{
			byte[] encoded = GenericConverter.convertUnsignedNumericToArray(length, model);

			Long fromByteArray = GenericConverter.convertByteArrayToAnyLong(length, encoded );
			Assert.assertEquals( "Unsigned Long values does not equal.", model, fromByteArray );

			encoded = GenericConverter.convertSignedNumericToArray(length, model);
			fromByteArray = GenericConverter.convertByteArrayToAnyLong(length, encoded );
			Assert.assertEquals( "Positive Signed Long values does not equal.", model, fromByteArray );

			model = -123321L;
			encoded = GenericConverter.convertSignedNumericToArray(length, model);
			fromByteArray = GenericConverter.convertByteArrayToAnyLong(length, encoded );
			Assert.assertEquals( "Negative Signed Long values does not equal.", model, fromByteArray );

			logger.info("Test of GenericConverter.FromByteArrayToAnyLong(): PASSED.");
		}
		catch(AssertionFailedError aseEx)
		{
			logger.error("Test of GenericConverter.FromByteArrayToAnyLong() - test FAILED.");
			logger.error(aseEx.getMessage(), aseEx);
			throw aseEx;
		}
		catch ( Exception ex )
		{
			logger.error("Test of GenericConverter.FromByteArrayToAnyLong(): FAILED.");
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
			throw ex;
		}
	}

	@Test
	public void testConvertByteBufferToAnyDouble() throws Exception
	{
		int length = 16;
		int precision = 5;
		Double model = 12344321.9876;

		logger.info("Test of GenericConverter.FromByteArrayToAnyDouble(),  value [{}] for length [{}].", model, length);

		try
		{
			byte[] encoded = GenericConverter.convertUnsignedNumericToArray(length, model, precision);

			Double fromByteArray = GenericConverter.convertByteArrayToAnyDouble(length, encoded, precision );
			Assert.assertEquals( "Unsigned Double values does not equal.", model, fromByteArray );

			encoded = GenericConverter.convertSignedNumericToArray(length, model, precision);
			fromByteArray = GenericConverter.convertByteArrayToAnyDouble(length, encoded.clone(), precision );
			Assert.assertEquals( "Positive Signed Double values does not equal.", model, fromByteArray );

			model = -2344321.9876;
			encoded = GenericConverter.convertSignedNumericToArray(length, model, precision);
			fromByteArray = GenericConverter.convertByteArrayToAnyDouble(length, encoded, precision );
			Assert.assertEquals( "Negative Signed Double values does not equal.", model, fromByteArray );

			logger.info("Test of GenericConverter.FromByteArrayToAnyDouble(): PASSED.");
		}
		catch(AssertionFailedError aseEx)
		{
			logger.error("Test of GenericConverter.FromByteArrayToAnyDouble() - test FAILED.");
			logger.error(aseEx.getMessage(), aseEx);
			throw aseEx;
		}
		catch ( Exception ex )
		{
			logger.error("Test of GenericConverter.FromByteArrayToAnyLong(): FAILED.");
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
			throw ex;
		}
	}

	@Test
	public void testConvertByteBufferToAnyBigDecimal() throws Exception
	{
		int length = 16;
		int precision = 5;
		BigDecimal model = new BigDecimal( "12344321.9876" );

		logger.info("Test of GenericConverter.FromByteArrayToAnyDecimal(),  value [{}] for length [{}].", model, length);

		try
		{
			byte[] encoded = GenericConverter.convertUnsignedNumericToArray(length, model, precision);

			BigDecimal fromByteArray = GenericConverter.convertByteArrayToAnyDecimal(length, encoded, precision );
			Assert.assertTrue( "Unsigned BigDecimal values does not equal.", 0 == model.compareTo( fromByteArray ));

			model = new BigDecimal( "12344321.9876" );
			encoded = GenericConverter.convertSignedNumericToArray(length, model, precision);
			fromByteArray = GenericConverter.convertByteArrayToAnyDecimal(length, encoded, precision );

			BigDecimal zero = new BigDecimal( "0.0" );
			BigDecimal substr = model.subtract( fromByteArray );
			boolean zeroResult = ( 0 == substr.compareTo( zero ));
			Assert.assertTrue( "Positive Signed BigDecimal values does not equal.", zeroResult );

			model = new BigDecimal( "-2344321.9876" );
			encoded = GenericConverter.convertSignedNumericToArray(length, model, precision);
			fromByteArray = GenericConverter.convertByteArrayToAnyDecimal(length, encoded, precision );

			zero = new BigDecimal( "0.0" );
			substr = model.subtract( fromByteArray );
			zeroResult = ( 0 == substr.compareTo( zero ));
			Assert.assertTrue( "Negative Signed BigDecimal values does not equal.", zeroResult );

			logger.info("Test of GenericConverter.FromByteArrayToAnyDecimal(): PASSED.");
		}
		catch(AssertionFailedError aseEx)
		{
			logger.error("Test of GenericConverter.FromByteArrayToAnyDecimal() - test FAILED.");
			logger.error(aseEx.getMessage(), aseEx);
			throw aseEx;
		}
		catch ( Exception ex )
		{
			logger.error("Test of GenericConverter.FromByteArrayToAnyDecimal(): FAILED.");
			logger.error(ex.getMessage(), ex);
			ex.printStackTrace();
			throw ex;
		}
	}

	private void DumpAsString(byte[] arr)
	{
		StringBuffer asString = new StringBuffer();

		for( byte bt : arr)
		{
			asString.append((char) bt );
		}
		logger.info("DumpAsString() - [{}]", asString);
	}
}
