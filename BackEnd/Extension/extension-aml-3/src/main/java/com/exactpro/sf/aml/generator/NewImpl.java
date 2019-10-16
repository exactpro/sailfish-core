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

import com.exactpro.sf.aml.AMLAction;
import com.exactpro.sf.aml.AMLBlockType;
import com.exactpro.sf.aml.AMLException;
import com.exactpro.sf.aml.AMLLangConst;
import com.exactpro.sf.aml.AMLLangUtil;
import com.exactpro.sf.aml.AMLTestCase;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.generator.matrix.RefParameter;
import com.exactpro.sf.aml.generator.matrix.Value;
import com.exactpro.sf.aml.generator.matrix.Variable;
import com.exactpro.sf.aml.script.ActionContext;
import com.exactpro.sf.common.adapting.IAdapterManager;
import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.impl.FieldStructure;
import com.exactpro.sf.common.util.Pair;
import com.exactpro.sf.common.util.StringUtil;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.scriptrunner.IConnectionManager;
import com.exactpro.sf.scriptrunner.actionmanager.ActionInfo;
import com.exactpro.sf.scriptrunner.actionmanager.IActionManager;
import com.exactpro.sf.scriptrunner.services.IStaticServiceManager;
import com.exactpro.sf.scriptrunner.utilitymanager.IUtilityManager;
import com.exactpro.sf.services.IService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import static com.exactpro.sf.aml.AMLLangUtil.isFunction;
import static com.exactpro.sf.common.messages.structures.StructureUtils.getAttributeValue;
import static com.exactpro.sf.common.util.StringUtil.enclose;
import static com.exactpro.sf.common.util.StringUtil.toJavaString;
import static java.lang.String.format;

public class NewImpl {

	private static final String CONV_VALUE_MISSING = "#";

	private static final String CONV_VALUE_PRESENT = "*";

    public static final String REGEX_VALUE_PREFIX = "Regexp[";
    public static final String REGEX_VALUE_SUFFIX = "]";

	private static final Logger logger = LoggerFactory.getLogger(NewImpl.class);

    protected static final String EOL = System.getProperty("line.separator");

	public static final String TAB1 = "\t";
	public static final String TAB2 = "\t\t";
	public static final String TAB3 = "\t\t\t";
	public static final String TAB4 = "\t\t\t\t";
    public static final String TAB5 = "\t\t\t\t\t";
	private static final String MESSAGE_PREFIX = "m";
	public static final String MAP_NAME = "messages";
	public static final String CONTEXT_NAME = "context";
	public static final String REPORT_NAME = "report";
	public static final String STATIC_MAP_NAME = CONTEXT_NAME+".getStaticMap()";
	public static final String SERVICE_MAP_NAME = CONTEXT_NAME+".getServiceNames()";
	private static final String LOGGER_NAME = "logger";
    private boolean continueOnFailed;

	private List<Variable> variables;
	private final AlertCollector alertCollector;
    private final CodeGenerator_new codeGenerator;
    private final IDictionaryManager dictionaryManager;
    private final IActionManager actionManager;
    private final IUtilityManager utilityManager;

    private static final int CAPACITY_4K = 4096;
    private static final int CAPACITY_128K = 131072;

	public NewImpl(AlertCollector alertCollector, IAdapterManager adapterManager, IConnectionManager connectionManager, IDictionaryManager dictionaryManager, IActionManager actionManager, IUtilityManager utilityManager, IStaticServiceManager staticServiceManager, CodeGenerator_new codeGenerator) {
	    this.dictionaryManager = dictionaryManager;
	    this.actionManager = actionManager;
	    this.utilityManager = utilityManager;
		this.alertCollector = alertCollector;
        this.codeGenerator = codeGenerator;
		MVELInitializer.getInstance();
	}

	protected void setContinueOnFailed(boolean b) {
		this.continueOnFailed = b;
	}

	public String writeSendMessage(AMLTestCase tc, AMLAction action, List<Variable> variables, boolean isDirty) {

		this.variables = variables;
		if (action.getGenerateStatus() == AMLGenerateStatus.GENERATED) {
			return null;
		}

		if (action.getGenerateStatus() == AMLGenerateStatus.GENERATING) {
            alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), "Recursion detected"));
			return null;
		}

		action.setGenerateStatus(AMLGenerateStatus.GENERATING);

		StringBuilder sb = new StringBuilder(CAPACITY_128K);
		Variable inputVariable = null;
