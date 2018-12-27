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
package com.exactpro.sf.aml.scriptutil;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.exactpro.sf.aml.scriptutil.MessageCount.Operation;

public class MessageCountTest {
    private Map<String, Map.Entry<String, Integer>> inputStrings = new HashMap<String, Map.Entry<String, Integer>>();

    @Before
    public void before() throws Exception {
        inputStrings.put("SingleValue",                     new HashMap.SimpleEntry<String, Integer>("150", 150));
        inputStrings.put("NotEqualsValue",                  new HashMap.SimpleEntry<String, Integer>("!= 150", 150));
        inputStrings.put("IntervalValue",                   new HashMap.SimpleEntry<String, Integer>("100 - 200", 150));
        inputStrings.put("IntervalValueLeftValue",          new HashMap.SimpleEntry<String, Integer>("100 - 200", 100));
        inputStrings.put("IntervalValueRightValue",         new HashMap.SimpleEntry<String, Integer>("100 - 200", 200));
        inputStrings.put("IntervalValueBracket",            new HashMap.SimpleEntry<String, Integer>("(100..200]", 150));
        inputStrings.put("IntervalValueBracketLeftValue",   new HashMap.SimpleEntry<String, Integer>("(100 .. 200]", 100));
        inputStrings.put("IntervalValueBracketRightValue",  new HashMap.SimpleEntry<String, Integer>("(100 .. 200]", 200));
        inputStrings.put("IncorrectValue1",                 new HashMap.SimpleEntry<String, Integer>(" - 100", null));
        inputStrings.put("IncorrectValue2",                 new HashMap.SimpleEntry<String, Integer>("any[1-3]any", null));
    }

    @Test
    public void testFromStringSingleValue() throws Exception {
        MessageCount messageCount = MessageCount.fromString(inputStrings.get("SingleValue").getKey());
        assertTrue(messageCount.checkInt(inputStrings.get("SingleValue").getValue()));
    }

    @Test
    public void testFromStringNotEqualsValue() throws Exception {
        MessageCount messageCount = MessageCount.fromString(inputStrings.get("NotEqualsValue").getKey());
        assertFalse(messageCount.checkInt(inputStrings.get("NotEqualsValue").getValue()));
        assertTrue(messageCount.checkInt(inputStrings.get("NotEqualsValue").getValue() + 1));
    }

    @Test
    public void testFromStringIntervalValue() throws Exception {
        MessageCount messageCount = MessageCount.fromString(inputStrings.get("IntervalValue").getKey());
        assertNotNull("MessageCount returned null, this is incorrect", messageCount);
        assertTrue(messageCount.checkInt(inputStrings.get("IntervalValue").getValue()));
        assertTrue(messageCount.checkInt(inputStrings.get("IntervalValueLeftValue").getValue()));
        assertTrue(messageCount.checkInt(inputStrings.get("IntervalValueRightValue").getValue()));
    }

    @Test
    public void testFromStringIntervalBracketValue() throws Exception {
        MessageCount messageCount = MessageCount.fromString(inputStrings.get("IntervalValueBracket").getKey());
        assertNotNull("MessageCount returned null, this is incorrect", messageCount);
        assertTrue(messageCount.checkInt(inputStrings.get("IntervalValueBracket").getValue()));
        assertFalse(messageCount.checkInt(inputStrings.get("IntervalValueBracketLeftValue").getValue()));
        assertTrue(messageCount.checkInt(inputStrings.get("IntervalValueBracketRightValue").getValue()));
    }

    @Test
    public void testFromStringIncorrectValue1() throws Exception {
        MessageCount messageCount = MessageCount.fromString(inputStrings.get("IncorrectValue1").getKey());
        assertNull(messageCount);
    }

    @Test
    public void testFromStringIncorrectValue2() throws Exception {
        MessageCount messageCount = MessageCount.fromString(inputStrings.get("IncorrectValue2").getKey());
        assertNull(messageCount);
    }
    
    @Test 
    public void testIsValidExpression() throws Exception {
        checkIsValidExpression("${ref.field}");
        checkIsValidExpression("${ref.field[1]}");
        checkIsValidExpression("${ref.field.field}");
        checkIsValidExpression("${ref.field[1].field}");
        
        for (Operation operation : MessageCount.Operation.values()) {
            checkIsValidExpression(operation.getValue() + " ${ref.field}");
        }
        
        checkIntervalExpression("${ref.field}",
                                "${ref.field} + 123", 
                                "${ref.field} - 123",
                                "${ref.field} * 123", 
                                "${ref.field} / 123",
                                "(${ref.field} + 123)",
                                "123");
    }
    
    @Test
    public void testFromStringIncorrectValue3() throws Exception {
        assertNull(MessageCount.fromString(">10>"));
        assertNull(MessageCount.fromString("<10>"));
        assertNull(MessageCount.fromString(" > 10 > "));
        assertNull(MessageCount.fromString("!== 10"));
        assertNull(MessageCount.fromString("!=!= 10"));
    }
    
    private void checkIsValidExpression(String expression) {
        assertTrue("Check is valid '" + expression + "'", MessageCount.isValidExpression(expression));
    }
    
    private void checkIntervalExpression(String ... values) {
        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values.length; j++) {
                checkIsValidExpression("(" + values[i] + " .. " + values[j] + ")");
                checkIsValidExpression("[" + values[i] + " .. " + values[j] + ")");
                checkIsValidExpression("(" + values[i] + " .. " + values[j] + "]");
                checkIsValidExpression("[" + values[i] + " .. " + values[j] + "]");
            }
        }
    }
}