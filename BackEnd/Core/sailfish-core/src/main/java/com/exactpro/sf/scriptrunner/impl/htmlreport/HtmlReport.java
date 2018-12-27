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
package com.exactpro.sf.scriptrunner.impl.htmlreport;

import static com.exactpro.sf.scriptrunner.StatusType.FAILED;
import static java.lang.Long.compare;
import static java.util.Comparator.comparingLong;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.mvel2.math.MathProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.AMLBlockType;
import com.exactpro.sf.aml.generator.AggregateAlert;
import com.exactpro.sf.aml.script.CheckPoint;
import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MessageUtil;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.comparison.Formatter;
import com.exactpro.sf.comparison.MessageComparator;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;
import com.exactpro.sf.scriptrunner.EnvironmentSettings.RelevantMessagesSortingMode;
import com.exactpro.sf.scriptrunner.IReportStats;
import com.exactpro.sf.scriptrunner.IScriptReport;
import com.exactpro.sf.scriptrunner.LoggerRow;
import com.exactpro.sf.scriptrunner.MessageLevel;
import com.exactpro.sf.scriptrunner.OutcomeCollector;
import com.exactpro.sf.scriptrunner.ReportEntity;
import com.exactpro.sf.scriptrunner.ReportUtils;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.scriptrunner.ScriptRunException;
import com.exactpro.sf.scriptrunner.StatusDescription;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.impl.ReportStats;
import com.exactpro.sf.scriptrunner.impl.ReportTable;
import com.exactpro.sf.scriptrunner.impl.htmlreport.data.Action;
import com.exactpro.sf.scriptrunner.impl.htmlreport.data.ActionGroup;
import com.exactpro.sf.scriptrunner.impl.htmlreport.data.ActionParameter;
import com.exactpro.sf.scriptrunner.impl.htmlreport.data.BaseEntity;
import com.exactpro.sf.scriptrunner.impl.htmlreport.data.MachineLearningData;
import com.exactpro.sf.scriptrunner.impl.htmlreport.data.Message;
import com.exactpro.sf.scriptrunner.impl.htmlreport.data.ParametersTable;
import com.exactpro.sf.scriptrunner.impl.htmlreport.data.Report;
import com.exactpro.sf.scriptrunner.impl.htmlreport.data.TestCase;
import com.exactpro.sf.scriptrunner.impl.htmlreport.data.Verification;
import com.exactpro.sf.scriptrunner.impl.htmlreport.data.VerificationParameter;
import com.exactpro.sf.scriptrunner.reportbuilder.textformatter.TextColor;
import com.exactpro.sf.scriptrunner.reportbuilder.textformatter.TextStyle;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.util.BugDescription;
import com.exactpro.sf.util.BugDescription.Category;
import com.exactpro.sf.util.EnumReplacer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.MultimapBuilder.SortedSetMultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.SortedSetMultimap;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

public class HtmlReport implements IScriptReport {
    private static final Logger logger = LoggerFactory.getLogger(HtmlReport.class);
    private static final String TEMPLATE_PACKAGE_PATH = "/com/exactpro/sf/scriptrunner/template/report";
    private static final String[] PAGE_RESOURCES = new String[] { "1.gif", "2.gif", "SF_white.jpg", "SFicon.png",
            "logic.js", "htmlreport.css", "checkpoint.png", "jquery.min.js", "jquery-ui.min.js", "jquery-ui.min.css", "ui-icons_222222_256x240.png"};
    private static final String RESOURCE_FOLDER = "resources";
    private static final int MAX_VERIFICATIONS = 100;
    private static final String KNOWN_BUGS_TABLE_NAME = "Known Bugs";
    private static final String BUG_CATEGORY_COLUMN_NAME = "Bug Category";
    private static final String REPRODUCED_COLUMN_NAME = "Reproduced";
    private static final String NOT_REPRODUCED_COLUMN_NAME = "Not Reproduced";

