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
package com.exactpro.sf.embedded.statistics.entities;

import com.exactpro.sf.statistics.LocalDateTimeDBConverter;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import java.time.LocalDateTime;

import javax.persistence.*;
import java.util.Set;

@Entity
@Table(name="stmatrixruns")
@SequenceGenerator(name="stmatrixruns_generator", sequenceName="stmatrixruns_sequence")
public class MatrixRun {
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO, generator="stmatrixruns_generator")
	private Long id;

    @Convert(converter = LocalDateTimeDBConverter.class)
    private LocalDateTime startTime;

    @Convert(converter = LocalDateTimeDBConverter.class)
    private LocalDateTime finishTime;
	
	private long sfRunId;

    @Type(type = "com.exactpro.sf.storage.TruncatedString", parameters = {@Parameter(name = "length", value = "16384")})
	private String failReason;
	
	private String reportFolder;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "sf_id", nullable = false)
	private SfInstance sfInstance;

	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sf_current_id")
    private SfInstance sfCurrentInstance;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "matrix_id", nullable = false)
	private Matrix matrix;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "environment_id", nullable = false)
	private Environment environment;
	
	@ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinTable(name = "stmrtags", joinColumns = { 
			@JoinColumn(name = "mr_id", nullable = false, updatable = true) }, 
			inverseJoinColumns = { @JoinColumn(name = "tag_id", 
					nullable = false, updatable = true) })
	private Set<Tag> tags; 

	public Set<Tag> getTags() {
		return tags;
	}

	public void setTags(Set<Tag> tags) {
		this.tags = tags;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public SfInstance getSfInstance() {
		return sfInstance;
	}

	public void setSfInstance(SfInstance sfInstance) {
		this.sfInstance = sfInstance;
	}

	public Matrix getMatrix() {
		return matrix;
	}

	public void setMatrix(Matrix matrix) {
		this.matrix = matrix;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Environment getEnvironment() {
		return environment;
	}

	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	public long getSfRunId() {
		return sfRunId;
	}

	public void setSfRunId(long sfRunId) {
		this.sfRunId = sfRunId;
	}

	public String getReportFolder() {
		return reportFolder;
	}

	public void setReportFolder(String reportFolder) {
		this.reportFolder = reportFolder;
	}

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String initException) {
        this.failReason = initException;
    }

    public SfInstance getSfCurrentInstance() {
        return sfCurrentInstance;
    }

    public void setSfCurrentInstance(SfInstance sfCurrentInstance) {
        this.sfCurrentInstance = sfCurrentInstance;
    }
	
}
