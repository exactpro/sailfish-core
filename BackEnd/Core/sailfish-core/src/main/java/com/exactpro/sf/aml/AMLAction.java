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
import com.exactpro.sf.common.util.Pair;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.actionmanager.ActionInfo;
import com.google.common.collect.ImmutableList;

/**
 * Represent a single action from matrix.
 * @author dmitry.guriev
 *
 */
public class AMLAction implements IAction, Cloneable, Serializable {

    private static final long serialVersionUID = 5614362887064463267L;

    private static final String GENERATION_PATH_DELIMITER = " -> ";

	private final Map<String, Value> parameters;
	private final Map<String, Value> serviceFields;
	private Map<Column, IField> definedServiceFields;	//all defined and interpreted service fields (copy)
	private final Set<String> definedColumns;
	private String reference = "";
	private String referenceToFilter = "";
	private String template = "";
	private String id = null;
	private String serviceName = null;
	private boolean execute = true;
	private SailfishURI actionURI = null; // null if not set
	private SailfishURI dictionaryURI = null;
	private Value timeout = null;
	private long line;
	private Class<?> actionParameterType;
	private String checkPoint;
	private String messageTypeColumn;
	private String description = "";
	private boolean addToReport = true;
	private AMLGenerateStatus generateStatus = AMLGenerateStatus.NOT_GENERATED;
	private StringBuilder generationPathBuilder;
	private String messageCount = null;
	private final List<Pair<String, String>> setters;
	private String doublePrecision = "";
	private String systemPrecision = "";
	private String failUnexpected = "";
	private String staticType;
	private Value staticValue;
	private boolean continueOnFailed = false;
	private boolean breakPoint = false;
	private boolean autoStart = false;
	private List<String> headers;
	private String outcome;
	private String outcomeGroup;
	private String outcomeName;
	private boolean isLastOutcome = false;
	private boolean isGroupFinished = false;
	private boolean checkGroupsOrder = false;
	private Map<String, MessageReference> messageReferences;
	private List<Pair<String, AMLAction>> children;
	private Value condition;
	private String includeBlockReference = "";
	private boolean reorderGroups = false;
	private ActionInfo actionInfo;
	private boolean staticAction = false;
    private String tag;
    private List<String> dependencies = Collections.emptyList();
    private List<String> verificationsOrder = Collections.emptyList();
    private final int hash;
    private final long uid;

	public AMLAction(long uid, int hash) {
		this.parameters = new HashMap<>();
		this.serviceFields = new HashMap<>();
		this.definedServiceFields = new HashMap<>();
		this.definedColumns = new HashSet<>();
		this.setters = new ArrayList<>();
		this.messageReferences = new HashMap<>();
		this.children = new ArrayList<>();
		this.generationPathBuilder = new StringBuilder();
		this.uid = uid;
		this.hash = hash;
	}

	@Override
	public void setReference(String ref)
	{
		this.reference = (ref == null) ? "" : ref;
	}

	@Override
	public String getReference()
	{
		return this.reference;
	}

	@Override
	public boolean hasReference()
	{
		return this.reference != null && !this.reference.equals("");
	}

	@Override
	public void setReferenceToFilter(String ref)
	{
		this.referenceToFilter = (ref == null) ? "" : ref;
	}

	@Override
	public boolean hasReferenceToFilter()
	{
		return this.referenceToFilter != null && false == this.referenceToFilter.equals("");
	}

	@Override
	public String getReferenceToFilter()
	{
		return this.referenceToFilter;
	}

	@Override
	public String getTemplate() {
		return template;
	}

	@Override
	public void setTemplate(String template) {
		this.template = template == null ? "" : template;
	}

	@Override
	public boolean hasTemplate() {
		return this.template != null && !this.template.equals("");
	}

	@Override
	public void put(String key, Value value)
	{
		this.parameters.put(key, value);
	}

	public void remove(String key) {
	    this.parameters.remove(key);
	}

	@Override
	public Value get(String key)
	{
		return this.parameters.get(key);
	}

	@Override
	public void setId(String id)
	{
		this.id = (id != null) ? id : "";
	}

	@Override
	public String getId()
	{
		return this.id;
	}

	@Override
	public boolean hasId()
	{
		return this.id != null && !this.id.trim().isEmpty();
	}

	@Override
	public void setServiceName(String serviceName)
	{
		this.serviceName = serviceName;
	}

	@Override
	public String getServiceName()
	{
		return this.serviceName;
	}

	@Override
	public boolean hasServiceName() {
	    return StringUtils.isNotEmpty(serviceName);
	}

	@Override
	public void setExecute(boolean b)
	{
		this.execute = b;
	}

	@Override
	public boolean isExecute()
	{
		return this.execute;
	}

	@Override
	public SailfishURI getActionURI()
	{
		return this.actionURI;
	}

