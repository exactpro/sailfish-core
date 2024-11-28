/*
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
 */
package com.exactpro.sf.comparison;

import com.exactpro.sf.aml.script.ActionContext;
import com.exactpro.sf.aml.script.MetaContainer;
import com.exactpro.sf.aml.scriptutil.StaticUtil;
import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;
import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.impl.messages.MapMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.util.AbstractTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class TestFieldFilter extends AbstractTest {

    @Test
    public void testEquals() {
        IMessage message = messageFactory.createMessage("name", "namespace");
        IMessage sub = messageFactory.createMessage("sub", "namespace");
        sub.addField("ExplicitNullValue", null);
        sub.addField("AnyValue", 1);
        sub.addField("EmptyValueList", Collections.emptyList());
        sub.addField("ValueList", Arrays.asList(2, 3, 4));
        sub.addField("ValueListWithNull", Arrays.asList(5, null, 6));

        message.addField("ExplicitNullValue", null);
        message.addField("AnyValue", 1);
        message.addField("MessageValue", sub);
        message.addField("EmptyValueList", Collections.emptyList());
        message.addField("ValueList", Arrays.asList(2, 3, 4));
        message.addField("ValueListWithNull", Arrays.asList(5, null, 6));
        message.addField("MessageList", Arrays.asList(sub, sub));

        equals(message, message);
    }

    @Test
    public void testNull() {

        IMessage message = messageFactory.createMessage("name", "namespace");
        IMessage filter = messageFactory.createMessage("name", "namespace");

        IFilter nullFilter = StaticUtil.nullFilter(0, null);

        message.addField("ExplicitNull_NullFilter", null);
        message.addField("AnyValue_NullFilter", new Object());

        filter.addField("ExplicitNull_NullFilter", nullFilter);
        filter.addField("AnyValue_NullFilter", nullFilter);
        filter.addField("HiddenNull_NullFilter", nullFilter);

        ComparatorSettings compareSettings = new ComparatorSettings();

        ComparisonResult comparisonResult = MessageComparator.compare(message, filter, compareSettings);

        validateResult(comparisonResult, 2, 1, 0);
        assertEquals(StatusType.PASSED, comparisonResult.getResult("ExplicitNull_NullFilter").getStatus());
        assertEquals(StatusType.FAILED, comparisonResult.getResult("AnyValue_NullFilter").getStatus());
        assertEquals(StatusType.PASSED, comparisonResult.getResult("HiddenNull_NullFilter").getStatus());
    }

    @Test
    public void testMissed() {

        IMessage message = messageFactory.createMessage("name", "namespace");
        IMessage filter = messageFactory.createMessage("name", "namespace");

        IFilter missedFilter = StaticUtil.simpleFilter(0, null, "com.exactpro.sf.comparison.Convention.CONV_MISSED_OBJECT");

        message.addField("ExplicitNull_EmptyValue", null);
        message.addField("AnyValue_EmptyValue", new Object());

        filter.addField("ExplicitNull_EmptyValue", missedFilter);
        filter.addField("AnyValue_EmptyValue", missedFilter);
        filter.addField("HiddenNull_EmptyValue", missedFilter);

        ComparatorSettings compareSettings = new ComparatorSettings();

        ComparisonResult comparisonResult = MessageComparator.compare(message, filter, compareSettings);

        validateResult(comparisonResult, 2, 1, 0);
        assertEquals(StatusType.PASSED, comparisonResult.getResult("ExplicitNull_EmptyValue").getStatus());
        assertEquals(StatusType.FAILED, comparisonResult.getResult("AnyValue_EmptyValue").getStatus());
        assertEquals(StatusType.PASSED, comparisonResult.getResult("HiddenNull_EmptyValue").getStatus());
    }

    @Test
    public void testNotNull() {

        IMessage message = messageFactory.createMessage("name", "namespace");
        IMessage filter = messageFactory.createMessage("name", "namespace");

        IFilter notNullFilter = StaticUtil.notNullFilter(0, null);

        message.addField("ExplicitNull_NotNullFilter", null);
        message.addField("AnyValue_NotNullFilter", new Object());

        filter.addField("ExplicitNull_NotNullFilter", notNullFilter);
        filter.addField("AnyValue_NotNullFilter", notNullFilter);
        filter.addField("HiddenNull_NotNullFilter", notNullFilter);

        ComparatorSettings compareSettings = new ComparatorSettings();

        ComparisonResult comparisonResult = MessageComparator.compare(message, filter, compareSettings);

        validateResult(comparisonResult, 1, 2, 0);
        assertEquals(StatusType.FAILED, comparisonResult.getResult("ExplicitNull_NotNullFilter").getStatus());
        assertEquals(StatusType.PASSED, comparisonResult.getResult("AnyValue_NotNullFilter").getStatus());
        assertEquals(StatusType.FAILED, comparisonResult.getResult("HiddenNull_NotNullFilter").getStatus());
    }

    @Test
    public void testPresent() {

        IMessage message = messageFactory.createMessage("name", "namespace");
        IMessage filter = messageFactory.createMessage("name", "namespace");

        IFilter presentFilter = StaticUtil.simpleFilter(0, null, "com.exactpro.sf.comparison.Convention.CONV_PRESENT_OBJECT");

        message.addField("ExplicitNull_AnyValue", null);
        message.addField("AnyValue_AnyValue", new Object());

        filter.addField("ExplicitNull_AnyValue", presentFilter);
        filter.addField("AnyValue_AnyValue", presentFilter);
        filter.addField("HiddenNull_AnyValue", presentFilter);

        ComparatorSettings compareSettings = new ComparatorSettings();

        ComparisonResult comparisonResult = MessageComparator.compare(message, filter, compareSettings);

        validateResult(comparisonResult, 1, 2, 0);
        assertEquals(StatusType.FAILED, comparisonResult.getResult("ExplicitNull_AnyValue").getStatus());
        assertEquals(StatusType.PASSED, comparisonResult.getResult("AnyValue_AnyValue").getStatus());
        assertEquals(StatusType.FAILED, comparisonResult.getResult("HiddenNull_AnyValue").getStatus());
    }

    @Test
    public void testIsNullSet() {

        IMessage message = messageFactory.createMessage("name", "namespace");
        IMessage filter = messageFactory.createMessage("name", "namespace");

        IFilter presentFilter = StaticUtil.existenceFilter(0, null);

        message.addField("ExplicitNull_ExistenceFilter", null);
        message.addField("AnyValue_ExistenceFilter", new Object());

        filter.addField("ExplicitNull_ExistenceFilter", presentFilter);
        filter.addField("AnyValue_ExistenceFilter", presentFilter);
        filter.addField("HiddenNull_ExistenceFilter", presentFilter);

        ComparatorSettings compareSettings = new ComparatorSettings();

        ComparisonResult comparisonResult = MessageComparator.compare(message, filter, compareSettings);

        validateResult(comparisonResult, 2, 1, 0);
        assertEquals(StatusType.PASSED, comparisonResult.getResult("ExplicitNull_ExistenceFilter").getStatus());
        assertEquals(StatusType.PASSED, comparisonResult.getResult("AnyValue_ExistenceFilter").getStatus());
        assertEquals(StatusType.FAILED, comparisonResult.getResult("HiddenNull_ExistenceFilter").getStatus());
    }

    @Test
    public void testExistence() {

        IMessage message = messageFactory.createMessage("name", "namespace");
        IMessage filter = messageFactory.createMessage("name", "namespace");

        IFilter presentFilter = StaticUtil.simpleFilter(0, null, "com.exactpro.sf.comparison.Convention.CONV_EXISTENCE_OBJECT");

        message.addField("ExplicitNull_ExistenceFilter", null);
        message.addField("AnyValue_ExistenceFilter", new Object());

        filter.addField("ExplicitNull_ExistenceFilter", presentFilter);
        filter.addField("AnyValue_ExistenceFilter", presentFilter);
        filter.addField("HiddenNull_ExistenceFilter", presentFilter);

        ComparatorSettings compareSettings = new ComparatorSettings();

        ComparisonResult comparisonResult = MessageComparator.compare(message, filter, compareSettings);

        validateResult(comparisonResult, 2, 1, 0);
        assertEquals(StatusType.PASSED, comparisonResult.getResult("ExplicitNull_ExistenceFilter").getStatus());
        assertEquals(StatusType.PASSED, comparisonResult.getResult("AnyValue_ExistenceFilter").getStatus());
        assertEquals(StatusType.FAILED, comparisonResult.getResult("HiddenNull_ExistenceFilter").getStatus());
    }

    private final IMessageFactory messageFactory = DefaultMessageFactory.getFactory();

    private void validateResult(ComparisonResult comparatorResult, int passed, int failed, int na) {
        System.out.println(comparatorResult);
        System.out.println();
        int allPassed = ComparisonUtil.getResultCount(comparatorResult, StatusType.PASSED);
        int allFailed = ComparisonUtil.getResultCount(comparatorResult, StatusType.FAILED);
        int allna = ComparisonUtil.getResultCount(comparatorResult, StatusType.NA);
        Assert.assertEquals("PASSED", passed, allPassed);
        Assert.assertEquals("FAILED", failed, allFailed);
        Assert.assertEquals("N/A", na, allna);
    }

    @Test
    public void testDoublePre()
    {
        IMessage mGrossRecord = messageFactory.createMessage("GrossRecord", "namespace");
        mGrossRecord.addField("BrokerId", "MEMBERFIRM1");
        mGrossRecord.addField("CurrentGross", 74598.4);
        mGrossRecord.addField("UserId", "NAT_MAX_1");

        IMessage mGrossRecord2 = messageFactory.createMessage("GrossRecord", "namespace");
        mGrossRecord2.addField("BrokerId", "MEMBERFIRM1");
        mGrossRecord2.addField("CurrentGross", 74599);
        mGrossRecord2.addField("UserId", "NAT_MAX_1");

        MetaContainer metaContainer = new MetaContainer();
        metaContainer.addDoublePrecision("CurrentGross=10");

        ActionContext settings = new ActionContext(getScriptContext(), true);
        settings.setMetaContainer(metaContainer);
        settings.setFailUnexpected("N");
        settings.setDescription("check CGC in database");
        settings.setAddToReport(true);

        ComparatorSettings compSettings = new ComparatorSettings();
        compSettings.setMetaContainer(metaContainer);

        ComparisonResult result = MessageComparator.compare(mGrossRecord, mGrossRecord2, compSettings);
        System.out.println(result);

    }

    @Test
    public void testFilters() throws IOException {
        IDictionaryStructure dictionary;

        try(InputStream stream = Files.newInputStream(Paths.get("src/test/workspace/cfg/dictionaries/test_aml.xml"))) {
            IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();
            dictionary = loader.load(stream);
        }

        Assert.assertNotNull("dictionary cannot be null", dictionary);

        ComparatorSettings settings = new ComparatorSettings().setDictionaryStructure(dictionary);
        IMessage message = new MapMessage(dictionary.getNamespace(), "ArrayMessage");
        IMessage filter = message.cloneMessage();

        filter.addField("MessageArray", StaticUtil.nullFilter(0, null));
        ComparisonResult result = MessageComparator.compare(message, filter, settings);

        Assert.assertEquals(1, ComparisonUtil.getResultCount(result, StatusType.PASSED));
        Assert.assertEquals(0, ComparisonUtil.getResultCount(result, StatusType.FAILED));
    }
}
