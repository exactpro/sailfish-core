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
package com.exactpro.sf.testwebgui.restapi.editor;

import com.exactpro.sf.aml.AMLBlockType;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.scriptrunner.StatusDescription;
import com.exactpro.sf.scriptrunner.impl.DefaultScriptReport;

public class GuiMatrixEditorScriptRunListener extends DefaultScriptReport {

	private final CollectorReceivedMessageProvider provider = new CollectorReceivedMessageProvider();

	private ScriptContext context;

	@Override
	public void createReport(ScriptContext scriptContext, String name, String description, long scriptRunId,
			String environmentName, String userName) {
		this.context = scriptContext;
	}

	@Override
    public void createTestCase(String name, String description, int order, int matrixOrder, String tcId, int tcHash, AMLBlockType type) {
		provider.startRecording(context, name);
	}

	@Override
	public void closeTestCase(StatusDescription status) {
		provider.stopRecording(context);
	}

	public CollectorReceivedMessageProvider getProvider() {
		return provider;
	}

}
