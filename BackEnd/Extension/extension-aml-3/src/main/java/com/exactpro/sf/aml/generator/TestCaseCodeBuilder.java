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

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import com.exactpro.sf.aml.AMLBlockType;
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
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.StringUtil;
import com.exactpro.sf.common.util.TextOutputStream;
import com.exactpro.sf.scriptrunner.DebugController;
import com.exactpro.sf.scriptrunner.SailFishAction;
import com.exactpro.sf.scriptrunner.SailFishTestCase;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.services.IService;
import com.exactpro.sf.services.ServiceStatus;

public class TestCaseCodeBuilder extends AbstractCodeBuilder {
    @Override
    public void writeMainClassDefinition(TextOutputStream stream, String className) throws IOException {
        writeClassDefinition(stream, className, SailFishTestCase.class.getSimpleName());
    }

    @Override
    public void writeActionClassDefinition(TextOutputStream stream, String className) throws IOException {
        writeClassDefinition(stream, className, SailFishAction.class.getSimpleName());
    }

    @Override
    public void writeMainMethodAnnotations(TextOutputStream stream, AMLTestCase testCase) throws IOException {
        String id = testCase.getId();
        String description = testCase.getDescription();
        String reference = StringUtils.defaultString(testCase.getReference());

        if (StringUtils.isNotBlank(id)) {
            stream.writeLine(1, "@%s(\"%s\")", Id.class.getSimpleName(), StringUtil.toJavaString(id));
        }

        if (StringUtils.isNotBlank(description)) {
            stream.writeLine(1, "@%s(\"%s\")", Description.class.getSimpleName(), StringUtil.toJavaString(description));
        }

        stream.writeLine(1, "@%s(order = %s, matrixOrder = %s)", ExecutionSequence.class.getSimpleName(), testCase.getExecOrder(), testCase.getMatrixOrder());
        stream.writeLine(1, "@%s(%s)", Hash.class.getSimpleName(), testCase.getHash());
        stream.writeLine(1, "@%s(%s.%s)", Type.class.getSimpleName(), AMLBlockType.class.getSimpleName(), testCase.getBlockType().name());
        stream.writeLine(1, "@%s(%s)", AddToReport.class.getSimpleName(), testCase.isAddToReport());
        stream.writeLine(1, "@%s(\"%s\")", Reference.class.getSimpleName(), StringUtil.toJavaString(reference));
    }

    @Override
    public void writeMainMethodDefinition(TextOutputStream stream, String methodName) throws IOException {
        stream.writeLine(1, "public void %s() throws Exception {", methodName);
    }

    @Override
    public void writeActionMethodDefinition(TextOutputStream stream, String methodName, String mapName, String contextName) throws IOException {
        stream.writeLine(1, "protected static void %s(Map<String, Object> %s, %s %s) throws Exception {", methodName, mapName, ScriptContext.class.getSimpleName(), contextName);
    }

    @Override
    public void writeTryClause(TextOutputStream stream) throws IOException {
        stream.writeLine(2, "try {");
    }

    @Override
    public void writeCatchClause(TextOutputStream stream, String loggerName, String contextName) throws IOException {
        stream.writeLine(2, "} catch(Throwable e) {");
        stream.writeLine(3, "%s.warn(e);", loggerName);
        stream.writeLine();
        stream.writeLine(3, "if(getReport().isActionCreated()) {");
        stream.writeLine(4, "getReport().closeAction(new StatusDescription(StatusType.FAILED, e.getMessage(), e), null);");
        stream.writeLine(3, "}");
        stream.writeLine();
        stream.writeLine(3, "%s.setInterrupt(e instanceof InterruptedException);", contextName);
        stream.writeLine();
        stream.writeLine(3, "throw e;");
    }

    @Override
    public void writeFinallyClause(TextOutputStream stream, String loggerName, String contextName) throws IOException {
        stream.writeLine(2, "} finally {");
        writeTimeLog(stream, loggerName, "finally", 3);
        stream.writeLine(2, "}");
        stream.writeLine();
    }

    @Override
    public void writeExceptionCheck(TextOutputStream stream, String contextName) throws IOException {
        stream.writeLine(2, "if(%s.getException() != null) {", contextName);
        stream.writeLine(3, "throw %s.getException();", contextName);
        stream.writeLine(2, "}");
    }

    public void writeBeforeMatrixPreparations(TextOutputStream stream, String serviceNamesArray, boolean autoStart) throws IOException {
        stream.writeLine();
        stream.writeLine(1, "@%s", BeforeMatrix.class.getSimpleName());
        stream.writeLine(1, "public static void beforeMatrixPreparations() throws Exception {");
        stream.writeLine(2, "SFLocalContext.getDefault().getEnvironmentManager().getConnectionManager().setServiceUsed(%s);", serviceNamesArray);
        if (autoStart) {
            stream.writeLine();
            stream.writeLine(2, "List<String> services = Arrays.<String>asList(%s);", serviceNamesArray);
            stream.writeLine(2, "startServices(services);");
        }
        stream.writeLine(1, "}");
    }

