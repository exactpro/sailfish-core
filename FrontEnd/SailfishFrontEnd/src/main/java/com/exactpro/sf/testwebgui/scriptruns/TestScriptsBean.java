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
package com.exactpro.sf.testwebgui.scriptruns;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.context.RequestContext;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.ReorderEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.embedded.statistics.StatisticsService;
import com.exactpro.sf.embedded.statistics.entities.Tag;
import com.exactpro.sf.matrixhandlers.MatrixProviderHolder;
import com.exactpro.sf.scriptrunner.ReportWriterOptions.Duration;
import com.exactpro.sf.scriptrunner.TestScriptDescription;
import com.exactpro.sf.scriptrunner.reportbuilder.ReportType;
import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.SFWebApplication;
import com.exactpro.sf.testwebgui.api.TestToolsAPI;
import com.exactpro.sf.testwebgui.notifications.scriptrunner.ScriptrunUpdateRetriever;
import com.google.common.collect.ImmutableList;

import net.sf.json.JSONArray;

@ManagedBean(name="testScriptsBean")
@ViewScoped
@SuppressWarnings("serial")
public class TestScriptsBean implements Serializable, IView {


    private static final Logger logger = LoggerFactory.getLogger(TestScriptsBean.class);

    @ManagedProperty(value="#{scriptRunsBean}")
	private ScriptRunsBean scriptRunsBean;

    public static final List<String> ENCODE_VALUES = ImmutableList.of("UTF-8", "ISO-8859-1");

    private static final String VALIDATION_FAILED = "validationFailed";

    private static final String TEST_SCRIPTS_RUNNING = "isRunningScripts";

    private static final String SELECTED_TEST_SCRIPTS = "selectedTestScripts";

    private static final String REMOVE_FROM_DISK_BOOL_VAR = "fromDiskBool";

    private static final String EXCEL_EDITOR = "exceleditor.xhtml";

    private static final String SCRIPTRUNS_XHTML = "scriptruns.xhtml";

    private static final String PLAIN_EDITOR = "plaineditor.xhtml";

    private static final String GUI_EDITOR = "guieditor.xhtml";

    private static final String UNICODE_ARROW_ASCENDING = "\u25b2";
    private static final String UNICODE_ARROW_DESCENDING = "\u25bc";

    private static final String DEFAULT_SORT_ORDER = UNICODE_ARROW_DESCENDING + " time";

    private boolean continueOnFailed;
    private boolean autoStart;
    private boolean ignoreAskForContinue;
    private boolean tryReloadBeforeStart;
    private boolean runNetDumper;
    private boolean skipOptional;
    private String selectedEnvironment;
	private SailfishURI selectedLanguageURI;
	private String selectedEncoding;
	private List<MatrixAdapter> matrixAdapterList;

	private String newFileName;

	private List<Long> selectedMatrices;
	private List<Long> filteredMatrices;

	private MatrixAdapter matrixToEdit;
	private Date dateFilter;
	private Date dateFilterFrom;

    private Date dateFilterTo;
	private String filterResultName;

    private Set<String> allResultStatuses;
	private Set<String> selectedResultStatuses;

	private List<Tag> tags;

	private List<Tag> allTags;

	private Tag tagToAdd;

	private boolean statisticsEnabled = false;

	private Map<String, ReportType> mapReportTypes;
	private Date startReportDate;
	private Date endReportDate;
	private boolean printReportDetails;
	private Duration reportDuration;
	private String reportType;

    private Map<String, Comparator<TestScriptDescription>> comparators;
    private String sortBy;

	public Set<SailfishURI> getLanguageURIs() {
		return SFLocalContext.getDefault().getLanguageManager().getLanguageURIs();
	}

	public TestScriptsBean() {
		selectedMatrices = new ArrayList<>();
		filteredMatrices = new ArrayList<>();
        allResultStatuses = new HashSet<>();

        allResultStatuses.addAll(
                Arrays.stream(TestScriptDescription.ScriptStatus.values()).map(Enum::name)
                        .filter(i -> !i.equals(TestScriptDescription.ScriptStatus.EXECUTED.name())).collect(Collectors.toSet()));

        allResultStatuses.add("EXECUTED SUCCESS");
        allResultStatuses.add("EXECUTED WITH ERRORS");

        allResultStatuses.addAll(Arrays.stream(TestScriptDescription.ScriptState.values()).map(Enum::name).collect(Collectors.toSet()));

        sortBy = DEFAULT_SORT_ORDER;
        comparators = new HashMap<>();
        comparators.put(DEFAULT_SORT_ORDER, Comparator.comparing(TestScriptDescription::getTimestamp).reversed());

        comparators.put(UNICODE_ARROW_ASCENDING + " time", Comparator.comparing(TestScriptDescription::getTimestamp));
        comparators.put(UNICODE_ARROW_DESCENDING + " id", Comparator.comparing(TestScriptDescription::getId).reversed());
        comparators.put(UNICODE_ARROW_ASCENDING + " id", Comparator.comparing(TestScriptDescription::getId));
	}

