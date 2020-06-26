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
package com.exactpro.sf.aml;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.exactpro.sf.aml.generator.AMLGenerateStatus;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.generator.matrix.Value;
import com.exactpro.sf.aml.reader.struct.ExecutionMode;
import com.exactpro.sf.common.util.Pair;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.actionmanager.ActionInfo;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Represent a single action from matrix.
 * @author dmitry.guriev
 *
 */
public class AMLAction implements Cloneable, Serializable {

    private static final long serialVersionUID = 5614362887064463267L;

    private static final String GENERATION_PATH_DELIMITER = " -> ";

    private final Map<String, Value> parameters = new HashMap<>();
    private final Map<String, Value> serviceFields = new HashMap<>();
    private final Map<Column, IField> definedServiceFields = new HashMap<>();    //all defined and interpreted service fields (copy)
    private final Set<String> definedColumns = new HashSet<>();
	private String reference = "";
	private String referenceToFilter = "";
	private String template = "";
    private String id;
    private Value serviceName;
    private ExecutionMode executionMode = ExecutionMode.EXECUTABLE;
    private SailfishURI actionURI; // null if not set
    private SailfishURI dictionaryURI;
    private Value timeout;
	private long line;
	private Class<?> actionParameterType;
	private String checkPoint;
	private String messageTypeColumn;
	private String description = "";
	private boolean addToReport = true;
	private AMLGenerateStatus generateStatus = AMLGenerateStatus.NOT_GENERATED;
    private final StringBuilder generationPathBuilder = new StringBuilder();
    private String messageCount;
    private final List<Pair<String, String>> setters = new ArrayList<>();
	private String doublePrecision = "";
	private String systemPrecision = "";
	private String failUnexpected = "";
	private String staticType;
	private Value staticValue;
    private boolean continueOnFailed;
    private boolean breakPoint;
    private boolean autoStart;
	private List<String> headers;
	private String outcome;
	private String outcomeGroup;
	private String outcomeName;
    private boolean isLastOutcome;
    private boolean isGroupFinished;
    private boolean checkGroupsOrder;
    private Map<String, MessageReference> messageReferences = new HashMap<>();
    private final List<Pair<String, AMLAction>> children = new ArrayList<>();
	private Value condition;
	private String includeBlockReference = "";
    private boolean reorderGroups;
	private ActionInfo actionInfo;
    private boolean staticAction;
    private String tag;
    private List<String> dependencies = Collections.emptyList();
    private List<String> verificationsOrder = Collections.emptyList();
    private Set<String> keyFields = Collections.emptySet();
    private final int hash;
    private final long uid;

	public AMLAction(long uid, int hash) {
        this.uid = uid;
		this.hash = hash;
	}

	public void setReference(String ref)
	{
        this.reference = StringUtils.defaultString(ref);
	}

	public String getReference()
	{
        return reference;
	}

    public boolean hasReference() {
        return StringUtils.isNotEmpty(reference);
	}

	public void setReferenceToFilter(String ref)
	{
        this.referenceToFilter = StringUtils.defaultString(ref);
	}

    public boolean hasReferenceToFilter() {
        return StringUtils.isNotEmpty(referenceToFilter);
    }

	public String getReferenceToFilter()
	{
        return referenceToFilter;
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
        this.template = StringUtils.defaultString(template);
	}

	public boolean hasTemplate() {
        return StringUtils.isNotEmpty(template);
    }

	public void put(String key, Value value)
	{
        parameters.put(key, value);
	}

	public void remove(String key) {
        parameters.remove(key);
	}

	public Value get(String key)
	{
        return parameters.get(key);
	}

	public void setId(String id)
	{
        this.id = StringUtils.defaultString(id);
	}

	public String getId()
	{
        return id;
	}

    public boolean hasId() {
        return StringUtils.isNotEmpty(id);
    }

    public void setServiceName(Value serviceName) {
        this.serviceName = serviceName;
    }

    public Value getServiceName() {
        return serviceName;
    }

