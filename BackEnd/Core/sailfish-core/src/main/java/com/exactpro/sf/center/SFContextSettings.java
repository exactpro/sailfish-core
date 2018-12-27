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
package com.exactpro.sf.center;

import org.apache.commons.configuration.HierarchicalConfiguration;

public class SFContextSettings {
    private static final String ENVIRONMENT_KEY = "Environment";
    private static final String LOGGING_KEY = "Logging";

    private HierarchicalConfiguration config;
	private String compilerClassPath;
	
	private String version;
	private String branchName;
    private boolean authEnabled;

    public HierarchicalConfiguration getEnvironmentConfig() {
        if (config.configurationsAt(ENVIRONMENT_KEY).isEmpty()) {
            config.getRootNode().addChild(new HierarchicalConfiguration.Node(ENVIRONMENT_KEY));
        }
        return config.configurationAt(ENVIRONMENT_KEY);
    }

    public HierarchicalConfiguration getLoggingConfig() {
        if (config.configurationsAt(LOGGING_KEY).isEmpty()) {
            config.getRootNode().addChild(new HierarchicalConfiguration.Node(LOGGING_KEY));
        }
        return config.configurationAt(LOGGING_KEY);
    }

    public void setConfig(HierarchicalConfiguration config) {
        this.config = config;
    }

	public String getCompilerClassPath() {
		return compilerClassPath;
	}

	public void setCompilerClassPath(String compilerClassPath) {
		this.compilerClassPath = compilerClassPath;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getBranchName() {
		return branchName;
	}

	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}

    public boolean isAuthEnabled() {
        return authEnabled;
    }

    public void setAuthEnabled(boolean authEnabled) {
        this.authEnabled = authEnabled;
	}
}
