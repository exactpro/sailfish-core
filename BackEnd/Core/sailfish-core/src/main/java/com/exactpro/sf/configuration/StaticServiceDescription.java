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
package com.exactpro.sf.configuration;

import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.configuration.suri.SailfishURI;

public class StaticServiceDescription {

	private final SailfishURI uri;
	private final ClassLoader classLoader;
	private final IVersion version;
	private final String description;
	private final String className;
	private final String settingsClassName;
	private final SailfishURI dictionaryURI;
	private final String dictionaryValidatorFactoryName;

	public StaticServiceDescription(SailfishURI uri, ClassLoader classLoader, IVersion version, String description, String className,
	        String settingsClassName, SailfishURI dictionaryURI, String dictionaryValidatorFactoryName) {
		this.uri = uri;
		this.classLoader = classLoader;
		this.version = version;
		this.description = description;
		this.className = className;
		this.settingsClassName = settingsClassName;
		this.dictionaryURI = dictionaryURI;
		this.dictionaryValidatorFactoryName = dictionaryValidatorFactoryName;
	}

	public SailfishURI getURI() {
		return uri;
	}

	public ClassLoader getClassLoader() {
		return classLoader;
	}

	public String getDescription() {
		return description;
	}

	public String getClassName() {
		return className;
	}

	public String getSettingsClassName() {
		return settingsClassName;
	}

	public SailfishURI getDictionaryURI() {
		return dictionaryURI;
	}

    public String getDictionaryValidatorFactoryName() {
        return dictionaryValidatorFactoryName;
    }

    public IVersion getVersion() {
        return version;
    }
}