	@PostConstruct
	public void init() {
		this.continueOnFailed = scriptRunsBean.isContinueOnFailed();
		this.autoStart = scriptRunsBean.isAutoStart();
		this.ignoreAskForContinue = scriptRunsBean.isIgnoreAskForContinue();
		this.tryReloadBeforeStart = scriptRunsBean.isTryReloadBeforeStart();
		this.runNetDumper = scriptRunsBean.isRunNetDumper();
		this.skipOptional = scriptRunsBean.isSkipOptional();
		this.selectedEnvironment = scriptRunsBean.getSelectedEnvironment();
		this.selectedLanguageURI = scriptRunsBean.getSelectedLanguageURI();
		this.selectedEncoding = scriptRunsBean.getSelectedEncoding();
		this.matrixAdapterList = scriptRunsBean.getMatrixAdapterList();
		this.tags = new ArrayList<>(scriptRunsBean.getTags());

		this.selectedResultStatuses = scriptRunsBean.getSelectedStatuses() != null ? new HashSet<>(scriptRunsBean.getSelectedStatuses()) : new HashSet<>(allResultStatuses);
        this.sortBy = scriptRunsBean.getSortBy() != null ? scriptRunsBean.getSortBy() : DEFAULT_SORT_ORDER;
        this.filterResultName = scriptRunsBean.getTestResultFilterString();
        this.dateFilterFrom = scriptRunsBean.getDateFrom();
		this.dateFilterTo = scriptRunsBean.getDateTo();

		for (MatrixAdapter matrixAdapter: matrixAdapterList) {
			matrixAdapter.setView(this);
		}

		ReportType[] reportTypes = ReportType.values();
		this.mapReportTypes = new LinkedHashMap<>();
		for (ReportType reportType : reportTypes) {
		    this.mapReportTypes.put(reportType.getShortName(), reportType);
        }

		try {
    		StatisticsService statisticsService = BeanUtil.getSfContext().getStatisticsService();

    		if (statisticsService !=null && statisticsService.isConnected()) {

    			this.statisticsEnabled = true;
    			this.allTags = statisticsService.getStorage().getAllTags();

    		}
		} catch (RuntimeException e) {
		    BeanUtil.showMessage(FacesMessage.SEVERITY_WARN, "Error loading tag list for statistic: " + e.getMessage(), "");
            logger.error(e.getMessage(), e);
		}

	}

