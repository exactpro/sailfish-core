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

import java.util.Date;
import java.util.List;

import com.exactpro.sf.embedded.statistics.entities.SfInstance;
import com.exactpro.sf.embedded.statistics.entities.Tag;
import com.exactpro.sf.util.DateTimeUtility;
import java.time.LocalDateTime;

public class AggregateReportParameters {

    private LocalDateTime from;

    private LocalDateTime to;

	private String matrixNamePattern;

	private List<SfInstance> sfInstances;

    private List<Long> matrixRunIds;

	private Long testCaseId;

    private List<Long> testCaseRunIds;

	private Long testCaseRunId;

	private TestCasesDisplayMode tcDisplayMode;

	private long limit;

	private long secondLimit;

	private List<Tag> tags;

	private boolean allTags; // All or at least one

	private boolean emptyCommentOnly = false;

	private String sortBy;

	private boolean sortAsc;

    private boolean includeExecutedInFromToRange;

	public LocalDateTime getFrom() {
		return from;
	}

	public void setFrom(LocalDateTime from) {
		this.from = from;
	}

    public void setFrom(Date from) {
        this.from = DateTimeUtility.toLocalDateTime(from);
    }

	public LocalDateTime getTo() {
		return to;
	}

	public void setTo(LocalDateTime to) {
		this.to = to;
	}

    public void setTo(Date to) {
        this.to = DateTimeUtility.toLocalDateTime(to);
    }

	public List<SfInstance> getSfInstances() {
		return sfInstances;
	}

	public void setSfInstances(List<SfInstance> sfInstances) {
		this.sfInstances = sfInstances;
	}

    public List<Long> getMatrixRunIds() {
        return matrixRunIds;
    }

    public void setMatrixRunIds(List<Long> matrixRunIds) {
        this.matrixRunIds = matrixRunIds;
    }

    public List<Long> getTestCaseRunIds() {
        return testCaseRunIds;
    }

    public void setTestCaseRunIds(List<Long> testCaseRunIds) {
        this.testCaseRunIds = testCaseRunIds;
    }

    public Long getTestCaseId() {
		return testCaseId;
	}

	public void setTestCaseId(Long testCaseId) {
		this.testCaseId = testCaseId;
	}

	public TestCasesDisplayMode getTcDisplayMode() {
		return tcDisplayMode;
	}

	public void setTcDisplayMode(TestCasesDisplayMode tcDisplayMode) {
		this.tcDisplayMode = tcDisplayMode;
	}

	public Long getTestCaseRunId() {
		return testCaseRunId;
	}

	public void setTestCaseRunId(Long testCaseRunId) {
		this.testCaseRunId = testCaseRunId;
	}

	public long getLimit() {
		return limit;
	}

	public void setLimit(long limit) {
		this.limit = limit;
	}

	public long getSecondLimit() {
		return secondLimit;
	}

	public void setSecondLimit(long secondLimit) {
		this.secondLimit = secondLimit;
	}

	public List<Tag> getTags() {
		return tags;
	}

	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}

	public boolean isAllTags() {
		return allTags;
	}

	public void setAllTags(boolean allTags) {
		this.allTags = allTags;
	}

	public boolean isEmptyCommentOnly() {
		return emptyCommentOnly;
	}

	public void setEmptyCommentOnly(boolean emptyCommentOnly) {
		this.emptyCommentOnly = emptyCommentOnly;
	}

	public String getMatrixNamePattern() {
		return matrixNamePattern;
	}

	public void setMatrixNamePattern(String matrixNamePattern) {
		this.matrixNamePattern = matrixNamePattern;
	}

	public String getSortBy() {
		return sortBy;
	}

	public void setSortBy(String sortBy) {
		this.sortBy = sortBy;
	}

	public boolean isSortAsc() {
		return sortAsc;
	}

	public void setSortAsc(boolean sortAsc) {
		this.sortAsc = sortAsc;
	}

    public boolean isIncludeExecutedInFromToRange() {
        return includeExecutedInFromToRange;
    }

    public void setIncludeExecutedInFromToRange(boolean includeExecutedInFromToRange) {
        this.includeExecutedInFromToRange = includeExecutedInFromToRange;
    }
}
