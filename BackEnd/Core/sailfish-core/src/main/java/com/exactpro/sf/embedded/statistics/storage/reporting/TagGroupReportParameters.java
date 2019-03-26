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

import com.exactpro.sf.embedded.statistics.entities.SfInstance;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class TagGroupReportParameters {
	
	private Set<Long> tagIds;
	
    private List<TagGroupDimension> dimensions;	
	
	private int loadForLevel;

	private int numberOfGroups;

    private Date from;

    private Date to;

    private List<SfInstance> selectedSfInstances;

	public Set<Long> getTagIds() {
        return tagIds;
    }

    public void setTags(Set<Long> tags) {
        this.tagIds = tags;
    }

    public List<TagGroupDimension> getDimensions() {
        return dimensions;
    }

    public void setDimensions(List<TagGroupDimension> dimensions) {
        this.dimensions = dimensions;
    }
    
    public int getLoadForLevel() {
		return loadForLevel;
	}

	public void setLoadForLevel(int loadForLevel) {
		this.loadForLevel = loadForLevel;
	}

    public int getNumberOfGroups() {
        return numberOfGroups;
    }

    public void setNumberOfGroups(int numberOfGroups) {
        this.numberOfGroups = numberOfGroups;
    }

    public Date getFrom() {
        return from;
    }

    public void setFrom(Date from) {
        this.from = from;
    }

    public Date getTo() {
        return to;
    }

    public void setTo(Date to) {
        this.to = to;
    }

    public List<SfInstance> getSelectedSfInstances() {
        return selectedSfInstances;
    }

    public void setSelectedSfInstances(List<SfInstance> selectedSfInstances) {
        this.selectedSfInstances = selectedSfInstances;
    }
}
