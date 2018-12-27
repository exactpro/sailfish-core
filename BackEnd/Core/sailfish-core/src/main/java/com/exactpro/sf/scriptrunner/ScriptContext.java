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
package com.exactpro.sf.scriptrunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.exactpro.sf.aml.script.AMLHashMap;
import com.exactpro.sf.aml.script.CheckPoint;
import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.IEnvironmentManager;
import com.exactpro.sf.configuration.netdumper.NetDumperService;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.Outcome.Status;
import com.exactpro.sf.scriptrunner.actionmanager.IActionManager;
import com.exactpro.sf.scriptrunner.utilitymanager.IUtilityManager;
import com.exactpro.sf.storage.ScriptRun;
import com.exactpro.sf.util.BugDescription;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class ScriptContext
{
	private final Map<String, Object> staticMap;
	private final Map<String, String> serviceNames;
	private final List<String> serviceList;
	private final Set<Object> receivedMessages;

	private final IEnvironmentManager environmentManager;
    private final OutcomeCollector outcomes;
    private final IScriptProgress scriptProgress;
    private final DebugController debugController;
    private final NetDumperService netDumper;

    private final long scriptDescriptionId;

    private final String userName;

    private boolean conditionallyPassed = false;
    private final Set<BugDescription> knownBugs = new HashSet<>();
	private String actionName;
	private Exception exception = null;
	private boolean interrupt = false;
	private ScriptRun scriptRun;
	private Set<Object> unexpectedMessage;
	private CheckPoint tCStartCheckPoint;
	private long scriptStartTime;
	private String testCaseName;
	private final String environmentName;
	private IScriptConfig scriptConfig;
	private final IActionManager actionManager;
	private final IUtilityManager utilityManager;
	private final IDictionaryManager dictionaryManager;
	private final IScriptReport report;
	private final IWorkspaceDispatcher workspaceDispatcher;
    private final IDataManager dataManager;
    private final Map<String, ClassLoader> pluginClassLoaders;
    private final SetMultimap<String, String> executedActions = HashMultimap.create();

    public ScriptContext(ISFContext context, IScriptProgress scriptProgress, IScriptReport report, DebugController debugController, String userName, long scriptDescriptionId) {
        this(context, scriptProgress, report, debugController, userName, scriptDescriptionId, ServiceName.DEFAULT_ENVIRONMENT);
    }

	public ScriptContext(ISFContext context, IScriptProgress scriptProgress, IScriptReport report, DebugController debugController, String userName, long scriptDescriptionId, String environmentName) {
		Objects.requireNonNull(context, "context cannot be null");

		this.scriptProgress = scriptProgress;
		this.debugController = debugController;
		this.staticMap = new AMLHashMap<>();
		this.serviceNames = new HashMap<>();
		this.serviceList = Collections.synchronizedList(new ArrayList<String>());
		this.environmentManager = context.getEnvironmentManager();
		this.outcomes = new OutcomeCollector();
		this.receivedMessages = new LinkedHashSet<>(); // preserve insert order
		this.unexpectedMessage = new LinkedHashSet<>(); // preserve insert order
		this.userName = userName;
		this.report = report;
		this.actionManager = context.getActionManager();
		this.utilityManager = context.getUtilityManager();
		this.dictionaryManager = context.getDictionaryManager();
		this.workspaceDispatcher = context.getWorkspaceDispatcher();
		this.netDumper = context.getNetDumperService();
		this.scriptDescriptionId = scriptDescriptionId;
        this.dataManager = context.getDataManager();
        this.pluginClassLoaders = context.getPluginClassLoaders();
        this.environmentName = environmentName;

		reset();
	}

	public void reset() {
		this.actionName = null;
		this.exception = null;
		this.outcomes.clear();
		this.receivedMessages.clear();
        this.unexpectedMessage.clear();
		this.tCStartCheckPoint = null;
        this.conditionallyPassed = false;
        this.knownBugs.clear();
	}

    public String getUserName() {
        return userName;
    }

	public String getActionName() {
		return actionName;
	}

	public void setActionName(String actionName) {
		this.actionName = actionName;
	}

	public Exception getException() {
		return exception;
	}

	public void setException(Exception exception) {
		if (this.exception == null) this.exception = exception;
	}

	public boolean isInterrupt() {
		return interrupt;
	}

	public void setInterrupt(boolean interrupt) {
		this.interrupt = interrupt;
	}

	public Map<String, Object> getStaticMap() {
		return staticMap;
	}

	public Map<String, String> getServiceNames() {
		return serviceNames;
	}

	public List<String> getServiceList() {
		return serviceList;
	}

	public IEnvironmentManager getEnvironmentManager() {
		return environmentManager;
	}

	public OutcomeCollector getOutcomeCollector() {
		return this.outcomes;
	}

	public void onOutcomeComplete(String group, String name) {
		this.outcomes.onOutcomeComplete(group, name);
	}

	public void onGroupComplete(String group) {
        this.outcomes.onGroupComplete(group);
	}

	public void storeOutcome(Outcome outcome) {
		this.outcomes.storeOutcome(outcome);
	}

    public Status getOutcomeStatus(String group, String name) {
		return this.outcomes.getOutcomeStatus(group, name);
	}

    public Status getOutcomeGroupStatus(String group) {
		return this.outcomes.getGroupStatus(group);
	}

	public IScriptProgress getScriptProgress()
	{
		return this.scriptProgress;
	}

	public ScriptRun getScriptRun() {
		return scriptRun;
	}

	public void setScriptRun(ScriptRun scriptRun) {
		this.scriptRun = scriptRun;
	}

	// generated code adds received (wait action) message to this list
	public Set<Object> getReceivedMessages() {
		return receivedMessages;
	}

    public Set<Object> getUnexpectedMessage() {
        return unexpectedMessage;
    }

    public void addUnexpectedMessages(Set<Object> setUnexpectedMessages){
        this.unexpectedMessage.addAll(setUnexpectedMessages);
    }

	public CheckPoint getTCStartCheckPoint() {
		return tCStartCheckPoint;
	}

	public void setTCStartCheckPoint(CheckPoint tCStartCheckPoint) {
		this.tCStartCheckPoint = tCStartCheckPoint;
	}

	public long getScriptStartTime() {
		return this.scriptStartTime;
	}

	public void setScriptStartTime(long time) {
		this.scriptStartTime = time;
	}

	public String getTestCaseName() {
		return this.testCaseName;
	}

	public void setTestCaseName(String testCaseName) {
		this.testCaseName = testCaseName;
	}

	public String getEnvironmentName() {
		return environmentName;
	}

	public IScriptReport getReport() {
		return report;
	}

    public void setScriptConfig(IScriptConfig scriptConfig) {
        this.scriptConfig = scriptConfig;
    }

    public IScriptConfig getScriptConfig() {
        return scriptConfig;
    }

    public DebugController getDebugController() {
        return debugController;
    }

    public IActionManager getActionManager() {
        return actionManager;
    }

    public IUtilityManager getUtilityManager() {
        return utilityManager;
    }

	public IDictionaryManager getDictionaryManager() {
		return dictionaryManager;
	}

    public IWorkspaceDispatcher getWorkspaceDispatcher() {
        return workspaceDispatcher;
    }

	public NetDumperService getNetDumperService() {
		return netDumper;
	}

	public long getScriptDescriptionId() {
		return scriptDescriptionId;
	}

    public boolean isConditionallyPassed() {
        return conditionallyPassed;
    }

    public void setConditionallyPassed(boolean conditionallyPassed) {
        this.conditionallyPassed = conditionallyPassed;
    }

    /**
     * @return the dataManager
     */
    public IDataManager getDataManager() {
        return dataManager;
    }

    public Set<BugDescription> getKnownBugs() {
        return knownBugs;
    }

    public Map<String, ClassLoader> getPluginClassLoaders() {
        return pluginClassLoaders;
    }

    public void addExecutedAction(String reference) {
        executedActions.put(getTestCaseName(), reference);
    }

    public boolean checkExecutedActions(List<String> references) {
        return executedActions.get(getTestCaseName()).containsAll(references);
    }
}
