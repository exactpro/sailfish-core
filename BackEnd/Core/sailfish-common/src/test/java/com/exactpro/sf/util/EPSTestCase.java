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
package com.exactpro.sf.util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.log4j.PropertyConfigurator;

public class EPSTestCase
{
	private static boolean isLoggingAlreadyConfigured = false;

	protected static final String BIN_FOLDER_PATH = "build/test-results";
	protected static final Path BASE_DIR = Paths.get((System.getProperty("basedir") == null) ? "." : System.getProperty("basedir")).toAbsolutePath().normalize();

	public EPSTestCase()
	{
		synchronized ( EPSTestCase.class )
		{
			if ( isLoggingAlreadyConfigured == false )
			{
				String configFile = getBaseDir() + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator + "log.properties";

				PropertyConfigurator.configure(configFile);
				isLoggingAlreadyConfigured = true;
			}

		}

	}


	protected String getBaseDir()
	{
		return BASE_DIR.toString();
	}

}
