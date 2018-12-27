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
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.impl.AttributeStructure;
import com.exactpro.sf.common.messages.structures.impl.FieldStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.services.ntg.exceptions.NullFieldValue;
import com.exactpro.sf.util.AbstractTest;

public final class TestNTGVisitorNegative extends AbstractTest {
    private static final Logger logger = LoggerFactory.getLogger(TestNTGVisitorNegative.class);

	@BeforeClass
	public static void setUpClass(){
        logger.info("Start negative tests of NTGVisitor");
	}

	@AfterClass
	public static void tearDownClass(){
        logger.info("Finish negative tests of NTGVisitor");
	}

	/**
     * Negative test NTGVisitorEncode's method visit(..) with integer type of value and invalid length
	 * @throws Exception
	 */
	@Test
    public void testNTGVisitorEncodeInvalidIntegerField() {
		Integer value = Integer.MAX_VALUE;
		int offset = 0 ;
		int length = 40;
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
		try {
			IFieldStructure fldStruct = new FieldStructure(fldName, fldNamespace,
					fldDescr, null, protocolAttributes, null, JavaType.JAVA_LANG_INTEGER,
					true, false, false, value.toString()) ;
            NTGVisitorEncode ntgVisitor = new NTGVisitorEncode();
            ntgVisitor.visit(fldName, value, fldStruct, false);
			Assert.fail("There is no exception was threw");
		}
		catch(EPSCommonException e){
			Assert.assertEquals("Unsupported length ["+length+"] for field [FieldInteger].", e.getMessage());
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			logger.error(ex.getMessage(),ex);
			Assert.fail(ex.getMessage());
		}
	}

	/**
     * Negative test NTGVisitorDecode's method visit(..) with integer type of value and invalid length
	 * @throws Exception
	 */