	public boolean hasServiceName() {
        return serviceName != null;
    }

	public void setExecutionMode(ExecutionMode executionMode)
	{
		this.executionMode = executionMode;
	}

	public ExecutionMode getExecutionMode()
	{
        return executionMode;
	}

	public SailfishURI getActionURI()
	{
        return actionURI;
	}

	public void setActionURI(SailfishURI actionURI)
	{
		this.actionURI = actionURI;
	}

    public boolean hasActionURI() {
	    return actionURI != null;
	}

	public SailfishURI getDictionaryURI()
	{
        return dictionaryURI;
	}

	public void setDictionaryURI(SailfishURI dictionaryURI)
	{
		this.dictionaryURI = dictionaryURI;
	}

    public boolean hasDictionaryURI() {
	    return dictionaryURI != null;
	}

	public void setTimeout(Value s) {
		this.timeout = s;
	}

	public Value getTimeout() {
        return timeout;
	}

	public void setLine(long line) {
		this.line = line;
	}

	public long getLine() {
        return line;
	}

	public Map<String, Value> getParameters() {
        return parameters;
	}

	public void setActionParameterType(Class<?> type)
	{
		this.actionParameterType = type;
	}

	public Class<?> getActionParameterType()
	{
        return actionParameterType;
	}

	public void setCheckPoint(String checkPoint) {
		this.checkPoint = checkPoint;
	}

	public String getCheckPoint() {
        return checkPoint;
	}

	public String getMessageTypeColumn() {
        return messageTypeColumn;
	}

	public void setMessageTypeColumn(String mesageTypeColumn) {
		this.messageTypeColumn = mesageTypeColumn;
	}

	public Map<String, Value> getServiceFields() {
        return serviceFields;
	}

	public Value getServiceField(String key) {
        return serviceFields.get(key);
	}

	public boolean isServiceFieldExist(String key) {
        return serviceFields.containsKey(key);
	}

	public void putServiceField(String key, Value value) {
        serviceFields.put(key, value);
	}

	public void addDefinedColumn(String columnName) {
        definedColumns.add(columnName);
	}

	public Set<String> getDefinedColumns() {
        return definedColumns;
	}

	public String getDescrption() {
        return description;
	}

	public void setDescrption(String description) {
		this.description = description;
	}

	public void setAddToReport(boolean addToReport) {
		this.addToReport = addToReport;
	}

	public boolean isAddToReport() {
        return addToReport;
	}

	public void setGenerateStatus(AMLGenerateStatus alreadyGenerated) {
	    this.generateStatus = alreadyGenerated;
    }

	public AMLGenerateStatus getGenerateStatus() {
        return generateStatus;
	}

	public void addGenerationSteps(String ... generationSteps) {
	    if (generationSteps != null) {
	        for (int i = 0; i < generationSteps.length; i++) {
	            if (!generationSteps[i].trim().isEmpty()) {
                    generationPathBuilder.append(generationSteps[i]).append(GENERATION_PATH_DELIMITER);
	            }
            }
	    }
    }

	public String getGenerationPath() {
        if(generationPathBuilder.length() >= GENERATION_PATH_DELIMITER.length()) {
            return generationPathBuilder.substring(0, generationPathBuilder.length() - GENERATION_PATH_DELIMITER.length());
	    }
	    return "";
    }

	public void setMessageCount(String messageCount) {
		this.messageCount = messageCount;
	}

	public String getMessageCount() {
        return messageCount;
	}

	public List<Pair<String, String>> getSetters() {
        return setters;
	}

	public String getDoublePrecision() {
        return doublePrecision;
	}

	public void setDoublePrecision(String precision) {
		this.doublePrecision = precision;
	}

	public String getSystemPrecision() {
        return systemPrecision;
	}

	public void setSystemPrecision(String precision) {
		this.systemPrecision = precision;
	}

	public String getFailUnexpected() {
        return failUnexpected;
	}

