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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.mina.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.AML;
import com.exactpro.sf.aml.AMLAction;
import com.exactpro.sf.aml.AMLBlockType;
import com.exactpro.sf.aml.AMLException;
import com.exactpro.sf.aml.AMLLangConst;
import com.exactpro.sf.aml.AMLLangUtil;
import com.exactpro.sf.aml.AMLList;
import com.exactpro.sf.aml.AMLPrivateActions;
import com.exactpro.sf.aml.AMLSettings;
import com.exactpro.sf.aml.AMLTestCase;
import com.exactpro.sf.aml.CustomColumn;
import com.exactpro.sf.aml.Direction;
import com.exactpro.sf.aml.ICodeGenerator;
import com.exactpro.sf.aml.MessageDirection;
import com.exactpro.sf.aml.generator.matrix.BoolExp;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.generator.matrix.JavaStatement;
import com.exactpro.sf.aml.generator.matrix.RefParameter;
import com.exactpro.sf.aml.generator.matrix.TypeHelper;
import com.exactpro.sf.aml.generator.matrix.Value;
import com.exactpro.sf.aml.generator.matrix.Variable;
import com.exactpro.sf.aml.script.CheckPoint;
import com.exactpro.sf.aml.script.DefaultSettings;
import com.exactpro.sf.aml.script.MetaContainer;
import com.exactpro.sf.aml.scriptutil.MessageCount;
import com.exactpro.sf.common.adapting.IAdapterManager;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.Pair;
import com.exactpro.sf.common.util.StringUtil;
import com.exactpro.sf.common.util.TextOutputStream;
import com.exactpro.sf.comparison.conversion.MultiConverter;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.IEnvironmentManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;
import com.exactpro.sf.configuration.workspace.WorkspaceStructureException;
import com.exactpro.sf.scriptrunner.IProgressListener;
import com.exactpro.sf.scriptrunner.IScriptReport;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.scriptrunner.actionmanager.ActionInfo;
import com.exactpro.sf.scriptrunner.actionmanager.IActionManager;
import com.exactpro.sf.scriptrunner.services.IStaticServiceManager;
import com.exactpro.sf.scriptrunner.utilitymanager.IUtilityManager;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityInfo;
import com.exactpro.sf.services.IService;
import com.exactpro.sf.services.IServiceSettings;
import com.google.common.collect.Iterables;


/**
 * Action Matrix Language code generator.
 *
 * @author dmitry.guriev
 *
 */
public class CodeGenerator_new implements ICodeGenerator {

	private static final Logger logger = LoggerFactory.getLogger(CodeGenerator_new.class);

	protected final static String EOL = System.getProperty("line.separator");

	private final Set<String> imports = new HashSet<>();
	public static final String MAP_NAME = "messages";
	public static final String CONTEXT_NAME = "context";
	public static final String REPORT_NAME = "report";
	public static final String STATIC_MAP_NAME = CONTEXT_NAME+".getStaticMap()";
	public static final String SERVICE_MAP_NAME = CONTEXT_NAME+".getServiceNames()";
    public static final String ACTION_MANAGER_CALL = CONTEXT_NAME + ".getActionManager().call";
    public static final String UTILITY_MANAGER = CONTEXT_NAME + ".getUtilityManager()";
    public static final String UTILITY_MANAGER_VARIABLE = "um";
    public static final String UTILITY_MANAGER_CALL =  UTILITY_MANAGER_VARIABLE + ".call";
	public static final String LOGGER_NAME = "logger";

	private final AlertCollector alertCollector;
	private Set<String> definedReferences; // references defined in the current test case
	private Map<String, String> definedServiceNames = new HashMap<>();
	private final Set<String> resolvedServiceNames = new HashSet<>();

	private int cycleCount = 0;

	public static final String TAB1 = "\t";
	public static final String TAB2 = "\t\t";
	public static final String TAB3 = "\t\t\t";
	public static final String TAB4 = "\t\t\t\t";
	public static final String TAB5 = "\t\t\t\t\t";

    public static final String ERROR_HOOK = "line:uid:reference:column:value";
    public static final int MAX_SETTERS_PER_CLASS = 10000;

	/**
	 * Value starts with this string should interpreted as java code
	 * and will be used without transformation.
	 * Before insertion it should be checked via compilation.
	 */
	private static final String TAG_INTERPRET_AS_JAVA = "java:";

	private static final String BEGIN_REFERENCE = "${";
	private static final String END_REFERENCE = "}";

	private static final String BEGIN_STATIC = "%{";
	private static final String END_STATIC = "}";

	private static final String BEGIN_FUNCTION = "#{";
	private static final String END_FUNCTION = "}";

	private boolean autoStart;

	private AMLSettings amlSettings;

	private File dir;

	private List<IProgressListener> progressListeners;

	private int loadedTestCases;
	private int totalActions;

	OldImpl impl;
	NewImpl newImpl;

	private ScriptContext scriptContext;

	private final static int CAPACITY_4K = 4096;
    private final static int CAPACITY_128K = 131072;

    private final TestCaseCodeBuilder tcCodeBuilder;

    private IWorkspaceDispatcher workspaceDispatcher;
    private IAdapterManager adapterManager;
    private IEnvironmentManager environmentManager;
    private IDictionaryManager dictionaryManager;
    private IStaticServiceManager staticServiceManager;
    private IActionManager actionManager;
    private IUtilityManager utilityManager;

    private String compilerClassPath;

	public CodeGenerator_new() {
		this.alertCollector = new AlertCollector();
        this.tcCodeBuilder = new TestCaseCodeBuilder();
	}

	@Override
	public void init(IWorkspaceDispatcher workspaceDispatcher,
	                 IAdapterManager adapterManager,
	                 IEnvironmentManager environmentManager,
	                 IDictionaryManager dictionaryManager,
	                 IStaticServiceManager staticServiceManager,
	                 IActionManager actionManager,
	                 IUtilityManager utilityManager,
	                 ScriptContext scriptContext,
	                 AMLSettings amlSettings,
	                 List<IProgressListener> progressListeners,
	                 String compilerClassPath) throws AMLException {
	    this.workspaceDispatcher = workspaceDispatcher;
	    this.adapterManager = adapterManager;
	    this.environmentManager = environmentManager;
	    this.dictionaryManager = dictionaryManager;
	    this.staticServiceManager = staticServiceManager;
	    this.actionManager = actionManager;
	    this.utilityManager = utilityManager;
	    this.scriptContext = scriptContext;
	    this.progressListeners = progressListeners;
	    this.compilerClassPath = compilerClassPath;

	    impl = new OldImpl(alertCollector, adapterManager, dictionaryManager, actionManager, utilityManager, this);
        newImpl = new NewImpl(alertCollector, adapterManager, this.environmentManager.getConnectionManager(), dictionaryManager, actionManager, utilityManager, this.staticServiceManager, this);

        setAutoStart(amlSettings.getAutoStart());
        this.amlSettings = amlSettings;
        this.impl.setContinueOnFailed(amlSettings.getContinueOnFailed());
        this.newImpl.setContinueOnFailed(amlSettings.getContinueOnFailed());
	}

