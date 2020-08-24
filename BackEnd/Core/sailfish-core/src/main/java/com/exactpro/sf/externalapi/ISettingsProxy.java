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
     * Set of dictionary types required by service
     */
    Set<DictionaryType> getDictionaryTypes();

    /**
     * Main service dictionary
     * @return dictionary URI
     * @throws UnsupportedOperationException if settings have not dictionary
     * @deprecated Use {@link #getDictionary(DictionaryType) getDictionary(DictionaryType.MAIN)} instead
     */
    @Deprecated
    default SailfishURI getDictionary() {
        return getDictionary(DictionaryType.MAIN);
    }

    /**
     * Returns URI of a dictionary with the specified type
     * @param dictionaryType type of service dictionary
     * @return dictionary URI
     */
    SailfishURI getDictionary(DictionaryType dictionaryType);

    /**
     * Sets main dictionary
     * @param dictionary dictionary URI
     * @throws UnsupportedOperationException if settings have not dictionary
     * @deprecated Use {@link #setDictionary(DictionaryType, SailfishURI) setDictionary(DictionaryType.MAIN, dictionaryUri)} instead
     */
    @Deprecated
    default void setDictionary(SailfishURI dictionary) {
        setDictionary(DictionaryType.MAIN, dictionary);
    }

    /**
     * Sets dictionary with the specified type
     * @param dictionaryType dictionary type
     * @param dictionaryUri dictionary URI
     */
    void setDictionary(DictionaryType dictionaryType, SailfishURI dictionaryUri);
}
