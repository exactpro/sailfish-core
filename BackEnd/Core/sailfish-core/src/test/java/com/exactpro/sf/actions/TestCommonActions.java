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

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.exactpro.sf.aml.script.DefaultSettings;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.util.AbstractTest;
import com.exactpro.sf.util.DateTimeUtility;

public class TestCommonActions extends AbstractTest {
    private DefaultSettings settings;
    private CommonActions commonActions;

    @Before
    public void init(){
        settings = new DefaultSettings(getScriptContext(), true);
        commonActions = new CommonActions();
    }


	@Test
	public void testBD() {

		Object o = null;
		long l = 5l;
		o = l;

		System.out.println(o.getClass().isPrimitive());

	}


	@Ignore
	@Test
	public void testAskForContinueAction() throws Exception {
		settings.setDescription("bla-bla-bla");
		commonActions.AskForContinue(settings);
	}

	@Ignore
	@Test
	public void testTimeSync() throws Exception {
		HashMap<String, String> map = new HashMap<>();
		map.put("TimeSync", "14:54:00+1m");
		System.out.println(System.currentTimeMillis());
		commonActions.TimeSync(settings, map);
		System.out.println(System.currentTimeMillis());
	}

    @Test
    @Ignore //Manual test
	public void testWaitTillTime() throws Exception {
        HashMap < String, Object > map = new HashMap<>();
        map.put(CommonActions.WAIT_TILL_TIME_ARG_DATE, DateTimeUtility.nowLocalDateTime().plusSeconds(1));
		long beforeTime = System.currentTimeMillis();
		commonActions.WaitTillTime(settings, map);
        long afterTime = System.currentTimeMillis();
        long diff = afterTime - beforeTime;
        Assert.assertTrue(diff <= 1000 + 50 && diff >= 1000);

        map.put(CommonActions.WAIT_TILL_TIME_ARG_DATE, "s+1");
        beforeTime = System.currentTimeMillis();
        commonActions.WaitTillTime(settings, map);
        afterTime = System.currentTimeMillis();
        diff = afterTime - beforeTime;
        Assert.assertTrue(diff <= 1000 + 50 && diff >= 1000);

        map.put(CommonActions.WAIT_TILL_TIME_ARG_DATE, "s+1");
		beforeTime = System.currentTimeMillis();
		commonActions.WaitTillTime(settings, map);
        afterTime = System.currentTimeMillis();
        diff = afterTime - beforeTime;
        Assert.assertTrue(diff <= 1000 + 50 && diff >= 1000);

        map.put(CommonActions.WAIT_TILL_TIME_ARG_DATE, DateTimeUtility.nowLocalDateTime().minusSeconds(1));
        boolean checkException = false;
        try{
            commonActions.WaitTillTime(settings, map);
        } catch (EPSCommonException e){
            checkException = true;
        }
        Assert.assertTrue(checkException);
	}

	@Test
	public void testConvertProperty() {
		String expected = "waitingTimeBeforeStarting";

		Assert.assertEquals(expected, ServiceActions.convertProperty("waitingTimeBeforeStarting"));
		Assert.assertEquals(expected, ServiceActions.convertProperty("WaitingTimeBeforeStarting"));
		Assert.assertEquals(expected, ServiceActions.convertProperty("Waiting Time Before Starting"));
		Assert.assertEquals(expected, ServiceActions.convertProperty("waiting time before starting"));
		Assert.assertEquals(expected, ServiceActions.convertProperty("WAITING time BEFORE starting"));
	}
}
