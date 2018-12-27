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
package com.exactpro.sf.scriptrunner.actionmanager.actioncontext.impl;

import java.util.Collections;
import java.util.HashMap;
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
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionReport;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionServiceManager;
import com.exactpro.sf.storage.MessageFilter;
import com.exactpro.sf.storage.MessageRow;
import com.exactpro.sf.util.KnownBugException;
import com.google.common.collect.ImmutableSet;

@SuppressWarnings("deprecation")
public class ActionContextWrapper implements IActionContext{
    private final IActionContext actionContext;

    private CheckPoint checkPoint;
    private MetaContainer metaContainer;
    private long timeout;
    private Set<String> unchekedFields = Collections.emptySet();
    private String reference;
    private String serviceName;
    private IFilter messageCountFilter;
    private IActionReport report;

    private String description;
    private SailfishURI dictionaryURI;
    private MessageCount messageCount;
    private final Map<String, Boolean> negativeMap;
    private final Map<String, Object> systemColumn;

    public ActionContextWrapper(IActionContext actionContext) {
        this.actionContext = actionContext;
        this.checkPoint = actionContext.getCheckPoint();
        this.metaContainer = actionContext.getMetaContainer();
        this.timeout = actionContext.getTimeout();
        this.unchekedFields = actionContext.getUncheckedFields();
        this.reference = actionContext.getReference();
        this.serviceName = actionContext.getServiceName();
        this.messageCountFilter = actionContext.getMessageCountFilter();
        this.report = actionContext.getReport();

        this.description = actionContext.getDescription();
        this.dictionaryURI = actionContext.getDictionaryURI();
        this.messageCount = actionContext.getMessageCount();
        this.negativeMap = new HashMap<>(actionContext.getNegativeMap());
        this.systemColumn = new HashMap<>();
    }

    @Override
    public CheckPoint getCheckPoint() {
        return checkPoint;
    }

    public ActionContextWrapper setCheckPoint(CheckPoint checkPoint) {
        this.checkPoint = checkPoint;
        return this;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public ActionContextWrapper setDescription(String description) {
        this.description = description;
        return this;
    }
    
    @Override
    public SailfishURI getDictionaryURI() {
        return dictionaryURI;
    }
    
    public ActionContextWrapper setDictionaryURI(SailfishURI dictionaryURI) {
        this.dictionaryURI = dictionaryURI;
        return this;
    }

    @Override
    public long getLine() {
        return actionContext.getLine();
    }

    @Override
    public String getId() {
        return actionContext.getId();
    }

    @Override
    public Object getMessage(String reference) {
        return actionContext.getMessage(reference);
    }

    @Override
    public MessageCount getMessageCount() {
        return messageCount;
    }

    public ActionContextWrapper setMessageCount(MessageCount messageCount) {
        this.messageCount = messageCount;
        return this;
    }

    @Override
    public IFilter getMessageCountFilter() {
        return messageCountFilter;
    }

    public ActionContextWrapper setMessageCountFilter(IFilter messageCountFilter) {
        this.messageCountFilter = messageCountFilter;
        return this;
    }

    @Override
    public MetaContainer getMetaContainer() {
        return metaContainer;
    }

    public ActionContextWrapper setMetaContainer(MetaContainer metaContainer) {
        this.metaContainer = metaContainer;
        return this;
    }

    @Override
    public Map<String, Boolean> getNegativeMap() {
        return Collections.unmodifiableMap(negativeMap);
    }

    public ActionContextWrapper setNegativeMap(Map<String, Boolean> values) {
        negativeMap.clear();
        return putNegativeMap(values);
    }
    
    public ActionContextWrapper putNegativeMap(Map<String, Boolean> values) {
        negativeMap.putAll(values);
        return this;
    }
    
    public ActionContextWrapper putNegativeMap(String key, Boolean value) {
        negativeMap.put(key, value);
        return this;
    }
    
    @Override
    public String getReference() {
        return reference;
    }

    public ActionContextWrapper setReference(String reference) {
        this.reference = reference;
        return this;
    }

    @Override
    public IActionReport getReport() {
        return report;
    }

    public void setReport(IActionReport report) {
        this.report = report;
    }

    @Override
    public IActionServiceManager getServiceManager() {
        return actionContext.getServiceManager();
    }

    @Override
    public String getEnvironmentName() {
        return actionContext.getEnvironmentName();
    }

    @Override
    public long getScriptStartTime() {
        return actionContext.getScriptStartTime();
    }

    @Override
    public String getTestCaseName() {
        return actionContext.getTestCaseName();
    }

    @Override
    public synchronized void pauseScript(long timeout, String reason)  throws InterruptedException {
        actionContext.pauseScript(timeout, reason);
    }

    @Override
    public Logger getLogger() {
        return actionContext.getLogger();
    }

    @Override
    public IDictionaryStructure getDictionary(SailfishURI dictionaryURI) throws RuntimeException {
        return actionContext.getDictionary(dictionaryURI);
    }

    @Override
    public Iterable<MessageRow> loadMessages(int count, MessageFilter filter) {
        return actionContext.loadMessages(count, filter);
    }

    @Override
    public void storeMessage(IMessage message) {
        actionContext.storeMessage(message);
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    public ActionContextWrapper setServiceName(ServiceName serviceName) {
        this.serviceName = serviceName.toString();
        return this;
    }

    @Override
    public Set<String> getServicesNames() {
        return actionContext.getServicesNames();
    }

    @Override
    public long getTimeout() {
        return timeout;
    }

    public ActionContextWrapper setTimeout(long timeout) {
        this.timeout = timeout;
        return this;
    }

    @Override
    public boolean isAddToReport() {
        return actionContext.isAddToReport();
    }

    @Override
    public boolean isCheckGroupsOrder() {
        return actionContext.isCheckGroupsOrder();
    }

    @Override
    public boolean isReorderGroups() {
        return actionContext.isReorderGroups();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getSystemColumn(String fieldName) {
        return (T) systemColumn.getOrDefault(fieldName, actionContext.getSystemColumn(fieldName));
    }

    public ActionContextWrapper setSystemColumns(Map<String, Object> values) {
        systemColumn.clear();
        return putSystemColumns(values);
    }
    
    public ActionContextWrapper putSystemColumns(Map<String, Object> values) {
        systemColumn.putAll(values);
        return this;
    }
    
    public ActionContextWrapper putSystemColumn(String key, Object value) {
        systemColumn.put(key, value);
        return this;
    }

    @Override
    public IDataManager getDataManager() {
        return actionContext.getDataManager();
    }

    @Override
    public Set<String> getUncheckedFields() {
        return this.unchekedFields;
    }

    public ActionContextWrapper setUncheckedFields(Set<String> uncheckedFields) {
        this.unchekedFields = ImmutableSet.copyOf(uncheckedFields);
        return this;
    }

    @Override
    public Optional<Object> handleKnownBugException(KnownBugException e, String reference) {
        return actionContext.handleKnownBugException(e, reference);
    }

    @Override
    public ClassLoader getPluginClassLoader(String pluginAlias) {
        return actionContext.getPluginClassLoader(pluginAlias);
    }
}