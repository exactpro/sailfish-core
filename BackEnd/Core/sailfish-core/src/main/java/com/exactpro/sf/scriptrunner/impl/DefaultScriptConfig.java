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
package com.exactpro.sf.scriptrunner.impl;

import java.io.IOException;
import java.util.Set;

import org.slf4j.Logger;

import com.exactpro.sf.scriptrunner.IScriptConfig;
import com.exactpro.sf.scriptrunner.ScriptSettings;

public class DefaultScriptConfig implements IScriptConfig
{
	private final ScriptSettings scriptSettings;
	private final String reportFolder;
	private final String description;
	private final Logger scriptLogger;

	public DefaultScriptConfig(ScriptSettings settings, String reportFolder, String description, Logger scriptLogger) throws IOException
	{
		if ( settings == null ) {
			throw new NullPointerException("settings can't be null");
		}

		if (reportFolder == null) {
			throw new NullPointerException("reportFolder can't be null");
		}

		this.scriptSettings = settings;

		this.description = description;

		this.reportFolder = reportFolder;

		this.scriptLogger = scriptLogger;
	}

	@Override
	public String getName()
	{
		return this.scriptSettings.getScriptName();
	}

	@Override
	public Set<String> getPropertiesKeys()
	{
		return this.scriptSettings.getPropertiesKeys();
	}

	@Override
	public String getProperty(String name)
	{
		return this.scriptSettings.getProperty(name);
	}

	@Override
	public String getReportFolder()
	{
		return this.reportFolder;
	}

	@Override
	public Logger getLogger()
	{
		return this.scriptLogger;
	}

	@Override
	public boolean isAddMessagesToReport() {
		return this.scriptSettings.isAddMessagesToReport();
	}

	@Override
	public String getDescription() {
		return this.description;
	}
}
