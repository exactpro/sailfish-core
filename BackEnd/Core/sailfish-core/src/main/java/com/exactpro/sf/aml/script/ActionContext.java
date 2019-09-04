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
package com.exactpro.sf.aml.script;

import static com.exactpro.sf.aml.script.ActionNameRetriever.getMethodName;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jooq.lambda.fi.util.function.CheckedSupplier;
import org.slf4j.Logger;

import com.exactpro.sf.aml.scriptutil.MessageCount;
import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;
import com.exactpro.sf.common.impl.messages.BaseMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MessageUtil;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.IEnvironmentManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.DebugController;
import com.exactpro.sf.scriptrunner.IScriptConfig;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.scriptrunner.StatusDescription;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.actionmanager.IActionCaller;
import com.exactpro.sf.scriptrunner.actionmanager.IActionCaller.ConsumerAction;
import com.exactpro.sf.scriptrunner.actionmanager.IActionCaller.ConsumerActionWithParameters;
import com.exactpro.sf.scriptrunner.actionmanager.IActionCaller.FunctionAction;
import com.exactpro.sf.scriptrunner.actionmanager.IActionCaller.FunctionActionWithParameters;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionReport;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionServiceManager;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.impl.ActionReport;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.impl.ActionServiceManager;
import com.exactpro.sf.scriptrunner.actionmanager.exceptions.ActionCallException;
import com.exactpro.sf.storage.MessageFilter;
import com.exactpro.sf.storage.MessageRow;
import com.exactpro.sf.util.KnownBugException;
import com.exactpro.sf.util.MessageKnownBugException;
import com.google.common.collect.ImmutableSet;

public class ActionContext implements IActionContext {
	/** Service name */
	private String serviceName;
	/** Timeout in milliseconds */
	private long timeout;
	/** Check point */
	private CheckPoint checkPoint;
	/** Reference */
	private String reference;
	/** Reference to filter */
	private String referenceToFilter;
    /** Script context */
    private final ScriptContext scriptContext;
	/** Control add message to report or not */
	private boolean addToReport = true;
    private boolean continueOnFailed;
	/** Count messages to check that no unnecessary messages received. */
    @Deprecated
    private MessageCount messageCount;
    private IFilter messageCountFilter;
	private String description;
	private String id;
	/** line in matrix */
    private long line;
	/** N - do not validate for unexpected tags.
	 * Y - fail unexpected tags, but do not fail unexpected repeating groups
	 * A - fail unexpected tags and unexpected repeating groups
	 */
	private String failUnexpected = "N";
	private SailfishURI dictionaryURI;
	private Map<String, Object> messages = Collections.emptyMap();
	private MetaContainer metaContainer;
    private boolean checkGroupsOrder;
	@Deprecated
	private Map<String, Boolean> negativeMap = Collections.emptyMap();
    private boolean reorderGroups;
	private Set<String> uncheckedFields = Collections.emptySet();
    private Set<String> ignoredFields = Collections.emptySet();
    private ActionReport report;
	private final IActionServiceManager serviceManager;
    private final IDataManager dataManager;
    private final Map<String, ClassLoader> pluginClassLoaders;
    private final boolean updateStatus;

    public ActionContext(ScriptContext scriptContext, boolean updateStatus) {
        this.scriptContext = Objects.requireNonNull(scriptContext, "script context cannot be null");
        this.updateStatus = updateStatus;
        IScriptConfig scriptConfig = Objects.requireNonNull(scriptContext.getScriptConfig(), "script config cannot be null");
        report = new ActionReport(scriptContext.getReport(), scriptConfig.getReportFolder(), updateStatus, scriptContext.getWorkspaceDispatcher());
        IEnvironmentManager environmentManager = Objects.requireNonNull(scriptContext.getEnvironmentManager(), "environment manager cannot be null");
        serviceManager = new ActionServiceManager(environmentManager.getConnectionManager(), environmentManager.getServiceStorage());
        this.dataManager = Objects.requireNonNull(scriptContext.getDataManager(), "data manager cannot be null");
        this.pluginClassLoaders = Objects.requireNonNull(scriptContext.getPluginClassLoaders(), "plugin class loaders cannot be null");

        Objects.requireNonNull(scriptContext.getDebugController(), "debug controller cannot be null");
        Objects.requireNonNull(scriptContext.getDictionaryManager(), "dictionary manager cannot be null");
        Objects.requireNonNull(environmentManager.getMessageStorage(), "message storage cannot be null");
    }

