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

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.impl.AttributeStructure;
import com.exactpro.sf.common.messages.structures.impl.FieldStructure;
import com.exactpro.sf.services.ntg.exceptions.TooLongStringValueException;
import com.exactpro.sf.util.AbstractTest;

public final class TestNTGVisitorPositive extends AbstractTest {
    private static final Logger logger = LoggerFactory.getLogger(TestNTGVisitorPositive.class);

	@BeforeClass
	public static void setUpClass(){
        logger.info("Start positive tests of NTGCodec");
	}

	@AfterClass
	public static void tearDownClass(){
        logger.info("Finish positive tests of NTGCodec");
	}
	/**
	 * Test case for String data type (null terminated)
	 */
	@Test
    public void testNTGVisitorEncode_String_With_Length_LessThen_FieldLength()
	{
		String value = "Test string value.";
		int offset = 0 ;
		int length = 30;
		String fldName = "FieldString";
        String fldNamespace = "NTG";
		String fldDescr = "Description";

		Map<String, IAttributeStructure> protocolAttributes = new HashMap<>();

        protocolAttributes.put(NTGProtocolAttribute.Offset.toString(), new AttributeStructure(
                NTGProtocolAttribute.Offset.toString(), Integer.toString(offset), offset, JavaType.JAVA_LANG_INTEGER));

        protocolAttributes.put(NTGProtocolAttribute.Format.toString(), new AttributeStructure(
                NTGProtocolAttribute.Format.toString(), NTGFieldFormat.A.toString(), NTGFieldFormat.A.toString(),
				JavaType.JAVA_LANG_STRING));

        protocolAttributes.put(NTGProtocolAttribute.Length.toString(), new AttributeStructure(
                NTGProtocolAttribute.Length.toString(), Integer.toString(length), length, JavaType.JAVA_LANG_INTEGER));

		IFieldStructure mockSimpleField =
			new FieldStructure(fldName, fldNamespace, fldDescr, null,
					protocolAttributes, null, JavaType.JAVA_LANG_STRING, false, false, false, null);

		String expectedValue = "Test string value.\0          ";

		byte[] byteArrayExpected = String.format( "%-" + length + "s", expectedValue )
		.getBytes(Charset.forName("ISO-8859-1"));

		try
		{

			FieldStructure fldStruct = new FieldStructure(fldName, fldNamespace,
					fldDescr, null, protocolAttributes, null, JavaType.JAVA_LANG_STRING,
					true, false, false, value) ;

            NTGVisitorEncode visitor = new NTGVisitorEncode();
            visitor.visit(mockSimpleField.getName(), value, fldStruct, false);
            IoBuffer ioBuffer = visitor.getBuffer();

			Assert.assertTrue( ioBuffer.position() == byteArrayExpected.length );

			for( int i = 0 ; i < byteArrayExpected.length; i++ )
			{
				logger.trace("Symbol at index [{}]. Expected [%s], got [{}].", i,
						new String( new byte[] { ioBuffer.array()[i] }, "ISO-8859-1" ),
						new String( new byte[] { byteArrayExpected[i] }, "ISO-8859-1" ));

                Assert.assertEquals(String.format("NTGVisitor.visit(String) failed. " +
						"Error at index [%d]. Expected [%s], got [%s].", i,
						new String( new byte[] { ioBuffer.array()[i] }, "ISO-8859-1" ),
						new String( new byte[] { byteArrayExpected[i] }, "ISO-8859-1" )),
						ioBuffer.array()[i], byteArrayExpected[i]);
			}
            logger.info("Visitor: method visit(String)  PASSED.");
		}
		catch(Exception ex)
		{
			logger.error(ex.getMessage(),ex);
			Assert.fail(ex.getMessage());

		}
	}

