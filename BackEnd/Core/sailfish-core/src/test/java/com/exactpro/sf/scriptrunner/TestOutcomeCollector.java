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
package com.exactpro.sf.scriptrunner;

import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.scriptrunner.Outcome.Status;
import com.exactpro.sf.util.AbstractTest;

public class TestOutcomeCollector extends AbstractTest {

	@Test
	public void testPassed() {
		OutcomeCollector collector = new OutcomeCollector();
		collector.storeOutcome(createOutcome(1, 1, Status.PASSED));
		Assert.assertEquals(Status.PASSED, getStatus(collector, 1, 1));
        Assert.assertEquals(Status.PASSED, getGroupStatus(collector, 1, false));
		checkCount(collector, 1, 1, 1, 0, 0);
		
		collector = new OutcomeCollector();
		collector.storeOutcome(createOutcome(1, 1, Status.PASSED));
		collector.storeOutcome(createOutcome(1, 1, Status.PASSED));
		Assert.assertEquals(Status.PASSED, getStatus(collector, 1, 1));
        Assert.assertEquals(Status.PASSED, getGroupStatus(collector, 1, false));
		checkCount(collector, 1, 1, 2, 0, 0);
	}
	
	@Test
	public void testConditionalyPassed() {
		OutcomeCollector collector = new OutcomeCollector();
		collector.storeOutcome(createOutcome(1, 1, Status.CONDITIONALLY_PASSED));
		Assert.assertEquals(Status.CONDITIONALLY_PASSED, getStatus(collector, 1, 1));
        Assert.assertEquals(Status.CONDITIONALLY_PASSED, getGroupStatus(collector, 1, false));
		checkCount(collector, 1, 1, 0, 1, 0);
		
		collector = new OutcomeCollector();
		collector.storeOutcome(createOutcome(1, 1, Status.CONDITIONALLY_PASSED));
		collector.storeOutcome(createOutcome(1, 1, Status.CONDITIONALLY_PASSED));
		Assert.assertEquals(Status.CONDITIONALLY_PASSED, getStatus(collector, 1, 1));
        Assert.assertEquals(Status.CONDITIONALLY_PASSED, getGroupStatus(collector, 1, false));
		checkCount(collector, 1, 1, 0, 2, 0);
		
		collector = new OutcomeCollector();
		collector.storeOutcome(createOutcome(1, 1, Status.PASSED));
		collector.storeOutcome(createOutcome(1, 1, Status.CONDITIONALLY_PASSED));
		Assert.assertEquals(Status.CONDITIONALLY_PASSED, getStatus(collector, 1, 1));
        Assert.assertEquals(Status.CONDITIONALLY_PASSED, getGroupStatus(collector, 1, false));
		checkCount(collector, 1, 1, 1, 1, 0);
		
		collector = new OutcomeCollector();
		collector.storeOutcome(createOutcome(1, 1, Status.CONDITIONALLY_PASSED));
		collector.storeOutcome(createOutcome(1, 1, Status.PASSED));
		Assert.assertEquals(Status.CONDITIONALLY_PASSED, getStatus(collector, 1, 1));
        Assert.assertEquals(Status.CONDITIONALLY_PASSED, getGroupStatus(collector, 1, false));
		checkCount(collector, 1, 1, 1, 1, 0);
	}
	
