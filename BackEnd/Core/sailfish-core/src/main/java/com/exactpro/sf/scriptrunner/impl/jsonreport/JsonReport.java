/******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.scriptrunner.impl.jsonreport;

import com.exactpro.sf.SerializeUtil;
import com.exactpro.sf.aml.AMLBlockType;
import com.exactpro.sf.aml.generator.AggregateAlert;
import com.exactpro.sf.aml.script.CheckPoint;
import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceStructureException;
import com.exactpro.sf.scriptrunner.*;
import com.exactpro.sf.scriptrunner.TestScriptDescription.ScriptState;
import com.exactpro.sf.scriptrunner.TestScriptDescription.ScriptStatus;
import com.exactpro.sf.scriptrunner.impl.ReportStats;
import com.exactpro.sf.scriptrunner.impl.ReportTable;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.*;
import com.exactpro.sf.scriptrunner.reportbuilder.textformatter.TextColor;
import com.exactpro.sf.scriptrunner.reportbuilder.textformatter.TextStyle;
import com.exactpro.sf.util.BugDescription;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection, unused, FieldCanBeLocal")
public class JsonReport implements IScriptReport {
    private static final Logger logger = LoggerFactory.getLogger(JsonReport.class);
    private static final ObjectMapper mapper;
    private static long actionIdCounter = 0;

    private ScriptContext scriptContext;
    private Context context;
    private IReportStats reportStats;
    private boolean isActionCreated;
    private Map<Long, Set<Long>> messageToActionIdMap;
    private IWorkspaceDispatcher dispatcher;
    private String reportRootDirectoryPath;
    private final TestScriptDescription testScriptDescription;

    //Main bean of report
    private final ReportRoot reportRoot = new ReportRoot();

    static {
        mapper = new ObjectMapper();
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker().withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE).withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE).withIsGetterVisibility(JsonAutoDetect.Visibility.NONE));
        mapper.registerModule(new JavaTimeModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS"));
    }

    public JsonReport(String reportRootDirectoryPath, IWorkspaceDispatcher dispatcher, TestScriptDescription testScriptDescription) {
        this.messageToActionIdMap = new HashMap<>();
        this.reportStats = new ReportStats();
        this.dispatcher = dispatcher;
        this.reportRootDirectoryPath = reportRootDirectoryPath;
        this.testScriptDescription = testScriptDescription;
    }

    @JsonIgnore public boolean isActionCreated() throws UnsupportedOperationException {
        return isActionCreated;
    }

    private void assertState(ContextType... states) {
        if (Arrays.stream(states).noneMatch(i -> this.context.cur.equals(i))) {
            throw new RuntimeException(String.format("Incorrect report state '%s' ('%s' expected)", this.context.cur, Arrays.toString(states)));
        }
    }

    private File getFile(String fileName, String extension) {
        fileName = fileName.replaceAll("\\W", "_");
        if (!fileName.endsWith(extension)) {
            fileName = fileName.concat(extension);
        }

        try {
            return this.dispatcher.createFile(FolderType.REPORT, true, reportRootDirectoryPath, "reportData", fileName);
        } catch (WorkspaceStructureException e) {
            throw new ScriptRunException("unable to create report file", e);
        }
    }

    private void exportToFile(Object data, String fileName) {
        File jsonFile = getFile(fileName, ".json");
        File jsonpFile = getFile(fileName, ".js");

        try (FileOutputStream jsonpStream = new FileOutputStream(jsonpFile); FileOutputStream jsonStream = new FileOutputStream(jsonFile)) {
            logger.info("saving json report - writing to file: '{}'", jsonpFile);
            byte[] jsonStringBytes = mapper.writeValueAsString(data).getBytes();
            jsonStream.write(jsonStringBytes);
            jsonpStream.write("window.loadJsonp(".getBytes());
            jsonpStream.write(jsonStringBytes);
            jsonpStream.write(")".getBytes());
        } catch (IOException e) {
            throw new ScriptRunException("unable to export json report", e);
        }

        if (data instanceof TestCase) {
            this.reportRoot.getMetadata().add(new TestCaseMetadata(((TestCase) data), jsonpFile, jsonFile));
        }
    }

    private void initProperties() {
        ScriptState state = testScriptDescription.getState();
        ScriptStatus status = testScriptDescription.getStatus();
        String matrixFile = testScriptDescription.getMatrixFileName();
        long timestamp = testScriptDescription.getTimestamp().getTime();
        String environmentNameAttr = testScriptDescription.getContext().getEnvironmentName();
        String languageURI = testScriptDescription.getLanguageURI().toString();
        String workFolder = testScriptDescription.getWorkFolder();

        IScriptProgress progress = testScriptDescription.getContext().getScriptProgress();
        long passed  = progress.getPassed();
        long conditionallyPassed = progress.getConditionallyPassed();
        long failed  = progress.getFailed();
        long total = progress.getLoaded();

        String username = testScriptDescription.getUsername();
        long startTime = testScriptDescription.getStartedTime();
        long finishTime = testScriptDescription.getFinishedTime();
        List<String> services = testScriptDescription.getContext().getServiceList();
        String range = testScriptDescription.getRange();
        boolean autostart = testScriptDescription.getAutoStart();
        String cause = null;
        // Cause serializing
        if (testScriptDescription.getCause() != null) {
            cause = SerializeUtil.serializeToBase64(testScriptDescription.getCause());
        }

        this.reportRoot.setReportProperties(
                new ReportProperties(state, status, matrixFile, timestamp, environmentNameAttr, languageURI, workFolder, passed, conditionallyPassed,
                        failed, total, username, startTime, finishTime, services, range, autostart, cause));
    }

    private void setContext(ContextType state, IJsonReportNode currentNode) {
        this.context = new Context(context, state, currentNode);
    }

    private void revertContext() {
        this.context = context.prev;
    }

    @SuppressWarnings("unchecked")
    private <T extends IJsonReportNode> T getCurrentContextNode() {
        return (T) this.context.node;
    }

    public void createReport(ScriptContext scriptContext, String name, String description, long scriptRunId, String environmentName,
            String userName) {

        this.scriptContext = scriptContext;
        this.reportRoot.setStartTime(Instant.now());

        try {
            this.reportRoot.setHostName(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            this.reportRoot.setHostName("n/a");
        }

        this.reportRoot.setName(name);
        this.reportRoot.setUserName(userName);
        this.reportRoot.setScriptRunId(scriptRunId);
        this.reportRoot.setVersion(SFLocalContext.getDefault().getVersion());
        this.reportRoot.setBranchName(SFLocalContext.getDefault().getBranchName());

        this.reportRoot.setPlugins(SFLocalContext.getDefault().getPluginVersions().stream().filter(i -> !i.isGeneral())
                .collect(Collectors.toMap(IVersion::getAlias, IVersion::buildVersion)));

        this.reportRoot.setDescription(description);

        setContext(ContextType.SCRIPT, null);
    }

    public void addAlerts(Collection<AggregateAlert> aggregatedAlerts) {
        synchronized (reportRoot.getAlerts()) {
            reportRoot.getAlerts().addAll(aggregatedAlerts.stream().map(a -> new Alert(a.joinLines(), a.getType().toString(), a.getColumn(), a.getMessage()))
                    .collect(Collectors.toList()));
        }
    }

    public void createTestCase(String reference, String description, int order, int matrixOrder, String tcId, int tcHash, AMLBlockType type) {
        this.reportStats = new ReportStats();
        assertState(ContextType.SCRIPT);

        TestCase testcase = new TestCase();

        testcase.setName(ReportUtils.generateTestCaseName(reference, matrixOrder, type));
        testcase.setOrder(order);
        testcase.setReference(reference);
        testcase.setType(type.getName());
        testcase.setStartTime(Instant.now());
        testcase.setMatrixOrder(matrixOrder);
        testcase.setId(tcId);
        testcase.setHash(tcHash);
        testcase.setDescription(description);

        setContext(ContextType.TESTCASE, testcase);
    }

    public void closeTestCase(StatusDescription status) {
        assertState(ContextType.TESTCASE);

        TestCase curTestCase = getCurrentContextNode();
        curTestCase.setStatus(new Status(status));
        curTestCase.setFinishTime(Instant.now());
        this.reportRoot.getBugs().addAll(curTestCase.getBugs());

        exportToFile(curTestCase, curTestCase.getName());

        revertContext();
        this.reportStats.updateTestCaseStatus(status.getStatus());
    }

    public void createAction(String name, String serviceName, String action, String msg, String description, Object inputParameters,
            CheckPoint checkPoint, String tag, int hash, List<String> verificationsOrder) {

        createAction(name, serviceName, action, msg, description, Collections.singletonList(inputParameters), checkPoint, tag, hash,
                verificationsOrder);
    }

    public void createAction(String name, String serviceName, String action, String msg, String description, List<Object> inputParameters,
            CheckPoint checkPoint, String tag, int hash, List<String> verificationsOrder) {

        assertState(ContextType.TESTCASE, ContextType.ACTION);

        Action curAction = new Action();
        getCurrentContextNode().addSubNodes(curAction);

        curAction.setId(actionIdCounter++);
        curAction.setStartTime(Instant.now());
        curAction.setName(name);
        curAction.setDescription(description);
        curAction.setCheckPointId(checkPoint != null ? checkPoint.getId() : null);
        if (inputParameters != null) {
            curAction.setParameters(inputParameters.stream().map(p -> new Parameter(new ReportEntity("Parameter", p))).collect(Collectors.toList()));
        }
        setContext(ContextType.ACTION, curAction);
        isActionCreated = true;
    }

    public void closeAction(StatusDescription status, Object actionResult) {
        assertState(ContextType.ACTION);

        Action curAction = getCurrentContextNode();
        curAction.setStatus(new Status(status));
        curAction.setFinishTime(Instant.now());

        if (status.isUpdateTestCaseStatus()) {
            this.reportStats.updateActions(status.getStatus());
        }

        isActionCreated = false;

        revertContext();

        //content propagation
        IJsonReportNode parentNode = getCurrentContextNode();

        parentNode.addSubNodes(curAction.getBugs());
        if (parentNode instanceof Action) {
            ((Action) parentNode).getRelatedMessages().addAll(curAction.getRelatedMessages());
        }

        for (Long id : curAction.getRelatedMessages()) {
            //noinspection ConstantConditions
            this.messageToActionIdMap.computeIfAbsent(id, k -> new HashSet<>()).add(curAction.getId());
        }
    }

    public void openGroup(String name, String description) {
        assertState(ContextType.ACTION, ContextType.ACTIONGROUP);
        Action curGroup = new Action();
        getCurrentContextNode().addSubNodes(curGroup);

        curGroup.setId(actionIdCounter++);
        curGroup.setName(name);
        curGroup.setDescription(description);

        setContext(ContextType.ACTIONGROUP, curGroup);
    }

    public void closeGroup(StatusDescription status) {
        assertState(ContextType.ACTIONGROUP);
        Action curGroup = getCurrentContextNode();
        curGroup.setStatus(new Status(status));

        revertContext();

        //content propagation
        IJsonReportNode parentNode = getCurrentContextNode();
        parentNode.addSubNodes(curGroup.getBugs());

        if (parentNode instanceof Action) {
            ((Action) parentNode).getRelatedMessages().addAll(curGroup.getRelatedMessages());
        }
    }

    public void createVerification(String name, String description, StatusDescription status, ComparisonResult result) {
        assertState(ContextType.ACTION, ContextType.ACTIONGROUP, ContextType.TESTCASE);

        Verification curVerification = new Verification();
        curVerification.setName(name);
        curVerification.setDescription(description);
        curVerification.setStatus(status);

        IJsonReportNode curNode = getCurrentContextNode();

        if (result != null) {
            if (result.getMetaData() != null) {
                curVerification.setMessageId(result.getMetaData().getId());
            }
            else {
                logger.warn("comparison result does not contain metadata");
            }
            Set<BugDescription> reproduced = result.getReproducedBugs();
            Set<BugDescription> notReproduced = Sets.difference(result.getAllKnownBugs(), reproduced);

            curNode.addSubNodes(reproduced.stream().map(d -> new Bug(d).markAsReproduced()).collect(Collectors.toList()));
            curNode.addSubNodes(notReproduced.stream().map(Bug::new).collect(Collectors.toList()));

            curVerification.setEntries(result.getResults().values().stream().map(VerificationEntry::new).collect(Collectors.toList()));
        }
        curNode.addSubNodes(curVerification);
    }

    public void createMessage(MessageLevel level, String... messages) {
        createMessage(level, null, null, null, messages);
    }

    public void createMessage(MessageLevel level, Throwable e, String... messages) {
        createMessage(level, e, null, null, messages);
    }

    public void createMessage(TextColor color, TextStyle style, String... messages) {
        createMessage(null, null, color, style, messages);
    }

    private void createMessage(MessageLevel level, Throwable e, TextColor color, TextStyle style, String... messages) {
        assertState(ContextType.TESTCASE, ContextType.ACTION, ContextType.ACTIONGROUP);

        if (ArrayUtils.isEmpty(messages) && e == null) {
            throw new ScriptRunException("Message array is empty");
        }

        getCurrentContextNode().addSubNodes(Arrays.stream(messages)
                .map(m -> new CustomMessage(m, Objects.toString(color, null), Objects.toString(style, null), level, e))
                .collect(Collectors.toSet()));
    }

    public void createException(Throwable cause) {
        if (cause != null) {
            if (getCurrentContextNode() != null) {
                getCurrentContextNode().addException(cause);
            } else {
                if (this.reportRoot.getException() == null) {
                    this.reportRoot.setException(new ReportException(cause));
                }
            }
        }
    }

    public void createTable(ReportTable table) {
        assertState(ContextType.ACTION, ContextType.ACTIONGROUP, ContextType.TESTCASE, ContextType.SCRIPT);

        IJsonReportNode currentNode = getCurrentContextNode();

        if (table.getName().equals("Messages")) {
            List<Message> messages = table.getRows().stream().map(Message::new).collect(Collectors.toList());

            if (currentNode instanceof Action) {
                long actionId = ((Action) currentNode).getId();

                for (Message message : messages) {
                    this.messageToActionIdMap.computeIfAbsent(message.getId(), k -> new HashSet<>()).add(actionId);
                }
            }

            if (currentNode instanceof TestCase) {
                for (Message message : messages) {
                    message.setRelatedActions(this.messageToActionIdMap.computeIfAbsent(message.getId(), k -> new HashSet<>()));
                }
            }
            currentNode.addSubNodes(messages);
        } else {
            currentNode.addSubNodes(new CustomTable(table.getRows()));
        }
    }

    public void createLogTable(List<String> header, List<LoggerRow> rows) {
        assertState(ContextType.TESTCASE, ContextType.ACTION, ContextType.ACTIONGROUP);

        List<IJsonReportNode> logs = rows.stream().map(LogEntry::new).collect(Collectors.toList());
        getCurrentContextNode().addSubNodes(logs);
    }

    public void setOutcomes(OutcomeCollector outcomes) {
        assertState(ContextType.TESTCASE);
        TestCase curTestCase = getCurrentContextNode();

        for (String group : outcomes.getGroupOrder()) {
            for (String name : outcomes.getDefinedOutcomes().get(group)) {
                curTestCase.getOutcomes()
                        .add(new OutcomeSummary(name, outcomes.getPassedCount(group, name), outcomes.getConditionallyPassedCount(group, name),
                                outcomes.getFailedCount(group, name)));
            }
        }
    }

    public IReportStats getReportStats() {
        return reportStats;
    }

    public void closeReport() {
        assertState(null, ContextType.SCRIPT);
        this.reportRoot.setFinishTime(Instant.now());
        initProperties();
        exportToFile(reportRoot, "report");
    }

    public void createLinkToReport(String linkToReport) {
        assertState(ContextType.ACTION, ContextType.ACTIONGROUP);
        getCurrentContextNode().addSubNodes(new CustomLink(linkToReport));
    }

    public void flush() {
        //do nothing
    }

    private class Context {
        final Context prev;
        final ContextType cur;
        final IJsonReportNode node;

        Context(Context prev, ContextType current, IJsonReportNode node) {
            this.prev = prev;
            this.cur = current;
            this.node = node;
        }
    }

}
