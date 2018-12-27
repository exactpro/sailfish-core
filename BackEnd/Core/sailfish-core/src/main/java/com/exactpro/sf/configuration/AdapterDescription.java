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

import org.apache.commons.configuration.HierarchicalConfiguration;

import com.exactpro.sf.common.util.ICommonSettings;

public class AdapterDescription implements ICommonSettings
{
	private String adapterForClass;
	private String adapterClass;
	private String adapterClassImpl;
	
	
	@Override
	public void load(HierarchicalConfiguration config) 
	{
		this.adapterForClass = config.getString("AdapterForClass");
	    this.adapterClass = config.getString("AdapterClass");
	    this.adapterClassImpl = config.getString("AdapterClassImpl");
	}
	
	
	public String getAdapterForClass() {
		return adapterForClass;
	}


	public void setAdapterForClass(String adapterForClass) {
		this.adapterForClass = adapterForClass;
	}


	public String getAdapterClass() {
		return adapterClass;
	}


	public void setAdapterClass(String adapterClass) {
		this.adapterClass = adapterClass;
	}


	public String getAdapterClassImpl() {
		return adapterClassImpl;
	}


	public void setAdapterClassImpl(String adapterClassImpl) {
		this.adapterClassImpl = adapterClassImpl;
	}

}
