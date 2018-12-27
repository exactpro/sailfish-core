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
package com.exactpro.sf.configuration.netdumper;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;

public class NetDumperOptions implements Cloneable {

	private final static String STORAGE_PREFIX = "netdumper.";
	
    private String rootHost = "0.0.0.0";
    private int rootPort = 6000;
	private boolean enabled;

	public void fillFromMap(Map<String, String> options) throws Exception {
        for (Map.Entry<String, String> entry : options.entrySet()) {
            if (entry.getKey().startsWith(STORAGE_PREFIX)) {
				BeanUtils.setProperty(this, entry.getKey().replace(STORAGE_PREFIX, ""), entry.getValue());
			}
		}
	}
	
    @SuppressWarnings("unchecked")
	public Map<String, String> toMap() throws Exception {

		Map<String, String> description = BeanUtils.describe(this);
        Map<String, String> result = new HashMap<>();
		
        for (Map.Entry<String, String> entry : description.entrySet()) {
            if (!entry.getKey().equals("class")) {
				result.put(STORAGE_PREFIX + entry.getKey(), entry.getValue());
			}
		}
		
		return result;
	}
	
    public String getRootUrl() {

        StringBuilder builder = new StringBuilder();

        builder.append("http://")
                .append(this.rootHost)
                .append(":")
                .append(this.rootPort);

        return builder.toString();
	}

    public NetDumperOptions clone() {

        NetDumperOptions options = new NetDumperOptions();

        options.setEnabled(this.enabled);
        options.setRootHost(this.rootHost);
        options.setRootPort(this.rootPort);

        return options;
    }

    public String getRootHost() {
        return rootHost;
    }

    public void setRootHost(String rootHost) {
        this.rootHost = rootHost;
    }

    public int getRootPort() {
        return rootPort;
    }

    public void setRootPort(int rootPort) {
        this.rootPort = rootPort;
    }

    public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
