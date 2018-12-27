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

import com.exactpro.sf.aml.generator.GeneratedScript;
import com.exactpro.sf.aml.iomatrix.AdvancedMatrixReader;
import com.exactpro.sf.aml.reader.struct.AMLMatrix;
import com.exactpro.sf.common.adapting.IAdapterManager;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.IEnvironmentManager;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.scriptrunner.actionmanager.IActionManager;
import com.exactpro.sf.scriptrunner.languagemanager.LanguageManager;
import com.exactpro.sf.scriptrunner.services.IStaticServiceManager;
import com.exactpro.sf.scriptrunner.utilitymanager.IUtilityManager;

public class JsonAML extends AML {

	private AMLMatrix amlMatrix;

	public JsonAML(AMLSettings settings, IWorkspaceDispatcher workspaceDispatcher, IAdapterManager adapterManager,
			IEnvironmentManager environmentManager, IDictionaryManager dictionaryManager,
			IStaticServiceManager staticServiceManager, LanguageManager languageManager, IActionManager actionManager,
			IUtilityManager utilityManager, String compilerClassPath, AMLMatrix matrix) throws AMLException {
		super(settings, workspaceDispatcher, adapterManager, environmentManager, dictionaryManager,
				staticServiceManager, languageManager, actionManager, utilityManager, compilerClassPath);
		if (matrix == null)
			throw new NullPointerException("matrix can't be null");
		this.amlMatrix = matrix;
	}

	@Override
	protected AdvancedMatrixReader initReader(String file, String fileEncoding) throws IOException {
		throw new UnsupportedOperationException("Not supported in JsonAML");
        }

    @Override
    public GeneratedScript run(ScriptContext scriptContext, String fileEncoding)
            throws AMLException, IOException, InterruptedException {
        return super.run(scriptContext, amlMatrix);
	}
}