//		Variable outputVariable = null;
		Class<?> type = action.getActionInfo().getMessageType();

        if(action.hasServiceName()) {
            IService service = codeGenerator.resolveService(action.getServiceName(), action.getLine(), action.getUID(), Column.ServiceName.getName());

            if(service != null) {
                action.setServiceName(service.getServiceName().getServiceName());
                codeGenerator.resolveDictionary(action, service);
            }
        }

		if (type != null)
		{
			if (!action.hasDictionaryURI()) {
                alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), Column.Dictionary.getName(), "Dictionary is not specified"));
				return null;
			}

			sb.append(TAB2+"try {"+EOL);

			inputVariable = getVariable(type, MESSAGE_PREFIX+action.getMessageTypeColumn());
			sb.append(createSendMessageDefinition(tc, action, inputVariable, isDirty));
			addReferenceToFilter(sb, action, inputVariable);
			action.setGenerateStatus(AMLGenerateStatus.GENERATED);
            writeFillComparisonErrorsDefinition(sb, tc, action, isDirty);
			sb.append(createSendCall(tc,action,inputVariable));
		}
		else
		{
			sb.append(TAB2+"try {"+EOL);
			action.setGenerateStatus(AMLGenerateStatus.GENERATED);
            writeFillComparisonErrorsDefinition(sb, tc, action, isDirty);
			sb.append(createSendCall(tc,action, null));
		}

		return sb.toString(); // TODO: to be done
	}

    private void writeFillComparisonErrorsDefinition(StringBuilder builder, AMLTestCase tc, AMLAction action, boolean isDirty)
	{
        IDictionaryStructure dictionary = getDictionary(action.getDictionaryURI());

        if (dictionary == null) {
            alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(),
                    "Can't find dictionary [" + action.getDictionaryURI() + "]"));
            return;
        }

        codeGenerator.writeMetaContainers(builder, tc, action, action.getActionInfo(), alertCollector, null);
    }

    private String createSendCall(AMLTestCase tc, AMLAction action,
			Variable inputVariable) {
		StringBuilder sb = new StringBuilder(CAPACITY_128K);

        Variable settings = getVariable(ActionContext.class, "actionContext");
        String s = codeGenerator.createFillSettings(tc, action, action.getMessageTypeColumn(), settings, alertCollector);
        sb.append(s);

		Class<?> returnType = action.getActionInfo().getReturnType();
		String inputType = inputVariable != null ? inputVariable.getType().getCanonicalName() : null;

		boolean continueOnFailed = action.getContinueOnFailed() || this.continueOnFailed;

		String id = (action.getId() == null) ? "" : action.getId()+" ";
		String serviceName = action.hasServiceName() ? action.getServiceName() + " " : "";
		String messageType = (action.getMessageTypeColumn() == null) ? "" : " "+action.getMessageTypeColumn();
        String description = "null";

        if (inputVariable != null) {
            sb.append(MessageFormat.format(TAB2 + "{0} = ({1})stripFilter({0});" + EOL, inputVariable.getName(), inputType));
        }


		if (action.isAddToReport())
		{
            description = getMvelString(tc, action, action.getDescrption(), Column.Description, alertCollector, codeGenerator.getDefinedReferences(), dictionaryManager, actionManager, utilityManager);

			if (inputVariable != null)
			{
				sb.append(TAB2+REPORT_NAME+".createAction(\""
                        + id + "\", "

						+ "\""+ serviceName.trim() + "\", "
						+ "\""+ action.getActionURI() + "\", "
						+ "\""+ messageType.trim() + "\", " +

						description+", "+inputVariable.getName());
			}
			else
			{
				sb.append(TAB2+REPORT_NAME+".createAction(\""
                        + id + "\", "

						+ "\""+ serviceName.trim() + "\", "
						+ "\""+ action.getActionURI() + "\", "
						+ "\""+ messageType.trim() + "\", "

						+description+", null");
			}

            sb.append(",");
            sb.append(settings.getName());
            sb.append(".getCheckPoint(), ");

            sb.append(action.hasTag() ? enclose(toJavaString(action.getTag()), '"') : "null");

            sb.append(",");
            sb.append(action.getHash());

            sb.append(", ");
            String verificationsOrder = action.getVerificationsOrder().stream().map(StringUtil::enclose).collect(Collectors.joining(", "));
            sb.append("Arrays.asList(");
            sb.append(verificationsOrder);
            sb.append("), ");

            if(action.hasOutcome()) {
                sb.append(enclose(toJavaString(action.getOutcome())));
            } else {
                sb.append("null");
            }

            sb.append(");");
            sb.append(EOL);
		}

		ActionInfo actionInfo = action.getActionInfo();
        String mainCallArgs = format("%s.parse(\"%s\"), %s", SailfishURI.class.getSimpleName(), actionInfo.getURI(), settings.getName());
        Variable outputVariable = null;


		if (returnType != void.class) {
            outputVariable = getVariable(returnType, MESSAGE_PREFIX + returnType.getSimpleName());
			if (inputType != null)
			{
				sb.append(TAB2 + outputVariable.getName() + " = " +
						CodeGenerator_new.ACTION_MANAGER_CALL + "(" + mainCallArgs + ", " + inputVariable.getName() + ");" + EOL);
			}
			else {
				sb.append(TAB2 + outputVariable.getName() + " = " +
				        CodeGenerator_new.ACTION_MANAGER_CALL + "(" + mainCallArgs + ");"+EOL);
			}
			addReference(sb, action, inputVariable, outputVariable, TAB2);
		} else {
			if (inputType != null)
			{
				sb.append(TAB2 +
				        CodeGenerator_new.ACTION_MANAGER_CALL + "(" + mainCallArgs +  ", " + inputVariable.getName() + ");" + EOL);
			}
			else
			{
				sb.append(TAB2 +
				        CodeGenerator_new.ACTION_MANAGER_CALL + "(" + mainCallArgs + ");"+EOL);
			}
		}

        CodeGenerator_new.addExecutedActionReferences(sb, action, TAB2);

        if(action.hasOutcome()) {
            sb.append(TAB2);
            sb.append(CONTEXT_NAME);
            sb.append(".getOutcomeCollector().storeOutcome(new Outcome(\"");
            sb.append(action.getOutcomeGroup());
            sb.append("\", \"");
            sb.append(action.getOutcomeName());
            sb.append("\").setStatus(Status.PASSED));");
            sb.append(EOL);
        }

        if (action.isAddToReport()) {
            addActionToReport(action, sb, false, outputVariable, TAB2);
        }

        writeCatchForConditionallyPassed(tc, action, inputVariable, sb, id, serviceName, messageType, description);
        writeCatch(tc, action, sb, continueOnFailed, id, serviceName, messageType, description);

		return sb.toString();
	}

	private String createSendMessageDefinition(AMLTestCase tc, AMLAction action, Variable inputVariable, boolean isDirty)
	{
		SailfishURI dictionaryURI = action.getDictionaryURI();
		if (dictionaryURI == null) {
            alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), "Empty namespace for action: " + action.getActionURI()));
			return "";
		}
		IDictionaryStructure dictionary = getDictionary(dictionaryURI);

		if (dictionary == null) {
            alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(),
					"Can't find dictionary [" + dictionaryURI + "]"));
			return "";
		}

		String namespace = dictionary.getNamespace();

        IFieldStructure dm = dictionary.getMessages().get(action.getMessageTypeColumn());
		if (dm == null) {
            alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), "Dictionary " + action.getDictionaryURI() +
					" does not contain message "+action.getMessageTypeColumn()));
			return "";
		}

		List<String> subMessages = new ArrayList<>();
		StringBuilder sb = new StringBuilder(CAPACITY_128K);

		if (action.hasTemplate()) {
			String template = action.getTemplate();
            AMLAction templateAction = tc.findActionByRef(template);

            if(templateAction == null) {
                alertCollector.add(new Alert(action.getLine(), action.getReference(), Column.Template.getName(), "Cannot find template action with reference: " + template));
                return StringUtils.EMPTY;
            }

            if(templateAction.getGenerateStatus() != AMLGenerateStatus.GENERATED) {
                alertCollector.add(new Alert(action.getLine(), action.getReference(), Column.Template.getName(), "Template action is not generated: " + template));
                return StringUtils.EMPTY;
            }

            SailfishURI templateDictionary = templateAction.getDictionaryURI();

            if(templateDictionary == null || !templateDictionary.matches(dictionaryURI)) {
                alertCollector.add(new Alert(action.getLine(), action.getReference(), Column.Template.getName(),
                        format("Template's dictionary '%s' differs from this action's dictionary '%s'. Template: %s", templateDictionary, dictionaryURI, template)));
                return StringUtils.EMPTY;
            }

            String messageType = action.getMessageTypeColumn();
            String templateMessageType = templateAction.getMessageTypeColumn();

            if(!StringUtils.equals(messageType, templateMessageType)) {
                alertCollector.add(new Alert(action.getLine(), action.getReference(), Column.Template.getName(),
                        format("Template's message type '%s' differs from this action's message type '%s'. Template: %s", templateMessageType, messageType, template)));
                return StringUtils.EMPTY;
            }

            sb.append(TAB2);
            sb.append(inputVariable.getName());
            sb.append(" = ((");
            sb.append(IMessage.class.getCanonicalName());
            sb.append(")");
            sb.append(CodeGenerator_new.MAP_NAME);
            sb.append(".get(");
            sb.append(enclose(template, '"'));
            sb.append(")).cloneMessage();");
            sb.append(EOL);
        } else {
            sb.append(TAB2 + inputVariable.getName() + " = " + CONTEXT_NAME + ".getDictionaryManager().getMessageFactory(" + SailfishURI.class.getSimpleName() + ".parse(\"" + action.getDictionaryURI() + "\")).createMessage(\""
                    + action.getMessageTypeColumn() + "\", \"" + namespace + "\");" + EOL);
		}

		long line = action.getLine();

		for (Entry<String, Value> entry : action.getParameters().entrySet())
		{
		    String fieldName = entry.getKey();
		    Value value = entry.getValue();

            logger.trace("name = {} == {}", fieldName, value.getOrigValue());

            IFieldStructure fStruct = dm.getFields().get(fieldName);

			if (fStruct == null) {
			    if(isDirty) {
			        boolean isCollection = AMLLangUtil.isCollection(value.getValue());
			        fStruct = new FieldStructure(null, null, null, null, null, null, null, false, isCollection, false, null);
				} else {
                    alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), fieldName, "Message '" + dm.getName() + "' in namespace '" + dm.getNamespace() + "' does not contain '" + fieldName + "' field"));
			        continue;
			    }
			}

            if(fStruct.isSimple() && fStruct.isServiceName()) {
                writeServiceFieldDefinition(value, action, fieldName, fStruct, sb, inputVariable);
                continue;
            }

			String[] refs = AMLLangUtil.findReferences(value.getValue());

			// workaround for old-style complex field syntax
			if(fStruct.isComplex() && refs.length == 0) {
			    try {
			        refs = AMLLangUtil.getReferences(value.getValue());
                } catch (AMLException e) {
                    alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), fieldName, "Column '" + fieldName + "': " + e.getMessage()));
                    continue;
                }

			    StringBuilder newValue = new StringBuilder(CAPACITY_4K);

			    for(int i = 0; i < refs.length; i++) {
			        if(AMLLangUtil.isStaticVariableReference(refs[i])) {
                        newValue.append(refs[i]);
                    } else {
			            newValue.append(NewImplHelper.BEGIN_REFERENCE);
			            newValue.append(refs[i]);
			            newValue.append(NewImplHelper.END_REFERENCE);
                    }

			        if(i < refs.length - 1) {
			            newValue.append(",");
			        }
			    }

                value.setValue("[" + newValue + "]");
			}

			for(String ref : refs) {
			    if(!AMLLangUtil.isStaticVariableReference(ref)) {
	    	    	generateSubAction(tc, action, fStruct, fieldName, ref, subMessages, false, isDirty);
			    }
			}

			try {
                NewImplHelper.substituteReference(tc, action, alertCollector, fieldName, value, codeGenerator.getDefinedReferences(), dictionaryManager, actionManager, utilityManager);
            } catch(SailfishURIException e) {
                alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), fieldName, e.getMessage()));
                continue;
            }

            if(value.isJava()) {
                String strValue = value.isReference() ? generateEval(line, fieldName, value, TAB3) : value.getValue();

                sb.append(TAB2 + inputVariable.getName() + ".addField(\"" + fieldName + "\", " + strValue + ");//java send" + EOL);

                continue;
            }

			if (fStruct.isEnum())
			{
		    	boolean collection = fStruct.isCollection() || (isDirty && AMLLangUtil.isCollection(value.getValue()));

		    	if(collection) {
			        String strValue = value.getValue();

			        if(!AMLLangUtil.isCollection(strValue)) {
                        alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), fieldName, "Column '" + fieldName + "': Invalid collection format '" + value.getValue() + "'"));
                        continue;
                    }

			        String[] values = AMLLangUtil.getValues(strValue);
			        boolean valid = true;

			        for(int i = 0; i < values.length; i++) {
			            values[i] = getEnumValue(values[i].replaceAll("^\"|\"$", ""), fStruct, action.getLine(), action.getUID(), fieldName, isDirty);
			            valid = valid ? values[i] != null : false;
			        }

			        if(valid) {
			            value.setValue("[" + StringUtils.join(values, ",") + "]");
			            sb.append(TAB2+inputVariable.getName()+".addField(\""+fieldName+"\", ");
                        sb.append(generateEval(line, fieldName, value, TAB3));
                        sb.append(");//enum collection send"+EOL);
			        }
			    } else {
				    if(value.isReference()) {
				        sb.append(TAB2+inputVariable.getName()+".addField(\""+fieldName+"\", ");
	                    sb.append(generateEval(line, fieldName, value, TAB3));
	                    sb.append(");//reference enum send"+EOL);
				    } else {
                        String v = value.getOrigValue();
                        String enumValue = getEnumValue(v, fStruct, action.getLine(), action.getUID(), fieldName, isDirty);

                        action.getSetters().add(new Pair<>(fieldName, "Object o = " + enumValue));

                        if(enumValue != null) {
                            sb.append(TAB2+inputVariable.getName()+".addField(\""+fieldName+"\", ");
                            sb.append(enumValue);
                            sb.append(");//enum send"+EOL);
                        }
				    }
			    }
			}
			else if (fStruct.isSimple())
			{
		    	boolean collection = fStruct.isCollection() || (isDirty && AMLLangUtil.isCollection(value.getValue()));

		    	if(collection) {
			        if(!AMLLangUtil.isCollection(value.getValue())) {
                        alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), fieldName, "Column '" + fieldName + "': Invalid collection format '" + value.getValue() + "'"));
			            continue;
			        }

					if(isDirty) {
                        value.setValue(format("(($ != null ? $.toString() : 'null') in %s)", value.getValue()));
			        }

			        sb.append(TAB2+inputVariable.getName()+".addField(\""+fieldName+"\", ");
                    sb.append(generateEval(line, fieldName, value, TAB3));
                    sb.append(");//simple collection send"+EOL);
			    } else {
			        if(value.isReference()) {
			            sb.append(TAB2+inputVariable.getName()+".addField(\""+fieldName+"\", ");
                        sb.append(generateEval(line, fieldName, value, TAB3));
                        sb.append(");//simple reference send"+EOL);
			        } else {
    					logger.trace("fStruct.getJavaType() = {}", fStruct.getJavaType());
						JavaType type = isDirty ? JavaType.JAVA_LANG_STRING : fStruct.getJavaType();
						String castValue = castValue(value.getOrigValue(), type, action.getLine(), action.getUID(), value.getFieldName());
                        action.getSetters().add(new Pair<>(fieldName, "Object o = " + castValue));
                        sb.append(TAB2+inputVariable.getName()+".addField(\""+fieldName+"\", "+castValue+");//simple send"+EOL);
			        }
			    }
			}
			else if (fStruct.isComplex())
			{
				if(!AMLLangUtil.isSubmessage(value.getValue())) {
                    alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), fieldName, "Column '" + fieldName + "': Invalid collection format '" + value.getValue() + "'"));
				    continue;
                }

				if (!fStruct.isCollection()) {
					if (value.getValue().split(",").length > 1) {
                        alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), fieldName, "Cannot set multiple values to field " + fStruct.getName()));
						continue;
					}

					value.setValue(value.getValue().substring(1, value.getValue().length() - 1));
				}

				sb.append(TAB2+inputVariable.getName()+".addField(\""+fieldName+"\", ");
                sb.append(generateEval(line, fieldName, value, TAB3));
                sb.append(");//complex send"+EOL);
			}
		}

		StringBuilder sb1 = new StringBuilder(CAPACITY_128K).append(EOL);

        for(String subMessage : subMessages) {
            sb1.append(subMessage);
        }

		sb1.append(sb);

		return sb1.toString();
	}

    private void writeServiceFieldDefinition(Value value, AMLAction action, String fieldName, IFieldStructure fStruct, StringBuilder sb, Variable inputVariable) {
        String valueString = value.getValue();

        if(valueString.startsWith(AMLLangConst.TAG_INTERPRET_AS_JAVA) || valueString.contains(AMLLangConst.BEGIN_FUNCTION) || valueString.contains(AMLLangConst.BEGIN_REFERENCE)) {
            alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), fieldName, "Service name field must contain a simple value or a service name reference: " + valueString));
            return;
        }

        if(fStruct.isCollection()) {
            if(!AMLLangUtil.isCollection(valueString)) {
                alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), fieldName, "Value is not a collection: " + valueString));
                return;
            }

            String[] elements = AMLLangUtil.getValues(valueString);

            for(int i = 0; i < elements.length; i++) {
                IService service = codeGenerator.resolveService(elements[i], action.getLine(), action.getUID(), fieldName);

                if(service == null) {
                    return;
                }

                elements[i] = enclose(service.getServiceName().getServiceName());
            }

            valueString = "Arrays.asList(" + StringUtils.join(elements, ", ") + ")";
        } else {
            if(AMLLangUtil.isCollection(valueString)) {
                alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), fieldName, "Value cannot be a collection: " + valueString));
                return;
            }

            IService service = codeGenerator.resolveService(valueString, action.getLine(), action.getUID(), fieldName);

            if(service == null) {
                return;
            }

            valueString = enclose(service.getServiceName().getServiceName());
        }

        sb.append(TAB2);
        sb.append(inputVariable.getName());
        sb.append(".addField(");
        sb.append(enclose(fieldName));
        sb.append(", ");
        sb.append(valueString);
        sb.append(");//service name field");
        sb.append(EOL);
    }

	private boolean isValueTypeOf(String v, JavaType valueType) {
		//FIXME: implement
		return true;
	}

	boolean isEmptyString(String string) {
        return string == null || "".equals(string);
	}

	private String getEnumValue(String value, IFieldStructure enumField, long line, long uid, String column, boolean isDirty) {
	    JavaType type = isDirty ? JavaType.JAVA_LANG_STRING : enumField.getJavaType();

	    for(String e : enumField.getValues().keySet()) {
	        if(e.equals(value) || enumField.getValues().get(e).getValue().equals(value)) {
	            IAttributeStructure as = enumField.getValues().get(e);
	            return castValue(isDirty ? as.getValue() : as.getCastValue(), type, line, uid, column);
	        }
	    }

	    if(isValueTypeOf(value, type)) {
	        return castValue(value, type, line, uid, column);
	    } else {
	        alertCollector.add(new Alert(line, uid, null, column, "Cannot set enum field value to: " + value));
	    }

	    return null;
	}

	private String castValue(Object object, JavaType valueType, long line, long uid, String column)
	{
		switch (valueType)
		{
        case JAVA_LANG_BOOLEAN:
            String value = object.toString().toLowerCase();
            if("n".equals(value) || "false".equals(value)) {
				return "false";
            } else if("y".equals(value) || "true".equals(value)) {
				return "true";
			}
            alertCollector.add(new Alert(line, uid, null, column, "Unknown boolean value: " + value));
            return null;
        case JAVA_LANG_BYTE:
            return "(byte)(" + object + ")";
		case JAVA_LANG_CHARACTER:		return "'"+object+"'";
        case JAVA_TIME_LOCAL_DATE_TIME:
        case JAVA_TIME_LOCAL_DATE:
        case JAVA_TIME_LOCAL_TIME:
            alertCollector.add(new Alert(line, uid, null, column, "Unknown date/time value: " + object));
            return null;
		case JAVA_LANG_SHORT:		return "(short)("+object+")";
        case JAVA_LANG_INTEGER:		return "(int)("+object+")";
        case JAVA_LANG_LONG:
            try {
                String string = object.toString();
                Long.valueOf(StringUtils.removeEndIgnoreCase(string, "L"));
                return StringUtils.appendIfMissingIgnoreCase(string, "L");
            } catch(NumberFormatException e) {
                alertCollector.add(new Alert(line, uid, null, column, "Invalid long value: " + object));
                return null;
            }
        case JAVA_LANG_FLOAT:
            return "(float)(" + object + ")";
		case JAVA_LANG_DOUBLE:	return "(double)("+object+")";
		case JAVA_MATH_BIG_DECIMAL:	return "new java.math.BigDecimal(\""+object+"\")";
        case JAVA_LANG_STRING:
            return "\"" + toJavaString(object.toString()) + "\"";
		default: return null;
		}
	}

    private static String castReceiveValue(Object object, JavaType valueType, AlertCollector alertCollector, long line, long uid, String column)
	{
		switch (valueType)
		{
        case JAVA_LANG_BOOLEAN:
            String value = object.toString().toLowerCase();
            if("n".equals(value) || "false".equals(value)) {
				return "false";
            } else if("y".equals(value) || "true".equals(value)) {
				return "true";
			}
            alertCollector.add(new Alert(line, uid, null, column, "Unknown boolean value: " + value));
            return null;
        case JAVA_LANG_BYTE:
            return "(byte)(" + object + ")";
		case JAVA_LANG_CHARACTER:		return "'"+object+"'.charAt(0)";
//		case CHAR:		return object.toString();
        case JAVA_TIME_LOCAL_DATE_TIME:
        case JAVA_TIME_LOCAL_DATE:
        case JAVA_TIME_LOCAL_TIME:
            alertCollector.add(new Alert(line, uid, null, column, "Unknown date/time value: " + object));
            return null;
		case JAVA_LANG_SHORT:		return "(short)("+object+")";
		case JAVA_LANG_INTEGER:		return "(int)("+object+")";
        case JAVA_LANG_LONG:
            try {
                String string = object.toString();
                Long.valueOf(StringUtils.removeEndIgnoreCase(string, "L"));
                return StringUtils.appendIfMissingIgnoreCase(string, "L");
            } catch(NumberFormatException e) {
                alertCollector.add(new Alert(line, uid, null, column, "Invalid long value: " + object));
                return null;
            }
        case JAVA_LANG_FLOAT:
            return "(float)(" + object + ")";
		case JAVA_LANG_DOUBLE:	return "(double)("+object+")";
        case JAVA_MATH_BIG_DECIMAL:
            try {
                new BigDecimal(object.toString());
                return "new BigDecimal(\"" + object + "\")";
            } catch(NumberFormatException e) {
                return "" + object + "";
            }
        case JAVA_LANG_STRING:
            return "\"" + toJavaString(object.toString()) + "\"";
		default: return null;
		}
	}

	private String castReceiveEnum(Object object, JavaType valueType, boolean isSimpleExpression, long line, long uid, String column)
	{
		if (isSimpleExpression) {
            return valueType == JavaType.JAVA_LANG_BOOLEAN ? object.toString().toLowerCase() : String.valueOf(object);
        }
		return castReceiveValue(object, valueType, alertCollector, line, uid, column);
	}

	private Variable getVariable(Class<?> type, String varNameOrig)
	{
		String varName = varNameOrig;

		Variable var = new Variable(varName, type);
		// check if variable with same name and type already exists
        if(variables.contains(var))
		{
			return var;
		}

		// check if variable with same name and different type already exists

		int i=1;
		boolean found = true;
		while (found)
		{
			found = false;
            for(Variable v : variables)
			{
				if (v.getName().equals(varName))
				{
					found = true;
                    varName = varNameOrig + i++;
					break;
				}
			}
			if (found == false)
			{
				if (type == null) {
					throw new NullPointerException("Variable type is null");
				}
				var = new Variable(varName, type);
                variables.add(var);
				return var;
			}
		}
		// should not happen
		logger.error("This should not happen");
		return null;
	}

	void addReferenceToFilter(StringBuilder sb, AMLAction action, Variable inputVariable)
	{
		if (!"".equals(action.getReferenceToFilter()))
		{
		    if (action.getActionInfo() == null || (action.getActionInfo() != null && !void.class.equals(action.getActionInfo().getReturnType())))
            {
				if (inputVariable != null)
				{
				    String mapName = action.isStaticAction() ? STATIC_MAP_NAME : MAP_NAME;
					sb.append(TAB2+mapName+".put(\""+action.getReferenceToFilter()+"\", "+inputVariable.getName()+");"+EOL);
				}
			}
		}
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
					sb.append(tab + mapName + ".put(\"" + action.getReference() + "\", " + varName + ");" + EOL);
                    codeGenerator.addDefinedReferences(action.getReference());

                    if(!action.getIncludeBlockReference().isEmpty()) {
                        String ref = action.getIncludeBlockReference();
                        String blockMapName = getVariable(HashMap.class, MESSAGE_PREFIX + ref).getName();

						sb.append(tab + blockMapName + " = (" + HashMap.class.getCanonicalName() + ")" + MAP_NAME
								+ ".get(\"" + ref + "\");" + EOL);
						sb.append(
								tab + blockMapName + ".put(\"" + action.getReference() + "\", " + varName + ");" + EOL);
                    }
			    }
			}
		}
	}

	// XXX: writeReceiveMessage
	public String writeReceiveMessage(AMLTestCase tc, AMLAction action,
			List<Variable> variables2) {

		this.variables = variables2;
		if (action.getGenerateStatus() == AMLGenerateStatus.GENERATED) {
			return null;
		}

		if (action.getGenerateStatus() == AMLGenerateStatus.GENERATING) {
            alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), "Recursion detected"));
			return null;
		}

		action.setGenerateStatus(AMLGenerateStatus.GENERATING);

		StringBuilder sb = new StringBuilder(CAPACITY_128K);
		Variable inputVariable = null;