	@Override
    public String getServiceName() {
		return serviceName;
	}

    @Override
    public IActionContext withServiceName(String serviceName) {
        ActionContext clone = clone();
        clone.serviceName = serviceName;
        return clone;
    }

    public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

    @Override
    public Set<String> getServicesNames() {
        return new HashSet<>(scriptContext.getServiceList());
    }

	@Override
    public long getTimeout() {
		return timeout;
	}

    @Override
    public IActionContext withTimeout(long timeout) {
        ActionContext clone = clone();
        clone.timeout = timeout;
        return clone;
    }

    public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	@Override
    public CheckPoint getCheckPoint() {
		return checkPoint;
	}

    @Override
    public IActionContext withCheckPoint(CheckPoint checkPoint) {
        ActionContext clone = clone();
        clone.checkPoint = checkPoint;
        return clone;
    }

    public void setCheckPoint(CheckPoint checkPoint) {
		this.checkPoint = checkPoint;
	}

	@Override
    public String getReference() {
		return reference;
	}

    @Override
    public IActionContext withReference(String reference) {
        ActionContext clone = clone();
        clone.reference = reference;
        return clone;
    }

    public void setReference(String reference) {
		this.reference = reference;
	}

	public String getReferenceToFilter() {
		return referenceToFilter;
	}

	public void setReferenceToFilter(String reference) {
		this.referenceToFilter = reference;
	}

    @Override
    public IActionReport getReport() {
        return report.getLastChild();
    }

    @Override
    public IActionServiceManager getServiceManager() {
        return serviceManager;
    }

    @Override
    public String getEnvironmentName() {
        return scriptContext.getEnvironmentName();
    }

    @Override
    public long getScriptStartTime() {
        return scriptContext.getScriptStartTime();
    }

    @Override
    public String getTestCaseName() {
        return scriptContext.getTestCaseName();
    }

    @Override
    public synchronized void pauseScript(long timeout, String reason) throws InterruptedException {
        DebugController debugController = scriptContext.getDebugController();

        debugController.pauseScript(timeout, reason);
        debugController.doWait();
    }

    @Override
    public Logger getLogger() {
        return scriptContext.getScriptConfig().getLogger();
    }

    @Override
    public IDictionaryStructure getDictionary(SailfishURI dictionaryURI) throws RuntimeException {
        return scriptContext.getDictionaryManager().getDictionary(dictionaryURI);
    }

    @Override
    public Iterable<MessageRow> loadMessages(int count, MessageFilter filter) {
        return scriptContext.getEnvironmentManager().getMessageStorage().getMessages(count, filter);
    }

    @Override
    public void storeMessage(IMessage message) {
        scriptContext.getEnvironmentManager().getMessageStorage().storeMessage(message);
    }

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		if (getServiceName() != null) {
			sb.append("client name=").append(getServiceName()).append(", ");
		}
		if (getReference() != null) {
			sb.append("reference=").append(getReference()).append(", ");
		}
		if (getTimeout() >= 0) {
			sb.append("timeout=").append(getTimeout()).append(", ");
		}

