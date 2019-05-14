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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.scriptrunner.Outcome.Status;

public class OutcomeCollector {

	private static final Logger logger = LoggerFactory.getLogger(OutcomeCollector.class);

	private final Map<String, Group> groups;
	private final List<String> groupOrder;
	private final Map<String, List<String>> completed;
	private final Map<String, Set<String>> defined;

	public OutcomeCollector() {
		this.groups = new HashMap<>();
		this.groupOrder = new ArrayList<>();
		this.completed = new HashMap<>();
		this.defined = new HashMap<>();
	}

	/**
	 * Clear outcome state
	 */
	public void clear() {
        for(List<String> list : completed.values()) {
			list.clear();
		}
        completed.clear();
        groups.clear();
        groupOrder.clear();
        defined.clear();
	}

	/**
	 * This method called from generated action.
	 * It update outcome status.
	 * If outcome status is FAILED this method add outcome to failed set.
	 * @param outcome
	 */
	public void storeOutcome(Outcome outcome) {
		logger.debug("storeOutcome: {}", outcome);
        Group group = groups.get(outcome.getGroup());
		if (group == null) {
			group = new Group();
            groups.put(outcome.getGroup(), group);
            groupOrder.add(outcome.getGroup());
		}

		if (outcome.getStatus() == Status.PASSED) {
			group.passed.add(outcome.getName());
		}

        if (outcome.getStatus() == Status.CONDITIONALLY_PASSED) {
            group.conditionallyPassed.add(outcome.getName());
        }

		if (outcome.getStatus() == Status.FAILED) {
			group.failed.add(outcome.getName());
            group.failedCounter.add(outcome.getName());
			logger.debug("storeOutcome: {}", group.failed);
		}
	}

	/**
	 * Check if Outcome is complete and if it is passed.
	 * @param group outcome group identifier
	 * @param name outcome name identifier
	 * @return {@code false} if Outcome is not completed yet or it is failed.
	 *  {@code true} if Outcome completed and all verifications are passed.
	 */
    public Status getOutcomeStatus(String group, String name) {
        Group grp = groups.get(group);
		if (grp == null) {
			logger.debug("getOutcomeStatus: {}:{} grp {} not started", group, name, group);
            return Status.PASSED;
		}

        if (grp.failed.contains(name)) {
            logger.debug("getOutcomeStatus: {}:{} Failed contain name: {}", group, name, name);
            return Status.FAILED;
        }

        if (grp.conditionallyPassed.counter.containsKey(name)) {
            logger.debug("getOutcomeStatus: {}:{} ConditionallyPassed contain name: {}", group, name, name);
            return Status.CONDITIONALLY_PASSED;
        }

		logger.debug("getOutcomeStatus: {}:{} groupPassed: {}", group, name, !grp.groupPassed);
        return Status.PASSED;
	}

    public Status getGroupStatus(String group) {
        Group grp = groups.get(group);
		if (grp == null) {
			logger.error("getGroupStatus: group {} not exist", group);
			throw new EPSCommonException("getGroupStatus: group \""+group+"\" not exist");
		}

        if (grp.groupPassed) {
            return Status.PASSED;
        } else if (grp.groupConditionallyPassed) {
            return Status.CONDITIONALLY_PASSED;
        } else {
            return Status.FAILED;
        }
	}

	public int getPassedCount(String group, String name)
	{
        Group grp = groups.get(group);
		if (grp == null) {
			return 0;
		}

		Integer cnt = grp.passed.get(name);
        return cnt == null ? 0 : cnt;
    }

    public int getConditionallyPassedCount(String group, String name) {
        Group grp = groups.get(group);
        if (grp == null) {
            return 0;
        }

        Integer cnt = grp.conditionallyPassed.get(name);
        return cnt == null ? 0 : cnt;
    }

    public int getFailedCount(String group, String name) {
        Group grp = groups.get(group);
        if (grp == null) {
            return 0;
        }

        Integer cnt = grp.failedCounter.get(name);
        return cnt == null ? 0 : cnt;
    }

	/**
	 * Called from report generator.
	 * @return all completed outcomes
	 */
	public Map<String, List<String>> getOutcomes() {
        return completed;
	}

	public List<String> getGroupOrder() {
        return groupOrder;
	}

	/**
	 * Called from report generator.
	 * @return all possible outcomes
	 */
	public Map<String, Set<String>> getDefinedOutcomes() {
        return defined;
	}

	/**
	 * Calculate status for outcome.
	 * @param group outcome group
	 * @param name outcome name
	 */
	public void onOutcomeComplete(String group, String name)
	{
        Group grp = groups.get(group);
		if (grp == null) {
			throw new NullPointerException("Group="+group+", name="+name);
		}
        if (!grp.failed.contains(name)) {
			grp.name = name;
            if (grp.conditionallyPassed.counter.containsKey(name)) {
                grp.groupConditionallyPassed = true;
            } else {
                grp.groupPassed = true;
            }
		}
		logger.debug("onOutcomeComplete: {}:{} groupPassed: {} groupConditionallyPassed: {}", group, name, grp.groupPassed, grp.groupConditionallyPassed);

        Set<String> set = defined.computeIfAbsent(group, k -> new HashSet<>());
		set.add(name);
	}

	/**
	 * 
	 * @param group
	 * @throws EPSCommonException if group's status is FAILED
	 */
    public void onGroupComplete(String group)
	{
        Group grp = groups.get(group);
		if (grp == null) {
			throw new NullPointerException("Group is null: "+group);
		}

		if (grp.name == null) {
			grp.name = "Unexpected";
		}

        List<String> list = completed.computeIfAbsent(group, k -> new LinkedList<>());

		list.add(grp.name);

        if (!grp.groupPassed && !grp.groupConditionallyPassed) {
            throw new EPSCommonException("All cases from outcome group (" + group + ") are failed");
        }

		grp.name = null;
        grp.failed.clear();
	}

	class Group
	{
        public Counter passed = new Counter();
		/**
		 * Unique name of the group
		 */
		String name;
		/**
         * Contain conditionally_passed variant of outcome.
         */
        public Counter conditionallyPassed = new Counter();

        public Counter failedCounter = new Counter();
        /**
         * Contain failed variant of outcome.
         */
        public Set<String> failed = new HashSet<>();
		/**
		 * Set when first result of outcome is PASSED or all possible variants are FAILED
		 */
        boolean groupPassed;
        /**
         * Set when first result of outcome is CONDITIONALLY PASSED or all
         * possible variants are FAILED
         */
        boolean groupConditionallyPassed;
	}

    class Counter
	{
        private final Map<String, Integer> counter = new HashMap<>();

		public void add(String name) {
			Integer cnt = counter.get(name);
			if (cnt == null) {
				cnt = 0;
			}
			counter.put(name, cnt+1);
		}

		public Integer get(String name) {
			return counter.get(name);
		}

		public int getCount() {
            return counter.size();
		}
	}
}
