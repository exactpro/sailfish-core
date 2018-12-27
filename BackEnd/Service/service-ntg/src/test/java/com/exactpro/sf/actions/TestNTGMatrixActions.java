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
package com.exactpro.sf.actions;

import java.time.LocalDateTime;

import com.exactpro.sf.util.DateTimeUtility;
import org.junit.Assert;
import org.junit.Test;

public class TestNTGMatrixActions {

	@Test
	public void test1() throws Exception {
	    LocalDateTime localDateTime = DateTimeUtility.nowLocalDateTime();
        long expireTime = NTGMatrixUtil.modifyDateTime(localDateTime, "MINUTE", "3");
        long expectedTime = DateTimeUtility.getMillisecond(localDateTime) + 3 * 60 * 1000;
        Assert.assertEquals(expectedTime, expireTime);
    }
}
