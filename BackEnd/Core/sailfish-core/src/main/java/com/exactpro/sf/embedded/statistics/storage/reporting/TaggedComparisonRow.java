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
package com.exactpro.sf.embedded.statistics.storage.reporting;

import java.io.Serializable;
import java.util.Map;

import com.exactpro.sf.scriptrunner.StatusType;
import java.time.LocalDateTime;

@SuppressWarnings("serial")
public class TaggedComparisonRow implements Serializable {

	private String testCaseId;

	// First set
    private final TaggedComparisonSet firstSet = new TaggedComparisonSet();

	// Second set
    private final TaggedComparisonSet secondSet = new TaggedComparisonSet();

    public TaggedComparisonSet getFirstSet() {
        return firstSet;
    }

    public TaggedComparisonSet getSecondSet() {
        return secondSet;
    }

    public String getTestCaseId() {
		return testCaseId;
	}

	public void setTestCaseId(String testCaseId) {
		this.testCaseId = testCaseId;
	}

	public Long getFirstTestCaseRunId() {
		return firstSet.getTestCaseRunId();
	}

	public void setFirstTestCaseRunId(Long firstTestCaseRunId) {
		this.firstSet.setTestCaseRunId(firstTestCaseRunId);
	}

	public Long getFirstMatrixRunId() {
		return firstSet.getMatrixRunId();
	}

	public void setFirstMatrixRunId(Long firstMatrixRunId) {
		this.firstSet.setMatrixRunId(firstMatrixRunId);
	}

	public void setFirstStatus(StatusType firstStatus) {
		this.firstSet.setStatus(firstStatus);
	}

	public String getFirstMatrixName() {
		return firstSet.getMatrixName();
	}

	public void setFirstMatrixName(String firstMatrixName) {
		this.firstSet.setMatrixName(firstMatrixName);
	}

	public String getFirstFailReason() {
		return firstSet.getFailReason();
	}

	public void setFirstFailReason(String firstFailReason) {
		this.firstSet.setFailReason(firstFailReason);
	}

	public Map<Long, String> getFirstFailedActions() {
	    return firstSet.getFailedActions();
	}

	public void setFirstFailedActions(Map<Long, String> firstFailedActions) {
	    this.firstSet.setFailedActions(firstFailedActions);
	}

	public String getFirstUserComment() {
		return firstSet.getUserComment();
	}

	public void setFirstUserComment(String firstUserComment) {
		this.firstSet.setUserComment(firstUserComment);
	}

	public String getFirstUserStatus() {
		return firstSet.getUserStatus();
	}

	public void setFirstUserStatus(String firstUserStatus) {
		this.firstSet.setUserStatus(firstUserStatus);
	}

	public LocalDateTime getFirstStartTime() {
		return firstSet.getStartTime();
	}

	public void setFirstStartTime(LocalDateTime firstStartTime) {
		this.firstSet.setStartTime(firstStartTime);
	}

	public Long getSecondTestCaseRunId() {
		return secondSet.getTestCaseRunId();
	}

	public void setSecondTestCaseRunId(Long secondTestCaseRunId) {
		this.secondSet.setTestCaseRunId(secondTestCaseRunId);
	}

	public Long getSecondMatrixRunId() {
		return secondSet.getMatrixRunId();
	}

	public void setSecondMatrixRunId(Long secondMatrixRunId) {
		this.secondSet.setMatrixRunId(secondMatrixRunId);
	}

	public void setSecondStatus(StatusType secondStatus) {
		this.secondSet.setStatus(secondStatus);
	}

	public String getSecondMatrixName() {
		return secondSet.getMatrixName();
	}

	public void setSecondMatrixName(String secondMatrixName) {
		this.secondSet.setMatrixName(secondMatrixName);
	}

	public String getSecondFailReason() {
		return secondSet.getFailReason();
	}

	public void setSecondFailReason(String secondFailReason) {
		this.secondSet.setFailReason(secondFailReason);
	}

	public Map<Long, String> getSecondFailedActions() {
	    return secondSet.getFailedActions();
	}

	public void setSecondFailedActions(Map<Long, String> secondFailedActions) {
	    this.secondSet.setFailedActions(secondFailedActions);
	}

	public String getSecondUserComment() {
		return secondSet.getUserComment();
	}

	public void setSecondUserComment(String secondUserComment) {
		this.secondSet.setUserComment(secondUserComment);
	}

	public String getSecondUserStatus() {
		return secondSet.getUserStatus();
	}

	public void setSecondUserStatus(String secondUserStatus) {
		this.secondSet.setUserStatus(secondUserStatus);
	}

	public LocalDateTime getSecondStartTime() {
		return secondSet.getStartTime();
	}

