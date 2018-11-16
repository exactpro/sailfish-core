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
import com.exactpro.sf.scriptrunner.impl.ReportStats;
import com.exactpro.sf.scriptrunner.impl.ReportTable;
import com.exactpro.sf.scriptrunner.reportbuilder.textformatter.TextColor;
import com.exactpro.sf.scriptrunner.reportbuilder.textformatter.TextStyle;
import com.exactpro.sf.util.BugDescription;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection, unused, FieldCanBeLocal")
public class JsonReport implements IScriptReport {
    @JsonIgnore private static final Logger logger = LoggerFactory.getLogger(JsonReport.class);
    @JsonIgnore private static final ObjectMapper mapper;
    @JsonIgnore private static long actionIdCounter = 0;

    @JsonIgnore private Context context;
    @JsonIgnore private IReportStats reportStats;
    @JsonIgnore private boolean isActionCreated;
    @JsonIgnore private Map<Long, Set<Long>> messageToActionIdMap;
    @JsonIgnore private IWorkspaceDispatcher dispatcher;
    @JsonIgnore private String reportRootDirectoryPath;

    //IMPORTANT: access should be synchronized
    private final List<Alert> alerts;

    private Instant startTime;
    private Instant finishTime;
    private Map<String, String> plugins;
    private Set<Bug> bugs;
    private String hostName;
    private String userName;
    private String name;
    private long scriptRunId;
    private String version;
    private String branchName;
    private String description;
    private ReportException exception;
    private List<String> testCaseLinks;