    public boolean reloadMatrix(MatrixAdapter matrixAdapter, boolean reloadBool) {

        if (!reloadBool) {
            return true;
        }

        if (matrixAdapter.getReloadEnabled()) {
            try {
                MatrixUtil.reloadMatrixByLink(
                        BeanUtil.getSfContext().getMatrixStorage(),
                		BeanUtil.findBean(BeanUtil.MATRIX_PROVIDER_HOLDER, MatrixProviderHolder.class),
                		matrixAdapter.getIMatrix());
                matrixAdapter = getMatrixAdapterById(matrixAdapter.getMatrixId());
            } catch (Exception e) {
                BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error reloading matrix " + matrixAdapter.getName() +
                        " :" + e.getMessage(), "");
                logger.error(e.getMessage(), e);
                return false;
            }
        }
        return true;
    }

    public List<Tag> completeTag(String query) {
    	List<Tag> result = new ArrayList<>();
    	String loweredQuery = query.toLowerCase();

    	for (Tag tag : this.allTags) {
    		if (tag.getName().toLowerCase().contains(loweredQuery)) {

    			result.add(tag);

    		}
    	}
    	return result;
    }

    public void onTagSelect() {
    	logger.debug("Tag select invoked");

    	this.tags.add(tagToAdd);
    	this.tagToAdd = null;
    	this.allTags.removeAll(tags);

    	saveSelectedTags();
    }

    public void removeTag(Tag tag) {
    	this.tags.remove(tag);
    	this.allTags.add(tag);

    	saveSelectedTags();
    }

    private void saveSelectedTags() {
    	this.scriptRunsBean.setTags(new ArrayList<>(tags));
    }

	private void enqueue(MatrixAdapter matrixAdapter, boolean autoRun) throws FileNotFoundException, IOException {
		logger.debug("Before adding matrix {} to queue...", matrixAdapter);

        if (!reloadMatrix(matrixAdapter, tryReloadBeforeStart)) {
            return;
        }

		String currentRange = matrixAdapter.getRange();
		long enqueuedID = TestToolsAPI.getInstance().executeMatrix(
				matrixAdapter.getIMatrix(), this.selectedLanguageURI, currentRange,
				this.selectedEncoding, this.selectedEnvironment,
				BeanUtil.getUser(), this.continueOnFailed, this.autoStart,
				autoRun, this.ignoreAskForContinue, runNetDumper, skipOptional, new ArrayList<>(this.tags), null, null, null);

        if (logger.isInfoEnabled()) {
            String tagsString = this.tags == null ? null : Arrays.toString(this.tags.toArray());
            logger.info(
                "Test Script {} was enqueued under {}. Script properties [selectedLanguage : [{}], range : [{}], " +
                "selectedEncoding : [{}], selectedEnvironment : [{}], user : [{}], continueOnFailed : [{}], " +
                "autoStart : [{}], autoRun : [{}], ignoreAskForContinue : [{}], tags : [{}], " +
                "staticVariables : [null], userListeners : [null], subFolder : [null]",
                    matrixAdapter.getIMatrix(), enqueuedID,  selectedLanguageURI, currentRange,
                    this.selectedEncoding, this.selectedEnvironment,
                    BeanUtil.getUser(), this.continueOnFailed, this.autoStart,
                    autoRun, this.ignoreAskForContinue, tagsString);

        }

		matrixAdapter.setLastScriptRun(enqueuedID);
	}

	public void preRenderView() {

		ISFContext context = BeanUtil.getSfContext();
		if (context == null) {
			BeanUtil.addErrorMessage("SFContext error", "SFContext is not initialized correctly. See log file for details.");
			return;
		}

		List<String> envs = context.getConnectionManager().getEnvironmentList();
		Map<String, String> environmentValues = new TreeMap<>();
		for (String env : envs) {
		    environmentValues.put(env, env);
		}
        this.scriptRunsBean.setEnvironmentValues(environmentValues);
	}

	private String packZip() throws IOException {

		Map<String, Integer> nameCounter = new HashMap<>();
		File temp = File.createTempFile("matrix.zip", Long.toString(System.currentTimeMillis()));
		String path = temp.getAbsolutePath();
		ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(temp));
		zip.setLevel(Deflater.DEFAULT_COMPRESSION);

		byte[] buf = new byte[1024];
		for (Long matrixId : selectedMatrices) {
			MatrixAdapter matrixAdapter = getMatrixAdapterById(matrixId);
			String name = matrixAdapter.getName();
			if (nameCounter.containsKey(name)) {
				Integer count = nameCounter.get(name);
				count++;
				nameCounter.put(name, count);
				name = "(" + count + ") " + name;
			} else {
				nameCounter.put(name, 0);
			}
			zip.putNextEntry(new ZipEntry(name));
			try (InputStream fis = matrixAdapter.readStream()) {
				int len;
				while ((len = fis.read(buf)) > 0) {
					zip.write(buf, 0, len);
				}
			}
			zip.closeEntry();
		}

		zip.flush();
		zip.close();

		return path;

	}

	public StreamedContent getResultsInCSV() {
		try {
			logger.debug("getResultsInCSV invoked {}", BeanUtil.getUser());

			ReportType reportType = this.mapReportTypes.get(this.reportType);

			String fileName = SFLocalContext.getDefault().getReportWriter().getFileName(reportType);

            File reportFile = TestToolsAPI.getInstance().createAggrigateReport(fileName, this.startReportDate, this.endReportDate,
                    this.printReportDetails, this.reportDuration, reportType);

			return new DefaultStreamedContent(new FileInputStream(reportFile), "text/csv", fileName);

		} catch(Exception e) {
			logger.error("Could not create csv file", e);
			return null;
		}
	}

	public String getCurrentRunsSnapshot() {
		logger.debug("getCurrentRunsSnapshot invoked {}", BeanUtil.getUser());
		ScriptrunUpdateRetriever retriever = (ScriptrunUpdateRetriever)SFWebApplication.getInstance().getScriptrunsUpdateRetriever();
        return retriever.getCurrentStateSnapshot(comparators.get(sortBy));
	}

	public void handleFileUpload(FileUploadEvent event) {

		logger.debug("Upload invoked {}", BeanUtil.getUser());

		UploadedFile uploadedFile = event.getFile() ;

		try {
			TestToolsAPI.getInstance().uploadMatrix(uploadedFile.getInputstream(), uploadedFile.getFileName(), null, "Unknown creator", null, null, null);
		} catch (Exception e) {
			logger.error("Could not store uploaded file", e);
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", "Could not store uploaded file " + e.getMessage() );
			return;
		}

		logger.debug("Upload finished");
    }

	public void createNewScript() {
		logger.debug("createNewScript invoked {}", BeanUtil.getUser());
		try {
			TestToolsAPI.getInstance().uploadMatrix(null, this.newFileName.trim() + ".csv", null, "Unknown creator", null, null, null);
		} catch (Exception e) {
			logger.error("Could not create new script file", e);
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Could not create new script file", e.getMessage() );
			return;
		}
		this.newFileName = "";
	}

	public boolean isNotOneSelected() {
		logger.debug("isNotOneSelected invoked {}", BeanUtil.getUser());
		if (selectedMatrices == null ) {
			return true;
		}
		return selectedMatrices.size() != 1;
	}

	public void run(MatrixAdapter matrixAdapter) throws FileNotFoundException, IOException {
		logger.debug("run invoked {} id[{}] range[{}]", BeanUtil.getUser(), matrixAdapter.getMatrixId(), matrixAdapter.getRange());
		onChange(matrixAdapter);
		enqueue(matrixAdapter, true);
	}

	/**
	 * RequestParams:
	 * 		id - matrixId
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws SailfishURIException
	 */
    public void runTestCaseToEdit() throws FileNotFoundException, IOException, SailfishURIException {
        long id = Long.parseLong(BeanUtil.getRequestParam("id"));
        String testCaseToRun = BeanUtil.getRequestParam("testcaseToEdit");
		logger.debug("runTestCaseToEdit invoked {} id[{}] testCase[{}]", BeanUtil.getUser(), id, testCaseToRun);


        MatrixAdapter matrixAdapter = getMatrixAdapterById(id);
        if (matrixAdapter == null) {
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Matrix not found", "");
            return;
        }

        if (BeanUtil.getRequestParam("contIfFailed") != null) {
        	this.continueOnFailed = Boolean.valueOf(BeanUtil.getRequestParam("contIfFailed"));
        	matrixAdapter.setContinueOnFailed(this.continueOnFailed);

        }

        if (BeanUtil.getRequestParam("autoStart") != null) {
        	this.autoStart = Boolean.valueOf(BeanUtil.getRequestParam("autoStart"));
        	matrixAdapter.setAutoStart(this.autoStart);
        }

        if (BeanUtil.getRequestParam("ignoreAskForContinue") != null) {
        	this.ignoreAskForContinue = Boolean.valueOf(BeanUtil.getRequestParam("ignoreAskForContinue"));
        }

        if (BeanUtil.getRequestParam("language") != null) {
        	this.selectedLanguageURI = SailfishURI.parse(BeanUtil.getRequestParam("language"));
        }

        if (BeanUtil.getRequestParam("encoding") != null) {
        	this.selectedEncoding = BeanUtil.getRequestParam("encoding");
        }

        if (BeanUtil.getRequestParam("environment") != null) {
        	this.selectedEnvironment = BeanUtil.getRequestParam("environment");
        }


        if (testCaseToRun.length() > 0) {
			//only one :
			if (testCaseToRun.indexOf(":") != testCaseToRun.lastIndexOf(":")) {
				String number = testCaseToRun.substring(0, testCaseToRun.indexOf(":"));
				matrixAdapter.setRange(number);
			} else {
				BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "This block can not be run", "Only testCases are able to execute");
				return;
			}
        }

        enqueue(matrixAdapter, true);
		matrixAdapter.setRange("");
    }

	public void queue() throws FileNotFoundException, IOException {

		long id = Long.parseLong(BeanUtil.getRequestParam("id"));
		logger.debug("queue invoked {} id[{}]", BeanUtil.getUser(), id);

		MatrixAdapter matrix = getMatrixAdapterById(id);

		if (matrix == null) {
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Matrix not found", "");
			return;
		}

		enqueue(matrix, false);
	}

	public void runSelected() throws FileNotFoundException, IOException {
		logger.debug("runSelected invoked {}", BeanUtil.getUser());
		for (Long matrixId : selectedMatrices) {
			MatrixAdapter matrix = getMatrixAdapterById(matrixId);
			enqueue(matrix, true);
		}
	}

	public void runAll() throws FileNotFoundException, IOException {
		logger.debug("runAll invoked {}", BeanUtil.getUser());
		for (MatrixAdapter matrix : matrixAdapterList) {
			enqueue(matrix, true);
		}
	}

	public void delete() {
		logger.debug("delete invoked {}", BeanUtil.getUser());
		for (Long matrixId : selectedMatrices) {
			MatrixAdapter matrixAdapter = getMatrixAdapterById(matrixId);
			TestToolsAPI.getInstance().deleteMatrix(matrixAdapter.getIMatrix());
		}
		selectedMatrices.clear();
		syncMatrixAdapterList();
	}

	public void deleteAllFiles() {
		logger.debug("deleteAllFiles invoked {}", BeanUtil.getUser());
		selectedMatrices.clear();
		TestToolsAPI.getInstance().deleteAllMatrix();
	}

    public void clearAllTestScriptsDescSafe() {
		logger.debug("clearAllTestScriptsDescSafe invoked {}", BeanUtil.getUser());
        SFLocalContext.getDefault().getScriptRunner().removeAllTestScripts(false);
    }


    public void reloadAllTestScript() {
		logger.debug("reloadAllTestScript invoked {}", BeanUtil.getUser());
        if (isRunningScriptsInternal()) {
            RequestContext context = RequestContext.getCurrentInstance();
            context.addCallbackParam(TEST_SCRIPTS_RUNNING, true);
        } else{
            clearAllTestScriptsDescSafe();
            SFLocalContext.getDefault().getScriptRunner().testScriptsInitFromWD();
        }
    }

	public void clearSelectedTestScript() {
		String params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(SELECTED_TEST_SCRIPTS);
		String fromDisk = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(REMOVE_FROM_DISK_BOOL_VAR);
		logger.debug("clearSelectedTestScript invoked {} params[{}], fromDisk[{}]", BeanUtil.getUser(), params, fromDisk);
		String[] selectedTestScripts = params.split(",");
		List<Long> testScriptIds = new ArrayList<>();
		for (String selectedTestScript: selectedTestScripts) {
			if (StringUtils.isNotEmpty(selectedTestScript)) {
				testScriptIds.add(Long.valueOf(selectedTestScript));
			}
		}
        List<TestScriptDescription> errors = SFLocalContext.getDefault().getScriptRunner().removeTestScripts(Boolean.valueOf(fromDisk), testScriptIds);
		StringBuilder stringBuilder;
        for (TestScriptDescription error : errors) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Report ");
            stringBuilder.append(error.getWorkFolder());
            stringBuilder.append(" is locked. Try to remove later.");
            BeanUtil.addErrorMessage("Failed to remove report", stringBuilder.toString());
        }

    }

    public void isRunningScripts() {
		logger.debug("isRunningScripts invoked {}", BeanUtil.getUser());
        if (isRunningScriptsInternal()) {
            RequestContext context = RequestContext.getCurrentInstance();
            context.addCallbackParam(TEST_SCRIPTS_RUNNING, true);
        }
    }

	public void onSafeBtnClick() {
		logger.debug("onSafeBtnClick invoked {}", BeanUtil.getUser());
		if (isRunningScriptsInternal()) {
			RequestContext context = RequestContext.getCurrentInstance();
			context.addCallbackParam(TEST_SCRIPTS_RUNNING, true);
		}
		else {
			clearAllTestScriptsDescSafe();
		}
	}

	public void stopScript() {
		logger.debug("stopScript() invoked: {}", BeanUtil.getRequestParam("id"));
//		ScriptRunner.getDefault().doShutdown();
		long id = Long.parseLong(BeanUtil.getRequestParam("id"));
		TestToolsAPI.getInstance().stopScriptRun(id);
	}

	public void stopAllScript() {
		logger.debug("stopAllScript() invoked {}", BeanUtil.getUser());
        SFLocalContext.getDefault().getScriptRunner().stopAllScript();
	}

	public void pauseAllScript() {
		logger.debug("pauseAllScript() invoked {}", BeanUtil.getUser());
        SFLocalContext.getDefault().getScriptRunner().pauseAllScript();
	}

	public void resumeAllScript() {
		logger.debug("resumeAllScript() invoked {}", BeanUtil.getUser());
        SFLocalContext.getDefault().getScriptRunner().resumeAllScript();
	}

	public void compileScript() {
		long id = Long.parseLong(BeanUtil.getRequestParam("id"));
		logger.debug("compileScript() invoked: {}", id);
        SFLocalContext.getDefault().getScriptRunner().compileScript(id);
	}

	public void runCompiledScript() {
		long id = Long.parseLong(BeanUtil.getRequestParam("id"));
		logger.debug("runCompiledScript() invoked: {}", id);
        SFLocalContext.getDefault().getScriptRunner().runCompiledScript(id);
	}

	public void resumeScript() {
		long id = Long.parseLong(BeanUtil.getRequestParam("id"));
		logger.debug("resumeScript() invoked: {}", id);
        SFLocalContext.getDefault().getScriptRunner().resumeScript(id);
	}

	public void pauseScript() {
		long id = Long.parseLong(BeanUtil.getRequestParam("id"));
		logger.debug("pauseScript() invoked: {}", id);
        SFLocalContext.getDefault().getScriptRunner().pauseScript(id);
	}

	public void nextStep() {
		long id = Long.parseLong(BeanUtil.getRequestParam("id"));
		logger.debug("nextStep() invoked: {}", id);
        SFLocalContext.getDefault().getScriptRunner().nextStep(id);
	}

	public StreamedContent getMatrixInZip() {
		logger.debug("getMatrixInZip invoked {}", BeanUtil.getUser());
		try {
			InputStream stream = new FileInputStream(packZip());
			return new DefaultStreamedContent(stream, "application/zip", "matrix.zip");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage());
		}

		return null;

	}

    public void onRowReorder(ReorderEvent event) {
		//Element position change primefaces in this bean
		int from = event.getFromIndex();
		int to = event.getToIndex();
		logger.debug("onRowReorder invoked {} from[{}], to[{}]", BeanUtil.getUser(), from, to);
		scriptRunsBean.onRowReorder(from, to);

		this.matrixAdapterList = scriptRunsBean.getMatrixAdapterList();
		for (MatrixAdapter matrixAdapter: matrixAdapterList) {
			matrixAdapter.setView(this);
		}
	}

    public void loadNewFileName(String idString)
    {
        long id = Long.parseLong(idString);
		logger.debug("getMatrixInZip invoked {} id[{}]", BeanUtil.getUser(), id);
        MatrixAdapter matrixAdapter = getMatrixAdapterById(id);

        if (matrixAdapter == null) {
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Matrix not found", "");
            return;
        }
        newFileName = matrixAdapter.getName();
        setMatrixToEdit(matrixAdapter);
    }

	public void renameMatrix() {
		logger.info("renameMatrix invoked {} matrixToEdit[{}]", BeanUtil.getUser(), matrixToEdit);

        MatrixAdapter matrixAdapter = matrixToEdit;

        if (matrixAdapter == null) {
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Matrix not found", "");
            return;
        }
		try {
			matrixAdapter.setName(newFileName);
			BeanUtil.getSfContext().getMatrixStorage().updateMatrix(matrixAdapter.getIMatrix());
            newFileName = "";
            BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Info", "Matrix was renamed");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage());
			RequestContext context = RequestContext.getCurrentInstance();
			context.addCallbackParam(VALIDATION_FAILED, true);
		}

	}

	public void goEditPlainMatrix() {

		long id = Long.parseLong(BeanUtil.getRequestParam("id"));
		logger.info("goEditPlainMatrix invoked {} id[{}]", BeanUtil.getUser(), id);

		MatrixAdapter matrixAdapter = getMatrixAdapterById(id);

		if (matrixAdapter == null) {
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Matrix not found", "");
			return;
		}
		try {
			onChange(matrixAdapter);
			setMatrixToEdit(matrixAdapter);
			FacesContext.getCurrentInstance().getExternalContext().redirect(PLAIN_EDITOR);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage());
		}
	}

	public void goEditGuiMatrix()
	{
		long id = Long.parseLong(BeanUtil.getRequestParam("id"));
		logger.info("goEditGuiMatrix invoked {} id[{}]", BeanUtil.getUser(), id);

		MatrixAdapter matrixAdapter = getMatrixAdapterById(id);

		if (matrixAdapter == null) {
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Matrix not found", "");
			return;
		}
		/*if (MatrixFileTypes.detectFileType(matrixAdapter.getFilePath()) != MatrixFileTypes.CSV) {
			BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Info", "Only CSV matrixes can be opened by GUI Editor");
			return;
		}*/
		try {
			onChange(matrixAdapter);
			setMatrixToEdit(matrixAdapter);
			FacesContext.getCurrentInstance().getExternalContext().redirect(GUI_EDITOR);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage());
		}
	}

	public void goEditMatrix() {

		long id = Long.parseLong(BeanUtil.getRequestParam("id"));
		logger.info("goEditMatrix invoked {} id[{}]", BeanUtil.getUser(), id);

		MatrixAdapter matrixAdapter = getMatrixAdapterById(id);

		if (matrixAdapter == null) {
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Matrix not found", "");
			return;
		}
		try {
			onChange(matrixAdapter);
			setMatrixToEdit(matrixAdapter);
			FacesContext.getCurrentInstance().getExternalContext().redirect(EXCEL_EDITOR);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage());
		}

	}

	public void goToScriptRuns() {
		logger.debug("goToScriptRuns invoked {}", BeanUtil.getUser());

		try {
			FacesContext.getCurrentInstance().getExternalContext().redirect(SCRIPTRUNS_XHTML);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

	}

    protected boolean isRunningScriptsInternal() {
		for (TestScriptDescription descr : SFLocalContext.getDefault().getScriptRunner().getDescriptions()) {
            if (descr.isLocked()) {
                return true;
            }
        }
        return false;
    }

    public boolean filterByDate(Object value, Object filter, Locale locale) {
		logger.debug("filterByDate invoked {} value[{}], filter[{}], locale[{}]", BeanUtil.getUser(), value, filter, locale);
        Date filterDate = (filter == null) ? null : (filter instanceof Date ? (Date)filter : null);
        Date valueDate = (value == null) ? null : (value instanceof Date ? (Date)value : null);

        if (filterDate == null || valueDate == null) {
            return true;
        }

        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");

        boolean isEqualsDate = format.format(filterDate).equals(format.format(valueDate));

        return isEqualsDate;
    }

	/* Simple getters?setters below */

	public boolean isContinueOnFailed() {
		return continueOnFailed;
	}

	public void setContinueOnFailed(boolean continueOnFailed) {
		logger.debug("setContinueOnFailed invoked {}", BeanUtil.getUser());
		this.continueOnFailed = continueOnFailed;
		scriptRunsBean.setContinueOnFailed(continueOnFailed);
	}

	public MatrixAdapter[] getSelectedMatrices() {
		List<MatrixAdapter> matrixList = matrixAdapterList;
		List<MatrixAdapter> selectedMatrixList = new ArrayList<>();
		for (MatrixAdapter matrix : matrixList) {
			if (selectedMatrices.contains(matrix.getMatrixId())) {
				selectedMatrixList.add(matrix);
			}
		}
		return selectedMatrixList.toArray(new MatrixAdapter[0]);
	}

	public void setSelectedMatrices(MatrixAdapter[] selectedMatrices) {
		logger.debug("setSelectedMatrices invoked {} selectedMatrices[{}]", BeanUtil.getUser(), selectedMatrices);
		this.selectedMatrices.clear();
		for (MatrixAdapter matrixAdapter: selectedMatrices) {
			this.selectedMatrices.add(matrixAdapter.getMatrixId());
		}
	}

	public List<Long> getFilteredMatrices() {
		return filteredMatrices;
	}

	public void setFilteredMatrices(List<Long> filteredMatrices) {
		this.filteredMatrices = filteredMatrices;
	}

	public MatrixAdapter getMatrixToEdit() {
		matrixToEdit = matrixToEdit == null ? getMatrixAdapterById(scriptRunsBean.getMatrixToEditId()) : matrixToEdit;
		return matrixToEdit;
	}

	public void setMatrixToEdit(MatrixAdapter matrixToEdit) {
		logger.debug("setMatrixToEdit invoked {} matrixToEdit[{}]", BeanUtil.getUser(), matrixToEdit);
		this.matrixToEdit = matrixToEdit;
		scriptRunsBean.setMatrixToEditId(matrixToEdit.getMatrixId());
	}

	public boolean isAutoStart() {
		return autoStart;
	}

	public void setAutoStart(boolean autoStart) {
		logger.debug("setAutoStart invoked {} autoStart[{}]", BeanUtil.getUser(), autoStart);
		this.autoStart = autoStart;
		scriptRunsBean.setAutoStart(autoStart);
	}

	public String getNewFileName() {
		return newFileName;
	}

	public void setNewFileName(String newFileName) {
		logger.debug("setNewFileName invoked {} newFileName[{}]", BeanUtil.getUser(), newFileName);
		this.newFileName = newFileName;
	}

	public SailfishURI getSelectedLanguageURI() {
		return selectedLanguageURI;
	}

	public void setSelectedLanguageURI(SailfishURI selectedLanguageURI) {
		logger.debug("setSelectedLanguageURI invoked {} selectedLanguageURI[{}]", BeanUtil.getUser(), selectedLanguageURI);
		this.selectedLanguageURI = selectedLanguageURI;
		scriptRunsBean.setSelectedLanguageURI(selectedLanguageURI);
	}

	public boolean isIgnoreAskForContinue() {
		return ignoreAskForContinue;
	}

	public void setIgnoreAskForContinue(boolean ignoreAskForContinue) {
		logger.debug("setIgnoreAskForContinue invoked {} ignoreAskForContinue[{}]", BeanUtil.getUser(), ignoreAskForContinue);
		this.ignoreAskForContinue = ignoreAskForContinue;
		scriptRunsBean.setIgnoreAskForContinue(ignoreAskForContinue);
	}

	public String getSelectedEncoding() {
		return selectedEncoding;
	}

	public void setSelectedEncoding(String selectedEncoding) {
		logger.debug("setSelectedEncoding invoked {} selectedEncoding[{}]", BeanUtil.getUser(), selectedEncoding);
		this.selectedEncoding = selectedEncoding;
		scriptRunsBean.setSelectedEncoding(selectedEncoding);
	}

	public List<String> getEncodeValues() {
		return ENCODE_VALUES;
	}

	public Set<String> getReportTypes() {
		return this.mapReportTypes.keySet();
	}

    public Duration[] getDurations() {
        return Duration.values();
    }

	public Date getStartReportDate() {
        return startReportDate;
    }

    public void setStartReportDate(Date startReportDate) {
        this.startReportDate = startReportDate;
    }

    public Date getEndReportDate() {
        return endReportDate;
    }

    public void setEndReportDate(Date endReportDate) {
        this.endReportDate = endReportDate;
    }

    public boolean isPrintReportDetails() {
        return printReportDetails;
    }

    public void setPrintReportDetails(boolean printReportDetails) {
        this.printReportDetails = printReportDetails;
    }

    public Duration getReportDuration() {
        return reportDuration;
    }

    public void setReportDuration(Duration reportDuration) {
        this.reportDuration = reportDuration;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

	public String getSelectedEnvironment() {
		return selectedEnvironment;
	}

	public void setSelectedEnvironment(String selectedEnvironment) {
		logger.debug("setSelectedEnvironment invoked {} selectedEnvironment[{}]", BeanUtil.getUser(), selectedEnvironment);
		this.selectedEnvironment = selectedEnvironment;
		scriptRunsBean.setSelectedEnvironment(selectedEnvironment);
	}

	public Map<String, String> getEnvironmentValues() {
		return scriptRunsBean.getEnvironmentValues();
	}

    public Date getDateFilter() {
        return dateFilter;
    }

    public void setDateFilter(Date dateFilter) {
		logger.debug("setDateFilter invoked {} dateFilter[{}]", BeanUtil.getUser(), dateFilter);
        this.dateFilter = dateFilter;
    }

    public Date getDateFilterFrom() {
        return dateFilterFrom;
    }

    public void setDateFilterFrom(Date dateFilterFrom) {
		logger.debug("setDateFilterFrom invoked {} dateFilterFrom[{}]", BeanUtil.getUser(), dateFilterFrom);
        this.dateFilterFrom = dateFilterFrom;
        scriptRunsBean.setDateFrom(dateFilterFrom);
    }

    public Date getDateFilterTo() {
        return dateFilterTo;
    }

    public void setDateFilterTo(Date dateFilterTo) {
		logger.debug("setDateFilterTo invoked {} dateFilterTo[{}]", BeanUtil.getUser(), dateFilterTo);
        this.dateFilterTo = dateFilterTo;
        scriptRunsBean.setDateTo(dateFilterTo);
    }

    public String getFilterResultName() {
        return filterResultName;
    }

    public void setFilterResultName(String filterResultName) {
		logger.debug("filterResultName invoked {} filterResultName[{}]", BeanUtil.getUser(), filterResultName);
        this.filterResultName = filterResultName;
        scriptRunsBean.setTestResultFilterString(filterResultName);
    }

    public Set<String> getAllResultStatuses() {
        return allResultStatuses;
    }

    public void setAllResultStatuses(Set<String> allResultStatuses) {
		logger.debug("setAllResultStatuses invoked {} allResultStatuses[{}]", BeanUtil.getUser(), allResultStatuses);
        this.allResultStatuses = allResultStatuses;
    }

	public boolean isTryReloadBeforeStart() {
		return tryReloadBeforeStart;
	}

	public void setTryReloadBeforeStart(boolean tryReloadBeforeStart) {
		logger.debug("setTryReloadBeforeStart invoked {} tryReloadBeforeStart[{}]", BeanUtil.getUser(), tryReloadBeforeStart);
		this.tryReloadBeforeStart = tryReloadBeforeStart;
		scriptRunsBean.setTryReloadBeforeStart(tryReloadBeforeStart);
	}

	public ScriptRunsBean getScriptRunsBean() {
		return scriptRunsBean;
	}

	public void setScriptRunsBean(ScriptRunsBean scriptRunsBean) {
		this.scriptRunsBean = scriptRunsBean;
	}

    public void onChange(MatrixAdapter newMatrixAdapter) {
		// replace matrixAdapter by updated one. (Search by matrixAdapter.id - refer to MatrixAdapter.equals() method)
	    Collections.replaceAll(matrixAdapterList, newMatrixAdapter, newMatrixAdapter);
		try {
			scriptRunsBean.onChangeMatrixAdapter(newMatrixAdapter.clone());
		} catch (CloneNotSupportedException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public List<MatrixAdapter> getMatrixAdapterList() {
		syncMatrixAdapterList();
		return matrixAdapterList;
	}

	protected void syncMatrixAdapterList() {
		List<MatrixAdapter> sessionMatrixAdapterList = scriptRunsBean.getMatrixAdapterList();
		//is deleted
		matrixAdapterList.retainAll(sessionMatrixAdapterList);
		//is added
		for (MatrixAdapter matrixAdapter: sessionMatrixAdapterList) {
			if (!matrixAdapterList.contains(matrixAdapter)) {
				matrixAdapter.setView(this);
				matrixAdapterList.add(0, matrixAdapter);
			}
		}
	}

	protected MatrixAdapter getMatrixAdapterById(Long id) {
		for (MatrixAdapter matrixAdapter: matrixAdapterList) {
			if (matrixAdapter.getMatrixId().equals(id)) {
				return matrixAdapter;
			}
		}
		throw new IllegalArgumentException("Matrix id[" + id + "] was not found");
	}

    public void addFilterCallback() {
        RequestContext.getCurrentInstance().addCallbackParam("statuses", JSONArray.fromObject(this.selectedResultStatuses).toString());
    }

	public void restoreFilters() {
        filterResultName = null;
        scriptRunsBean.setTestResultFilterString(filterResultName);

        dateFilterFrom = null;
        scriptRunsBean.setDateFrom(dateFilterFrom);

        dateFilterTo = null;
        scriptRunsBean.setDateTo(dateFilterFrom);

        selectedResultStatuses = new HashSet<>(allResultStatuses);
        scriptRunsBean.setSelectedStatuses(new HashSet<>(selectedResultStatuses));
    }

	@Override
	public MatrixHolder getMatrixHolder() {
		return BeanUtil.getMatrixHolder();
	}

	public Set<String> getSelectedResultStatuses() {
		return selectedResultStatuses;
	}

	public void setSelectedResultStatuses(Set<String> selectedResultStatuses) {
		this.selectedResultStatuses = selectedResultStatuses;
		scriptRunsBean.setSelectedStatuses(new HashSet<>(selectedResultStatuses));
	}

	public List<Tag> getTags() {
		return tags;
	}

	public void setTags(List<Tag> tags) {
		this.tags = tags;
		saveSelectedTags();
	}

	public List<Tag> getAllTags() {
		return allTags;
	}

	public boolean isStatisticsEnabled() {
		return statisticsEnabled;
	}

	public void setStatisticsEnabled(boolean statisticsEnabled) {
		this.statisticsEnabled = statisticsEnabled;
	}

	public Tag getTagToAdd() {
		return tagToAdd;
	}

	public void setTagToAdd(Tag tagToAdd) {
		this.tagToAdd = tagToAdd;
	}

	public String getMatrixCountInfo() {
		int selected = this.selectedMatrices.size();
		int total = this.matrixAdapterList.size();
		return "Selected: " + selected + ", Total: " + total;
	}

	public boolean isRunNetDumper() {
		return runNetDumper;
	}

	public void setRunNetDumper(boolean runNetDumper) {
		this.runNetDumper = runNetDumper;
		scriptRunsBean.setRunNetDumper(runNetDumper);
	}

    public boolean isSkipOptional() {
        return skipOptional;
    }

    public void setSkipOptional(boolean skipOptional) {
        this.skipOptional = skipOptional;
        scriptRunsBean.setSkipOptional(skipOptional);
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
	    this.sortBy = sortBy;
	    scriptRunsBean.setSortBy(sortBy);
    }

    public Map<String, Comparator<TestScriptDescription>> getComparators() {
        return comparators;
    }
}
