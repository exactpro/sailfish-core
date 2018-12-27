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
package com.exactpro.sf.aml.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.exactpro.sf.actions.ActionUtil;
import com.exactpro.sf.aml.AML;
import com.exactpro.sf.aml.AMLAction;
import com.exactpro.sf.aml.AMLBlockType;
import com.exactpro.sf.aml.AMLPrivateActions;
import com.exactpro.sf.aml.AMLTestCase;
import com.exactpro.sf.aml.AddToReport;
import com.exactpro.sf.aml.AfterMatrix;
import com.exactpro.sf.aml.BeforeMatrix;
import com.exactpro.sf.aml.Description;
import com.exactpro.sf.aml.ExecutionSequence;
import com.exactpro.sf.aml.Hash;
import com.exactpro.sf.aml.Id;
import com.exactpro.sf.aml.Reference;
import com.exactpro.sf.aml.Type;
import com.exactpro.sf.aml.script.AMLHashMap;
import com.exactpro.sf.aml.script.CheckPoint;
import com.exactpro.sf.aml.scriptutil.MessageCount;
import com.exactpro.sf.aml.scriptutil.StaticUtil;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.common.util.StringUtil;
import com.exactpro.sf.common.util.TextOutputStream;
import com.exactpro.sf.comparison.conversion.MultiConverter;
import com.exactpro.sf.configuration.DictionaryManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.scriptrunner.IScriptReport;
import com.exactpro.sf.scriptrunner.Outcome;
import com.exactpro.sf.scriptrunner.Outcome.Status;
import com.exactpro.sf.scriptrunner.SailFishAction;
import com.exactpro.sf.scriptrunner.SailFishTestCase;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.scriptrunner.ScriptProgress;
import com.exactpro.sf.scriptrunner.StatusDescription;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.TestScriptProgressListener;
import com.exactpro.sf.scriptrunner.actionmanager.exceptions.ActionCallException;
import com.exactpro.sf.services.util.ServiceUtil;
import com.exactpro.sf.util.KnownBugException;
import com.exactpro.sf.util.MessageKnownBugException;

public abstract class AbstractCodeBuilder {
    public static final String TAB1 = "\t";
    public static final String TAB2 = "\t\t";
    public static final String TAB3 = "\t\t\t";
    public static final String TAB4 = "\t\t\t\t";
    public static final String TAB5 = "\t\t\t\t\t";

    public static final String EOL = System.getProperty("line.separator");

    public void writePackageName(TextOutputStream stream) throws IOException {
        writeCopyright(stream);

        stream.writeLine("package %s;", AML.PACKAGE_NAME);
        stream.writeLine();
    }

    public void writeCopyright(TextOutputStream stream) throws IOException {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        stream.writeLine("/******************************************************************************");
        stream.writeLine("* Copyright (c) 2009-%s, Exactpro Systems LLC", currentYear);
        stream.writeLine("* www.exactpro.com");
        stream.writeLine("* Build Software to Test Software");
        stream.writeLine("*");
        stream.writeLine("* All rights reserved.");
        stream.writeLine("* This is unpublished, licensed software, confidential and proprietary");
        stream.writeLine("* information which is the property of Exactpro Systems LLC or its licensors.");
        stream.writeLine("******************************************************************************/");
    }

