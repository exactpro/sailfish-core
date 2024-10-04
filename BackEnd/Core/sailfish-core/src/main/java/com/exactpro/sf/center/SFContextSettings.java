/******************************************************************************
 * Copyright 2009-2024 Exactpro (Exactpro Systems Limited)
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

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;

import static com.exactpro.sf.util.Configuration2Utils.addChildNodeAndUpdate;
import static com.exactpro.sf.util.Configuration2Utils.createNode;

public class SFContextSettings {
    private static final String ENVIRONMENT_KEY = "Environment";
    private static final String LOGGING_KEY = "Logging";
    private static final String UPDATER_KEY = "Update";
    private static final String CLEANUP_KEY = "Cleanup";

    private HierarchicalConfiguration<ImmutableNode> config;
	private String compilerClassPath;
	
	private String version;
	private String branchName;
    private boolean authEnabled;

    public HierarchicalConfiguration<ImmutableNode> getEnvironmentConfig() {
        if (config.configurationsAt(ENVIRONMENT_KEY).isEmpty()) {
			addChildNodeAndUpdate(config, createNode(ENVIRONMENT_KEY));
        }
        return config.configurationAt(ENVIRONMENT_KEY);
    }

    public HierarchicalConfiguration<ImmutableNode> getLoggingConfig() {
        if (config.configurationsAt(LOGGING_KEY).isEmpty()) {
			addChildNodeAndUpdate(config, createNode(LOGGING_KEY));
        }
        return config.configurationAt(LOGGING_KEY);
    }

    public HierarchicalConfiguration<ImmutableNode> getUpdateServiceConfiguration() {
        if (config.configurationsAt(UPDATER_KEY).isEmpty()) {
			addChildNodeAndUpdate(config, createNode(UPDATER_KEY));
        }
        return config.configurationAt(UPDATER_KEY);
    }

    public HierarchicalConfiguration<ImmutableNode> getCleanupConfig() {
        if (config.configurationsAt(CLEANUP_KEY).isEmpty()) {
            addChildNodeAndUpdate(config, createNode(CLEANUP_KEY));
        }
        return config.configurationAt(CLEANUP_KEY);
    }

    public void setConfig(HierarchicalConfiguration<ImmutableNode> config) {
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