    static {
        mapper = new ObjectMapper();
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker().withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE).withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE).withIsGetterVisibility(JsonAutoDetect.Visibility.NONE));
        mapper.registerModule(new JsonInstantModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public JsonReport(String reportRootDirectoryPath, IWorkspaceDispatcher dispatcher) {
        this.messageToActionIdMap = new HashMap<>();
        this.reportStats = new ReportStats();
        this.alerts = new ArrayList<>();
        this.plugins = new HashMap<>();
        this.bugs = new HashSet<>();
        this.testCaseLinks = new ArrayList<>();
        this.dispatcher = dispatcher;
        this.reportRootDirectoryPath = reportRootDirectoryPath;
    }

    @JsonIgnore public boolean isActionCreated() throws UnsupportedOperationException {
        return isActionCreated;
    }

    private void assertState(ContextType... states) {
        if (Arrays.stream(states).noneMatch(i -> this.context.cur.equals(i))) {
            throw new RuntimeException(String.format("Incorrect report state '%s' ('%s' expected)", this.context.cur, Arrays.toString(states)));
        }
    }

    private File getFile(String fileName) {
        fileName = fileName.replaceAll("\\W", "_");
        if (!fileName.endsWith(".json")) {
            fileName = fileName.concat(".json");
        }

        try {
            return this.dispatcher.createFile(FolderType.REPORT, true, reportRootDirectoryPath, "reportData", fileName);
        } catch (WorkspaceStructureException e) {
            throw new ScriptRunException("unable to create report file", e);
        }
    }

    private void exportToFile(Object data, String fileName) {
        File file = getFile(fileName);
        if (file == null) {
            throw new ScriptRunException(String.format("file '%s' does not exist - unable to export json report", fileName));
        }

        try {
            logger.info(String.format("saving json report - writing to file: '%s'", file.getAbsolutePath()));
            mapper.writeValue(file, data);
        } catch (IOException e) {
            throw new ScriptRunException("unable to export json report", e);
        }
        this.testCaseLinks.add(file.getName());
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

        this.startTime = Instant.now();

        try {
            this.hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            this.hostName = "n/a";
        }

        this.name = name;
        this.userName = userName;
        this.scriptRunId = scriptRunId;
        this.version = SFLocalContext.getDefault().getVersion();
        this.branchName = SFLocalContext.getDefault().getBranchName();

        this.plugins = SFLocalContext.getDefault().getPluginVersions().stream().filter(i -> !i.isGeneral())
                .collect(Collectors.toMap(IVersion::getAlias, IVersion::buildVersion));

        this.description = description;

        setContext(ContextType.SCRIPT, null);
    }

    public void addAlerts(Collection<AggregateAlert> aggregatedAlerts) {
        synchronized (alerts) {
            alerts.addAll(aggregatedAlerts.stream().map(a -> new Alert(a.joinLines(), a.getType().toString(), a.getColumn(), a.getMessage()))
                    .collect(Collectors.toList()));
        }
    }

    public void createTestCase(String reference, String description, int order, int matrixOrder, String tcId, int tcHash, AMLBlockType type) {
        this.reportStats = new ReportStats();
        assertState(ContextType.SCRIPT);

        TestCase testcase = new TestCase();

        testcase.name = ReportUtils.generateTestCaseName(reference, matrixOrder, type);
        testcase.order = order;
        testcase.reference = reference;
        testcase.type = type.getName();
        testcase.startTime = Instant.now();
        testcase.matrixOrder = matrixOrder;
        testcase.id = tcId;
        testcase.hash = tcHash;
        testcase.description = description;

        setContext(ContextType.TESTCASE, testcase);
    }

    public void closeTestCase(StatusDescription status) {
        assertState(ContextType.TESTCASE);

        TestCase curTestCase = getCurrentContextNode();
        curTestCase.status = new Status(status);
        curTestCase.finishTime = Instant.now();
        this.bugs.addAll(curTestCase.bugs);

        exportToFile(curTestCase, curTestCase.name);

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

        curAction.id = actionIdCounter++;
        curAction.startTime = Instant.now();
        curAction.name = name;
        curAction.description = description;
        curAction.checkPointId = checkPoint != null ? checkPoint.getId() : null;
        if (inputParameters != null) {
            curAction.parameters = inputParameters.stream().map(p -> new Parameter(new ReportEntity("Parameter", p))).collect(Collectors.toList());
        }
        setContext(ContextType.ACTION, curAction);
        isActionCreated = true;
    }

    public void closeAction(StatusDescription status, Object actionResult) {
        assertState(ContextType.ACTION);

        Action curAction = getCurrentContextNode();
        curAction.status = new Status(status);
        curAction.finishTime = Instant.now();

        if (status.isUpdateTestCaseStatus()) {
            this.reportStats.updateActions(status.getStatus());
        }

        isActionCreated = false;

        revertContext();

        //content propagation
        IJsonReportNode parentNode = getCurrentContextNode();

        parentNode.addSubNodes(curAction.bugs);
        if (parentNode instanceof Action) {
            ((Action) parentNode).relatedMessages.addAll(curAction.relatedMessages);
        }

        for (Long id : curAction.relatedMessages) {
            //noinspection ConstantConditions
            this.messageToActionIdMap.computeIfAbsent(id, k -> new HashSet<>()).add(curAction.id);
        }
    }

    public void openGroup(String name, String description) {
        assertState(ContextType.ACTION, ContextType.ACTIONGROUP);
        Action curGroup = new Action();
        getCurrentContextNode().addSubNodes(curGroup);

        curGroup.id = actionIdCounter++;
        curGroup.name = name;
        curGroup.description = description;

        setContext(ContextType.ACTIONGROUP, curGroup);
    }

    public void closeGroup(StatusDescription status) {
        assertState(ContextType.ACTIONGROUP);
        Action curGroup = getCurrentContextNode();
        curGroup.status = new Status(status);

        revertContext();

        //content propagation
        IJsonReportNode parentNode = getCurrentContextNode();
        parentNode.addSubNodes(curGroup.bugs);

        if (parentNode instanceof Action) {
            ((Action) parentNode).relatedMessages.addAll(curGroup.relatedMessages);
        }
    }

    public void createVerification(String name, String description, StatusDescription status, ComparisonResult result) {
        assertState(ContextType.ACTION, ContextType.ACTIONGROUP, ContextType.TESTCASE);

        Verification curVerification = new Verification();
        curVerification.name = name;
        curVerification.description = description;
        curVerification.status = status;

        IJsonReportNode curNode = getCurrentContextNode();

        if (result != null) {
            curVerification.messageId = result.getMetaData().getId();
            Set<BugDescription> reproduced = result.getReproducedBugs();
            Set<BugDescription> notReproduced = Sets.difference(result.getAllKnownBugs(), reproduced);

            curNode.addSubNodes(reproduced.stream().map(d -> new Bug(d).markAsReproduced()).collect(Collectors.toList()));
            curNode.addSubNodes(notReproduced.stream().map(Bug::new).collect(Collectors.toList()));

            curVerification.entries = result.getResults().values().stream().map(VerificationEntry::new).collect(Collectors.toList());
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
                if (this.exception == null) {
                    this.exception = new ReportException(cause);
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
                long actionId = ((Action) currentNode).id;

                for (Message message : messages) {
                    this.messageToActionIdMap.computeIfAbsent(message.id, k -> new HashSet<>()).add(actionId);
                }
            }

            if (currentNode instanceof TestCase) {
                for (Message message : messages) {
                    message.relatedActions = this.messageToActionIdMap.computeIfAbsent(message.id, k -> new HashSet<>());
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
                curTestCase.outcomes
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
        this.finishTime = Instant.now();
        exportToFile(this, "report");
    }

    public void createLinkToReport(String linkToReport) {
        assertState(ContextType.ACTION, ContextType.ACTIONGROUP);
        getCurrentContextNode().addSubNodes(new CustomLink(linkToReport));
    }

    public void flush() {
        //do nothing
    }

    private enum ContextType {SCRIPT, TESTCASE, ACTION, ACTIONGROUP}


    private enum KnownBugStatus {REPRODUCED, NOT_REPRODUCED}


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


    private class Parameter {
        String name;
        String value;
        List<Parameter> subParameters;

        public Parameter(ReportEntity e) {
            this.name = e.getName();
            this.value = e.getValue().toString();
            this.subParameters = e.getFields().stream().map(Parameter::new).collect(Collectors.toList());
        }
    }


    private class Bug implements IJsonReportNode {
        final BugDescription description;
        KnownBugStatus status;

        Bug(BugDescription description) {
            this.description = description;
            this.status = KnownBugStatus.NOT_REPRODUCED;
        }

        Bug markAsReproduced() {
            this.status = KnownBugStatus.REPRODUCED;
            return this;
        }
    }


    private class Message implements IJsonReportNode {
        long id;
        Set<Long> relatedActions;
        String checkPoint;
        String raw;
        String from;
        String to;
        String msgName;
        String content;
        String contentHumanReadable;
        String timestamp; //IMPORTANT: datetime format may divert from the default one

        Message(Map<String, String> data) {
            this.id = Long.parseLong(data.get("Id"));
            this.contentHumanReadable = data.get("ContentJson");
            this.content = data.get("Content");
            this.checkPoint = data.get("UnderCheckPoint").isEmpty() ? null : data.get("UnderCheckPoint");
            this.raw = data.get("RawMessage");
            this.from = data.get("From");
            this.to = data.get("To");
            this.msgName = data.get("MsgName");
            this.timestamp = data.get("Timestamp");
        }
    }


    private class TestCase implements IJsonReportNode {
        final List<IJsonReportNode> actions;
        final List<LogEntry> logs;
        final List<Message> messages;
        final List<Verification> verifications;
        final Set<Bug> bugs;
        final List<OutcomeSummary> outcomes;
        public Instant startTime;
        public Instant finishTime;
        String name;
        String type;
        String reference;
        int order;
        int matrixOrder;
        String id;
        int hash;
        String description;
        Status status;

        TestCase() {
            this.outcomes = new ArrayList<>();
            this.actions = new ArrayList<>();
            this.logs = new ArrayList<>();
            this.messages = new ArrayList<>();
            this.bugs = new HashSet<>();
            this.verifications = new ArrayList<>();
        }

        @Override public void addSubNodes(Collection<? extends IJsonReportNode> nodes) {
            for (IJsonReportNode child : nodes) {
                if (child instanceof Action || child instanceof CustomMessage) {
                    this.actions.add(child);
                } else if (child instanceof Message) {
                    this.messages.add((Message) child);
                } else if (child instanceof Bug) {
                    this.bugs.add((Bug) child);
                } else if (child instanceof LogEntry) {
                    this.logs.add((LogEntry) child);
                } else if (child instanceof Verification) {
                    this.verifications.add((Verification) child);
                } else {
                    throw new IllegalArgumentException("unsupported child node type: " + child.getClass().toString());
                }
            }
        }

        @Override public void addException(Throwable t) {
            if (this.status == null) {
                this.status = new Status(t);
            }
        }
    }


    private class Verification implements IJsonReportNode {
        private static final String ACTION_NODE_TYPE = "verification";

        Long messageId;
        String name;
        String description;
        StatusDescription status;
        List<VerificationEntry> entries;

        Verification() {
            this.entries = new ArrayList<>();
        }

        @JsonProperty("actionNodeType")
        public String getActionNodeType() {
            return ACTION_NODE_TYPE;
        }
    }


    private class VerificationEntry {
        String name;
        String actual;
        String expected;
        StatusType status;
        Double precision;
        Double systemPrecision;
        List<VerificationEntry> subEntries;
        ReportException exception;

        VerificationEntry(ComparisonResult result) {
            this.name = result.getName();
            this.actual = Objects.toString(result.getActual(), null);
            this.expected = Objects.toString(result.getExpected(), null);
            this.precision = result.getDoublePrecision();
            this.systemPrecision = result.getSystemPrecision();
            this.status = result.getStatus();
            this.exception = result.getException() != null ? new ReportException(result.getException()) : null;

            if (result.hasResults()) {
                this.subEntries = result.getResults().values().stream().map(VerificationEntry::new).collect(Collectors.toList());
            }
        }
    }


    private class Alert {
        String lines;
        String type;
        String column;
        String message;

        Alert(String lines, String type, String column, String message) {
            this.lines = lines;
            this.type = type;
            this.column = column;
            this.message = message;
        }
    }


    private class Action implements IJsonReportNode {
        private static final String ACTION_NODE_TYPE = "action";

        long id;
        Long checkPointId;
        List<IJsonReportNode> subNodes;
        String name;
        String description;
        Set<Bug> bugs;
        Set<Long> relatedMessages;
        Status status;
        List<Parameter> parameters;
        List<LogEntry> logs;
        Instant startTime;
        Instant finishTime;

        Action() {
            this.bugs = new HashSet<>();
            this.subNodes = new ArrayList<>();
            this.relatedMessages = new HashSet<>();
            this.logs = new ArrayList<>();
            this.bugs = new HashSet<>();
        }

        @Override public void addSubNodes(Collection<? extends IJsonReportNode> nodes) {
            for (IJsonReportNode child : nodes) {
                if (child instanceof Message) {
                    this.relatedMessages.add(((Message) child).id);
                } else if (child instanceof Action || child instanceof CustomMessage || child instanceof CustomTable || child instanceof CustomLink) {
                    this.subNodes.add(child);
                } else if (child instanceof Bug) {
                    this.bugs.add((Bug) child);
                } else if (child instanceof Verification) {
                    this.subNodes.add(child);
                    if (((Verification) child).messageId != null) {
                        this.relatedMessages.add(((Verification) child).messageId);
                    }
                } else if (child instanceof LogEntry) {
                    this.logs.add((LogEntry) child);
                } else {
                    throw new IllegalArgumentException("unsupported child node type: " + child.getClass().toString());
                }
            }
        }

        @Override public void addException(Throwable t) {
            if (this.status == null) {
                this.status = new Status(t);
            }
        }

        @JsonProperty("actionNodeType")
        public String getActionNodeType() {
            return ACTION_NODE_TYPE;
        }
    }


    private class CustomMessage implements IJsonReportNode {
        private static final String ACTION_NODE_TYPE = "message";

        String message;
        String color;
        String style;
        MessageLevel level;
        ReportException exception;

        CustomMessage(String message, String color, String style, MessageLevel level, Throwable t) {
            this.message = message;
            this.level = level;
            this.exception = t != null ? new ReportException(t) : null;
            this.color = color;
            this.style = style;
        }

        @JsonProperty("actionNodeType")
        public String getActionNodeType() {
            return ACTION_NODE_TYPE;
        }
    }


    private class CustomLink implements IJsonReportNode {
        private static final String ACTION_NODE_TYPE = "link";

        String link;

        CustomLink(String link) {
            this.link = link;
        }

        @JsonProperty("actionNodeType")
        public String getActionNodeType() {
            return ACTION_NODE_TYPE;
        }
    }


    private class CustomTable implements IJsonReportNode {
        private static final String ACTION_NODE_TYPE = "table";

        List<Map<String, String>> content;

        CustomTable(List<Map<String, String>> content) {
            this.content = content;
        }

        @JsonProperty("actionNodeType")
        public String getActionNodeType() {
            return ACTION_NODE_TYPE;
        }
    }


    private class ReportException implements IJsonReportNode {
        String message;
        ReportException cause;

        String stacktrace;

        ReportException(Throwable t) {
            this.message = t.getMessage();

            StringWriter writer = new StringWriter();
            t.printStackTrace(new PrintWriter(writer));
            this.stacktrace = writer.toString();

            this.cause = t.getCause() != null ? new ReportException(t.getCause()) : null;
        }

    }


    private class Status {
        StatusType status;
        ReportException cause;
        String description;

        Status(StatusDescription description) {
            this.status = description.getStatus();
            this.description = description.getDescription();
            this.cause = description.getCause() != null ? new ReportException(description.getCause()) : null;
        }

        Status(Throwable t) {
            this.status = StatusType.FAILED;
            this.cause = new ReportException(t);
        }
    }


    private class LogEntry implements IJsonReportNode {
        Instant timestamp;
        String level;
        String thread;
        String message;
        ReportException exception;

        @JsonProperty("class")
        String clazz;

        LogEntry(LoggerRow row) {
            this.timestamp = Instant.ofEpochMilli(row.getTimestamp());
            this.level = Objects.toString(row.getLevel(), null);
            this.thread = row.getThread();
            this.message = row.getMessage();
            this.clazz = row.getClazz();
            this.exception = row.getEx() != null ? new ReportException(row.getEx()) : null;
        }
    }


    private class OutcomeSummary {
        String name;
        int passedCount;
        int conditionallyPassedCount;
        int failedCount;

        OutcomeSummary(String name, int passed, int conditionallyPassed, int failed) {
            this.name = name;
            this.passedCount = passed;
            this.conditionallyPassedCount = conditionallyPassed;
            this.failedCount = failed;
        }
    }
}
