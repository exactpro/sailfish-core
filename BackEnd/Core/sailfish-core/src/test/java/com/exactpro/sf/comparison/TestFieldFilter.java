/*
 * Copyright 2009-2023 Exactpro (Exactpro Systems Limited)
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
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestFieldFilter extends AbstractTest {

    @Test
    public void testNullAndNotNull() {

        IMessage message = messageFactory.createMessage("name", "namespace");
        IMessage filter = messageFactory.createMessage("name", "namespace");

        IFilter nullFilter = StaticUtil.nullFilter(0, null);
        IFilter notNullFilter = StaticUtil.notNullFilter(0, null);
        IFilter missedFilter = StaticUtil.simpleFilter(0, null, "com.exactpro.sf.comparison.Convention.CONV_MISSED_OBJECT");
        IFilter presentFilter = StaticUtil.simpleFilter(0, null, "com.exactpro.sf.comparison.Convention.CONV_PRESENT_OBJECT");

        message.addField("ExplicitNull_NullFilter", null);
        filter.addField("ExplicitNull_NullFilter", nullFilter);
        filter.addField("HiddenNull_NullFilter", nullFilter);

        message.addField("ExplicitNull_EmptyValue", null);
        filter.addField("ExplicitNull_EmptyValue", missedFilter);
        filter.addField("HiddenNull_EmptyValue", missedFilter);

        message.addField("ExplicitNull_NotNullFilter", null);
        filter.addField("ExplicitNull_NotNullFilter", notNullFilter);
        filter.addField("HiddenNull_NotNullFilter", notNullFilter);

        message.addField("ExplicitNull_AnyValue", null);
        filter.addField("ExplicitNull_AnyValue", presentFilter);
        filter.addField("HiddenNull_AnyValue", presentFilter);

        ComparatorSettings compareSettings = new ComparatorSettings();

        ComparisonResult comparisonResult = MessageComparator.compare(message, filter, compareSettings);

        validateResult(comparisonResult, 4, 4, 0);
    }

    private IMessageFactory messageFactory = DefaultMessageFactory.getFactory();

    private void validateResult(ComparisonResult comparatorResult, int passed, int failed, int na) {
        System.out.println(comparatorResult);
        System.out.println();
        int apassed = ComparisonUtil.getResultCount(comparatorResult, StatusType.PASSED);
        int afailed = ComparisonUtil.getResultCount(comparatorResult, StatusType.FAILED);
        int ana = ComparisonUtil.getResultCount(comparatorResult, StatusType.NA);
        Assert.assertEquals("PASSED", passed, apassed);
        Assert.assertEquals("FAILED", failed, afailed);
        Assert.assertEquals("N/A", na, ana);
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