//		Variable outputVariable = null;
		Class<?> type = action.getActionInfo().getMessageType();

        if(action.hasServiceName()) {
            IService service = codeGenerator.resolveService(action.getServiceName(), action.getLine(), action.getUID(), Column.ServiceName.getName());

            if(service != null) {
                action.setServiceName(service.getServiceName().getServiceName());
                codeGenerator.resolveDictionary(action, service);
            }
        }

		if (type != null)
		{
			if (!action.hasDictionaryURI()) {
                alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), Column.Dictionary.getName(), "Dictionary is not specified"));
				return null;
			}

			sb.append(TAB2+"try {"+EOL);

			inputVariable = getVariable(type, MESSAGE_PREFIX+action.getMessageTypeColumn());
			sb.append(createReceiveMessageDefinition(tc, action, inputVariable));
			addReferenceToFilter(sb, action, inputVariable);
			action.setGenerateStatus(AMLGenerateStatus.GENERATED);
            writeFillComparisonErrorsDefinition(sb, tc, action, false);
			sb.append(createReceiveCall(tc,action,inputVariable));
		}

		return sb.toString(); // TODO: to be done
	}

	private Object createReceiveCall(AMLTestCase tc, AMLAction action,
			Variable inputVariable) {
		StringBuilder sb = new StringBuilder(CAPACITY_128K);

        Variable settings = getVariable(ActionContext.class, "actionContext");
        String s = codeGenerator.createFillSettings(tc, action, action.getMessageTypeColumn(), settings, alertCollector);
        sb.append(s);

		Class<?> returnType = action.getActionInfo().getReturnType();

		boolean continueOnFailed = action.getContinueOnFailed() || this.continueOnFailed;

		String id = (action.getId() == null) ? "" : action.getId()+" ";
		String serviceName = action.hasServiceName() ? action.getServiceName() + " " : "";
		String messageType = (action.getMessageTypeColumn() == null) ? "" : " "+action.getMessageTypeColumn();
        String description = "null";

		if (action.isAddToReport())
		{
            description = getMvelString(tc, action, action.getDescrption(), Column.Description, alertCollector, codeGenerator.getDefinedReferences(), dictionaryManager, actionManager, utilityManager);

            sb.append(TAB2 + REPORT_NAME + ".createAction(\""
                    + id + "\", "

					+ "\""+ serviceName.trim() + "\", "
					+ "\""+ action.getActionURI() + "\", "
					+ "\""+ messageType.trim() + "\", " +

					description+", "+inputVariable.getName());

            sb.append(", ");
            sb.append(settings.getName());
            sb.append(".getCheckPoint(), ");

            sb.append(action.hasTag() ? enclose(toJavaString(action.getTag()), '"') : "null");

            sb.append(",");
            sb.append(action.getHash());

            sb.append(", ");
            String verificationsOrder = action.getVerificationsOrder().stream().map(StringUtil::enclose).collect(Collectors.joining(", "));
            sb.append("Arrays.asList(");
            sb.append(verificationsOrder);
            sb.append("), ");

            if(action.hasOutcome()) {
                sb.append(enclose(toJavaString(action.getOutcome())));
            } else {
                sb.append("null");
            }

            sb.append(");");
            sb.append(EOL);
		}

        ActionInfo actionInfo = action.getActionInfo();
        String callArgs = format("%s.parse(\"%s\"), %s, %s", SailfishURI.class.getSimpleName(), actionInfo.getURI(), settings.getName(), inputVariable.getName());
        Variable outputVariable = null;

		if (returnType != void.class) {
            outputVariable = getVariable(returnType, MESSAGE_PREFIX + returnType.getSimpleName());
			sb.append(TAB2 + outputVariable.getName() + " = " +
					CodeGenerator_new.ACTION_MANAGER_CALL + "(" + callArgs + ");"+EOL);
            sb.append(TAB2 + CONTEXT_NAME+".getReceivedMessages().add("+outputVariable.getName()+");"+EOL);
			addReference(sb, action, inputVariable, outputVariable, TAB2);
        } else {
            sb.append(TAB2 +
                    CodeGenerator_new.ACTION_MANAGER_CALL + "(" + callArgs + ");"+EOL);

		}

        CodeGenerator_new.addExecutedActionReferences(sb, action, TAB2);

        if(action.hasOutcome()) {
            sb.append(TAB2);
            sb.append(CONTEXT_NAME);
            sb.append(".getOutcomeCollector().storeOutcome(new Outcome(\"");
            sb.append(action.getOutcomeGroup());
            sb.append("\", \"");
            sb.append(action.getOutcomeName());
            sb.append("\").setStatus(Status.PASSED));");
            sb.append(EOL);
        }

		if (action.isAddToReport()) {
            addActionToReport(action, sb, false, outputVariable, TAB2);
		}

        writeCatchForConditionallyPassed(tc, action, inputVariable, sb, id, serviceName, messageType, description);
        writeCatch(tc, action, sb, continueOnFailed, id, serviceName, messageType, description);

        return sb.toString();
    }

    private void writeCatchForConditionallyPassed(AMLTestCase tc, AMLAction action, Variable inputVariable, StringBuilder sb, String id, String serviceName, String messageType, String description) {
        Variable variable = new Variable("containedMessage", Object.class);

        sb.append(TAB2 + "} catch (KnownBugException e) {" + EOL);
        sb.append(TAB3 + LOGGER_NAME + ".warn(e.getMessage(), e);" + EOL);
        sb.append(TAB3 + variable.getType().getSimpleName() + " ");
        sb.append(variable.getName() + " = null;" + EOL);
        sb.append(TAB3 + "if(e instanceof MessageKnownBugException) {" + EOL);
        sb.append(TAB4 + variable.getName() + " = ((MessageKnownBugException)e).getContainedMessage();" + EOL);
        sb.append(TAB4 + CONTEXT_NAME + ".getReceivedMessages().add(");
        sb.append(variable.getName());
        sb.append(");" + EOL);

        addReference(sb, action, inputVariable, variable, TAB4);
        sb.append(TAB3 + "}" + EOL);
        CodeGenerator_new.addExecutedActionReferences(sb, action, TAB4);

        if(action.hasOutcome()) {
            sb.append(TAB2);
            sb.append(CONTEXT_NAME);
            sb.append(".getOutcomeCollector().storeOutcome(new Outcome(\"");
            sb.append(action.getOutcomeGroup());
            sb.append("\", \"");
            sb.append(action.getOutcomeName());
            sb.append("\").setStatus(Status.CONDITIONALLY_PASSED));");
            sb.append(EOL);
        }

        addConditionallyPassedToReport(tc, action, sb, id, serviceName, messageType, description, variable);
    }

    private void writeCatch(AMLTestCase tc, AMLAction action, StringBuilder sb, boolean continueOnFailed, String id, String serviceName, String messageType, String description) {
		sb.append(TAB2+"} catch (Exception e) {"+EOL);
		sb.append(TAB3+LOGGER_NAME+".warn(e.getMessage(), e);"+EOL);
		sb.append(TAB3+CONTEXT_NAME+".setInterrupt(e instanceof InterruptedException);"+EOL);

        if(action.hasOutcome()) {
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

        addFailedActionToReport(tc, action, sb, id, serviceName, messageType, description);

        if(continueOnFailed || action.hasOutcome()) {
            sb.append(TAB3+"if (e instanceof InterruptedException) {"+EOL);
			sb.append(TAB4+"throw e;"+EOL);
            sb.append(TAB3+"}"+EOL);
		} else {
		    sb.append(TAB3+"throw e;"+EOL);
        }

		sb.append(TAB2+"}"+EOL);
    }

    protected static void writeCreateTestCase(AMLTestCase tc, StringBuilder sb) {
        sb.append(TAB3);
        sb.append("if(!");
        sb.append(REPORT_NAME);
        sb.append(".isTestCaseCreated()) {");
        sb.append(EOL);

        sb.append(TAB4);
        sb.append(REPORT_NAME);
        sb.append(".createTestCase(");

        sb.append(tc.hasReference() ? enclose(tc.getReference()) : "null");

        sb.append(", ");

        String description = tc.getDescription();

        if(StringUtils.isNotBlank(description)) {
            sb.append(enclose(toJavaString(description)));
            sb.append(", ");
        } else {
            sb.append("null, ");
        }

        sb.append(tc.getExecOrder());
        sb.append(", ");

        sb.append(tc.getMatrixOrder());
        sb.append(", ");

        String id = tc.getId();

        if(StringUtils.isNotBlank(id)) {
            sb.append(enclose(toJavaString(id)));
            sb.append(", ");
        } else {
            sb.append("null, ");
        }

        sb.append(tc.getHash());
        sb.append(", ");

        sb.append(AMLBlockType.class.getSimpleName());
        sb.append('.');
        sb.append(tc.getBlockType().name());
        sb.append(", ");

        sb.append("new HashSet<>(Arrays.asList(new String[]" + tc.getTagsAsString() + "))");

        sb.append(");");
        sb.append(EOL);

        sb.append(TAB3);
        sb.append("}");
        sb.append(EOL);
	}

    private void writeCreateAction(AMLAction action, StringBuilder sb, String id, String serviceName, String messageType, String description) {
        String verificationsOrder = action.getVerificationsOrder().stream().map(StringUtil::enclose).collect(Collectors.joining(", "));

        sb.append(TAB3 + "if (!" + REPORT_NAME + ".isActionCreated()) {" + EOL);
        sb.append(TAB4 + REPORT_NAME + ".createAction(\"" + id + "\", "

                + "\"" + serviceName.trim() + "\", "
                + "\"" + action.getActionURI() + "\", "
                + "\"" + messageType.trim() + "\", "

                + description + " , null, null, "
                + (action.hasTag() ? enclose(toJavaString(action.getTag()), '"') : "null") + ", "
                + action.getHash() + ", "
                + "Arrays.asList( " + verificationsOrder + "), "
                + (action.hasOutcome() ? enclose(toJavaString(action.getOutcome())) : "null")
                +");" + EOL);
        sb.append(TAB3 + "}" + EOL);
    }

    protected static void addActionToReport(AMLAction action, StringBuilder sb, boolean coniditionallyPassed, Variable outputVariable, String indent) {
        String output = outputVariable != null ? outputVariable.getName() : null;

        if(action.getOutcomeGroup() == null) {
            if(coniditionallyPassed) {
                sb.append(indent + CONTEXT_NAME + ".setConditionallyPassed(true);" + EOL);
                sb.append(indent + CONTEXT_NAME + ".getKnownBugs().addAll(e.getPotentialDescriptions());" + EOL);
            }

            sb.append(indent + REPORT_NAME + ".closeAction(new StatusDescription(");
            sb.append(coniditionallyPassed ? "StatusType.CONDITIONALLY_PASSED" : "StatusType.PASSED");
            sb.append(", ");
            sb.append(coniditionallyPassed ? "e.getMessage(), e.getPotentialDescriptions()" : "\"\"");
            sb.append("), ");
            sb.append(output);
            sb.append(");");
            sb.append(EOL);
        } else {
            sb.append(indent + "Status status = " + CONTEXT_NAME + ".getOutcomeStatus(\"" + action.getOutcomeGroup() + "\", \"" + action.getOutcomeName() + "\");" + EOL);
            sb.append(indent + REPORT_NAME + ".closeAction(new StatusDescription(Status.getStatusType(status), \"Outcome \" + status, false), " + output + ");" + EOL);
        }
    }

    private void addFailedActionToReport(AMLTestCase tc, AMLAction action, StringBuilder sb, String id, String serviceName, String messageType, String description) {
        writeCreateTestCase(tc, sb);
        writeCreateAction(action, sb, id, serviceName, messageType, description);

        sb.append(TAB3 + REPORT_NAME + ".closeAction(new StatusDescription(StatusType.FAILED, e.getMessage(), e");

        if(action.hasOutcome()) {
            sb.append(", false");
        }

        sb.append("), null);");
        sb.append(EOL);
    }

    private void addConditionallyPassedToReport(AMLTestCase tc, AMLAction action, StringBuilder sb, String id, String serviceName, String messageType, String description, Variable outputVariable) {
        writeCreateTestCase(tc, sb);
        writeCreateAction(action, sb, id, serviceName, messageType, description);
        addActionToReport(action, sb, true, outputVariable, TAB3);
    }

    private String createReceiveMessageDefinition(AMLTestCase tc, AMLAction action, Variable inputVariable)
	{
		IDictionaryStructure dictionary = getDictionary(action.getDictionaryURI());

		if (dictionary == null) {
            alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(),
					"Can't find dictionary [" + action.getDictionaryURI() + "]"));
			return "";
		}

		String namespace = dictionary.getNamespace();

        IFieldStructure dm = dictionary.getMessages().get(action.getMessageTypeColumn());
		if (dm == null) {
            alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), "Dictionary " + action.getDictionaryURI() +
					" does not contain message "+action.getMessageTypeColumn()));
			return "";
		}
		List<String> subMessages = new ArrayList<>();

		StringBuilder sb = new StringBuilder(CAPACITY_4K);
		sb.append(TAB2+inputVariable.getName()+" = " + CONTEXT_NAME + ".getDictionaryManager().getMessageFactory(" + SailfishURI.class.getSimpleName() + ".parse(\"" + action.getDictionaryURI() + "\")).createMessage(\""+action.getMessageTypeColumn()+"\", \""+ namespace +"\");"+EOL);

		Boolean isAdmin = null;
		for (String attributeName : dm.getAttributes().keySet()) {
            if ("IsAdmin".equalsIgnoreCase(attributeName)) { //Attribute value may be null
                isAdmin = getAttributeValue(dm, attributeName);
                break;
            }
        }

		if (isAdmin == null) {
			isAdmin = Boolean.FALSE;
		}

		sb.append(TAB2+inputVariable.getName()+".getMetaData().setAdmin(" + isAdmin + ");"+EOL);

		long line = action.getLine();

		for (Entry<String, Value> entry : action.getParameters().entrySet())
		{
		    String fieldName = entry.getKey();
		    Value value = entry.getValue();

            logger.trace("name = {} == {}", fieldName, value.getOrigValue());

            IFieldStructure fStruct = dm.getFields().get(fieldName);

			if (fStruct == null) {
                alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), fieldName, "Message '" + dm.getName() + "' in namespace '" + dm.getNamespace() + "' does not contain '" + fieldName + "' field"));
			} else {
                if(fStruct.isSimple() && fStruct.isServiceName()) {
                    writeServiceFieldDefinition(value, action, fieldName, fStruct, sb, inputVariable);
                    continue;
                }

				String[] refs = AMLLangUtil.findReferences(value.getValue());

                // workaround for old-style complex field syntax
                if(fStruct.isComplex() && refs.length == 0 &&
                        !(value.getValue().equals(CONV_VALUE_MISSING) || value.getValue().equals(CONV_VALUE_PRESENT) || isFunction(value.getValue()))) {
                    try {
                        refs = AMLLangUtil.getReferences(value.getValue());
                    } catch (AMLException e) {
                        alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), fieldName, "Column '" + fieldName + "': " + e.getMessage()));
                        continue;
                    }

                    StringBuilder newValue = new StringBuilder(CAPACITY_4K);

                    for(int i = 0; i < refs.length; i++) {
                        if(AMLLangUtil.isStaticVariableReference(refs[i])) {
                            newValue.append(refs[i]);
                        } else {
                            newValue.append(NewImplHelper.BEGIN_REFERENCE);
                            newValue.append(refs[i]);
                            newValue.append(NewImplHelper.END_REFERENCE);
                        }

                        if(i < refs.length - 1) {
                            newValue.append(",");
                        }
                    }

                    value.setValue("[" + newValue + "]");
                }

                for(String ref : refs) {
                    if(!AMLLangUtil.isStaticVariableReference(ref)) {
                        generateSubAction(tc, action, fStruct, fieldName, ref, subMessages, true, false);
                    }
                }

                try {
                    NewImplHelper.substituteReference(tc, action, alertCollector, fieldName, value, codeGenerator.getDefinedReferences(), dictionaryManager, actionManager, utilityManager);
                } catch(SailfishURIException e) {
                    alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), fieldName, e.getMessage()));
                    continue;
                }

                if(value.isJava()) {
                    String strValue = value.isReference() ? generateEval(line, fieldName, value, TAB3) : value.getValue();

                    sb.append(TAB2 + inputVariable.getName() + ".addField(\"" + fieldName + "\", " + strValue + ");//java receive" + EOL);

                    continue;
                }

				if (fStruct.isEnum())
				{
				    if(fStruct.isCollection()) {
				        String strValue = value.getValue();

				        if(strValue.equals(CONV_VALUE_MISSING) || strValue.equals(CONV_VALUE_PRESENT)) {
                            sb.append(TAB2 + inputVariable.getName() + ".addField(\"" + fieldName + "\", " + createFilterExpression(fStruct, strValue, action.getLine(), action.getUID(), value.getFieldName(), alertCollector) + ");" + EOL);
	                        continue;
	                    }

                        if(!AMLLangUtil.isCollection(strValue)) {
                            alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), fieldName, "Column '" + fieldName + "': Invalid collection format '" + value.getValue() + "'"));
                            continue;
                        }

                        String[] values = AMLLangUtil.getValues(strValue);
                        boolean valid = true;

                        for(int i = 0; i < values.length; i++) {
                            values[i] = getEnumValue(values[i].replaceAll("^\"|\"$", ""), fStruct, action.getLine(), action.getUID(), fieldName, false);
                            valid = valid ? values[i] != null : false;
                        }

                        if(valid) {
                            value.setValue("[" + StringUtils.join(values, ",") + "]");
                            sb.append(TAB2+inputVariable.getName()+".addField(\""+fieldName+"\", ");
                            sb.append(generateEval(line, fieldName, value, TAB3));
                            sb.append(");//enum collection receive"+EOL);
                        }
				    } else {
    				    if(value.isReference()) {
    				        sb.append(TAB2+inputVariable.getName()+".addField(\""+fieldName+"\", ");
    	                    sb.append(generateFilter(line, fieldName, value, TAB3));
    	                    sb.append(");//reference enum receive"+EOL);
    				    } else {
        					String v = prepareEnum(value.getOrigValue(), fStruct, action.getLine(), action.getUID(), value.getFieldName());
                            String filterExpression = createFilterExpression(fStruct, v, action.getLine(), action.getUID(), value.getFieldName(), alertCollector);

        					sb.append(TAB2+inputVariable.getName()+".addField(\""+fieldName+"\", "+filterExpression+");//enum receive"+EOL);
    				    }
				    }
				}
				else if (fStruct.isSimple())
				{
				    if(fStruct.isCollection()) {
				        if(value.getValue().equals(CONV_VALUE_MISSING) || value.getValue().equals(CONV_VALUE_PRESENT)) {
                            sb.append(TAB2 + inputVariable.getName() + ".addField(\"" + fieldName + "\", " + createFilterExpression(fStruct, value.getValue(), action.getLine(), action.getUID(), value.getFieldName(), alertCollector) + ");"
                                    + EOL);
	                        continue;
	                    }

				        if(!AMLLangUtil.isCollection(value.getValue())) {
                            alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), fieldName, "Column '" + fieldName + "': Invalid collection format '" + value.getValue() + "'"));
				            continue;
                        }

                        sb.append(TAB2+inputVariable.getName()+".addField(\""+fieldName+"\", ");
                        sb.append(generateEval(line, fieldName, value, TAB3));
                        sb.append(");//simple collection receive"+EOL);
				    } else {
				        if(value.isReference()) {
				            sb.append(TAB2+inputVariable.getName()+".addField(\""+fieldName+"\", ");
	                        sb.append(generateFilter(line, fieldName, value, TAB3));
	                        sb.append(");//simple reference receive"+EOL);
				        } else {
                            String filterExpression = createFilterExpression(fStruct, value.getOrigValue(), action.getLine(), action.getUID(), value.getFieldName(), alertCollector);

				            sb.append(TAB2+inputVariable.getName()+".addField(\""+fieldName+"\", "+filterExpression+");//simple receive"+EOL);
				        }
				    }
				}
				else if (fStruct.isComplex())
				{
                    if(isFunction(value.getOrigValue())) {
                        sb.append(TAB2 + inputVariable.getName() + ".addField(\"" + fieldName + "\", " + generateFilter(action.getLine(), value.getFieldName(), value, TAB3) + ");" + EOL);
                        continue;
                    }

                    if(value.getValue().equals(CONV_VALUE_MISSING) || value.getValue().equals(CONV_VALUE_PRESENT)) {
                        sb.append(
                                TAB2 + inputVariable.getName() + ".addField(\"" + fieldName + "\", " + createFilterExpression(fStruct, value.getValue(), action.getLine(), action.getUID(), value.getFieldName(), alertCollector) + ");" + EOL);
                        continue;
                    }

                    if(!AMLLangUtil.isSubmessage(value.getValue())) {
                        alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), fieldName, "Column '" + fieldName + "': Invalid collection format"));
                        continue;
                    }

                    if (!fStruct.isCollection()) {
                        if (value.getValue().split(",").length > 1) {
                            alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), fieldName, "Cannot set multiple values to field " + fStruct.getName()));
                            continue;
                        }

                        value.setValue(value.getValue().substring(1, value.getValue().length() - 1));
                    }

                    sb.append(TAB2+inputVariable.getName()+".addField(\""+fieldName+"\", ");
                    sb.append(generateEval(line, fieldName, value, TAB3));
                    sb.append(");//complex receive"+EOL);
				}
			}
		}

		StringBuilder sb1 = new StringBuilder(CAPACITY_128K);

		sb1.append(EOL);

		for (String subMessage : subMessages)
		{
			sb1.append(subMessage);
		}

		sb1.append(sb);

		return sb1.toString();
	}

	private void generateSubAction(AMLTestCase tc, AMLAction action, IFieldStructure fieldType, String fieldName, String reference, List<String> subMessages, boolean isReceive, boolean isDirty) {
	    String[] refSplit = reference.split("[:.]");
	    String refName = refSplit[0];

	    AMLAction subAction = tc.findActionByRef(refName);

        if(subAction == null || action.equals(subAction)) {
            alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), fieldName, format(
                    "Column '%s': Reference '%s' is not defined in matrix", fieldName, refName)));

            return;
        }

        if(subAction.getGenerateStatus() != AMLGenerateStatus.GENERATED) {
            if(fieldType.isComplex()) {
                SailfishURI dictionary = subAction.getDictionaryURI();
                SailfishURI actionDictionary = action.getDictionaryURI();

                if(dictionary != null && !dictionary.matches(actionDictionary)) {
                    alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), fieldName, format(
                            "Subaction dictionary '%s' differs from action dictionary '%s'", dictionary, actionDictionary)));

                    return;
                }

                subAction.setDictionaryURI(actionDictionary);

                String subMsgType = subAction.getMessageTypeColumn();
                String msgType = fieldType.getReferenceName();

                if(StringUtils.isEmpty(subMsgType)) {
                    subAction.setMessageTypeColumn(msgType);
                } else if(!subMsgType.equals(msgType)) {
                    alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), fieldName, format(
                            "Subaction message type '%s' differs from action message type '%s'", subMsgType, msgType)));

                    return;
                }
            } else {
                alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), fieldName, format(
                    "Subaction must predefined to use references to it's fields.")));
                return;
            }
        }


        if (subAction.getActionInfo() == null) {
            ActionInfo actionInfo = action.getActionInfo().clone();
            subAction.setActionInfo(actionInfo);
            actionInfo.setReturnType(actionInfo.getMessageType()); // for actions with void return type
        }

        Class<?> returnType = subAction.getActionInfo().getReturnType();

        if(refSplit.length > 1 && returnType != HashMap.class) {
            String subField = refSplit[1];

            SailfishURI dictURI = subAction.getDictionaryURI();
            String msgType = subAction.getMessageTypeColumn();

            IMessageStructure msgStruct = getMessageStructure(dictURI, msgType);

            if(msgStruct == null) {
                alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), fieldName, format(
                        "Column '%s': Reference to unknown message('%s')/dictionary('%s') in reference '%s'", fieldName, msgType, dictURI, reference)));
                return;
            }

            if(msgStruct.getFields().get(subField) == null) {
                alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), fieldName, format(
                        "Column '%s': Reference to unknown column '%s' in reference '%s'", fieldName, subField, reference)));
                return;
            }
        }

        if(refSplit.length == 1 && fieldType.isComplex() && returnType != IMessage.class) {
            alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), fieldName, format("Incompatible value types: cannot use %s instead of %s", returnType.getSimpleName(), IMessage.class.getSimpleName())));
            return;
        }

        if(fieldType.isComplex()) {
            action.addChildAction(fieldName, subAction);
        }

	    if (subAction.getGenerateStatus() != AMLGenerateStatus.GENERATED) {

	    	if (subAction.getGenerateStatus() == AMLGenerateStatus.GENERATING) {
                alertCollector.add(new Alert(subAction.getLine(), action.getUID(), subAction.getReference(), "Recursion detected"));
				return;
			}

	    	subAction.setGenerateStatus(AMLGenerateStatus.GENERATING);
	    	subAction.addGenerationSteps(action.getGenerationPath(), action.getReference());

	        StringBuilder sb = new StringBuilder(CAPACITY_128K);
	        Class<?> type = subAction.getActionInfo().getMessageType();

	        if (type == null) {
	            subAction.getActionInfo().setMessageType(type = action.getActionInfo().getMessageType());
	            subAction.getActionInfo().setReturnType(type);
	        }

	        Variable subMsgVar = getVariable(type, MESSAGE_PREFIX + subAction.getMessageTypeColumn());

	        if(isReceive) {
	            sb.append(createReceiveMessageDefinition(tc, subAction, subMsgVar) + EOL);
	        } else {
	            sb.append(createSendMessageDefinition(tc, subAction, subMsgVar, isDirty) + EOL);
	        }

	        subAction.setGenerateStatus(AMLGenerateStatus.GENERATED);
			addReference(sb, subAction, subMsgVar, null, TAB2);
	        addReferenceToFilter(sb, subAction, subMsgVar);
            CodeGenerator_new.addExecutedActionReferences(sb, subAction, TAB2);

	        subMessages.add(sb.toString());
	    }
	}

    public static String getMvelString(AMLTestCase tc, AMLAction action, String input, Column column, AlertCollector alertCollector, Set<String> definedReferences, IDictionaryManager dictionaryManager, IActionManager actionManager, IUtilityManager utilityManager) {
	    if(input.contains(NewImplHelper.BEGIN_FUNCTION) ||
	       input.contains(NewImplHelper.BEGIN_REFERENCE) ||
	       input.contains(NewImplHelper.BEGIN_STATIC)) {

	        StringBuilder sb = new StringBuilder(CAPACITY_4K);
            Value inputValue = new Value(input);

            try {
                NewImplHelper.substituteReference(tc, action, alertCollector, column.getName(), inputValue, definedReferences, dictionaryManager, actionManager, utilityManager);
            } catch(SailfishURIException e) {
                alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column.getName(), e.getMessage()));
                return null;
            }

            sb.append(generateEval(action.getLine(), column.getName(), inputValue, TAB3));
            sb.append(".toString()");

            return sb.toString();
        } else {
            return "\"" + toJavaString(input) + "\"";
        }
	}

	public static String generateEval(long line, String column, Value value, String indent) {
        StringBuilder sb = new StringBuilder(CAPACITY_4K);

        sb.append("eval(");
        sb.append(line);
        sb.append(", ");
        sb.append(enclose(column));
        sb.append(", ");
        sb.append(enclose(toJavaString(value.getValue())));

        for(RefParameter p : value.getParameters()) {
            sb.append(EOL);
            sb.append(indent);
            sb.append(", ");
            sb.append(enclose(p.getName()));
            sb.append(", ");
            sb.append(p.getValue());
        }

        sb.append(")");

        return sb.toString();
    }

    public static String generateFilter(long line, String column, Value value, String indent) {
        String type = getFilterType(value);
        return generateFilter(line, column, value, indent, type);
    }

    public static String generateFilter(long line, String column, Value value, String indent, String type) {
        String v = toJavaString(value.getValue());

        if("regexFilter".equals(type)) {
	        v = getRegex(value.getValue());
	    }

        StringBuilder sb = new StringBuilder(CAPACITY_4K).append(type);

	    sb.append("(");
	    sb.append(line);
	    sb.append(", ");
        sb.append(enclose(column));
	    sb.append(", ");
        sb.append(enclose(v));

	    for(RefParameter p : value.getParameters()) {
	        sb.append(EOL);
            sb.append(indent);
            sb.append(", ");
            sb.append(enclose(p.getName()));
            sb.append(", ");
            sb.append(p.getValue());
        }

        sb.append(")");

	    return sb.toString();
	}

    static String getFilterType(Value value) {
        if (!checkValue(value.getOrigValue())) {
            return "simpleFilter";
        } else if(isRegex(value.getOrigValue())){
            return "regexFilter";
        }

        return "filter";
    }

    public static boolean isNotASimpleFilter(String value) {
        return value.equals(CONV_VALUE_PRESENT) ||
                value.equals(CONV_VALUE_MISSING) ||
                checkValue(value) ||
                isRegex(value);
    }

    public static String createFilterExpression(IFieldStructure fType, String origValue, long line, long uid, String column, AlertCollector alertCollector) {
		String v = origValue;

		if (origValue.equals(CONV_VALUE_PRESENT)) {
            return "notNullFilter(" + line + ", " + enclose(column) + ")";
		} else if (origValue.equals(CONV_VALUE_MISSING)) {
            return "nullFilter(" + line + ", " + enclose(column) + ")";
		} else if (checkValue(origValue)) {
            if(fType.getJavaType() == JavaType.JAVA_LANG_BOOLEAN) {
				v = v.replace("TRUE", "true"); // TODO: add parameter TRUE and FALSE
				v = v.replace("FALSE", "false"); // TODO: add parameter TRUE and FALSE
            } else if(fType.getJavaType() == JavaType.JAVA_LANG_STRING) {
                v = toJavaString(v);
			}
            return "filter(" + line + ", " + enclose(column) + ", " + enclose(v) + ")";
		} else if (isRegex(origValue)) {
            String regexp = getRegex(origValue);
            return "regexFilter(" + line + ", " + enclose(column) + ", " + enclose(regexp) + ")";
        }

//		v = "x == "+v;
//		if (s.getValueType() == FieldTypes.BOOLEAN){
//			v = v.replace("TRUE", "true"); // TODO: add parameter TRUE and FALSE
//			v = v.replace("FALSE", "false"); // TODO: add parameter TRUE and FALSE
//		} else if (s.getValueType() == FieldTypes.STRING){
//			v = escape(v);
//		}
//		return "filter(\""+v+"\")";

		v = castReceiveValue(v, fType.getJavaType(), alertCollector, line, uid, column);
        v = toJavaString(v);

        return "simpleFilter(" + line + ", " + enclose(column) + ", " + enclose(v) + ")";
	}

	/*
	 * If enum alias will not be replaced it will be detected using MVEL.eval
	 */
	/**
	 * Replace enum alias with its value
	 */
	private String prepareEnum(String v, IFieldStructure s, long line, long uid, String column)
	{
		// avoid check for begin of line and end of line;
		boolean isSimpleExpression = !checkValue(v);
		v = " "+v+" ";

		for (String e : s.getValues().keySet())
		{
			int index = v.indexOf(e);

			while (index != -1)
			{
                char c = (char)v.getBytes()[index - 1];
				if (isAlpha(c)) {
					index = v.indexOf(e, index+1);
					continue;
				}

				// check next byte after end of alias
                c = (char)v.getBytes()[index + e.length()];
				if (isAlphaNum(c)) {
					index = v.indexOf(e, index+1);
					continue;
				}

				String replacement = castReceiveEnum(s.getValues().get(e).getCastValue(), s.getJavaType(), isSimpleExpression, line, uid, column);
				if (replacement == null) {
					// TODO: add error message here
					break;
				}
				logger.trace("replacement === {}", replacement);
				v = v.substring(0, index) + replacement + v.substring(index+e.length());
				index = v.indexOf(e, index+replacement.length());
			}
		}
		logger.trace("v === {}", v.substring(1, v.length()-1));
		return v.substring(1, v.length()-1);
	}

	private boolean isAlpha(char c)
	{
		return (c >= 'a' && c <= 'z')
		|| (c >= 'A' && c <= 'Z')
		|| c == '_';
	}

	private boolean isAlphaNum(char c)
	{
		return isAlpha(c) || (c >= '0' && c <= '9');
	}

    private static boolean checkValue(String v) {

        if (!"x".equals(v)) {
        	String[] tokens = v.split(AMLLangConst.REGEX_MVEL_NOT_VARIABLE);
            for (String token : tokens) {
                if("x".equals(token)) {
                    return true;
                }
            }
        }
		return false;
	}

    private static boolean isRegex(String v) {
		return v.startsWith(REGEX_VALUE_PREFIX) && v.endsWith(REGEX_VALUE_SUFFIX);
	}

    private static String getRegex(String regex) {
        return toJavaString(regex.substring(regex.indexOf("[") + 1, regex.lastIndexOf(REGEX_VALUE_SUFFIX)));
	}

    /**
     *
     * @param dictionaryURI
     * @return DictionaryStructure or null (if no such dictionary)
     */
    private IDictionaryStructure getDictionary(SailfishURI dictionaryURI) {
    	try {
    		return dictionaryManager.getDictionary(dictionaryURI);
    	} catch (RuntimeException ex) {
    		logger.error("Failed to getDictionary", ex);
    		return null; // should be hanled by caller
    	}
    }

    /**
     * @param dictionaryURI
     * @param messageName
     * @return MessageStructure or null (if no such Dictionary/Message)
     */
    private IMessageStructure getMessageStructure(SailfishURI dictionaryURI,
            String messageName) {
        try {
        	IDictionaryStructure dictionary = dictionaryManager.getDictionary(dictionaryURI);
            return dictionary.getMessages().get(messageName);
        } catch (RuntimeException ex) {
        	return null; // should be handled by caller
        }
    }


}
