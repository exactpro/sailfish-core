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
package com.exactpro.sf.scriptrunner.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.scriptrunner.IReportStats;
import com.exactpro.sf.scriptrunner.StatusType;

public class ReportStats implements IReportStats {

	private static final Logger logger = LoggerFactory.getLogger(ReportStats.class);
	
	private int actionPassed;
	private int actionFailed;
	private int actionSkipped;
	private int actionConditionnalyPassed;
	private int actionConditionnalyFailed;
	private int verificationPassed;
	private int verificationFailed;
	private int verificationSkipped;
	private int verificationConditionnalyPassed;
	private int verificationConditionnalyFailed;
	private StatusType testCaseStatus = StatusType.NA;

	@Override
	public void updateActions(StatusType status) {
		switch (status) {
		case CONDITIONALLY_FAILED:
			incrementActionConditionnalyFailed();
			break;
		case CONDITIONALLY_PASSED:
			incrementActionConditionnalyPassed();
			break;
		case FAILED:
			incrementActionFailed();
			break;
		case PASSED:
			incrementActionPassed();
			break;
		case SKIPPED:
			incrementActionSkipped();
			break;
		case NA:
		default:
			break;
		}
		updateTestCaseStatus(status);
	}

	@Override
	public void updateVerifications(StatusType status) {
		switch (status) {
		case CONDITIONALLY_FAILED:
			incrementVerificationConditionnalyFailed();
			break;
		case CONDITIONALLY_PASSED:
			incrementVerificationConditionnalyPassed();
			break;
		case FAILED:
			incrementVerificationFailed();
			break;
		case PASSED:
			incrementVerificationPassed();
			break;
		case SKIPPED:
			incrementVerificationSkipped();
		case NA:
		default:
			break;
		}
		updateTestCaseStatus(status);
	}

	@Override
	public int getActionPassed() {
		return actionPassed;
	}

	public void incrementActionPassed() {
		this.actionPassed++;
	}

	@Override
	public int getActionFailed() {
		return actionFailed;
	}

	public void incrementActionFailed() {
		this.actionFailed++;
	}

	@Override
	public int getActionSkipped() {
		return actionSkipped;
	}

	public void incrementActionSkipped() {
		this.actionSkipped++;
	}

	@Override
	public int getActionConditionnalyPassed() {
		return actionConditionnalyPassed;
	}

	public void incrementActionConditionnalyPassed() {
		this.actionConditionnalyPassed++;
	}

	@Override
	public int getActionConditionnalyFailed() {
		return actionConditionnalyFailed;
	}

	public void incrementActionConditionnalyFailed() {
		this.actionConditionnalyFailed++;
	}

	@Override
	public int getVerificationPassed() {
		return verificationPassed;
	}

	public void incrementVerificationPassed() {
		this.verificationPassed++;
	}

	@Override
	public int getVerificationFailed() {
		return verificationFailed;
	}

	public void incrementVerificationFailed() {
		this.verificationFailed++;
	}

	@Override
	public int getVerificationSkipped() {
		return verificationSkipped;
	}

	public void incrementVerificationSkipped() {
		this.verificationSkipped++;
	}

	@Override
	public int getVerificationConditionnalyPassed() {
		return verificationConditionnalyPassed;
	}

	public void incrementVerificationConditionnalyPassed() {
		this.verificationConditionnalyPassed++;
	}

	@Override
	public int getVerificationConditionnalyFailed() {
		return verificationConditionnalyFailed;
	}

	public void incrementVerificationConditionnalyFailed() {
		this.verificationConditionnalyFailed++;
	}

	@Override
	public StatusType getTestCaseStatus() {
		return testCaseStatus;
	}

	@Override
	public void updateTestCaseStatus(StatusType status)
	{
		switch (status) {
		case PASSED:
			if (testCaseStatus == StatusType.NA) {
				logger.debug("TestCase Status: {} -> PASSED", testCaseStatus);
				testCaseStatus = StatusType.PASSED;
			}
			break;
		case CONDITIONALLY_PASSED:
			if (testCaseStatus == StatusType.NA || testCaseStatus == StatusType.PASSED) {
				logger.debug("TestCase Status: {} -> CONDITIONALLY_PASSED", testCaseStatus);
				testCaseStatus = StatusType.CONDITIONALLY_PASSED;
			}
			break;
		case CONDITIONALLY_FAILED:
			if (testCaseStatus != StatusType.FAILED) {
				logger.debug("TestCase Status: {} -> CONDITIONALLY_FAILED", testCaseStatus);
				testCaseStatus = StatusType.CONDITIONALLY_FAILED;
			}
			break;
		case FAILED:
			logger.debug("TestCase Status: {} -> FAILED", testCaseStatus);
			testCaseStatus = StatusType.FAILED;
			break;
		case SKIPPED:
			break;
		case NA:
			break;
		default:
			break;
		}
	}

}
