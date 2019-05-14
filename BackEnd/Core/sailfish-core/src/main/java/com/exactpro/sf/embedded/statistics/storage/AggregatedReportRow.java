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

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import com.exactpro.sf.embedded.statistics.entities.SfInstance;
import com.exactpro.sf.embedded.statistics.entities.Tag;
import com.exactpro.sf.embedded.statistics.storage.reporting.KnownBugCategoryRow;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.util.DateTimeUtility;
import com.google.common.collect.Lists;

@SuppressWarnings("serial")
public class AggregatedReportRow extends CommonReportRow {

    private static final List<String> TYPES = Lists.newArrayList("id", "Name", "Test Case Id", "Test Case Name", "Description", "Status",
                                                                 "Failure Reason",
                                                                 "Matrix Failure Reason", "Failed Actions", "Start Time", "Finish Time", "Matrix Start Time", "Matrix Finish Time", "Execution Time",
                                                                 "User Name", "SF", "Environment", "Services Used", "User Status", "User Comments", "Report Folder", "Report File", "Host", "Port",
                                                                 "Matrix Row", "Matrix Name", "Test Case Run Id", "Matrix Run Id", "Tags", "Passed", "Failed", "CondPassed", "Comment", "Fix Revision", "Hash",
                                                                 "Tagged Actions", "Message Type", "Action Row", "Reproduced Known Bugs Count", "Non-reproduced Known Bugs Count", "Known Bugs", "Test Case Number");

    private final Map<String, Object> fields = new HashMap<>();
    private final List<AggregatedReportRow> testCaseRows = new ArrayList<>();

	public AggregatedReportRow() {
        for (String type : TYPES) {
            fields.put(type, null);
        }
        fields.put("User Comments", new TestCaseRunComments());
        fields.put("SfInstance", new SfInstance());
	}

