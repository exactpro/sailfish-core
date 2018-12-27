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
package com.exactpro.sf.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.time.LocalDateTime;

import com.exactpro.sf.common.impl.messages.AbstractMessageFactory;
import com.exactpro.sf.common.messages.CreateIMessageVisitor;
import com.exactpro.sf.common.messages.IHumanMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MessageStructureWriter;
import com.exactpro.sf.common.messages.MessageUtil;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.comparison.ComparisonUtil;
import com.exactpro.sf.comparison.MessageComparator;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.storage.util.JsonMessageConverter;
import com.fasterxml.jackson.core.JsonParseException;
import com.google.common.collect.Lists;

public class JsonMessageConverterTest extends AbstractTest {

    private IDictionaryManager manager;
    private SailfishURI dictionaryURI;
    private IDictionaryStructure dictionary;
    private IMessageStructure messageStructure;
    private AbstractMessageFactory messageFactory;

    @Before
    public void init() {
        manager = SFLocalContext.getDefault().getDictionaryManager();
        dictionaryURI = SailfishURI.unsafeParse("Example");
        dictionary = manager.getDictionary(dictionaryURI);
        messageStructure = dictionary.getMessageStructure("ComplexMessage");
        messageFactory = new AbstractMessageFactory() {

            @Override
            public IMessage createMessage(String name, String namespace) {
                IMessage message = super.createMessage(name, namespace);
                message.getMetaData().setDictionaryURI(dictionaryURI);
                return message;
            }

            @Override
            public String getProtocol() {
                return "TEST";
            }
        };
    }

    @Test
    public void testFullFormat() throws JsonParseException, IOException, SailfishURIException {
        IMessage message = generate();

        String json = JsonMessageConverter.toJson(message, false);
        IMessage actual = JsonMessageConverter.fromJson(json, false);
        compare(message, actual, 43, 0, 0);

        Assert.assertNull(actual.getMetaData().getRejectReason());
        Assert.assertFalse(actual.getMetaData().isRejected());

        json = JsonMessageConverter.toJson(message, dictionary, false);
        actual = JsonMessageConverter.fromJson(json, manager, false);
        compare(message, actual, 43, 0, 0);

        Assert.assertNull(actual.getMetaData().getRejectReason());
        Assert.assertFalse(actual.getMetaData().isRejected());
    }

    @Test
    public void testFullFormatRejected() throws JsonParseException, IOException, SailfishURIException {
        IMessage message = generate();

        message.getMetaData().setRejectReason("Test reject");

        String json = JsonMessageConverter.toJson(message, false);
        IMessage actual = JsonMessageConverter.fromJson(json, false);
        compare(message, actual, 43, 0, 0);

        Assert.assertEquals("Test reject", actual.getMetaData().getRejectReason());
        Assert.assertTrue(actual.getMetaData().isRejected());

        json = JsonMessageConverter.toJson(message, dictionary, false);
        actual = JsonMessageConverter.fromJson(json, manager, false);
        compare(message, actual, 43, 0, 0);

        Assert.assertEquals("Test reject", actual.getMetaData().getRejectReason());
        Assert.assertTrue(actual.getMetaData().isRejected());
    }

    @Test
    public void testFilter() throws JsonParseException, IOException, SailfishURIException {
        IMessage message = messageFactory.createMessage("name", "namespace");
        message.addField("NotNullFilter", "*");
        message.addField("NullFilter", "#");
        message.addField("SimpleFilter", "123");
        message.addField("KnownBugFilter", "Expected: 1 (Integer), Bugs:]");
        message.addField("RegexFilter", "\"123\"");
        message.addField("Filter", "x == 321 && x > 123");

        String json = "{\"id\":0,\"timestamp\":1512736145191,\"name\":\"name\",\"namespace\":\"namespace\",\"dictionaryURI\":\"Example\",\"protocol\":\"TEST\","
                + "\"message\":{\"KnownBugFilter\":{\"type\":\"KnownBugFilter\",\"value\":\"Expected: 1 (Integer), Bugs:]\"},"
                + "\"Filter\":{\"type\":\"MvelFilter\",\"value\":\"x == 321 && x > 123\"},"
                + "\"SimpleFilter\":{\"type\":\"SimpleMvelFilter\",\"value\":123},"
                + "\"NotNullFilter\":{\"type\":\"NotNullFilter\",\"value\":\"*\"},"
                + "\"NullFilter\":{\"type\":\"NullFilter\",\"value\":\"#\"},"
                + "\"RegexFilter\":{\"type\":\"RegexMvelFilter\",\"value\":\"\\\"123\\\"\"}}}";
        IMessage actual = JsonMessageConverter.fromJson(json, false);
        compare(message, actual, 5, 1, 0);
    }

    @Test
    public void testNull() throws JsonParseException, IOException, SailfishURIException {
        IMessage message = messageFactory.createMessage("name", "namespace");
        message.addField("Field", null);

        String json = "{\"id\":0,\"timestamp\":1512736145191,\"name\":\"name\",\"namespace\":\"namespace\",\"dictionaryURI\":\"Example\",\"protocol\":\"TEST\","
                + "\"message\":{\"Field\":{\"type\":null,\"value\":null}}}";
        IMessage actual = JsonMessageConverter.fromJson(json, false);
        compare(message, actual, 0, 0, 0);
    }