	@Test
	public void testFailed() {
		OutcomeCollector collector = new OutcomeCollector();
		collector.storeOutcome(createOutcome(1, 1, Status.FAILED));
		Assert.assertEquals(Status.FAILED, getStatus(collector, 1, 1));
        Assert.assertEquals(Status.FAILED, getGroupStatus(collector, 1, true));
		checkCount(collector, 1, 1, 0, 0, 1);
		
		collector = new OutcomeCollector();
		collector.storeOutcome(createOutcome(1, 1, Status.FAILED));
		collector.storeOutcome(createOutcome(1, 1, Status.FAILED));
		Assert.assertEquals(Status.FAILED, getStatus(collector, 1, 1));
        Assert.assertEquals(Status.FAILED, getGroupStatus(collector, 1, true));
		checkCount(collector, 1, 1, 0, 0, 2);
		
		collector = new OutcomeCollector();
		collector.storeOutcome(createOutcome(1, 1, Status.PASSED));
		collector.storeOutcome(createOutcome(1, 1, Status.FAILED));
		Assert.assertEquals(Status.FAILED, getStatus(collector, 1, 1));
        Assert.assertEquals(Status.FAILED, getGroupStatus(collector, 1, true));
		checkCount(collector, 1, 1, 1, 0, 1);
		
		collector = new OutcomeCollector();
		collector.storeOutcome(createOutcome(1, 1, Status.FAILED));
		collector.storeOutcome(createOutcome(1, 1, Status.PASSED));
		Assert.assertEquals(Status.FAILED, getStatus(collector, 1, 1));
        Assert.assertEquals(Status.FAILED, getGroupStatus(collector, 1, true));
		checkCount(collector, 1, 1, 1, 0, 1);
		
		collector = new OutcomeCollector();
		collector.storeOutcome(createOutcome(1, 1, Status.CONDITIONALLY_PASSED));
		collector.storeOutcome(createOutcome(1, 1, Status.FAILED));
		Assert.assertEquals(Status.FAILED, getStatus(collector, 1, 1));
        Assert.assertEquals(Status.FAILED, getGroupStatus(collector, 1, true));
		checkCount(collector, 1, 1, 0, 1, 1);
		
		collector = new OutcomeCollector();
		collector.storeOutcome(createOutcome(1, 1, Status.FAILED));
		collector.storeOutcome(createOutcome(1, 1, Status.CONDITIONALLY_PASSED));
		Assert.assertEquals(Status.FAILED, getStatus(collector, 1, 1));
        Assert.assertEquals(Status.FAILED, getGroupStatus(collector, 1, true));
		checkCount(collector, 1, 1, 0, 1, 1);
	}
	
	@Test
	public void testMultiGroup() {
		OutcomeCollector collector = new OutcomeCollector();
		collector.storeOutcome(createOutcome(1, 1, Status.PASSED));
		
		collector.storeOutcome(createOutcome(1, 2, Status.PASSED));
		collector.storeOutcome(createOutcome(1, 2, Status.CONDITIONALLY_PASSED));
		
		collector.storeOutcome(createOutcome(1, 3, Status.PASSED));
		collector.storeOutcome(createOutcome(1, 3, Status.CONDITIONALLY_PASSED));
		collector.storeOutcome(createOutcome(1, 3, Status.FAILED));
		
		Assert.assertEquals(Status.PASSED, getStatus(collector, 1, 1));
		Assert.assertEquals(Status.CONDITIONALLY_PASSED, getStatus(collector, 1, 2));
		Assert.assertEquals(Status.FAILED, getStatus(collector, 1, 3));
        Assert.assertEquals(Status.PASSED, getGroupStatus(collector, 1, false));
		checkCount(collector, 1, 1, 1, 0, 0);
		checkCount(collector, 1, 2, 1, 1, 0);
		checkCount(collector, 1, 3, 1, 1, 1);
	}
	
	private Status getStatus(OutcomeCollector collector, int group, int name) {
		String str_group = Integer.toString(group);
		String str_name = Integer.toString(name);
		collector.onOutcomeComplete(str_group, str_name);
		return collector.getOutcomeStatus(str_group, str_name);
	}
	
    private Status getGroupStatus(OutcomeCollector collector, int group, boolean exception) {
		String str_group = Integer.toString(group);
		try {
			collector.onGroupComplete(str_group);
			if (exception) {
				Assert.fail("Group " + group + " not failed");
			}
		} catch (Exception e) {
			if (!exception) {
				Assert.fail("Group " + group + " failed " + e.getMessage());
			}
		}
		return collector.getGroupStatus(str_group);
	}
	
	private Outcome createOutcome(int group, int name, Status status) {
		Outcome outcome = new Outcome(Integer.toString(group), Integer.toString(name));
		outcome.setStatus(status);
		return outcome;
	}
	
	private void checkCount(OutcomeCollector collector, int group, int name, int passed, int conditionaly_passed, int failed) {
		String str_group = Integer.toString(group);
		String str_name = Integer.toString(name);
		Assert.assertEquals(passed, collector.getPassedCount(str_group, str_name));
		Assert.assertEquals(conditionaly_passed, collector.getConditionallyPassedCount(str_group, str_name));
		Assert.assertEquals(failed, collector.getFailedCount(str_group, str_name));
	}

}
