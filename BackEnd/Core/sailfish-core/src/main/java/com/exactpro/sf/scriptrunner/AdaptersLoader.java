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
package com.exactpro.sf.scriptrunner;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;

import com.exactpro.sf.configuration.AdapterDescription;

public class AdaptersLoader {

	private final static String SETTINGS_ADAPTERS_KEY = "Adapters";
	private final static String SETTINGS_ADAPTER_KEY = "Adapter";

	private final List<AdapterDescription> adapters = new LinkedList<>();

	public void load(HierarchicalConfiguration config) {
		loadAdaptersSettings(config.configurationAt(SETTINGS_ADAPTERS_KEY));
	}

	private void loadAdaptersSettings(HierarchicalConfiguration config) {
		List<?> list = config.configurationsAt(SETTINGS_ADAPTER_KEY);

		for (Iterator<?> it = list.iterator(); it.hasNext();) {
			HierarchicalConfiguration sub = (HierarchicalConfiguration) it.next();

			AdapterDescription adapterDescription = new AdapterDescription();
			adapterDescription.load(sub);

			this.adapters.add(adapterDescription);
		}

	}

	public List<AdapterDescription> getAdapters() {
		return this.adapters;
	}

}
