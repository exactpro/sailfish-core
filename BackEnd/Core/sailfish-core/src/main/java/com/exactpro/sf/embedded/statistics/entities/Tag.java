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

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

@SuppressWarnings("serial")
@Entity
@Table(name="sttags",
uniqueConstraints = {@UniqueConstraint(columnNames={"name"})})
@SequenceGenerator(name="sttags_generator", sequenceName="sttags_sequence")
public class Tag implements Serializable {
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO, generator="sttags_generator")
	private Long id;
	
	private String name;

    @OneToMany(mappedBy = "tag", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TestCaseRunTag> testCaseRuns = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "tags")
    private Set<MatrixRun> matrixRuns;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "group_id", nullable = true)
	private TagGroup group;

    @Transient
    private boolean forAllTestCaseRuns;

    @Transient
    private Boolean custom;

    public Set<MatrixRun> getMatrixRuns() {
        return matrixRuns;
    }

    public void setMatrixRuns(Set<MatrixRun> matrixRuns) {
        this.matrixRuns = matrixRuns;
    }

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

    public Set<TestCaseRunTag> getTestCaseRuns() {
        return testCaseRuns;
    }

    public void setTestCaseRuns(Set<TestCaseRunTag> testCaseRuns) {
        this.testCaseRuns = testCaseRuns;
    }

    public boolean isForAllTestCaseRuns() {
        return forAllTestCaseRuns;
    }

    public Boolean getCustom() {
        return custom;
    }

    public void setCustom(Boolean custom) {
        this.custom = custom;
    }

    public void setForAllTestCaseRuns(boolean forAllTestCaseRuns) {
        this.forAllTestCaseRuns = forAllTestCaseRuns;
    }

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Tag other = (Tag) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	public TagGroup getGroup() {
		return group;
	}

	public void setGroup(TagGroup group) {
		this.group = group;
	}

	@Override
	public String toString() {
		return "Tag [id=" + id + ", name=" + name + "]";
	}
	
}