	protected void setAutoStart(boolean b) {
		this.autoStart = b;
	}

	protected boolean getAutoStart() {
		return this.autoStart;
	}

    @Override
    public ScriptContext getScriptContext() {
        return this.scriptContext;
    }

    /* (non-Javadoc)
     * @see com.exactpro.sf.aml.generator.ICodeGenerator#generateCode(java.util.List)
	 */
	@Override
	public GeneratedScript generateCode(List<AMLTestCase> testCases, List<AMLTestCase> beforeTCBlocks, List<AMLTestCase> afterTCBlocks) throws AMLException, IOException, InterruptedException
	{
	    logger.info("GenerateCode");

        this.dir = createOutputDirectory();
        this.loadedTestCases = (int)testCases.stream().filter(AMLTestCase::isAddToReport).count();

        File file = new File(dir, AML.CLASS_NAME+".java");
        GeneratedScript script = new GeneratedScript();

        try (TextOutputStream mainClass = new TextOutputStream(new FileOutputStream(file))) {
            tcCodeBuilder.writeJavaHeader(mainClass);
            tcCodeBuilder.writeMainClassDefinition(mainClass, AML.CLASS_NAME);

            writeJavaClass(testCases, beforeTCBlocks, afterTCBlocks, mainClass, script);
        }

        testCode(testCases);

        script.setMainFile(file);

        logger.info("File saved to: {}", file.getCanonicalPath());

        return script;
	}

	private File createOutputDirectory() throws AMLException, WorkspaceStructureException {
        String srcDir = this.amlSettings.getSrcDir();

        if (srcDir == null) {
            srcDir = "";
        }

        return workspaceDispatcher.createFolder(FolderType.REPORT, this.amlSettings.getBaseDir(), srcDir, AML.PACKAGE_PATH);
    }

	private void writeJavaClass(List<AMLTestCase> testCases, List<AMLTestCase> beforeTCBlocks, List<AMLTestCase> afterTCBlocks, TextOutputStream mainClass, GeneratedScript script) throws AMLException, IOException, InterruptedException
	{
		logger.info("WriteJavaClass");

		long time = System.currentTimeMillis();

	    tcCodeBuilder.writeLogger(mainClass, LOGGER_NAME, false);

        writeTestCases(testCases, mainClass, script);
        writeTestCases(afterTCBlocks, mainClass, script);
        writeTestCases(beforeTCBlocks, mainClass, script);

	    scriptContext.getServiceList().addAll(resolvedServiceNames);
		final String servNames = tcCodeBuilder.getServiceNamesArray(resolvedServiceNames);

        tcCodeBuilder.writeBeforeMatrixPreparations(mainClass, servNames, getAutoStart());
        tcCodeBuilder.writeBeforeTestCasePreparations(mainClass, servNames, LOGGER_NAME, CONTEXT_NAME, amlSettings.isRunNetDumper(),
                environmentManager.getEnvironmentSettings().isNotificationIfServicesNotStarted());

        tcCodeBuilder.writeAfterTestCasePreparations(mainClass, CONTEXT_NAME, amlSettings.isRunNetDumper());
        tcCodeBuilder.writeAfterMatrixPreparations(mainClass, servNames, getAutoStart());


		mainClass.writeLine("}");

        time = System.currentTimeMillis() - time;
        logger.debug("Time to build Java source code: " + time);
	}

    private void writeTestCases(List<AMLTestCase> testCases, TextOutputStream mainClass, GeneratedScript script) throws IOException, AMLException, InterruptedException {
	    TextOutputStream actionClass = null;

	    for(int i = 0; i < testCases.size(); i++) {
	        try {
                AMLTestCase tc = testCases.get(i);

                if(tc.getActions().size() == 0) {
                    continue;
                }

                AMLBlockType type = tc.getBlockType();

                if(type == AMLBlockType.TestCase) {
                    progressChanged(30 + 30 * (i / testCases.size()));
                }

                String actionClassName = String.format("%s_Actions_%s", type.name(), i + 1);
                File file = new File(this.dir, actionClassName + ".java");
                actionClass = new TextOutputStream(new FileOutputStream(file));

                script.addFile(file);

                tcCodeBuilder.writeMainMethodAnnotations(mainClass, tc);
                tcCodeBuilder.writeMainMethodDefinition(mainClass, type.name() + '_' + tc.getMatrixOrder());

                if(type == AMLBlockType.TestCase) {
                    tcCodeBuilder.writeSetupContext(mainClass, CONTEXT_NAME, tc, loadedTestCases, totalActions);
                }

                tcCodeBuilder.writeInterruptedCheck(mainClass, CONTEXT_NAME);
                tcCodeBuilder.writeTimeLog(mainClass, LOGGER_NAME, "start", 2);
                mainClass.writeLine();
                tcCodeBuilder.writeTryClause(mainClass);
                tcCodeBuilder.writeMessagesMapDefinition(mainClass, MAP_NAME);

                writeTestCase(tc, mainClass, actionClass, actionClassName);

                mainClass.writeLine();
                tcCodeBuilder.writeTimeLog(mainClass, LOGGER_NAME, "end", 3);
                tcCodeBuilder.writeCatchClause(mainClass, LOGGER_NAME, CONTEXT_NAME);
                tcCodeBuilder.writeFinallyClause(mainClass, LOGGER_NAME, CONTEXT_NAME);
                tcCodeBuilder.writeExceptionCheck(mainClass, CONTEXT_NAME);

                mainClass.writeLine(1, "}");
                mainClass.writeLine();
	        } finally {
	            if(actionClass != null) {
	                actionClass.close();
	            }
	        }
        }
	}

