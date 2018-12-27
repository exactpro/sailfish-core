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
    public CheckPoint getCheckPoint();
    public String getDescription();
    public SailfishURI getDictionaryURI();
    public String getId();
    public long getLine();
    public Object getMessage(String reference);

    /**
     * @deprecated Used in AML 2 only
     */
    @Deprecated
    public MessageCount getMessageCount();
    public IFilter getMessageCountFilter();
    public MetaContainer getMetaContainer();
    /**
     * @deprecated Used in AML 2 only
     */
    @Deprecated
    public Map<String, Boolean> getNegativeMap();
    public String getReference();
    public IActionReport getReport();
    public IActionServiceManager getServiceManager();
    // from ScriptContext
    public String getEnvironmentName();
    public long getScriptStartTime();
    public String getTestCaseName();
    // from DebugController
    public void pauseScript(long timeout, String reason) throws InterruptedException;
    // from IScriptConfig
    public Logger getLogger();
    // from IDictionaryManager
    IDictionaryStructure getDictionary(SailfishURI dictionaryURI) throws RuntimeException;
    // from IMessageStorage
    public Iterable<MessageRow> loadMessages(int count, MessageFilter filter);
    public void storeMessage(IMessage message);

    public String getServiceName();
    // from ScriptContext
    public Set<String> getServicesNames();
    public long getTimeout();
    public boolean isAddToReport();
    public boolean isCheckGroupsOrder();
    public boolean isReorderGroups();

    /**
     * @deprecated
     *
     * Use {@link MetaContainer#getSystemColumn(String)} instead.<br>
     * {@link MetaContainer} can be obtained by calling {@link #getMetaContainer()} method.
     */
    @Deprecated
    public <T> T getSystemColumn(String name);

    public IDataManager getDataManager();

    Set<String> getUncheckedFields();
    ClassLoader getPluginClassLoader(String pluginAlias);

    default Optional<Object> handleKnownBugException(KnownBugException e, String reference) {
        throw new UnsupportedOperationException("Handling known bug exceptions is not supported");
    }
}
