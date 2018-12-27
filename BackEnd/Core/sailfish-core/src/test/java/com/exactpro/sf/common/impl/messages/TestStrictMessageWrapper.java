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
package com.exactpro.sf.common.impl.messages;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.comparison.conversion.ConversionException;

/**
 * @author oleg.smirnov
 *
 */
public class TestStrictMessageWrapper {
    private static IDictionaryStructure dictionary;
    private static String dictionaryName = "/messages/strictMessage.xml";
    private String namespace = "TestStrictmessage";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @BeforeClass
    public static void init() throws IOException {
        IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();
        try (InputStream input = TestStrictMessageWrapper.class.getResourceAsStream(dictionaryName)) {
            dictionary = loader.load(input);
        }
    }

    @Test
    public void testPositive() {
        String msgName = "TestMessage";
        IMessageStructure msgStr = dictionary.getMessageStructure(msgName);
        IMessage strictMsg = new StrictMessageWrapper(msgStr);

        // add normal fields
        strictMsg.addField("boolean", true);
        strictMsg.addField("boolean_collection", Arrays.asList(true, false));
        strictMsg.addField("integer", 10);
        strictMsg.addField("integer_collection", Arrays.asList(-1, 0, 1));
        strictMsg.addField("string", "hello");
        strictMsg.addField("string_collection", Arrays.asList("abc", "def"));
        strictMsg.addField("short", 10);
        strictMsg.addField("short_collection", Arrays.asList(20, 30, 40));
        strictMsg.addField("long", 50l);
        strictMsg.addField("long_collection", Arrays.asList(60l, 70l, 80l));
        strictMsg.addField("byte", 1);
        strictMsg.addField("byte_collection", Arrays.asList(3, 5, 7));
        strictMsg.addField("float", 1.1f);
        strictMsg.addField("float_collection", Arrays.asList(3.3f, 5.5f, 7.7f));
        strictMsg.addField("double", 10.1d);
        strictMsg.addField("double_collection", Arrays.asList(30.3d, 50.5d, 70.7d));
        strictMsg.addField("character", 'a');
        strictMsg.addField("character_collection", Arrays.asList('b', 'c', 'd'));
        strictMsg.addField("bigDecimal", new BigDecimal("100.7"));
        strictMsg.addField("bigDecimal_collection",
                Arrays.asList(new BigDecimal("10.5"), new BigDecimal("20.5")));
        strictMsg.addField("datetime", LocalDateTime.now());
        strictMsg.addField("datetime_collection", Arrays
                .asList(LocalDateTime.now().plusWeeks(1), LocalDateTime.now().plusWeeks(2)));
        strictMsg.addField("date", LocalDate.now());
        strictMsg.addField("date_collection",
                Arrays.asList(LocalDate.now().plusWeeks(1), LocalDate.now().plusWeeks(2)));
        strictMsg.addField("time", LocalTime.now());
        strictMsg.addField("time_collection",
                Arrays.asList(LocalTime.now().plusHours(1), LocalTime.now().plusHours(2)));
        strictMsg.addField("message", wrappedTestMsg());
        strictMsg.addField("message_collection", Arrays.asList(wrappedTestMsg(), wrappedTestMsg()));

        // add collection in none-collection field
        strictMsg.addField("integer", Arrays.asList(99));

        // add single element in collection
        strictMsg.addField("double_collection", 10.1d);

        System.out.println(strictMsg.toString());
    }

    @Test
    public void testWrongFieldType() {
        expectedException.expect(ConversionException.class);
        expectedException.expectMessage("Cannot convert from");
        String msgName = "TestMessage";
        IMessageStructure msgStr = dictionary.getMessageStructure(msgName);
        IMessage strictMsg = new StrictMessageWrapper(msgStr);
        strictMsg.addField("byte", "some number");
    }

    @Test
    public void testNotWrappedIMessage() {
        expectedException.expect(ClassCastException.class);
        expectedException.expectMessage(
                "com.exactpro.sf.common.impl.messages.MapMessage is not instance or subclass of "
                        + "com.exactpro.sf.common.impl.messages.StrictMessageWrapper");
        String msgName = "TestMessage";
        IMessageStructure msgStr = dictionary.getMessageStructure(msgName);
        IMessage strictMsg = new StrictMessageWrapper(msgStr);
        strictMsg.addField("message", iMessage("SomeMessage"));
    }

    @Test
    public void testRequiedFields() {
        expectedException.expect(EPSCommonException.class);
        expectedException.expectMessage("Requied field [integer] must have NOT NULL value or default value");
        String msgName = "TestRequedMessage";
        IMessageStructure msgStr = dictionary.getMessageStructure(msgName);
        IMessage strictMsg = new StrictMessageWrapper(msgStr);
        strictMsg.addField("string", null);
        strictMsg.addField("integer", null);
    }

    @Test
    public void testNegativeCollectionInNoneCollectionField() {
        expectedException.expect(EPSCommonException.class);
        expectedException.expectMessage("Can't extract single value from collection cause it have more than 1 element");
        String msgName = "TestMessage";
        IMessageStructure msgStr = dictionary.getMessageStructure(msgName);
        IMessage strictMsg = new StrictMessageWrapper(msgStr);
        strictMsg.addField("integer", Arrays.asList(1, 2));
    }

    @Test
    public void testEmptyListInNoneCollectionRequiredField() {
        expectedException.expect(EPSCommonException.class);
        expectedException.expectMessage("Requied field [integer] must have NOT NULL value or default value");
        String msgName = "TestRequedMessage";
        IMessageStructure msgStr = dictionary.getMessageStructure(msgName);
        IMessage strictMsg = new StrictMessageWrapper(msgStr);
        strictMsg.addField("integer", new ArrayList<Integer>());
    }
    
    @Test
    public void testCheckedList() {
        expectedException.expect(ClassCastException.class);
        expectedException.expectMessage("Attempt to insert class "
                + "com.exactpro.sf.common.impl.messages.MapMessage element into collection with "
                + "element type class com.exactpro.sf.common.impl.messages.StrictMessageWrapper");
        String msgName = "TestMessage";
        IMessageStructure msgStr = dictionary.getMessageStructure(msgName);
        IMessage strictMsg = new StrictMessageWrapper(msgStr);
        strictMsg.addField("message_collection", Arrays.asList(new IMessage[] { wrappedTestMsg(), wrappedTestMsg() }));
        List<IMessage> testList = strictMsg.getField("message_collection");
        testList.add(iMessage("SomeMessage"));
    }

    private IMessage wrappedTestMsg() {
        String refMsgName = "SomeMessage";
        IMessageStructure str = dictionary.getMessageStructure(refMsgName);
        IMessage msg = new StrictMessageWrapper(str);
        msg.addField("testField", 101);
        return msg;
    }

    private IMessage iMessage(String name) {
        return new MapMessage(namespace, name);
    }
}
