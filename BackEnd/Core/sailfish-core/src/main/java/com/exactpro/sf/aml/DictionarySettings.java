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
package com.exactpro.sf.aml;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.configuration.suri.SailfishURI;

@SuppressWarnings("serial")
public class DictionarySettings implements Serializable {

    private SailfishURI uri;
	private Class<? extends IMessageFactory> factoryClass;
	private Set<SailfishURI> utilityClassURIs;

	public DictionarySettings() {
	    this.utilityClassURIs = new HashSet<>();
	}

	public DictionarySettings(DictionarySettings settings) {
	    this.uri = settings.uri;
		this.factoryClass = settings.factoryClass;
		this.utilityClassURIs = new HashSet<>();
	}

    public void addUtilityClassURI(SailfishURI uri) {
	    utilityClassURIs.add(uri);
	}

	public Set<SailfishURI> getUtilityClassURIs() {
	    return Collections.unmodifiableSet(utilityClassURIs);
	}

	public SailfishURI getURI() {
        return uri;
    }

    public void setURI(SailfishURI uri) {
        this.uri = uri;
    }

	public Class<? extends IMessageFactory> getFactoryClass() {
		return factoryClass;
	}

	public void setFactoryClass(Class<? extends IMessageFactory> factoryClass) {
		this.factoryClass = factoryClass;
	}

}
