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
package com.exactpro.sf.embedded.statistics.configuration;

import java.util.Map;

import com.exactpro.sf.embedded.configuration.AbstractHibernateServiceSettings;
import com.exactpro.sf.embedded.storage.HibernateStorageSettings;

@SuppressWarnings("serial")
public class StatisticsServiceSettings extends AbstractHibernateServiceSettings {

	private static final String SETTINGS_NAME = "statistics";
	
	private static final String STORAGE_PREFIX = "statistics.";
	
	private String thisSfHost; // No need to store in DB
	
	private String thisSfName; // No need to store in DB
	
	private String thisSfPort; // No need to store in DB
	
	public StatisticsServiceSettings() {
		super(new HibernateStorageSettings("statistics.db."));
	}
	
	public StatisticsServiceSettings(StatisticsServiceSettings toClone) {
		super(new HibernateStorageSettings(toClone.getStorageSettings()));
	    
		this.serviceEnabled = toClone.isServiceEnabled();
		
		this.thisSfHost = toClone.getThisSfHost();
		this.thisSfName = toClone.getThisSfName();
		this.thisSfPort = toClone.getThisSfPort();
		
	}

    @Override
    public String settingsName() {
        return SETTINGS_NAME;
    }
	
	public String getThisSfHost() {
		return thisSfHost;
	}

	public void setThisSfHost(String thisSfHost) {
		this.thisSfHost = thisSfHost;
	}

	public String getThisSfName() {
		return thisSfName;
	}

	public void setThisSfName(String thisSfName) {
		this.thisSfName = thisSfName;
	}

	public String getThisSfPort() {
		return thisSfPort;
	}

	public void setThisSfPort(String thisSfPort) {
		this.thisSfPort = thisSfPort;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("StatisticsServiceSettings [statisticsServiceEnabled=");
		builder.append(serviceEnabled);
		builder.append(", thisSfHost=");
		builder.append(thisSfHost);
		builder.append(", thisSfName=");
		builder.append(thisSfName);
		builder.append(", thisSfPort=");
		builder.append(thisSfPort);
		builder.append(", storageSettings=");
		builder.append(storageSettings);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (serviceEnabled ? 1231 : 1237);
		result = prime * result
				+ ((storageSettings == null) ? 0 : storageSettings.hashCode());
		result = prime * result
				+ ((thisSfHost == null) ? 0 : thisSfHost.hashCode());
		result = prime * result
				+ ((thisSfName == null) ? 0 : thisSfName.hashCode());
		result = prime * result
				+ ((thisSfPort == null) ? 0 : thisSfPort.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StatisticsServiceSettings other = (StatisticsServiceSettings) obj;
		if (serviceEnabled != other.serviceEnabled)
			return false;
		if (storageSettings == null) {
			if (other.storageSettings != null)
				return false;
		} else if (!storageSettings.equals(other.storageSettings))
			return false;
		if (thisSfHost == null) {
			if (other.thisSfHost != null)
				return false;
		} else if (!thisSfHost.equals(other.thisSfHost))
			return false;
		if (thisSfName == null) {
			if (other.thisSfName != null)
				return false;
		} else if (!thisSfName.equals(other.thisSfName))
			return false;
		if (thisSfPort == null) {
			if (other.thisSfPort != null)
				return false;
		} else if (!thisSfPort.equals(other.thisSfPort))
			return false;
		return true;
	}

    @Override
    protected String getStoragePrefix() {
        return STORAGE_PREFIX;
    }
}