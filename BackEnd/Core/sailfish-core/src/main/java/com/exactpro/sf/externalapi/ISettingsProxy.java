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
package com.exactpro.sf.externalapi;

import java.util.Set;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.suri.SailfishURI;

/**
 * Provides the way to change settings.
 * @author sergey.smirnov
 */
public interface ISettingsProxy {

    /**
     * Available parameter names
     */
    Set<String> getParameterNames();
    
    /**
     * Gets parameter's class by parameter name.
     * @param name
     * @return null if settings does not contain passed name, otherwise class of object which presented value
     */
    Class<?> getParameterType(String name);
    
    /**
     * Get parameter value by name.
     * @param name
     * @return null if settings does not contain passed name, otherwise actual value
     * @throws EPSCommonException if target type is incorrect
     */
    <T> T getParameterValue(String name);
    
    /**
     * Set value by name.
     * @param name
     * @param value
     * @throws EPSCommonException if passed value has incorrect type,
     *                              or settings does not contain passed name
     */
    void setParameterValue(String name, Object value);
    
    /**
     * Current service dictionary
     * @return
     * @throws UnsupportedOperationException if settings have not dictionary
     */
    SailfishURI getDictionary();

    /**
     * Set current service status.
     * @param dictionary
     * @return
     * @throws UnsupportedOperationException if settings have not dictionary
     */
    void setDictionary(SailfishURI dictionary);

}
