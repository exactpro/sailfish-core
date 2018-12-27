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

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.util.StringUtil;
import com.exactpro.sf.aml.scriptutil.StaticUtil;
import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.comparison.ComparisonUtil;
import com.exactpro.sf.comparison.MessageComparator;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.utilitymanager.IUtilityManager;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.ArrayUtils;
/**
 * @author sergey.vasiliev
 *
 */
public class KnownBugPostValidationTest extends AbstractTest {
    private static final Function<String, String> REGEX_FILTER_BUILDER = value -> {
        return "um.call(SailfishURI.parse(\"General:MessageUtil.Regex\"), \"" + StringUtil.toJavaString(value) + "\")";
    };

    private final IUtilityManager utilityManger = SFLocalContext.getDefault().getUtilityManager();
    private final IMessageFactory mesageFactory = DefaultMessageFactory.getFactory();

    @Test
    public void testFailedKnowBugEqualsActual() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "0").build(true));
        iMessage.addField("ClOrdID", 0);

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.FAILED));
        Assert.assertEquals("Alternative value 0 with description 1 are equal to origin value 0", ExceptionUtils.getRootCause(result.getResult("ClOrdID").getException()).getMessage());
    }
    
    @Test
    public void testPassedOneKnowBugInOneField() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iMessage.addField("ClOrdID", 0);

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.PASSED));
    }

    @Test
    public void testPassedSomeKnowBugInOneField() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));
        iMessage.addField("ClOrdID", 0);

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.PASSED));
    }

    @Test
    public void testPassedOneKnowBugInSomeFields() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("1", "1").build(true));

        iMessage.addField("ClOrdID", 0);
        iMessage.addField("SecurityIDSource", 0);
        iMessage.addField("ExpireTime", 0);

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 3, ComparisonUtil.getResultCount(result, StatusType.PASSED));
    }

    @Test
    public void testPassedSomeKnowBugInSomeFields() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("2", "2").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("2", "2").build(true));

        iMessage.addField("ClOrdID", 0);
        iMessage.addField("SecurityIDSource", 0);
        iMessage.addField("ExpireTime", 0);

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 3, ComparisonUtil.getResultCount(result, StatusType.PASSED));
    }

    @Test
    public void testPassedAnyEmpty() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder(true).bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder(false).bugEmpty("1").build(true));

        iMessage.addField("SecurityIDSource", 0);

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 2, ComparisonUtil.getResultCount(result, StatusType.PASSED));
    }

    @Test
    public void testConditionallyPassedOneKnowBugInOneField() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iMessage.addField("ClOrdID", 1);

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        Assert.assertEquals("It's part of bug(s): [1]", result.getResult("ClOrdID").getExceptionMessage());
    }

    @Test
    public void testConditionallyPassedSomeKnowBugInOneField() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));
        iMessage.addField("ClOrdID", 1);

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        Assert.assertEquals("It's part of bug(s): [1]", result.getResult("ClOrdID").getExceptionMessage());
    }

    @Test
    public void testConditionallyPassedOneKnowBugInSomeFields() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("1", "1").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 1);
        iMessage.addField("ExpireTime", 1);

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 3, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        for(ComparisonResult comparisonResult : result) {
            Assert.assertEquals("It's part of bug(s): [1]", comparisonResult.getExceptionMessage());
        }
    }

    @Test
    public void testConditionallyPassedSomeKnowBugInSomeFields() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("2", "2").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("2", "2").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 2);
        iMessage.addField("ExpireTime", 2);

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 3, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        Assert.assertEquals("It's part of bug(s): [1]", result.getResult("ClOrdID").getExceptionMessage());
        Assert.assertEquals("It's part of bug(s): [2]", result.getResult("SecurityIDSource").getExceptionMessage());
        Assert.assertEquals("It's part of bug(s): [2]", result.getResult("ExpireTime").getExceptionMessage());
    }

    @Test
    public void testConditionallyPassedSomeKnowBugInSomeFieldsIntersection() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 1);

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 2, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        Assert.assertEquals("It's part of bug(s): [1]", result.getResult("ClOrdID").getExceptionMessage());
        Assert.assertEquals("It's part of bug(s): [1]", result.getResult("SecurityIDSource").getExceptionMessage());
    }

    @Test
    public void testConditionallyPassedAnyEmpty() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder(true).bugAny("1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder(false).bugEmpty("1").build(true));

        iMessage.addField("ClOrdID", 1);

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 2, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        for(ComparisonResult comparisonResult : result) {
            Assert.assertEquals("It's part of bug(s): [1]", comparisonResult.getExceptionMessage());
        }
    }

    @Test
    public void testFailedPartiallyKnownBugFailed() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 3);

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 2, ComparisonUtil.getResultCount(result, StatusType.FAILED));

        Assert.assertEquals("Known bug '1' has not been reproduced in full", result.getResult("ClOrdID").getExceptionMessage());
        Assert.assertNotNull(result.getResult("SecurityIDSource").getExceptionMessage());
    }

    @Test
    public void testFailedPartiallyKnownBugPassed() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 0);

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 2, ComparisonUtil.getResultCount(result, StatusType.FAILED));

        Assert.assertEquals("Known bug '1' has not been reproduced in full", result.getResult("ClOrdID").getExceptionMessage());
        Assert.assertEquals(appendOriginReason("Known bug '1' has not been reproduced in full", BugsCheckerBuilder.ORIGIN_VALUE_MESSAGE), result.getResult("SecurityIDSource").getExceptionMessage());
    }

    @Test
    public void testMultiBugPassed() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").bug("2", "1").build(true));

        iMessage.addField("ClOrdID", 0);

        ComparisonResult result = compare(iFilter, iMessage);
        System.out.println(result);
        Assert.assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.PASSED));
    }
    
    @Test
    public void testMultiBugConditionallyPassed() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "3").bug("2", "3").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("2", "2").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 3);
        iMessage.addField("ExpireTime", 2);

        ComparisonResult result = compare(iFilter, iMessage);
        System.out.println(result);
        Assert.assertEquals("Count of status", 3, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));

        Assert.assertEquals("It's part of bug(s): [1]", result.getResult("ClOrdID").getExceptionMessage());
        Assert.assertEquals("It's part of bug(s): [1, 2]", result.getResult("SecurityIDSource").getExceptionMessage());
        Assert.assertEquals("It's part of bug(s): [2]", result.getResult("ExpireTime").getExceptionMessage());
    }
    
    @Test
    public void testFailedPartiallyKnownBugNotIntersectionPassed() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("2", "2").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("2", "2").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 2);
        iMessage.addField("ExpireTime", 0);

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        Assert.assertEquals("Count of status", 2, ComparisonUtil.getResultCount(result, StatusType.FAILED));

        Assert.assertEquals("It's part of bug(s): [1]", result.getResult("ClOrdID").getExceptionMessage());
        Assert.assertEquals("Known bug '2' has not been reproduced in full", result.getResult("SecurityIDSource").getExceptionMessage());
        Assert.assertEquals(appendOriginReason("Known bug '2' has not been reproduced in full", BugsCheckerBuilder.ORIGIN_VALUE_MESSAGE), result.getResult("ExpireTime").getExceptionMessage());
    }

    @Test
    public void testFailedPartiallyKnownBugNotIntersectionFailed() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("2", "2").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("2", "2").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 2);
        iMessage.addField("ExpireTime", 3);

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        Assert.assertEquals("Count of status", 2, ComparisonUtil.getResultCount(result, StatusType.FAILED));

        Assert.assertEquals("It's part of bug(s): [1]", result.getResult("ClOrdID").getExceptionMessage());
        Assert.assertEquals("Known bug '2' has not been reproduced in full", result.getResult("SecurityIDSource").getExceptionMessage());
        Assert.assertNotNull(result.getResult("ExpireTime").getExceptionMessage());
    }

    @Test
    public void testFailedPartiallyKnownBugIntersectionPassed() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("2", "2").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 1);
        iMessage.addField("ExpireTime", 0);

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 2, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        Assert.assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.PASSED));

        Assert.assertEquals("It's part of bug(s): [1]", result.getResult("ClOrdID").getExceptionMessage());
        Assert.assertEquals("It's part of bug(s): [1]", result.getResult("SecurityIDSource").getExceptionMessage());
        Assert.assertEquals("", result.getResult("ExpireTime").getExceptionMessage());
    }

    @Test
    public void testFailedPartiallyKnownBugIntersectionFailed() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("2", "2").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 1);
        iMessage.addField("ExpireTime", 3);

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 2, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        Assert.assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.FAILED));

        Assert.assertEquals("It's part of bug(s): [1]", result.getResult("ClOrdID").getExceptionMessage());
        Assert.assertEquals("It's part of bug(s): [1]", result.getResult("SecurityIDSource").getExceptionMessage());
        Assert.assertNotNull(result.getResult("ExpireTime").getExceptionMessage());
    }

    @Test
    public void testFailedPartiallyKnownBugIntersection() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").bug("3", "3").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("2", "2").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 3);
        iMessage.addField("ExpireTime", 0);

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 2, ComparisonUtil.getResultCount(result, StatusType.FAILED));
        Assert.assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.PASSED));

        Assert.assertEquals("Some of defined known bugs have not been reproduced in full", result.getResult("ClOrdID").getExceptionMessage());
        Assert.assertEquals("Some of defined known bugs have not been reproduced in full", result.getResult("SecurityIDSource").getExceptionMessage());
        Assert.assertEquals("", result.getResult("ExpireTime").getExceptionMessage());
    }

    @Test
    public void testSubMessageCheckOrder() {
        List<IMessage> listMessage = new ArrayList<>();
        List<IMessage> listFilter = new ArrayList<>();

        //SubMessage 1
        IMessage iSubFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iSubMessage = iSubFilter.cloneMessage();

        iSubFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("bug", "1").build(true));
        iSubMessage.addField("ClOrdID", 1);

        listFilter.add(iSubFilter);
        listMessage.add(iSubMessage);

        //SubMessage 2
        iSubFilter = mesageFactory.createMessage("name", "namespace");
        iSubMessage = iSubFilter.cloneMessage();

        iSubFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("bug", "2").build(true));
        iSubMessage.addField("ClOrdID", 2);

        listFilter.add(iSubFilter);
        listMessage.add(iSubMessage);

        // Message
        IMessage iFilterAsc = mesageFactory.createMessage("name", "namespace");
        IMessage iFilterDesc = iFilterAsc.cloneMessage();
        IMessage iMessage = iFilterAsc.cloneMessage();

        iFilterAsc.addField("listMessage", listFilter);
        iFilterDesc.addField("listMessage", Lists.reverse(listFilter));
        iMessage.addField("listMessage", listMessage);

        ComparisonResult resultAsc = compare(iFilterAsc, iMessage);
        ComparisonResult resultDesc = compare(iFilterDesc, iMessage);

        Assert.assertTrue("Count of status", ComparisonUtil.getResultCount(resultDesc, StatusType.PASSED) == ComparisonUtil.getResultCount(resultAsc, StatusType.PASSED));
        Assert.assertTrue("Count of status", ComparisonUtil.getResultCount(resultDesc, StatusType.CONDITIONALLY_PASSED) == ComparisonUtil.getResultCount(resultAsc, StatusType.CONDITIONALLY_PASSED));
        Assert.assertTrue("Count of status", ComparisonUtil.getResultCount(resultDesc, StatusType.FAILED) == ComparisonUtil.getResultCount(resultAsc, StatusType.FAILED));
    }

    @Test
    public void testMultiConverterExpected() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iMessage.addField("ClOrdID", "0");

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.PASSED));
    }

    @Test
    public void testMultiConverterBug() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iMessage.addField("ClOrdID", "1");

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
    }

    @Test
    public void testSubMessage() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        //Body
        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("3", "3").build(true));

        iMessage.addField("ClOrdID", 0);
        iMessage.addField("SecurityIDSource", 2);
        iMessage.addField("ExpireTime", 3);

        //SubMessage
        IMessage iSubFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iSubMessage = iSubFilter.cloneMessage();

        iSubFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("3", "3").build(true));
        iSubFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));
        iSubFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("1", "1").build(true));

        iSubMessage.addField("ClOrdID", 3);
        iSubMessage.addField("SecurityIDSource", 2);
        iSubMessage.addField("ExpireTime", 0);

        iFilter.addField("subMessage", iSubFilter);
        iMessage.addField("subMessage", iSubMessage);

        List<IMessage> listFilter = new ArrayList<>();
        List<IMessage> listMessage = new ArrayList<>();

        //First Group
        iSubFilter = mesageFactory.createMessage("name", "namespace");
        iSubMessage = iSubFilter.cloneMessage();

        iSubFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));
        iSubFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("3", "3").build(true));
        iSubFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("1", "1").build(true));
        iSubFilter.addField("Addition", 0); //This field need for correct order checking in Tree comparator

        iSubMessage.addField("ClOrdID", 2);
        iSubMessage.addField("SecurityIDSource", 3);
        iSubMessage.addField("ExpireTime", 0);
        iSubMessage.addField("Addition", 0);

        listFilter.add(iSubFilter);
        listMessage.add(iSubMessage);

        //Second Group
        iSubFilter = mesageFactory.createMessage("name", "namespace");
        iSubMessage = iSubFilter.cloneMessage();

        iSubFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));
        iSubFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").build(true));
        iSubFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("3", "3").build(true));

        iSubMessage.addField("ClOrdID", 2);
        iSubMessage.addField("SecurityIDSource", 0);
        iSubMessage.addField("ExpireTime", 4);

        listFilter.add(iSubFilter);
        listMessage.add(0, iSubMessage);

        iFilter.addField("listMessage", listFilter);
        iMessage.addField("listMessage", listMessage);

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 5, ComparisonUtil.getResultCount(result, StatusType.PASSED));
        Assert.assertEquals("Count of status", 4, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        Assert.assertEquals("Count of status", 4, ComparisonUtil.getResultCount(result, StatusType.FAILED));

        Map<Integer, String> errorMap = new HashMap<>();
        errorMap.put(0, "");
        errorMap.put(2, "It's part of bug(s): [2]");
        errorMap.put(3, "Known bug '3' has not been reproduced in full");
        errorMap.put(4, "Known bug '3' has not been reproduced in full" + System.lineSeparator() +
                "FAILED: Expected 0 (Integer) value is not equal actual 4 (Integer) value");

        checkSubComparisonResult(errorMap, result, "");
    }

    @Test
    public void testSubMessageAsymmetrically() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        //Body
        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("3", "3").build(true));

        iMessage.addField("ClOrdID", 0);
        iMessage.addField("SecurityIDSource", 2);
        iMessage.addField("ExpireTime", 3);

        //SubMessage
        IMessage iSubFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iSubMessage = iSubFilter.cloneMessage();

        iSubFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("3", "3").build(true));
        iSubFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));
        iSubFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("1", "1").build(true));

        iSubMessage.addField("ClOrdID", 3);
        iSubMessage.addField("SecurityIDSource", 2);
        iSubMessage.addField("ExpireTime", 0);

        iFilter.addField("subMessage", iSubFilter);
        iMessage.addField("subMessage", iSubMessage);

        List<IMessage> listFilter = new ArrayList<>();
        List<IMessage> listMessage = new ArrayList<>();

        //First Group
        iSubFilter = mesageFactory.createMessage("name", "namespace");
        iSubMessage = iSubFilter.cloneMessage();

        iSubFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));
        iSubFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("3", "3").build(true));
        iSubFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("1", "1").build(true));
        iSubFilter.addField("Addition", 0); //This field need for correct order checking in Tree comparator

        iSubMessage.addField("ClOrdID", 2);
        iSubMessage.addField("SecurityIDSource", 3);
        iSubMessage.addField("ExpireTime", 0);
        iSubMessage.addField("Addition", 0);

        listFilter.add(iSubFilter);
        listMessage.add(iSubMessage);

        //Second Group
        iSubFilter = mesageFactory.createMessage("name", "namespace");

        iSubFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));
        iSubFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").build(true));
        iSubFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("3", "3").build(true));

        listFilter.add(iSubFilter);

        //Third Group
        iSubFilter = mesageFactory.createMessage("name", "namespace");

        iSubFilter.addField("ClOrdID", new KnownBugBuilder("0").bugEmpty("4").build(true));
        iSubFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bugEmpty("4").build(true));
        iSubFilter.addField("ExpireTime", new KnownBugBuilder("0").bugEmpty("4").build(true));

        listFilter.add(iSubFilter);

        iFilter.addField("listMessage", listFilter);
        iMessage.addField("listMessage", listMessage);

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 4, ComparisonUtil.getResultCount(result, StatusType.PASSED));
        Assert.assertEquals("Count of status", 4, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        Assert.assertEquals("Count of status", 10, ComparisonUtil.getResultCount(result, StatusType.FAILED));

        Assert.assertEquals("Body ClOrdID", "", result.getResult("ClOrdID").getExceptionMessage());
        Assert.assertEquals("Body SecurityIDSource", "Some of defined known bugs have not been reproduced in full", result.getResult("SecurityIDSource").getExceptionMessage());
        Assert.assertEquals("Body ExpireTime", "Known bug '3' has not been reproduced in full", result.getResult("ExpireTime").getExceptionMessage());
        ComparisonResult subMessage = result.getResult("subMessage");
        Assert.assertEquals("SubMessage ClOrdID", "Known bug '3' has not been reproduced in full", subMessage.getResult("ClOrdID").getExceptionMessage());
        Assert.assertEquals("SubMessage SecurityIDSource", "Some of defined known bugs have not been reproduced in full", subMessage.getResult("SecurityIDSource").getExceptionMessage());
        Assert.assertEquals("SubMessage ExpireTime", "", subMessage.getResult("ExpireTime").getExceptionMessage());
        ComparisonResult fistGroup = result.getResult("listMessage").getResult("0");
        Assert.assertEquals("FistGroup Addition", "", fistGroup.getResult("Addition").getExceptionMessage());
        Assert.assertEquals("FistGroup ClOrdID", "Some of defined known bugs have not been reproduced in full", fistGroup.getResult("ClOrdID").getExceptionMessage());
        Assert.assertEquals("FistGroup SecurityIDSource", "Known bug '3' has not been reproduced in full", fistGroup.getResult("SecurityIDSource").getExceptionMessage());
        Assert.assertEquals("FistGroup ExpireTime", "", fistGroup.getResult("ExpireTime").getExceptionMessage());
        ComparisonResult secondGroup = result.getResult("listMessage").getResult("1");
        Assert.assertEquals("SecondGroup ClOrdID", "Some of defined known bugs have not been reproduced in full" + System.lineSeparator() +
                "FAILED: Expected 0 (Integer) value is not equal actual # value", secondGroup.getResult("ClOrdID").getExceptionMessage());
        Assert.assertEquals("SecondGroup SecurityIDSource", "Expected 0 (Integer) value is not equal actual # value", secondGroup.getResult("SecurityIDSource").getExceptionMessage());
        Assert.assertEquals("SecondGroup Expected", "Known bug '3' has not been reproduced in full" + System.lineSeparator() +
                "FAILED: Expected 0 (Integer) value is not equal actual # value", secondGroup.getResult("ExpireTime").getExceptionMessage());
        ComparisonResult thirdGroup = result.getResult("listMessage").getResult("2");
        Assert.assertEquals(StatusType.CONDITIONALLY_PASSED, thirdGroup.getStatus());
        Assert.assertEquals(StatusType.CONDITIONALLY_PASSED, thirdGroup.getResult("ClOrdID").getStatus());
        Assert.assertEquals(StatusType.CONDITIONALLY_PASSED, thirdGroup.getResult("SecurityIDSource").getStatus());
        Assert.assertEquals(StatusType.CONDITIONALLY_PASSED, thirdGroup.getResult("ExpireTime").getStatus());
    }

    @Test
    public void testPostfix() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder(true).bug("1", "1").build(false));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bugEmpty("1").build(false));

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 2, ComparisonUtil.getResultCount(result, StatusType.FAILED));
        Assert.assertEquals("ClOrdID", appendOriginReason("Known bug '1' has not been reproduced in full", BugsCheckerBuilder.ORIGIN_VALUE_MESSAGE), result.getResult("ClOrdID").getExceptionMessage());
        Assert.assertEquals("SecurityIDSource", "Known bug '1' has not been reproduced in full", result.getResult("SecurityIDSource").getExceptionMessage());
    }

    private void checkSubComparisonResult(Map<Integer, String> errorMap, ComparisonResult comparisonResult, String path) {
        path += '.' + comparisonResult.getName();
        if(comparisonResult.hasResults()) {
            for(ComparisonResult subComparisonResult : comparisonResult) {
                checkSubComparisonResult(errorMap, subComparisonResult, path);
            }
        } else {
            String error = errorMap.get(comparisonResult.getActual());
            if (error != null) {
                Assert.assertEquals(path, error, comparisonResult.getExceptionMessage());
            } else {
                Assert.fail("Unkowon error for actual value " + comparisonResult.getActual());
            }
        }
    }

    @Test
    public void testConditionallyPassed() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").bug("3", "3").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("2", "2").bug("3", "3").bug("4", "4").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 1);
        iMessage.addField("ExpireTime", 4);

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 3, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));

        Assert.assertEquals("It's part of bug(s): [1]", result.getResult("ClOrdID").getExceptionMessage());
        Assert.assertEquals("It's part of bug(s): [1]", result.getResult("SecurityIDSource").getExceptionMessage());
        Assert.assertEquals("It's part of bug(s): [4]", result.getResult("ExpireTime").getExceptionMessage());
    }

    @Test
    public void testConditionallyPassedCategories() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1", "a", "b").bug("1", "3", "c", "d").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1", "a", "b").bug("2", "2", "c", "d").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("2", "2", "c", "d").bug("1", "3", "c", "d").bug("2", "4", "a", "b").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 1);
        iMessage.addField("ExpireTime", 4);

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 3, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));

        Assert.assertEquals("It's part of bug(s): [a(b(1))]", result.getResult("ClOrdID").getExceptionMessage());
        Assert.assertEquals("It's part of bug(s): [a(b(1))]", result.getResult("SecurityIDSource").getExceptionMessage());
        Assert.assertEquals("It's part of bug(s): [a(b(2))]", result.getResult("ExpireTime").getExceptionMessage());
    }

    @Test
    public void testFailed() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").bug("3", "3").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("2", "2").bug("3", "3").bug("4", "4").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 2);
        iMessage.addField("ExpireTime", 4);

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of field", 3, ComparisonUtil.getResultCount(result, StatusType.FAILED));

        for (ComparisonResult comparisonResult : result) {
            Assert.assertEquals("Field name " + comparisonResult.getName(), "Some of defined known bugs have not been reproduced in full", comparisonResult.getExceptionMessage());
        }
    }

    @Test
    public void testRegexFilter() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder(REGEX_FILTER_BUILDER.apply(".*Z")).bug("1", REGEX_FILTER_BUILDER.apply(".*S")).build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder(REGEX_FILTER_BUILDER.apply(".*Z")).bug("2", REGEX_FILTER_BUILDER.apply(".*S")).build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder(REGEX_FILTER_BUILDER.apply(".*Z")).bug("3", REGEX_FILTER_BUILDER.apply(".*S")).build(true));

        iMessage.addField("ClOrdID", "zzZ");
        iMessage.addField("SecurityIDSource", "zzS");
        iMessage.addField("ExpireTime", "zzA");

        ComparisonResult result = compare(iFilter, iMessage);

        ComparisonResult subResult = result.getResult("ClOrdID");
        Assert.assertEquals(StatusType.PASSED, subResult.getStatus());
        Assert.assertEquals("", subResult.getExceptionMessage());

        subResult = result.getResult("SecurityIDSource");
        Assert.assertEquals(StatusType.CONDITIONALLY_PASSED, subResult.getStatus());
        Assert.assertEquals("It's part of bug(s): [2]", subResult.getExceptionMessage());

        subResult = result.getResult("ExpireTime");
        Assert.assertEquals(StatusType.FAILED, subResult.getStatus());
        Assert.assertEquals("Expected .*Z (RegexMvelFilter) value is not equal actual zzA (String) value", subResult.getExceptionMessage());
    }

    @Test
    public void testPassedBugsList() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        IMessage iSubMessage = mesageFactory.createMessage("sub", "namespace");
        iSubMessage.addField("ClOrdID", 0);

        iFilter.addField("List", new KnownBugBuilder("[ref]").bug("1", "[ref, ref]").build(true, "ref", iSubMessage));
        iMessage.addField("List", ImmutableList.of(iSubMessage));

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 2, ComparisonUtil.getResultCount(result, StatusType.PASSED));
    }

    @Test
    public void testConditionallyPassedBugsList() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        IMessage iSubMessage = mesageFactory.createMessage("sub", "namespace");
        iSubMessage.addField("ClOrdID", 0);

        iFilter.addField("List", new KnownBugBuilder("[ref]").bug("1", "[ref, ref]").build(true, "ref", iSubMessage));
        iMessage.addField("List", ImmutableList.of(iSubMessage, iSubMessage));

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        Assert.assertEquals("Count of status", 2, ComparisonUtil.getResultCount(result, StatusType.PASSED));
    }

    @Test
    public void testFailedBugsList() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        IMessage iSubMessage = mesageFactory.createMessage("sub", "namespace");
        iSubMessage.addField("ClOrdID", 0);

        iFilter.addField("List", new KnownBugBuilder("[ref]").bug("1", "[ref, ref]").build(true, "ref", iSubMessage));

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.FAILED));
        Assert.assertEquals("Expected list (1) value is not equal actual list (#) value", result.getResult("List").getExceptionMessage());
    }

    @Test
    public void testMultiConditionallyPassedBugsList() {
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        IMessage iSubFilter = mesageFactory.createMessage("sub", "namespace");
        IMessage iSubMessage = iSubFilter.cloneMessage();

        iSubFilter.addField("ClOrdID", new KnownBugBuilder("1").bug("1", "0").build(true));
        iSubMessage.addField("ClOrdID", 0);

        iFilter.addField("List", new KnownBugBuilder("[ref]").bug("1", "[ref, ref]").build(true, "ref", iSubFilter));
        iMessage.addField("List", ImmutableList.of(iSubMessage, iSubMessage));

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 3, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
    }

    @Test
    public void testMultiFailedPartiallyBugsList() {
        String bugA = "A";
        String bugB = "B";
        IMessage iFilter = mesageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        IMessage iSubFilterA = mesageFactory.createMessage("sub", "namespace");
        IMessage iSubFilterB = iSubFilterA.cloneMessage();
        IMessage iSubMessageA = iSubFilterA.cloneMessage();
        IMessage iSubMessageB = iSubFilterA.cloneMessage();

        iSubFilterA.addField("ClOrdIDA", new KnownBugBuilder("1").bug(bugA, "0").build(true));
        iSubFilterB.addField("ClOrdIDB", new KnownBugBuilder("1").bug(bugB, "0").build(true));
        iSubMessageA.addField("ClOrdIDA", 1);
        iSubMessageB.addField("ClOrdIDB", 0);

        iFilter.addField("ListA", new KnownBugBuilder("[ref]").bug(bugA, "[ref, ref]").build(true, "ref", iSubFilterA));
        iMessage.addField("ListA", ImmutableList.of(iSubMessageA, iSubMessageA));

        iFilter.addField("ListB", new KnownBugBuilder("[ref]").bug(bugB, "[ref, ref]").build(true, "ref", iSubFilterB));
        iMessage.addField("ListB", ImmutableList.of(iSubMessageB));

        ComparisonResult result = compare(iFilter, iMessage);

        Assert.assertEquals("Count of status", 5, ComparisonUtil.getResultCount(result, StatusType.FAILED));

        result.getResults().values().stream()
                .flatMap(it -> Stream.concat(Stream.of(it), it.getResults().values().stream()))
                .filter(it -> it.getStatus() != null)
                .forEach(it -> {
                    String bug = it.getName().substring(it.getName().length() - 1).toLowerCase(); // BugDescription convert to lower case automatically
                    String expected = "Known bug '" + bug + "' has not been reproduced in full";
                    if ("ListB".equals(it.getName())) {
                        expected = appendOriginReason(expected, BugsListCheckerBuilder.ORIGIN_SIZE_MESSAGE);
                    }
                    Assert.assertEquals("Field name " + it.getName(), expected, it.getExceptionMessage());
                });
    }

    private String appendOriginReason(String message, String originReason) {
        return message + System.lineSeparator() + "PASSED: " + originReason;
    }

    private ComparisonResult compare(IMessage iFilter, IMessage iMessage) {
        KnownBugPostValidation validation = new KnownBugPostValidation(null, iFilter);
        ComparatorSettings compSettings = new ComparatorSettings();
        compSettings.setPostValidation(validation);

        ComparisonResult result = MessageComparator.compare(iMessage, iFilter, compSettings);
        return result;
    }

    private class KnownBugBuilder {
        private final StringBuilder builder = new StringBuilder("um.call(SailfishURI.parse(\"General:BugsUtils.Expected");

        public KnownBugBuilder(String expectedValue) {
            this.builder.append("\"), ").append(expectedValue).append(" )");
        }

        public KnownBugBuilder(boolean empty) {
            if (empty) {
                this.builder.append("Empty\"))");
            } else {
                this.builder.append("Any\"))");
            }
        }

        public KnownBugBuilder bug(String description, String value, String... categories) {
            this.builder.append(".Bug( \"").append(description).append("\" , ").append(value);
            for (String category : categories) {
                this.builder.append(" , \"").append(category).append("\"");
            }
            this.builder.append(" )");
            return this;
        }

        public KnownBugBuilder bug(String description, String value) {
            this.builder.append(".Bug( \"").append(description).append("\" , ").append(value).append(" )");
            return this;
        }

        public KnownBugBuilder bugEmpty(String description) {
            this.builder.append(".BugEmpty( \"").append(description).append("\" )");
            return this;
        }

        public KnownBugBuilder bugAny(String description) {
            this.builder.append(".BugAny( \"").append(description).append("\" )");
            return this;
        }

        public IFilter build(boolean setActual, Object ... args) {
            String postfix = "";
            if (setActual) {
                postfix = ".Actual(x)";
            }
            args = ArrayUtils.addAll(args, "um", utilityManger);
            return StaticUtil.filter(0, null, this.builder.toString() + postfix, args);
        }
    }
}
