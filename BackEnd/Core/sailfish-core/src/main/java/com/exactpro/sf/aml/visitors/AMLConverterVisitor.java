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
package com.exactpro.sf.aml.visitors;

import static com.exactpro.sf.aml.AMLLangUtil.isFunction;
import static com.exactpro.sf.aml.AMLLangUtil.isReference;
import static com.exactpro.sf.aml.AMLLangUtil.isStaticVariableReference;
import static org.apache.commons.lang3.StringUtils.isNumeric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.exactpro.sf.aml.AMLAction;
import com.exactpro.sf.aml.AMLBlockType;
import com.exactpro.sf.aml.AMLBlockUtility;
import com.exactpro.sf.aml.AMLException;
import com.exactpro.sf.aml.AMLLangConst;
import com.exactpro.sf.aml.AMLLangUtil;
import com.exactpro.sf.aml.AMLMatrixWrapper;
import com.exactpro.sf.aml.AMLSettings;
import com.exactpro.sf.aml.AMLTestCase;
import com.exactpro.sf.aml.generator.Alert;
import com.exactpro.sf.aml.generator.AlertCollector;
import com.exactpro.sf.aml.generator.AlertType;
import com.exactpro.sf.aml.generator.JavaValidator;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.generator.matrix.JavaStatement;
import com.exactpro.sf.aml.generator.matrix.TypeHelper;
import com.exactpro.sf.aml.generator.matrix.Value;
import com.exactpro.sf.aml.reader.struct.AMLBlock;
import com.exactpro.sf.aml.reader.struct.AMLElement;
import com.exactpro.sf.aml.scriptutil.MessageCount;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.actionmanager.ActionInfo;
import com.exactpro.sf.scriptrunner.actionmanager.IActionManager;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class AMLConverterVisitor implements IAMLElementVisitor {
    private final AMLSettings settings;
    private final IActionManager actionManager;
    private final AMLMatrixWrapper wrapper;
    private final ListMultimap<AMLBlockType, AMLTestCase> blocks;
    private final AlertCollector alertCollector;
    private final Map<String, String> staticVariables;
    private final Map<AMLBlock, Integer> cache;
    private final Set<String> references;
    private final Map<AMLBlockType, Integer> matrixOrders;

    private AMLTestCase currentBlock;

    public AMLConverterVisitor(AMLSettings settings, IActionManager actionManager, AMLMatrixWrapper wrapper) {
        this.settings = settings;
        this.actionManager = actionManager;
        this.wrapper = wrapper;
        this.blocks = ArrayListMultimap.create();
        this.alertCollector = new AlertCollector();
        this.currentBlock = null;
        this.staticVariables = settings.getStaticVariables();
        this.cache = new HashMap<>();
        this.references = new HashSet<>();
        this.matrixOrders = new HashMap<>();
    }

    @Override
    public void visit(AMLElement element) throws AMLException {
        if(!element.isExecutable()) {
            return;
        }

        SailfishURI actionURI = null;

        try {
            actionURI = SailfishURI.parse(element.getValue(Column.Action));
        } catch(SailfishURIException e) {
            addError(element, Column.Action, StringUtils.replace(e.getMessage(), "%", "%%"));
        }

        if(actionURI == null) {
            if(!element.containsCell(Column.Reference) && !element.containsCell(Column.ReferenceToFilter)) {
                return;
            }
        } else if(settings.isSuppressAskForContinue() && AMLLangConst.ASK_FOR_CONTINUE_ACTION_URI.matches(actionURI)) {
            return;
        } else if(JavaStatement.DEFINE_HEADER == JavaStatement.value(actionURI)) {
            return;
        }

        int hash = alertCollector.getCount(AlertType.ERROR) > 0
                   ? 0
                   : AMLBlockUtility.hash(element, wrapper, staticVariables, cache, references);

        AMLAction action = new AMLAction(element.getUID(), hash);

        action.setLine(element.getLine());
        action.setExecute(element.isExecutable());
        action.setActionURI(actionURI);
        action.getDefinedColumns().addAll(element.getCells().keySet());
        action.setHeaders(new ArrayList<>(action.getDefinedColumns()));
        action.setAddToReport(currentBlock.isAddToReport());

        for(String cellName : element.getCells().keySet()) {
            String cellValue = element.getValue(cellName);

            if(cellName.startsWith(Column.getIgnoredPrefix())) {
                continue;
            }

            if(cellName.startsWith(Column.getSystemPrefix())) {
                Column column = Column.value(cellName);

                if(column != null) {
                    switch(column) {
                    case Action:
                        break;
                    case AddToReport:
                        if(action.isAddToReport()) {
                            action.setAddToReport(!AMLLangConst.NO.equalsIgnoreCase(cellValue));
                        }
                        break;
                    case BreakPoint:
                        action.setBreakPoint(AMLLangConst.YES.equalsIgnoreCase(cellValue));
                        break;
                    case CheckGroupsOrder:
                        action.setCheckGroupsOrder(AMLLangConst.YES.equalsIgnoreCase(cellValue));
                        break;
                    case CheckPoint:
                        action.setCheckPoint(cellValue);
                        break;
                    case Condition:
                        action.setCondition(new Value(cellValue));
                        break;
                    case ContinueOnFailed:
                        action.setContinueOnFailed(AMLLangConst.YES.equalsIgnoreCase(cellValue));
                        break;
                    case Dependencies:
                        List<String> dependencies = new ArrayList<>();

                        for(String dependency : cellValue.split("\\s*,\\s*")) {
                            if(validateVariableName(element, column, dependency)) {
                                dependencies.add(dependency);
                            }
                        }

                        action.setDependencies(dependencies);
                        break;
                    case Description:
                        action.setDescrption(cellValue);
                        break;
                    case Dictionary:
                        try {
                            action.setDictionaryURI(SailfishURI.parse(cellValue));
                        } catch(SailfishURIException e) {
                            addError(element, Column.Dictionary, StringUtils.replace(e.getMessage(), "%", "%%"));
                        }

                        break;
                    case DoublePrecision:
                        action.setDoublePrecision(cellValue);
                        break;
                    case Execute:
                        break;
                    case FailUnexpected:
                        action.setFailUnexpected(cellValue);
                        break;
                    case Id:
                        action.setId(cellValue);
                        break;
                    case IsStatic:
                        if(JavaStatement.value(actionURI) == null) {
                            action.setStaticAction(AMLLangConst.YES.equalsIgnoreCase(cellValue));
                        }
                        break;
                    case MessageType:
                        if(settings.getLanguageURI().matches(AMLLangConst.AML3)) {
                            action.setMessageTypeColumn(cellValue);
                            break;
                        }

                        // hack to support old namespaces for FIX messages
                        if(cellValue.startsWith("quickfix.fix")) {
                            cellValue = "com.exactpro." + cellValue;
                        }

                        if(actionURI == null && element.containsCell(Column.Reference)) {
                            try {
                                ActionInfo actionInfo = actionManager.getActionInfo(AMLLangConst.DEFINE_MESSAGE_ACTION_URI);
                                Class<?> messageType = Class.forName(cellValue);

                                actionInfo.setMessageType(messageType);
                                actionInfo.setReturnType(messageType);

                                action.setActionInfo(actionInfo);
                                action.setActionURI(actionInfo.getURI());
                            } catch(ClassNotFoundException e) {
                                /*addError(action, column, "Failed to get message type class: %s", cellValue);
                                break;*/
                            }
                        }

                        action.setMessageTypeColumn(cellValue);

                        break;
                    case MessageCount:
                        try {
                            if(settings.getLanguageURI().matches(AMLLangConst.AML2)) {
                                MessageCount messageCount = MessageCount.fromString(cellValue);

                                if(messageCount == null) {
                                    addError(element, column, "Invalid value: %s", cellValue);
                                    break;
                                }
                            } else if(JavaStatement.BEGIN_LOOP == JavaStatement.value(actionURI)) {
                                if(!isNumeric(cellValue) && !isFunction(cellValue) && !isReference(cellValue) && !isStaticVariableReference(cellValue)) {
                                    addError(element, column, "Invalid value: %s", cellValue);
                                    break;
                                }
                            } else if(!MessageCount.isValidExpression(cellValue)) {
                                addError(element, column, "Invalid value: %s", cellValue);
                                break;
                            }
                        } catch(Exception e) {
                            addError(element, column, "Invalid value: %s", cellValue);
                            break;
                        }

                        action.setMessageCount(cellValue);

                        break;
                    case Outcome:
                        String[] outcome = cellValue.split("\\s*:\\s*");

                        if(outcome.length != 2) {
                            addError(element, column, "Invalid value: %s (expected: <group>:<name>)", cellValue);
                            break;
                        }

                        String error = JavaValidator.validateVariableName(outcome[0]);

                        if(error != null) {
                            addError(element, column, "Invalid outcome group: %s (%s)", outcome[0], error);
                        }

                        error = JavaValidator.validateVariableName(outcome[1]);

                        if(error != null) {
                            addError(element, column, "Invalid outcome name: %s (%s)", outcome[1], error);
                            break;
                        }

                        action.setOutcome(outcome[0] + ':' + outcome[1]);
                        action.setOutcomeGroup(outcome[0]);
                        action.setOutcomeName(outcome[1]);

                        break;
                    case Reference:
                        String tempValue = cellValue;

                        if(AMLLangConst.GET_CHECK_POINT_ACTION_URI.matches(actionURI) || AMLLangConst.GET_ADMIN_CHECK_POINT_ACTION_URI.matches(actionURI)) {
                            if(cellValue.startsWith(AMLLangConst.SMART_CHECKPOINT_PREFIX)) {
                                tempValue = cellValue.substring(AMLLangConst.SMART_CHECKPOINT_PREFIX.length());
                            }
                        } else if(JavaStatement.DEFINE_SERVICE_NAME == JavaStatement.value(actionURI)) {
                            if(AMLLangUtil.isStaticVariableReference(cellValue)) {
                                tempValue = AMLLangUtil.getStaticVariableName(cellValue);
                            } else {
                                addError(element, column, "Invalid value: %s (expected: %sname%s)", cellValue, AMLLangConst.BEGIN_STATIC, AMLLangConst.END_STATIC);
                                break;
                            }
                        }

                        if(validateVariableName(element, column, tempValue)) {
                            action.setReference(cellValue);
                        }

                        break;
                    case ReferenceToFilter:
                        if(validateVariableName(element, column, cellValue)) {
                            action.setReferenceToFilter(cellValue);
                        }
                        break;
                    case ReorderGroups:
                        action.setReorderGroups(AMLLangConst.YES.equalsIgnoreCase(cellValue));
                        break;
                    case ServiceName:
                        tempValue = cellValue;

                        if(AMLLangUtil.isStaticVariableReference(cellValue)) {
                            tempValue = AMLLangUtil.getStaticVariableName(cellValue);
                        }

                        if(validateVariableName(element, column, tempValue)) {
                            action.setServiceName(cellValue);
                        }

                        break;
                    case StaticType:
                        Class<?> type = TypeHelper.getClass(cellValue);

                        if(type != null) {
                            action.setStaticType(type.getSimpleName());
                        } else {
                            addError(element, column, "Unknown type: %s", cellValue);
                        }
                        break;
                    case StaticValue:
                        Value value = new Value(cellValue);

                        if(staticVariables != null) {
                            String newValue = staticVariables.get(element.getValue(Column.Reference));

                            if(newValue != null) {
                                value.setValue(newValue);
                            }
                        }

                        action.setStaticValue(value);

                        break;
                    case SystemPrecision:
                        action.setSystemPrecision(cellValue);
                        break;
                    case Tag:
                        action.setTag(cellValue);
                        break;
                    case Template:
                        if(validateVariableName(element, column, cellValue)) {
                            action.setTemplate(cellValue);
                        }
                        break;
                    case Timeout:
                        try {
                            if(Long.parseLong(cellValue) < 0) {
                                addError(element, column, "Value must be positive: %s", cellValue);
                                break;
                            }
                        } catch(NumberFormatException e) {
                            if(AMLLangUtil.isStaticVariableReference(cellValue)) {
                                String variableName = AMLLangUtil.getStaticVariableName(cellValue);
                                if(!validateVariableName(element, column, variableName)) {
                                    break;
                                }
                            } else {
                                addError(element, column, "Value must be in long format or a static variable reference: %s", cellValue);
                                break;
                            }
                        }

                        action.setTimeout(new Value(cellValue));

                        break;
                    case VerificationsOrder:
                        List<String> verificationsOrder = new ArrayList<>();
                        String nameError;
                        for (String order : cellValue.split("\\s*,\\s*")){
                            if (order.contains(":")) {
                                String[] parsedOrder = order.split("\\s*:\\s*");

                                if (parsedOrder.length != 2) {
                                    addError(element, column, "Invalid value: %s (expected: <field>:<status>)", order);
                                    break;
                                }

                                String fieldName = StringUtils.trimToEmpty(parsedOrder[0]);
                                String statusType = StringUtils.trimToEmpty(parsedOrder[1]);

                                nameError = JavaValidator.validateVariableName(parsedOrder[0]);

                                if (nameError != null) {
                                    addError(element, column, "Invalid message field name: %s (%s)", fieldName,
                                             nameError);
                                }

                                nameError = JavaValidator.validateVariableName(statusType);

                                if (nameError != null) {
                                    addError(element, column, "Invalid status name: %s (%s)", statusType,
                                             nameError);
                                    break;
                                }

                                try {
                                    StatusType.getStatusType(statusType);
                                } catch (EPSCommonException e) {
                                    addError(element, column, "Invalid status name: %s (%s)", statusType, order);
                                    break;
                                }

                            } else {
                                nameError = JavaValidator.validateVariableName(order);

                                if (nameError != null) {
                                    addError(element, column, "Invalid message field name: %s (%s)", order,
                                             nameError);
                                }

                                order = order + ":" + StatusType.PASSED.name();
                            }

                            verificationsOrder.add(order);
                        }
                        action.setVerificationsOrder(verificationsOrder);
                        break;
                    default:
                        action.addDefinedServiceField(column, new Value(cellValue));
                        break;
                    }
                } else {
                    action.putServiceField(cellName, new Value(cellValue));
                }
            } else {
                action.put(cellName, new Value(cellValue));
            }
        }

        JavaStatement statement = JavaStatement.value(actionURI);

        if(statement != null) {
            for(Column column : statement.getRequiredColumns()) {
                if(!element.containsCell(column)) {
                    addError(element, column, "Required column is missing");
                }
            }
        } else if(actionURI != null) {
            ActionInfo actionInfo = actionManager.getActionInfo(actionURI, settings.getLanguageURI());

            if(actionInfo == null) {
                addError(element, Column.Action, "Unknown action: %s", actionURI);
            } else {
                action.setActionInfo(actionInfo);
                actionInfo.getRequirements().checkRequirements(action, alertCollector);
                String[] messageTypes = actionInfo.getAllowedMessageTypes();

                if(!ArrayUtils.isEmpty(messageTypes)) {
                    if(!element.containsCell(Column.MessageType)) {
                        addError(element, Column.MessageType, "Required column is missing");
                    } else if(!ArrayUtils.contains(messageTypes, element.getValue(Column.MessageType))) {
                        addError(element, Column.MessageType, "Incompatible message type: %s", element.getValue(Column.MessageType));
                    }
                }
            }
        }

        if(action.isStaticAction() && settings.getLanguageURI().matches(AMLLangConst.AML3)) {
            if(!action.hasActionURI()) {
                addError(element, Column.Action, "Static action must have a name");
            }

            if(!action.hasReference() && !action.hasReferenceToFilter()) {
                addError(element, Column.Reference, "Static action must have a reference or reference to filter");
            }

            ActionInfo actionInfo = action.getActionInfo();

            if(actionInfo != null && actionInfo.getReturnType() == void.class) {
                addError(element, Column.Action, "Static action must return a value");
            }
        }

        String reference = action.getReference();
        String referenceToFilter = action.getReferenceToFilter();
        String template = action.getTemplate();

        if(StringUtils.isNotEmpty(reference)) {
            if(reference.equals(referenceToFilter)) {
                addError(element, Column.Reference, "%s cannot be equal to %s", Column.Reference.getName(), Column.ReferenceToFilter.getName());
            }

            if(reference.equals(template)) {
                addError(element, Column.Reference, "%s cannot be equal to %s", Column.Reference.getName(), Column.Template.getName());
            }
        }

        if(StringUtils.isNotBlank(referenceToFilter) && referenceToFilter.equals(template)) {
            addError(element, Column.ReferenceToFilter, "%s cannot be equal to %s", Column.ReferenceToFilter.getName(), Column.Template.getName());
        }

        try {
            currentBlock.addAction(action);
        } catch(AMLException e) {
            addError(element, null, StringUtils.replace(e.getMessage(), "%", "%%"));
        }

    }

    @Override
    public void visit(AMLBlock block) throws AMLException {
        String actionName = block.getValue(Column.Action);
        AMLBlockType blockType = AMLBlockType.value(actionName);

        if(blockType != null) {
            int matrixOrder = matrixOrders.merge(blockType, 1, Integer::sum);
            String id = block.getValue(Column.Id);
            String reference = block.getValue(Column.Reference);

            if(StringUtils.isNotEmpty(reference)) {
                validateVariableName(block, Column.Reference, reference);
            }

            boolean failOnUnexpectedMessage = AMLLangConst.YES.equalsIgnoreCase(block.getValue(Column.FailOnUnexpectedMessage));
            int hash = 0;

            if(alertCollector.getCount(AlertType.ERROR) == 0 && blockType != AMLBlockType.Block && blockType != AMLBlockType.GlobalBlock) {
                hash = AMLBlockUtility.hash(block, wrapper, staticVariables, cache, references);
            }

            currentBlock = new AMLTestCase(id == null ? Integer.toString(matrixOrder) : id, block.getUID(), hash);

            currentBlock.setLine(block.getLine());
            currentBlock.setBlockType(blockType);
            currentBlock.setExecutable(true);
            currentBlock.setDescription(block.getValue(Column.Description));
            currentBlock.setFailOnUnexpectedMessage(failOnUnexpectedMessage);
            currentBlock.setReference(reference);
            currentBlock.setExecOrder(matrixOrder);
            currentBlock.setMatrixOrder(matrixOrder);

            if(blockType != AMLBlockType.TestCase) {
                currentBlock.setAddToReport(!AMLLangConst.NO.equalsIgnoreCase(block.getValue(Column.AddToReport)));
            }
        }

        if(!block.isExecutable()) {
            return;
        }

        JavaStatement statement = JavaStatement.value(actionName);

        if(statement != null) {
            visit((AMLElement)block);
        }

        for(AMLElement element : block) {
            element.accept(this);
        }

        if(blockType != null) {
            blocks.put(blockType, currentBlock);
            currentBlock = null;
        } else if(statement == JavaStatement.BEGIN_LOOP) {
            new AMLElement().setValue(Column.Action, JavaStatement.END_LOOP.getValue()).accept(this);
        } else if(actionName == null) { // wrapper block for conditional statement doesn't have action
            new AMLElement().setValue(Column.Action, JavaStatement.END_IF.getValue()).accept(this);
        }
    }

    private void addError(AMLElement element, Column column, String message, Object... args) {
        String reference = StringUtils.defaultString(element.getValue(Column.Reference), element.getValue(Column.ReferenceToFilter));
        String columnName = column != null ? column.getName() : null;
        alertCollector.add(new Alert(element.getLine(), element.getUID(), reference, columnName, String.format(message, args)));
    }

    public ListMultimap<AMLBlockType, AMLTestCase> getBlocks() {
        return blocks;
    }

    public AlertCollector getAlertCollector() {
        return alertCollector;
    }

    private boolean validateVariableName(AMLElement element, Column column, String value) {
        String error = JavaValidator.validateVariableName(value);
        if(error != null) {
            addError(element, column, "%s", error);
            return false;
        }
        return true;
    }
}
