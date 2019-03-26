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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.primefaces.context.RequestContext;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.embedded.statistics.MatrixInfo;
import com.exactpro.sf.embedded.statistics.StatisticsUtils;
import com.exactpro.sf.embedded.statistics.entities.SfInstance;
import com.exactpro.sf.embedded.statistics.entities.Tag;
import com.exactpro.sf.embedded.statistics.entities.TestCaseRunStatus;
import com.exactpro.sf.embedded.statistics.storage.AggregatedReportRow;
import com.exactpro.sf.embedded.statistics.storage.TestCaseRunComments;
import com.exactpro.sf.embedded.statistics.storage.reporting.AggregateReportParameters;
import com.exactpro.sf.embedded.statistics.storage.reporting.TestCasesDisplayMode;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.storage.IOptionsStorage;
import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.general.SessionStorage;
import com.exactpro.sf.testwebgui.general.SessionStored;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ManagedBean(name="mrHistBean")
@ViewScoped
@SuppressWarnings("serial")
public class TestScriptsHistoryBean extends AbstractTagsStatisticsBean implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger(TestScriptsHistoryBean.class);

	private static final String REPORT_FILE_PREFIX = "Script_runs_history";

	private static final String OPTIONS_STORAGE_PREFIX = "mrHistBean.";

	private boolean statisticsDbAvailable;

	@SessionStored
	private TestCasesDisplayMode tcDisplayMode = TestCasesDisplayMode.AllNatural;

	private Date loadedFrom;

	private Date loadedTo;

	private List<AggregatedReportRow> lastResult = new ArrayList<>();

    private String customReportsPath;

	private AggregatedReportRow selectedRow;

	private AggregatedReportRow selected;

    private List<AggregatedReportRow> selectedRows;

    private List<Tag> allCustomerTags;

    private List<Tag> customerTags = new ArrayList<>();

    private Tag customTagToAdd;

    private Action action = Action.ADD;

	private List<TestCaseRunStatus> allRunStatuses;

	@SessionStored
    private String[] selectedColumns = StatisticsUtils.availableScriptRunHistoryColumns;

	@SessionStored
    private boolean renderExpaned;

	private Set<Long> manualyToggled = new HashSet<>();

	@SessionStored
    private boolean emptyCommentOnly;

	@SessionStored
	private String sortBy;

	@SessionStored
	private boolean sortAsc = true;

	@SessionStored
    private boolean splitByStatus;

	private TestCaseRunComments userComments;

	@SessionStored
	private boolean commentOnlyFailedTcs = true;

    private MatrixInfo matrixInfo;

	private boolean exportWithTCs;
    private boolean exportWithActions;
    private boolean includeExecutedInFromToRange;

    private String lastResultReportNamesJson;

	private boolean downloadReportsEnabled;

	private ObjectMapper mapper;


	private void doSplitByStatus() {

	    List<AggregatedReportRow> failed = new ArrayList<>();
		List<AggregatedReportRow> bad = new ArrayList<>();
		List<AggregatedReportRow> casual = new ArrayList<>();
		List<AggregatedReportRow> good = new ArrayList<>();

		List<AggregatedReportRow> currentTarget = bad;

        for(AggregatedReportRow row : lastResult) {

			if(row.isMatrixRow()) {
				if(row.getPassedCount() > 0 && row.getFailedCount() > 0) {
					currentTarget = casual;
				} else if(row.getPassedCount() == 0 && row.getFailedCount() == 0) {
				    currentTarget = failed;
			    } else if(row.getFailedCount() > 0) {
					currentTarget = bad;
				} else {
					currentTarget = good;
				}
			}

			currentTarget.add(row);

		}

        this.lastResult = new ArrayList<>(lastResult.size());

        lastResult.addAll(failed);
        lastResult.addAll(bad);
        lastResult.addAll(casual);
        lastResult.addAll(good);

	}

	private void restoreOptions() {

		try {

			IOptionsStorage optStorage = BeanUtil.getSfContext().getOptionsStorage();

			this.customReportsPath = optStorage.getOption(OPTIONS_STORAGE_PREFIX + "customReportsPath");

		} catch(Exception e) {

			logger.error(e.getMessage(), e);

			BeanUtil.addErrorMessage("Options restore failed", e.getMessage());

		}

	}

	private List<AggregatedReportRow> findTcRowsForMatrixRun(AggregatedReportRow matrixRow) {

		List<AggregatedReportRow> result = new ArrayList<>();

        int matrixRowIndex = lastResult.indexOf(matrixRow);

		if(matrixRowIndex < 0) {
			return result;
		}

		AggregatedReportRow row;

        for(int i = matrixRowIndex + 1; i < lastResult.size(); i++) {

            row = lastResult.get(i);

			if(!row.getMatrixRunId().equals(matrixRow.getMatrixRunId())) {
				break;
			}

			result.add(row);

		}

		logger.debug("Found {} test cases", result.size());

		return result;

	}

	@PostConstruct
	public void init() {
        super.init();

        this.allCustomerTags = new ArrayList<>(allTags);

		this.statisticsDbAvailable = BeanUtil.getSfContext().getStatisticsService().isConnected();

        if(statisticsDbAvailable) {

			this.allRunStatuses = BeanUtil.getSfContext().getStatisticsService().getStorage().getAllRunStatuses();

			this.mapper = new ObjectMapper();

		}

		restoreOptions();

		logger.debug("TestScriptsHistoryBean [{}] constructed", hashCode());

	}

	public void copyUserComments(AggregatedReportRow row) {

        int rowIndex = lastResult.indexOf(row);

		AggregatedReportRow otherRow;

		for(int i = rowIndex -1; i > 0; i--) {

            otherRow = lastResult.get(i);

			if(otherRow.getUserComments().getStatus() != null
					|| StringUtils.isNotEmpty(otherRow.getUserComments().getComment())) {

				this.userComments = otherRow.getUserComments();
				this.selectedRow = row;

				updateUserComments();
				return;
			}

		}

		logger.info("Can not find tcr with not empty comment to copy");

	}

	public void prepareUserComments() {
        this.userComments = selectedRow.isMatrixRow() ? new TestCaseRunComments() : new TestCaseRunComments(selectedRow.getUserComments());
	}

    public void getExported() {

		try {

            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();
            externalContext.responseReset();
            externalContext.setResponseContentType("text/csv");
            String reportName = StatisticsUtils.createScriptRunsHistoryName();
            externalContext.setResponseHeader("Content-Disposition", "attachment; filename=\"" + reportName + "\"");
            OutputStream output = externalContext.getResponseOutputStream();
            StatisticsUtils.writeScriptRunsHistory(BeanUtil.getSfContext(), output,
                                                   new ArrayList<>(Arrays.asList(selectedColumns)), lastResult,
                                                   exportWithTCs, exportWithActions, matrixInfo);
            facesContext.responseComplete();

		} catch (IOException e) {

			logger.error("Could not export data to csv file", e);

			BeanUtil.addErrorMessage("Could not export statistics to csv file", "");

		}
	}

	public void rowToggled() {

		long id = Long.valueOf(FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("mrId"));

		logger.debug("toggled {}", id);

        if(manualyToggled.contains(id)) {
            manualyToggled.remove(id);
		} else {
            manualyToggled.add(id);
		}

	}

	public void onRowSelect(SelectEvent event) {
        if(selectedRows != null) {
            Object row = event.getObject();
            selectedRows.remove(row);
        }

	}

	public boolean isRenderExpanded(AggregatedReportRow row) {
        return renderExpaned != manualyToggled.contains(row.getMatrixRunId());
	}

	public boolean isColumnSelected(String column) {

        for(String selected : selectedColumns) {

			if(column.equals(selected)) {

				return true;

			}

		}

		return false;

	}

	public void updateUserComments() {

		List<AggregatedReportRow> rowsToUpdate = new ArrayList<>();

        if(selectedRow.isMatrixRow()) {
            rowsToUpdate = findTcRowsForMatrixRun(selectedRow);
		} else {
			rowsToUpdate.add(selectedRow);
		}

		try {

			for(AggregatedReportRow row : rowsToUpdate) {

                if(selectedRow.isMatrixRow() && commentOnlyFailedTcs && row.getStatus() != StatusType.FAILED) {
					continue;
				}

				row.setUserComments(userComments);

				BeanUtil.getSfContext().getStatisticsService().getStorage()
                        .updateTcrUserComments(row.getTestCaseRunId(), userComments);

			}

			RequestContext.getCurrentInstance().execute("PF('tcrCommentsDlg').hide();");

			BeanUtil.addInfoMessage("Saved", "");

		} catch(Throwable t) {

			logger.error(t.getMessage(), t);
			BeanUtil.addErrorMessage("Error", t.getMessage());

		}

	}

	public void setSelectedRow(AggregatedReportRow selectedRow) {

		this.selectedRow = selectedRow;

		prepareUserComments();

	}

	public void applyOptions() {

		try {

			IOptionsStorage optStorage = BeanUtil.getSfContext().getOptionsStorage();

            optStorage.setOption(OPTIONS_STORAGE_PREFIX + "customReportsPath", customReportsPath);

			BeanUtil.addInfoMessage("Applied", "But you have to generate report again");

		} catch(Exception e) {

			logger.error(e.getMessage(), e);

			BeanUtil.addErrorMessage("Apply failed", e.getMessage());

		}

	}

    public String getReportRequest(AggregatedReportRow row) {
        return BeanUtil.getReportRequest(customReportsPath, row, false);
    }

    public String getZipReport(AggregatedReportRow row) {
        return BeanUtil.getZipReport(customReportsPath, row, false);
    }

    public String getLastResultZipReports() {
        StringBuilder sb = new StringBuilder(BeanUtil.getContextPath(customReportsPath, true));

        sb.append("/report/reports?reports=");
        sb.append(lastResultReportNamesJson);

        return sb.toString();
    }

	public String buildReportUrl(AggregatedReportRow row, boolean report) {
        return BeanUtil.buildReportUrl(customReportsPath, row, report);
	}

    @PreDestroy
    public void destroy() {
        logger.debug("TestScriptsHistoryBean [{}] destroy", hashCode());
    }


	public TestCasesDisplayMode[] getDisplayModes() {

		return TestCasesDisplayMode.values();

	}

	public String getExportFileName() {

        if(lastResult == null || lastResult.isEmpty()) {

			return "";

		}

		DateFormat format = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");

		StringBuilder sb = new StringBuilder();

		sb.append(REPORT_FILE_PREFIX)
		.append("__")
                .append(format.format(loadedFrom))
		.append("__")
                .append(format.format(loadedTo))
		.append(".csv");

		return sb.toString();

	}

	public void generateReport() {

        logger.debug("Generate {} - {}; {}", from, to, selectedSfInstances);

		BeanUtil.findBean("sessionStorage", SessionStorage.class).saveStateOfAnnotatedBean(this);

		this.manualyToggled = new HashSet<>();

		AggregateReportParameters params = new AggregateReportParameters();

		params.setFrom(from);
		params.setTo(to);
		params.setSfInstances(new ArrayList<>(selectedSfInstances));
		params.setTcDisplayMode(tcDisplayMode);
		params.setTags(new ArrayList<>(tags));
		params.setAllTags(!anyTag);
		params.setEmptyCommentOnly(emptyCommentOnly);
		params.setMatrixNamePattern(matrixNamePattern);
		params.setIncludeExecutedInFromToRange(includeExecutedInFromToRange);

		params.setSortBy(sortBy);
		params.setSortAsc(sortAsc);

		try {

			this.lastResult = BeanUtil.getSfContext().getStatisticsService().getReportingStorage().generateTestScriptsReport(params);
            this.matrixInfo = MatrixInfo.extractMatrixInfo(lastResult);

            if(splitByStatus) {

				doSplitByStatus();

			}

			this.loadedFrom = from;
			this.loadedTo = to;

			List<String> reports = new ArrayList<>();

            for(AggregatedReportRow row : lastResult) {
	            if (row.isMatrixRow()) {
	                reports.add(row.getReportFolder());
	            }
	        }

            this.lastResultReportNamesJson = mapper.writeValueAsString(reports);
	        this.downloadReportsEnabled = true;

		} catch(Exception e) {

			logger.error(e.getMessage(), e);

			BeanUtil.addErrorMessage(e.getMessage(), "");

			if (e instanceof JsonProcessingException) {
			    this.downloadReportsEnabled = false;
			}

		}

	}

    public void applyAction() {
        logger.debug("applyAction {} - {};", customerTags, action);

        if(selectedRows == null || selectedRows.isEmpty()) {
            BeanUtil.addErrorMessage("There are no selected rows", "");
            return;
        }
        if(customerTags == null || customerTags.isEmpty()) {
            BeanUtil.addErrorMessage("There are no selected customer tags)", "");
            return;
        }

        if(action == Action.REMOVE) {
            RequestContext.getCurrentInstance().execute("PF('tagConfirmation').show()");
        } else {
            confirmAction();
        }
    }

    public void confirmAction() {
        manageTagForRows(selectedRows, action, customerTags);
        generateReport();
    }

    public void removeTagFromResult(AggregatedReportRow row, Tag tag) {
        logger.debug("removeTagFromResult {};", tag);
        manageTagForRows(Collections.singletonList(row), Action.REMOVE, Collections.singletonList(tag));
        generateReport();
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

	public List<AggregatedReportRow> getLastResult() {
		return lastResult;
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

	public TestCasesDisplayMode getTcDisplayMode() {
		return tcDisplayMode;
	}

	public void setTcDisplayMode(TestCasesDisplayMode tcDisplayMode) {
		this.tcDisplayMode = tcDisplayMode;
	}

	public List<Tag> getTags() {
		return tags;
	}

	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}

	public String getCustomReportsPath() {
		return customReportsPath;
	}

	public void setCustomReportsPath(String customReportsPath) {
		this.customReportsPath = customReportsPath;
	}

	public AggregatedReportRow getSelectedRow() {
		return selectedRow;
	}

	public List<TestCaseRunStatus> getAllRunStatuses() {
		return allRunStatuses;
	}

	public String[] getSelectedColumns() {
		return selectedColumns;
	}

	public void setSelectedColumns(String[] selectedColumns) {
		this.selectedColumns = selectedColumns;
	}

	public String[] getAvailableColumns() {
        return StatisticsUtils.availableScriptRunHistoryColumns;
	}

	public boolean isRenderExpaned() {
		return renderExpaned;
	}

	public void setRenderExpaned(boolean renderExpaned) {
		this.renderExpaned = renderExpaned;
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

	public boolean isSplitByStatus() {
		return splitByStatus;
	}

	public void setSplitByStatus(boolean splitByStatus) {
		this.splitByStatus = splitByStatus;
	}

	public TestCaseRunComments getUserComments() {
		return userComments;
	}

	public void setUserComments(TestCaseRunComments userComments) {
		this.userComments = userComments;
	}

	public boolean isCommentOnlyFailedTcs() {
		return commentOnlyFailedTcs;
	}

	public void setCommentOnlyFailedTcs(boolean commentOnlyFailedTcs) {
		this.commentOnlyFailedTcs = commentOnlyFailedTcs;
	}

	public AggregatedReportRow getSelected() {
		return selected;
	}

	public void setSelected(AggregatedReportRow selected) {
		this.selected = selected;
	}

    public MatrixInfo getMatrixInfo() {
        return matrixInfo;
    }

    public String getLastResultExecutionTime() {
        return StatisticsUtils.getLastResultExecutionTime(lastResult);
    }

    public String msToMinutesSeconds(long duration) {
        return DurationFormatUtils.formatDuration(duration, "mm:ss");
    }

    public boolean isExportWithTCs() {
        return exportWithTCs;
    }

    public void setExportWithTCs(boolean exportWithTCs) {
        this.exportWithTCs = exportWithTCs;
    }

    public boolean isExportWithActions() {
        return exportWithActions;
    }

    public void setExportWithActions(boolean exportWithActions) {
        this.exportWithActions = exportWithActions;
    }

    public boolean isDownloadReportsEnabled() {
        return downloadReportsEnabled;
    }

    public boolean isIncludeExecutedInFromToRange() {
        return includeExecutedInFromToRange;
    }

    public void setIncludeExecutedInFromToRange(boolean includeExecutedInFromToRange) {
        this.includeExecutedInFromToRange = includeExecutedInFromToRange;
    }

    public List<AggregatedReportRow> getSelectedRows() {
        return selectedRows;
    }

    public void setSelectedRows(List<AggregatedReportRow> selectedRows) {
        this.selectedRows = selectedRows;
    }

    public List<Tag> getCustomerTags() {
        return customerTags;
    }

    public void setCustomerTags(List<Tag> customerTags) {
        this.customerTags = customerTags;
    }

    public Tag getCustomTagToAdd() {
        return customTagToAdd;
    }

    public void setCustomTagToAdd(Tag customTagToAdd) {
        this.customTagToAdd = customTagToAdd;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public Action[] getActions() {
       return Action.values();
    }

    public List<Tag> getAllCustomerTags() {
        return allCustomerTags;
    }

    public void onCustomTagSelect() {
        logger.debug("Custom tag select invoked");

        customerTags.add(customTagToAdd);
        this.customTagToAdd = null;
        allCustomerTags.removeAll(customerTags);
    }

    public void removeCustomTag(Tag tag) {
        customerTags.remove(tag);
        allCustomerTags.add(tag);
    }

    public List<Tag> completeCustomTag(String query) {
        return completeTag(query, allCustomerTags);
    }

    private void manageTagForRows(List<AggregatedReportRow> rows, Action action, List<Tag> tags) {
        try {
            List<AggregatedReportRow> filteredRows = new ArrayList<>(rows);
            for (AggregatedReportRow row : rows) {
                if (row.isMatrixRow()) {
                    filteredRows.removeAll(row.getTestCaseRows());
                }
            }
            BeanUtil.getSfContext().getStatisticsService().manageTagToRows(filteredRows, action.getTargetAction(), tags);
            BeanUtil.addInfoMessage("Action applied", "");
        } catch(Throwable e) {
            logger.error(e.getMessage(), e);
            BeanUtil.addErrorMessage(e.getMessage(), "");
        }
    }
}