    public void writeAfterMatrixPreparations(TextOutputStream stream, String serviceNamesArray, boolean autoStart) throws IOException {
        stream.writeLine();
        stream.writeLine(1, "@%s", AfterMatrix.class.getSimpleName());
        stream.writeLine(1, "public static void afterMatrixPreparations() throws Exception {");
        stream.writeLine(2, "Exception exception = null;");
        if (autoStart) {
            writeTryClause(stream);
            stream.writeLine(3, "List<String> services = Arrays.<String>asList(%s);", serviceNamesArray);
            stream.writeLine(3, "disposeServices(services);");
            writeCatchException(stream);
            stream.writeLine();
        }
        writeTryClause(stream);
        stream.writeLine(3, "SFLocalContext.getDefault().getEnvironmentManager().getConnectionManager().setServiceNotUsed(%s);", serviceNamesArray);
        stream.writeLine(3, "SFLocalContext.getDefault().getActionManager().reset();");
        stream.writeLine(3, "SFLocalContext.getDefault().getUtilityManager().reset();");
        writeCatchException(stream);
        writeThrowsException(stream);
        stream.writeLine(1, "}");
    }

    public void writeBeforeTestCasePreparations(TextOutputStream stream, String serviceNamesArray, String loggerName, String contextName, boolean runNetDumper, boolean notificationIfServicesNotStarted) throws IOException {
        stream.writeLine();
        stream.writeLine(1, "@%s(%s.%s)", Type.class.getSimpleName(), AMLBlockType.class.getSimpleName(), AMLBlockType.BeforeTCBlock);
        stream.writeLine(1, "public void beforeTestCasePreparations() throws Exception {");
        if (runNetDumper) {
            stream.writeLine(2, "%s.getNetDumperService().startRecording(%s.getScriptDescriptionId(), %s);", contextName, contextName, serviceNamesArray);
            stream.writeLine();
        }
        if (notificationIfServicesNotStarted) {
            stream.writeLine(2, "List<String> notStartedServices = new ArrayList<String>();");
            stream.writeLine(2, "try {");
            stream.writeLine(3, "for(String serviceName : %s) {", serviceNamesArray);
            stream.writeLine(4,"%s servName = %s.parse(serviceName);", ServiceName.class.getCanonicalName(), ServiceName.class.getCanonicalName());
            stream.writeLine(4, "%s service = SFLocalContext.getDefault().getEnvironmentManager().getConnectionManager().getService(servName);", IService.class.getCanonicalName());
            stream.writeLine();
            stream.writeLine(4, "if(service != null && service.getStatus() != %s.STARTED) {", ServiceStatus.class.getCanonicalName());
            stream.writeLine(5, "notStartedServices.add(service.getName());");
            stream.writeLine(4, "}");
            stream.writeLine(3, "}");
            stream.writeLine(2, "} catch(Exception e) {");
            stream.writeLine(3, "%s.warn(e.getMessage());", loggerName);
            stream.writeLine(2, "}");
            stream.writeLine();
            stream.writeLine(2, "if(!notStartedServices.isEmpty()) {");
            stream.writeLine(3, "%s dmc = "+CodeGenerator_new.CONTEXT_NAME+".getDebugController();", DebugController.class.getCanonicalName());
            stream.writeLine();
            stream.writeLine(3, "dmc.pauseScript(0, \"The following services have not been started:\" + notStartedServices.toString());");
            stream.writeLine(3, "dmc.doWait();");
            stream.writeLine(2, "}");
        }
        stream.writeLine(1, "}");
    }

    public void writeAfterTestCasePreparations(TextOutputStream stream, String contextName, boolean stopNetDumper) throws IOException {
        stream.writeLine();
        stream.writeLine(1, "@%s(%s.%s)", Type.class.getSimpleName(), AMLBlockType.class.getSimpleName(), AMLBlockType.AfterTCBlock);
        stream.writeLine(1, "public void afterTestCasePreparations() throws Exception {");
        stream.writeLine(2, "Exception exception = null;");
        stream.writeLine(2, "%s.getScriptProgress().incrementExecutedTC();", contextName);
        stream.writeLine();
        if (stopNetDumper) {
            writeTryClause(stream);
            stream.writeLine(2, "File dir = %s.getWorkspaceDispatcher().createFolder(FolderType.REPORT, %s.getScriptConfig().getReportFolder(), \"Test_Case_\" + String.valueOf(%s.getScriptProgress().getCurrentTC()));".replaceAll("%s", contextName));
            stream.writeLine(2, "dir.mkdirs();");
            stream.writeLine(2, "%s.getNetDumperService().stopAndStore(%s.getScriptDescriptionId(), dir);", contextName, contextName);
            writeCatchException(stream);
            writeThrowsException(stream);
        }
        stream.writeLine(1, "}");
    }

    private void writeCatchException(TextOutputStream stream) throws IOException {
        stream.writeLine(2, "} catch(Exception ex) {");
        stream.writeLine(3, "exception = %s.collectExceptions(exception, ex);", SailFishTestCase.class.getCanonicalName());
        stream.writeLine(2, "}");
    }

    private void writeThrowsException(TextOutputStream stream) throws IOException {
        stream.writeLine(2, "if (exception != null) {");
        stream.writeLine(3, "throw exception;");
        stream.writeLine(2, "}");
    }
}
