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
package com.exactpro.sf.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.aml.scriptutil.StaticUtil;
import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.util.StringUtil;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.comparison.ComparisonUtil;
import com.exactpro.sf.comparison.MessageComparator;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.utilitymanager.IUtilityManager;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

/**
 * @author sergey.vasiliev
 *
 */
public class KnownBugPostValidationTest extends AbstractTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(KnownBugPostValidationTest.class);
    private static final Function<String, String> REGEX_FILTER_BUILDER = value ->
            "um.call(SailfishURI.parse(\"General:MessageUtil.Regex\"), \"" + StringUtil.toJavaString(value) + "\")";

    private final IUtilityManager utilityManger = SFLocalContext.getDefault().getUtilityManager();
    private final IMessageFactory messageFactory = DefaultMessageFactory.getFactory();

    @Test
    public void testFailedKnowBugEqualsActual() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "0").build(true));
        iMessage.addField("ClOrdID", 0);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.FAILED));
        assertEquals("Alternative value 0 with description 1 are equal to origin value 0", ExceptionUtils.getRootCause(result.getResult("ClOrdID").getException()).getMessage());
    }
    
    @Test
    public void testPassedOneKnowBugInOneField() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iMessage.addField("ClOrdID", 0);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.PASSED));
    }

    @Test
    public void testPassedSomeKnowBugInOneField() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));
        iMessage.addField("ClOrdID", 0);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.PASSED));
    }

    @Test
    public void testPassedOneKnowBugInSomeFields() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("1", "1").build(true));

        iMessage.addField("ClOrdID", 0);
        iMessage.addField("SecurityIDSource", 0);
        iMessage.addField("ExpireTime", 0);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 3, ComparisonUtil.getResultCount(result, StatusType.PASSED));
    }

    @Test
    public void testPassedSomeKnowBugInSomeFields() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("2", "2").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("2", "2").build(true));

        iMessage.addField("ClOrdID", 0);
        iMessage.addField("SecurityIDSource", 0);
        iMessage.addField("ExpireTime", 0);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 3, ComparisonUtil.getResultCount(result, StatusType.PASSED));
    }

    @Test
    public void testPassedAny() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ValueAny1", new KnownBugBuilder(ExpectedType.ANY, false).build(true));
        iFilter.addField("ValueAny2", new KnownBugBuilder(ExpectedType.ANY, false).bugEmpty("1").build(true));

        iMessage.addField("ValueAny1", 0);
        iMessage.addField("ValueAny2", 0);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", iFilter.getFieldCount(), ComparisonUtil.getResultCount(result, StatusType.PASSED));
    }

    @Test
    public void testPassedEmpty() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ValueNull1", new KnownBugBuilder(ExpectedType.EMPTY, false).build(true));
        iFilter.addField("ValueNull2", new KnownBugBuilder(ExpectedType.EMPTY, false).bug("1", "1").build(true));
        iFilter.addField("ValueExistence1", new KnownBugBuilder(ExpectedType.EMPTY, false).build(true));
        iFilter.addField("ValueExistence2", new KnownBugBuilder(ExpectedType.EMPTY, false).bug("2", "2").build(true));

        iMessage.addField("ValueExistence1", null);
        iMessage.addField("ValueExistence2", null);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", iFilter.getFieldCount(), ComparisonUtil.getResultCount(result, StatusType.PASSED));
    }

    @Test
    public void testPassedExistence() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ValueExistence1", new KnownBugBuilder(ExpectedType.EXISTENCE, false).build(true));
        iFilter.addField("ValueExistence2", new KnownBugBuilder(ExpectedType.EXISTENCE, false).bugEmpty("1").build(true));
        iFilter.addField("ValueAny1", new KnownBugBuilder(ExpectedType.EXISTENCE, false).build(true));
        iFilter.addField("ValueAny2", new KnownBugBuilder(ExpectedType.EXISTENCE, false).bugEmpty("2").build(true));

        iMessage.addField("ValueExistence1", null);
        iMessage.addField("ValueExistence2", null);
        iMessage.addField("ValueAny1", 0);
        iMessage.addField("ValueAny2", 0);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", iFilter.getFieldCount(), ComparisonUtil.getResultCount(result, StatusType.PASSED));
    }

    @Test
    public void testFailedAny() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ValueNull1", new KnownBugBuilder(ExpectedType.ANY, false).build(true));
        iFilter.addField("ValueNull2", new KnownBugBuilder(ExpectedType.ANY, false).bug("1", "1").build(true));
        iFilter.addField("ValueExistence1", new KnownBugBuilder(ExpectedType.ANY, false).build(true));
        iFilter.addField("ValueExistence2", new KnownBugBuilder(ExpectedType.ANY, false).bug("2", "2").build(true));

        iMessage.addField("ValueExistence1", null);
        iMessage.addField("ValueExistence2", null);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", iFilter.getFieldCount(), ComparisonUtil.getResultCount(result, StatusType.FAILED));
        assertEquals("Expected * value is not equal actual # value", result.getResult("ValueNull1").getExceptionMessage());
        assertEquals("Expected * value is not equal actual # value", result.getResult("ValueNull2").getExceptionMessage());
        assertEquals("Expected * value is not equal actual * or sf-null value", result.getResult("ValueExistence1").getExceptionMessage());
        assertEquals("Expected * value is not equal actual * or sf-null value", result.getResult("ValueExistence2").getExceptionMessage());
    }

    @Test
    public void testFailedEmpty() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ValueAny1", new KnownBugBuilder(ExpectedType.EMPTY, false).build(true));
        iFilter.addField("ValueAny2", new KnownBugBuilder(ExpectedType.EMPTY, false).bug("1", "1").build(true));

        iMessage.addField("ValueAny1", -1);
        iMessage.addField("ValueAny2", -2);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", iFilter.getFieldCount(), ComparisonUtil.getResultCount(result, StatusType.FAILED));
        assertEquals("Expected # value is not equal actual -1 (Integer) value", result.getResult("ValueAny1").getExceptionMessage());
        assertEquals("Expected # value is not equal actual -2 (Integer) value", result.getResult("ValueAny2").getExceptionMessage());
    }

    @Test
    public void testFailedExistence() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ValueNull1", new KnownBugBuilder(ExpectedType.EXISTENCE, false).build(true));
        // Existence check covers any not null values, the next not null checks don't matter.
        // the bug method is called for test with bug description only.
        iFilter.addField("ValueNull2", new KnownBugBuilder(ExpectedType.EXISTENCE, false).bug("1", "1").build(true));

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", iFilter.getFieldCount(), ComparisonUtil.getResultCount(result, StatusType.FAILED));
        assertEquals("Expected * or sf-null value is not equal actual # value", result.getResult("ValueNull1").getExceptionMessage());
        assertEquals("Expected * or sf-null value is not equal actual # value", result.getResult("ValueNull2").getExceptionMessage());
    }

    @Test
    public void testConditionallyPassedOneKnowBugInOneField() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iMessage.addField("ClOrdID", 1);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        assertEquals("It's part of bug(s): [1]", result.getResult("ClOrdID").getExceptionMessage());
    }

    @Test
    public void testConditionallyPassedSomeKnowBugInOneField() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));
        iMessage.addField("ClOrdID", 1);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        assertEquals("It's part of bug(s): [1]", result.getResult("ClOrdID").getExceptionMessage());
    }

    @Test
    public void testConditionallyPassedOneKnowBugInSomeFields() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("1", "1").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 1);
        iMessage.addField("ExpireTime", 1);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 3, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        for(ComparisonResult comparisonResult : result) {
            assertEquals("It's part of bug(s): [1]", comparisonResult.getExceptionMessage());
        }
    }

    @Test
    public void testConditionallyPassedSomeKnowBugInSomeFields() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("2", "2").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("2", "2").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 2);
        iMessage.addField("ExpireTime", 2);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 3, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        assertEquals("It's part of bug(s): [1]", result.getResult("ClOrdID").getExceptionMessage());
        assertEquals("It's part of bug(s): [2]", result.getResult("SecurityIDSource").getExceptionMessage());
        assertEquals("It's part of bug(s): [2]", result.getResult("ExpireTime").getExceptionMessage());
    }

    @Test
    public void testConditionallyPassedSomeKnowBugInSomeFieldsIntersection() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 1);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 2, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        assertEquals("It's part of bug(s): [1]", result.getResult("ClOrdID").getExceptionMessage());
        assertEquals("It's part of bug(s): [1]", result.getResult("SecurityIDSource").getExceptionMessage());
    }

    @Test
    public void testConditionallyPassedAny() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ValueAny", new KnownBugBuilder(ExpectedType.EMPTY, false).bugAny("1").build(true));

        iMessage.addField("ValueAny", 1);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", iFilter.getFieldCount(), ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        for(ComparisonResult comparisonResult : result) {
            assertEquals("It's part of bug(s): [1]", comparisonResult.getExceptionMessage());
        }
    }

    @Test
    public void testConditionallyPassedEmpty() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ValueNull", new KnownBugBuilder(ExpectedType.ANY, false).bugEmpty("1").build(true));
        iFilter.addField("ValueExistence", new KnownBugBuilder(ExpectedType.ANY, false).bugEmpty("2").build(true));

        iMessage.addField("ValueExistence", null);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", iFilter.getFieldCount(), ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        assertEquals("It's part of bug(s): [1]", result.getResult("ValueNull").getExceptionMessage());
        assertEquals("It's part of bug(s): [2]", result.getResult("ValueExistence").getExceptionMessage());
    }

    @Test
    public void testConditionallyPassedExistence() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ValueAny", new KnownBugBuilder(ExpectedType.EMPTY, false).bugExistence("1").build(true));
        iFilter.addField("ValueExistence", new KnownBugBuilder("2", false).bugExistence("2").build(true));

        iMessage.addField("ValueAny", 1);
        iMessage.addField("ValueExistence", null);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", iFilter.getFieldCount(), ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        assertEquals("It's part of bug(s): [1]", result.getResult("ValueAny").getExceptionMessage());
        assertEquals("It's part of bug(s): [2]", result.getResult("ValueExistence").getExceptionMessage());
    }

    @Test
    public void testConditionallyPassedAnyList() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        IMessage iSubMessage = messageFactory.createMessage("sub", "namespace");

        iFilter.addField("ListEmpty", new KnownBugBuilder(ExpectedType.EMPTY, true).bugAny("1").build(true));
        iFilter.addField("ListAny", new KnownBugBuilder(ExpectedType.EMPTY, true).bugAny("2").build(true));

        iMessage.addField("ListEmpty", ImmutableList.of());
        iMessage.addField("ListAny", ImmutableList.of(iSubMessage));

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", iFilter.getFieldCount(), ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        assertEquals("It's part of bug(s): [1]", result.getResult("ListEmpty").getExceptionMessage());
        assertEquals("It's part of bug(s): [2]", result.getResult("ListAny").getExceptionMessage());
    }

    @Test
    public void testConditionallyPassedEmptyList() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ListNull", new KnownBugBuilder(ExpectedType.ANY, true).bugEmpty("1").build(true));
        iFilter.addField("ListExistence", new KnownBugBuilder(ExpectedType.ANY, true).bugEmpty("2").build(true));

        iMessage.addField("ListExistence", null);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", iFilter.getFieldCount(), ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        assertEquals("It's part of bug(s): [1]", result.getResult("ListNull").getExceptionMessage());
        assertEquals("It's part of bug(s): [2]", result.getResult("ListExistence").getExceptionMessage());
    }

    @Test
    public void testConditionallyPassedExistenceList() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        IMessage iSubMessage = messageFactory.createMessage("sub", "namespace");

        iFilter.addField("ListEmpty", new KnownBugBuilder(ExpectedType.EMPTY, true).bugExistence("1").build(true));
        iFilter.addField("ListAny", new KnownBugBuilder(ExpectedType.EMPTY, true).bugExistence("2").build(true));
        iFilter.addField("ListExistence", new KnownBugBuilder("[ref]", true).bugExistence("3").build(true, "ref", iSubMessage));

        iMessage.addField("ListEmpty", ImmutableList.of());
        iMessage.addField("ListAny", ImmutableList.of(iSubMessage));
        iMessage.addField("ListExistence", null);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", iFilter.getFieldCount(), ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        assertEquals("It's part of bug(s): [1]", result.getResult("ListEmpty").getExceptionMessage());
        assertEquals("It's part of bug(s): [2]", result.getResult("ListAny").getExceptionMessage());
        assertEquals("It's part of bug(s): [3]", result.getResult("ListExistence").getExceptionMessage());
    }

    @Test
    public void testFailedPartiallyKnownBugFailed() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 3);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 2, ComparisonUtil.getResultCount(result, StatusType.FAILED));

        assertEquals("Known bug '1' has not been reproduced in full", result.getResult("ClOrdID").getExceptionMessage());
        Assert.assertNotNull(result.getResult("SecurityIDSource").getExceptionMessage());
    }

    @Test
    public void testFailedPartiallyKnownBugPassed() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 0);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 2, ComparisonUtil.getResultCount(result, StatusType.FAILED));

        assertEquals("Known bug '1' has not been reproduced in full", result.getResult("ClOrdID").getExceptionMessage());
        assertEquals(appendOriginReason("Known bug '1' has not been reproduced in full", BugsCheckerBuilder.ORIGIN_VALUE_MESSAGE), result.getResult("SecurityIDSource").getExceptionMessage());
    }

    @Test
    public void testMultiBugPassed() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").bug("2", "1").build(true));

        iMessage.addField("ClOrdID", 0);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.PASSED));
    }
    
    @Test
    public void testMultiBugConditionallyPassed() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "3").bug("2", "3").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("2", "2").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 3);
        iMessage.addField("ExpireTime", 2);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 3, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));

        assertEquals("It's part of bug(s): [1]", result.getResult("ClOrdID").getExceptionMessage());
        assertEquals("It's part of bug(s): [1, 2]", result.getResult("SecurityIDSource").getExceptionMessage());
        assertEquals("It's part of bug(s): [2]", result.getResult("ExpireTime").getExceptionMessage());
    }
    
    @Test
    public void testFailedPartiallyKnownBugNotIntersectionPassed() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("2", "2").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("2", "2").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 2);
        iMessage.addField("ExpireTime", 0);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        assertEquals("Count of status", 2, ComparisonUtil.getResultCount(result, StatusType.FAILED));

        assertEquals("It's part of bug(s): [1]", result.getResult("ClOrdID").getExceptionMessage());
        assertEquals("Known bug '2' has not been reproduced in full", result.getResult("SecurityIDSource").getExceptionMessage());
        assertEquals(appendOriginReason("Known bug '2' has not been reproduced in full", BugsCheckerBuilder.ORIGIN_VALUE_MESSAGE), result.getResult("ExpireTime").getExceptionMessage());
    }

    @Test
    public void testFailedPartiallyKnownBugNotIntersectionFailed() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("2", "2").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("2", "2").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 2);
        iMessage.addField("ExpireTime", 3);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        assertEquals("Count of status", 2, ComparisonUtil.getResultCount(result, StatusType.FAILED));

        assertEquals("It's part of bug(s): [1]", result.getResult("ClOrdID").getExceptionMessage());
        assertEquals("Known bug '2' has not been reproduced in full", result.getResult("SecurityIDSource").getExceptionMessage());
        Assert.assertNotNull(result.getResult("ExpireTime").getExceptionMessage());
    }

    @Test
    public void testFailedPartiallyKnownBugIntersectionPassed() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("2", "2").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 1);
        iMessage.addField("ExpireTime", 0);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 2, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.PASSED));

        assertEquals("It's part of bug(s): [1]", result.getResult("ClOrdID").getExceptionMessage());
        assertEquals("It's part of bug(s): [1]", result.getResult("SecurityIDSource").getExceptionMessage());
        assertEquals("", result.getResult("ExpireTime").getExceptionMessage());
    }

    @Test
    public void testFailedPartiallyKnownBugIntersectionFailed() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("2", "2").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 1);
        iMessage.addField("ExpireTime", 3);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 2, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.FAILED));

        assertEquals("It's part of bug(s): [1]", result.getResult("ClOrdID").getExceptionMessage());
        assertEquals("It's part of bug(s): [1]", result.getResult("SecurityIDSource").getExceptionMessage());
        Assert.assertNotNull(result.getResult("ExpireTime").getExceptionMessage());
    }

    @Test
    public void testFailedPartiallyKnownBugIntersection() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").bug("3", "3").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("2", "2").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 3);
        iMessage.addField("ExpireTime", 0);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 2, ComparisonUtil.getResultCount(result, StatusType.FAILED));
        assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.PASSED));

        assertEquals("Some of defined known bugs have not been reproduced in full", result.getResult("ClOrdID").getExceptionMessage());
        assertEquals("Some of defined known bugs have not been reproduced in full", result.getResult("SecurityIDSource").getExceptionMessage());
        assertEquals("", result.getResult("ExpireTime").getExceptionMessage());
    }

    @Test
    public void testSubMessageCheckOrder() {
        List<IMessage> listMessage = new ArrayList<>();
        List<IMessage> listFilter = new ArrayList<>();

        //SubMessage 1
        IMessage iSubFilter = messageFactory.createMessage("name", "namespace");
        IMessage iSubMessage = iSubFilter.cloneMessage();

        iSubFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("bug", "1").build(true));
        iSubMessage.addField("ClOrdID", 1);

        listFilter.add(iSubFilter);
        listMessage.add(iSubMessage);

        //SubMessage 2
        iSubFilter = messageFactory.createMessage("name", "namespace");
        iSubMessage = iSubFilter.cloneMessage();

        iSubFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("bug", "2").build(true));
        iSubMessage.addField("ClOrdID", 2);

        listFilter.add(iSubFilter);
        listMessage.add(iSubMessage);

        // Message
        IMessage iFilterAsc = messageFactory.createMessage("name", "namespace");
        IMessage iFilterDesc = iFilterAsc.cloneMessage();
        IMessage iMessage = iFilterAsc.cloneMessage();

        iFilterAsc.addField("listMessage", listFilter);
        iFilterDesc.addField("listMessage", Lists.reverse(listFilter));
        iMessage.addField("listMessage", listMessage);

        ComparisonResult resultAsc = compare(iFilterAsc, iMessage);
        LOGGER.debug(resultAsc.toString());
        ComparisonResult resultDesc = compare(iFilterDesc, iMessage);
        LOGGER.debug(resultDesc.toString());

        assertEquals("Count of status", ComparisonUtil.getResultCount(resultDesc, StatusType.PASSED), ComparisonUtil.getResultCount(resultAsc, StatusType.PASSED));
        assertEquals("Count of status", ComparisonUtil.getResultCount(resultDesc, StatusType.CONDITIONALLY_PASSED), ComparisonUtil.getResultCount(resultAsc, StatusType.CONDITIONALLY_PASSED));
        assertEquals("Count of status", ComparisonUtil.getResultCount(resultDesc, StatusType.FAILED), ComparisonUtil.getResultCount(resultAsc, StatusType.FAILED));
    }

    @Test
    public void testMultiConverterExpected() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iMessage.addField("ClOrdID", "0");

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.PASSED));
    }

    @Test
    public void testMultiConverterBug() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iMessage.addField("ClOrdID", "1");

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
    }

    @Test
    public void testSubMessage() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        //Body
        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("3", "3").build(true));

        iMessage.addField("ClOrdID", 0);
        iMessage.addField("SecurityIDSource", 2);
        iMessage.addField("ExpireTime", 3);

        //SubMessage
        IMessage iSubFilter = messageFactory.createMessage("name", "namespace");
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
        iSubFilter = messageFactory.createMessage("name", "namespace");
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
        iSubFilter = messageFactory.createMessage("name", "namespace");
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
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 5, ComparisonUtil.getResultCount(result, StatusType.PASSED));
        assertEquals("Count of status", 4, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        assertEquals("Count of status", 4, ComparisonUtil.getResultCount(result, StatusType.FAILED));

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
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        //Body
        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("3", "3").build(true));

        iMessage.addField("ClOrdID", 0);
        iMessage.addField("SecurityIDSource", 2);
        iMessage.addField("ExpireTime", 3);

        //SubMessage
        IMessage iSubFilter = messageFactory.createMessage("name", "namespace");
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
        iSubFilter = messageFactory.createMessage("name", "namespace");
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
        iSubFilter = messageFactory.createMessage("name", "namespace");

        iSubFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));
        iSubFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").build(true));
        iSubFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("3", "3").build(true));

        listFilter.add(iSubFilter);

        //Third Group
        iSubFilter = messageFactory.createMessage("name", "namespace");

        iSubFilter.addField("ClOrdID", new KnownBugBuilder("0").bugEmpty("4").build(true));
        iSubFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bugEmpty("4").build(true));
        iSubFilter.addField("ExpireTime", new KnownBugBuilder("0").bugEmpty("4").build(true));

        listFilter.add(iSubFilter);

        iFilter.addField("listMessage", listFilter);
        iMessage.addField("listMessage", listMessage);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 4, ComparisonUtil.getResultCount(result, StatusType.PASSED));
        assertEquals("Count of status", 4, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        assertEquals("Count of status", 10, ComparisonUtil.getResultCount(result, StatusType.FAILED));

        assertEquals("Body ClOrdID", "", result.getResult("ClOrdID").getExceptionMessage());
        assertEquals("Body SecurityIDSource", "Some of defined known bugs have not been reproduced in full", result.getResult("SecurityIDSource").getExceptionMessage());
        assertEquals("Body ExpireTime", "Known bug '3' has not been reproduced in full", result.getResult("ExpireTime").getExceptionMessage());
        ComparisonResult subMessage = result.getResult("subMessage");
        assertEquals("SubMessage ClOrdID", "Known bug '3' has not been reproduced in full", subMessage.getResult("ClOrdID").getExceptionMessage());
        assertEquals("SubMessage SecurityIDSource", "Some of defined known bugs have not been reproduced in full", subMessage.getResult("SecurityIDSource").getExceptionMessage());
        assertEquals("SubMessage ExpireTime", "", subMessage.getResult("ExpireTime").getExceptionMessage());
        ComparisonResult fistGroup = result.getResult("listMessage").getResult("0");
        assertEquals("FistGroup Addition", "", fistGroup.getResult("Addition").getExceptionMessage());
        assertEquals("FistGroup ClOrdID", "Some of defined known bugs have not been reproduced in full", fistGroup.getResult("ClOrdID").getExceptionMessage());
        assertEquals("FistGroup SecurityIDSource", "Known bug '3' has not been reproduced in full", fistGroup.getResult("SecurityIDSource").getExceptionMessage());
        assertEquals("FistGroup ExpireTime", "", fistGroup.getResult("ExpireTime").getExceptionMessage());
        ComparisonResult secondGroup = result.getResult("listMessage").getResult("1");
        assertEquals("SecondGroup ClOrdID", "Some of defined known bugs have not been reproduced in full" + System.lineSeparator() +
                "FAILED: Expected 0 (Integer) value is not equal actual # value", secondGroup.getResult("ClOrdID").getExceptionMessage());
        assertEquals("SecondGroup SecurityIDSource", "Expected 0 (Integer) value is not equal actual # value", secondGroup.getResult("SecurityIDSource").getExceptionMessage());
        assertEquals("SecondGroup Expected", "Known bug '3' has not been reproduced in full" + System.lineSeparator() +
                "FAILED: Expected 0 (Integer) value is not equal actual # value", secondGroup.getResult("ExpireTime").getExceptionMessage());
        ComparisonResult thirdGroup = result.getResult("listMessage").getResult("2");
        assertEquals(StatusType.CONDITIONALLY_PASSED, thirdGroup.getStatus());
        assertEquals(StatusType.CONDITIONALLY_PASSED, thirdGroup.getResult("ClOrdID").getStatus());
        assertEquals(StatusType.CONDITIONALLY_PASSED, thirdGroup.getResult("SecurityIDSource").getStatus());
        assertEquals(StatusType.CONDITIONALLY_PASSED, thirdGroup.getResult("ExpireTime").getStatus());
    }

    @Test
    public void testPostfix() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder(ExpectedType.EMPTY, false).bug("1", "1").build(false));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bugEmpty("1").build(false));

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 2, ComparisonUtil.getResultCount(result, StatusType.FAILED));
        assertEquals("ClOrdID", appendOriginReason("Known bug '1' has not been reproduced in full", BugsCheckerBuilder.ORIGIN_VALUE_MESSAGE), result.getResult("ClOrdID").getExceptionMessage());
        assertEquals("SecurityIDSource", "Known bug '1' has not been reproduced in full", result.getResult("SecurityIDSource").getExceptionMessage());
    }

    private void checkSubComparisonResult(Map<Integer, String> errorMap, ComparisonResult comparisonResult, String path) {
        path += '.' + comparisonResult.getName();
        if(comparisonResult.hasResults()) {
            for(ComparisonResult subComparisonResult : comparisonResult) {
                checkSubComparisonResult(errorMap, subComparisonResult, path);
            }
        } else {
            String error = errorMap.get(comparisonResult.<Integer>getActual());
            if (error != null) {
                assertEquals(path, error, comparisonResult.getExceptionMessage());
            } else {
                Assert.fail("Unknown error for actual value " + comparisonResult.getActual());
            }
        }
    }

    @Test
    public void testConditionallyPassed() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").bug("3", "3").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("2", "2").bug("3", "3").bug("4", "4").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 1);
        iMessage.addField("ExpireTime", 4);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 3, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));

        assertEquals("It's part of bug(s): [1]", result.getResult("ClOrdID").getExceptionMessage());
        assertEquals("It's part of bug(s): [1]", result.getResult("SecurityIDSource").getExceptionMessage());
        assertEquals("It's part of bug(s): [4]", result.getResult("ExpireTime").getExceptionMessage());
    }

    @Test
    public void testConditionallyPassedCategories() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1", "a", "b").bug("1", "3", "c", "d").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1", "a", "b").bug("2", "2", "c", "d").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("2", "2", "c", "d").bug("1", "3", "c", "d").bug("2", "4", "a", "b").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 1);
        iMessage.addField("ExpireTime", 4);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 3, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));

        assertEquals("It's part of bug(s): [a(b(1))]", result.getResult("ClOrdID").getExceptionMessage());
        assertEquals("It's part of bug(s): [a(b(1))]", result.getResult("SecurityIDSource").getExceptionMessage());
        assertEquals("It's part of bug(s): [a(b(2))]", result.getResult("ExpireTime").getExceptionMessage());
    }

    @Test
    public void testFailed() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder("0").bug("1", "1").bug("3", "3").build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder("0").bug("1", "1").bug("2", "2").build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder("0").bug("2", "2").bug("3", "3").bug("4", "4").build(true));

        iMessage.addField("ClOrdID", 1);
        iMessage.addField("SecurityIDSource", 2);
        iMessage.addField("ExpireTime", 4);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of field", 3, ComparisonUtil.getResultCount(result, StatusType.FAILED));

        for (ComparisonResult comparisonResult : result) {
            assertEquals("Field name " + comparisonResult.getName(), "Some of defined known bugs have not been reproduced in full", comparisonResult.getExceptionMessage());
        }
    }

    @Test
    public void testRegexFilter() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ClOrdID", new KnownBugBuilder(REGEX_FILTER_BUILDER.apply(".*Z")).bug("1", REGEX_FILTER_BUILDER.apply(".*S")).build(true));
        iFilter.addField("SecurityIDSource", new KnownBugBuilder(REGEX_FILTER_BUILDER.apply(".*Z")).bug("2", REGEX_FILTER_BUILDER.apply(".*S")).build(true));
        iFilter.addField("ExpireTime", new KnownBugBuilder(REGEX_FILTER_BUILDER.apply(".*Z")).bug("3", REGEX_FILTER_BUILDER.apply(".*S")).build(true));

        iMessage.addField("ClOrdID", "zzZ");
        iMessage.addField("SecurityIDSource", "zzS");
        iMessage.addField("ExpireTime", "zzA");

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        ComparisonResult subResult = result.getResult("ClOrdID");
        assertEquals(StatusType.PASSED, subResult.getStatus());
        assertEquals("", subResult.getExceptionMessage());

        subResult = result.getResult("SecurityIDSource");
        assertEquals(StatusType.CONDITIONALLY_PASSED, subResult.getStatus());
        assertEquals("It's part of bug(s): [2]", subResult.getExceptionMessage());

        subResult = result.getResult("ExpireTime");
        assertEquals(StatusType.FAILED, subResult.getStatus());
        assertEquals("Expected .*Z (RegexMvelFilter) value is not equal actual zzA (String) value", subResult.getExceptionMessage());
    }

    @Test
    public void testPassedBugsList() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        IMessage iSubMessage = messageFactory.createMessage("sub", "namespace");
        iSubMessage.addField("ClOrdID", 0);

        iFilter.addField("List", new KnownBugBuilder("[ref]", true).bug("1", "[ref, ref]").build(true, "ref", iSubMessage));
        iMessage.addField("List", ImmutableList.of(iSubMessage));

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 2, ComparisonUtil.getResultCount(result, StatusType.PASSED));
    }

    @Test
    public void testConditionallyPassedBugsList() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        IMessage iSubMessage = messageFactory.createMessage("sub", "namespace");
        iSubMessage.addField("ClOrdID", 0);

        iFilter.addField("List", new KnownBugBuilder("[ref]", true).bug("1", "[ref, ref]").build(true, "ref", iSubMessage));
        iMessage.addField("List", ImmutableList.of(iSubMessage, iSubMessage));

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
        assertEquals("Count of status", 2, ComparisonUtil.getResultCount(result, StatusType.PASSED));
    }

    @Test
    public void testFailedBugsList() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        IMessage iSubMessage = messageFactory.createMessage("sub", "namespace");
        iSubMessage.addField("ClOrdID", 0);

        iFilter.addField("List", new KnownBugBuilder("[ref]", true).bug("1", "[ref, ref]").build(true, "ref", iSubMessage));

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 1, ComparisonUtil.getResultCount(result, StatusType.FAILED));
        assertEquals("Expected list (1) value is not equal actual list (#) value", result.getResult("List").getExceptionMessage());
    }

    @Test
    public void testMultiConditionallyPassedBugsList() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        IMessage iSubFilter = messageFactory.createMessage("sub", "namespace");
        IMessage iSubMessage = iSubFilter.cloneMessage();

        iSubFilter.addField("ClOrdID", new KnownBugBuilder("1").bug("1", "0").build(true));
        iSubMessage.addField("ClOrdID", 0);

        iFilter.addField("List", new KnownBugBuilder("[ref]", true).bug("1", "[ref, ref]").build(true, "ref", iSubFilter));
        iMessage.addField("List", ImmutableList.of(iSubMessage, iSubMessage));

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 3, ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED));
    }

    @Test
    public void testMultiFailedPartiallyBugsList() {
        String bugA = "A";
        String bugB = "B";
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        IMessage iSubFilterA = messageFactory.createMessage("sub", "namespace");
        IMessage iSubFilterB = iSubFilterA.cloneMessage();
        IMessage iSubMessageA = iSubFilterA.cloneMessage();
        IMessage iSubMessageB = iSubFilterA.cloneMessage();

        iSubFilterA.addField("ClOrdIDA", new KnownBugBuilder("1").bug(bugA, "0").build(true));
        iSubFilterB.addField("ClOrdIDB", new KnownBugBuilder("1").bug(bugB, "0").build(true));
        iSubMessageA.addField("ClOrdIDA", 1);
        iSubMessageB.addField("ClOrdIDB", 0);

        iFilter.addField("ListA", new KnownBugBuilder("[ref]", true).bug(bugA, "[ref, ref]").build(true, "ref", iSubFilterA));
        iMessage.addField("ListA", ImmutableList.of(iSubMessageA, iSubMessageA));

        iFilter.addField("ListB", new KnownBugBuilder("[ref]", true).bug(bugB, "[ref, ref]").build(true, "ref", iSubFilterB));
        iMessage.addField("ListB", ImmutableList.of(iSubMessageB));

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", 5, ComparisonUtil.getResultCount(result, StatusType.FAILED));

        result.getResults().values().stream()
                .flatMap(it -> Stream.concat(Stream.of(it), it.getResults().values().stream()))
                .filter(it -> it.getStatus() != null)
                .forEach(it -> {
                    String bug = it.getName().substring(it.getName().length() - 1).toLowerCase(); // BugDescription convert to lower case automatically
                    String expected = "Known bug '" + bug + "' has not been reproduced in full";
                    if ("ListB".equals(it.getName())) {
                        expected = appendOriginReason(expected, BugsListCheckerBuilder.ORIGIN_SIZE_MESSAGE);
                    }
                    assertEquals("Field name " + it.getName(), expected, it.getExceptionMessage());
                });
    }

    @Test
    public void testPassedAnyList() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        IMessage iSubMessage = messageFactory.createMessage("sub", "namespace");

        iFilter.addField("ListEmpty1", new KnownBugBuilder(ExpectedType.ANY, true).build(true));
        iFilter.addField("ListEmpty2", new KnownBugBuilder(ExpectedType.ANY, true).bugEmpty("1").build(true));
        iFilter.addField("ListAny1", new KnownBugBuilder(ExpectedType.ANY, true).build(true));
        iFilter.addField("ListAny2", new KnownBugBuilder(ExpectedType.ANY, true).bugEmpty("2").build(true));

        iMessage.addField("ListEmpty1", ImmutableList.of());
        iMessage.addField("ListEmpty2", ImmutableList.of());
        iMessage.addField("ListAny1", ImmutableList.of(iSubMessage));
        iMessage.addField("ListAny2", ImmutableList.of(iSubMessage));

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", iFilter.getFieldCount(), ComparisonUtil.getResultCount(result, StatusType.PASSED));
    }

    @Test
    public void testPassedEmptyList() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        iFilter.addField("ListNull1", new KnownBugBuilder(ExpectedType.EMPTY, true).build(true));
        iFilter.addField("ListNull2", new KnownBugBuilder(ExpectedType.EMPTY, true).bugAny("1").build(true));
        iFilter.addField("ListExistence1", new KnownBugBuilder(ExpectedType.EMPTY, true).build(true));
        iFilter.addField("ListExistence2", new KnownBugBuilder(ExpectedType.EMPTY, true).bugAny("2").build(true));

        iMessage.addField("ListExistence1", null);
        iMessage.addField("ListExistence2", null);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", iFilter.getFieldCount(), ComparisonUtil.getResultCount(result, StatusType.PASSED));
    }

    @Test
    public void testPassedExistenceList() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        IMessage iSubMessage = messageFactory.createMessage("sub", "namespace");

        iFilter.addField("ListEmpty1", new KnownBugBuilder(ExpectedType.EXISTENCE, true).build(true));
        iFilter.addField("ListEmpty2", new KnownBugBuilder(ExpectedType.EXISTENCE, true).bugEmpty("1").build(true));
        iFilter.addField("ListAny1", new KnownBugBuilder(ExpectedType.EXISTENCE, true).build(true));
        iFilter.addField("ListAny2", new KnownBugBuilder(ExpectedType.EXISTENCE, true).bugEmpty("2").build(true));
        iFilter.addField("ListExistence1", new KnownBugBuilder(ExpectedType.EXISTENCE, true).build(true));
        iFilter.addField("ListExistence2", new KnownBugBuilder(ExpectedType.EXISTENCE, true).bugEmpty("3").build(true));

        iMessage.addField("ListEmpty1", ImmutableList.of());
        iMessage.addField("ListEmpty2", ImmutableList.of());
        iMessage.addField("ListAny1", ImmutableList.of(iSubMessage));
        iMessage.addField("ListAny2", ImmutableList.of(iSubMessage));
        iMessage.addField("ListExistence1", null);
        iMessage.addField("ListExistence2", null);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", iFilter.getFieldCount(), ComparisonUtil.getResultCount(result, StatusType.PASSED));
    }

    @Test
    public void testFailedAnyList() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        IMessage iSubMessage = messageFactory.createMessage("sub", "namespace");

        iFilter.addField("ListNull1", new KnownBugBuilder(ExpectedType.ANY, true).build(true));
        // Any check covers any not null values, the next not null checks don't matter.
        // the bug method is called for test with bug description only.
        iFilter.addField("ListNull2", new KnownBugBuilder(ExpectedType.ANY, true).bug("1", "[ref]").build(true, "ref", iSubMessage));
        iFilter.addField("ListExistence1", new KnownBugBuilder(ExpectedType.ANY, true).build(true));
        // Any check covers any not null values, the next not null checks don't matter.
        // the bug method is called for test with bug description only.
        iFilter.addField("ListExistence2", new KnownBugBuilder(ExpectedType.ANY, true).bug("2", "[ref]").build(true, "ref", iSubMessage));

        iMessage.addField("ListExistence1", null);
        iMessage.addField("ListExistence2", null);

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", iFilter.getFieldCount(), ComparisonUtil.getResultCount(result, StatusType.FAILED));
        assertEquals("Expected list (*) value is not equal actual list (#) value", result.getResult("ListNull1").getExceptionMessage());
        assertEquals("Expected list (*) value is not equal actual list (#) value", result.getResult("ListNull2").getExceptionMessage());
        assertEquals("Expected list (*) value is not equal actual list (sf-null) value", result.getResult("ListExistence1").getExceptionMessage());
        assertEquals("Expected list (*) value is not equal actual list (sf-null) value", result.getResult("ListExistence2").getExceptionMessage());
    }

    @Test
    public void testFailedEmptyList() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        IMessage iSubMessage = messageFactory.createMessage("sub", "namespace");

        iFilter.addField("ListAny1", new KnownBugBuilder(ExpectedType.EMPTY, true).build(true));
        iFilter.addField("ListAny2", new KnownBugBuilder(ExpectedType.EMPTY, true).bug("1", "[ref, ref]").build(true, "ref", iSubMessage));
        iFilter.addField("ListEmpty1", new KnownBugBuilder(ExpectedType.EMPTY, true).build(true));
        iFilter.addField("ListEmpty2", new KnownBugBuilder(ExpectedType.EMPTY, true).bug("2", "[ref, ref]").build(true, "ref", iSubMessage));

        iMessage.addField("ListAny1", ImmutableList.of(iSubMessage));
        iMessage.addField("ListAny2", ImmutableList.of(iSubMessage));
        iMessage.addField("ListEmpty1", ImmutableList.of());
        iMessage.addField("ListEmpty2", ImmutableList.of());

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", iFilter.getFieldCount(), ComparisonUtil.getResultCount(result, StatusType.FAILED));
        assertEquals("Expected list (#) value is not equal actual list (1) value", result.getResult("ListAny1").getExceptionMessage());
        assertEquals("Expected list (#) value is not equal actual list (1) value", result.getResult("ListAny2").getExceptionMessage());
        assertEquals("Expected list (#) value is not equal actual list (0) value", result.getResult("ListEmpty1").getExceptionMessage());
        assertEquals("Expected list (#) value is not equal actual list (0) value", result.getResult("ListEmpty2").getExceptionMessage());
    }

    @Test
    public void testFailedExistenceList() {
        IMessage iFilter = messageFactory.createMessage("name", "namespace");
        IMessage iMessage = iFilter.cloneMessage();

        IMessage iSubMessage = messageFactory.createMessage("sub", "namespace");

        iFilter.addField("ListNull1", new KnownBugBuilder(ExpectedType.EXISTENCE, true).build(true));
        // Existence check covers any not null values, the next not null checks don't matter.
        // the bug method is called for test with bug description only.
        iFilter.addField("ListNull2", new KnownBugBuilder(ExpectedType.EXISTENCE, true).bug("1","[ref]").build(true, "ref", iSubMessage));

        ComparisonResult result = compare(iFilter, iMessage);
        LOGGER.debug(result.toString());

        assertEquals("Count of status", iFilter.getFieldCount(), ComparisonUtil.getResultCount(result, StatusType.FAILED));
        assertEquals("Expected list (sf-null) value is not equal actual list (#) value", result.getResult("ListNull1").getExceptionMessage());
        assertEquals("Expected list (sf-null) value is not equal actual list (#) value", result.getResult("ListNull2").getExceptionMessage());

    }

    private String appendOriginReason(String message, String originReason) {
        return message + System.lineSeparator() + "PASSED: " + originReason;
    }

    private ComparisonResult compare(IMessage iFilter, IMessage iMessage) {
        KnownBugPostValidation validation = new KnownBugPostValidation(null, iFilter);
        ComparatorSettings compSettings = new ComparatorSettings();
        compSettings.setPostValidation(validation);

        return MessageComparator.compare(iMessage, iFilter, compSettings);
    }

    private enum ExpectedType {
        ANY("Any"),
        EMPTY("Empty"),
        EXISTENCE("Existence");

        private final String value;

        ExpectedType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private class KnownBugBuilder {
        private final StringBuilder builder = new StringBuilder("um.call(SailfishURI.parse(\"General:BugsUtils.Expected");

        public KnownBugBuilder(String expectedValue) {
            this(expectedValue, false);
        }

        public KnownBugBuilder(String expectedValue, boolean list) {
            if(list) {
                builder.append("List");
            }

            builder.append("\"), ").append(expectedValue).append(" )");
        }

        public KnownBugBuilder(ExpectedType expectedType, boolean list) {
            builder.append(expectedType.getValue());

            if (list) {
                builder.append("List");
            }

            builder.append("\"))");
        }

        public KnownBugBuilder bug(String description, String value, String... categories) {
            builder.append(".Bug( \"").append(description).append("\" , ").append(value);
            for (String category : categories) {
                builder.append(" , \"").append(category).append("\"");
            }
            builder.append(" )");
            return this;
        }

        public KnownBugBuilder bug(String description, String value) {
            builder.append(".Bug( \"").append(description).append("\" , ").append(value).append(" )");
            return this;
        }

        public KnownBugBuilder bugEmpty(String description) {
            builder.append(".BugEmpty( \"").append(description).append("\" )");
            return this;
        }

        public KnownBugBuilder bugAny(String description) {
            builder.append(".BugAny( \"").append(description).append("\" )");
            return this;
        }

        public KnownBugBuilder bugExistence(String description) {
            builder.append(".BugExistence( \"").append(description).append("\" )");
            return this;
        }

        public IFilter build(boolean setActual, Object ... args) {
            String postfix = "";
            if (setActual) {
                postfix = ".Actual(x)";
            }
            args = ArrayUtils.addAll(args, "um", utilityManger);
            return StaticUtil.filter(0, null, builder + postfix, args);
        }
    }
}