	/**
	 * Testing too long string case.
	 * Visitor must fire EPSCommonException exception.
	 */
	@Test
    public void testNTGVisitorEncode_StringField_With_TooLongString()
	{
		String value = "This is a string wiht length=65  which exceeds target field length.";
		int offset = 0 ;
		int length = value.length() - 10;
		String fldName = "FieldString";
        String fldNamespace = "NTG";
		String fldDescr = "Description";

		Map<String, IAttributeStructure> protocolAttributes = new HashMap<>();

        protocolAttributes.put(NTGProtocolAttribute.Offset.toString(), new AttributeStructure(
                NTGProtocolAttribute.Offset.toString(), Integer.toString(offset), offset, JavaType.JAVA_LANG_INTEGER));

        protocolAttributes.put(NTGProtocolAttribute.Format.toString(), new AttributeStructure(
                NTGProtocolAttribute.Format.toString(), NTGFieldFormat.A.toString(), NTGFieldFormat.A.toString(),
				JavaType.JAVA_LANG_STRING));

        protocolAttributes.put(NTGProtocolAttribute.Length.toString(), new AttributeStructure(
                NTGProtocolAttribute.Length.toString(), Integer.toString(length), length, JavaType.JAVA_LANG_INTEGER));

		IFieldStructure mockSimpleField =
				new FieldStructure(fldName, fldNamespace, fldDescr, null,
						protocolAttributes, null, JavaType.JAVA_LANG_STRING, false, false, false, null);

		try
		{
			IFieldStructure fldStruct = new FieldStructure(fldName, fldNamespace,
					fldDescr, null, protocolAttributes, null, JavaType.JAVA_LANG_STRING,
					true, false, false, value);

            NTGVisitorEncode visitor = new NTGVisitorEncode();

			try
			{
				// Expected TooLongStringValueException on call next method
                visitor.visit(mockSimpleField.getName(), value, fldStruct, false);
			}
			catch( TooLongStringValueException tlsEx )
			{
				// That'a OK
                logger.info("Visitor: method visit(String) with too long value - Passed. Exception TooLongStringValueException has been caught.");
			}
			catch(Exception ex)
			{
				// That'a NOT
                logger.info("Visitor: method visit(String) with too long value - FAILED.");
				logger.error(ex.getMessage(),ex);
				Assert.fail(ex.getMessage());
			}

            logger.info("Visitor: method visit(String) with too long value - PASSED.");
		}
		catch(Exception ex)
		{
            logger.info("Visitor: method visit(String) with too long value - FAILED.");
			logger.error(ex.getMessage(),ex);
			Assert.fail(ex.getMessage());
		}
	}

	@Test
    public void testNTGVisitorEncode_StringField_With_NullValue()
	{
		String value = null;
		int offset = 0 ;
		int length = 27;
		String fldName = "FieldString";
        String fldNamespace = "NTG";
		String fldDescr = "Description";

		Map<String, IAttributeStructure> protocolAttributes = new HashMap<>();

        protocolAttributes.put(NTGProtocolAttribute.Offset.toString(), new AttributeStructure(
                NTGProtocolAttribute.Offset.toString(), Integer.toString(offset), offset, JavaType.JAVA_LANG_INTEGER));

        protocolAttributes.put(NTGProtocolAttribute.Format.toString(), new AttributeStructure(
                NTGProtocolAttribute.Format.toString(), NTGFieldFormat.A.toString(), NTGFieldFormat.A.toString(),
				JavaType.JAVA_LANG_STRING));

        protocolAttributes.put(NTGProtocolAttribute.Length.toString(), new AttributeStructure(
                NTGProtocolAttribute.Length.toString(), Integer.toString(length), length, JavaType.JAVA_LANG_INTEGER));

		IFieldStructure mockSimpleField =
				new FieldStructure(fldName, fldNamespace, fldDescr, null,
						protocolAttributes, null, JavaType.JAVA_LANG_STRING, false, false, false, null);

		String expectedValue = "";

		@SuppressWarnings("unused")
		byte[] byteArrayExpected = String.format( "%-" + length + "s", expectedValue )
		.getBytes(Charset.forName("ISO-8859-1"));

		try
		{
			IFieldStructure fldStruct = new FieldStructure(fldName, fldNamespace,
					fldDescr, null, protocolAttributes, null, JavaType.JAVA_LANG_STRING,
					true, false, false, value) ;

            NTGVisitorEncode visitor = new NTGVisitorEncode();
            visitor.visit(mockSimpleField.getName(), value, fldStruct, false);

            logger.info("Visitor: method visit(String) with NULL string - PASSED.");
		}
		catch(Exception ex)
		{
            logger.info("Visitor: method visit(String) with NULL string - Failed.");
			logger.error(ex.getMessage(),ex);
			Assert.fail(ex.getMessage());
		}
	}

