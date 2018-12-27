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
package com.exactpro.sf.testwebgui.statistics;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.embedded.statistics.entities.Tag;
import com.exactpro.sf.embedded.statistics.entities.TagGroup;
import com.exactpro.sf.testwebgui.BeanUtil;

@ManagedBean(name="tagsAdminBean")
@ViewScoped
@SuppressWarnings("serial")
public class TagsAdminBean implements Serializable {
	
	private static final Logger logger = LoggerFactory.getLogger(TagsAdminBean.class);
	
	private List<Tag> allTags; // without group
	
	private List<TagGroup> allGroups;
	
	private boolean editMode = false; // edit/add new 
	
	private Tag selectedTag = new Tag();
	
	private TagGroup selectedGroup = new TagGroup();
	
	public void refresh() {
		
		if(BeanUtil.getSfContext().getStatisticsService().isConnected()) {
		
			this.allTags = BeanUtil.getSfContext().getStatisticsService().getStorage().getTagsWithoutGroup();
			this.allGroups = BeanUtil.getSfContext().getStatisticsService().getStorage().getAllTagGroups();
			
			//Collections.sort(this.allTags, new TagNameComparator());
			//Collections.sort(this.allGroups, new GroupNameComparator());
		
		} else {
			
			this.allTags = null;
			this.allGroups = null;
			
		}
		
	}
	
	@PostConstruct
	public void init() {
		
		refresh();
		
	}
	
	public void editTagSubmit() {
		
		try {
			
			if(this.editMode) {
				
				saveSelectedTag();
				
			} else {
				
				addTag();
				
			}
			
			refresh();
			
			RequestContext.getCurrentInstance().execute("PF('newTagDialog').hide();");
			
		} catch(Exception e) {
			
			logger.error(e.getMessage(), e);
			
			BeanUtil.addErrorMessage("Error", e.getMessage());
			
		}
		
	}
	
	public void editGroupSubmit() {
		
		try {
			
			if(this.editMode) {
				
				saveSelectedGroup();
				
			} else {
				
				addGroup();
				
			}
			
			refresh();
			
			RequestContext.getCurrentInstance().execute("PF('newGroupDialog').hide();");
			
		} catch(Exception e) {
			
			logger.error(e.getMessage(), e);
			
			BeanUtil.addErrorMessage("Error", e.getMessage());
			
		}
		
	}
	
	public void addTag() {
		BeanUtil.getSfContext().getStatisticsService().getStorage().add(selectedTag);
	}
	
	public void saveSelectedTag() {
		BeanUtil.getSfContext().getStatisticsService().getStorage().update(selectedTag);
	}
	
	public void addGroup() {
		BeanUtil.getSfContext().getStatisticsService().getStorage().add(selectedGroup);
	}
	
	public void saveSelectedGroup() {
		BeanUtil.getSfContext().getStatisticsService().getStorage().update(selectedGroup);
	}

	public List<Tag> getAllTags() {
		return allTags;
	}

	public Tag getSelectedTag() {
		return selectedTag;
	}

	public void setSelectedTag(Tag selectedTag) {
		this.selectedTag = selectedTag;
		this.editMode = true;
	}

	public boolean isEditMode() {
		return editMode;
	}

	public void setEditMode(boolean editMode) {
		this.editMode = editMode;
		
		if(!editMode) {
			this.selectedTag = new Tag();
		}
		
	}

	public List<TagGroup> getAllGroups() {
		return allGroups;
	}

	public TagGroup getSelectedGroup() {
		return selectedGroup;
	}

	public void setSelectedGroup(TagGroup selectedGroup) {
		this.selectedGroup = selectedGroup;
	}
	
}
