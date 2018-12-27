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
package com.exactpro.sf.storage.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.exactpro.sf.storage.IOptionsStorage;
import com.exactpro.sf.storage.IStorage;
import com.exactpro.sf.storage.entities.StoredOption;

public class DatabaseOptionsStorage implements IOptionsStorage {
	
	private IStorage hbStorage;
	
	public DatabaseOptionsStorage(IStorage hbStorage) {
		
		this.hbStorage = hbStorage;
		
	}

	@Override
	public void setOption(String key, String value) {
		
		StoredOption stored = this.hbStorage.getEntityByField(StoredOption.class, "optionName", key);
		
		if(stored == null) {
			
			stored = new StoredOption(key, value);
			this.hbStorage.add(stored);
			
		} else {
			
			stored.setOptionValue(value);
			this.hbStorage.update(stored);
			
		}
		
	}

	@Override
	public String getOption(String key) {
		
		StoredOption stored = this.hbStorage.getEntityByField(StoredOption.class, "optionName", key);
		
		if(stored != null) {
			return stored.getOptionValue();
		}
		
		return null;
		
	}

	@Override
	public Map<String, String> getAllOptions() {
		
		return optionsToMap(this.hbStorage.getAllEntities(StoredOption.class));
		
	}
	
	private Map<String, String> optionsToMap(Collection<StoredOption> options) {
		
		Map<String, String> result = new HashMap<String, String>();
		
		for(StoredOption option : options) {
			
			result.put(option.getOptionName(), option.getOptionValue());
			
		}
		
		return result;
		
	}

}