	/**
	 * Test case aplha data type
	 */
	@Test
    public void testNTGVisitorEncode_AplhaField()
	{
		String value = "W";
		int offset = 0 ;
		int length = 1;
		String fldName = "FieldAplha";
        String fldNamespace = "NTG";
		String fldDescr = "Description";

		Map<String, IAttributeStructure> protocolAttributes = new HashMap<>();

        protocolAttributes.put(NTGProtocolAttribute.Offset.toString(), new AttributeStructure(
                NTGProtocolAttribute.Offset.toString(), Integer.toString(offset), offset, JavaType.JAVA_LANG_INTEGER));

        protocolAttributes.put(NTGProtocolAttribute.Format.toString(), new AttributeStructure(
                NTGProtocolAttribute.Format.toString(), NTGFieldFormat.A.toString(), NTGFieldFormat.A.toString(),
				JavaType.JAVA_LANG_STRING));

        protocolAttributes.put(NTGProtocolAttribute.Length.toString(), new AttributeStructure(
                NTGProtocolAttribute.Length.toString(), Integer.toString(length), length, JavaType.JAVA_LANG_INTEGER));

		IFieldStructure mockSimpleField =
				new FieldStructure(fldName, fldNamespace, fldDescr, null,
						protocolAttributes, null, JavaType.JAVA_LANG_STRING, false, false, false, null);

		String expectedValue = "W";

		byte[] byteArrayExpected = String.format( "%-" + length + "s", expectedValue )
		.getBytes(Charset.forName("ISO-8859-1"));

		try
		{

			IFieldStructure fldStruct = new FieldStructure(fldName, fldNamespace,
					fldDescr, null, protocolAttributes, null, JavaType.JAVA_LANG_STRING,
					true, false, false, value) ;

            NTGVisitorEncode visitor = new NTGVisitorEncode();
            visitor.visit(mockSimpleField.getName(), value, fldStruct, false);
            IoBuffer ioBuffer = visitor.getBuffer();

			Assert.assertTrue( ioBuffer.position() == byteArrayExpected.length );

			for( int i = 0 ; i < byteArrayExpected.length; i++ )
			{
				logger.trace("Symbol at index [{}]. Expected [{}], got [{}].", i,
						new String( new byte[] { ioBuffer.array()[i] }, "ISO-8859-1" ),
						new String( new byte[] { byteArrayExpected[i] }, "ISO-8859-1" ));

                Assert.assertEquals(String.format("NTGVisitor.visit(String) failed. " +
						"Error at index [%d]. Expected [%s], got [%s].", i,
						new String( new byte[] { ioBuffer.array()[i] }, "ISO-8859-1" ),
						new String( new byte[] { byteArrayExpected[i] }, "ISO-8859-1" )),
						ioBuffer.array()[i], byteArrayExpected[i]);
			}
            logger.info("Visitor: method visit(Aplha)  PASSED.");
		}
		catch(Exception ex)
		{
			logger.error(ex.getMessage(),ex);
			Assert.fail(ex.getMessage());

		}
	}