    @Test
    public void testCompactFormat() throws SailfishURIException {
        IMessage message = generate();

        String json = JsonMessageConverter.toJson(message);
        IMessage actual = JsonMessageConverter.fromJson(json);
        compare(message, actual, 11, 32, 0);

        json = JsonMessageConverter.toJson(message, dictionary, true);
        actual = JsonMessageConverter.fromJson(json, manager, true);
        compare(message, actual, 43, 0, 0);
    }

    @Test
    public void testFromJsonToHuman() {
        IMessage message = createDirtyMessage();

        String json = JsonMessageConverter.toJson(message);
        IHumanMessage actual = JsonMessageConverter.fromJsonToHuman(json, manager, true);
        Assert.assertEquals(
                "EmptyComplexCollection=[]; " +
                "SimpleMessage={" +
                    "FCharacter=c; FShort=0; " +
                    "FBoolean=true; FDouble=1.0; " +
                    "FStringEnum=Enum1(Foo); " +
                    "FCharacterEnum=LetterA(a); FDateTime=1970-01-01T00:00:00.000000333; " +
                    "FIntegerEnum=Hundred(2); FDate=1970-01-01; " +
                    "FTime=00:00:00.000000333; FFloat=2.0; " +
                    "FInteger=-1; FString=str; FTest=test; FLong=2}; " +
                "ArrayMessage={FCharacter=[0; 1]}; " +
                "EmptySimpleCollection=[]; "+
                "Trailer={}",
                actual.toString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHumanFormat() {
        Map<String, Object> map = new HashMap<>();
        map.put("Fake", 1L);
        
        IMessage message = createDirtyMessage();
        message.addField("FBoolean", new Object() { //Markers for special actions
            public String toString() {
                return "excluded field";
            };
        });
        message.addField("FakeMessage", map);
        message.addField("FakeMessages", Lists.newArrayList(map));

        IHumanMessage actual = MessageUtil.convertToIHumanMessage(messageFactory, messageStructure, message);
        Assert.assertEquals(
                "SimpleMessage={"
                + "FBoolean=true; "
                + "FCharacter=c; "
                + "FShort=0; "
                + "FInteger=-1; "
                + "FLong=2; "
                + "FFloat=2.0; "
                + "FDouble=1.0; "
                + "FString=str; "
                + "FDateTime=1970-01-01T00:00:00.000000333; "
                + "FTime=00:00:00.000000333; "
                + "FDate=1970-01-01; "
                + "FCharacterEnum=LetterA(a); "
                + "FIntegerEnum=Hundred(2); "
                + "FStringEnum=Enum1(Foo); "
                + "FTest=test}; "
                + "ArrayMessage={"
                + "FCharacter=[0; 1]}; "
                + "EmptyComplexCollection=[]; "
                + "EmptySimpleCollection=[]; "
                + "Trailer={}; "
                + "FakeMessage={Fake=1}; "
                + "FBoolean=excluded field; "
                + "FakeMessages=[{Fake=1}]",
                actual.toString());
    }

    private IMessage createDirtyMessage() {
        IMessage message = messageFactory.createMessage("ComplexMessage", "example");
    
        IMessage simple = messageFactory.createMessage("SimpleMessage", "example");
        simple.addField("FTest", "test");
        simple.addField("FCharacter", 'c');
        simple.addField("FShort", (short)0);
        simple.addField("FBoolean", true);
        simple.addField("FDouble", 1.0);
        simple.addField("FStringEnum", "Foo");
        simple.addField("FCharacterEnum", 'a');
        simple.addField("FIntegerEnum", 2);
        simple.addField("FFloat", 2.0);
        simple.addField("FInteger", -1);
        simple.addField("FString", "str");
        simple.addField("FLong", "2");
        LocalDateTime dateTime = DateTimeUtility.toLocalDateTime(1, 333);
        simple.addField("FDateTime", dateTime);
        simple.addField("FDate", DateTimeUtility.toLocalDate(dateTime));
        simple.addField("FTime", DateTimeUtility.toLocalTime(dateTime));
    
        message.addField("SimpleMessage", simple);
    
        IMessage array = messageFactory.createMessage("ArrayMessage", "example");
        array.addField("FCharacter", Arrays.asList('0', '1'));
        message.addField("ArrayMessage", array);
    
        message.addField("Trailer", messageFactory.createMessage("Trailer", "example"));
        message.addField("EmptySimpleCollection", new ArrayList<String>());
        message.addField("EmptyComplexCollection", new ArrayList<IMessage>());
        return message;
    }

    private void compare(IMessage expected, IMessage actual, int passed, int failed, int na) {
        ComparatorSettings settings = new ComparatorSettings();
        ComparisonResult comparisonResult = MessageComparator.compare(expected, actual, settings);
        System.out.println(comparisonResult);
        Assert.assertEquals(passed, ComparisonUtil.getResultCount(comparisonResult, StatusType.PASSED));
        Assert.assertEquals(failed, ComparisonUtil.getResultCount(comparisonResult, StatusType.FAILED));
        Assert.assertEquals(na, ComparisonUtil.getResultCount(comparisonResult, StatusType.NA));
    }

    private IMessage generate() throws SailfishURIException {

        MessageStructureWriter msw = new MessageStructureWriter();
        CreateIMessageVisitor visitor = new CreateIMessageVisitor(messageFactory, "ComplexMessage", messageStructure.getNamespace());
        msw.traverse(visitor, messageStructure.getFields());
        IMessage result = visitor.getImessage();
        result.addField("Trailer", messageFactory.createMessage("Trailer", "namespace"));
        result.addField("EmptySimpleCollection", new ArrayList<String>());
        result.addField("EmptyComplexCollection", new ArrayList<IMessage>());

        return result;
    }
}