    private static final ObjectWriter JSON_WRITER = new ObjectMapper()
            .registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false).writer();

    private final String reportFolder;
    private final IWorkspaceDispatcher workspaceDispatcher;
    private final IDictionaryManager dictionaryManager;
    private final TemplateWrapperFactory templateWrapperFactory;
    private final ReportTable warningsTable;

    private ContextType currentContext = ContextType.NONE;

    private Writer testCaseWriter;

    private Report report;
    private TestCase testCase;
    private Action action;
    private Deque<ActionGroup> actionGroups = new ArrayDeque<>();

    private int totalTestCases = 0;
    private int passedTestCases = 0;
    private int cpTestCases = 0;
    private int failedTestCases = 0;
    private int notAppTestCases = 0;

    private int nodeId = 0;
    private int verificationId = 0;
    private int sequenceId = 100000;

    private OutcomeCollector outcomes;
    private IReportStats reportStats = new ReportStats();

    private ArrayList<IReportEntryContainer> containers = new ArrayList<>();
    private Map<Integer, MachineLearningData> dataMap;
    private RelevantMessagesSortingMode relevantMessagesSortingMode;

    public HtmlReport(String reportFolder, IWorkspaceDispatcher workspaceDispatcher, IDictionaryManager dictionaryManager, RelevantMessagesSortingMode relevantMessagesSortingMode) {
        this.reportFolder = reportFolder;
        this.workspaceDispatcher = workspaceDispatcher;
        this.dictionaryManager = dictionaryManager;
        this.templateWrapperFactory = new TemplateWrapperFactory(TEMPLATE_PACKAGE_PATH);
        this.warningsTable = new ReportTable("Warnings", Arrays.asList("Line", "Column", "Message"));
        this.relevantMessagesSortingMode = relevantMessagesSortingMode;
    }

    @Override
    public void createReport(ScriptContext scriptContext, String name, String description, long scriptRunId, String environmentName, String userName) {
        logger.debug("createReport - name: {}, description: {}, id: {}, environment: {}, user: {}", name, description, scriptRunId, environmentName, userName);

        checkContext(ContextType.NONE);
        copyResources();

        String host;

        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch(UnknownHostException e) {
            host = "unknown";
        }

        report = new Report();

        report.setHost(host);
        report.setUser(userName);
        report.setName(name);
        report.setDescription(description);
        report.setId(scriptRunId);
        report.setDate(new Date());

        currentContext = ContextType.REPORT;
    }

    @Override
    public void addAlerts(Collection<AggregateAlert> aggregatedAlerts) {
        if(aggregatedAlerts == null || aggregatedAlerts.isEmpty()) {
            return;
        }

        for (AggregateAlert aggregatedAlert : aggregatedAlerts) {
            Map<String, String> warning = new HashMap<>();

            warning.put("Line", aggregatedAlert.joinLines());
            warning.put("Column", aggregatedAlert.getColumn());
            warning.put("Message", aggregatedAlert.getMessage());

            warningsTable.addRow(warning);
        }
    }

    @Override
    public void closeReport() {
        logger.debug("closeReport - name: {}, id: {}", report.getName(), report.getId());

        checkContext(ContextType.REPORT);

        updateReport();

        currentContext = ContextType.NONE;
    }

    @Override
    public void flush() {
        logger.debug("flush");
    }

    @Override
    public void createTestCase(String reference, String description, int order, int matrixOrder, String tcId, int tcHash, AMLBlockType type) {
        logger.debug("createTestCase - reference: {}, description: {}, order: {}, matrixOrder: {}, id: {}, hash: {}, type: {}", reference, description, order, matrixOrder, tcId, tcHash, type);

        checkContext(ContextType.REPORT);

        totalTestCases++;

        testCase = new TestCase();

        testCase.setName(ReportUtils.generateTestCaseName(reference, matrixOrder, type));
        testCase.setDescription(StringUtils.trimToNull(description));
        testCase.setOrder(order);
        testCase.setStartTime(new Date());
        testCase.setHash(tcHash);
        testCase.setId(tcId);

        testCaseWriter = createWriter(testCase.getName().replaceAll("\\W", "_") + ".html");

        try {
            writeHeader(testCaseWriter, testCase.getName(), true);

            TemplateWrapper testCaseContentHeaderTemplate = templateWrapperFactory.createWrapper("test_case_content_header.ftlh");

            testCaseContentHeaderTemplate.setData("description", testCase.getDescription());
            testCaseContentHeaderTemplate.write(testCaseWriter, 3);
        } catch(TemplateException | IOException e) {
            throw new ScriptRunException("Failed to create test case", e);
        }

        currentContext = ContextType.TESTCASE;
        reportStats = new ReportStats();
        dataMap = new HashMap<>();

        containers.add(new ReportTestcaseContainer(testCase));
        updateReport();
    }

    @Override
    public void closeTestCase(StatusDescription status) {
        logger.debug("closeTestCase - name: {}, status: {}", testCase.getName(), status.getStatus());

        checkContext(ContextType.TESTCASE);

        switch(status.getStatus()) {
        case FAILED:
            failedTestCases++;
            break;
        case CONDITIONALLY_PASSED:
            cpTestCases++;
            break;
        case PASSED:
            passedTestCases++;
            break;
        default:
            notAppTestCases++;
            break;
        }

        testCase.setFinishTime(new Date());
        testCase.setStatus(status);

        try {
            writeKnownBugsTable(testCaseWriter, 0, testCase.getAllKnownBugs(), testCase.getReproducedBugs());
            writeStatus(testCaseWriter, status, 0);

            if(outcomes != null && !outcomes.getGroupOrder().isEmpty()) {
                TemplateWrapper testCaseOutcomesTableTemplate = templateWrapperFactory.createWrapper("test_case_outcomes_table.ftlh");

                testCaseOutcomesTableTemplate.setData("outcomes", outcomes);
                testCaseOutcomesTableTemplate.write(testCaseWriter, 5);

                outcomes = null;
            }
            writeElements(testCaseWriter, testCase.getElements(), 5);

            boolean passed = status.getStatus() == StatusType.PASSED;
            boolean conditionallyPassed = status.getStatus() == StatusType.CONDITIONALLY_PASSED;
            long duration = testCase.getFinishTime().getTime() - testCase.getStartTime().getTime();

            writeLine(testCaseWriter, "</div>", 4);

            TemplateWrapper testCaseSummaryTemplate = templateWrapperFactory.createWrapper("test_case_summary.ftlh");

            testCaseSummaryTemplate.setData("start_time", testCase.getStartTime());
            testCaseSummaryTemplate.setData("finish_time", testCase.getFinishTime());
            testCaseSummaryTemplate.setData("hash", testCase.getHash());
            testCaseSummaryTemplate.setData("id", testCase.getId());
            testCaseSummaryTemplate.write(testCaseWriter, 4);

            writeLine(testCaseWriter, "</div>", 3);

            TemplateWrapper testCaseHeaderTemplate = templateWrapperFactory.createWrapper("test_case_header.ftlh");

            testCaseHeaderTemplate.setData("name", testCase.getName());
            testCaseHeaderTemplate.setData("description", testCase.getDescription());
            testCaseHeaderTemplate.setData("conditionallyPassed", conditionallyPassed);
            testCaseHeaderTemplate.setData("passed", passed);
            testCaseHeaderTemplate.setData("order", testCase.getOrder());
            testCaseHeaderTemplate.setData("duration", duration);
            testCaseHeaderTemplate.write(testCaseWriter, 3);

            writeLine(testCaseWriter, "<script id='machine-learning-data' type='text/javascript'>", 3);
            writeLine(testCaseWriter, "machineLearningData = " + JSON_WRITER.writeValueAsString(dataMap), 4);
            writeLine(testCaseWriter, "</script>", 3);

            writeFooter(testCaseWriter, true);
        } catch(IOException | TemplateException e) {
            throw new ScriptRunException("Failed to close test case", e);
        } finally {
            try {
                testCaseWriter.close();
            } catch (IOException e) {
                throw new ScriptRunException("Failed to close test case stream", e);
            }
        }

        reportStats.updateTestCaseStatus(status.getStatus());

        report.putAllKnownBugs(testCase.getName(), testCase.getAllKnownBugs());
        report.putReproducedBugs(testCase.getName(), testCase.getReproducedBugs());

        testCase = null;
        currentContext = ContextType.REPORT;

        updateReport();
    }

    @Override
    public void createAction(String name, String serviceName, String action, String msg, String description, Object inputParameters, CheckPoint checkPoint, String tag, int hash,
                             List<String> verificationsOrder) {
        logger.debug("createAction - name: {}, service: {}, action: {}, message: {}, description: {}, parameters: {}, tag: {}", name, serviceName, action, msg, description, inputParameters, tag);

        checkContext(ContextType.TESTCASE);

        this.action = new Action();

        this.action.setId(++sequenceId);
        this.action.setName(name);
        this.action.setMessageName(msg);
        this.action.setDescription(StringUtils.trimToNull(description));
        this.action.setParameters(convert(inputParameters, String.valueOf(this.action.getId())));
        this.action.setStartTime(System.currentTimeMillis());
        this.action.setCheckPoint(checkPoint);
        this.action.setVerificationsOrder(verificationsOrder);

        currentContext = ContextType.ACTION;

        if (Objects.nonNull(inputParameters) && inputParameters instanceof IMessage) {

            ComparisonResult result = getInitialComparisonResult((IMessage) inputParameters);
            addMachineLearningData(null, result, false);
    }

    }

    private ComparisonResult getInitialComparisonResult(IMessage inputParameters) {
        IMessage expected = inputParameters;
        IMessage actual = DefaultMessageFactory.getFactory().createMessage(expected.getName(), expected.getNamespace());

        MsgMetaData exMeta =  expected.getMetaData();
        MsgMetaData acMeta = actual.getMetaData();

        MessageUtil.transferMetadata(exMeta, acMeta);

        return MessageComparator.compare(actual, expected, new ComparatorSettings());
    }



    @Override
    public boolean isActionCreated() throws UnsupportedOperationException {
        return currentContext == ContextType.ACTION;
    }

    @Override
    public void createAction(String name, String serviceName, String action, String msg, String description, List<Object> inputParameters, CheckPoint checkPoint, String tag, int hash,
                             List<String> verificationsOrder) {
        logger.debug("createAction - name: {}, service: {}, action: {}, message: {}, description: {}, parameters: {}, tag: {}", name, serviceName, action, msg, description, inputParameters, tag);

        checkContext(ContextType.TESTCASE);

        this.action = new Action();

        this.action.setId(++sequenceId);
        this.action.setName(name);
        this.action.setMessageName(msg);
        this.action.setDescription(StringUtils.trimToNull(description));
        this.action.setParameters(convert(inputParameters, String.valueOf(this.action.getId())));
        this.action.setStartTime(System.currentTimeMillis());
        this.action.setCheckPoint(checkPoint);
        this.action.setVerificationsOrder(verificationsOrder);

        currentContext = ContextType.ACTION;

        if (Objects.nonNull(inputParameters) && !inputParameters.isEmpty()) {

            Object expected = inputParameters.get(0);
            if (Objects.nonNull(expected) && expected instanceof IMessage) {

                ComparisonResult result = getInitialComparisonResult((IMessage) inputParameters);

                addMachineLearningData(null, result, false);
            }
        }
    }

    @Override
    public void closeAction(StatusDescription status, Object actionResult) {
        logger.debug("closeAction - name: {}, status: {}", action.getName(), status.getStatus());

        checkContext(ContextType.ACTION);

        if(!actionGroups.isEmpty()) {
            String groups = actionGroups.stream().map(ActionGroup::getName).collect(Collectors.joining(", "));
            throw new ScriptRunException("Cannot close action due to presence of unclosed groups: " + groups);
        }

        long finishTime = System.currentTimeMillis();
        double duration = (finishTime - action.getStartTime()) / 1000.0;
        String nodeTitle = String.format("Action: %s (%s) [%ss]", action.getName(), status.getStatus(), duration);

        try {
            createNode(testCaseWriter, nodeTitle, generateDescription(action.getDescription(), actionResult),
                       NodeType.ACTION, status.getStatus(), null, 5, action.getCheckPoint(), null,
                       action.getVerificationsOrder(), null, true);

            String messageName = action.getMessageName();
            List<ActionParameter> parameters = action.getParameters();

            if(messageName != null && parameters != null) {
                writeParametersTable(action.getId(), messageName, parameters, action.isHasHeaders());
            }

            writeElements(testCaseWriter, action.getElements(), 7);
            writeKnownBugsTable(testCaseWriter, 7, action.getAllKnownBugs(), action.getReproducedBugs());
            writeStatus(testCaseWriter, status, 7);

            String linkToReport = action.getLinkToReport();

            if(StringUtils.isNotBlank(linkToReport)) {
                createNode(testCaseWriter, "Report", NodeType.DESCRIPTION, null, null, 7);
                writeLine(testCaseWriter, "<a href='" + linkToReport + "'>Link to report</a>", 9);
                closeNode(testCaseWriter, 7);
            }

            closeNode(testCaseWriter, 5);
        } catch(IOException | TemplateException e) {
            throw new ScriptRunException("Failed to close action", e);
        }

        if(status.isUpdateTestCaseStatus()) {
            reportStats.updateActions(status.getStatus());
        }

        MachineLearningData data = action.getMachineLearningData();

        if(data != null && status.getStatus() == StatusType.FAILED) {
            data.setPeriodEnd(finishTime);
            dataMap.put(action.getId(), data);
        }

        action = null;
        currentContext = ContextType.TESTCASE;
    }

    private void writeParametersTable(int id, String messageName, List<ActionParameter> parameters, boolean hasHeaders) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
        createNode(testCaseWriter, "Input Parameters", NodeType.INPUT, null, null, 7);

        TemplateWrapper testCaseActionParametersTemplate = templateWrapperFactory.createWrapper("test_case_parameters_table.ftlh");

        testCaseActionParametersTemplate.setData("tableId", id);
        testCaseActionParametersTemplate.setData("message_name", messageName);
        testCaseActionParametersTemplate.setData("parameters", parameters);
        testCaseActionParametersTemplate.setData("hasHeaders", hasHeaders);
        testCaseActionParametersTemplate.write(testCaseWriter, 9);

        closeNode(testCaseWriter, 7);
    }

    @Override
    public void openGroup(String name, String description) {
        if(currentContext != ContextType.ACTION && currentContext != ContextType.ACTIONGROUP) {
            throw new ScriptRunException(String.format("Invalid context: %s (expected: %s)", currentContext, Arrays.asList(ContextType.ACTION, ContextType.ACTIONGROUP)));
        }

        ActionGroup actionGroup = new ActionGroup();

        actionGroup.setName(name);
        actionGroup.setDescription(description);
        actionGroups.push(actionGroup);

        this.currentContext = ContextType.ACTIONGROUP;
    }

    @Override
    public void closeGroup(StatusDescription status) {
        checkContext(ContextType.ACTIONGROUP);

        ActionGroup actionGroup = actionGroups.pop();
        BaseEntity parent = actionGroups.isEmpty() ? action : actionGroups.peek();

        parent.addElement(actionGroup.copyWithStatus(status.getStatus()));
        parent.addAllKnownBugs(actionGroup.getAllKnownBugs());
        parent.addReproducedBugs(actionGroup.getReproducedBugs());

        if(actionGroups.isEmpty()) {
            this.currentContext = ContextType.ACTION;
        }
    }

    private void writeStatus(Writer writer, StatusDescription status, int indentSize) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
        logger.debug("writeStatus - context: {}, status: {}, description: {}", currentContext, status.getStatus(), status.getDescription());

        if(status.getStatus() != StatusType.PASSED) {
            createNode(writer, "Status", NodeType.STATUS, status.getStatus(), null, indentSize);

            TemplateWrapper statusTableTemplate = templateWrapperFactory.createWrapper("status_table.ftlh");

            statusTableTemplate.setData("status", status.getStatus());
            statusTableTemplate.setData("description", status.getDescription());
            statusTableTemplate.setData("exception", status.getCause());
            statusTableTemplate.setData("id", ++nodeId * 1000);
            statusTableTemplate.write(writer, indentSize + 2);

            closeNode(writer, indentSize);
        }
    }

    private void writeElements(Writer writer, List<Object> elements, int indentSize) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
        List<Verification> verifications = new ArrayList<>();

        for(Object element : elements) {
            if(element instanceof Message) {
                if(!verifications.isEmpty()) {
                    writeVerifications(writer, verifications, indentSize);
                    verifications.clear();
                }

                writeMessage(writer, (Message)element, indentSize);
            } else if(element instanceof Verification) {
                verifications.add((Verification)element);
            } else if(element instanceof ReportTable) {
                if(!verifications.isEmpty()) {
                    writeVerifications(writer, verifications, indentSize);
                    verifications.clear();
                }

                writeTable(writer, null, (ReportTable) element, indentSize);
            } else if (element instanceof ActionGroup) {
                ActionGroup group = (ActionGroup) element;
                createNode(testCaseWriter, group.getName(), group.getDescription(), NodeType.ACTION, group.getStatus(),
                           null, 5, null, null, Collections.emptyList(), null, true);
                writeElements(writer, group.getElements(), indentSize);
                String linkToReport = group.getLinkToReport();

                if(StringUtils.isNotBlank(linkToReport)) {
                    createNode(testCaseWriter, "Report", NodeType.DESCRIPTION, null, null, 7);
                    writeLine(testCaseWriter, "<a href='" + linkToReport + "'>Link to report</a>", 9);
                    closeNode(testCaseWriter, 7);
                }

                closeNode(testCaseWriter, 7);
            } else if(element instanceof Throwable) {
                writeException(testCaseWriter, (Throwable)element);
            } else if(element instanceof ParametersTable) {
                ParametersTable table = (ParametersTable)element;
                writeParametersTable(table.getId(), table.getMessageName(), table.getParameters(), table.isHasHeaders());
            }
        }

        if(!verifications.isEmpty()) {
            writeVerifications(writer, verifications, indentSize);
            verifications.clear();
        }
    }

    private void writeVerifications(Writer writer, List<Verification> verifications, int indentSize) {
        if(action != null) {
            logger.debug("writeVerifications - action: {}, count: {}", action.getName(), verifications.size());
        } else {
            logger.debug("writeVerifications - count: {}", verifications.size());
        }

        switch(relevantMessagesSortingMode) {
        case FAILED_FIELDS:
            verifications.sort(comparingLong(a -> getStatusTypeCount(a, FAILED)));
            break;
        case ARRIVAL_TIME:
            break;
        }

        Verification first = verifications.get(0);
        if(first.getStatusDescription().getStatus() == StatusType.FAILED){
            writeLine(writer, "<div class='sort-container'><label class='input-label' for=\"input" + first.getId() + "\">Sort verifications by fields:</label><input list=\"datalist" + first.getId() + "\" id=\"input" + first.getId() + "\" onkeyup='verificationSorter.parse(this)' class='sort-verifications-input' title='Syntax: fieldName:status,fieldName2:status'>", indentSize);
            writeLine(writer, "<div class=\"chips\"></div>", indentSize);
            writeLine(writer, "<datalist id=\"datalist" + first.getId() + "\"></datalist></div>", indentSize);
        }

        for(int i = 0; i < verifications.size(); i++) {
            Verification v = verifications.get(i);

            if(i == MAX_VERIFICATIONS) {
                String fileName = testCase.getName() + "_verifications_" + (++verificationId) + ".html";

                writeLine(writer, "<button class='ui-button ui-big-button eps-show-all-btn' href='" + fileName + "' onclick='return expandToIFrame(event, this);'>show all steps</button>", indentSize);

                try {
                    writeHeader(writer = createWriter(fileName), "", false);
                } catch(IOException | TemplateException e) {
                    throw new ScriptRunException("Failed to write header to verifications file", e);
                }

                indentSize = 0;
            }

            StatusDescription statusDescription = v.getStatusDescription();
            StatusType status = statusDescription.getStatus();
            String description = v.getDescription();
            List<VerificationParameter> parameters = v.getParameters();

            boolean hasDescription = StringUtils.isNotBlank(description);
            boolean hasParameters = parameters != null && !parameters.isEmpty();
            boolean isPassed = status == StatusType.PASSED;

            boolean isVerificationNode = hasParameters || !isPassed;
            boolean isContainOnlyComparison = hasParameters && isPassed;

            String nodeTitle = String.format("Verification: %s (%s)", v.getName(), statusDescription.getStatus());

            if(!isVerificationNode) {
                if(hasDescription) {
                    nodeTitle += " : " + StringEscapeUtils.escapeHtml4(description);
                }

                writeLine(writer, "<p class='verification'>" + nodeTitle + "</p>", indentSize);
                continue;
            }

            createNode(writer, nodeTitle, description, NodeType.VERIFICATION, statusDescription.getStatus(), null,
                       indentSize, null, v.getMessageId(), Collections.emptyList(), v.toJson(), true);

            try {
                if(hasParameters) {
                    if(!isContainOnlyComparison) {
                        createNode(writer, "Comparison Table", NodeType.COMPARISON, statusDescription.getStatus(), null,
                                   indentSize + 2);
                    }

                    writeLine(writer, "<div class='eps-table-wrapper'>", indentSize + 2);
                    writeLine(writer, "<div style='text-align: center; padding: 5px; position: relative'>Comparison Table", indentSize + 2);

                    TemplateWrapper testCaseComparisonTableTemplate = templateWrapperFactory.createWrapper("test_case_comparison_table.ftlh");

                    testCaseComparisonTableTemplate.setData("parameters", parameters);
                    testCaseComparisonTableTemplate.setData("tableId", v.getId());
                    testCaseComparisonTableTemplate.setData("hasHeaders", v.isHasHeaders());
                    testCaseComparisonTableTemplate.write(writer, indentSize + 4);

                    writeLine(writer, "</div>", indentSize + 2);

                    if(!isContainOnlyComparison) {
                        closeNode(writer, indentSize + 2);
                    }
                }

                writeElements(writer, v.getElements(), indentSize + 2);
                writeKnownBugsTable(writer, indentSize + 2, v.getAllKnownBugs(), v.getReproducedBugs());
                writeStatus(writer, statusDescription, indentSize + 2);
            } catch (Exception e) {
                throw new ScriptRunException("Failed to write verification", e);
            }

            closeNode(writer, indentSize);
        }

        if(verifications.size() > MAX_VERIFICATIONS) {
            try {
                writeFooter(writer, false);
            } catch(IOException | TemplateException e) {
                throw new ScriptRunException("Failed to write header to verifications file", e);
            }
        }
    }

    private void writeMessage(Writer writer, Message message, int indentSize) throws IOException, TemplateException {
        logger.debug("writeMessage - context: {}", currentContext);

        createNode(writer, StringUtils.join(message.getTitle(), message.getMessage()), NodeType.INFO, null,
                   message.getLevel(), indentSize, !message.getSubmessages().isEmpty());

        TemplateWrapper reportMessageTemplate = templateWrapperFactory.createWrapper("message.ftlh");

        reportMessageTemplate.setData("message", message);
        reportMessageTemplate.setData("id", ++nodeId * 1000);
        reportMessageTemplate.write(writer, indentSize + 2);

        closeNode(writer, indentSize);
    }

    private void writeTable(Writer writer, StatusType status, ReportTable table, int indentSize)
            throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
        logger.debug("writeTables - context: {}", currentContext);

        createNode(writer, "Table: " + table.getName(), NodeType.INFO, status, MessageLevel.INFO, indentSize);

        TemplateWrapper tableTemplate = templateWrapperFactory.createWrapper("table.ftlh");

        tableTemplate.setData("table", table);
        tableTemplate.write(writer, indentSize + 2);

        closeNode(writer, indentSize);
    }

    private void writeHeader(Writer writer, String title, boolean withWrapper) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
        logger.debug("writeHeader - context: {}, title: {}, withWrapper: {}", currentContext, title, withWrapper);

        TemplateWrapper headerTemplate = templateWrapperFactory.createWrapper("header.ftlh");

        headerTemplate.setData("title", title);
        headerTemplate.setData("with_wrapper", withWrapper);
        headerTemplate.write(writer);
    }

    private void writeFooter(Writer writer, boolean withWrapper) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
        logger.debug("writeFooter - context: {}, withWrapper: {}", currentContext, withWrapper);

        TemplateWrapper footerTemplate = templateWrapperFactory.createWrapper("footer.ftlh");

        footerTemplate.setData("with_wrapper", withWrapper);
        footerTemplate.write(writer, 0);
    }

    private void writeKnownBugsTable(Writer writer, int indentSize, Set<BugDescription> allKnownBugs, Set<BugDescription> reproducedBugs) throws IOException, TemplateException {
        if(allKnownBugs.isEmpty()) {
            return;
        }

        ReportTable table = new ReportTable(KNOWN_BUGS_TABLE_NAME, Arrays.asList(BUG_CATEGORY_COLUMN_NAME, REPRODUCED_COLUMN_NAME, NOT_REPRODUCED_COLUMN_NAME));

        List<Map<String, String>> rows = fillKnowBugTable(allKnownBugs, reproducedBugs);
        table.addRows(rows);

        writeTable(writer, StatusType.NA, table, indentSize);
    }

    private List<Map<String, String>> fillKnowBugTable(Set<BugDescription> allKnownBugs, Set<BugDescription> reproducedBugs) {
        SortedSetMultimap<Category, String> reproducedMap = toMultimap(reproducedBugs);
        SortedSetMultimap<Category, String> notReproducedMap = toMultimap(Sets.difference(allKnownBugs, reproducedBugs));

        List<Category> allKeys = Stream.concat(reproducedMap.keySet().stream(), notReproducedMap.keySet().stream())
                .distinct().sorted()
                .collect(Collectors.toList());

        List<Map<String, String>> rows = new ArrayList<>(allKeys.size());
        for (Category category : allKeys) {
            Set<String> reproducedSet = ObjectUtils.defaultIfNull(reproducedMap.get(category), Collections.emptySet());
            Set<String> notReproducedSet = ObjectUtils.defaultIfNull(notReproducedMap.get(category), Collections.emptySet());

            Map<String, String> row = new HashMap<>();
            row.put(BUG_CATEGORY_COLUMN_NAME, category.toString());
            row.put(REPRODUCED_COLUMN_NAME, String.join(", ", reproducedSet));
            row.put(NOT_REPRODUCED_COLUMN_NAME, String.join(", ", notReproducedSet));

            rows.add(row);
        }
        return rows;
    }

    private SortedSetMultimap<Category, String> toMultimap(Set<BugDescription> bugSet) {
        SortedSetMultimap<Category, String> bugMap = SortedSetMultimapBuilder.hashKeys().treeSetValues().build();
        for (BugDescription bugDescription : bugSet) {
            bugMap.put(bugDescription.getCategories(), bugDescription.getSubject().toUpperCase());
        }
        return bugMap;
    }

    private void writeReportKnownBugsTable(SetMultimap<String, BugDescription> allKnownBugsMap, SetMultimap<String, BugDescription> reproducedBugsMap, Writer reportWriter) throws IOException, TemplateException {
        if(allKnownBugsMap.isEmpty()) {
            return;
        }

        ReportTable table = new ReportTable(KNOWN_BUGS_TABLE_NAME, Arrays.asList("Test Case", BUG_CATEGORY_COLUMN_NAME, REPRODUCED_COLUMN_NAME, NOT_REPRODUCED_COLUMN_NAME));

        for(String testCaseDescription : allKnownBugsMap.keySet()) {
            Set<BugDescription> allKnownBugs = allKnownBugsMap.get(testCaseDescription);
            Set<BugDescription> reproducedBugs = reproducedBugsMap.get(testCaseDescription);

            List<Map<String, String>> rows = fillKnowBugTable(allKnownBugs, reproducedBugs);
            if (!rows.isEmpty()) {
                rows.get(0).put("Test Case", testCaseDescription);
                table.addRows(rows);
            }
        }

        writeTable(reportWriter, StatusType.NA, table, 4);
    }

    @Override
    public void createVerification(String name, String description, StatusDescription status, ComparisonResult result) {
        logger.debug("addVerification - name: {}, description: {}, status: {}", name, description, status.getStatus());

        Verification verification = new Verification();

        verification.setName(name);
        verification.setDescription(description);

        if(status.isUpdateTestCaseStatus()) {
            reportStats.updateActions(status.getStatus());
        }

        addMachineLearningData(status, result, true);

        verification.setStatusDescription(status);
        verification.setId(++sequenceId);
        verification.setParameters(convert(result, String.valueOf(verification.getId())));
        if (result != null && result.getMetaData() != null) {
            verification.setMessageId(result.getMetaData().getId());
        }

        Set<BugDescription> allKnownBugs = result != null ? result.getAllKnownBugs() : Collections.emptySet();
        Set<BugDescription> reproducedBugs = result != null ? result.getReproducedBugs() : Collections.emptySet();

        verification.addAllKnownBugs(allKnownBugs);
        verification.addReproducedBugs(reproducedBugs);

        testCase.addAllKnownBugs(allKnownBugs);
        testCase.addReproducedBugs(reproducedBugs);

        if (currentContext == ContextType.ACTION) {
            action.addElement(verification);
            action.addAllKnownBugs(allKnownBugs);
            action.addReproducedBugs(reproducedBugs);
        } else if (currentContext == ContextType.ACTIONGROUP) {
            ActionGroup actionGroup = actionGroups.peek();
            actionGroup.addElement(verification);
            actionGroup.addAllKnownBugs(allKnownBugs);
            actionGroup.addReproducedBugs(reproducedBugs);
        } else {
            writeVerifications(testCaseWriter, Arrays.asList(verification), 5);
        }
    }

    private void addMachineLearningData(StatusDescription status, ComparisonResult result, boolean addActual) {
        if(currentContext != ContextType.ACTION || result == null || result.getMetaData() == null) {
            return;
        }

        MachineLearningData data = action.getMachineLearningData();

        if(data == null) {
            CheckPoint checkPoint = action.getCheckPoint();
            long periodStart = checkPoint != null ? checkPoint.getTimestamp() : testCase.getStartTime().getTime();
            data = new MachineLearningData(result, periodStart);
            action.setMachineLearningData(data);
        }

        if (addActual) {
            data.addActual(result);
        }
    }

    @Override
    public void createMessage(MessageLevel level, String... messages) {
        createMessage(level, null, messages);
    }

    @Override
    public void createMessage(MessageLevel level, Throwable e, String... messages) {
        if(ArrayUtils.isEmpty(messages)) {
            throw new ScriptRunException("Message array is empty");
        }

        Message infoMessage = new Message();

        infoMessage.setLevel(level);
        infoMessage.setMessage(messages[0]);
        infoMessage.setCause(e);
        infoMessage.setTitle("Message: ");

        for(int i = 1; i < messages.length; i++) {
            infoMessage.addSubmessage(messages[i]);
        }

        addMessage(infoMessage);
    }

    @Override
    public void createMessage(TextColor color, TextStyle style, String... messages) {
        if (ArrayUtils.isEmpty(messages)) {
            throw new ScriptRunException("Message array is empty");
        }
        Message infoMessage = new Message();
        infoMessage.setMessage(convertMessage(color, style, messages[0]));
        infoMessage.setLevel(MessageLevel.INFO);

        for (int i = 1; i < messages.length; i++) {
            infoMessage.addSubmessage(convertMessage(color, style, messages[i]));
        }

        switch (currentContext) {
        case TESTCASE:
            try {
                writeMessage(testCaseWriter, infoMessage, 5);
            } catch (IOException | TemplateException e) {
                throw new ScriptRunException("Failed write message", e);
            }
            break;
        case ACTION:
        case ACTIONGROUP:
        default:
            addMessage(infoMessage);
            break;
        }
    }

    private String convertMessage(TextColor color, TextStyle style, String message) {
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("<div class='custom-msg ");
        messageBuilder.append(color.getCssClass());
        messageBuilder.append("' ");
        messageBuilder.append("style='");
        messageBuilder.append(style.getCssRule());
        messageBuilder.append("'");
        messageBuilder.append(">");
        messageBuilder.append(message);
        messageBuilder.append("</div>");

        return messageBuilder.toString();
    }

    private void addMessage(Message message) {
        logger.debug("addMessage - context: {}, message: {}", currentContext, message);
        switch(currentContext) {
        case ACTION:
            action.addElement(message);
            break;
        case ACTIONGROUP:
            actionGroups.peek().addElement(message);
            break;
        case TESTCASE:
            testCase.addElement(message);
            break;
        default:
            throw new ScriptRunException("Cannot cannot create message in context: " + currentContext);
        }
    }

    @Override
    public void createException(Throwable cause) {
        logger.debug("createException - cause: {}", cause);

        switch(currentContext) {
        case ACTION:
            action.addElement(cause);
            break;
        case ACTIONGROUP:
            actionGroups.peek().addElement(cause);
            break;
        case TESTCASE:
            testCase.addElement(cause);
            break;
        case REPORT:
            containers.add(new ReportExceptionContainer(cause));
            updateReport();
            break;
        default:
            throw new ScriptRunException("Cannot cannot create message in context: " + currentContext);
        }
    }

    private void writeException(Writer writer, Throwable cause) {
        createNode(writer, "Status", NodeType.STATUS, StatusType.FAILED, null, 5);

        try {
            TemplateWrapper statusTableTemplate = templateWrapperFactory.createWrapper("status_table.ftlh");

            statusTableTemplate.setData("status", StatusType.FAILED);
            statusTableTemplate.setData("description", cause.getClass() + ": " + cause.getMessage());
            statusTableTemplate.setData("exception", cause);
            statusTableTemplate.setData("id", ++nodeId * 1000);
            statusTableTemplate.write(writer, 7);
        } catch(IOException | TemplateException e) {
            logger.error("Failed to create exception", e);
            throw new ScriptRunException("Failed to create exception", e);
        }

        closeNode(writer, 5);
    }

    @Override
    public void createTable(ReportTable table) {
        String tableName = table.getName();
        logger.debug("createTable - context: {}, name: {}", currentContext, tableName);

        switch(currentContext) {
        case REPORT:
            containers.add(new ReportTableContainer(table));
            updateReport();
            break;
        case TESTCASE:
            if(tableName.equals("Messages")) {
                createNode(testCaseWriter, "Table: All messages", NodeType.INFO, null, MessageLevel.INFO, 5);

                try {
                    TemplateWrapper testCaseMessagesTableTemplate = templateWrapperFactory.createWrapper("test_case_messages_table.ftlh");

                    testCaseMessagesTableTemplate.setData("table", table);
                    testCaseMessagesTableTemplate.write(testCaseWriter, 7);
                } catch(IOException | TemplateException e) {
                    throw new ScriptRunException("Failed to write messages table", e);
                }

                closeNode(testCaseWriter, 5);

                ReportTable rejectedMessagesTable = getRejectedMessagesTable(table);

                if(!rejectedMessagesTable.isEmpty()) {
                    createNode(testCaseWriter, "Table: Rejected messages", NodeType.INFO, null, MessageLevel.INFO, 5);

                    try {
                        TemplateWrapper testCaseMessagesTableTemplate = templateWrapperFactory.createWrapper("test_case_messages_table.ftlh");

                        testCaseMessagesTableTemplate.setData("table", rejectedMessagesTable);
                        testCaseMessagesTableTemplate.write(testCaseWriter, 7);
                    } catch(IOException | TemplateException e) {
                        throw new ScriptRunException("Failed to write rejected messages table", e);
                    }

                    closeNode(testCaseWriter, 5);
                }
            } else {
                testCase.addElement(table);
            }
            break;
        case ACTION:
            action.addElement(table);
            break;
        case ACTIONGROUP:
            actionGroups.peek().addElement(table);
            break;
        default:
            throw new ScriptRunException("Cannot cannot create table in context: " + currentContext);
        }
    }

    @Override
    public void createLogTable(List<String> header, List<LoggerRow> rows) {
        logger.debug("createLogTable");

        if(currentContext != ContextType.TESTCASE) {
            throw new ScriptRunException("Cannot cannot create log table in context: " + currentContext);
        }

        createNode(testCaseWriter, "Table: Logs", NodeType.INFO, null, MessageLevel.INFO, 5);

        try {
            TemplateWrapper testCaseLogTableTemplate = templateWrapperFactory.createWrapper("test_case_log_table.ftlh");

            testCaseLogTableTemplate.setData("headers", header);
            testCaseLogTableTemplate.setData("rows", rows);
            testCaseLogTableTemplate.setData("id", ++nodeId * 1000);
            testCaseLogTableTemplate.write(testCaseWriter, 7);
        } catch(IOException | TemplateException e) {
            throw new ScriptRunException("Failed to write log table", e);
        }

        closeNode(testCaseWriter, 5);
    }

    @Override
    public void createParametersTable(String messageName, Object message) {
        logger.debug("createParametersTable - messageName: {}, message: {}", messageName, message);

        if(currentContext != ContextType.ACTION && currentContext != ContextType.ACTIONGROUP) {
            throw new ScriptRunException(String.format("Invalid context: %s", currentContext));
        }

        Objects.requireNonNull(message, "message cannot be null");

        if(StringUtils.isBlank(messageName)) {
            throw new IllegalArgumentException("messageName cannot be null");
        }

        int id = ++sequenceId;
        List<ActionParameter> parameters = convert(message, String.valueOf(id));
        boolean hasHeaders = parameters.stream().skip(1).anyMatch(ActionParameter::isHeader);
        ParametersTable table = new ParametersTable(id, messageName, parameters, hasHeaders);

        switch(currentContext) {
        case ACTION:
            action.addElement(table);
            break;
        case ACTIONGROUP:
            actionGroups.peek().addElement(table);
            break;
        default:
            throw new ScriptRunException("Cannot cannot create table in context: " + currentContext);
        }
    }

    @Override
    public void setOutcomes(OutcomeCollector outcomes) {
        logger.debug("setOutcomes");
        this.outcomes = outcomes;
    }

    @Override
    public void createLinkToReport(String linkToReport) {
        logger.debug("createLinkToReport - link: {}", linkToReport);

        switch(currentContext) {
        case ACTION:
            action.setLinkToReport(linkToReport);
            break;
        case ACTIONGROUP:
            actionGroups.peek().setLinkToReport(linkToReport);
            break;
        default:
            throw new ScriptRunException("Cannot cannot create link to report in context: " + currentContext);
        }
    }

    @Override
    public IReportStats getReportStats() {
        return reportStats;
    }

    private void copyResources() {
        try {
            for(String resource : PAGE_RESOURCES) {
                File destinationFile = workspaceDispatcher.createFile(FolderType.REPORT, true, reportFolder,
                                                                      RESOURCE_FOLDER, resource);
                FileUtils.copyURLToFile(getClass().getResource(resource), destinationFile);
            }
        } catch(WorkspaceSecurityException | IOException e) {
            logger.error("Failed to copy page resources to report folder", e);
            throw new ScriptRunException("Failed to copy page resources to report folder", e);
        }
    }


    private void createNode(Writer writer, String nodeTitle, NodeType nodeType, StatusType statusType,
                            MessageLevel messageLevel, int indentSize) {
        createNode(writer, nodeTitle, null, nodeType, statusType, messageLevel, indentSize, null, null,
                   Collections.emptyList(), null, true);
    }

    private void createNode(Writer writer, String nodeTitle, NodeType nodeType, StatusType statusType,
                            MessageLevel messageLevel, int indentSize, boolean hasChild) {
        createNode(writer, nodeTitle, null, nodeType, statusType, messageLevel, indentSize, null, null,
                   Collections.emptyList(), null, hasChild);
    }

    private void createNode(Writer writer, String nodeTitle, String nodeDescription, NodeType nodeType,
                            StatusType statusType, MessageLevel messageLevel, int indentSize, CheckPoint checkPoint,
                            Long msgId, List<String> verificationsOrder, String jsonVerificationResult,
                            boolean hasChild) {
        logger.debug("createNode - context: {}, title: {}, type: {}", currentContext, nodeTitle, nodeType);

        String nodeClass = getNodeClass(nodeType, statusType, messageLevel);

        try {
            TemplateWrapper nodeOpenTemplate = templateWrapperFactory.createWrapper("node_create.ftlh");

            nodeOpenTemplate.setData("id", ++nodeId);
            nodeOpenTemplate.setData("node_class", nodeClass);
            nodeOpenTemplate.setData("title", nodeTitle);
            nodeOpenTemplate.setData("description", StringEscapeUtils.escapeHtml4(StringUtils.stripToNull(nodeDescription)));
            nodeOpenTemplate.setData("action", nodeType == NodeType.ACTION);
            nodeOpenTemplate.setData("status_type", statusType);
            nodeOpenTemplate.setData("checkPoint", checkPoint);
            nodeOpenTemplate.setData("hasChild", hasChild);
            nodeOpenTemplate.setData("msgId", msgId);
            nodeOpenTemplate.setData("verificationsOrder", String.join(",", verificationsOrder));
            nodeOpenTemplate.setData("jsonVerificationResult", jsonVerificationResult);
            nodeOpenTemplate.write(writer, indentSize);
        } catch(IOException | TemplateException e) {
            throw new ScriptRunException("Failed to open node: " + nodeTitle, e);
        }

    }

    private String getNodeClass(NodeType nodeType, StatusType statusType, MessageLevel messageLevel) {
        String nodeClass = "";

        if (nodeType == NodeType.TESTCASE && statusType == StatusType.PASSED)
            nodeClass = "testcase_passed";
        else if (nodeType == NodeType.TESTCASE && statusType == StatusType.CONDITIONALLY_PASSED)
            nodeClass = "testcase_conditionally_passed";
        else if (nodeType == NodeType.TESTCASE && statusType == StatusType.FAILED)
            nodeClass = "testcase_failed";
        else if (nodeType == NodeType.TESTCASE && statusType == StatusType.SKIPPED)
            nodeClass = "testcase_skipped";
        else if (nodeType == NodeType.DESCRIPTION)
            nodeClass = "description";
        else if (nodeType == NodeType.ACTION && statusType == StatusType.PASSED)
            nodeClass = "action_passed";
        else if (nodeType == NodeType.ACTION && statusType == StatusType.CONDITIONALLY_PASSED)
            nodeClass = "action_conditionally_passed";
        else if (nodeType == NodeType.ACTION && statusType == StatusType.FAILED)
            nodeClass = "action_failed";
        else if (nodeType == NodeType.ACTION && statusType == StatusType.SKIPPED)
            nodeClass = "action_skipped";
        else if (nodeType == NodeType.INPUT)
            nodeClass = "inputparams";
        else if (nodeType == NodeType.INFO)
            nodeClass = "info_" + messageLevel.toString().toLowerCase();
        else if (nodeType == NodeType.STATUS && statusType == StatusType.PASSED)
            nodeClass = "statustype_passed";
        else if (nodeType == NodeType.STATUS && statusType == StatusType.CONDITIONALLY_PASSED)
            nodeClass = "statustype_conditionally_passed";
        else if (nodeType == NodeType.STATUS && statusType == StatusType.CONDITIONALLY_FAILED)
            nodeClass = "statustype_conditionally_failed";
        else if (nodeType == NodeType.STATUS && statusType == StatusType.FAILED)
            nodeClass = "statustype_failed";
        else if (nodeType == NodeType.EXCEPTION && statusType == StatusType.CONDITIONALLY_PASSED)
            nodeClass = "conditionally_exceptiontype";
        else if (nodeType == NodeType.EXCEPTION)
            nodeClass = "exceptiontype";
        else if (nodeType == NodeType.ALERTS)
            nodeClass = "alerttype";
        else if (nodeType == NodeType.VERIFICATION && statusType == StatusType.PASSED)
            nodeClass = "verification_passed";
        else if (nodeType == NodeType.VERIFICATION && statusType == StatusType.CONDITIONALLY_PASSED)
            nodeClass = "verification_conditionally_passed";
        else if (nodeType == NodeType.VERIFICATION && statusType == StatusType.FAILED)
            nodeClass = "verification_failed";
        else if (nodeType == NodeType.VERIFICATION && statusType == StatusType.SKIPPED)
            nodeClass = "verification_skipped";
        else if (nodeType == NodeType.COMPARISON && statusType == StatusType.PASSED)
            nodeClass = "comparisontype_passed";
        else if (nodeType == NodeType.COMPARISON && statusType == StatusType.CONDITIONALLY_PASSED)
            nodeClass = "comparisontype_conditionally_passed";
        else if (nodeType == NodeType.COMPARISON && statusType != StatusType.PASSED)
            nodeClass = "comparisontype_notpassed";
        else if (nodeType == NodeType.MATRIX && statusType == StatusType.PASSED)
            nodeClass = "matrix_passed";
        else if (nodeType == NodeType.MATRIX && statusType == StatusType.CONDITIONALLY_PASSED)
            nodeClass = "matrix_conditionally_passed";
        else if (nodeType == NodeType.MATRIX && statusType == StatusType.FAILED)
            nodeClass = "matrix_failed";
        else if (nodeType == NodeType.MATRIX && statusType == StatusType.SKIPPED)
            nodeClass = "matrix_skipped";

        return nodeClass;
    }

    private String getStatusClass(StatusType status) {
        if(status == null) {
            return null;
        }

        switch(status) {
        case CONDITIONALLY_FAILED:
            return "statuscondfailed";
        case CONDITIONALLY_PASSED:
            return "statuscondpassed";
        case FAILED:
            return "statusfailed";
        case PASSED:
            return "statuspassed";
        default:
            return null;
        }
    }

    private void closeNode(Writer writer, int indentSize) {
        logger.debug("closeNode - context: {}", currentContext);

        writeLine(writer, "</div>", indentSize + 1);
        writeLine(writer, "</div>", indentSize);
    }

    private void writeLine(Writer writer, String line, int indentSize) {
        try {
            writer.write(StringUtils.repeat(StringUtils.SPACE, indentSize * 4));
            writer.write(line);
            writer.write(System.lineSeparator());
        } catch(IOException e) {
            throw new ScriptRunException("Failed to write line: " + line, e);
        }
    }

    private Writer createWriter(String fileName) {
        logger.debug("createWriter - fileName: {}", fileName);

        try {
            File file = workspaceDispatcher.createFile(FolderType.REPORT, true, reportFolder, fileName);
            return new BufferedWriter(new FileWriter(file));
        } catch(WorkspaceSecurityException | IOException e) {
            throw new ScriptRunException("Failed to create writer for file: " + fileName, e);
        }
    }

    private List<ActionParameter> convert(Object value, String actionId) {
        if(value == null) {
            return null;
        }

        if(value instanceof IMessage) {
            IMessage message = (IMessage)value;
            IMessageStructure messageStructure = getMessageStructure(message.getMetaData());

                    if(messageStructure != null) {
                try {
                        value = EnumReplacer.replaceEnums(message, messageStructure);
                } catch(Exception e) {
                    logger.debug("Enum replacement has failed", e);
                }
            }
        }

        return convert(new ReportEntity("Parameters", value), actionId, 0, -1);
    }

    private List<ActionParameter> convert(ReportEntity reportEntity, String parentRaw, int counter, int nestingLevel) {
        List<ActionParameter> parameters = new ArrayList<>();
        List<ReportEntity> fields = reportEntity.getFields();
        String id = parentRaw + "_" + counter;
        String formattedValue = Formatter.formatForHtml(reportEntity.getValue(), true);
        ActionParameter parameter = new ActionParameter(id, reportEntity.getName(), formattedValue, nestingLevel - 1, reportEntity.hasFields());
        parameters.add(parameter);

        for(int i = 0; i < fields.size(); i++) {
            parameters.addAll(convert(fields.get(i), id, i, nestingLevel + 1));
        }

        return parameters;
    }

    private List<VerificationParameter> convert(ComparisonResult result, String verificationId) {
        if(result == null) {
            return null;
        }

        IMessageStructure structure = getMessageStructure(result.getMetaData());

        if(structure != null) {
            EnumReplacer.replaceEnums(result, structure);
        }

        return convert(result, verificationId, 0, 0);
    }

    private IMessageStructure getMessageStructure(MsgMetaData metaData) {
        if(metaData == null) {
            return null;
        }

        SailfishURI dictionaryURI = metaData.getDictionaryURI();

        if(dictionaryURI == null) {
            return null;
        }

        IDictionaryStructure dictionaryStructure = dictionaryManager.getDictionary(dictionaryURI);

        if(dictionaryStructure == null) {
            return null;
        }

        return dictionaryStructure.getMessageStructure(metaData.getMsgName());
    }

    private List<VerificationParameter> convert(ComparisonResult result, String parentRaw, int counter, int nestingLevel) {
        List<VerificationParameter> parameters = new ArrayList<>();

        VerificationParameter parameter = new VerificationParameter();


        parameter.setId(parentRaw + "_" + counter);

        parameter.setName(result.getName());
        parameter.setStatus(result.getStatus());
        parameter.setHeader(result.getStatus() == null || result.hasResults());
        parameter.setFailReason(result.getExceptionMessage());
        parameter.setStatusClass(getStatusClass(result.getStatus()));
        parameter.setActual(result.getActual());
        parameter.setExpected(result);
        parameter.setPrecision(result.getDoublePrecision());
        parameter.setSystemPrecision(result.getSystemPrecision());
        parameter.setLevel(nestingLevel - 2);
        parameters.add(parameter);

        if(result.hasResults()) {
            int i = 0;
            for (ComparisonResult subResult : result) {
                parameters.addAll(convert(subResult, parameter.getId(), i++, nestingLevel + 1));
            }
        }

        return parameters;
    }

    private ReportTable getRejectedMessagesTable(ReportTable table) {
        ReportTable rejectedTable = new ReportTable(table.getName(), table.getHeader());

        for(Map<String, String> row : table.getRows()) {
            String msgName = Objects.toString(row.get("MsgName"));

            if(msgName.endsWith(MessageUtil.MESSAGE_REJECTED_POSTFIX)) {
                rejectedTable.addRow(row);
            }
        }

        return rejectedTable;
    }

    private void checkContext(ContextType expectedContext) {
        if(currentContext != expectedContext) {
            throw new ScriptRunException(String.format("Invalid context: %s (expected: %s)", currentContext, expectedContext));
        }
    }

    private String generateDescription(String originalDescription, Object returnedValue) {
        if(!(returnedValue instanceof IMessage)) {
            return originalDescription;
        }

        IMessage message = (IMessage)returnedValue;
        IMessageStructure messageStructure = Optional.ofNullable(message.getMetaData().getDictionaryURI())
                .map(x -> dictionaryManager.getDictionary(x))
                .map(x -> x.getMessageStructure(message.getName())).orElse(null);

        if(messageStructure == null) {
            return originalDescription;
        }

        StringBuilder generatedDescription = new StringBuilder();

        for(IFieldStructure field : messageStructure.getFields()) {
            if(field.isComplex()) {
                continue;
            }

            String descriptionPrefix = (String)field.getAttributeValueByName(MessageHelper.ATTRIBUTE_DESCRIPTION_PREFIX);

            if(descriptionPrefix == null) {
                continue;
            }

            if(generatedDescription.length() > 0) {
                generatedDescription.append("; ");
            }

            String name = field.getName();

            generatedDescription.append(descriptionPrefix).append(name);
            generatedDescription.append("=").append(message.<Object>getField(name));
        }

        if(originalDescription != null) {
            if(generatedDescription.length() > 0) {
                generatedDescription.append(" (");
            }

            generatedDescription.append(originalDescription);

            if(generatedDescription.length() > 0) {
                generatedDescription.append(")");
            }
        }

        return generatedDescription.toString();
    }

    private void updateReport() {
        if (reportStats == null) {
            reportStats = new ReportStats();
        }

        logger.debug("updating report: " + report.getId());

        Writer reportWriter = createWriter("report.html");


        //header
        try {
            writeHeader(reportWriter, "TestScript: " + report.getName(), true);

            TemplateWrapper reportHeaderTemplate = templateWrapperFactory.createWrapper("report_header.ftlh");

            reportHeaderTemplate.setData("name", report.getName());
            reportHeaderTemplate.write(reportWriter, 3);

            writeLine(reportWriter, "<div>", 3);
            writeLine(reportWriter, "<div id='content-wrapper'>", 3);
            writeLine(reportWriter, "<table id='content'>", 4);
        } catch(WorkspaceSecurityException | IOException | TemplateException e) {
            throw new ScriptRunException("Failed to create report", e);
        }


        //content
        for (IReportEntryContainer container : containers) {
            container.write(reportWriter);
        }


        //alerts
        if (!warningsTable.isEmpty()) {
            writeLine(reportWriter, "<tr><td>", 5);
            createNode(reportWriter, "Alerts", NodeType.ALERTS, null, null, 6);

            try {
                TemplateWrapper reportAlertsTableTemplate = templateWrapperFactory.createWrapper("report_alerts_table.ftlh");

                reportAlertsTableTemplate.setData("table", warningsTable);
                reportAlertsTableTemplate.write(reportWriter, 8);
            } catch(IOException | TemplateException e) {
                throw new ScriptRunException("Failed to write alerts table", e);
            }

            closeNode(reportWriter, 6);
            writeLine(reportWriter, "</tr></td>", 5);
        }


        //known bugs
        try {
            writeLine(reportWriter, "<tr><td>", 5);
            writeElements(reportWriter, report.getElements(), 6);
            writeLine(reportWriter, "</tr></td>", 5);
            writeReportKnownBugsTable(report.getAllKnownBugsMap(), report.getReproducedBugsMap(), reportWriter);
            writeLine(reportWriter, "</table>", 4);
            writeLine(reportWriter, "</div>", 3);
        } catch (IOException | TemplateException e) {
            throw new ScriptRunException("Failed to write known bugs table", e);
        }


        //summary
        try {
            TemplateWrapper reportSummaryTemplate = templateWrapperFactory.createWrapper("report_summary.ftlh");

            reportSummaryTemplate.setData("host", report.getHost());
            reportSummaryTemplate.setData("user", report.getUser());
            reportSummaryTemplate.setData("duration", System.currentTimeMillis() - report.getDate().getTime());
            reportSummaryTemplate.setData("id", report.getId());
            reportSummaryTemplate.setData("date", report.getDate());
            reportSummaryTemplate.setData("version", SFLocalContext.getDefault().getVersion());
            reportSummaryTemplate.setData("precision", MathProcessor.COMPARISON_PRECISION.toPlainString());

            if (SFLocalContext.getDefault().getPluginVersions().size() > 1) {

                StringBuilder pluginsLine = new StringBuilder();

                for (IVersion version : SFLocalContext.getDefault().getPluginVersions()) {

                    if (version.isGeneral()) {
                        continue;
                    }

                    pluginsLine.append(version.getAlias());
                    pluginsLine.append(" (");
                    pluginsLine.append(version.buildVersion());
                    pluginsLine.append(") ");
                }

                reportSummaryTemplate.setData("plugins", pluginsLine.toString());
                reportSummaryTemplate.setData("plugins_visible", true);
            } else {
                reportSummaryTemplate.setData("plugins_visible", false);
            }

            reportSummaryTemplate.setData("tc_total", totalTestCases);
            reportSummaryTemplate.setData("tc_passed", passedTestCases);
            reportSummaryTemplate.setData("tc_failed", failedTestCases);
            reportSummaryTemplate.setData("tc_cp", cpTestCases);
            reportSummaryTemplate.setData("tc_notapp", notAppTestCases);
            reportSummaryTemplate.write(reportWriter, 3);
            writeLine(reportWriter, "</div>", 3);

            writeFooter(reportWriter, true);
        } catch(IOException | TemplateException e) {
            throw new ScriptRunException("Failed to write summary", e);
        } finally {
            try {
                reportWriter.close();
            } catch (IOException e) {
                throw new ScriptRunException("Failed to close report stream", e);
            }
        }
    }

    private static long getStatusTypeCount(Verification verification, StatusType status) {
        return verification.getParameters()
                .stream()
                .filter(parameter -> parameter.getStatus() == status)
                .count();
    }

    public enum ContextType {
        NONE,
        REPORT,
        TESTCASE,
        ACTION,
        ACTIONGROUP
    }

    private enum NodeType {
        MATRIX,
        TESTCASE,
        ACTION,
        VERIFICATION,
        INFO,
        DESCRIPTION,
        INPUT,
        STATUS,
        ALERTS,
        EXCEPTION,
        COMPARISON
    }

    private interface IReportEntryContainer {
        public void write(Writer writer);
    }

    private class ReportTestcaseContainer implements IReportEntryContainer {

        TestCase testcase;

        public ReportTestcaseContainer(TestCase testcase) {
            this.testcase = testcase;
        }

        @Override
        public void write(Writer writer) {
            Long duration = null;
            StatusType status = null;
            if (this.testcase.getStatus() != null && this.testcase.getFinishTime() != null) {
                status = this.testcase.getStatus().getStatus();
                duration = this.testcase.getFinishTime().getTime() - this.testcase.getStartTime().getTime();
            }

            TemplateWrapper reportTestCaseLinkTemplate;
            try {
                reportTestCaseLinkTemplate = templateWrapperFactory.createWrapper("report_test_case_link.ftlh");

                reportTestCaseLinkTemplate.setData("name", this.testcase.getName());
                reportTestCaseLinkTemplate.setData("fileName", this.testcase.getName().replaceAll("\\W", "_"));
                reportTestCaseLinkTemplate.setData("description", this.testcase.getDescription());
                reportTestCaseLinkTemplate.setData("status", status);
                reportTestCaseLinkTemplate.setData("duration", duration);
                reportTestCaseLinkTemplate.write(writer, 5);

            } catch (TemplateException | IOException e) {
                logger.warn("unable to add test case link", e);
            }
        }
    }

    private class ReportExceptionContainer implements IReportEntryContainer {

        Throwable exception;

        public ReportExceptionContainer(Throwable exception) {
            this.exception = exception;
        }

        @Override
        public void write(Writer writer) {
            writeLine(writer, "<tr><td>", 5);
            writeException(writer, exception);
            writeLine(writer, "</tr></td>", 5);
        }
    }

    private class ReportTableContainer implements IReportEntryContainer {

        ReportTable table;

        public ReportTableContainer(ReportTable table) {
            this.table = table;
        }

        @Override
        public void write(Writer writer) {
            report.addElement(table);
        }
    }
}