	/**
	 * Test case Integer data type
	 */
	@Test
    public void testNTGVisitorEncode_IntegerField()
	{
		Integer value = Integer.MAX_VALUE;
		int offset = 0 ;
		int length = 4;
		String fldName = "FieldInteger";
        String fldNamespace = "NTG";
		String fldDescr = "Description " + fldName;

		Map<String, IAttributeStructure> protocolAttributes = new HashMap<>();

        protocolAttributes.put(NTGProtocolAttribute.Offset.toString(), new AttributeStructure(
                NTGProtocolAttribute.Offset.toString(), Integer.toString(offset), offset, JavaType.JAVA_LANG_INTEGER));

        protocolAttributes.put(NTGProtocolAttribute.Format.toString(), new AttributeStructure(
                NTGProtocolAttribute.Format.toString(), NTGFieldFormat.S.toString(), NTGFieldFormat.S.toString(),
				JavaType.JAVA_LANG_STRING));

        protocolAttributes.put(NTGProtocolAttribute.Length.toString(), new AttributeStructure(
                NTGProtocolAttribute.Length.toString(), Integer.toString(length), length, JavaType.JAVA_LANG_INTEGER));

		IFieldStructure mockSimpleField =
				new FieldStructure(fldName, fldNamespace, fldDescr, null,
						protocolAttributes, null, JavaType.JAVA_LANG_INTEGER, false, false, false, null);

		ByteBuffer bb = ByteBuffer.wrap(new byte[length]);
		bb.order( ByteOrder.LITTLE_ENDIAN);
		bb.putInt(value);

		try
		{
			IFieldStructure fldStruct = new FieldStructure(fldName, fldNamespace,
					fldDescr, null, protocolAttributes, null, JavaType.JAVA_LANG_INTEGER,
					true, false, false, value.toString()) ;

            NTGVisitorEncode visitor = new NTGVisitorEncode();
            visitor.visit(mockSimpleField.getName(), value, fldStruct, false);
            IoBuffer ioBuffer = visitor.getBuffer();

			for( int i = 0 ; i < bb.array().length; i++ )
			{
				logger.trace("Symbol at index [{}]. Expected [{}], actual [{}].", i,
						bb.array()[i], ioBuffer.array()[i]);

                Assert.assertEquals(String.format("NTGVisitor.visit(Integer) failed. " +
						"Error at index [%d]. Expected [%x], actual [%x].", i,
						bb.array()[i], ioBuffer.array()[i]),
						bb.array()[i], ioBuffer.array()[i]);
			}

			IoBuffer copy = ioBuffer.duplicate();
			copy.order(ByteOrder.LITTLE_ENDIAN);
			copy.flip();
			Integer restored = copy.getInt();

			Assert.assertEquals( value, restored );

            logger.info("Visitor: method visit(Integer)  PASSED.");
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			logger.error(ex.getMessage(),ex);
			Assert.fail(ex.getMessage());
		}
	}

