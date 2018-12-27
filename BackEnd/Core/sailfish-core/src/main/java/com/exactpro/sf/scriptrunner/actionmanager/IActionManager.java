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
package com.exactpro.sf.scriptrunner.actionmanager;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.exactpro.sf.common.impl.messages.BaseMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.scriptrunner.actionmanager.exceptions.ActionCallException;
import com.exactpro.sf.scriptrunner.actionmanager.exceptions.ActionNotFoundException;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityInfo;

public interface IActionManager {
    /**
     * Calls action by specified URI
     *
     * @param uri               action URI (must contain at least class alias and resource name)
     * @param actionContext     action context
     *
     * @return action call result
     */
    <T> T call(SailfishURI uri, IActionContext actionContext) throws ActionCallException, ActionNotFoundException, InterruptedException;

    /**
     * Calls action by specified URI
     *
     * @param uri               action URI (must contain at least class alias and resource name)
     * @param actionContext     action context
     * @param message           message for action to work with
     *
     * @return action call result
     */
    <T> T call(SailfishURI uri, IActionContext actionContext, IMessage message) throws ActionCallException, ActionNotFoundException, InterruptedException;

    /**
     * Calls action by specified URI
     *
     * @param uri               action URI (must contain at least class alias and resource name)
     * @param actionContext     action context
     * @param message           message for action to work with
     *
     * @return action call result
     */
    <T> T call(SailfishURI uri, IActionContext actionContext, BaseMessage message) throws ActionCallException, ActionNotFoundException, InterruptedException;

    /**
     * Calls action by specified URI
     *
     * @param uri               action URI (must contain at least class alias and resource name)
     * @param actionContext     action context
     * @param message           message for action to work with
     *
     * @return action call result
     */
    <T> T call(SailfishURI uri, IActionContext actionContext, Object message) throws ActionCallException, ActionNotFoundException, InterruptedException;

    /**
     * Calls action by specified URI
     *
     * @param uri               action URI (must contain at least class alias and resource name)
     * @param actionContext     action context
     * @param map               action parameters map
     *
     * @return action call result
     */
    <T> T call(SailfishURI uri, IActionContext actionContext, HashMap<?, ?> map) throws ActionCallException, ActionNotFoundException, InterruptedException;

    /**
     * Destructs all instances of action callers for current thread
     */
    void reset();

    /**
     * Retrieves action infos by specified URI
     *
     * @param uri           action URI (must contain at least resource name)
     *
     * @return set of action infos
     */
    Set<ActionInfo> getActionInfos(SailfishURI uri);

    /**
     * Retrieves action info by specified URI and language
     *
     * @param uri           action URI (must contain at least resource name)
     * @param languageURI   action language
     *
     * @return action info or {@code null}
     */
    ActionInfo getActionInfo(SailfishURI uri, SailfishURI languageURI);

    /**
     * Retrieves action infos by action class URI
     *
     * @param uri           action class URI (must contain at least plugin alias and class alias)
     *
     * @return set of action infos
     */
    Set<ActionInfo>  getActionInfosByClass(SailfishURI uri);

    /**
     * Retrieves action info by specified URI and language
     *
     * @param uri           action URI (must contain at least resource name)
     *
     * @return action info or {@code null}
     */
    ActionInfo getActionInfo(SailfishURI uri);

    /**
     * Checks if action with specified URI and language exists
     *
     * @param uri           action URI (must contain at least resource name)
     * @param languageURI   action language
     *
     * @return {@code true} if action exists, {@code false} otherwise
     */
    boolean containsAction(SailfishURI uri, SailfishURI languageURI);

    /**
     * Checks if action with specified URI exists
     *
     * @param uri           action URI (must contain at least resource name)
     *
     * @return {@code true} if action exists, {@code false} otherwise
     */
    boolean containsAction(SailfishURI uri);

    /**
     * Retrieves utility info by specified utility URI and argument types.
     * Search is scoped by utility classes assigned to a class specified by class URI.
     * If utility URI is absolute then search isn't scoped.
     *
     * @param classURI      class URI (must contain at least class alias)
     * @param utilityURI    utility URI (must contain at least resource name)
     * @param argTypes      utility argument types
     *
     * @return utility info or {@code null}
     */
    UtilityInfo getUtilityInfo(SailfishURI classURI, SailfishURI utilityURI, Class<?>... argTypes) throws SailfishURIException;

    Set<SailfishURI> getUtilityURIs(SailfishURI actionURI);

    /**
     * Checks if utility with specified URI and argument types exists
     * Search is scoped by utility classes assigned to a class specified by class URI.
     * If utility URI is absolute then search isn't scoped.
     *
     * @param classURI      class URI (must contain at least class alias)
     * @param utilityURI    utility URI (must contain at least resource name)
     * @param argTypes      utility argument types
     *
     * @return {@code true} if utility exists, {@code false} otherwise
     */
    boolean containsUtility(SailfishURI classURI, SailfishURI utilityURI, Class<?>... argTypes) throws SailfishURIException;

    /**
     * Retrieves action class by specified name
     *
     * @param className action class name
     *
     * @return action class or {@code null}
     */
    ActionClass getActionClassByName(String className);

    /**
     * Retrieves action class by specified URI
     *
     * @param uri action class URI (must contain at least class alias)
     *
     * @return action class or {@code null}
     */
    ActionClass getActionClassByURI(SailfishURI uri);

    /**
     * Returns list of action classes
     */
    List<ActionClass> getActionClasses();

    List<ActionInfo> getActionInfos();

    /**
     * Returns unmodifiable set of action class uris
     */
    Set<SailfishURI> getActionClassURIs();
}
