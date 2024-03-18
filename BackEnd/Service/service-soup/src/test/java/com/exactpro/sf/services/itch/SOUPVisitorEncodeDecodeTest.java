/*******************************************************************************
 * Copyright 2009-2024 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.sf.services.itch;

import java.math.BigDecimal;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exactpro.sf.common.messages.structures.IMessageStructure;
import org.apache.mina.core.buffer.IoBuffer;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.impl.AttributeStructure;
import com.exactpro.sf.common.util.EPSCommonException;

public class SOUPVisitorEncodeDecodeTest {

    private IoBuffer buffer;
    private IFieldStructure fldStructure;
    private SOUPVisitorEncode soupVisitorEncode;
    private SOUPVisitorSettings visitorSettings;

    public SOUPVisitorEncodeDecodeTest() {
        this.buffer = IoBuffer.allocate(4);
        this.fldStructure = Mockito.mock(IFieldStructure.class);
        Map<String, IAttributeStructure> map = new HashMap<>();
        map.put("Type", new AttributeStructure("Type", "Price4", "Price4", JavaType.JAVA_LANG_STRING));
        map.put("Length", new AttributeStructure("Length", "4", 4, JavaType.JAVA_LANG_INTEGER));
        Mockito.when(fldStructure.getAttributes()).thenReturn(map);
        this.soupVisitorEncode = new SOUPVisitorEncode(buffer, ByteOrder.BIG_ENDIAN);
        this.visitorSettings = new SOUPVisitorSettings() {{
            setTrimLeftPaddingEnabled(true);
        }};
    }

    @Test
    public void testVisitEncodeDecodeCollectionString() {

        List<String> testList = new ArrayList<>();
        testList.add("1111");
        testList.add("2222");
        testList.add("3333");
        testList.add("4444");

        this.buffer = IoBuffer.allocate(64);
        this.fldStructure = Mockito.mock(IFieldStructure.class);
        Map<String, IAttributeStructure> map = new HashMap<>();
        map.put("Type", new AttributeStructure("Type", "Alpha", "Alpha", JavaType.JAVA_LANG_STRING));
        map.put("SizeField", new AttributeStructure("SizeField", "NumberOfLevelItems", "NumberOfLevelItems", JavaType.JAVA_LANG_STRING));
        map.put("Length", new AttributeStructure("Length", "16", 16, JavaType.JAVA_LANG_INTEGER));
        Mockito.when(fldStructure.getAttributes()).thenReturn(map);
        Mockito.when(fldStructure.getName()).thenReturn("ArrayOfItems");
        Mockito.when(fldStructure.isCollection()).thenReturn(true);
        this.soupVisitorEncode = new SOUPVisitorEncode(buffer, ByteOrder.BIG_ENDIAN);

        IMessageStructure msgStructure = Mockito.mock(IMessageStructure.class);
        Map<String, IFieldStructure> mapFields = new HashMap<>();
        mapFields.put("ArrayOfItems", fldStructure);
        Mockito.when(msgStructure.getFields()).thenReturn(mapFields);

        IMessageFactory msgFactory = DefaultMessageFactory.getFactory();
        IMessage msg = msgFactory.createMessage("MarketByPrice", "SOUP");
        SOUPVisitorDecode soupVisitorDecode = new SOUPVisitorDecode(buffer, ByteOrder.BIG_ENDIAN, msg, msgFactory, visitorSettings);

        buffer.position(0);
        soupVisitorEncode.visitStringCollection("ArrayOfItems", testList, fldStructure, false);
        buffer.flip();
        msg.addField("NumberOfLevelItems",4);
        soupVisitorDecode.visitStringCollection("ArrayOfItems", null, fldStructure, false);

        List<String> actualList  = msg.getField("ArrayOfItems");
        Assert.assertEquals( testList, actualList);
    }

    @Test
    public void testVisitEncodeDecodePrice4() {

        BigDecimal[] correctlyTestValues = new BigDecimal[] {
                new BigDecimal(777L),
                new BigDecimal("123.45"),
                new BigDecimal("0"),
                new BigDecimal("11.1111")
        };

        IMessageFactory msgFactory = DefaultMessageFactory.getFactory();
        IMessage msg = msgFactory.createMessage("AddOrder", "SOUP");
        SOUPVisitorDecode soupVisitorDecode = new SOUPVisitorDecode(buffer, ByteOrder.BIG_ENDIAN, msg, msgFactory, visitorSettings);

        //Testing correctly values
        for (BigDecimal testValue : correctlyTestValues) {
            buffer.position(0);
            soupVisitorEncode.visit("Price", testValue, fldStructure, false);
            buffer.position(0);
            soupVisitorDecode.visit("Price", /*stub*/BigDecimal.ZERO, fldStructure, false);
            BigDecimal actualValue = msg.getField("Price");
            Assert.assertTrue("Expected: '" + testValue + "'; Actual: '" + actualValue + "'",
                    testValue.compareTo(actualValue) == 0);
        }
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    //Testing negative value
    @Test
    public void testThrowExceptionForNegativeValue() {
        BigDecimal negativeTestValue = new BigDecimal(-1);
        exceptionRule.expect(EPSCommonException.class);
        exceptionRule.expectMessage("cannot be negative");
        buffer.position(0);
        soupVisitorEncode.visit("Price", negativeTestValue, fldStructure, false);
    }

    //Testing exceed value
    @Test
    public void testThrowExceptionForExceedValue() {
        BigDecimal exceedTestValue = new BigDecimal("109951162.7775"); // (2^40 - 1) / 10^4
        exceptionRule.expect(EPSCommonException.class);
        exceptionRule.expectMessage("does not fit into 4 bytes");
        buffer.position(0);
        soupVisitorEncode.visit("Price", exceedTestValue, fldStructure, false);
    }

    //Testing value with fractional part
    @Test
    public void testThrowExceptionForFractionalValue() {
        BigDecimal fractionalTestValue = new BigDecimal("11.11111");
        exceptionRule.expect(EPSCommonException.class);
        exceptionRule.expectMessage("has more than 4 decimal places");
        buffer.position(0);
        soupVisitorEncode.visit("Price", fractionalTestValue, fldStructure, false);
    }
}