	/**
	 * Test case Long data type
	 */
	@Test
    public void testNTGVisitorEncode_LongField()
	{
		Long value = Long.MAX_VALUE;
		int offset = 0 ;
		int length = 8;
		String fldName = "FieldLong";
        String fldNamespace = "NTG";
		String fldDescr = "Description " + fldName;

		Map<String, IAttributeStructure> protocolAttributes = new HashMap<>();

        protocolAttributes.put(NTGProtocolAttribute.Offset.toString(), new AttributeStructure(
                NTGProtocolAttribute.Offset.toString(), Integer.toString(offset), offset, JavaType.JAVA_LANG_INTEGER));

        protocolAttributes.put(NTGProtocolAttribute.Format.toString(), new AttributeStructure(
                NTGProtocolAttribute.Format.toString(), NTGFieldFormat.S.toString(), NTGFieldFormat.S.toString(),
				JavaType.JAVA_LANG_STRING));

        protocolAttributes.put(NTGProtocolAttribute.Length.toString(), new AttributeStructure(
                NTGProtocolAttribute.Length.toString(), Integer.toString(length), length, JavaType.JAVA_LANG_INTEGER));

		IFieldStructure mockSimpleField =
				new FieldStructure(fldName, fldNamespace, fldDescr, null,
						protocolAttributes, null, JavaType.JAVA_LANG_LONG, false, false, false, null);

		ByteBuffer bb = ByteBuffer.wrap(new byte[length]);
		bb.order( ByteOrder.LITTLE_ENDIAN);
		bb.putLong(value);

		try
		{
			IFieldStructure fldStruct = new FieldStructure(fldName, fldNamespace,
					fldDescr, null, protocolAttributes, null, JavaType.JAVA_LANG_LONG,
					true, false, false, value.toString()) ;

            NTGVisitorEncode visitor = new NTGVisitorEncode();
            visitor.visit(mockSimpleField.getName(), value, fldStruct, false);
            IoBuffer ioBuffer = visitor.getBuffer();

			for( int i = 0 ; i < bb.array().length; i++ )
			{
				logger.trace("Symbol at index [{}]. Expected [{}], actual [{}].", i,
						bb.array()[i], ioBuffer.array()[i]);

                Assert.assertEquals(String.format("NTGVisitor.visit(Long) failed. " +
						"Error at index [%d]. Expected [%x], actual [%x].", i,
						bb.array()[i], ioBuffer.array()[i]),
						bb.array()[i], ioBuffer.array()[i]);
			}

			IoBuffer copy = ioBuffer.duplicate();
			copy.order(ByteOrder.LITTLE_ENDIAN);
			copy.flip();
			Long restored = copy.getLong();

			Assert.assertEquals( value, restored );

            logger.info("Visitor: method visit(Long)  PASSED.");
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			logger.error(ex.getMessage(),ex);
			Assert.fail(ex.getMessage());
		}
	}

	/**
	 * Test case Double data type
	 */
	@Test
    public void testNTGVisitorEncode_DoubleField()
	{
		Double value = (double) (Integer.MAX_VALUE/10000);
		int offset = 0 ;
		int length = 8;
		String fldName = "FieldDouble";
        String fldNamespace = "NTG";
		String fldDescr = "Description " + fldName;

		Map<String, IAttributeStructure> protocolAttributes = new HashMap<>();

        protocolAttributes.put(NTGProtocolAttribute.Offset.toString(), new AttributeStructure(
                NTGProtocolAttribute.Offset.toString(), Integer.toString(offset), offset, JavaType.JAVA_LANG_INTEGER));

        protocolAttributes.put(NTGProtocolAttribute.Format.toString(), new AttributeStructure(
                NTGProtocolAttribute.Format.toString(), NTGFieldFormat.S.toString(), NTGFieldFormat.S.toString(),
				JavaType.JAVA_LANG_STRING));

        protocolAttributes.put(NTGProtocolAttribute.Length.toString(), new AttributeStructure(
                NTGProtocolAttribute.Length.toString(), Integer.toString(length), length, JavaType.JAVA_LANG_INTEGER));

		IFieldStructure mockSimpleField =
				new FieldStructure(fldName, fldNamespace, fldDescr, null,
						protocolAttributes, null, JavaType.JAVA_LANG_DOUBLE, false, false, false, null);

		ByteBuffer bb = ByteBuffer.wrap(new byte[length]);
		bb.order( ByteOrder.LITTLE_ENDIAN);

		BigDecimal baseValue = new BigDecimal( value );
		BigDecimal baseScaled  = baseValue.setScale( 8, BigDecimal.ROUND_HALF_UP );
		BigDecimal multiplied = baseScaled.multiply( new BigDecimal(Math.pow(10, 8))) ;
		bb.putLong(multiplied.longValue());

		try
		{
			IFieldStructure fldStruct = new FieldStructure(fldName, fldNamespace,
					fldDescr, null, protocolAttributes, null, JavaType.JAVA_LANG_DOUBLE,
					true, false, false, value.toString()) ;

            NTGVisitorEncode visitor = new NTGVisitorEncode();
            visitor.visit(mockSimpleField.getName(), value, fldStruct, false);
            IoBuffer ioBuffer = visitor.getBuffer();

			for( int i = 0 ; i < bb.array().length; i++ )
			{
				logger.trace("Symbol at index [{}]. Expected [{}], actual [{}].", i,
						bb.array()[i], ioBuffer.array()[i]);

                Assert.assertEquals(String.format("NTGVisitor.visit(Double) failed. " +
						"Error at index [%d]. Expected [%x], actual [%x].", i,
						bb.array()[i], ioBuffer.array()[i]),
						bb.array()[i], ioBuffer.array()[i]);
			}
			IoBuffer copy = ioBuffer.duplicate();
			copy.order(ByteOrder.LITTLE_ENDIAN);
			copy.flip();
			Double restored = (copy.getLong())/Math.pow(10, 8);

			Assert.assertEquals( value, restored );

            logger.info("Visitor: method visit(Double)  PASSED.");
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			logger.error(ex.getMessage(),ex);
			Assert.fail(ex.getMessage());
		}
	}

