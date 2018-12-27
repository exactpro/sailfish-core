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
package com.exactpro.sf.services;

import java.io.Serializable;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIAdapter;

/**
 * The class defines all characteristics of a service
 *
 */
@SuppressWarnings("serial")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {
    "type",
    "name",
    "serviceHandlerClassName",
    "environment",
    "settings"
})
public class ServiceDescription implements Cloneable, Serializable {
	@XmlElement(name="type")
	@XmlJavaTypeAdapter(SailfishURIAdapter.class)
	private SailfishURI type;
	@XmlElement(name="name")
	private String name;
	@XmlElement(name="serviceHandlerClassName")
	private String serviceHandlerClassName;
	@XmlElement(name="environment")
	private String environment;
	@XmlAnyElement(lax=true)
	private IServiceSettings settings;

	public ServiceDescription()
	{
		// default constructor
	}

	public ServiceDescription(SailfishURI type) {
	    setType(type);
	}

	public IServiceSettings getSettings()
	{
		return this.settings;
	}

	public void setSettings(IServiceSettings settings)
	{
		this.settings = settings;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public SailfishURI getType() {
		return type;
	}

	public void setType(SailfishURI type) {
	    this.type = Objects.requireNonNull(type, "type cannot be null");
	}

	public String getServiceHandlerClassName() {
		return serviceHandlerClassName;
	}

	public void setServiceHandlerClassName(String serviceHandlerClassName) {
		this.serviceHandlerClassName = serviceHandlerClassName;
	}

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	@Override
	public ServiceDescription clone() {
		ServiceDescription cloned = new ServiceDescription(type);

		cloned.setEnvironment(environment);
		cloned.setName(name);
		cloned.setType(type);
		cloned.setServiceHandlerClassName(serviceHandlerClassName);
		cloned.setSettings(settings);

		return cloned;
	}

}
