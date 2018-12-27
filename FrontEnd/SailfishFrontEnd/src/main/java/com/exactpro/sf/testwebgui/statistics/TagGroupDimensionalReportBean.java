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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.exactpro.sf.embedded.statistics.StatisticsException;
import com.exactpro.sf.embedded.statistics.StatisticsService;
import com.exactpro.sf.embedded.statistics.StatisticsUtils;
import org.apache.commons.lang3.StringUtils;
import org.omnifaces.util.Utils;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.embedded.statistics.entities.Tag;
import com.exactpro.sf.embedded.statistics.entities.TagGroup;
import com.exactpro.sf.embedded.statistics.storage.reporting.TagGroupDimension;
import com.exactpro.sf.embedded.statistics.storage.reporting.TagGroupReportResult;
import com.exactpro.sf.embedded.statistics.storage.reporting.TagGroupReportRow;
import com.exactpro.sf.testwebgui.BeanUtil;

@ManagedBean(name="tgReportBean")
@ViewScoped
@SuppressWarnings("serial")
public class TagGroupDimensionalReportBean implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger(TagGroupDimensionalReportBean.class);

    private static final String DIMENSIONS_DIR = "dimensions";

	private List<TagGroupDimension> selectedDemensions = new ArrayList<>();

	private TagGroupReportResult lastResult;

	private List<TagGroupDimension> allDimensions;

	private TagGroupDimension tagToAdd;

	private TreeNode root = new DefaultTreeNode(new TagGroupReportRow(), null); // root for tree table;

	// Row styling fields

	private String[] rowClasses = new String[] {"ui-datatable-odd", "ui-datatable-even"};

	private Map<Integer, Integer> classIndexes = new HashMap<>();

	private TreeNode selectedNode;

    private String dimensionsFileName;

    private List<TagGroupReportResult> results;

    private boolean saveDialog = true;

	private void buildTreeModel(List<TagGroupReportResult> results) {

        TreeNode newRoot = new DefaultTreeNode(new TagGroupReportRow(), null);

        if(!results.isEmpty()) {

            addChildNodes(null, results, 0, newRoot);

        }

        this.root = newRoot;

	}

	private void addChildNodes(String parentName, List<TagGroupReportResult> results, int curLevelIndex, TreeNode parent) {

		List<TagGroupReportRow> rowsCurrentLev = results.get(curLevelIndex).getRows();

		for(TagGroupReportRow row : rowsCurrentLev) {

			if(parentName == null || row.getDimensionsPath()[curLevelIndex - 1].equals(parentName)) { // 'row' is a child of 'parent'

				TreeNode node = new DefaultTreeNode(row, parent);

				node.setExpanded(true);

				//node.

				if(curLevelIndex < results.size() -1) { // next level exists

					addChildNodes(row.getDimensionsPath()[curLevelIndex], results, curLevelIndex + 1, node);

				}

			}

		}


    }

	public void generateReport() {

		if(this.selectedDemensions.isEmpty()) {

			BeanUtil.addErrorMessage("No tags selected", "");

			return;

		}

		try {
            StatisticsService statisticsService = BeanUtil.getSfContext().getStatisticsService();
            List<TagGroupReportResult> results = StatisticsUtils.generateTagGroupReportResults(
                                statisticsService, this.selectedDemensions);
			buildTreeModel(results);
            this.results = results;
		} catch (StatisticsException e) {
            BeanUtil.addErrorMessage(e.getSummary(), e.getMessage());
            logger.error(e.getMessage(), e);
        } catch (Exception e) {

			BeanUtil.addErrorMessage("Error", e.getMessage());

			logger.error(e.getMessage(), e);

		}

	}

	public String getRowClass(TagGroupReportRow row) {

		if(!this.classIndexes.containsKey(row.getDimensionsPath().length)) {

			this.classIndexes.put(row.getDimensionsPath().length, (row.getDimensionsPath().length -1) % this.rowClasses.length);

		}

		int index = this.classIndexes.get(row.getDimensionsPath().length);

		this.classIndexes.put(row.getDimensionsPath().length, (index + 1) % this.rowClasses.length);

		return rowClasses[index];

	}

	public void getExported() {

		try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();
            externalContext.responseReset();
            externalContext.setResponseContentType("text/csv");
            String reportName = StatisticsUtils.createStatsPerTagsName();
            externalContext.setResponseHeader("Content-Disposition", "attachment; filename=\"" + reportName + "\"");

            OutputStream output = externalContext.getResponseOutputStream();
            StatisticsUtils.writeTagGroupReportToCsv(output, results);
            facesContext.responseComplete();
		} catch (IOException e) {

			logger.error("Could not export data to csv file", e);
			
			BeanUtil.addErrorMessage("Could not export statistics to csv file", "");
		}
		
	}
	
	public void onTagSelect() {
    	
    	logger.debug("Tag select invoked");
    		
		this.selectedDemensions.add(tagToAdd);
    	
    	this.tagToAdd = null;
    	
    }
    
    public void removeTag(TagGroupDimension tag) {
    	
		this.selectedDemensions.remove(tag);
    	
    }
	
	public List<TagGroupDimension> completeDimension(String query) {
    	
    	List<TagGroupDimension> result = new ArrayList<>();
    	
    	List<TagGroup> groups = BeanUtil.getSfContext().getStatisticsService().getStorage().getGroupsContains(query);
    	List<Tag> tags = BeanUtil.getSfContext().getStatisticsService().getStorage().getTagsContains(query);
    	
    	for(TagGroup group : groups) {
    		
    		TagGroupDimension dimension = TagGroupDimension.fromGroup(group);
    		
    		if(!this.selectedDemensions.contains(dimension)) {
    			result.add(dimension);
    		}
    		
    	}
    	
    	for(Tag tag: tags) {
    		
    		TagGroupDimension dimension = TagGroupDimension.fromTag(tag);
    		
    		if(!this.selectedDemensions.contains(dimension)) {
    			result.add(dimension);
    		}
    		
    	}
    	
    	this.allDimensions = result;
    	
    	return result;
    	
    }
	
	public TagGroupReportResult getLastResult() {
		return lastResult;
	}

	public void setLastResult(TagGroupReportResult lastResult) {
		this.lastResult = lastResult;
	}

	public List<TagGroupDimension> getSelectedDemensions() {
		return selectedDemensions;
	}

	public void setSelectedDemensions(List<TagGroupDimension> selectedDemensions) {
		this.selectedDemensions = selectedDemensions;
	}

	public TagGroupDimension getTagToAdd() {
		return tagToAdd;
	}

	public void setTagToAdd(TagGroupDimension tagToAdd) {
		this.tagToAdd = tagToAdd;
	}

	public List<TagGroupDimension> getAllDimensions() {
		return allDimensions;
	}

	public TreeNode getRoot() {
		return root;
	}

	public void setRoot(TreeNode root) {
		this.root = root;
	}

	public TreeNode getSelectedNode() {
		return selectedNode;
	}

	public void setSelectedNode(TreeNode selectedNode) {
		this.selectedNode = selectedNode;
	}
	
    public Set<String> getDimensionsFileNames() {
        Set<String> result;
        try {
            result = BeanUtil.getSfContext().getWorkspaceDispatcher().listFiles(null, FolderType.CFG, DIMENSIONS_DIR);
        } catch (Throwable e) {
            result = Collections.emptySet();
        }
        return result;
    }

    public void preSaveDimensions() {
        this.saveDialog = true;
    }

    public void preLoadDimensions() {
        this.saveDialog = false;
    }

    public void saveLoadDimensions() {

        if (StringUtils.isEmpty(this.dimensionsFileName)) {
            BeanUtil.addErrorMessage(this.saveDialog ? "Not saved" : "Not loaded", "Please specify a file name");
            return;
        }

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(TagGroupDimensionListWrapper.class, TagGroupDimension.class);

            if (this.saveDialog) {
                Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
                jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                File file = BeanUtil.getSfContext().getWorkspaceDispatcher().createFile(FolderType.CFG, true, DIMENSIONS_DIR,
                        this.dimensionsFileName);
                TagGroupDimensionListWrapper wrapper = new TagGroupDimensionListWrapper();
                wrapper.setSelectedDemensions(this.selectedDemensions);
                jaxbMarshaller.marshal(wrapper, file);
            } else {
                Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                TagGroupDimensionListWrapper wrapper = (TagGroupDimensionListWrapper) jaxbUnmarshaller
                        .unmarshal(BeanUtil.getSfContext().getWorkspaceDispatcher().getFile(FolderType.CFG, DIMENSIONS_DIR, this.dimensionsFileName));
                this.selectedDemensions = wrapper.getSelectedDemensions();
            }
        } catch (Throwable e) {
            logger.error(buildErrorMessage(), e);
            BeanUtil.addErrorMessage(buildErrorMessage(), e.getMessage());
        }

        this.dimensionsFileName = StringUtils.EMPTY;
    }

    private String buildErrorMessage() {
        StringBuilder builder = new StringBuilder("Failed to ");
        builder.append(this.saveDialog ? "save " : "load ");
        builder.append("dimensions ");
        builder.append(this.saveDialog ? "to " : "from ");
        builder.append("file: ");
        builder.append(this.dimensionsFileName);

        return builder.toString();
    }

    public String getDimensionsFileName() {
        return dimensionsFileName;
    }

    public void setDimensionsFileName(String dimensionsFileName) {
        this.dimensionsFileName = dimensionsFileName;
    }

    public boolean isSaveDialog() {
        return saveDialog;
    }

}
