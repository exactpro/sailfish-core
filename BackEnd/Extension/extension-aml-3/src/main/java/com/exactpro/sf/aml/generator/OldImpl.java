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

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.AMLAction;
import com.exactpro.sf.aml.AMLException;
import com.exactpro.sf.aml.AMLLangConst;
import com.exactpro.sf.aml.AMLLangUtil;
import com.exactpro.sf.aml.AMLTestCase;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.generator.matrix.JavaStatement;
import com.exactpro.sf.aml.generator.matrix.RefParameter;
import com.exactpro.sf.aml.generator.matrix.Value;
import com.exactpro.sf.aml.generator.matrix.Variable;
import com.exactpro.sf.aml.script.DefaultSettings;
import com.exactpro.sf.aml.script.MetaContainer;
import com.exactpro.sf.common.adapting.IAdapterManager;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.Pair;
import com.exactpro.sf.common.util.StringUtil;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.scriptrunner.actionmanager.ActionInfo;
import com.exactpro.sf.scriptrunner.actionmanager.IActionManager;
import com.exactpro.sf.scriptrunner.utilitymanager.IUtilityManager;
import com.exactpro.sf.services.IService;

public class OldImpl {

	private static final Logger logger = LoggerFactory.getLogger(OldImpl.class);

	protected final static String EOL = System.getProperty("line.separator");

	public static final String TAB1 = "\t";
	public static final String TAB2 = "\t\t";
	public static final String TAB3 = "\t\t\t";
	public static final String TAB4 = "\t\t\t\t";
	private static final String MESSAGE_PREFIX = "m";
	private static final String LIST_PREFIX = "l";
	public static final String MAP_NAME = "messages";
	public static final String CONTEXT_NAME = "context";
	public static final String REPORT_NAME = "report";
	public static final String STATIC_MAP_NAME = CONTEXT_NAME+".getStaticMap()";
	public static final String SERVICE_MAP_NAME = CONTEXT_NAME+".getServiceNames()";
	private static final String LOGGER_NAME = "logger";
	private boolean continueOnFailed;

	private List<Variable> variables;

	private final AlertCollector alertCollector;
	private final IAdapterManager adapterManager;
	private final IDictionaryManager dictionaryManager;
	private final IActionManager actionManager;
	private final IUtilityManager utilityManager;
	private final CodeGenerator_new codeGenerator;

	private final static int CAPACITY_4K = 4096;
    private final static int CAPACITY_128K = 131072;

	public OldImpl(AlertCollector alertCollector, IAdapterManager adapterManager, IDictionaryManager dictionaryManager, IActionManager actionManager, IUtilityManager utilityManager, CodeGenerator_new codeGenerator) {
		this.alertCollector = alertCollector;
		this.adapterManager = adapterManager;
		this.dictionaryManager = dictionaryManager;
		this.actionManager = actionManager;
		this.utilityManager = utilityManager;
        this.codeGenerator = codeGenerator;
	}

	protected void setContinueOnFailed(boolean b) {
		this.continueOnFailed = b;
	}

