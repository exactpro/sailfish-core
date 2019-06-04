/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.sf.embedded.statistics.storage.utils;

import static com.exactpro.sf.embedded.statistics.storage.utils.Conditions.*;
import static org.junit.Assert.*;

import org.junit.Test;

public class TestConditions {

    @Test
    public void testCreate() {
        String value = "value == 42";
        ICondition condition = create(value);

        assertEquals(value, condition.getCondition());
    }

    @Test
    public void testOr() {
        String value1 = "value == 3";
        ICondition condition1 = create(value1);

        String value2 = "data < 9.05.1945";
        ICondition condition2 = create(value2);

        String value3 = "time < 21:42";
        ICondition condition3 = create(value3);

        ICondition orCondition = or(condition1, condition2, condition3);

        assertEquals(String.join(" or ", value1, value2, value3), orCondition.getCondition());
    }

    @Test
    public void testAnd() {
        String value1 = "value == 3";
        ICondition condition1 = create(value1);

        String value2 = "data < 9.05.1945";
        ICondition condition2 = create(value2);

        String value3 = "time < 21:42";
        ICondition condition3 = create(value3);

        ICondition andCondition = and(condition1, condition2, condition3);

        assertEquals(String.join(" and ", value1, value2, value3), andCondition.getCondition());
    }

    @Test
    public void testWrap() {
        String value = "value == 3";
        ICondition condition = create(value);
        ICondition wrapped = wrap(condition);

        assertEquals("(" + value + ")", wrapped.getCondition());
    }
}