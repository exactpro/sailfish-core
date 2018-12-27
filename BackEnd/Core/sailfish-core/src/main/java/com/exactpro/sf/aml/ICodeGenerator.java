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

import java.io.IOException;
import java.util.List;

import com.exactpro.sf.aml.generator.AlertCollector;
import com.exactpro.sf.aml.generator.GeneratedScript;
import com.exactpro.sf.common.adapting.IAdapterManager;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.IEnvironmentManager;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.IProgressListener;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.scriptrunner.actionmanager.IActionManager;
import com.exactpro.sf.scriptrunner.services.IStaticServiceManager;
import com.exactpro.sf.scriptrunner.utilitymanager.IUtilityManager;

/**
 *
 * @author dmitry.guriev
 *
 */
public interface ICodeGenerator {

	public abstract GeneratedScript generateCode(List<AMLTestCase> testCases, List<AMLTestCase> beforeTCBlocks, List<AMLTestCase> afterTCBlocks)
			throws AMLException, IOException, InterruptedException;

	public abstract AlertCollector getAlertCollector();

	public abstract void cleanup();

	public abstract void init(IWorkspaceDispatcher workspaceDispatcher, IAdapterManager adapterManager, IEnvironmentManager environment, IDictionaryManager dictionaryManager, IStaticServiceManager staticServiceManager, IActionManager actionManager, IUtilityManager utilityManager, ScriptContext scriptContext, AMLSettings amlSettings, List<IProgressListener> progressListeners, String compilerClassPath) throws AMLException;

    public abstract ScriptContext getScriptContext();
}