    /**
	 * Test case Float data type
	 */
    @Test
    public void testNTGVisitorEncode_FloatField()
	{
		Float value = (float) (Integer.MAX_VALUE/10000);
		int offset = 0 ;
		int length = 4;
		String fldName = "FieldFloat";
        String fldNamespace = "NTG";
		String fldDescr = "Description " + fldName;

		Map<String, IAttributeStructure> protocolAttributes = new HashMap<>();

        protocolAttributes.put(NTGProtocolAttribute.Offset.toString(), new AttributeStructure(
                NTGProtocolAttribute.Offset.toString(), Integer.toString(offset), offset, JavaType.JAVA_LANG_INTEGER));

        protocolAttributes.put(NTGProtocolAttribute.Format.toString(), new AttributeStructure(
                NTGProtocolAttribute.Format.toString(), NTGFieldFormat.S.toString(), NTGFieldFormat.S.toString(),
				JavaType.JAVA_LANG_STRING));

        protocolAttributes.put(NTGProtocolAttribute.Length.toString(), new AttributeStructure(
                NTGProtocolAttribute.Length.toString(), Integer.toString(length), length, JavaType.JAVA_LANG_INTEGER));

		IFieldStructure mockSimpleField =
				new FieldStructure(fldName, fldNamespace, fldDescr, null,
						protocolAttributes, null, JavaType.JAVA_LANG_FLOAT, false, false, false, null);

		ByteBuffer bb = ByteBuffer.wrap(new byte[length]);
		bb.order( ByteOrder.LITTLE_ENDIAN);


		BigDecimal baseValue = new BigDecimal( value );
		BigDecimal baseScaled  = baseValue.setScale( 4, BigDecimal.ROUND_HALF_UP );
		BigDecimal multiplied = baseScaled.multiply( new BigDecimal(10000.0f)) ;
		bb.putInt(multiplied.intValue());


		try
		{
			IFieldStructure fldStruct = new FieldStructure(fldName, fldNamespace,
					fldDescr, null, protocolAttributes, null, JavaType.JAVA_LANG_FLOAT,
					true, false, false, value.toString()) ;

            NTGVisitorEncode visitor = new NTGVisitorEncode();
            visitor.visit(mockSimpleField.getName(), value, fldStruct, false);
            IoBuffer ioBuffer = visitor.getBuffer();

			for( int i = 0 ; i < bb.array().length; i++ )
			{
				logger.trace("Symbol at index [{}]. Expected [{}], actual [{}].", i,
						bb.array()[i], ioBuffer.array()[i]);

                Assert.assertEquals(String.format("NTGVisitor.visit(Float) failed. " +
						"Error at index [%d]. Expected [%x], actual [%x].", i,
						bb.array()[i], ioBuffer.array()[i]),
						bb.array()[i], ioBuffer.array()[i]);
			}

			IoBuffer copy = ioBuffer.duplicate();
			copy.order(ByteOrder.LITTLE_ENDIAN);
			copy.flip();
			Float restored = ((float) copy.getInt() ) / 10000;

			Assert.assertEquals( value, restored );

            logger.info("Visitor: method visit(Float)  PASSED.");
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			logger.error(ex.getMessage(),ex);
			Assert.fail(ex.getMessage());
		}
	}
}
