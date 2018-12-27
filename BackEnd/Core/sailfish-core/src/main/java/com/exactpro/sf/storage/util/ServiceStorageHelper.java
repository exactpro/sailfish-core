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
package com.exactpro.sf.storage.util;

import java.beans.PropertyDescriptor;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.DictionarySettings;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.StaticServiceDescription;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIConverter;
import com.exactpro.sf.configuration.suri.SailfishURIRule;
import com.exactpro.sf.scriptrunner.services.IStaticServiceManager;
import com.exactpro.sf.services.DisabledServiceSettings;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ServiceDescription;

public class ServiceStorageHelper {

    private static final Logger logger = LoggerFactory.getLogger(ServiceStorageHelper.class);

    public static void setServiceSettingsToMap(Map<String, String> params, IServiceSettings serviceSettings) {
        if(isDisabledSettings(serviceSettings)) {
            params.putAll(((DisabledServiceSettings) serviceSettings).getSettings());
        } else {
            convertServiceSettingsToMap(params, serviceSettings);
        }
    }

    public static void setMapToServiceSettings(IServiceSettings serviceSettings, Map<String, String> params) {
        if(isDisabledSettings(serviceSettings)) {
            ((DisabledServiceSettings) serviceSettings).setSettings(params);
        } else {
            convertMapToServiceSettings(serviceSettings, params);
        }
    }

    public static void convertServiceSettingsToMap(Map<String, String> params, IServiceSettings serviceSettings) {
        ConvertUtilsBean converter = new ConvertUtilsBean();
        PropertyUtilsBean beanUtils = new PropertyUtilsBean();
        PropertyDescriptor[] descriptors = beanUtils.getPropertyDescriptors(serviceSettings);

        for (PropertyDescriptor descr : descriptors) {
            //check that setter exists
            try {
                if (descr.getWriteMethod() != null) {
                    Object value = BeanUtils.getProperty(serviceSettings, descr.getName());
                    params.put(descr.getName(), converter.convert(value));
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public static void convertMapToServiceSettings(IServiceSettings serviceSettings, Map<String, String> params) {
        ConvertUtilsBean converter = new ConvertUtilsBean();
        PropertyUtilsBean beanUtils = new PropertyUtilsBean();
        PropertyDescriptor[] descriptors = beanUtils.getPropertyDescriptors(serviceSettings);

        converter.register(new SailfishURIConverter(), SailfishURI.class);

        try {
            for (PropertyDescriptor descr : descriptors) {
                //check that setter exists
                if (descr.getWriteMethod() != null) {
                    if (params.containsKey(descr.getName()))
                        BeanUtils.setProperty(serviceSettings, descr.getName(),
                                converter.convert(params.get(descr.getName()), descr.getPropertyType()));
                }
            }
        } catch (Exception e) {
            throw new EPSCommonException(e);
        }
    }

    public static boolean isDisabledSettings(IServiceSettings serviceSettings) {
        return serviceSettings instanceof DisabledServiceSettings;
    }

    //TODO: think of a way to display partial URI to user (usually in case of dictionary name)
    public static ServiceDescription processDescription(ServiceDescription serviceDescription, IStaticServiceManager staticServiceManager, IDictionaryManager dictionaryManager) {
        SailfishURI typeURI = serviceDescription.getType();

        if(!SailfishURIRule.REQUIRE_PLUGIN.check(typeURI)) {
            StaticServiceDescription staticServiceDescription = staticServiceManager.findStaticServiceDescription(typeURI);

            if(staticServiceDescription != null) {
                serviceDescription.setType(staticServiceDescription.getURI());
            }
        }

        IServiceSettings serviceSettings = serviceDescription.getSettings();
        SailfishURI dictionaryURI = serviceSettings.getDictionaryName();

        if(dictionaryURI == null) {
            return serviceDescription;
        }

        if(!SailfishURIRule.REQUIRE_PLUGIN.check(dictionaryURI)) {
            DictionarySettings dictionarySettings = dictionaryManager.getSettings(dictionaryURI);

            if(dictionarySettings != null) {
                serviceSettings.setDictionaryName(dictionarySettings.getURI());
            }
        }

        return serviceDescription;
    }
}
