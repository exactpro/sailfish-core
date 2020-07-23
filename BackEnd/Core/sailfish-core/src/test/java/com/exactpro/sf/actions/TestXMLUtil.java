/******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.sf.common.util.EPSCommonException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestXMLUtil {
    private XMLUtil xmlUtil;
    private static final String XML_EXAMPLE = "<A><B>value1</B><B>value2</B><C><D>Test!</D></C></A>";

    @Before
    public void clean() {
        this.xmlUtil = new XMLUtil();
    }

    @Test
    public void testGetValueByXPath() {
        String value = xmlUtil.getValueByXPath(XML_EXAMPLE, "A/C/D");
        Assert.assertEquals("Test!", value);
    }

    @Test(expected = EPSCommonException.class)
    public void testGetValueByXPathException() {
        String value = xmlUtil.getValueByXPath(XML_EXAMPLE, "A/C");
    }

    @Test
    public void testGetListByXPath() {
        List<String> list = xmlUtil.getListByXPath(XML_EXAMPLE, "A/B");
        Assert.assertEquals(Arrays.asList("value1", "value2"), list);
    }

    @Test(expected = EPSCommonException.class)
    public void testGetListByXPathException() {
        List<String> list = xmlUtil.getListByXPath(XML_EXAMPLE, "A/C");
    }

    @Test
    public void testGetObjectByXPath() {
        Map<String, ?> object = xmlUtil.getObjectByXPath(XML_EXAMPLE, "A");
        Map<String, Object> aObject = new HashMap<>();
        aObject.put("B", Arrays.asList("value1", "value2"));
        Map<String, Object> cObject = new HashMap<>();
        cObject.put("D", "Test!");
        aObject.put("C", cObject);
        Assert.assertEquals(aObject, object);
    }

    @Test(expected = EPSCommonException.class)
    public void testGetObjectByXPathException() {
        Map<String, ?> object = xmlUtil.getObjectByXPath(XML_EXAMPLE, "A/C/D");
    }

    @Test
    public void testSetValueByXPath() {
        String newValue = "It works!";
        String xmlDoc = xmlUtil.setValueByXPath(XML_EXAMPLE, "A/C/D", newValue);
        Assert.assertEquals(newValue, xmlUtil.getValueByXPath(xmlDoc, "A/C/D"));
    }

    @Test(expected = EPSCommonException.class)
    public void testSetValueByXPathException() {
        String value = xmlUtil.getValueByXPath(XML_EXAMPLE, "A/C");
    }
}