	public void setFailUnexpected(String value)
	{
		this.failUnexpected = value;
	}

	public String getStaticType() {
        return staticType;
	}

	public void setStaticType(String type) {
		this.staticType = type;
	}

	public Value getStaticValue() {
        return staticValue;
	}

	public void setStaticValue(Value value) {
		this.staticValue = value;
	}

	public boolean getContinueOnFailed() {
        return continueOnFailed;
	}

	public void setContinueOnFailed(boolean b) {
		this.continueOnFailed = b;
	}

	public boolean getBreakPoint() {
        return breakPoint;
	}

	public void setBreakPoint(boolean b) {
		this.breakPoint = b;
	}

	public boolean getAutoStart() {
        return autoStart;
	}

	public void setAutoStart(boolean b) {
		this.autoStart = b;
	}

	public boolean getReorderGroups() {
        return reorderGroups;
	}

	public void setReorderGroups(boolean b) {
	    this.reorderGroups = b;
	}

	public List<String> getHeaders() {
        return headers;
	}

	public void setHeaders(List<String> headers) {
		this.headers = headers;
	}

	public String getOutcome() {
        return outcome;
	}

	public void setOutcome(String cell) {
		this.outcome = cell;
	}

    public boolean hasOutcome() {
        return StringUtils.isNotEmpty(outcome);
    }

    public String getOutcomeGroup() {
		return outcomeGroup;
	}

	public void setOutcomeGroup(String outcomeGroup) {
		this.outcomeGroup = outcomeGroup;
	}

	public String getOutcomeName() {
		return outcomeName;
	}

	public void setOutcomeName(String outcomeName) {
		this.outcomeName = outcomeName;
	}

	public boolean isLastOutcome() {
		return isLastOutcome;
	}

	public void setLastOutcome(boolean isLastOutcome) {
		this.isLastOutcome = isLastOutcome;
	}

	public boolean isGroupFinished() {
		return isGroupFinished;
	}

	public void setGroupFinished(boolean isGroupFinished) {
		this.isGroupFinished = isGroupFinished;
	}

    public boolean isOptional() {
        return executionMode == ExecutionMode.OPTIONAL;
    }

	public void addMessageReference(String lineRef, MessageReference ref) {
        messageReferences.put(lineRef, ref);
	}

	public Map<String, MessageReference> getMessageReferences() {
        return messageReferences;
	}

	public void setMessageReferences(Map<String, MessageReference> messageReferences) {
		this.messageReferences = messageReferences;
	}

	public void addChildAction(String column, AMLAction action) {
        children.add(new Pair<>(column, action));
	}

	public List<Pair<String, AMLAction>> getChildren() {
        return children;
	}

	public Value getCondition() {
        return condition;
    }

    public void setCondition(Value condition) {
        this.condition = condition;
    }

    public String getIncludeBlockReference() {
        return includeBlockReference;
    }

    public void setIncludeBlockReference(String includeBlockReference) {
        this.includeBlockReference = includeBlockReference;
    }