	private void writeTestCase(AMLTestCase tc, TextOutputStream mainClass, TextOutputStream actionClass, String actionClassName) throws AMLException, IOException, InterruptedException
	{
		definedReferences = new HashSet<>();

        logger.info("writeTestCase: {}", tc);

		cycleCount = 0;

		tcCodeBuilder.writeJavaHeader(actionClass);
        tcCodeBuilder.writeActionClassDefinition(actionClass, actionClassName);
        tcCodeBuilder.writeLogger(actionClass, LOGGER_NAME, true);

		int actionNumber = 0;

        for (AMLAction action : tc.getActions()) {
            Thread.sleep(0);

            if(action.hasActionURI()) {
                JavaStatement statement = JavaStatement.value(action.getActionURI());

			    if(statement != null) {
			        String line = null;

			        switch(statement) {
			        case BEGIN_LOOP:
			        case END_LOOP:
			        case BEGIN_IF:
			        case BEGIN_ELIF:
			        case BEGIN_ELSE:
			        case END_IF:
			            line = writeJavaStatement(tc, action, statement);
			            break;
			        case SET_STATIC:
			            line = writeStaticDefinition(tc, action);
			            break;
			        case DEFINE_SERVICE_NAME:
			            line = writeServiceNameDefinition(action);
			            break;
                    default:
                        this.alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), "Java statement not implemented: " + statement));
                        break;
			        }

			        if(line != null) {
			            mainClass.writeLine(line);
			        }
			    } else {
			        mainClass.writeLine(3, "this.checkInterrupted();");

				    this.totalActions++;
				    List<Variable> variables = new ArrayList<>();

				    if (action.getActionInfo() == null) {
					    throw new AMLException("action.getActionMethod() == null");
				    }

				    String methodBody = "";
				    MessageDirection annotation = action.getActionInfo().getAnnotation(MessageDirection.class);

				    if(annotation == null) {
				        methodBody = impl.writeFillMessage(tc, action, variables);
				    } else if(annotation.direction() == Direction.SEND || annotation.direction() == Direction.SENDDIRTY) {
				        methodBody = newImpl.writeSendMessage(tc, action, variables, annotation.direction() == Direction.SENDDIRTY);
				    } else if(annotation.direction() == Direction.RECEIVE) {
				        methodBody = newImpl.writeReceiveMessage(tc, action, variables);
				    }

                    if (methodBody == null) {
                        if(!action.isStaticAction()) {
                            this.alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), "Action already generated [generation path: " + action.getGenerationPath() + "] or another error occured..."));
                        }

                        continue;
                    }

				    if (methodBody.length() != 0) {
		                String methodName = String.format("Action_%s_%s", ++actionNumber, action.getActionURI().getResourceName());

				        mainClass.writeLine(3, "%s.%s(%s, %s);", actionClassName, methodName, MAP_NAME, CONTEXT_NAME);

                        tcCodeBuilder.writeActionMethodDefinition(actionClass, methodName, MAP_NAME, CONTEXT_NAME);

                        actionClass.writeLine(2, "%s %s = %s.getReport();", IScriptReport.class.getSimpleName(), REPORT_NAME, CONTEXT_NAME);
                        actionClass.writeLine(2, "%s.getDebugController().doWait(\"%s\");", CONTEXT_NAME, StringUtil.toJavaString(action.getDescrption()));

                        if (action.getOutcomeGroup() != null) {
                            tcCodeBuilder.writeOutcome(actionClass, action, CONTEXT_NAME, REPORT_NAME);
                        }

                        if(!action.getDependencies().isEmpty()) {
                            tcCodeBuilder.writeDependencyCheck(actionClass, action, CONTEXT_NAME, REPORT_NAME);
                        }

                        for (Variable var : variables) {
                            logger.debug("Print variable {}", var);
                            actionClass.writeLine(2, "%s %s;", var.getType().getCanonicalName(), var.getName());
                        }

                        actionClass.writeLine();
                        actionClass.writeLine(methodBody);
                        actionClass.writeLine(1, "}");
                        actionClass.writeLine();

                        if (action.getReference() != null) {
                            definedReferences.add(action.getReference());
                        }

                        if (action.getReferenceToFilter() != null) {
                            definedReferences.add(action.getReferenceToFilter());
                        }

                        tcCodeBuilder.writeOutcomeFinished(mainClass, action, CONTEXT_NAME, action.getContinueOnFailed() || amlSettings.getContinueOnFailed());
				    }

				    variables.clear();
			    }
		    }
		}

        if (tc.isFailOnUnexpectedMessage()) {
            mainClass.writeLine(3, "%s.%s(%s);", AMLPrivateActions.class.getSimpleName(), AMLPrivateActions.CHECK_UNEXPECTED_ACTION_NAME, CONTEXT_NAME);
        }

		actionClass.writeLine("}");
        actionClass.close();

        if (this.alertCollector.getCount(AlertType.ERROR) != 0) {
            String s = StringUtil.getSSuffix(this.alertCollector.getCount(AlertType.ERROR));

            if (logger.isErrorEnabled()) {
                for (Alert alert : this.alertCollector.getAlerts(AlertType.ERROR)) {
                    logger.error(alert.toString());
                }
            }

            throw new AMLException("Error" + s + " in matrix", this.alertCollector);
        }
	}

	private String writeStaticDefinition(AMLTestCase tc, AMLAction action) {

		if (action.getGenerateStatus() == AMLGenerateStatus.GENERATED) {
			return null;
		}

		if (action.getGenerateStatus() == AMLGenerateStatus.GENERATING) {
			this.alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), "Recursion detected"));
			return null;
		}

		action.setGenerateStatus(AMLGenerateStatus.GENERATING);

		try {
		    NewImplHelper.substituteReference(tc, action, alertCollector, Column.StaticValue.getName(), action.getStaticValue(), definedReferences, dictionaryManager, actionManager, utilityManager);
		} catch(SailfishURIException e) {
            alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), Column.StaticValue.getName(), e.getMessage()));
            return null;
        }

		Value value = action.getStaticValue();
		String newValue = value.getValue();

		if (!value.isReference()) {
			try {
				newValue = TypeHelper.convertValue(action.getStaticType(), newValue);
			} catch (AMLException e) {
				this.alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), Column.StaticType.getName(),  e.getMessage()));
				return null;
			}
		} else {
			newValue = NewImpl.generateEval(action.getLine(), Column.StaticValue.getName(), value, TAB3);
		}

		String ref = action.getReference();
		action.setGenerateStatus(AMLGenerateStatus.GENERATED);

		return TAB3+STATIC_MAP_NAME+".put(\""+ref+"\", "+newValue+");"+EOL;
	}

	private String writeServiceNameDefinition(AMLAction action) {
        String serviceName = action.getServiceName();
        String reference = action.getReference();

        if(getService(serviceName, action.getLine(), action.getUID(), Column.ServiceName.getName()) == null) {
            return null;
        }

        definedServiceNames.put(reference, serviceName);

        return TAB3 + SERVICE_MAP_NAME + ".put(\"" + reference + "\", \"" + serviceName + "\");" + EOL;
	}

	private String writeJavaStatement(AMLTestCase tc, AMLAction action, JavaStatement word)
	{
		switch (word)
		{
		case BEGIN_LOOP:
			String in = "i"+(cycleCount++);
            String column = Column.MessageCount.getName();
            Value count = new Value(action.getMessageCount());

            try {
                NewImplHelper.substituteReference(tc, action, alertCollector, column, count, definedReferences, dictionaryManager, actionManager, utilityManager);
                StringBuilder builder = new StringBuilder();

                builder.append(TAB3);
                builder.append("for (int ");
                builder.append(in);
                builder.append(" = 0; ");
                builder.append(in);
                builder.append(" < ");
                builder.append(MultiConverter.class.getSimpleName());
                builder.append(".convert(");
                builder.append(NewImpl.generateEval(action.getLine(), column, count, TAB4));
                builder.append(", ");
                builder.append(Integer.class.getCanonicalName());
                builder.append(".class); ");
                builder.append(in);
                builder.append("++) {");

                return builder.toString();
            } catch(SailfishURIException e) {
                alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, e.getMessage()));
                return TAB3 + "for (int " + in + " = 0; " + in + " < -1; " + in + "++) {" + EOL;
            }
		case END_LOOP:
			return TAB3+"}"+EOL;
		case BEGIN_IF:
		    return EOL+TAB3+"if("+writeCondition(tc, action)+") {";
		case BEGIN_ELIF:
		    return TAB3+"} else if("+writeCondition(tc, action)+") {";
		case BEGIN_ELSE:
		    return TAB3+"} else {";
		case END_IF:
		    return TAB3+"}"+EOL;
		default:
			this.alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), "Java statement not implemented: "+word));
			return null;

		}
	}

	private String writeCondition(AMLTestCase tc, AMLAction action) {
        try {
            NewImplHelper.substituteReference(tc, action, alertCollector, Column.Condition.getName(), action.getCondition(), definedReferences, dictionaryManager, actionManager, utilityManager);
        } catch(SailfishURIException e) {
            alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), Column.Condition.getName(), e.getMessage()));
            return null;
        }

	    StringBuilder sb = new StringBuilder(CAPACITY_4K);

	    sb.append("Boolean.TRUE.equals(");
        sb.append(NewImpl.generateEval(action.getLine(), Column.Condition.getName(), action.getCondition(), TAB4));
        sb.append(")");

        return sb.toString();
	}

	public String createFillSettings(AMLTestCase tc, AMLAction action, String message, Variable settings, AlertCollector alertCollector)
	{
		String varName = settings.getName();
		StringBuilder sb = new StringBuilder(CAPACITY_128K);

		sb.append(EOL);
		sb.append(TAB2+varName+" = new "+settings.getType().getCanonicalName()+"(" + CONTEXT_NAME + ", " + (action.getOutcome() == null) + ");"+EOL);

		if (action.getTimeout() != null)
		{
            Value timeout = action.getTimeout();

            try {
                NewImplHelper.substituteReference(tc, action, alertCollector, Column.Timeout.getName(), timeout, definedReferences, dictionaryManager, actionManager, utilityManager);

                getMethod(alertCollector, DefaultSettings.class, "setTimeout", action, Column.Timeout.getName(), long.class);

                String timeoutStr = timeout.isReference() ? "(long)" + NewImpl.generateEval(action.getLine(), Column.Timeout.getName(), timeout, TAB3) : timeout.getValue();
                sb.append(TAB2 + varName + ".setTimeout(" + timeoutStr + ");" + EOL);
            } catch(SailfishURIException e) {
                alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), Column.Timeout.getName(), e.getMessage()));
            }
		}
		if (action.getServiceName() != null)
		{
			getMethod(alertCollector, DefaultSettings.class, "setServiceName", action, Column.ServiceName.getName(), String.class);
			sb.append(TAB2+varName+".setServiceName(\""+ServiceName.toString(scriptContext.getEnvironmentName(), action.getServiceName())+"\");"+EOL);
		}
		if (action.hasReference())
		{
			getMethod(alertCollector, DefaultSettings.class, "setReference", action, Column.Reference.getName(), String.class);
			sb.append(TAB2+varName+".setReference(\""+action.getReference()+"\");"+EOL);
		}
		if (action.hasReferenceToFilter())
		{
			getMethod(alertCollector, DefaultSettings.class, "setReferenceToFilter", action, Column.ReferenceToFilter.getName(), String.class);
			sb.append(TAB2+varName+".setReferenceToFilter(\""+action.getReferenceToFilter()+"\");"+EOL);
		}
		if (action.hasId())
		{
			getMethod(alertCollector, DefaultSettings.class, "setId", action, Column.Id.getName(), String.class);
			sb.append(TAB2+varName+".setId(\""+action.getId()+"\");"+EOL);
		}
		getMethod(alertCollector, DefaultSettings.class, "setLine", action, null, long.class);
        sb.append(TAB2+varName+".setLine("+action.getLine()+");"+EOL);
		if (action.hasDictionaryURI())
		{
			getMethod(alertCollector, DefaultSettings.class, "setDictionaryURI", action, Column.Dictionary.getName(), SailfishURI.class);
			sb.append(TAB2+varName+".setDictionaryURI(SailfishURI.parse(\""+action.getDictionaryURI()+"\"));"+EOL);
            sb.append(TAB2+varName+".setUncheckedFields("+ CONTEXT_NAME + ".getDictionaryManager().getMessageFactory(" + SailfishURI.class.getSimpleName()  + ".parse(\"" + action.getDictionaryURI() + "\")).getUncheckedFields());"+EOL);
        }
		if (action.getMessageCount() != null)
		{
			getMethod(alertCollector, DefaultSettings.class, "setMessageCount", action, Column.MessageCount.getName(), MessageCount.class);

			String count = action.getMessageCount();
			Value countValue = new Value(count);

			boolean isAML3Action = action.getActionInfo().isLanguageCompatible(AMLLangConst.AML3, true);

			try {
			    if (isAML3Action) {
				    NewImplHelper.substituteReference(tc, action, alertCollector, Column.MessageCount.getName(), countValue, definedReferences, dictionaryManager, actionManager, utilityManager);
			    } else {
				    OldImplHelper.substituteReference(tc, action, alertCollector, Column.MessageCount.getName(), countValue, definedReferences, dictionaryManager, actionManager, utilityManager);
			    }

                if(!count.startsWith(AMLLangConst.BEGIN_FUNCTION)) {
                    String expression = countValue.getValue().replace(" ", "").replaceAll("\\.\\.", "+'$0'+");

                    boolean isRange = expression.matches("^[\\(\\[].*") && expression.matches(".*[\\)\\]]$");
                    boolean isCondition = expression.matches("^(>=|<=|>|<|=|!=).*");

                    if(isRange) {
                        expression = expression.replaceAll("^[\\(\\[]", "'$0'+").replaceAll("[\\)\\]]$", "+'$0'");
                    }

                    if(isCondition) {
                        expression = expression.replaceAll("^(>=|<=|>|<|=|!=)", "'$0'+");
                    }

                    countValue.setValue(expression);
                }

			    sb.append(TAB2);
	        	sb.append(varName);
                sb.append(".setMessageCountFilter(");
                sb.append(NewImpl.generateFilter(action.getLine(), Column.MessageCount.getName(), countValue, TAB3, "countFilter"));
                sb.append(");" + EOL);
			} catch(SailfishURIException e) {
                alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), Column.MessageCount.getName(), e.getMessage()));
			}
		}

		getMethod(alertCollector, DefaultSettings.class, "setMetaContainer", action, null, MetaContainer.class);
		sb.append(TAB2+varName+".setMetaContainer(metaContainer);"+EOL);

		if (false == action.getFailUnexpected().equals(""))
		{
			getMethod(alertCollector, DefaultSettings.class, "setFailUnexpected", action, Column.FailUnexpected.getName(), String.class);
			String s = action.getFailUnexpected();
			if (s.equalsIgnoreCase("Y") || s.equalsIgnoreCase("A"))
				sb.append(TAB2+varName+".setFailUnexpected(\""+s+"\");"+EOL);
		} else {
			getMethod(alertCollector, DefaultSettings.class, "setFailUnexpected", action, Column.FailUnexpected.getName(), String.class);
			String s = environmentManager.getEnvironmentSettings().getFailUnexpected();
			if (s.equalsIgnoreCase("Y") || s.equalsIgnoreCase("A"))
				sb.append(TAB2+varName+".setFailUnexpected(\""+s+"\");"+EOL);
		}

		if (false == action.getDescrption().equals(""))
		{
            getMethod(alertCollector, DefaultSettings.class, "setDescription", action, Column.Description.getName(), String.class);

            sb.append(TAB2+varName+".setDescription("+NewImpl.getMvelString(tc, action, action.getDescrption(), Column.Description, alertCollector, definedReferences, dictionaryManager, actionManager, utilityManager)+");"+EOL);
		}

		if (action.getCheckPoint() != null)
		{
		    String oldCp = action.getCheckPoint();
			getMethod(alertCollector, DefaultSettings.class, "setCheckPoint", action, Column.CheckPoint.getName(), CheckPoint.class);

			logger.debug("find reference for checkpoint: {}", oldCp);
			String newCp = NewImpl.getMvelString(tc, action, oldCp, Column.CheckPoint, alertCollector, definedReferences, dictionaryManager, actionManager, utilityManager);

			if(newCp.substring(1, newCp.length() - 1).equals(oldCp)) {
                for(AMLAction a : tc.getActions()) {
                    if(a == action) {
                        alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(),
                                "Checkpoint referred to undefined checkpoint '" + oldCp + "'. Please check that '" + oldCp
                                        + "' checkpoint was defined earlier."));
                        return null;
                    }

                    if(oldCp.equals(a.getReference())) {
                        // checkpoint found
                        logger.debug("reference found");
                        break;
                    }
                }
			} else {
			    alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), Column.CheckPoint.getName(), "Unable to check checkpoint at compile time", AlertType.WARNING));
			}

			sb.append(TAB2+varName+".setCheckPoint(("+CheckPoint.class.getSimpleName()+")"+MAP_NAME+".get("+newCp+"));"+EOL);
		}

		getMethod(alertCollector, DefaultSettings.class, "setCheckGroupsOrder", action, null, boolean.class);
        sb.append(TAB2+varName+".setCheckGroupsOrder("+action.isCheckGroupsOrder()+");"+EOL);

		getMethod(alertCollector, DefaultSettings.class, "setAddToReport", action, null, boolean.class);
		sb.append(TAB2+varName+".setAddToReport("+action.isAddToReport()+");"+EOL);

		getMethod(alertCollector, DefaultSettings.class, "setMessages", action, null, Map.class);
		sb.append(TAB2+varName+".setMessages(messages);"+EOL);

		getMethod(alertCollector, DefaultSettings.class, "setReorderGroups", action, null, boolean.class);
		sb.append(TAB2+varName+".setReorderGroups("+action.getReorderGroups()+");"+EOL);

		sb.append(EOL);

		return sb.toString();
	}

    protected void putSystemColumns(StringBuilder sb, String varName, AMLTestCase tc, AMLAction action, ActionInfo actionInfo, AlertCollector alertCollector) {
        for(Entry<String, Value> entry : action.getServiceFields().entrySet()) {
            String columnName = entry.getKey();

            if(Column.value(columnName) != null) {
                continue;
            }

            CustomColumn column = actionInfo.getCustomColumn(entry.getKey());

            if(column == null) {
                continue;
            }

            Value columnValue = entry.getValue();

            try {
                NewImplHelper.substituteReference(tc, action, alertCollector, columnName, columnValue, definedReferences, dictionaryManager, actionManager, utilityManager);
            } catch(SailfishURIException e) {
                alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), columnName, e.getMessage()));
                continue;
            }

            sb.append(TAB2);
            sb.append(varName);
            sb.append(".putSystemColumn(\"");
            sb.append(columnName);
            sb.append("\", ");

            if(columnValue.isReference()) {
                sb.append(MultiConverter.class.getSimpleName());
                sb.append(".convert(");
                sb.append(NewImpl.generateEval(action.getLine(), columnName, columnValue, TAB3));
                sb.append(", ");
                sb.append(column.type().getCanonicalName());
                sb.append(".class)");
            } else {
                sb.append(TypeConverter.convert(column.type(), columnValue.getValue()));
            }

            sb.append(");");
            sb.append(EOL);
        }
    }

	protected static Method getMethod(AlertCollector alertCollector,
			Class<?> clazz,
			String methodName,
			AMLAction action,
			String column,
			Class<?>... parameterTypes)
	{
		if (clazz == null)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Class cannot be null."));
			return null;
		}

		if (methodName == null)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Setting method name cannot be null."));
			return null;
		}

		if ("".equals(methodName))
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Setting method name cannot be empty."));
			return null;
		}

		try {
			return clazz.getDeclaredMethod(methodName, parameterTypes);
		} catch (SecurityException e) {
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Cannot access to setting method '"+methodName+"' in class '"+clazz.getCanonicalName()+"'"));
		} catch (NoSuchMethodException e) {

			if (clazz.getSuperclass() != null)
			{
				return getMethod(alertCollector, clazz.getSuperclass(), methodName, action, column, parameterTypes);
			}
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Cannot find setting method '"+methodName+"' in class '"+clazz.getCanonicalName()+"'"));
		}

		StringBuilder sb = new StringBuilder(CAPACITY_4K);
		for (Class<?> pclazz : parameterTypes)
		{
			sb.append(pclazz.getCanonicalName())
			.append(", ");
		}
		if (sb.length() > 2)
		{
			sb.delete(sb.length()-2, sb.length());
		}
		alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Cannot find setting method '"+methodName+"("+sb.toString()+")' for column "+column+" in class '"+clazz.getCanonicalName()+"'"));
		return null;
	}

	static Method getMethod(AMLList<String> errors, Class<?> clazz, String methodName, AMLAction action, String column)
	{
		String ref = action.getReference() == null ? "" : action.getReference();
		if (clazz == null) {
			errors.add("Error in line "+action.getLine()+" ["+ref+"]: Class cannot be null.");
			return null;
		}

		if (methodName == null) {
			errors.add("Error in line "+action.getLine()+" ["+ref+"]: Setting method name cannot be null.");
			return null;
		}

		if ("".equals(methodName))
		{
			errors.add("Error in line "+action.getLine()+" ["+ref+"]: Setting method name cannot be empty.");
			return null;
		}

		Class<?> tempClass = clazz;
		while (tempClass != null)
		{
			Method[] methods = tempClass.getDeclaredMethods();
			for (Method method : methods)
			{
				if (method.getName().equals(methodName))
					return method;
			}
			tempClass = tempClass.getSuperclass();
		}
		errors.add("Error in line "+action.getLine()+" ["+ref+"]: can not find setting method '"+methodName+"' for column "+column+" in class '"+clazz.getCanonicalName()+"'");
		return null;
	}

	public void substituteReference(AMLTestCase tc, AMLAction action, AlertCollector alertCollector, String column, Value value)
	{
		logger.debug("substituteReference: column: {}; Value: {}", column ,value);
		if (column == null)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Key is null"));
			return;
		}

		if (value.getValue() == null)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Value is null for parameter '"+column+"'"));
			return;
		}

		if ("".equals(value.getValue()))
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Empty value found for parameter '"+column+"'"));
			return;
		}

		// value is java code

		if (value.getValue().toLowerCase().startsWith(TAG_INTERPRET_AS_JAVA))
		{
			String v = value.getValue().substring(TAG_INTERPRET_AS_JAVA.length()).trim();
			value.setValue(v);
			value.setReference(true);
		}

		if (value.getValue().contains(BEGIN_REFERENCE))
		{
			// expand complex value

			int index1 = value.getValue().indexOf(BEGIN_REFERENCE);
			while (index1 != -1)
			{
				index1 = expandReferenceValue(tc, action, column, value, index1, alertCollector);
			}
		}

		if (value.getValue().contains(BEGIN_STATIC))
		{
			// expand static value

			int index1 = value.getValue().indexOf(BEGIN_STATIC);
			while (index1 != -1)
			{
				index1 = expandStaticValue(tc, action, column, value, index1, alertCollector);
			}
		}

		if (value.getValue().contains(BEGIN_FUNCTION))
		{
			// expand function

			int index1 = value.getValue().indexOf(BEGIN_FUNCTION);
			while (index1 != -1)
			{
				index1 = expandUtilityFunction(action, column, value, index1, alertCollector);
			}
		}
	}

	private int expandReferenceValue(AMLTestCase tc, AMLAction action,
			String column, Value value, int index, AlertCollector alertCollector)
	{

		int index2 = value.getValue().indexOf(END_REFERENCE, index);
		if (index2 == -1)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Unbalansed brackets found in column '"+column+"'."));
			return -1;
		}

		// get reference to previous message and field name

		String var = value.getValue().substring(index+BEGIN_REFERENCE.length(), index2);
		String lineRef;
		String columnRef;

		if (var.length() == 0)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Reference is empty column '"+column+"'."));
			return -1;
		}

		String[] arr = StringUtil.split(var, ":");
		if (arr.length == 0 || arr.length > 2)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Invalid reference format in column '"+column+"': '"+var+"'. "
					+"Expected format: ${reference:column} or ${reference}."));
			return -1;
		}

		lineRef = arr[0].trim(); // reference to message
		columnRef = (arr.length == 2) ? arr[1].trim() : column; // reference to field

		if (lineRef.length() == 0)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Reference to row is missed in column '"+column+"': '"+value.getOrigValue()+"'."));
			return -1;
		}
		if (columnRef.length() == 0)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Reference to column is missed in column '"+column+"': '"+value.getOrigValue()+"'."));
			return -1;
		}

		// find action by reference

		AMLAction refAction = tc.findActionByRef(lineRef);

		if (refAction == null)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Reference to unknown action '"+lineRef
					+"' is found in column '"+column+"': '"+value.getOrigValue()+"'."));
			return -1;
		}

		// ${ref:field} linked to submessage we should not be expanded immediately
		// because type of the submessage is not yet defined until it expanded
		if (!definedReferences.contains(lineRef))
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Reference '"+lineRef+"' is not yet defined in column '"
					+column+"': '"+value.getOrigValue()+"'."));
			return -1;
		}

        Class<?> messageType = refAction.getActionInfo().getReturnType();

        if(messageType == void.class)
		{
            alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Cannot refer to action."));
            return -1;
		}

		// check field in referred message

		boolean columnExist = action.getHeaders().contains(columnRef);
		boolean getterExist = false;
		if (columnExist == false)
		{
			Method[] methods = messageType.getMethods();
			for (Method method : methods)
			{
				if (method.getName().equalsIgnoreCase("get"+columnRef)) {
					getterExist = true;
					break;
				}
			}
		}


		if (false == (getterExist || columnExist))
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Reference to unknown column '"
					+columnRef+"' is found in column '"+column+"': '"+value.getOrigValue()+"'."));
			return -1;
		}

		// replace reference

		IGetterSetterGenerator gs = (IGetterSetterGenerator)adapterManager.getAdapter(messageType, IGetterSetterGenerator.class);
		if (gs == null)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Reference error in column '"+column+"': "
					+"No getter/setter factory registered for class '"+messageType
					+"' in line "+refAction.getLine()+" ["+lineRef+"]"));
			return -1;
		}

		String getter = "";
        String source = "(("+messageType.getCanonicalName()+")"+CodeGenerator_new.MAP_NAME+".get(\""+lineRef+"\"))";
		try {
			getter = gs.getGetter(messageType, columnRef, source);
		} catch (AMLException e) {
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Column '"+column+"': "+e.getMessage()));
			return -1;
		}

		if (getter == null)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Reference error in column '"+column+"': "+
					"Cannot extract value '"+columnRef+"' from message '"
					+messageType.getCanonicalName()+"' in line "
					+refAction.getLine()+"["+lineRef+"]."));
			return -1;
		}

		String src = BEGIN_REFERENCE+var+END_REFERENCE;
		String v = value.getValue().replaceFirst(Pattern.quote(src), getter);
		value.setValue(v);
		value.setReference(true);

		// search for the next reference

		index = value.getValue().indexOf(BEGIN_REFERENCE);

		return index;
	}

	private int expandStaticValue(AMLTestCase tc, AMLAction action,
			String column, Value value, int index, AlertCollector alertCollector)
	{

		int index2 = value.getValue().indexOf(END_STATIC, index);

		if (index == -1)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Unbalansed brackets found in column '"+column+"'."));
			return -1;
		}

		// get reference to previous message and field name

		String val = value.getValue().substring(index+2, index2);
		if (val.length() == 0)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Reference is empty in column '"+column+"'."));
			return -1;
		}

		if (val.contains(":"))
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Invalid reference format to static variable in column '"+column+"': '"+val+"'. "
					+"Expected format: %{reference}."));
			return -1;
		}

		// find action by reference

		AMLAction refAction = tc.findActionByRef(val);

		if (refAction == null)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Undefined reference ["+val
					+"] is found column '"
					+column+"': '"+value.getOrigValue()+"'."));
			return -1;
		}


		// replace reference

		String var = value.getValue().substring(index, index2+1);
		String targ = "("+refAction.getStaticType()+")("+CodeGenerator_new.STATIC_MAP_NAME+".get(\""+val+"\"))";
		String v = value.getValue().replaceFirst(Pattern.quote(var), targ);
		value.setValue(v);
		value.setReference(true);

		// search for the next reference

		index = value.getValue().indexOf(BEGIN_STATIC);

		return index;
	}

	private int expandUtilityFunction(AMLAction action,
			String column, Value value, int index, AlertCollector alertCollector)
	throws SecurityException
	{

		int index2 = CodeGenerator_new.indexOfCloseBracket(value.getValue(), BEGIN_FUNCTION, END_FUNCTION, index);
		if (index2 == -1)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Unbalansed brackets in column '"+column+"'."));
			return -1;
		}

		// get name of the static method declared in action class
		// with @UtilFunction annotation

		String var = value.getValue().substring(index+2, index2);

		if (var.length() == 0)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Utility function name is empty in column '"+column+"'."));
			return -1;
		}

		int openSpaceIndex = var.indexOf("(");

		if (openSpaceIndex == -1) {
            alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Syntaxis error in column '"+column+"': missed close bracket ')'."));
            return -1;
        }

		int closeSpaceIndex = CodeGenerator_new.indexOfCloseBracket(var, "(", ")", openSpaceIndex);

        if (closeSpaceIndex == -1) {
            alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Syntaxis error in column '"+column+"': missed close bracket ')'."));
            return -1;
        }

		SailfishURI utilityURI;

		try {
            utilityURI = SailfishURI.parse(var.substring(0, openSpaceIndex).trim());
        } catch(SailfishURIException e) {
            alertCollector.add(new Alert(action.getLine(),  action.getUID(), action.getReference(), column, e.getMessage()));
            return -1;
        }

        StringBuilder utilityArgs = new StringBuilder(var.substring(openSpaceIndex + 1, closeSpaceIndex).trim());

        if(utilityArgs.length() > 0) {
            utilityArgs.insert(0, ", ");
        }
        utilityArgs.append(')').append(var.substring(closeSpaceIndex + 1).trim());

		if (action.getStaticType() != null)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Invalid use of function in static variable definition: "+var));
			return -1;
		}

		if (action.getActionInfo() == null)
		{
			throw new NullPointerException("action.getActionInfo()");
		}

		ActionInfo actionInfo = action.getActionInfo();
		UtilityInfo utilityInfo;

		try {
            utilityInfo = actionManager.getUtilityInfo(actionInfo.getURI(), utilityURI);
        } catch(SailfishURIException e) {
            alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, e.getMessage()));
            return -1;
        }

		if(utilityInfo == null) {
		    alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), "Unable to resolve utility function: " + utilityURI));
		    return -1;
		}

		String src = BEGIN_FUNCTION+var+END_FUNCTION;
        String targ = String.format("%s(%s.parse(\"%s\")%s", UTILITY_MANAGER_CALL, SailfishURI.class.getSimpleName(), utilityInfo.getURI(), utilityArgs);
        targ = targ.replace("\\", "\\\\").replace("$", "\\$");

        String v = value.getValue().replaceFirst(Pattern.quote(src), targ);
        value.setValue(v);
        value.setReference(true);
        value.addParameter(new RefParameter(CodeGenerator_new.UTILITY_MANAGER_VARIABLE, CodeGenerator_new.UTILITY_MANAGER));

        // search next function

        index = value.getValue().indexOf(BEGIN_FUNCTION);

        return index;
	}

	/**
	 * Compile simple class to check whether value valid.
	 * If compilation failed appropriate message will be stored in ErrorStore.
	 * @throws AMLException
	 */
    protected final String compileTest(File javaFile, File classFile) throws AMLException
	{
		String status = null;
		try {
			List<String> args = new LinkedList<>();
			args.add("-classpath");
			args.add(compilerClassPath);
			args.add(javaFile.getAbsolutePath());

            logger.debug("compilerClassPath = " + compilerClassPath);

			String[] sargs = args.toArray(new String[0]);

			InputStream inStream = new ByteArrayInputStream(new byte[0]);
			OutputStream outStream = new ByteArrayOutputStream();
			OutputStream errStream = new ByteArrayOutputStream();
			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
			long time = System.currentTimeMillis();
			int err = compiler.run(inStream, outStream, errStream, sargs);
            logger.debug("compile time: " + (System.currentTimeMillis() - time));
			if (err != 0)
			{
				logger.error("Test file saved to: {}", javaFile.getCanonicalPath());
				// leave test file
				return errStream.toString();
			}

			javaFile.delete();
			if (classFile.exists())
			{
				classFile.delete();
			}

		} catch (IOException e) {
			throw new AMLException("Failed write to file '"+javaFile+"':"+e.getLocalizedMessage(), e);
		}
		return status;
	}

    private void testCode(List<AMLTestCase> testCases) throws WorkspaceSecurityException, AMLException, FileNotFoundException, IOException {
        progressChanged(60);

        List<SetterInfo> setters = new ArrayList<>();

        for(AMLTestCase testCase : testCases) {
            for(AMLAction action : testCase.getActions()) {
                for(Pair<String, String> setter : action.getSetters()) {
                    String column = setter.getFirst();
                    String code = setter.getSecond();
                    String value = action.getParameters().get(column).getValue();
                    String reference = ObjectUtils.defaultIfNull(action.getReference(), action.getReferenceToFilter());

                    StringUtils.removeStart(value, BoolExp.NotEqualsUnary.getName());
                    value = new String(Base64.encodeBase64(value.getBytes()));
                    SetterInfo info = new SetterInfo(column, code, value, action.getLine(), action.getUID(), reference);

                    setters.add(info);
                }
            }
        }

        int count = 0;

        for(List<SetterInfo> subList : Iterables.partition(setters, MAX_SETTERS_PER_CLASS)) {
            String className = "TestClass" + count++;
            File javaFile = workspaceDispatcher.createFile(FolderType.REPORT, true, amlSettings.getBaseDir(), className + ".java");
            File classFile = workspaceDispatcher.createFile(FolderType.REPORT, true, amlSettings.getBaseDir(), className + ".class");

            try(TextOutputStream stream = new TextOutputStream(new FileOutputStream(javaFile))) {
                tcCodeBuilder.writeTestClass(stream, className, subList);
                String error = compileTest(javaFile, classFile);

                if(error == null) {
                    continue;
                }

                parseErrors(error);
            }
        }

        progressChanged(70);
    }

	private void parseErrors(String error)
	{
		if (error.contains(ERROR_HOOK))
		{
			String[] lines = error.split("\n");
			for (String line : lines)
			{
				if (line.contains(ERROR_HOOK))
				{
					int index = line.indexOf(ERROR_HOOK);
					index += ERROR_HOOK.length()+1;
					String data = line.substring(index);
					String[] vals = StringUtil.split(data, ":");
					String base64 = StringUtil.join(":", vals, 4);
					byte[] bytes = Base64.decodeBase64(base64.getBytes());
					String value = new String(bytes);
					error = "Invalid value in column '"+vals[3]+"': "+value;
					if (!alertCollector.contains(AlertType.ERROR, error)) {
						this.alertCollector.add(new Alert(Long.parseLong(vals[0]), Long.parseLong(vals[1]), vals[2], vals[3], error));
					}
				}
			}
			return;
		}
		this.alertCollector.add(new Alert(error));

	}

	/* (non-Javadoc)
     * @see com.exactpro.sf.aml.generator.ICodeGenerator#getErrors()
	 */
	@Override
	public AlertCollector getAlertCollector() {
		return this.alertCollector;
	}

	/* (non-Javadoc)
     * @see com.exactpro.sf.aml.generator.ICodeGenerator#cleanup()
	 */
	@Override
	public void cleanup()
	{
        if(definedReferences != null) {
			definedReferences.clear();
        }

        definedServiceNames.clear();
        resolvedServiceNames.clear();

        if(this.imports != null) {
			this.imports.clear();
        }
	}

	private void progressChanged(int progress)
	{
		for (IProgressListener listener : this.progressListeners)
		{
			listener.onProgressChanged(progress);
		}
	}

    public void addDefinedReferences(String definedReference) {
        definedReferences.add(definedReference);
    }

    public Set<String> getDefinedReferences() {
        return definedReferences;
    }

    protected void resolveDictionary(AMLAction action, IService service) {
        if(action.hasDictionaryURI() || service == null) {
            return;
        }

        IServiceSettings settings = service.getSettings();

        if(settings != null) {
            action.setDictionaryURI(settings.getDictionaryName());
        }
    }

    protected IService resolveService(String name, long line, long uid, String column) {
        if(name == null) {
            return null;
        }

        if(!Column.ServiceName.getName().equals(column)) {
            String temp = name;

            if(AMLLangUtil.isStaticVariableReference(name)) {
                temp = AMLLangUtil.getStaticVariableName(name);
            }

            String error = JavaValidator.validateVariableName(temp);

            if(error != null) {
                alertCollector.add(new Alert(line, uid, null, column, error));
                return null;
            }
        }

        if(AMLLangUtil.isStaticVariableReference(name)) {
            if(definedServiceNames.containsKey(name)) {
                name = definedServiceNames.get(name);
            } else {
                alertCollector.add(new Alert(line, uid, null, column, "Unknown service name reference: " + name));
                return null;
            }
        }

        return getService(name, line, uid, column);
    }

    protected static int indexOfCloseBracket(String source, String openBracket, String closeBracket, int indexOfStartBracket) {
        if (StringUtils.isBlank(source)) {
            throw new IllegalArgumentException("Source is empty");
        }

        if (source.indexOf(openBracket, indexOfStartBracket) != indexOfStartBracket) {
            throw new IllegalArgumentException("Incorrect start index");
        }

        for (int index = indexOfStartBracket + 1, status = 1; index < source.length(); index++) {
            if (source.indexOf(openBracket, index) == index) {
                status++;
            } else if (source.indexOf(closeBracket, index) == index) {
                status--;
            }

            if (status == 0) {
                return index;
            }
        }

        return -1;
//        throw new EPSCommonException("String '" + source + "' does not contains close bracket '" + closeBracket + "' for open bracket '" + openBracket
//                + "' on index " + indexOfStartBracket);
    }

    protected static void addExecutedActionReferences(StringBuilder sb, AMLAction action, String tab) {
        if(action.hasReference()) {
            sb.append(tab);
            sb.append(CONTEXT_NAME);
            sb.append(".addExecutedAction(");
            sb.append(StringUtil.enclose(action.getReference()));
            sb.append(");");
            sb.append(EOL);
        }

        if(action.hasReferenceToFilter()) {
            sb.append(tab);
            sb.append(CONTEXT_NAME);
            sb.append(".addExecutedAction(");
            sb.append(StringUtil.enclose(action.getReferenceToFilter()));
            sb.append(");");
            sb.append(EOL);
        }
    }

    private IService getService(String name, long line, long uid, String column) {
        ServiceName serviceName = new ServiceName(scriptContext.getEnvironmentName(), name);
        IService service = environmentManager.getConnectionManager().getService(serviceName);

        if(service == null) {
            alertCollector.add(new Alert(line, uid, null, column, "Unknown service: " + name));
            return null;
        }

        resolvedServiceNames.add(serviceName.toString());

        return service;
    }
}
