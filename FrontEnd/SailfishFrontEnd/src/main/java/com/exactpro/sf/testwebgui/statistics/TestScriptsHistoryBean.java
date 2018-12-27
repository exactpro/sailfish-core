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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.util.StringUtil;
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
import com.exactpro.sf.testwebgui.servlets.ReportServlet;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ManagedBean(name="mrHistBean")
@ViewScoped
@SuppressWarnings("serial")
public class TestScriptsHistoryBean extends AbstractStatisticsBean implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger(TestScriptsHistoryBean.class);

	private static final String REPORT_FILE_PREFIX = "Script_runs_history";

	private static final String OPTIONS_STORAGE_PREFIX = "mrHistBean.";

	private boolean statisticsDbAvailable;

	@SessionStored
	private TestCasesDisplayMode tcDisplayMode = TestCasesDisplayMode.AllNatural;

	private Date loadedFrom;

	private Date loadedTo;

	private List<AggregatedReportRow> lastResult = new ArrayList<>();

	private String customReportsPath = null;

	private AggregatedReportRow selectedRow;

	private AggregatedReportRow selected;

	private List<TestCaseRunStatus> allRunStatuses;

	@SessionStored
    private String[] selectedColumns = StatisticsUtils.availableScriptRunHistoryColumns;

	@SessionStored
	private boolean renderExpaned = false;

	private Set<Long> manualyToggled = new HashSet<>();

	@SessionStored
	private boolean emptyCommentOnly = false;

	@SessionStored
	private String sortBy;

	@SessionStored
	private boolean sortAsc = true;

	@SessionStored
	private boolean splitByStatus = false;

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

		for(AggregatedReportRow row : this.lastResult) {

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

		this.lastResult = new ArrayList<>(this.lastResult.size());

		this.lastResult.addAll(failed);
		this.lastResult.addAll(bad);
		this.lastResult.addAll(casual);
		this.lastResult.addAll(good);

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

		int matrixRowIndex = this.lastResult.indexOf(matrixRow);

		if(matrixRowIndex < 0) {
			return result;
		}

		AggregatedReportRow row;

		for(int i = matrixRowIndex + 1; i < this.lastResult.size(); i++) {

			row = this.lastResult.get(i);

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

		this.statisticsDbAvailable = BeanUtil.getSfContext().getStatisticsService().isConnected();

		if(this.statisticsDbAvailable) {

			this.allRunStatuses = BeanUtil.getSfContext().getStatisticsService().getStorage().getAllRunStatuses();

			this.mapper = new ObjectMapper();

		}

		restoreOptions();

		logger.debug("TestScriptsHistoryBean [{}] constructed", hashCode());

	}

	public void copyUserComments(AggregatedReportRow row) {

		int rowIndex = this.lastResult.indexOf(row);

		AggregatedReportRow otherRow;

		for(int i = rowIndex -1; i > 0; i--) {

			otherRow = this.lastResult.get(i);

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

		if(this.selectedRow.isMatrixRow()) {

			this.userComments = new TestCaseRunComments();

		} else {

			this.userComments = new TestCaseRunComments(this.selectedRow.getUserComments());
		}

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

		if(this.manualyToggled.contains(id)) {
			this.manualyToggled.remove(id);
		} else {
			this.manualyToggled.add(id);
		}

	}

	public boolean isRenderExpanded(AggregatedReportRow row) {

		if(renderExpaned) {

			return !this.manualyToggled.contains(row.getMatrixRunId());

		} else {

			return this.manualyToggled.contains(row.getMatrixRunId());

		}

	}

	public boolean isColumnSelected(String column) {

		for(String selected : this.selectedColumns) {

			if(column.equals(selected)) {

				return true;

			}

		}

		return false;

	}

	public void updateUserComments() {

		List<AggregatedReportRow> rowsToUpdate = new ArrayList<>();

		if(this.selectedRow.isMatrixRow()) {
			rowsToUpdate = findTcRowsForMatrixRun(this.selectedRow);
		} else {
			rowsToUpdate.add(selectedRow);
		}

		try {

			for(AggregatedReportRow row : rowsToUpdate) {

				if(this.selectedRow.isMatrixRow() && commentOnlyFailedTcs && row.getStatus() != StatusType.FAILED) {
					continue;
				}

				row.setUserComments(userComments);

				BeanUtil.getSfContext().getStatisticsService().getStorage()
					.updateTcrUserComments(row.getTestCaseRunId(), this.userComments);

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

			optStorage.setOption(OPTIONS_STORAGE_PREFIX + "customReportsPath", this.customReportsPath);

			BeanUtil.addInfoMessage("Applied", "But you have to generate report again");

		} catch(Exception e) {

			logger.error(e.getMessage(), e);

			BeanUtil.addErrorMessage("Apply failed", e.getMessage());

		}

	}

    public String getReportRequest(AggregatedReportRow row) {
        StringBuilder sb = new StringBuilder(getContextPath(false));

        sb.append("/report.xhtml?report=");
        sb.append(buildReportUrl(row, true));

        return sb.toString();
    }

    public String getZipReport(AggregatedReportRow row) {
        StringBuilder sb = new StringBuilder(getContextPath(false));

        sb.append("/report/");
        sb.append(row.getReportFolder());
        sb.append("?action=simplezip");

        return sb.toString();
    }

    public String getLastResultZipReports() {
        StringBuilder sb = new StringBuilder(getContextPath(true));

        sb.append("/report/reports?reports=");
        sb.append(this.lastResultReportNamesJson);

        return sb.toString();
    }

    private String getContextPath(boolean button) {
        return StringUtils.isEmpty(this.customReportsPath) ? (button ? StringUtils.EMPTY : FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath())
                : customReportsPath.substring(0, customReportsPath.lastIndexOf("/"));
    }

	public String buildReportUrl(AggregatedReportRow row, boolean report) {

		StringBuilder sb = new StringBuilder();

        if(StringUtils.isNotEmpty(this.customReportsPath)) {
            sb.append(this.customReportsPath).append("/");
        } else {
            String host;
            Integer port;
            String name;
            if (row.getSfCurrentInstance() != null) {
                SfInstance sfCurrentInstance = row.getSfCurrentInstance();
                host = sfCurrentInstance.getHost();
                port = sfCurrentInstance.getPort();
                name = sfCurrentInstance.getName();
            } else {
                host = row.getHost();
                port = row.getPort();
                name = row.getSfName();
            }
            sb.append("http://");
            sb.append(host)
                    .append(":")
                    .append(port)
                    .append(name)
                    .append("/");
            sb.append(ReportServlet.REPORT_URL_PREFIX + "/"); // see web.xml > servlet-mapping
        }

		sb.append(StringUtil.escapeURL(row.getReportFolder())).append("/");

		if(report) {

			sb.append(row.getReportFile()).append(".html");

		} else {

			sb.append(StringUtil.escapeURL(row.getMatrixName()));

		}

		return sb.toString();

	}

    @PreDestroy
    public void destroy() {
        logger.debug("TestScriptsHistoryBean [{}] destroy", hashCode());
    }


	public TestCasesDisplayMode[] getDisplayModes() {

		return TestCasesDisplayMode.values();

	}

	public String getExportFileName() {

		if(this.lastResult == null || this.lastResult.isEmpty()) {

			return "";

		}

		DateFormat format = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");

		StringBuilder sb = new StringBuilder();

		sb.append(REPORT_FILE_PREFIX)
		.append("__")
		.append(format.format(this.loadedFrom))
		.append("__")
		.append(format.format(this.loadedTo))
		.append(".csv");

		return sb.toString();

	}

	public void generateReport() {

		logger.debug("Generate {} - {}; {}", this.from, this.to, this.selectedSfInstances);

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
            this.matrixInfo = MatrixInfo.extractMatrixInfo(this.lastResult);


			if(this.splitByStatus) {

				doSplitByStatus();

			}

			this.loadedFrom = from;
			this.loadedTo = to;

			List<String> reports = new ArrayList<>();

	        for (AggregatedReportRow row : this.lastResult) {
	            if (row.isMatrixRow()) {
	                reports.add(row.getReportFolder());
	            }
	        }

	        this.lastResultReportNamesJson = this.mapper.writeValueAsString(reports);
	        this.downloadReportsEnabled = true;

		} catch(Exception e) {

			logger.error(e.getMessage(), e);

			BeanUtil.addErrorMessage(e.getMessage(), "");

			if (e instanceof JsonProcessingException) {
			    this.downloadReportsEnabled = false;
			}

		}

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
}
