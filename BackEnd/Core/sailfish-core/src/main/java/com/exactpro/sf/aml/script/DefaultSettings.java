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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.exactpro.sf.aml.scriptutil.MessageCount;
import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.IEnvironmentManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.DebugController;
import com.exactpro.sf.scriptrunner.IScriptConfig;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionReport;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionServiceManager;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.impl.ActionReport;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.impl.ActionServiceManager;
import com.exactpro.sf.storage.MessageFilter;
import com.exactpro.sf.storage.MessageRow;
import com.exactpro.sf.util.KnownBugException;
import com.exactpro.sf.util.MessageKnownBugException;
import com.google.common.collect.ImmutableSet;

public class DefaultSettings implements IActionContext {
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
	/** Count messages to check that no unnecessary messages received. */
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
	private boolean checkGroupsOrder = false;
	@Deprecated
	private Map<String, Boolean> negativeMap = Collections.emptyMap();
	private boolean reorderGroups = false;
	private Set<String> uncheckedFields = Collections.emptySet();
	private final IActionReport report;
	private final IActionServiceManager serviceManager;
    private final IDataManager dataManager;
    private final Map<String, ClassLoader> pluginClassLoaders;

    public DefaultSettings(ScriptContext scriptContext, boolean updateStatus) {
        this.scriptContext = Objects.requireNonNull(scriptContext, "script context cannot be null");
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

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	@Override
    public CheckPoint getCheckPoint() {
		return checkPoint;
	}

	public void setCheckPoint(CheckPoint checkPoint) {
		this.checkPoint = checkPoint;
	}

	@Override
    public String getReference() {
		return reference;
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
        return report;
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

		if (sb.length() > 2) {
			return sb.substring(0, sb.length()-2);
		}

		return sb.toString();
	}

	public void setAddToReport(boolean addToReport) {
		this.addToReport = addToReport;
	}

	@Override
    public boolean isAddToReport() {
		return addToReport;
	}

	public void setMessageCount(MessageCount messageCount) {
		this.messageCount = messageCount;
	}

	@Override
    public MessageCount getMessageCount() {
		return this.messageCount;
	}

    @Override
    public IFilter getMessageCountFilter() {
        return messageCountFilter;
    }

    public void setMessageCountFilter(IFilter messageCountFilter) {
        this.messageCountFilter = messageCountFilter;
    }

    @Override
    public String getDescription() {
		return description;
	}

	public void setDescription(String s) {
		this.description = s;
	}

	@Override
    public String getId() {
		return id;
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

	public void setDictionaryURI(SailfishURI dictionaryURI) {
		this.dictionaryURI = dictionaryURI;
	}

	@Override
    public Object getMessage(String reference) {
		Object message = messages.get(reference);

		if(message instanceof IMessage) {
		    return ((IMessage)message).cloneMessage();
		}

		return message;
	}

	public void setMessages(Map<String, Object> messages) {
		this.messages = messages;
	}

	@Override
    public MetaContainer getMetaContainer() {
		return this.metaContainer;
	}

	public void setMetaContainer(MetaContainer metaContainer) {
		this.metaContainer = metaContainer;
	}

	@Override
    public boolean isCheckGroupsOrder() {
        return checkGroupsOrder;
    }

    public void setCheckGroupsOrder(boolean checkGroupsOrder) {
        this.checkGroupsOrder = checkGroupsOrder;
    }

    @Deprecated // In AML3 we use 'x != 2'
	public void setNegativeMap(Map<String, Boolean> negativeMap) {
		this.negativeMap = negativeMap;
	}

	@Override
    @Deprecated // In AML3 we use 'x != 2'
	public Map<String, Boolean> getNegativeMap() {
		return this.negativeMap;
	}

    @Override
    public boolean isReorderGroups() {
        return reorderGroups;
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
        return this.dataManager;
    }

    @Override
    public Set<String> getUncheckedFields() {
        return this.uncheckedFields;
    }

    public void setUncheckedFields(Set<String> uncheckedFields) {
        this.uncheckedFields = ImmutableSet.copyOf(uncheckedFields);
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
}