    public void writeImports(TextOutputStream stream) throws IOException {
        Set<String> imports = new TreeSet<>();

        imports.add("com.exactpro.sf.common.util.*");
        imports.add("java.math.*");
        imports.add("java.util.*");
        imports.add(File.class.getCanonicalName());
        imports.add(OutputStream.class.getCanonicalName());
        imports.add(FileOutputStream.class.getCanonicalName());
        imports.add(AfterMatrix.class.getCanonicalName());
        imports.add(BeforeMatrix.class.getCanonicalName());
        imports.add(KnownBugException.class.getCanonicalName());
        imports.add(MessageKnownBugException.class.getCanonicalName());
        imports.add(ActionCallException.class.getCanonicalName());
        imports.add(ActionUtil.class.getCanonicalName());
        imports.add(AMLHashMap.class.getCanonicalName());
        imports.add(AMLPrivateActions.class.getCanonicalName());
        imports.add(CheckPoint.class.getCanonicalName());
        imports.add(Description.class.getCanonicalName());
        imports.add(DictionaryManager.class.getCanonicalName());
        imports.add(ExecutionSequence.class.getCanonicalName());
        imports.add(Id.class.getCanonicalName());
        imports.add(Hash.class.getCanonicalName());
        imports.add(IScriptReport.class.getCanonicalName());
        imports.add(Map.class.getCanonicalName());
        imports.add(Outcome.class.getCanonicalName());
        imports.add(Outcome.Status.class.getCanonicalName());
        imports.add(SailFishAction.class.getCanonicalName());
        imports.add(SailFishTestCase.class.getCanonicalName());
        imports.add(ScriptContext.class.getCanonicalName());
        imports.add(ScriptProgress.class.getCanonicalName());
        imports.add(StatusDescription.class.getCanonicalName());
        imports.add(StatusType.class.getCanonicalName());
        imports.add(TestScriptProgressListener.class.getCanonicalName());
        imports.add(SFLocalContext.class.getCanonicalName());
        imports.add(Status.class.getCanonicalName());
        imports.add(SailfishURI.class.getCanonicalName());
        imports.add(LocalDateTime.class.getCanonicalName());
        imports.add(LocalDate.class.getCanonicalName());
        imports.add(LocalTime.class.getCanonicalName());
        imports.add(MessageCount.class.getCanonicalName());
        imports.add(FolderType.class.getCanonicalName());
        imports.add(MultiConverter.class.getCanonicalName());
        imports.add(AddToReport.class.getCanonicalName());
        imports.add(AMLBlockType.class.getCanonicalName());
        imports.add(Type.class.getCanonicalName());
        imports.add(Reference.class.getCanonicalName());

        for (String imp : imports) {
            stream.writeLine("import %s;", imp);
        }

        stream.writeLine("import static %s.*;", StaticUtil.class.getCanonicalName());
        stream.writeLine("import static %s.*;", ServiceUtil.class.getCanonicalName());
    }

    public void writeClassDefinition(TextOutputStream stream, String className, String superClassName) throws IOException {
        stream.writeLine();
        stream.write("public class %s", className);

        if (StringUtils.isNotBlank(superClassName)) {
            stream.write(" extends %s", superClassName);
        }

        stream.writeLine(" {");
        stream.writeLine();
    }

    public void writeMessagesMapDefinition(TextOutputStream stream, String mapName) throws IOException {
        stream.writeLine(3, "Map<String, Object> %s = new AMLHashMap<String, Object>();", mapName);
        stream.writeLine();
    }

    public abstract void writeMainClassDefinition(TextOutputStream stream, String className) throws IOException;

    public abstract void writeActionClassDefinition(TextOutputStream stream, String className) throws IOException;

    public void writeLogger(TextOutputStream stream, String loggerName, boolean isStatic) throws IOException {
        String loggerClass = org.apache.log4j.Logger.class.getCanonicalName();
        String varPrefix = isStatic ? "static " : "";

        stream.writeLine(1, "%s%s %s = %2$s.getLogger(\"TimeStamps\");", varPrefix, loggerClass, loggerName);
        stream.writeLine();
    }

    public void writeTimeLog(TextOutputStream stream, String loggerName, String logMessage, int tabs) throws IOException {
        stream.writeLine(tabs, "%s.debug(\"%s \"+System.currentTimeMillis() +\" \"+new Date());", loggerName, logMessage);
    }

    public void writeJavaHeader(TextOutputStream stream) throws IOException {
        writePackageName(stream);
        writeImports(stream);
    }

    public abstract void writeMainMethodAnnotations(TextOutputStream stream, AMLTestCase testCase) throws IOException;

    public abstract void writeMainMethodDefinition(TextOutputStream stream, String methodName) throws IOException;

    public abstract void writeActionMethodDefinition(TextOutputStream stream, String methodName, String mapName, String contextName) throws IOException;

