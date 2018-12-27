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
package com.exactpro.sf.embedded.statistics.storage;

import java.io.Serializable;

import java.time.LocalDateTime;

import com.exactpro.sf.scriptrunner.StatusType;

@SuppressWarnings("serial")
public class TestCaseHistoryRow implements Serializable {

	private String testCaseId;

	private String matrixName;

	private LocalDateTime started;

	private LocalDateTime finished;

	private long duration;

	private StatusType status;

	private String failReason;

	private String durationString;

	public TestCaseHistoryRow(String testCaseId, String matrixName,
                              LocalDateTime started, LocalDateTime finished, int status, String failReason) {
		super();
		this.testCaseId = testCaseId;
		this.matrixName = matrixName;
		this.started = started;
		this.finished = finished;
		this.status = StatusType.getStatusType(status);
		this.failReason = failReason;
	}

	public String getTestCaseId() {
		return testCaseId;
	}

	public void setTestCaseId(String testCaseId) {
		this.testCaseId = testCaseId;
	}

	public String getMatrixName() {
		return matrixName;
	}

	public void setMatrixName(String matrixName) {
		this.matrixName = matrixName;
	}

	public LocalDateTime getStarted() {
		return started;
	}

	public void setStarted(LocalDateTime started) {
		this.started = started;
	}

	public LocalDateTime getFinished() {
		return finished;
	}

	public void setFinished(LocalDateTime finished) {
		this.finished = finished;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public StatusType getStatus() {
		return status;
	}

	public void setStatus(StatusType status) {
		this.status = status;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TestCaseHistoryRow [testCaseId=");
		builder.append(testCaseId);
		builder.append(", matrixName=");
		builder.append(matrixName);
		builder.append(", started=");
		builder.append(started);
		builder.append(", finished=");
		builder.append(finished);
		builder.append(", duration=");
		builder.append(duration);
		builder.append(", status=");
		builder.append(status);
		builder.append(", failReason=");
		builder.append(failReason);
		builder.append("]");
		return builder.toString();
	}

	public String getFailReason() {
		return failReason;
	}

	public void setFailReason(String failReason) {
		this.failReason = failReason;
	}

	public String getDurationString() {
		return durationString;
	}

	public void setDurationString(String durationString) {
		this.durationString = durationString;
	}



}
