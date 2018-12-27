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
package com.exactpro.sf.embedded.configuration;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;

import com.exactpro.sf.embedded.storage.HibernateStorageSettings;
import com.exactpro.sf.storage.IMapableSettings;

public abstract class AbstractHibernateServiceSettings implements IMapableSettings, Serializable {

    private static final long serialVersionUID = 6465531653053776115L;
    
    protected final HibernateStorageSettings storageSettings;
    
    protected boolean serviceEnabled = false;
    
    protected String testQuery;
    
    public AbstractHibernateServiceSettings(HibernateStorageSettings storageSettings) {
        this.storageSettings = storageSettings;
    }
    
    @Override
    public void fillFromMap(Map<String, String> options) throws Exception {
        this.storageSettings.fillFromMap(options);
        this.serviceEnabled = BooleanUtils.toBoolean(options.get(getStoragePrefix() + "serviceEnabled"));
        this.testQuery = options.get(getStoragePrefix() + "testQuery");
    }
    
    @Override
    public Map<String, String> toMap() throws Exception {
        Map<String, String> result = this.storageSettings.toMap();
        result.put(getStoragePrefix() + "serviceEnabled", Boolean.toString(serviceEnabled));
        result.put(getStoragePrefix() + "testQuery", testQuery);
        return result;
        
    }
    
    protected abstract String getStoragePrefix();
    
    public HibernateStorageSettings getStorageSettings() {
        return storageSettings;
    }
    
    public boolean isServiceEnabled() {
        return serviceEnabled;
    }
    
    public void setServiceEnabled(boolean serviceEnabled) {
        this.serviceEnabled = serviceEnabled;
    }

    public String getTestQuery() {
        return testQuery;
    }

    public void setTestQuery(String testQuery) {
        this.testQuery = testQuery;
    }
}