    @Override
	public AMLAction clone()
	{
        AMLAction that = new AMLAction(uid, hash);
        that.actionInfo = actionInfo;
        that.actionParameterType = actionParameterType;
        that.addToReport = addToReport;
        that.generateStatus = generateStatus;
        that.autoStart = autoStart;
        that.checkGroupsOrder = checkGroupsOrder;
        that.checkPoint = checkPoint;
        that.children.addAll(children);
        that.condition = condition != null ? condition.clone() : null;
        that.continueOnFailed = continueOnFailed;
        that.definedColumns.addAll(definedColumns);
        that.dictionaryURI = dictionaryURI;
        that.dependencies = dependencies;
        that.description = description;
        that.executionMode = executionMode;
        that.actionURI = actionURI;
        that.failUnexpected = failUnexpected;
        that.headers = headers;
        that.id = id;
        that.includeBlockReference = includeBlockReference;
        that.isLastOutcome = isLastOutcome;
        that.isGroupFinished = isGroupFinished;
        that.line = line;
        that.messageCount = messageCount;
        that.messageTypeColumn = messageTypeColumn;
        that.outcome = outcome;
        that.outcomeGroup = outcomeGroup;
        that.outcomeName = outcomeName;

        for(Entry<String, Value> entry : parameters.entrySet()) {
			that.parameters.put(entry.getKey(), entry.getValue().clone());
		}

        that.reference = reference;
        that.referenceToFilter = referenceToFilter;
        that.reorderGroups = reorderGroups;

        for(Entry<String, MessageReference> entry : messageReferences.entrySet()) {
			that.messageReferences.put(entry.getKey(), new MessageReference(entry.getValue().getAccessor(), entry.getValue().getDefinition()));
		}
        for(Entry<String, Value> entry : serviceFields.entrySet()) {
			that.serviceFields.put(entry.getKey(), entry.getValue().clone());
		}

        that.serviceName = serviceName == null ? null : serviceName.clone();

        for(Pair<String, String> entry : setters) {
			that.setters.add(new Pair<>(entry.getFirst(), entry.getSecond()));
		}

        that.staticType = staticType;
        that.staticValue = staticValue != null ? staticValue.clone() : null;
        that.tag = tag;
        that.template = template;
        that.timeout = timeout != null ? timeout.clone() : null;

        that.doublePrecision = doublePrecision;
        that.systemPrecision = systemPrecision;

        for(Entry<Column, IField> entry : definedServiceFields.entrySet()) {
			that.definedServiceFields.put(entry.getKey(), entry.getValue());
		}

        that.breakPoint = breakPoint;
        that.staticAction = staticAction;

        that.verificationsOrder = verificationsOrder;

		return that;
	}

	public void clear()
	{
        definedColumns.clear();
        headers.clear();
        parameters.clear();
        serviceFields.clear();
        setters.clear();
        messageReferences.clear();
        children.clear();

		this.headers = null;
	}

	@Override
	public String toString() {
		ToStringBuilder sb = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);

		sb.append("line", line);
		sb.append("reference", reference);
		sb.append("actionURI", actionURI);
		sb.append("executionMode", executionMode);
		sb.append("description", description);
		sb.append("serviceName", serviceName);
		sb.append("id", id);
		sb.append("addToReport", addToReport);

		return sb.toString();
	}

	public boolean isCheckGroupsOrder() {
        return checkGroupsOrder;
    }

    public void setCheckGroupsOrder(boolean checkGroupsOrder) {
        this.checkGroupsOrder = checkGroupsOrder;
    }

	public Map<Column, IField> getDefinedServiceFields() {
		return definedServiceFields;
	}

	public void addDefinedServiceField(Column key, IField value) {
		definedServiceFields.put(key, value);
	}

	public IField getDefinedServiceField(Column key) {
		return definedServiceFields.get(key);
	}

    public ActionInfo getActionInfo() {
        return actionInfo;
    }

    public void setActionInfo(ActionInfo actionInfo) {
        this.actionInfo = actionInfo;
    }

    public boolean isStaticAction() {
        return staticAction;
    }

    public void setStaticAction(boolean staticAction) {
        this.staticAction = staticAction;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = StringUtils.trimToNull(tag);
    }

    public boolean hasTag() {
        return tag != null;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = ImmutableList.copyOf(dependencies);
    }

    public long getUID() {
        return uid;
    }

    public int getHash() {
        return hash;
    }

    public List<String> getVerificationsOrder() {
        return verificationsOrder;
    }

    public void setVerificationsOrder(List<String> verificationsOrder) {
        this.verificationsOrder = verificationsOrder == null ? Collections.emptyList() : ImmutableList.copyOf(verificationsOrder);
    }

    public Set<String> getKeyFields() {
        return keyFields;
    }

    public void setKeyFields(Set<String> keyFields) {
        this.keyFields = keyFields == null ? Collections.emptySet() : ImmutableSet.copyOf(keyFields);
    }
}
