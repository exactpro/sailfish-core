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
package com.exactpro.sf.testwebgui.bigbutton;

import com.exactpro.sf.SFAPIClient;
import com.exactpro.sf.bigbutton.BigButtonSettings;
import com.exactpro.sf.bigbutton.RegressionRunner;
import com.exactpro.sf.bigbutton.execution.ExecutorClient;
import com.exactpro.sf.bigbutton.execution.ProgressView;
import com.exactpro.sf.bigbutton.execution.RegressionRunnerUtils;
import com.exactpro.sf.bigbutton.importing.CsvLibraryBuilder;
import com.exactpro.sf.bigbutton.importing.ErrorType;
import com.exactpro.sf.bigbutton.importing.ImportError;
import com.exactpro.sf.bigbutton.importing.LibraryImportResult;
import com.exactpro.sf.bigbutton.library.Executor;
import com.exactpro.sf.bigbutton.library.ScriptList;
import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.api.TestToolsAPI;
import com.exactpro.sf.testwebgui.configuration.ResourceCleaner;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.primefaces.event.NodeCollapseEvent;
import org.primefaces.event.NodeExpandEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.TreeNode;
import org.primefaces.model.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

@ManagedBean(name="bbBean")
@SessionScoped
@SuppressWarnings("serial")
public class BigButtonBean implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger(BigButtonBean.class);

	private static final int IN_QUEUE_SIZE  = 12;
	private static final int OUT_QUEUE_SIZE = 5;

	private LibraryImportResult libraryImportResult;

	private transient UploadedFile uploadedFile;

	private transient ProgressView progressView;

	private ScriptList selectedList;

	private List<ScriptList> listsCollection;

	private List<ScriptList> listsCollectionFiltered;

    private Map<ErrorType, TreeNode> errorNodes;

    private TreeNode selectedListTreeNode;

    private boolean collapseQueue = true;

    private boolean collapseRejectedQueue = true;

    private boolean collapseWarnings = false;

    private boolean inCleaning = false;

    private int cleaningProgress;

    private String cleaningLabel;

    //restore transient fields after deserialization
    private Object readResolve()  {
        init();
        return this;
    }
    
	@PostConstruct
	public void init() {
        refreshProgress();
    }

	public void handleLibraryFileUpload() {

		try {
            Objects.requireNonNull(uploadedFile, "Please select a file");
            ISFContext sfContext = BeanUtil.getSfContext();

            this.errorNodes = null;

            try(InputStream stream = uploadedFile.getInputstream()) {
                CsvLibraryBuilder builder = new CsvLibraryBuilder(stream,
                        BeanUtil.findBean(BeanUtil.WORKSPACE_DISPATCHER, IWorkspaceDispatcher.class),
                        sfContext.getStaticServiceManager(), sfContext.getDictionaryManager());

                this.libraryImportResult = builder.buildFromCsv(uploadedFile.getFileName());

                RegressionRunner runner = sfContext.getRegressionRunner();

                runner.reset();

                runner.prepare(this.libraryImportResult);
            }
		} catch(Throwable e) {
            BeanUtil.addErrorMessage("Upload failed", e.getMessage());
			logger.error(e.getMessage(), e);
		}
	}

    private TreeNode fillErrorNode(ImportError error) {
        String lineNumber = error.getLineNumber() == 0l ? "none" : String.valueOf(error.getLineNumber());
        TreeNode root = new DefaultTreeNode(String.format("CSV line: %s. %s", lineNumber, error.getMessage()));
        if (error.getCause() != null && !error.getCause().isEmpty()) {
            for (ImportError causeError : error.getCause()) {
                root.getChildren().add(fillErrorNode(causeError));
            }
        }
        return root;
    }

	public void bbPressed() {

		RegressionRunner runner = BeanUtil.getSfContext().getRegressionRunner();

		runner.run();

	}

	public void interrupt() {

        BeanUtil.getSfContext().getRegressionRunner().interrupt("Interrupted by user");

	}

	public void refreshProgress() {

		this.progressView = BeanUtil.getSfContext().getRegressionRunner()
				.getProgressView(IN_QUEUE_SIZE, OUT_QUEUE_SIZE);

	}

	public void reset() {

		try {

			BeanUtil.getSfContext().getRegressionRunner().reset();

			this.libraryImportResult = null;

			this.listsCollection = null;
			this.listsCollectionFiltered = null;

			refreshProgress();

		} catch(Exception e) {

			BeanUtil.addErrorMessage("Reset failed", e.getMessage());

		}

	}

	public void pause(){
        BeanUtil.getSfContext().getRegressionRunner().pause();
    }

    public void resume(){
        BeanUtil.getSfContext().getRegressionRunner().resume();
    }

	public int getExecutorsCount() {

		if(this.progressView == null) {
			return 0;
		}

		return this.progressView.getAllExecutors().size();

	}

	public StreamedContent getReportFile() {

        try {

			return new DefaultStreamedContent(new FileInputStream(this.progressView.getReportFile()),
					"text/csv",
					"SF_Big_Button_results.csv");

		} catch (FileNotFoundException e) {

			logger.error(e.getMessage(), e);
			return null;

		}

    }

	public String formatDuration(Date from, Date to) {

		if (to == null || from == null) {
			return "";
		}

		return DurationFormatUtils.formatDuration(to.getTime() - from.getTime(), "HH:mm:ss");

	}

	public LibraryImportResult getLibraryImportResult() {
		return libraryImportResult;
	}

	public UploadedFile getUploadedFile() {
		return uploadedFile;
	}

	public void setUploadedFile(UploadedFile uploadedFile) {
		this.uploadedFile = uploadedFile;
	}

	public ProgressView getProgressView() {
		return progressView;
	}

	public ScriptList getSelectedList() {
		return selectedList;
	}

	public void setSelectedList(ScriptList selectedList) {
		this.selectedList = selectedList;
        TreeNode node = new DefaultTreeNode();
        if (this.selectedList.isRejected()) {
            TreeNode node1 = new DefaultTreeNode("Reject cause");
            for (ImportError error : selectedList.getRejectCause().getCause()) {
                node1.getChildren().add(fillErrorNode(error));
            }
            node.getChildren().add(node1);
        }
        this.selectedListTreeNode = node;
	}

	public List<ScriptList> getListsCollection() {
		return listsCollection;
	}

	public void setListsCollection(List<ScriptList> listsCollection) {
		this.listsCollection = listsCollection;
	}

	public List<ScriptList> getListsCollectionFiltered() {
		return listsCollectionFiltered;
	}

	public void setListsCollectionFiltered(List<ScriptList> listsCollectionFiltered) {
		this.listsCollectionFiltered = listsCollectionFiltered;
	}

    public TreeNode getSelectedListTreeNode() {
        return this.selectedListTreeNode;
    }

    public void collapsingORexpanding(String nodeName) {
        switch (nodeName) {
        case "selectedList":
            collapsingORexpanding(getSelectedListTreeNode(), !isExpanded(getSelectedListTreeNode()));
            break;
        case "COMMON":
            collapsingORexpanding(errorNodes.get(ErrorType.COMMON), !isExpanded(errorNodes.get(ErrorType.COMMON)));
            break;
        case "GLOBALS":
            collapsingORexpanding(errorNodes.get(ErrorType.GLOBALS), !isExpanded(errorNodes.get(ErrorType.GLOBALS)));
            break;
        case "EXECUTOR":
            collapsingORexpanding(errorNodes.get(ErrorType.EXECUTOR), !isExpanded(errorNodes.get(ErrorType.EXECUTOR)));
            break;
        case "SCRIPTLIST":
            collapsingORexpanding(errorNodes.get(ErrorType.SCRIPTLIST), !isExpanded(errorNodes.get(ErrorType.SCRIPTLIST)));
            break;
        }
    }

    public String isExpanded(String nodeName) {
        boolean expanded;
        switch (nodeName) {
        case "selectedList":
            expanded = isExpanded(getSelectedListTreeNode());
            break;
        case "COMMON":
            expanded = isExpanded(errorNodes.get(ErrorType.COMMON));
            break;
        case "GLOBALS":
            expanded = isExpanded(errorNodes.get(ErrorType.GLOBALS));
            break;
        case "EXECUTOR":
            expanded = isExpanded(errorNodes.get(ErrorType.EXECUTOR));
            break;
        case "SCRIPTLIST":
            expanded = isExpanded(errorNodes.get(ErrorType.SCRIPTLIST));
            break;
        default:
            expanded = false;
        }
        return expanded ? "Collapse all" : "Expand all";
    }

    public boolean isOnlyLeafsInTree(TreeNode node) {
        if (!node.isLeaf()) {
            for (TreeNode child : node.getChildren()) {
                if (!child.isLeaf()) {
                    return false;
                }
            }
        }
        return true;
    }

    private void collapsingORexpanding(TreeNode node, boolean option) {
        if (node.getChildren().size() == 0) {
            node.setSelected(false);
        } else {
            for (TreeNode child : node.getChildren()) {
                collapsingORexpanding(child, option);
            }
            node.setExpanded(option);
            node.setSelected(false);
        }
    }

    private boolean isExpanded(TreeNode node) {
        for (TreeNode child : node.getChildren()) {
            if (child.isExpanded()) {
                return true;
            }
        }
        return false;
    }

    public void onNodeExpand(NodeExpandEvent event) {
        event.getTreeNode().setExpanded(true);
    }

    public void onNodeCollapse(NodeCollapseEvent event) {
        event.getTreeNode().setExpanded(false);
    }

    public void collapseAllNodesOnDialogClose() {
        if (this.errorNodes != null) {
            for (Map.Entry<ErrorType, TreeNode> entry : this.errorNodes.entrySet()) {
                collapsingORexpanding(entry.getValue(), false);
            }
        }
    }

    public String readable(String errorType) {
        switch (errorType) {
        case "COMMON":
            return "Common Errors";
        case "GLOBALS":
            return "Globals Errors";
        case "EXECUTOR":
            return "Executor Errors";
        case "SCRIPTLIST":
            return "Script List Errors";
        default:
            return "Unknown";
        }
    }

    public String errorColor(String errorType) {
        return (errorType.equals("COMMON") || errorType.equals("GLOBALS")) ? "red" : "none";
    }

    public Map<ErrorType, TreeNode> getErrorNodes() {
        if (this.errorNodes == null) {
            if (this.progressView.getImportErrors() != null) {
                this.errorNodes = new TreeMap<>();
                for (Map.Entry<ErrorType, Set<ImportError>> entry : this.progressView.getImportErrors().entrySet()) {
                if (entry.getValue().isEmpty()) {
                        continue;
                    }
                    TreeNode node = new DefaultTreeNode();
                    for (ImportError error : entry.getValue()) {
                        node.getChildren().add(fillErrorNode(error));
                    }
                    this.errorNodes.put(entry.getKey(), node);
                }
            }
        }
        return this.errorNodes;
    }

    public void prepareToClean() {
        inCleaning = true;
    }

    private void postClean() {
        inCleaning = false;
        cleaningProgress = 0;
        cleaningLabel = "";
    }

    public void preRunClean() {
        List<ExecutorClient> allExecutors = progressView.getAllExecutors();

        int totalSteps = allExecutors.size() * ResourceCleaner.values().length;
        int currentStep = 0;
        Instant now = Instant.now();

        logger.info("performing pre-run cleanup older than {}", now);

        for (ExecutorClient client : allExecutors) {
            Executor executor = client.getExecutor();
            String name = executor.getName();
            try (SFAPIClient apiClient = new SFAPIClient(
                    URI.create(executor.getHttpUrl() + "/sfapi").normalize().toString())) {
                for (ResourceCleaner cleaner : ResourceCleaner.values()) {
                    if (!inCleaning) {
                        postClean();
                        return;
                    }
                    cleaningLabel = new StringBuilder(name).append(": ").append(cleaner.getName()).toString();
                    apiClient.cleanResources(now, cleaner.getName());
                    currentStep++;
                    cleaningProgress = RegressionRunnerUtils.calcPercent(currentStep, totalSteps);
                }
            } catch (Exception e) {
                BeanUtil.addErrorMessage("Cleaning failed", e.getMessage());
                logger.error(e.getMessage(), e);
            }
        }

        boolean cleanSuccess = inCleaning;

        postClean();

        if (cleanSuccess) {
            bbPressed();
        }
    }

    public void setShowCleanupDialog(boolean value) {
        BigButtonSettings settings = BeanUtil.getSfContext().getRegressionRunner().getSettings();
        settings.setShowPreCeanDialog(value);

        try {
            logger.info("Apply bb settings [{}]", settings);
            TestToolsAPI.getInstance().setRegressionRunnerSettings(settings);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            settings = BeanUtil.getSfContext().getRegressionRunner().getSettings();
        }
    }

    public boolean getShowCleanupDialog() {
        return BeanUtil.getSfContext().getRegressionRunner().getSettings().isShowPreCeanDialog();
    }

    public boolean isCollapseQueue() {
        return collapseQueue;
    }

    public void showHideQueue() {
        this.collapseQueue = !collapseQueue;
    }

    public boolean isCollapseRejectedQueue() {
        return collapseRejectedQueue;
    }

    public void showHideRejectedQueue() {
        this.collapseRejectedQueue = !collapseRejectedQueue;
    }

    public boolean isCollapseWarnings() {
        return collapseWarnings;
    }

    public void showHideWarnings() {
        this.collapseWarnings = !collapseWarnings;
    }

    public boolean isInCleaning() {
        return inCleaning;
    }

    public void setInCleaning(boolean inCleaning) {
        this.inCleaning = inCleaning;
    }

    public int getCleaningProgress() {
        return cleaningProgress;
    }

    public void setCleaningProgress(int cleaningProgress) {
        this.cleaningProgress = cleaningProgress;
    }

    public String getCleaningLabel() {
        return cleaningLabel;
    }

    public void setCleaningLabel(String cleaningLabel) {
        this.cleaningLabel = cleaningLabel;
    }

    public void stopCleaning() {
        inCleaning = false;
    }
}