    public void writeSetupContext(TextOutputStream stream, String contextName, AMLTestCase testCase, int loadedTestCases, int totalActions) throws IOException {
        stream.writeLine(2, "%s.setTestCaseName(\"%s\");", contextName, testCase.getBlockType().name() + '_' + testCase.getMatrixOrder());
        stream.writeLine(2, "%s.getScriptProgress().setLoaded(%s);", contextName, loadedTestCases);
        stream.writeLine(2, "%s.getScriptProgress().setCurrentActions(0);", contextName);
        stream.writeLine(2, "%s.getScriptProgress().setTotalActions(%s);", contextName, totalActions);
        stream.writeLine(2, "%s.getScriptProgress().setCurrentTC(%s);", contextName, testCase.getExecOrder());
        stream.writeLine(2, "%s.setScriptStartTime(System.currentTimeMillis());", contextName);

        if (testCase.isFailOnUnexpectedMessage()) {
            stream.writeLine(2, "%s.setTCStartCheckPoint(%s.%s(%1$s));", contextName, AMLPrivateActions.class.getSimpleName(), AMLPrivateActions.GET_CHECKPOINT_ACTION_NAME);
        }

        stream.writeLine();
    }

    public void writeOutcome(TextOutputStream stream, AMLAction action, String contextName, String reportName) throws IOException {
        stream.writeLine(2, "if(%s.isTestCaseCreated() && %s.getOutcomeStatus(\"%s\", \"%s\")==Status.FAILED) {", reportName, contextName, action.getOutcomeGroup(), action.getOutcomeName());

        String id = (action.getId() == null) ? "" : action.getId() + " ";
        String serviceName = action.hasServiceName() ? action.getServiceName() + " " : "";
        String messageType = (action.getMessageTypeColumn() == null) ? "" : " " + action.getMessageTypeColumn();
        String description = StringUtil.toJavaString(action.getDescrption());
        String name = id + serviceName + action.getActionURI() + messageType;
        String tag = StringUtil.toJavaString(action.getTag());
        String verificationsOrder = action.getVerificationsOrder().stream().map(StringUtil::enclose).collect(Collectors.joining(", "));

        if (action.getOutcome() != null) {
            description = action.getOutcome() + " " + description;
        }

        if(tag != null) {
            tag = StringUtil.enclose(tag, '"');
        }

        stream.writeLine(3, "%s.createAction(\"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"\", null, %s, %d, Arrays.asList(%s));",
                         reportName, name, serviceName, action.getActionURI(), messageType, description, tag,
                         action.getHash(), verificationsOrder);
        stream.writeLine(3, "%s.closeAction(new StatusDescription(StatusType.NA, \"Action skipped\"), null);", reportName);
        stream.writeLine();
        stream.writeLine(3, "return;");
        stream.writeLine(2, "}");
    }

    public void writeOutcomeFinished(TextOutputStream stream, AMLAction action, String contextName, boolean continueOnFailed) throws IOException {
        if (action.isLastOutcome()) {
            stream.writeLine(3, "%s.onOutcomeComplete(\"%s\", \"%s\");", contextName, action.getOutcomeGroup(), action.getOutcomeName());
        }

        if (action.isGroupFinished()) {
            if(continueOnFailed) {
                stream.writeLine();
                stream.writeLine(3, "try {");
            }

            int tabs = continueOnFailed ? 4 : 3;

            stream.writeLine(tabs, "%s.onGroupComplete(\"%s\");", contextName, action.getOutcomeGroup());
            stream.writeLine(tabs, "if(%s.getOutcomeGroupStatus(\"%s\") == Status.CONDITIONALLY_PASSED){", contextName, action.getOutcomeGroup());
            stream.writeLine(tabs + 1, "%s.setConditionallyPassed(true);", contextName);
            stream.writeLine(tabs, "}");
            stream.writeLine(tabs, "getReport().getReportStats().updateTestCaseStatus(Status.getStatusType(%s.getOutcomeGroupStatus(\"%s\")));", contextName, action.getOutcomeGroup());

            if(continueOnFailed) {
                stream.writeLine(3, "} catch(Exception e) {");
                stream.writeLine(4, "logger.warn(e);");
                stream.writeLine(4, "%s.setException(e);", contextName);
                stream.writeLine();
                stream.writeLine(4, "if(e instanceof InterruptedException) {");
                stream.writeLine(5, "throw e;");
                stream.writeLine(4, "}");
                stream.writeLine(3, "}");
                stream.writeLine();
            }
        }
    }