	public void setSecondStartTime(LocalDateTime secondStartTime) {
		this.secondSet.setStartTime(secondStartTime);
	}

	public Long getFirstTestCaseId() {
		return firstSet.getTestCaseId();
	}

	public void setFirstTestCaseId(Long firstTestCaseId) {
		this.firstSet.setTestCaseId(firstTestCaseId);
	}

	public LocalDateTime getFirstFinishTime() {
		return firstSet.getFinishTime();
	}

	public void setFirstFinishTime(LocalDateTime firstFinishTime) {
		this.firstSet.setFinishTime(firstFinishTime);
	}

	public String getFirstRawTags() {
		return firstSet.getRawTags();
	}

	public void setFirstRawTags(String firstRawTags) {
		this.firstSet.setRawTags(firstRawTags);
	}

	public Integer getFirstRawHash() {
	    return firstSet.getRawHash();
	}

	public void setFirstRawHash(Integer firstRawHash) {
	    this.firstSet.setRawHash(firstRawHash);
	}

	public Long getSecondTestCaseId() {
		return secondSet.getTestCaseId();
	}

	public void setSecondTestCaseId(Long secondTestCaseId) {
		this.secondSet.setTestCaseId(secondTestCaseId);
	}

	public LocalDateTime getSecondFinishTime() {
		return secondSet.getFinishTime();
	}

	public void setSecondFinishTime(LocalDateTime secondFinishTime) {
		this.secondSet.setFinishTime(secondFinishTime);
	}

	public String getSecondRawTags() {
		return secondSet.getRawTags();
	}

	public void setSecondRawTags(String secondRawTags) {
		this.secondSet.setRawTags(secondRawTags);
	}

	public Integer getSecondRawHash() {
	    return secondSet.getRawHash();
	}

	public void setSecondRawHash(Integer secondRawHash) {
	    this.secondSet.setRawHash(secondRawHash);
	}

	public StatusType getFirstStatus() {
		return firstSet.getStatus();
	}

	public StatusType getSecondStatus() {
		return secondSet.getStatus();
	}

    public String getFirstDescription() {
        return firstSet.getDescription();
    }

    public void setFirstDescription(String firstDescription) {
        this.firstSet.setDescription(firstDescription);
    }

    public String getSecondDescription() {
        return secondSet.getDescription();
    }

    public void setSecondDescription(String secondDescription) {
        this.secondSet.setDescription(secondDescription);
    }

    public static class TaggedComparisonSet {

        private Long testCaseId;

        private Long testCaseRunId;

        private Long matrixRunId;

        private StatusType status;

        private String matrixName;

        private String failReason;

        private Map<Long, String> failedActions;

        private String userComment;

        private String userStatus;

        private LocalDateTime startTime;

        private LocalDateTime finishTime;

        private String rawTags;

        private Integer rawHash;

        private String description;

        public Long getTestCaseId() {
            return testCaseId;
        }

        public void setTestCaseId(Long testCaseId) {
            this.testCaseId = testCaseId;
        }

        public Long getTestCaseRunId() {
            return testCaseRunId;
        }

        public void setTestCaseRunId(Long testCaseRunId) {
            this.testCaseRunId = testCaseRunId;
        }

        public Long getMatrixRunId() {
            return matrixRunId;
        }

        public void setMatrixRunId(Long matrixRunId) {
            this.matrixRunId = matrixRunId;
        }

        public StatusType getStatus() {
            return status;
        }

        public void setStatus(StatusType status) {
            this.status = status;
        }

        public String getMatrixName() {
            return matrixName;
        }

        public void setMatrixName(String matrixName) {
            this.matrixName = matrixName;
        }

        public String getFailReason() {
            return failReason;
        }

        public void setFailReason(String failReason) {
            this.failReason = failReason;
        }

        public Map<Long, String> getFailedActions() {
            return failedActions;
        }

        public void setFailedActions(Map<Long, String> failedActions) {
            this.failedActions = failedActions;
        }

        public String getUserComment() {
            return userComment;
        }

        public void setUserComment(String userComment) {
            this.userComment = userComment;
        }

        public String getUserStatus() {
            return userStatus;
        }

        public void setUserStatus(String userStatus) {
            this.userStatus = userStatus;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        public LocalDateTime getFinishTime() {
            return finishTime;
        }

        public void setFinishTime(LocalDateTime finishTime) {
            this.finishTime = finishTime;
        }

        public String getRawTags() {
            return rawTags;
        }

        public void setRawTags(String rawTags) {
            this.rawTags = rawTags;
        }

        public Integer getRawHash() {
            return rawHash;
        }

        public void setRawHash(Integer rawHash) {
            this.rawHash = rawHash;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}