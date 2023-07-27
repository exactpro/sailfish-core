/******************************************************************************
 * Copyright 2009-2023 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.embedded.statistics.entities;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.statistics.LocalDateTimeDBConverter;

@Entity
@Table(name="sttestcaseruns")
@SequenceGenerator(name="sttestcaseruns_generator", sequenceName="sttestcaseruns_sequence")
public class TestCaseRun {

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO, generator="sttestcaseruns_generator")
	private Long id;

    @Type(type = "com.exactpro.sf.storage.TruncatedString", parameters = @Parameter(name = "length", value = "255"))
	private String description;

    @Convert(converter = LocalDateTimeDBConverter.class)
    private LocalDateTime startTime;

    @Convert(converter = LocalDateTimeDBConverter.class)
    private LocalDateTime finishTime;

	private int status;

    @Type(type = "com.exactpro.sf.storage.TruncatedString", parameters = @Parameter(name = "length", value = "255"))
    private String failReason;

	private long rank;

	private String reportFile;

    @Type(type = "com.exactpro.sf.storage.TruncatedString", parameters = @Parameter(name = "length", value = "1024"))
	private String comment;

	private String fixRevision;

	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "matrix_run_id", nullable = false)
	private MatrixRun matrixRun;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "tc_id", nullable = false)
	private TestCase testCase;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "status_id", nullable = true)
	private TestCaseRunStatus runStatus;

    @OneToMany(mappedBy = "testCaseRun", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TestCaseRunTag> tags = new HashSet<>();

	private int hash;

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "tcRun")
	private Set<ActionRun> actionRuns;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	public StatusType getStatus() {
		return StatusType.getStatusType(status);
	}

	public void setStatus(StatusType status) {
		this.status = status.getId();
	}

	public MatrixRun getMatrixRun() {
		return matrixRun;
	}

	public void setMatrixRun(MatrixRun matrixRun) {
		this.matrixRun = matrixRun;
	}

	public TestCase getTestCase() {
		return testCase;
	}

	public void setTestCase(TestCase testCase) {
		this.testCase = testCase;
	}

	public String getFailReason() {
		return failReason;
	}

	public void setFailReason(String failReason) {
		this.failReason = failReason;
	}

	public long getRank() {
		return rank;
	}

	public void setRank(long rank) {
		this.rank = rank;
	}

	public String getReportFile() {
		return reportFile;
	}

	public void setReportFile(String reportFile) {
		this.reportFile = reportFile;
	}

	public TestCaseRunStatus getRunStatus() {
		return runStatus;
	}

	public void setRunStatus(TestCaseRunStatus status) {
		this.runStatus = status;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getFixRevision() {
		return fixRevision;
	}

	public void setFixRevision(String fixRevision) {
		this.fixRevision = fixRevision;
	}

    public int getHash() {
        return hash;
    }

    public void setHash(int hash) {
        this.hash = hash;
    }

    public boolean addTags(List<Tag> tags, Boolean custom) {
	    boolean result = false;
        for (Tag tag : tags) {
            TestCaseRunTag testCaseRunTag = new TestCaseRunTag(this, tag);
            testCaseRunTag.setCustom(custom);
            result |= this.tags.add(testCaseRunTag);
        }
        return result;
    }

    public boolean addTags(List<Tag> tags) {
        return addTags(tags, false);
    }

    public boolean removeTags(List<Tag> tags) {
        boolean result = false;
        for (Tag tag : tags) {
            TestCaseRunTag expected = new TestCaseRunTag(this, tag);
            result |= this.tags.remove(expected);
        }
        return result;
    }

    public Set<TestCaseRunTag> getTags() {
        return tags;
    }

    public void setTags(Set<TestCaseRunTag> tags) {
        this.tags = tags;
    }

	public void setActionRuns(Set<ActionRun> actionRuns) {
		this.actionRuns = actionRuns;
	}

	public Set<ActionRun> getActionRuns() {
		return actionRuns;
	}

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        TestCaseRun that = (TestCaseRun) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
