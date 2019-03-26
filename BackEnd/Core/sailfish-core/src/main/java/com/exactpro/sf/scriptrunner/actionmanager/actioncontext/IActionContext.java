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
package com.exactpro.sf.scriptrunner.actionmanager.actioncontext;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;

import com.exactpro.sf.aml.script.CheckPoint;
import com.exactpro.sf.aml.script.MetaContainer;
import com.exactpro.sf.aml.scriptutil.MessageCount;
import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.storage.MessageFilter;
import com.exactpro.sf.storage.MessageRow;
import com.exactpro.sf.util.KnownBugException;

public interface IActionContext {
    CheckPoint getCheckPoint();
    String getDescription();
    SailfishURI getDictionaryURI();
    String getId();
    long getLine();
    Object getMessage(String reference);

    /**
     * @deprecated Used in AML 2 only
     */
    @Deprecated
    MessageCount getMessageCount();
    IFilter getMessageCountFilter();
    MetaContainer getMetaContainer();
    /**
     * @deprecated Used in AML 2 only
     */
    @Deprecated
    Map<String, Boolean> getNegativeMap();
    String getReference();
    IActionReport getReport();
    IActionServiceManager getServiceManager();
    // from ScriptContext
    String getEnvironmentName();
    long getScriptStartTime();
    String getTestCaseName();
    // from DebugController
    void pauseScript(long timeout, String reason) throws InterruptedException;
    // from IScriptConfig
    Logger getLogger();
    // from IDictionaryManager
    IDictionaryStructure getDictionary(SailfishURI dictionaryURI) throws RuntimeException;
    // from IMessageStorage
    Iterable<MessageRow> loadMessages(int count, MessageFilter filter);
    void storeMessage(IMessage message);

    String getServiceName();
    // from ScriptContext
    Set<String> getServicesNames();
    long getTimeout();
    boolean isAddToReport();
    boolean isCheckGroupsOrder();
    boolean isReorderGroups();

    /**
     * @deprecated
     *
     * Use {@link MetaContainer#getSystemColumn(String)} instead.<br>
     * {@link MetaContainer} can be obtained by calling {@link #getMetaContainer()} method.
     */
    @Deprecated
    <T> T getSystemColumn(String name);

    IDataManager getDataManager();

    Set<String> getUncheckedFields();
    ClassLoader getPluginClassLoader(String pluginAlias);

    default Optional<Object> handleKnownBugException(KnownBugException e, String reference) {
        throw new UnsupportedOperationException("Handling known bug exceptions is not supported");
    }
}