	String writeFillMessage(AMLTestCase tc, AMLAction action, List<Variable> variables2) throws AMLException {

		this.variables = variables2;
		if (action.getGenerateStatus() == AMLGenerateStatus.GENERATED) {
			return null;
		}

		if (action.getGenerateStatus() == AMLGenerateStatus.GENERATING) {
			this.alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), "Recursion detected"));
			return null;
		}

		action.setGenerateStatus(AMLGenerateStatus.GENERATING);

		StringBuilder sb = new StringBuilder(CAPACITY_128K);
		Variable inputVariable = null;
		Variable outputVariable = null;
		Class<?> type = null;

        if(action.hasServiceName()) {
            IService service = codeGenerator.resolveService(action.getServiceName(), action.getLine(), action.getUID(), Column.ServiceName.getName());

            if(service != null) {
                action.setServiceName(service.getServiceName().getServiceName());
                codeGenerator.resolveDictionary(action, service);
            }
        }

		if(action.hasActionURI()) {
	        openTryClause(sb);
	    }

		if (action.getActionInfo() != null)
		{
		    type = action.getActionInfo().getMessageType();

		    if (type != null)
	        {
	            inputVariable = getVariable(type, MESSAGE_PREFIX + type.getSimpleName());
	            sb.append(createMessageDefinition(tc, action, inputVariable));
	            addReferenceToFilter(sb, action, inputVariable);
	        }

			Class<?> returnType = action.getActionInfo().getReturnType();
			if (action.hasReference() && void.class.equals(returnType))
			{
				this.alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), "Cannot refer to void method: "+action.getActionURI()+". "
						+ "Please remove reference label and all reference occurrences to this row.", AlertType.WARNING));
			}
			else if (false == void.class.equals(returnType))
			{
				outputVariable = getVariable(returnType, MESSAGE_PREFIX + returnType.getSimpleName());
			}
		}

		if (action.hasActionURI())
		{
            sb.append(writeFillMetaContainer(tc, action, action.getActionInfo(), null, getVariable(MetaContainer.class, "metaContainer"), 0, null));
			JavaStatement statement = JavaStatement.value(action.getActionURI());

			if (statement != null)
			{
				action.setGenerateStatus(AMLGenerateStatus.GENERATED);
				return sb.toString();
			}

			Variable settings = getVariable(DefaultSettings.class, "settings");
			String s = codeGenerator.createFillSettings(tc, action, null, settings, this.alertCollector);
			sb.append(s);
			sb.append(TAB2+LOGGER_NAME+".debug(\"start action: "+action.getActionURI()+", line:"+action.getLine()+"\");"+EOL);

            writeActionCall(tc, action, inputVariable, outputVariable, settings, sb);

            Variable variable = new Variable("containedMessage", Object.class);

            sb.append(TAB2 + "} catch (KnownBugException e) {" + EOL);
            sb.append(TAB3 + LOGGER_NAME + ".warn(e);" + EOL);
            sb.append(TAB3 + variable.getType().getSimpleName() + " ");
            sb.append(variable.getName() + " = null;" + EOL);
            sb.append(TAB3 + "if(e instanceof MessageKnownBugException) {" + EOL);
            sb.append(TAB4 + variable.getName() + " = ((MessageKnownBugException)e).getContainedMessage();" + EOL);
            sb.append(TAB4 + CONTEXT_NAME + ".getReceivedMessages().add(");
            sb.append(variable.getName());
            sb.append(");" + EOL);

            addReference(sb, action, inputVariable, variable, TAB4);

            sb.append(TAB3 + "}" + EOL);

            CodeGenerator_new.addExecutedActionReferences(sb, action, TAB3);

            if(action.getOutcome() != null) {
                sb.append(TAB3 + CONTEXT_NAME + ".getOutcomeCollector().storeOutcome(settings.getOutcome());" + EOL);
            }

            NewImpl.writeCreateTestCase(tc, sb);
            writeCreateAction(action, sb);
            NewImpl.addActionToReport(action, sb, true, variable, TAB3);

            closeTryClause(tc, action, sb, action.getContinueOnFailed() || this.continueOnFailed);
		}
		else
		{
			// add reference for submessage
			addReference(sb, action, inputVariable, outputVariable, TAB2);
            CodeGenerator_new.addExecutedActionReferences(sb, action, TAB2);
		}

		if (sb.length() != 0) {
			sb.append(EOL);
		}

        action.setGenerateStatus(AMLGenerateStatus.GENERATED);

		return sb.toString();
	}

    private String writeFillMetaContainer(AMLTestCase tc, AMLAction action, ActionInfo actionInfo, String field, Variable metaContainer, int index, String parentVar) {
        StringBuilder sb = new StringBuilder(CAPACITY_128K);

        sb.append(EOL);
        String failUnexpected = null;

        if(action.getFailUnexpected() != null && !"".equals(action.getFailUnexpected())) {
            failUnexpected = TypeConverter.convert(java.lang.String.class, action.getFailUnexpected());
        }

        if(parentVar != null) {
            sb.append(TAB2 + metaContainer.getName() + " = createMetaContainer(" + parentVar + ", " + TypeConverter.convert(java.lang.String.class, field) + ", " + failUnexpected + ");" + EOL);
        } else {
            sb.append(TAB2 + metaContainer.getName() + " = createMetaContainer(" + failUnexpected + ");" + EOL);
        }

        codeGenerator.putSystemColumns(sb, metaContainer.getName(), tc, action, actionInfo, alertCollector);

        for(Pair<String, AMLAction> child : action.getChildren()) {
            index++;
            sb.append(writeFillMetaContainer(tc, child.getSecond(), actionInfo, child.getFirst(), getVariable(MetaContainer.class, "mc" + (child.getFirst() == null ? "" : child.getFirst()) + index), index, metaContainer.getName()));
        }

        return sb.toString();
    }

	private String getReturnTypeName(AMLAction action) {

		String msgType = null;

		if(action.getActionInfo() != null && action.getActionInfo().getReturnType() != null) {
			msgType = action.getActionInfo().getReturnType().getSimpleName();
		}

		return msgType;

	}

	private Variable getVariable(Class<?> type, String varNameOrig)
	{
		String varName = varNameOrig;

		Variable var = new Variable(varName, type);
		// check if variable with same name and type already exists
		if (this.variables.contains(var))
		{
			return var;
		}

		// check if variable with same name and different type already exists

		int i=1;
		boolean found = true;
		while (found)
		{
			found = false;
			for (Variable v : this.variables)
			{
				if (v.getName().equals(varName))
				{
					found = true;
					varName = varNameOrig+(i++);
					break;
				}
			}
			if (found == false)
			{
				var = new Variable(varName, type);
				this.variables.add(var);
				return var;
			}
		}
		// should not happen
		logger.error("This should not happen");
		return null;
	}


	private String createMessageDefinition(AMLTestCase tc, AMLAction action, Variable variable)
	{
		List<String> subMessages = new ArrayList<>();
		StringBuilder sb = new StringBuilder(CAPACITY_128K);
		sb.append(TAB2+variable.getName()+" = new "+variable.getType().getCanonicalName()+"();"+EOL);
		for (Entry<String, Value> e : action.getParameters().entrySet())
		{
			String column = e.getKey();
			Value v = e.getValue();

			try {
				IGetterSetterGenerator gsFactory = (IGetterSetterGenerator)
				this.adapterManager.getAdapter(action.getActionInfo().getMessageType(), IGetterSetterGenerator.class);

				if (gsFactory == null)
				{
					this.alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), "Cannot find getter&setter generator instance for type '"
							+action.getActionInfo().getMessageType().getCanonicalName()+"'"));
					continue;
				}

				if (AMLLangUtil.isSubmessage(v.getValue()))
				{
					expandSubmessage(tc, action, variable, column, v, sb, gsFactory, subMessages);
				}
				else if (AMLLangUtil.isArray(v.getValue()))
				{
				    OldImplHelper.substituteReference(tc, action, alertCollector, column, v, codeGenerator.getDefinedReferences(), dictionaryManager, actionManager, utilityManager);
					String[] values = AMLLangUtil.getValues(v.getValue());

					for (String val : values)
					{
						expandSetter(action, gsFactory, sb, column, v, val, variable.getName());
					}
				}
				else
				{
				    OldImplHelper.substituteReference(tc, action, alertCollector, column, v, codeGenerator.getDefinedReferences(), dictionaryManager, actionManager, utilityManager);
					expandSetter(action, gsFactory, sb, column, v, v.getValue(), variable.getName());
				}
			} catch (AMLException | SailfishURIException ex) {
				this.alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Column '"+column+"': "+ex.getMessage()));
				continue;
			}
		}

		StringBuilder sb1 = new StringBuilder(CAPACITY_128K);

		for (String subMessage : subMessages)
		{
			sb1.append(subMessage);
		}

		sb1.append(sb);

		return sb1.toString();
	}

	private void expandSetter(AMLAction action, IGetterSetterGenerator gsFactory, StringBuilder sb, String column, Value v, String val, String variable) throws AMLException {
		if (v.isReference()) {
		    Value tempValue = new Value(val);

		    for(RefParameter p : v.getParameters()) {
		        tempValue.addParameter(p);
            }

		    val = NewImpl.generateEval(action.getLine(), column, tempValue, TAB3);
		}

		String setter = gsFactory.getSetter(action.getActionInfo().getMessageType(), column, val, v.isReference() || v.isJava());
		if (setter == null)
		{
			this.alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Cannot set value '"+val+"' of type '"
					+column+"' for message '"+action.getActionInfo().getMessageType().getCanonicalName()+"'"));
			return;
		}

		//TODO: Add isCheck for IGetterSetterGenerator (for example HashMap)
		if (v.isCheck())
		{
			String code = "new "+action.getActionInfo().getMessageType().getCanonicalName()+"()"+setter;
			action.getSetters().add(new Pair<>(column, code));
		}
		sb.append(TAB2+variable+setter+";"+EOL);
	}

	private void expandSubmessage(AMLTestCase tc, AMLAction action, Variable variable, String column, Value v, StringBuilder sb, IGetterSetterGenerator gsFactory, List<String> subMessages)
	throws AMLException
	{
		String[] references = AMLLangUtil.getReferences(v.getValue());
		StringBuilder setter = new StringBuilder(CAPACITY_4K);
		Variable subListVariable = getVariable(List.class, LIST_PREFIX + column);

		for (String ref : references)
		{
		    if (setter.length() == 0) {
                setter.append(TAB2 + subListVariable.getName() +" = new ArrayList<Object>();" + EOL);
            }

		    if(AMLLangUtil.isString(ref)) {
		        ref = ref.substring(1, ref.length() - 1);
		        setter.append(TAB2 + subListVariable.getName() + ".add(\"" + StringEscapeUtils.escapeJava(ref) + "\");" + EOL);

		        continue;
		    }

		    if(ref.startsWith(AMLLangConst.BEGIN_STATIC) && ref.endsWith(AMLLangConst.END_STATIC)) {
		        ref = StringUtils.substringBetween(ref, AMLLangConst.BEGIN_STATIC, AMLLangConst.END_STATIC);
		        String[] refSplit = ref.split("[:.]");
	            String reference = refSplit[0];
	            AMLAction subAction = tc.findActionByRef(reference);

	            if (subAction == null)
	            {
	                this.alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Column '"+column+"'"
	                        +": Reference '"+ref+"' not defined in matrix."));
	                continue;
	            }

	            if(subAction.getGenerateStatus() != AMLGenerateStatus.GENERATED) {
	                alertCollector.add(new Alert(action.getLine(), action.getReference(), column, "Static reference to a not generated action: " + reference));
	                continue;
	            }

	            if(JavaStatement.SET_STATIC.getURI().equals(subAction.getActionURI())) {
	                if(refSplit.length > 1) {
                        alertCollector.add(new Alert(action.getLine(), action.getReference(), column,
                                "Invalid reference format to static variable in column '" + column + "': '" + ref + "'. " + "Expected format: %{reference}."));
                        continue;
	                }
	            } else if(!subAction.isStaticAction()) {
                    alertCollector.add(new Alert(action.getLine(), action.getReference(), column, "Reference to a non-static action: " + reference));
                    continue;
	            }

                refSplit[0] = "v0";
		        StringBuilder eval = new StringBuilder(CAPACITY_4K);

		        eval.append("eval(");
		        eval.append(action.getLine());
		        eval.append(", ");
		        eval.append(StringUtil.enclose(column));
		        eval.append(", ");
	            eval.append(StringUtil.enclose(StringUtils.join(refSplit, '.')));
	            eval.append(", ");
	            eval.append(StringUtil.enclose("v0"));
	            eval.append(", ");
	            eval.append(STATIC_MAP_NAME);
	            eval.append(".get(");
	            eval.append(StringUtil.enclose(ref));
	            eval.append("))");

	            setter.append(TAB2 + subListVariable.getName() + ".add(" + eval.toString() + ");" + EOL);

                action.addChildAction(column, subAction);

	            continue;
		    }

		    String[] newRefs = AMLLangUtil.findReferences(ref);

		    if(newRefs.length > 0) {
		        ref = newRefs[0];
		    }

		    String[] refSplit = ref.split("[:.]");
            String reference = refSplit[0];
			AMLAction subAction = tc.findActionByRef(reference);

			if (subAction == null)
			{
				this.alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Column '"+column+"'"
						+": Reference '"+reference+"' not defined in matrix."));
				continue;
			}

			/*if(refSplit.length > 2) {
                this.alertCollector.add(new Alert(action.getLine(), action.getReference(), column, "Column '"+column+"'"
                        +": Invalid reference '"+ref+"'."));
                break;
            }

            if(refSplit.length > 1) {
                refColumn = refSplit[1];

                if(!subAction.getParameters().containsKey(refColumn)) {
                    alertCollector.add(new Alert(action.getLine(), action.getReference(), column, "Reference to unknown column '"
                            +refColumn+"' is found in column '"+column+"': '"+ref+"'."));
                }
            }*/

			if(subAction.getActionInfo() == null) {
			    subAction.setActionInfo(action.getActionInfo().clone());
			}

			Class<?> type = gsFactory.getSubmessageClass(action.getActionInfo().getMessageType(), column, subAction.getActionInfo().getMessageType());

			if (type == null)
			{
				this.alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Cannot find submessage "+column+" in message '"+action.getActionInfo().getMessageType().getCanonicalName()+"'."));
				continue;
			}

			subAction.getActionInfo().setMessageType(type);
			subAction.getActionInfo().setReturnType(type);

			if (subAction.getMessageTypeColumn() != null
					&& false == subAction.getMessageTypeColumn().equals(""))
			{
				if (false == type.equals(List.class)
				        && false == type.equals(IMessage.class)
						&& false == type.getCanonicalName().equals(subAction.getMessageTypeColumn())
						&& false == type.getSimpleName().equals(subAction.getMessageTypeColumn()))
				{
					this.alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Message type ["+subAction.getMessageTypeColumn()+"] for submessage ["+subAction.getReference()
							+"] does not match calculated submessage type '"+type.getCanonicalName()+"'."
							+" Possible that reference placed in wrong column or specified submessage type is incorrect."
							+" Make sure that value in "+Column.MessageType.getName()+" column for message in line "
							+subAction.getLine()+" ["+subAction.getReference()+"] is equals with value in column "
							+column+" for message in line "+action.getLine()+" ["+action.getReference()+"]"));
					continue;
				}
			}
            codeGenerator.addDefinedReferences(reference);
			String subMessage = writeFillMessage(tc, subAction, this.variables);
			if (subMessage != null) {
				subMessages.add(subMessage);
				subAction.addGenerationSteps(action.getGenerationPath(), action.getReference());
			}

            refSplit[0] = "v0";
			StringBuilder eval = new StringBuilder(CAPACITY_4K);

			eval.append("eval(");
	        eval.append(action.getLine());
	        eval.append(", ");
	        eval.append(StringUtil.enclose(column));
	        eval.append(", ");
            eval.append(StringUtil.enclose(StringUtils.join(refSplit, '.')));
            eval.append(", ");
            eval.append(StringUtil.enclose("v0"));
            eval.append(", ");
            eval.append(MAP_NAME);
            eval.append(".get(");
            eval.append(StringUtil.enclose(reference));
            eval.append("))");

			setter.append(TAB2 + subListVariable.getName() + ".add(" + eval.toString() + ");" + EOL);

            action.addChildAction(column, subAction);
		}

		if(setter.length() > 0) {
			sb.append(EOL + setter.toString() + EOL);
			sb.append(TAB2 + variable.getName() + ".put(\"" + column + "\", " + subListVariable.getName() + ");" + EOL);

    		if (v.isCheck()) {
    			action.getSetters().add(new Pair<>(column, subListVariable.getType().getCanonicalName() + " " + setter.toString()));
    		}
		}
	}

	void addReferenceToFilter(StringBuilder sb, AMLAction action, Variable inputVariable)
	{
		if (false == "".equals(action.getReferenceToFilter()))
		{
			if ((action.getActionInfo() == null)
					|| (action.getActionInfo() != null
							&& false == void.class.equals(action.getActionInfo().getReturnType())))
			{
				if (inputVariable != null)
				{
				    String mapName = action.isStaticAction() ? STATIC_MAP_NAME : MAP_NAME;
					sb.append(TAB2+mapName+".put(\""+action.getReferenceToFilter()+"\", "+inputVariable.getName()+");"+EOL);
				}
			}
		}
	}


    private void writeActionCall(AMLTestCase tc, AMLAction action, Variable inputVariable, Variable outputVariable, Variable settings, StringBuilder sb)
	throws AMLException
	{
		IGetterSetterGenerator factory = null;

		if(inputVariable != null) {
            factory = (IGetterSetterGenerator)this.adapterManager.getAdapter(action.getActionInfo().getMessageType(), IGetterSetterGenerator.class);

    		if (factory == null)
    		{
    			this.alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), "Cannot find getter&setter generator instance for type '"
    					+action.getActionInfo().getMessageType().getCanonicalName()));
    			throw new AMLException("Cannot find getter&setter generator", this.alertCollector);
    		}
		}

		if (action.isAddToReport())
		{
			String id = (action.getId() == null) ? "" : action.getId()+" ";
			String serviceName = action.hasServiceName() ? action.getServiceName() + " " : "";
			String method = "";

			if(factory != null) {
		        method = factory.getMethodForExtractingTreeEntity();

		        if (method == null) {
		            throw new AMLException("Factory "+factory.getClass().getCanonicalName()+" return null from getMethodForExtractingTreeEntity()");
		        }
			}

			String description = StringUtil.toJavaString(action.getDescrption());
			if (action.getOutcome() != null) {
				description = action.getOutcome()+" "+description;
			}
			sb.append(TAB2+REPORT_NAME+".createAction(\""
					+id+serviceName+action.getActionURI()+"\", "

					+ "\""+ serviceName + "\", "
					+ "\""+ action.getActionURI() + "\", "
					+ "\""+ getReturnTypeName(action) + "\", "

					+ "\""+description+"\", ");

			if(inputVariable != null) {
			    sb.append(inputVariable.getName());
			    sb.append(method);
			} else {
			    sb.append("null");
			}

            sb.append(", ");
            sb.append(settings.getName());
            sb.append(".getCheckPoint(), ");

            if(action.hasTag()) {
                sb.append(StringUtil.enclose(StringUtil.toJavaString(action.getTag()), '"'));
            } else {
                sb.append("null");
            }

            sb.append(",");
            sb.append(action.getHash());

            sb.append(", ");
            String verificationsOrder = action.getVerificationsOrder().stream().map(StringUtil::enclose).collect(Collectors.joining(", "));
            sb.append("Arrays.asList(");
            sb.append(verificationsOrder);
            sb.append(")");

            sb.append(");");
            sb.append(EOL);
		}

		ActionInfo actionInfo = action.getActionInfo();
        String arguments = String.format("%s.parse(\"%s\"), %s", SailfishURI.class.getSimpleName(), actionInfo.getURI(), settings.getName());

        if(inputVariable != null) {
            arguments += ", " + inputVariable.getName();
        }

		if (void.class.equals(actionInfo.getReturnType()))
		{
			sb.append(TAB2+CodeGenerator_new.ACTION_MANAGER_CALL+"("+arguments+");"+EOL);
		}
		else
		{
			sb.append(TAB2+outputVariable.getName()+" = "+CodeGenerator_new.ACTION_MANAGER_CALL+"("+arguments+");"+EOL);
			addReference(sb, action, inputVariable, outputVariable, TAB2);
		}

        CodeGenerator_new.addExecutedActionReferences(sb, action, TAB2);

		if(action.getOutcome() != null) {
            sb.append(TAB2);
            sb.append(CONTEXT_NAME);
            sb.append(".getOutcomeCollector().storeOutcome(new Outcome(\"");
            sb.append(action.getOutcomeGroup());
            sb.append("\", \"");
            sb.append(action.getOutcomeName());
            sb.append("\").setStatus(Status.PASSED));");
            sb.append(EOL);
		}

		if (action.isAddToReport())
		{
            NewImpl.addActionToReport(action, sb, false, outputVariable, TAB2);
		}

	}

	private void openTryClause(StringBuilder sb) {
		sb.append(TAB2+"try {"+EOL);
	}

    private void closeTryClause(AMLTestCase tc, AMLAction action, StringBuilder sb, boolean continueOnFailed) {
		sb.append(TAB2+"} catch (Exception e) {"+EOL);
		sb.append(TAB3+LOGGER_NAME+".warn(e);"+EOL);
		sb.append(TAB3+CONTEXT_NAME+".setInterrupt(e instanceof InterruptedException);"+EOL);

		if(action.getOutcome() != null) {
            sb.append(TAB3);
            sb.append(CONTEXT_NAME);
            sb.append(".getOutcomeCollector().storeOutcome(new Outcome(\"");
            sb.append(action.getOutcomeGroup());
            sb.append("\", \"");
            sb.append(action.getOutcomeName());
            sb.append("\").setStatus(Status.FAILED));");
            sb.append(EOL);
        } else {
            sb.append(TAB3+CONTEXT_NAME+".setException(e);"+EOL);
        }

        String id = (action.getId() == null) ? "" : action.getId() + " ";
        String serviceName = action.hasServiceName() ? action.getServiceName() + " " : "";
        String description = StringUtil.toJavaString(action.getDescrption());

        if(action.getOutcome() != null) {
            description = action.getOutcome() + " " + description;
        }

        NewImpl.writeCreateTestCase(tc, sb);
        writeCreateAction(action, sb, id, serviceName, description);

        sb.append(TAB3 + REPORT_NAME + ".closeAction(new StatusDescription(StatusType.FAILED, e.getMessage(), e");

        if(action.getOutcome() != null) {
            sb.append(", false");
        }

        sb.append("), null);");
        sb.append(EOL);

        if(continueOnFailed || action.getOutcome() != null) {
            sb.append(TAB3+"if (e instanceof InterruptedException) {"+EOL);
            sb.append(TAB4+"throw e;"+EOL);
            sb.append(TAB3+"}"+EOL);
        } else {
            sb.append(TAB3+"throw e;"+EOL);
        }

		sb.append(TAB2+"}"+EOL);
	}

    private void writeCreateAction(AMLAction action, StringBuilder sb) {
        String id = (action.getId() == null) ? "" : action.getId() + " ";
        String serviceName = action.hasServiceName() ? action.getServiceName() + " " : "";
        String description = StringUtil.toJavaString(action.getDescrption());
        writeCreateAction(action, sb, id, serviceName, description);
    }

    private void writeCreateAction(AMLAction action, StringBuilder sb, String id, String serviceName, String description) {
        String verificationsOrder = action.getVerificationsOrder().stream().map(StringUtil::enclose).collect(Collectors.joining(", "));

        sb.append(TAB3 + "if (!" + REPORT_NAME + ".isActionCreated()) {" + EOL);
        sb.append(TAB4 + REPORT_NAME + ".createAction(\""
                + id + serviceName + action.getActionURI() + "\", "

                + "\"" + serviceName + "\", "
                + "\"" + action.getActionURI() + "\", "
                + "\"" + getReturnTypeName(action) + "\", "

                + "\"" + description + "\" , null, null, "
                + (action.hasTag() ? StringUtil.enclose( StringUtil.toJavaString(action.getTag()), '"') : "null") + ", "
                + action.getHash()  + ", "
                + "Arrays.asList( " + verificationsOrder + ")"
                + ");" + EOL);
        sb.append(TAB3 + "}" + EOL);
    }

	void addReference(StringBuilder sb, AMLAction action, Variable inputVariable, Variable outputVariable, String tab)
    {
        if (!"".equals(action.getReference()))
        {
            if (action.getActionInfo() == null || (action.getActionInfo() != null && !void.class.equals(action.getActionInfo().getReturnType())))
            {
                if(outputVariable != null || inputVariable != null) {
                    String varName = (outputVariable != null ? outputVariable : inputVariable).getName();
                    String mapName = action.isStaticAction() ? STATIC_MAP_NAME : MAP_NAME;
                    sb.append(tab+mapName+".put(\""+action.getReference()+"\", "+varName+");"+EOL);
                    codeGenerator.addDefinedReferences(action.getReference());

                    if(!action.getIncludeBlockReference().isEmpty()) {
                        String ref = action.getIncludeBlockReference();
						String blockMapName = "m" + ref;

                        sb.append(tab+"java.util.HashMap "+blockMapName+" = (java.util.HashMap)"+MAP_NAME+".get(\""+ref+"\");"+EOL);
                        sb.append(tab+blockMapName+".put(\""+action.getReference()+"\", "+varName+");"+EOL);
                    }
                }
            }
        }
    }

}