	@Test
    public void testNTGVisitorDecodeInvalidIntegerField()
	{
		Integer value = Integer.MAX_VALUE;
		int offset = 0 ;
		int length = 40;
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
                NTGProtocolAttribute.Length.toString(), Integer.toString(4), 4, JavaType.JAVA_LANG_INTEGER));

		try{
			IFieldStructure fldStruct = new FieldStructure(fldName, fldNamespace,
					fldDescr, null, protocolAttributes, null, JavaType.JAVA_LANG_INTEGER,
					true, false, false, value.toString()) ;

            NTGVisitorEncode ntgVisitor = new NTGVisitorEncode();
            ntgVisitor.visit(fldName, value, fldStruct, false);
            IoBuffer ioBuffer = ntgVisitor.getBuffer();

            protocolAttributes.put(NTGProtocolAttribute.Length.toString(), new AttributeStructure(
                    NTGProtocolAttribute.Length.toString(), Integer.toString(length), length, JavaType.JAVA_LANG_INTEGER));

			fldStruct = new FieldStructure(fldName, fldNamespace,fldDescr, null, protocolAttributes,
					null, JavaType.JAVA_LANG_INTEGER, true, false, false, value.toString()) ;
            NTGVisitorDecode ntgVisitorDecode = new NTGVisitorDecode(ioBuffer,
					DefaultMessageFactory.getFactory(), null);
            ntgVisitorDecode.visit(fldName, value, fldStruct, false);
			Assert.fail("There is no exception was threw");
		}
		catch(EPSCommonException e){
			Assert.assertEquals("Unsupported length ["+length+"] for field [FieldInteger].", e.getMessage());
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			logger.error(ex.getMessage(),ex);
			Assert.fail(ex.getMessage());
		}
	}

	/**
     * Negative test NTGVisitorEncode's method visit(..) with BigDecimal type of value and
	 * invalid attribute type. Available attribute type: Uint64, Price
	 * @throws Exception
	 */


	@Test
    public void testNTGVisitorEncodeInvalidBigDecimalField()
	{
		BigDecimal value = new BigDecimal(10);
		int offset = 0 ;
		int length = 8;
		String fldName = "FieldInteger";
        String fldNamespace = "NTG";
		String fldDescr = "Description " + fldName;
		String type="Invalid";

		Map<String, IAttributeStructure> protocolAttributes = new HashMap<>();

        protocolAttributes.put(NTGProtocolAttribute.Offset.toString(), new AttributeStructure(
                NTGProtocolAttribute.Offset.toString(), Integer.toString(offset), offset, JavaType.JAVA_LANG_INTEGER));

        protocolAttributes.put(NTGProtocolAttribute.Format.toString(), new AttributeStructure(
                NTGProtocolAttribute.Format.toString(), NTGFieldFormat.S.toString(), NTGFieldFormat.S.toString(),
				JavaType.JAVA_LANG_STRING));

        protocolAttributes.put(NTGProtocolAttribute.Length.toString(), new AttributeStructure(
                NTGProtocolAttribute.Length.toString(), Integer.toString(length), length, JavaType.JAVA_LANG_INTEGER));

        protocolAttributes.put(NTGProtocolAttribute.Type.toString(), new AttributeStructure(
                NTGProtocolAttribute.Type.toString(), type, type, JavaType.JAVA_LANG_STRING));

		try{
			IFieldStructure fldStruct = new FieldStructure(fldName, fldNamespace,
					fldDescr, null, protocolAttributes, null, JavaType.JAVA_MATH_BIG_DECIMAL,
					true, false, false, value.toString()) ;
            NTGVisitorEncode ntgVisitorEncode = new NTGVisitorEncode();
            ntgVisitorEncode.visit(fldName, value, fldStruct, false);
			Assert.fail("There is no exception was threw");
		}
		catch(EPSCommonException e){
			Assert.assertEquals("Unknown protocol atribute Type: "+type, e.getMessage());
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			logger.error(ex.getMessage(),ex);
			Assert.fail(ex.getMessage());
		}
	}

	/**
     * Negative test NTGVisitorDecode's method visit(..) with BigDecimal type of value and
	 * invalid attribute type. Available attribute type: Uint64, Price
	 * @throws Exception
	 */
	@Test
    public void testNTGVisitorDecodeInvalidBigDecimalField()
	{
		BigDecimal value = new BigDecimal(10);
		int offset = 0 ;
		int length = 8;
		String fldName = "FieldInteger";
        String fldNamespace = "NTG";
		String fldDescr = "Description " + fldName;
		String type="Invalid";

		Map<String, IAttributeStructure> protocolAttributes = new HashMap<>();

        protocolAttributes.put(NTGProtocolAttribute.Offset.toString(), new AttributeStructure(
                NTGProtocolAttribute.Offset.toString(), Integer.toString(offset), offset, JavaType.JAVA_LANG_INTEGER));

        protocolAttributes.put(NTGProtocolAttribute.Format.toString(), new AttributeStructure(
                NTGProtocolAttribute.Format.toString(), NTGFieldFormat.S.toString(), NTGFieldFormat.S.toString(),
				JavaType.JAVA_LANG_STRING));

        protocolAttributes.put(NTGProtocolAttribute.Length.toString(), new AttributeStructure(
                NTGProtocolAttribute.Length.toString(), Integer.toString(length), length, JavaType.JAVA_LANG_INTEGER));

        protocolAttributes.put(NTGProtocolAttribute.Type.toString(), new AttributeStructure(
                NTGProtocolAttribute.Type.toString(), "Price", "Price", JavaType.JAVA_LANG_STRING));

		try {
			IFieldStructure fldStruct = new FieldStructure(fldName, fldNamespace,
					fldDescr, null, protocolAttributes, null, JavaType.JAVA_MATH_BIG_DECIMAL,
					true, false, false, value.toString()) ;
            NTGVisitorEncode ntgVisitorEncode = new NTGVisitorEncode();
            ntgVisitorEncode.visit(fldName, value, fldStruct, false);
            IoBuffer ioBuffer = ntgVisitorEncode.getBuffer();

            protocolAttributes.put(NTGProtocolAttribute.Type.toString(), new AttributeStructure(
                    NTGProtocolAttribute.Type.toString(), type, type, JavaType.JAVA_LANG_STRING));

			fldStruct = new FieldStructure(fldName, fldNamespace,fldDescr, null, protocolAttributes,
					null, JavaType.JAVA_MATH_BIG_DECIMAL, true, false, false, value.toString()) ;
            NTGVisitorDecode ntgVisitorDecode = new NTGVisitorDecode(ioBuffer,
					DefaultMessageFactory.getFactory(), null);
            ntgVisitorDecode.visit(fldName, value, fldStruct, false);
			Assert.fail("There is no exception was threw");
		}
		catch(EPSCommonException e){
			Assert.assertEquals("Unknown protocol atribute Type: "+type, e.getMessage());
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			logger.error(ex.getMessage(),ex);
			Assert.fail(ex.getMessage());
		}
	}

	/**
     * Test all visit(...) method in NTGVisitorEncode with null value. There is test of following type:
	 * Integer, IMessage, Double, Float, Long, Byte, BigDecimal.
	 * @throws Exception
	 */

	@Test
	public void testNullValueInVisitMethod(){
		try{
			String fieldName="test";
			Map<String, IAttributeStructure> protocolAttributes = new HashMap<>();
			IFieldStructure mockFieldStruct = new FieldStructure("test", "test",
					"test", null, protocolAttributes, null, JavaType.JAVA_LANG_INTEGER,
					true, false, false, "0") ;
            NTGVisitorEncode ntgVisitorEncode = new NTGVisitorEncode();
			try{
				IMessage message=null;
                ntgVisitorEncode.visit(fieldName, message, mockFieldStruct, false);
				Assert.fail("There is no exception was threw");
			}catch(NullPointerException e){
				Assert.assertEquals("Message is null. Field name = "+fieldName, e.getMessage());
			}

			try{
				Double value=null;
                ntgVisitorEncode.visit(fieldName, value, mockFieldStruct, false);
				Assert.fail("There is no exception was threw");
			}catch(NullFieldValue e){
				Assert.assertEquals("Field name = ["+fieldName+"] has null value", e.getMessage());
			}

			try{
				Float value=null;
                ntgVisitorEncode.visit(fieldName, value, mockFieldStruct, false);
				Assert.fail("There is no exception was threw");
			}catch(NullFieldValue e){
				Assert.assertEquals("Field name = ["+fieldName+"] has null value", e.getMessage());
			}

			try{
				Long value=null;
                ntgVisitorEncode.visit(fieldName, value, mockFieldStruct, false);
				Assert.fail("There is no exception was threw");
			}catch(NullFieldValue e){
				Assert.assertEquals("Field name = ["+fieldName+"] has null value", e.getMessage());
			}

			try{
				Integer value=null;
                ntgVisitorEncode.visit(fieldName, value, mockFieldStruct, false);
				Assert.fail("There is no exception was threw");
			}catch(NullFieldValue e){
				Assert.assertEquals("Field name = ["+fieldName+"] has null value", e.getMessage());
			}

			try{
				Byte value=null;
                ntgVisitorEncode.visit(fieldName, value, mockFieldStruct, false);
				Assert.fail("There is no exception was threw");
			}catch(NullFieldValue e){
				Assert.assertEquals("Field name = ["+fieldName+"] has null value", e.getMessage());
			}

			try{
				BigDecimal value=null;
                ntgVisitorEncode.visit(fieldName, value, mockFieldStruct, false);
				Assert.fail("There is no exception was threw");
			}catch(NullFieldValue e){
				Assert.assertEquals("Field name = ["+fieldName+"] has null value", e.getMessage());
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			logger.error(ex.getMessage(),ex);
			Assert.fail(ex.getMessage());
		}
	}
}
