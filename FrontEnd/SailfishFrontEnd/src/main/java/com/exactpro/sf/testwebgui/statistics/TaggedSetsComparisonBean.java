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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.embedded.statistics.entities.Tag;
import com.exactpro.sf.embedded.statistics.storage.reporting.TaggedComparisonResult;
import com.exactpro.sf.embedded.statistics.storage.reporting.TaggedComparisonRow;
import com.exactpro.sf.embedded.statistics.storage.reporting.TaggedSetsComparisonParameters;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.testwebgui.BeanUtil;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

@ManagedBean(name="taggedSetsBean")
@ViewScoped
@SuppressWarnings("serial")
public class TaggedSetsComparisonBean implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(TaggedSetsComparisonBean.class);
    private static final String DESCRIPTION_COLUMN = "Description";

    private List<Tag> allTags;
	
	private List<Tag> firstSet = new ArrayList<>();
	
	private List<Tag> secondSet = new ArrayList<>();
	
	private Tag tagToAdd;
	
	private TaggedComparisonResult lastResult;
	
	private TaggedComparisonResult diffResult;
	
	private TaggedComparisonRow selected;
	
	private String[] availableColumns = new String[] {"Matrix", "Status", DESCRIPTION_COLUMN, "Fail Reason", "Failed Actions", "User Status", "Comment", "Start Time", "Finish Time", "Tags", "Hash"};
	
	private String[] selectedColumns = availableColumns;
	
	private boolean showDiffOnly;
	
	private SetMultimap<String, String> diffHighlights;
	
	private SetMultimap<Long, Long> actionDiffHighlights;
	
	private SetMultimap<Long, Long> actionFailReasonDiffHighlights;
	
	@PostConstruct
	public void init() {
	
		this.allTags = BeanUtil.getSfContext().getStatisticsService().getStorage().getAllTags();
		
	}
	
	public void generate() {
		
		if(this.firstSet.isEmpty()) {
			BeanUtil.addErrorMessage("First set is empty", "");
			return;
		}
		
		if(this.secondSet.isEmpty()) {
			BeanUtil.addErrorMessage("Second set is empty", "");
			return;
		}
		
		logger.debug("Sets: {}; {}", firstSet, secondSet);
		
		TaggedSetsComparisonParameters params = new TaggedSetsComparisonParameters();
		
		params.setFirstSet(firstSet);
		params.setSecondSet(secondSet);
		
		try {
			
			this.lastResult = BeanUtil.getSfContext().getStatisticsService().getReportingStorage().generateTaggedSetsComparisonReport(params);
			generateDiffResult();
			
		} catch(Exception e) {
			
			logger.error(e.getMessage(), e);
			
		}
		
	}
	
   private void generateDiffResult() {
       List<TaggedComparisonRow> diffRows = new ArrayList<>();
	        
       this.diffHighlights = HashMultimap.create();
       this.actionDiffHighlights = HashMultimap.create();
       this.actionFailReasonDiffHighlights = HashMultimap.create();
	        
       for (TaggedComparisonRow row : this.lastResult.getRows()) {
           boolean toAdd = false;
        
           if (!StringUtils.equals(row.getFirstMatrixName(), row.getSecondMatrixName())) {
               toAdd = true;
               this.diffHighlights.put(row.getTestCaseId(), "Matrix");
           }
           if (!Objects.equals(row.getFirstRawHash(), row.getSecondRawHash())) {
               toAdd = true;
               this.diffHighlights.put(row.getTestCaseId(), "Hash");
           }
           if (row.getFirstStatus() != row.getSecondStatus()) {
               toAdd = true;
               this.diffHighlights.put(row.getTestCaseId(), "Status");
           }
           if (row.getFirstStatus() == StatusType.FAILED && row.getSecondStatus() == StatusType.FAILED) {
               if (!StringUtils.equals(row.getFirstFailReason(), row.getSecondFailReason())) {
                   toAdd = true;
                   this.diffHighlights.put(row.getTestCaseId(), "Fail Reason");
               }
           }
           if (!StringUtils.equals(row.getFirstDescription(), row.getSecondDescription())) {
               toAdd = true;
               this.diffHighlights.put(row.getTestCaseId(), DESCRIPTION_COLUMN);
           }
           
           if (!CollectionUtils.isEqualCollection(row.getFirstFailedActions().entrySet(), row.getSecondFailedActions().entrySet())) {
               toAdd = true;
               this.diffHighlights.put(row.getTestCaseId(), "Failed Actions");
	            
               Set<Long> actionsIntersection = new HashSet<>(row.getFirstFailedActions().keySet());
               actionsIntersection.retainAll(row.getSecondFailedActions().keySet());
	            
               for (Long actionRank : row.getFirstFailedActions().keySet()) {
                   if (!actionsIntersection.contains(actionRank)) {
                       this.actionDiffHighlights.put(row.getFirstTestCaseRunId(), actionRank);
                   }
               }
	            
               for (Long actionRank : row.getSecondFailedActions().keySet()) {
                   if (!actionsIntersection.contains(actionRank)) {
                       this.actionDiffHighlights.put(row.getSecondTestCaseRunId(), actionRank);
                   }
               }
                
               for (Long actionRank : actionsIntersection) {
                   if (!StringUtils.equals(row.getFirstFailedActions().get(actionRank), row.getSecondFailedActions().get(actionRank))) {
                       this.actionFailReasonDiffHighlights.put(row.getFirstTestCaseRunId(), actionRank);
                       this.actionFailReasonDiffHighlights.put(row.getSecondTestCaseRunId(), actionRank);
                   }
               }
           }
	        
           if (toAdd) {
               diffRows.add(row);
           }
       }
	        
       TaggedComparisonResult diffResult = new TaggedComparisonResult();
       diffResult.setRows(diffRows);
       this.diffResult = diffResult;
    }
	
    public String getHighlightStyle(String tcId, String column) {
        if (this.diffHighlights.containsKey(tcId) && this.diffHighlights.get(tcId).contains(column)) {
            return "eps-diff-highlight";
        }
        return "";
    }
    
    public String getActionHighlightStyle(Long tcRunId, Long actionRank) {
        if (this.actionDiffHighlights != null 
            && this.actionDiffHighlights.containsKey(tcRunId) 
                && this.actionDiffHighlights.get(tcRunId).contains(actionRank)) {
            
            return "eps-diff-highlight";
        }
        return "";
    }
    
    public String getActionFailReasonHighlightStyle(Long tcRunId, Long actionRank) {
        String style = getActionHighlightStyle(tcRunId, actionRank);
        if (!StringUtils.isEmpty(style)) {
            return style;
        }
        if (this.actionFailReasonDiffHighlights != null 
            && this.actionFailReasonDiffHighlights.containsKey(tcRunId) 
                && this.actionFailReasonDiffHighlights.get(tcRunId).contains(actionRank)) {
            
            return "eps-diff-highlight";
        }
        return "";
    }
	
	public boolean isColumnSelected(String column) {
		
		for(String selected : this.selectedColumns) {
			
			if(column.equals(selected)) {
				
				return true;
				
			}
			
		}
		
		return false;
		
	}
	
	public List<Tag> completeFirst(String query) {
		
		return completeTag(query, firstSet);
		
	}
	
	public List<Tag> completeSecond(String query) {
		
		return completeTag(query, secondSet);
		
	}
	
	public void onTagSelect(boolean first) {
    	
    	logger.debug("Tag select invoked {}", first);
    	
    	if(first) {
    	
	    	this.firstSet.add(tagToAdd);
    	
    	} else {
    		
    		this.secondSet.add(tagToAdd);
    		
    	}
    	
    	this.tagToAdd = null;
    	
    }
    
    public void removeTag(Tag tag, boolean first) {
    	
    	if(first) {
    	
    		this.firstSet.remove(tag);
    	
    	} else {
    		
    		this.secondSet.remove(tag);
    		
    	}
    	
    }
	
	private List<Tag> completeTag(String query, List<Tag> alreadySelected) {
    	
    	List<Tag> result = new ArrayList<>();
    	
    	String loweredQuery = query.toLowerCase();
    	
    	for(Tag tag : this.allTags) {
    		
    		if(tag.getName().toLowerCase().contains(loweredQuery)
    				&& !alreadySelected.contains(tag)) {
    			
				result.add(tag);
    			
    		}
    		
    	}
    	
    	return result;
    	
    }

	public List<Tag> getFirstSet() {
		return firstSet;
	}

	public void setFirstSet(List<Tag> firstSet) {
		this.firstSet = firstSet;
	}

	public List<Tag> getSecondSet() {
		return secondSet;
	}

	public void setSecondSet(List<Tag> secondSet) {
		this.secondSet = secondSet;
	}

	public TaggedComparisonResult getLastResult() {
        return this.showDiffOnly ? this.diffResult : this.lastResult;
	}

	public List<Tag> getAllTags() {
		return allTags;
	}

	public void setAllTags(List<Tag> allTags) {
		this.allTags = allTags;
	}

	public Tag getTagToAdd() {
		return tagToAdd;
	}

	public void setTagToAdd(Tag tagToAdd) {
		this.tagToAdd = tagToAdd;
	}

	public String[] getAvailableColumns() {
		return availableColumns;
	}

	public String[] getSelectedColumns() {
		return selectedColumns;
	}

	public void setSelectedColumns(String[] selectedColumns) {
		this.selectedColumns = selectedColumns;
	}

    public boolean isShowDiffOnly() {
        return showDiffOnly;
    }

    public void setShowDiffOnly(boolean showDiffOnly) {
        this.showDiffOnly = showDiffOnly;
    }

	public TaggedComparisonRow getSelected() {
		return selected;
	}

	public void setSelected(TaggedComparisonRow selected) {
		this.selected = selected;
	}
	
}
