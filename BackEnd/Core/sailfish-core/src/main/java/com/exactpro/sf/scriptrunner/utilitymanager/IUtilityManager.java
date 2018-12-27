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
package com.exactpro.sf.scriptrunner.utilitymanager;

import java.util.List;
import java.util.Set;

import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.utilitymanager.exceptions.UtilityCallException;
import com.exactpro.sf.scriptrunner.utilitymanager.exceptions.UtilityNotFoundException;

public interface IUtilityManager {
    /**
     * Retrieves instance of utility caller by specified URI
     *
     * @param uri utility caller URI (must contain at least class alias)
     *
     * @return utility caller instance or {@code null}
     */
    IUtilityCaller getInstance(SailfishURI uri);

    /**
     * Calls utility by specified URI
     *
     * @param uri   utility URI (must contain at least class alias and resource name)
     * @param args  utility arguments
     */
    <T> T call(SailfishURI uri, Object... args) throws UtilityCallException, UtilityNotFoundException, InterruptedException;

    /**
     * Destructs all instances of utility callers for current thread
     */
    void reset();

    /**
     * Retrieves utility info by specified URI and argument types
     *
     * @param uri       utility URI (must contain at least resource name)
     * @param argTypes  utility argument types
     *
     * @return utility info or {@code null}
     */
    UtilityInfo getUtilityInfo(SailfishURI uri, Class<?>... argTypes);

    /**
     * @param utilityURI
     * @return {@code Set} of UtilityInfo for all overloads of the given utility function URI
     */
    Set<UtilityInfo> getUtilityInfos(SailfishURI utilityURI);

    /**
     * Checks if utility with specified URI and argument types exists
     *
     * @return {@code true} if utility exists, {@code false} otherwise
     */
    boolean containsUtility(SailfishURI uri, Class<?>... argTypes);

    /**
     * Retrieves utility class by specified name
     *
     * @param className utility class name
     *
     * @return utility class of {@code null}
     */
    UtilityClass getUtilityClassByName(String className);

    /**
     * Retrieves utility class by specified URI
     *
     * @param uri utility class URI (must contain at least class alias)
     *
     * @return utility class of {@code null}
     */
    UtilityClass getUtilityClassByURI(SailfishURI uri);

    /**
     * Returns list of utility classes
     */
    List<UtilityClass> getUtilityClasses();

    /**
     * Returns list of utility SURIs
     */
    List<SailfishURI> getUtilityURIs();
}