	@Override
	public void setActionURI(SailfishURI actionURI)
	{
		this.actionURI = actionURI;
	}

	@Override
    public boolean hasActionURI() {
	    return actionURI != null;
	}

	@Override
	public SailfishURI getDictionaryURI()
	{
		return this.dictionaryURI;
	}

	@Override
	public void setDictionaryURI(SailfishURI dictionaryURI)
	{
		this.dictionaryURI = dictionaryURI;
	}

	@Override
    public boolean hasDictionaryURI() {
	    return dictionaryURI != null;
	}

	@Override
	public void setTimeout(Value s) {
		this.timeout = s;
	}

	@Override
	public Value getTimeout() {
		return this.timeout;
	}

	public void setLine(long line) {
		this.line = line;
	}

	public long getLine() {
		return this.line;
	}

	@Override
	public Map<String, Value> getParameters() {
		return this.parameters;
	}

	public void setActionParameterType(Class<?> type)
	{
		this.actionParameterType = type;
	}

	public Class<?> getActionParameterType()
	{
		return this.actionParameterType;
	}

	@Override
	public void setCheckPoint(String checkPoint) {
		this.checkPoint = checkPoint;
	}

	@Override
	public String getCheckPoint() {
		return this.checkPoint;
	}

	@Override
	public String getMessageTypeColumn() {
		return this.messageTypeColumn;
	}

	@Override
	public void setMessageTypeColumn(String mesageTypeColumn) {
		this.messageTypeColumn = mesageTypeColumn;
	}

	public Map<String, Value> getServiceFields() {
		return this.serviceFields;
	}

	public Value getServiceField(String key) {
		return this.serviceFields.get(key);
	}

	public boolean isServiceFieldExist(String key) {
		return this.serviceFields.containsKey(key);
	}

	public void putServiceField(String key, Value value) {
		this.serviceFields.put(key, value);
	}

	@Override
	public void addDefinedColumn(String columnName) {
		this.definedColumns.add(columnName);
	}

	@Override
	public Set<String> getDefinedColumns() {
		return this.definedColumns;
	}

	@Override
	public String getDescrption() {
		return this.description;
	}

	@Override
	public void setDescrption(String description) {
		this.description = description;
	}

	public void setAddToReport(boolean addToReport) {
		this.addToReport = addToReport;
	}

	public boolean isAddToReport() {
		return this.addToReport;
	}

	public void setGenerateStatus(AMLGenerateStatus alreadyGenerated) {
	    this.generateStatus = alreadyGenerated;
    }

	public AMLGenerateStatus getGenerateStatus() {
		return this.generateStatus;
	}

	public void addGenerationSteps(String ... generationSteps) {
	    if (generationSteps != null) {
	        for (int i = 0; i < generationSteps.length; i++) {
	            if (!generationSteps[i].trim().isEmpty()) {
	                this.generationPathBuilder.append(generationSteps[i]).append(GENERATION_PATH_DELIMITER);
	            }
            }
	    }
    }

	public String getGenerationPath() {
	    if (this.generationPathBuilder.length() >= GENERATION_PATH_DELIMITER.length()) {
    	    return this.generationPathBuilder.substring(0, this.generationPathBuilder.length() - GENERATION_PATH_DELIMITER.length());
	    }
	    return "";
    }

	@Override
	public void setMessageCount(String messageCount) {
		this.messageCount = messageCount;
	}

	@Override
	public String getMessageCount() {
		return this.messageCount;
	}

	public List<Pair<String, String>> getSetters() {
		return this.setters;
	}

	@Override
	public String getDoublePrecision() {
		return this.doublePrecision;
	}

	@Override
	public void setDoublePrecision(String precision) {
		this.doublePrecision = precision;
	}

	@Override
	public String getSystemPrecision() {
		return this.systemPrecision;
	}

	@Override
	public void setSystemPrecision(String precision) {
		this.systemPrecision = precision;
	}

	public String getFailUnexpected() {
		return this.failUnexpected;
	}

	public void setFailUnexpected(String value)
	{
		this.failUnexpected = value;
	}

	@Override
	public String getStaticType() {
		return this.staticType;
	}

	@Override
	public void setStaticType(String type) {
		this.staticType = type;
	}

	@Override
	public Value getStaticValue() {
		return this.staticValue;
	}

	@Override
	public void setStaticValue(Value value) {
		this.staticValue = value;
	}

	@Override
	public boolean getContinueOnFailed() {
		return this.continueOnFailed;
	}

	@Override
	public void setContinueOnFailed(boolean b) {
		this.continueOnFailed = b;
	}

	public boolean getBreakPoint() {
		return this.breakPoint;
	}

	public void setBreakPoint(boolean b) {
		this.breakPoint = b;
	}

	@Override
	public boolean getAutoStart() {
		return this.autoStart;
	}

	@Override
	public void setAutoStart(boolean b) {
		this.autoStart = b;
	}

