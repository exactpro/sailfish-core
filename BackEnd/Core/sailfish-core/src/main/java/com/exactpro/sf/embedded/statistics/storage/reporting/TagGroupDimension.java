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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.exactpro.sf.embedded.statistics.entities.Tag;
import com.exactpro.sf.embedded.statistics.entities.TagGroup;

@SuppressWarnings("serial")
@XmlRootElement(name = "dimension")
@XmlAccessorType(XmlAccessType.FIELD)
public class TagGroupDimension implements Serializable {

    private long id;

    private String name;

    private List<TagGroupDimension> possibleSubTags;
	private List<TagGroupDimension> selectedSubTags;
	
	public TagGroupDimension() {
			
		}
	
	public TagGroupDimension(long id, String name, List<TagGroupDimension> subTags) {
		this.id = id;
		this.name = name;
		this.possibleSubTags = subTags;
		if (subTags != null) {
		    this.selectedSubTags = new ArrayList<>(this.possibleSubTags);
		}
	}	
	
	public static TagGroupDimension fromTag(Tag tag) {
		
		return new TagGroupDimension(tag.getId(), tag.getName(), null);
		
	}
	
	public static TagGroupDimension fromGroup(TagGroup group) {
		
	    List<TagGroupDimension> list = new ArrayList<>();
	    
	    for (Tag tag : group.getTags()) {
            list.add(fromTag(tag));
        }
	    
		return new TagGroupDimension(group.getId(), group.getName(), Collections.unmodifiableList(list));
		
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public boolean isTag() {
		return possibleSubTags == null;
	}

    public List<TagGroupDimension> getPossibleSubTags() {
        return possibleSubTags;
    }
    
    public List<TagGroupDimension> getSelectedSubTags() {
        return selectedSubTags;
    }
    
    public void setSelectedSubTags(List<TagGroupDimension> selectedSubTags) {
        this.selectedSubTags = selectedSubTags;
    }
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result + (isTag() ? 1231 : 1237);
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
		TagGroupDimension other = (TagGroupDimension) obj;
		if (id != other.id)
			return false;
		if (isTag() != other.isTag())
			return false;
		return true;
	}
	
}