    public void writeInterruptedCheck(TextOutputStream stream, String contextName) throws IOException {
        stream.writeLine(2, "if(%s.isInterrupt()) {", contextName);
        stream.writeLine(3, "if(getReport().isTestCaseCreated()) {");
        stream.writeLine(4, "getReport().createAction(\"Test interrupted\", null, null, null, \"Test interrupted\", null, null, null, 0, Collections.emptyList());");
        stream.writeLine(3, "}");
        stream.writeLine();
        stream.writeLine(3, "throw new InterruptedException(\"Test interrupted\");");
        stream.writeLine(2, "}");
        stream.writeLine();
    }

    public void writeDependencyCheck(TextOutputStream stream, AMLAction action, String contextName, String reportName) throws IOException {
        String dependencies = action.getDependencies().stream().map(StringUtil::enclose).collect(Collectors.joining(", "));
        stream.writeLine(2, "if(!%s.checkExecutedActions(Arrays.asList(%s))) {", contextName, dependencies);

        String id = (action.getId() == null) ? "" : action.getId() + " ";
        String serviceName = action.hasServiceName() ? action.getServiceName() + " " : "";
        String messageType = (action.getMessageTypeColumn() == null) ? "" : " " + action.getMessageTypeColumn();
        String description = StringUtil.toJavaString(action.getDescrption());
        String name = id + serviceName + action.getActionURI() + messageType;
        String tag = StringUtil.toJavaString(action.getTag());
        String verificationsOrder = action.getVerificationsOrder().stream().map(StringUtil::enclose).collect(Collectors.joining(", "));

        if (action.getOutcome() != null) {
            description = action.getOutcome() + " " + description;
        }

        if(tag != null) {
            tag = StringUtil.enclose(tag, '"');
        }

        stream.writeLine(3, "%s.createAction(\"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"\", null, %s, Arrays.asList(%s));", reportName, name, serviceName, action.getActionURI(), messageType, description, tag, verificationsOrder);
        stream.writeLine(3, "%s.closeAction(new StatusDescription(StatusType.FAILED, \"Skipped due to failed dependencies\"), null);", reportName);
        stream.writeLine();
        stream.writeLine(3, "return;");
        stream.writeLine(2, "}");
    }

    public abstract void writeTryClause(TextOutputStream stream) throws IOException;

    public abstract void writeCatchClause(TextOutputStream stream, String loggerName, String contextName) throws IOException;

    public abstract void writeFinallyClause(TextOutputStream stream, String loggerName, String contextName) throws IOException;

    public abstract void writeExceptionCheck(TextOutputStream stream, String contextName) throws IOException;

    public String getServiceNamesArray(Set<String> serviceNames) {
        StringBuilder sb = new StringBuilder("new String[] {");
        Iterator<String> it = serviceNames.iterator();

        while (it.hasNext()) {
            String serviceName = it.next();

            if (serviceName != null) {
                sb.append("\"" + serviceName + "\"");

                if (it.hasNext()) {
                    sb.append(", ");
                }
            }
        }

        sb.append("}");

        return sb.toString();
    }

    public void writeTestClass(TextOutputStream stream, String className, List<SetterInfo> setters) throws IOException {
        writeImports(stream);

        stream.writeLine();
        stream.writeLine("public class %s {", className);
        stream.writeLine(1, "private final static Map<String, Object> messages = new HashMap<>();");
        stream.writeLine(1, "private final static %s context = new %1$s(null, null, null, null, null, 0);", ScriptContext.class.getSimpleName());
        stream.writeLine();

        int count = 0;

        for(SetterInfo setter : setters) {
            stream.writeLine(1, "private void setterTest%s() {", count++);
            stream.writeLine(2, "try {");
            stream.writeLine(3, "%s; // %s %s:%s:%s:%s:%s", setter.getCode(), CodeGenerator_new.ERROR_HOOK, setter.getLine(), setter.getUID(), setter.getReference(), setter.getColumn(), setter.getValue());
            stream.writeLine(2, "} catch(Throwable t) {}");
            stream.writeLine(1, "}");
            stream.writeLine();
        }

        stream.writeLine("}");
    }
}
