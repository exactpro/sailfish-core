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
package com.exactpro.sf.storage.entities;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.exactpro.sf.configuration.suri.SailfishURIUtils;
import com.exactpro.sf.storage.impl.AbstractPersistentObject;

public class StoredService extends AbstractPersistentObject {

	private String name;
	private String type;
	private String serviceHandlerClassName;
	private Map<String, String> parameters = new HashMap<String, String>();
	private StoredEnvironment environment;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
	    // sanitize value in case of loading old settings
		this.type = SailfishURIUtils.sanitize(type);
	}

	public String getServiceHandlerClassName() {
		return serviceHandlerClassName;
	}

	public void setServiceHandlerClassName(String serviceHandlerClassName) {
		this.serviceHandlerClassName = serviceHandlerClassName;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	public StoredEnvironment getEnvironment() {
		return environment;
	}

	public void setEnvironment(StoredEnvironment environment) {
		this.environment = environment;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).
				append("name", name).
				append("type", type).
				append("handler", serviceHandlerClassName).
				append("settings", parameters).
				append("environment", (null != environment) ? environment.getName() : "").
				toString();
	}

}