	@Override
	public boolean getReorderGroups() {
	    return this.reorderGroups;
	}

	@Override
	public void setReorderGroups(boolean b) {
	    this.reorderGroups = b;
	}

	@Override
	public List<String> getHeaders() {
		return this.headers;
	}

	@Override
	public void setHeaders(List<String> headers) {
		this.headers = headers;
	}

	@Override
	public String getOutcome() {
		return this.outcome;
	}

	@Override
	public void setOutcome(String cell) {
		this.outcome = cell;
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

	@Override
	public void addMessageReference(String lineRef, MessageReference ref) {
		this.messageReferences.put(lineRef, ref);
	}

	@Override
	public Map<String, MessageReference> getMessageReferences() {
		return this.messageReferences;
	}

	@Override
	public void setMessageReferences(Map<String, MessageReference> messageReferences) {
		this.messageReferences = messageReferences;
	}

	public void addChildAction(String column, AMLAction action) {
		this.children.add(new Pair<>(column, action));
	}

	public List<Pair<String, AMLAction>> getChildren() {
		return this.children;
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
		AMLAction that = new AMLAction(this.uid, this.hash);
		that.actionInfo = this.actionInfo;
		that.actionParameterType = this.actionParameterType;
		that.addToReport = this.addToReport;
		that.generateStatus = this.generateStatus;
		that.autoStart = this.autoStart;
		that.checkGroupsOrder = this.checkGroupsOrder;
		that.checkPoint = this.checkPoint;
		that.children.addAll(this.children);
		that.condition = this.condition != null ? this.condition.clone() : null;
		that.continueOnFailed = this.continueOnFailed;

		for (String s : this.definedColumns) {
			that.definedColumns.add(s);
		}

		that.dictionaryURI =  this.dictionaryURI;
        that.dependencies = this.dependencies;
		that.description = this.description;
		that.execute = this.execute;
		that.actionURI = this.actionURI;
		that.failUnexpected = this.failUnexpected;
		that.headers = this.headers;
		that.id = this.id;
		that.includeBlockReference = this.includeBlockReference;
		that.isLastOutcome = this.isLastOutcome;
		that.isGroupFinished = this.isGroupFinished;
		that.line = this.line;
		that.messageCount = this.messageCount;
		that.messageTypeColumn = this.messageTypeColumn;
		that.outcome = this.outcome;
		that.outcomeGroup = this.outcomeGroup;
		that.outcomeName = this.outcomeName;

		for(Entry<String, Value> entry : this.parameters.entrySet()) {
			that.parameters.put(entry.getKey(), entry.getValue().clone());
		}

		that.reference = this.reference;
		that.referenceToFilter = this.referenceToFilter;
		that.reorderGroups = this.reorderGroups;

		for (Entry<String, MessageReference> entry : this.messageReferences.entrySet()) {
			that.messageReferences.put(entry.getKey(), new MessageReference(entry.getValue().getAccessor(), entry.getValue().getDefinition()));
		}
		for(Entry<String, Value> entry : this.serviceFields.entrySet()) {
			that.serviceFields.put(entry.getKey(), entry.getValue().clone());
		}

		that.serviceName = this.serviceName;

		for(Pair<String, String> entry : this.setters) {
			that.setters.add(new Pair<>(entry.getFirst(), entry.getSecond()));
		}

		that.staticType = this.staticType;
		that.staticValue = this.staticValue != null ? this.staticValue.clone() : null;
        that.tag = this.tag;
		that.template = this.template;
		that.timeout = this.timeout != null ? this.timeout.clone() : null;

		that.doublePrecision = this.doublePrecision;
        that.systemPrecision = this.systemPrecision;

		for(Entry<Column, IField> entry : this.definedServiceFields.entrySet()) {
			that.definedServiceFields.put(entry.getKey(), entry.getValue());
		}

        that.breakPoint = this.breakPoint;
        that.staticAction = this.staticAction;

        that.verificationsOrder = this.verificationsOrder;

		return that;
	}

	public void clear()
	{
		this.definedColumns.clear();
		this.headers.clear();
		this.parameters.clear();
		this.serviceFields.clear();
		this.setters.clear();
		this.messageReferences.clear();
		this.children.clear();

		this.headers = null;
	}

	@Override
	public String toString() {
		ToStringBuilder sb = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);

		sb.append("line", line);
		sb.append("reference", reference);
		sb.append("actionURI", actionURI);
		sb.append("executable", execute);
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

    @Override
	public Map<Column, IField> getDefinedServiceFields() {
		return definedServiceFields;
	}

	@Override
	public void addDefinedServiceField(Column key, IField value) {
		definedServiceFields.put(key, value);
	}

	@Override
	public IField getDefinedServiceField(Column key) {
		return definedServiceFields.get(key);
	}

    @Override
    public ActionInfo getActionInfo() {
        return actionInfo;
    }

    @Override
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
}
