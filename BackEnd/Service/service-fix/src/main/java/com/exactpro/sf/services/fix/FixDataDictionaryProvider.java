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
package com.exactpro.sf.services.fix;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.suri.SailfishURI;

import quickfix.ConfigError;
import quickfix.DataDictionary;
import quickfix.DataDictionaryProvider;
import quickfix.FieldConvertError;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.field.ApplVerID;

public class FixDataDictionaryProvider implements DataDictionaryProvider {

    private static final Logger logger = LoggerFactory.getLogger(FixDataDictionaryProvider.class);
    
    private final Object configureLock = new Object();
    private final IDictionaryStructure dictionaryStructure;
    private Map<DataDictionarySetting, Boolean> settingMap;
    
    private volatile DataDictionary dataDictionary;
    
	public FixDataDictionaryProvider(IDictionaryManager dictionaryManager, SailfishURI dictionaryURI) {
	    this.dictionaryStructure = dictionaryManager.getDictionary(dictionaryURI);
	}

	public void configure(SessionSettings settings, SessionID sessionID) throws ConfigError, FieldConvertError {
	    synchronized (this.configureLock) {
	        if (this.settingMap != null) {
	            logger.warn("Data dictionary provider already configured {}", this.settingMap);
	        }
	        
	        this.settingMap = new EnumMap<>(DataDictionarySetting.class);
	        
	        for (DataDictionarySetting setting : DataDictionarySetting.values()) {
	            if (settings.isSetting(sessionID, setting.settingName)) {
	                this.settingMap.put(setting, settings.getBool(sessionID, setting.settingName));
	            }
	        }
	    }
	}

	@Override
	public DataDictionary getSessionDataDictionary(String beginString) {
	    return getDictionary();
	}

    @Override
    public DataDictionary getApplicationDataDictionary(ApplVerID applVerID) {
        return getDictionary();
    }
	
	public IDictionaryStructure getDictionaryStructure() {
        return dictionaryStructure;
    }
	
    private DataDictionary getDictionary() {
        DataDictionary localDataDictionary = this.dataDictionary;
        if (localDataDictionary == null) {
            synchronized (this.configureLock) {
                localDataDictionary = this.dataDictionary;
                if (localDataDictionary == null) {
                    this.dataDictionary = localDataDictionary = new QFJDictionaryAdapter(this.dictionaryStructure);

                    if (this.settingMap != null) {
                        for (Entry<DataDictionarySetting, Boolean> entry : this.settingMap.entrySet()) {
                            DataDictionarySetting setting = entry.getKey();
                            boolean flag = entry.getValue();
                            setting.set(localDataDictionary, flag);
                        }
                    } else {
                        logger.warn("Data dictionary provider is not yet configured");
                    }
                }
            }
        }

        return localDataDictionary;
    }
    
    private static enum DataDictionarySetting {
        ALLOW_UNKNOWN_MSG_FIELDS(Session.SETTING_ALLOW_UNKNOWN_MSG_FIELDS) {
            @Override
            public void set(DataDictionary dataDictionary, boolean flag) {
                dataDictionary.setAllowUnknownMessageFields(flag);
            }
        },
        VALIDATE_FIELDS_HAVE_VALUES(Session.SETTING_VALIDATE_FIELDS_HAVE_VALUES) {
            @Override
            public void set(DataDictionary dataDictionary, boolean flag) {
                dataDictionary.setCheckFieldsHaveValues(flag);
            }
        },
        VALIDATE_FIELDS_OUT_OF_ORDER(Session.SETTING_VALIDATE_FIELDS_OUT_OF_ORDER) {
            @Override
            public void set(DataDictionary dataDictionary, boolean flag) {
                dataDictionary.setCheckFieldsOutOfOrder(flag);
            }
        },
        VALIDATE_UNORDERED_GROUP_FIELDS(Session.SETTING_VALIDATE_UNORDERED_GROUP_FIELDS) {
            @Override
            public void set(DataDictionary dataDictionary, boolean flag) {
                dataDictionary.setCheckUnorderedGroupFields(flag);
            }
        },
        VALIDATE_USER_DEFINED_FIELDS(Session.SETTING_VALIDATE_USER_DEFINED_FIELDS) {
            @Override
            public void set(DataDictionary dataDictionary, boolean flag) {
                dataDictionary.setCheckUserDefinedFields(flag);
            }
        };
        
        private final String settingName;
        
        private DataDictionarySetting(String settingName) {
            this.settingName = settingName;
        }
        
        public abstract void set(DataDictionary dataDictionary, boolean flag);
    }
}