        return sb.length() > 2 ? sb.substring(0, sb.length() - 2) : sb.toString();
    }

	public void setAddToReport(boolean addToReport) {
		this.addToReport = addToReport;
	}

	@Override
    public boolean isAddToReport() {
		return addToReport;
	}

    @Override
    public IActionContext withAddToReport(boolean addToReport) {
        ActionContext clone = clone();
        clone.addToReport = addToReport;
        return clone;
    }

    @Override
    public boolean isContinueOnFailed() {
        return false;
    }

    public void setContinueOnFailed(boolean continueOnFailed) {
        this.continueOnFailed = continueOnFailed;
    }

    @Override
    public IActionContext withContinueOnFailed(boolean continueOnFailed) {
        ActionContext clone = clone();
        clone.continueOnFailed = continueOnFailed;
        return clone;
    }

    public void setMessageCount(MessageCount messageCount) {
		this.messageCount = messageCount;
	}

	@Override
    public MessageCount getMessageCount() {
        return messageCount;
	}

    @Override
    public IFilter getMessageCountFilter() {
        return messageCountFilter;
    }

    @Override
    public IActionContext withMessageCountFilter(IFilter messageCountFilter) {
        ActionContext clone = clone();
        clone.messageCountFilter = messageCountFilter;
        return clone;
    }

    public void setMessageCountFilter(IFilter messageCountFilter) {
        this.messageCountFilter = messageCountFilter;
    }

    @Override
    public String getDescription() {
		return description;
	}

    @Override
    public IActionContext withDescription(String description) {
        ActionContext clone = clone();
        clone.description = description;
        return clone;
    }

    public void setDescription(String s) {
		this.description = s;
	}

	@Override
    public String getId() {
		return id;
	}

    @Override
    public IActionContext withId(String id) {
        ActionContext clone = clone();
        clone.id = id;
        return clone;
    }

    public void setId(String id) {
		this.id = id;
	}

    @Override
    public long getLine() {
        return line;
    }

    public void setLine(long line) {
        this.line = line;
    }

	public String getFailUnexpected() {
		return failUnexpected;
	}

	public void setFailUnexpected(String failUnexpected) {
		this.failUnexpected = failUnexpected;
	}

	@Override
	public SailfishURI getDictionaryURI() {
		return dictionaryURI;
	}

    @Override
    public IActionContext withDictionaryURI(SailfishURI dictionaryURI) {
        ActionContext clone = clone();
        clone.dictionaryURI = dictionaryURI;
        return clone;
    }

    public void setDictionaryURI(SailfishURI dictionaryURI) {
		this.dictionaryURI = dictionaryURI;
	}

	@Override
    public Object getMessage(String reference) {
		Object message = messages.get(reference);
        return message instanceof IMessage ? ((IMessage)message).cloneMessage() : message;
    }

	public void setMessages(Map<String, Object> messages) {
        this.messages = Objects.requireNonNull(messages, "messages cannot be null");
	}

	@Override
    public MetaContainer getMetaContainer() {
        return metaContainer;
	}

    @Override
    public IActionContext withMetaContainer(MetaContainer metaContainer) {
        ActionContext clone = clone();
        clone.metaContainer = metaContainer.clone(true);
        return clone;
    }

    public void setMetaContainer(MetaContainer metaContainer) {
		this.metaContainer = metaContainer;
	}

	@Override
    public boolean isCheckGroupsOrder() {
        return checkGroupsOrder;
    }

    @Override
    public IActionContext withCheckGroupsOrder(boolean checkGroupsOrder) {
        ActionContext clone = clone();
        clone.checkGroupsOrder = checkGroupsOrder;
        return clone;
    }

    public void setCheckGroupsOrder(boolean checkGroupsOrder) {
        this.checkGroupsOrder = checkGroupsOrder;
    }

    @Deprecated // In AML3 we use 'x != 2'
	public void setNegativeMap(Map<String, Boolean> negativeMap) {
        this.negativeMap = Objects.requireNonNull(negativeMap, "negativeMap cannot be null");
	}

	@Override
    @Deprecated // In AML3 we use 'x != 2'
	public Map<String, Boolean> getNegativeMap() {
        return negativeMap;
	}

    @Override
    public boolean isReorderGroups() {
        return reorderGroups;
    }

    @Override
    public IActionContext withReorderGroups(boolean reorderGroups) {
        ActionContext clone = clone();
        clone.reorderGroups = reorderGroups;
        return clone;
    }

    public void setReorderGroups(boolean reorderGroups) {
        this.reorderGroups = reorderGroups;
    }

    @Override
    public <T> T getSystemColumn(String name) {
        return getMetaContainer().getSystemColumn(name);
    }

    @Deprecated
    public void putSystemColumn(String name, Object value) {
        getMetaContainer().putSystemColumn(name, value);
    }

    @Override
    public IDataManager getDataManager() {
        return dataManager;
    }

    @Override
    public Set<String> getUncheckedFields() {
        return uncheckedFields;
    }

    @Override
    public IActionContext withUncheckedFields(Set<String> uncheckedFields) {
        ActionContext clone = clone();
        clone.setUncheckedFields(uncheckedFields);
        return clone;
    }

    public void setUncheckedFields(Set<String> uncheckedFields) {
        this.uncheckedFields = ImmutableSet.copyOf(uncheckedFields);
    }

    @Override
    public Set<String> getIgnoredFields() {
        return ignoredFields;
    }

    @Override
    public IActionContext withIgnoredFields(Set<String> ignoredFields) {
        ActionContext clone = clone();
        clone.setIgnoredFields(ignoredFields);
        return clone;
    }
  
    public void setIgnoredFields(Set<String> ignoredFields) {
        this.ignoredFields = ImmutableSet.copyOf(ignoredFields);
    }

    @Override
    public ClassLoader getPluginClassLoader(String pluginAlias) {
        if(!pluginClassLoaders.containsKey(pluginAlias)) {
            throw new EPSCommonException("Unknown plugin: " + pluginAlias);
        }

        return pluginClassLoaders.get(pluginAlias);
    }

    @Override
    public Optional<Object> handleKnownBugException(KnownBugException e, String reference) {
        Objects.requireNonNull(e, "exception cannot be null");
        getLogger().warn(e.getMessage(), e);

        Object message = null;

        if(e instanceof MessageKnownBugException) {
            message = ((MessageKnownBugException)e).getContainedMessage();

            if(StringUtils.isNotBlank(reference)) {
                messages.put(reference, message);
            }
        }

        scriptContext.setConditionallyPassed(true);
        scriptContext.getKnownBugs().addAll(e.getPotentialDescriptions());

        return Optional.ofNullable(message);
    }

    @Override
    public <T extends IActionCaller> void callAction(T actionClass, ConsumerAction<T> action, String tag, List<String> verificationOrder) throws Throwable {
        callAction(() -> {
            action.accept(actionClass, this);
            return null;
        }, getMethodName(actionClass.getClass(), action, this), null, tag, verificationOrder);
    }

    @Override
    public <T extends IActionCaller, R> R callAction(T actionClass, FunctionAction<T, R> action, String tag, List<String> verificationOrder) throws Throwable {
        return callAction(() -> action.apply(actionClass, this), getMethodName(actionClass.getClass(), action, this), null, tag, verificationOrder);
    }

    @Override
    public <T extends IActionCaller, P> void callAction(T actionClass, ConsumerActionWithParameters<T, P> action, P parameters, String tag, List<String> verificationOrder) throws Throwable {
        callAction(() -> {
            action.accept(actionClass, this, parameters);
            return null;
        }, getMethodName(actionClass.getClass(), action, this, parameters), parameters, tag, verificationOrder);
    }

    @Override
    public <T extends IActionCaller, P, R> R callAction(T actionClass, FunctionActionWithParameters<T, P, R> action, P parameters, String tag, List<String> verificationOrder) throws Throwable {
        return callAction(() -> action.apply(actionClass, this, parameters), getMethodName(actionClass.getClass(), action, this, parameters), parameters, tag, verificationOrder);
    }

    @SuppressWarnings("unchecked")
    private <P, R> R callAction(CheckedSupplier<R> action, String methodName, P parameters, String tag, List<String> verificationOrder) throws Throwable {
        IMessage message = null;

        if(parameters instanceof Map<?, ?>) {
            message = MessageUtil.convertToIMessage((Map<?, ?>)parameters, null, "Namespace", "Message");
        } else if(parameters instanceof BaseMessage) {
            message = ((BaseMessage)parameters).getMessage();
        } else if(parameters instanceof IMessage) {
            message = (IMessage)parameters;
        } else if(parameters != null) {
            throw new ActionCallException("Unsupported parameters type: " + parameters.getClass().getCanonicalName());
        }

        ActionReport report = this.report.getLastChild();
        boolean actionCreated = false;
        int hashCode = action.hashCode();
        String messageType = "";

        if(message != null) {
            messageType = message.getName();
        }

        try {
            if(addToReport) {
                report.createAction(id, serviceName, methodName, messageType, description, message, checkPoint, tag, hashCode, verificationOrder, null);
                actionCreated = true;
            }

            Object returnValue = action.get();

            if(returnValue != null) {
                scriptContext.getReceivedMessages().add(returnValue);

                if(StringUtils.isNotEmpty(reference)) {
                    messages.put(reference, returnValue);
                }
            }

            if(addToReport) {
                report.closeAction(new StatusDescription(StatusType.PASSED, ""), returnValue);
            }

            return (R)returnValue;
        } catch(KnownBugException e) {
            getLogger().warn(e.getMessage(), e);
            Object containedMessage = null;

            if(e instanceof MessageKnownBugException) {
                containedMessage = ((MessageKnownBugException)e).getContainedMessage();
                scriptContext.getReceivedMessages().add(containedMessage);

                if(StringUtils.isNotBlank(reference)) {
                    messages.put(reference, containedMessage);
                }
            }

            if(StringUtils.isNotBlank(reference)) {
                scriptContext.addExecutedAction(reference);
            }

            scriptContext.setConditionallyPassed(true);
            scriptContext.getKnownBugs().addAll(e.getPotentialDescriptions());

            if(!actionCreated) {
                report.createAction(id, serviceName, methodName, messageType, description, message, checkPoint, tag, hashCode, verificationOrder, null);
            }

            report.closeAction(new StatusDescription(StatusType.CONDITIONALLY_PASSED, e.getMessage(), e.getPotentialDescriptions()), containedMessage);

            return (R)containedMessage;
        } catch(Exception e) {
            getLogger().warn(e.getMessage(), e);
            scriptContext.setInterrupt(e instanceof InterruptedException);
            scriptContext.setException(e);

            if(!actionCreated) {
                report.createAction(id, serviceName, methodName, messageType, description, message, checkPoint, tag, hashCode, verificationOrder, null);
            }

            report.closeAction(new StatusDescription(StatusType.FAILED, e.getMessage(), e), null);

            if(!continueOnFailed || e instanceof InterruptedException) {
                throw e;
            }

            return null;
        }
    }

    @Override
    protected ActionContext clone() {
        ActionContext cloned = new ActionContext(scriptContext, updateStatus);

        cloned.serviceName = serviceName;
        cloned.timeout = timeout;
        cloned.checkPoint = checkPoint;
        cloned.reference = reference;
        cloned.referenceToFilter = referenceToFilter;
        cloned.addToReport = addToReport;
        cloned.continueOnFailed = continueOnFailed;
        cloned.messageCount = messageCount;
        cloned.messageCountFilter = messageCountFilter;
        cloned.description = description;
        cloned.id = id;
        cloned.line = line;
        cloned.failUnexpected = failUnexpected;
        cloned.dictionaryURI = dictionaryURI;
        cloned.messages = new HashMap<>(messages);
        cloned.metaContainer = metaContainer.clone(true);
        cloned.checkGroupsOrder = checkGroupsOrder;
        cloned.negativeMap = new HashMap<>(negativeMap);
        cloned.reorderGroups = reorderGroups;
        cloned.uncheckedFields = ImmutableSet.copyOf(uncheckedFields);
        cloned.ignoredFields = ImmutableSet.copyOf(ignoredFields);
        cloned.report = report;

        return cloned;
    }
}
