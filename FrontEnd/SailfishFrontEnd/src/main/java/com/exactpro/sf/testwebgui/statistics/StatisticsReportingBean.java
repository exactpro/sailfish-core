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

import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.embedded.statistics.StatisticsService;
import com.exactpro.sf.embedded.statistics.entities.SfInstance;
import com.exactpro.sf.embedded.statistics.entities.Tag;
import com.exactpro.sf.embedded.statistics.handlers.IStatisticsReportHandler;
import com.exactpro.sf.embedded.statistics.handlers.ReportRow;
import com.exactpro.sf.embedded.statistics.storage.StatisticsReportingStorage;
import com.exactpro.sf.embedded.statistics.storage.reporting.AggregateReportParameters;
import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.general.SessionStored;

@ManagedBean(name="statReportingBean")
@ViewScoped
@SuppressWarnings("serial")
public class StatisticsReportingBean implements Serializable {
	
	private static final Logger logger = LoggerFactory.getLogger(StatisticsReportingBean.class);
	
	private static final String REPORT_FILE_PREFIX = "Aggregated_report";

	// average number of test cases per matrix is 100 so we decided to choose this limit for loading matrices
	private static final int LOADING_MATRIX_LIMIT = 100;
	
	private boolean statisticsDbAvailable;
	
	private Date from;
	
	private Date to;
	
	private Date loadedFrom;
	
	private Date loadedTo;
	
	private List<SfInstance> selectedSfInstances = new ArrayList<>();
	
	private List<ReportRow> lastResult = Collections.emptyList();
	
	private List<SfInstance> allSfInstances;

	private IStatisticsReportHandler statisticsReportHandler;

    @SessionStored
    private List<Tag> tags = new ArrayList<>();

    private Tag tagToAdd;
    private List<Tag> allTags;

    private List<SailfishURI> reportHandlers;

    private SailfishURI currentReportURI;

	@PostConstruct
	public void init() {

        StatisticsService statisticsService = BeanUtil.getSfContext().getStatisticsService();

        this.statisticsDbAvailable = statisticsService.isConnected();
		
		if(this.statisticsDbAvailable) {
		
			this.from = DateUtils.truncate( new Date(), Calendar.DATE );
			
			this.to = new Date();
			
			this.allSfInstances = statisticsService.getStorage().getAllSfInstances();
			
			this.selectedSfInstances.addAll(this.allSfInstances);

            this.allTags = statisticsService.getStorage().getAllTags();

            this.reportHandlers = new ArrayList<>(statisticsService.getStatisticsReportHandlersURI());

            if (reportHandlers.isEmpty()) {
                throw new EPSCommonException("Can't find any report");
            }

            currentReportURI = reportHandlers.get(0);

            statisticsReportHandler = statisticsService.getStatisticsReportHandler(currentReportURI);
		
		}

	}

    public String getExportFileName() {
        return statisticsReportHandler.getReportName(getAggregateReportParameters());
    }
	
	public void generateReport() {
		
		logger.debug("Generate {} - {}; {}", this.from, this.to, this.selectedSfInstances);

        AggregateReportParameters params = getAggregateReportParameters();

        try {
            statisticsReportHandler.reset();
            StatisticsReportingStorage reportingStorage = BeanUtil.getSfContext().getStatisticsService().getReportingStorage();
            List<Long> matrixIds = reportingStorage.getMatrixRunIDs(params);
            if (!matrixIds.isEmpty()) {
                int limit = 0;
                while (limit < matrixIds.size()) {
                    params.setMatrixRunIds(matrixIds.subList(limit, Math.min((limit += LOADING_MATRIX_LIMIT), matrixIds.size())));
                    statisticsReportHandler.handleMatrixRunTestCases(reportingStorage.generateAggregatedReport(params), reportingStorage);
                }
            }
            statisticsReportHandler.finalize(reportingStorage);
            this.lastResult = statisticsReportHandler.getReportRows();

			
			this.loadedFrom = from;
			this.loadedTo = to;
			
		} catch(Exception e) {
			
			logger.error(e.getMessage(), e);
			
			BeanUtil.addErrorMessage(e.getMessage(), "");
			
		}
		
	}

    private AggregateReportParameters getAggregateReportParameters() {
        AggregateReportParameters params = new AggregateReportParameters();

        params.setFrom(from);
        params.setTo(to);
        params.setSfInstances(new ArrayList<>(selectedSfInstances));
        params.setTags(new ArrayList<>(tags));
        return params;
    }

    public void getResultsInCSV() {
        logger.info("getResultsInCSV invoked {}", System.getProperty("user.name"));
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();
            externalContext.responseReset();
            String reportName = getExportFileName();
            externalContext.setResponseHeader("Content-Disposition", "attachment; filename=\"" + reportName + "\"");
            OutputStream output = externalContext.getResponseOutputStream();
            statisticsReportHandler.writeReport(output);
            facesContext.responseComplete();
        } catch (Exception e) {
            logger.error("Could not export report", e);
            BeanUtil.addErrorMessage("Could not export report", e.getMessage());
        }
    }

    public void reportTypeChanged(AjaxBehaviorEvent event) {
        this.statisticsReportHandler = BeanUtil.getSfContext().getStatisticsService().getStatisticsReportHandler(currentReportURI);
        this.lastResult = Collections.emptyList();
    }

    public void onTagSelect() {
        logger.debug("Tag select invoked");

        this.tags.add(tagToAdd);
        this.tagToAdd = null;
        this.allTags.removeAll(tags);
    }

    public void removeTag(Tag tag) {
        this.tags.remove(tag);
        this.allTags.add(tag);
    }

    public List<Tag> completeTag(String query) {
        List<Tag> result = new ArrayList<>();
        String loweredQuery = query.toLowerCase();
        for(Tag tag : this.allTags) {
            if(tag.getName().toLowerCase().contains(loweredQuery)) {
                result.add(tag);
            }
        }
        return result;
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

	public List<ReportRow> getLastResult() {
		return lastResult;
	}

	public List<SfInstance> getAllSfInstances() {
		return allSfInstances;
	}

	public Date getLoadedFrom() {
		return loadedFrom;
	}

	public void setLoadedFrom(Date loadedFrom) {
		this.loadedFrom = loadedFrom;
	}

	public Date getLoadedTo() {
		return loadedTo;
	}

	public void setLoadedTo(Date loadedTo) {
		this.loadedTo = loadedTo;
	}

	public boolean isStatisticsDbAvailable() {
		return statisticsDbAvailable;
	}

	public void setStatisticsDbAvailable(boolean statisticsDbAvailable) {
		this.statisticsDbAvailable = statisticsDbAvailable;
	}

    public List<Tag> getTags() {
        return tags;
    }

    public Tag getTagToAdd() {
        return tagToAdd;
    }

    public void setTagToAdd(Tag tagToAdd) {
        this.tagToAdd = tagToAdd;
    }

    public List<Tag> getAllTags() {
        return allTags;
    }

    public List<SailfishURI> getReportHandlers() {
        return reportHandlers;
    }

    public List<String> getReportHeader() {
        return statisticsReportHandler.getHeaderColumns();
    }

    public SailfishURI getCurrentReportURI() {
        return currentReportURI;
    }

    public void setCurrentReportURI(SailfishURI currentReportURI) {
        this.currentReportURI = currentReportURI;
    }
}