	public AggregatedReportRow(Long sfId, String name, String description,
			int status, String failReason,
            Timestamp startTime, Timestamp finishTime, String userName, String host,
			String reportFolder, String reportFile) {

        fields.put("id", sfId);
        fields.put("Name", name);
        fields.put("Description", description);
        fields.put("Status", StatusType.getStatusType(status));
        fields.put("Failure Reason", failReason);
		//this.faildActions = faildActions;
        fields.put("Start Time", DateTimeUtility.toLocalDateTime(startTime));
        fields.put("Finish Time", DateTimeUtility.toLocalDateTime(finishTime));
        fields.put("User Name", userName);
        SfInstance sfInstance = new SfInstance();
        sfInstance.setHost(host);
        fields.put("SfInstance", sfInstance);
		//this.servicesUsed = servicesUsed;

        fields.put("Report Folder", reportFolder);
        fields.put("Report File", reportFile);

	}

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        if (!TYPES.contains(key)) {
            throw new RuntimeException("There is no field in AggregatedReportRow: " + key);
        }
        return ObjectUtils.defaultIfNull((T)fields.get(key), defaultValue);
    }

	public boolean isUnknownTc() {
        return "_unknown_tc_".equals(getTestCaseId());
	}

	public String getRowKey() {

        return Long.toString(getMatrixRunId() != null ? getMatrixRunId() : -1l)
                + (getTestCaseRunId() != null ? getTestCaseRunId() : -1l);

	}

	/*
	public AggregatedReportRow(Object sfId, Object name, Object description,
			Object status, Object failReason, Object faildActions,
			Object startTime, Object finishTime, Object userName, Object host,
			Object servicesUsed) {

		logger.info("{}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}",
		sfId != null ? sfId.getClass() : "null",
		name != null ? name.getClass() : "null",
		description != null ? description.getClass() : "null",
		status != null ? status.getClass() : "null",
		failReason != null ? failReason.getClass() : "null",
		faildActions != null ? faildActions.getClass() : "null",
		startTime != null ? startTime.getClass() : "null",
		finishTime != null ? finishTime.getClass() : "null",
		userName != null ? userName.getClass() : "null",
		host != null ? host.getClass() : "null",
		servicesUsed != null ? servicesUsed.getClass() : "null");

	}*/

	public long getSfId() {
        return get("id", 0l);
	}

	public void setSfId(long sfId) {
        fields.put("id", sfId);
	}

	public String getDescription() {
        return (String)fields.get("Description");
	}

	public void setDescription(String description) {
	    //TODO: Remove escaping after release 3.1, because escaping is executed on save data
        fields.put("Description", StringEscapeUtils.escapeEcmaScript(description));
	}

	public StatusType getStatus() {
        return (StatusType)fields.get("Status");
	}

	public void setStatus(StatusType status) {
        fields.put("Status", status);
	}

	public String getFailReason() {
        return (String)fields.get("Failure Reason");
	}

	public void setFailReason(String failReason) {
        fields.put("Failure Reason", failReason);
	}

	public String getMatrixFailReason() {
        return (String)fields.get("Matrix Failure Reason");
    }

    public void setMatrixFailReason(String matrixFailReason) {
        fields.put("Matrix Failure Reason", matrixFailReason);
    }

    public String getFailedActions() {
        return (String)fields.get("Failed Actions");
	}

	public void setFailedActions(String faildActions) {
        fields.put("Failed Actions", faildActions);
	}

	public LocalDateTime getStartTime() {
        return (LocalDateTime)fields.get("Start Time");
	}

	public void setStartTime(LocalDateTime startTime) {
        fields.put("Start Time", startTime);
	}

	public LocalDateTime getFinishTime() {
        return (LocalDateTime)fields.get("Finish Time");
	}

	public void setFinishTime(LocalDateTime finishTime) {
        fields.put("Finish Time", finishTime);
	}

    public Long getExecutionTime() {
        return isMatrixRow()
                ? getExecutionMatrixTime()
                : getExecutionCaseTime();
    }

    public Long getExecutionMatrixTime() {
        return DateTimeUtility.getMillisecond(getMatrixFinishTime()) - DateTimeUtility.getMillisecond(getMatrixStartTime());
    }

    public Long getExecutionCaseTime() {
        return DateTimeUtility.getMillisecond(getFinishTime())  - DateTimeUtility.getMillisecond(getStartTime());
    }

	public String getUserName() {
        return (String)fields.get("User Name");
	}

	public void setUserName(String userName) {
        fields.put("User Name", userName);
	}

	public String getHost() {
        return getSfInstance().getHost();
	}

	public void setHost(String host) {
        getSfInstance().setHost(host);
	}

	public String getServicesUsed() {
        return (String)fields.get("Services Used");
	}

	public void setServicesUsed(String servicesUsed) {
        fields.put("Services Used", servicesUsed);
	}

	public String getMatrixName() {
        return (String)fields.get("Matrix Name");
	}

	public void setMatrixName(String matrixName) {
        fields.put("Matrix Name", matrixName);
	}

	public String getTestCaseName() {
        return (String)fields.get("Test Case Name");
	}

	public void setTestCaseName(String testCaseName) {
        fields.put("Test Case Name", testCaseName);
	}

    public Long getTestCaseNumber() {
        return (Long)fields.get("Test Case Number");
    }

    public void setTestCaseNumber(Long testCaseNumber) {
        fields.put("Test Case Number", testCaseNumber);
    }

	public LocalDateTime getMatrixStartTime() {
        return (LocalDateTime)fields.get("Matrix Start Time");
	}

	public void setMatrixStartTime(LocalDateTime matrixStartTime) {
        fields.put("Matrix Start Time", matrixStartTime);
	}

	public LocalDateTime getMatrixFinishTime() {
        return (LocalDateTime)fields.get("Matrix Finish Time");
	}

	public void setMatrixFinishTime(LocalDateTime matrixFinishTime) {
        fields.put("Matrix Finish Time", matrixFinishTime);
	}

	public Long getTestCaseRunId() {
        return (Long)fields.get("Test Case Run Id");
	}

	public void setTestCaseRunId(Long testCaseRunId) {
        fields.put("Test Case Run Id", testCaseRunId);
	}

	public String getEnvironmentName() {
        return (String)fields.get("Environment");
	}

	public void setEnvironmentName(String environmentName) {
        fields.put("Environment", environmentName);
	}

	public boolean isMatrixRow() {
        return get("Matrix Row", false);
	}

	public void setMatrixRow(boolean matrixRow) {
        fields.put("Matrix Row", matrixRow);
	}

	public int getPort() {
        return getSfInstance().getPort();
	}

	public void setPort(int port) {
        getSfInstance().setPort(port);
	}

	public String getSfName() {
        return getSfInstance().getName();
	}

	public void setSfName(String sfName) {
        getSfInstance().setName(sfName);
	}

	public Long getMatrixRunId() {
        return (Long)fields.get("Matrix Run Id");
	}

	public void setMatrixRunId(Long matrixRunId) {
        fields.put("Matrix Run Id", matrixRunId);
	}

	public String getTestCaseId() {
        return (String)fields.get("Test Case Id");
	}

	public void setTestCaseId(String testCaseId) {
        fields.put("Test Case Id", testCaseId);
	}

    @SuppressWarnings("unchecked")
    public List<Tag> getTags() {
        return (List<Tag>)fields.get("Tags");
	}

    public String getTagsString() {
        List<Tag> tags = getTags();
        if(tags != null && !tags.isEmpty()) {
            return tags.stream().map(Tag::getName).collect(Collectors.joining(","));
        }
        return "";
    }

	public void setTags(List<Tag> tags) {
        fields.put("Tags", tags);
	}

	public String getReportFolder() {
        return (String)fields.get("Report Folder");
	}

	public void setReportFolder(String reportFolder) {
        fields.put("Report Folder", reportFolder);
	}

	public String getReportFile() {
        return (String)fields.get("Report File");
	}

	public void setReportFile(String reportFile) {
        fields.put("Report File", reportFile);
	}

	public TestCaseRunComments getUserComments() {
        return (TestCaseRunComments)fields.get("User Comments");
	}

	public void setUserComments(TestCaseRunComments userComments) {
        fields.put("User Comments", userComments);
	}

	public long getPassedCount() {
        return get("Passed", 0l);
	}

	public void setPassedCount(long passedCount) {
        fields.put("Passed", passedCount);
	}

	public long getFailedCount() {
        return get("Failed", 0l);
	}

	public void setFailedCount(long failedCount) {
        fields.put("Failed", failedCount);
	}

	public long getConditionallyPassedCount() {
        return get("CondPassed", 0l);
    }

    public void setConditionallyPassedCount(long conditionallyPassedCount) {
        fields.put("CondPassed", conditionallyPassedCount);
    }

    public int getHash() {
        return get("Hash", 0);
    }

    public void setHash(int hash) {
        fields.put("Hash", hash);
    }

    public String getTaggedActions() {
        return isMatrixRow() ? StringUtils.EMPTY : "View";
    }

    public boolean isFailed() {
        return getMatrixFailReason() != null;
    }

    public String getMessageType() {
        return (String)fields.get("Message Type");
    }

    public void setMessageType(String messageType) {
        fields.put("Message Type", messageType);
    }

    //TODO merge ActionName, TestCaseName, MatrixName >> Name
    public String getActionName() {
        return (String)fields.get("Action Name");
    }

    public void setActionName(String matrixName) {
        fields.put("Action Name", matrixName);
    }

    public boolean isActionRow() {
        return get("Action Row", false);
    }

    public void setActionRow(boolean matrixRow) {
        fields.put("Action Row", matrixRow);
    }

    public SfInstance getSfCurrentInstance() {
        return (SfInstance)fields.get("sfCurrentInstance");
    }

    public void setSfCurrentInstance(SfInstance sfCurrent) {
        fields.put("sfCurrentInstance", sfCurrent);
    }

    public SfInstance getSfInstance() {
        return (SfInstance)fields.get("SfInstance");
    }

    public void setSfInstance(SfInstance instance) {
        fields.put("SfInstance", instance);
    }

    public long getReproducedKnownBugsCount() {
        return get("Reproduced Known Bugs Count", 0L);
    }

    public void setReproducedKnownBugsCount(long count) {
        fields.put("Reproduced Known Bugs Count", count);
    }

    public long getNonReproducedKnownBugsCount() {
        return get("Non-reproduced Known Bugs Count", 0L);
    }

    public void setNonReproducedKnownBugsCount(long count) {
        fields.put("Non-reproduced Known Bugs Count", count);
    }

    public List<KnownBugCategoryRow> getCategorisedKnownBugs() {
        return get("Known Bugs", null);
    }

    public void setCategorisedKnownBugs(List<KnownBugCategoryRow> categorisedKnownBugs) {
        fields.put("Known Bugs", categorisedKnownBugs);
    }

    public List<AggregatedReportRow> getTestCaseRows() {
        return testCaseRows;
    }
}
