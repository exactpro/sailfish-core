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
import java.util.List;
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
import com.exactpro.sf.scriptrunner.actionmanager.IActionCaller;
import com.exactpro.sf.scriptrunner.actionmanager.IActionCaller.ConsumerAction;
import com.exactpro.sf.scriptrunner.actionmanager.IActionCaller.ConsumerActionWithParameters;
import com.exactpro.sf.scriptrunner.actionmanager.IActionCaller.FunctionAction;
import com.exactpro.sf.scriptrunner.actionmanager.IActionCaller.FunctionActionWithParameters;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionReport;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionServiceManager;
import com.exactpro.sf.storage.MessageFilter;
import com.exactpro.sf.storage.MessageRow;
import com.exactpro.sf.util.KnownBugException;
import com.google.common.collect.ImmutableSet;

/**
 * Deprecated and will be removed in future release. To modify existing context use {@link IActionContext}{@code .with*()} methods instead
 */
@Deprecated
@SuppressWarnings("deprecation")
public class ActionContextWrapper implements IActionContext{
    private final IActionContext actionContext;

    private CheckPoint checkPoint;
    private MetaContainer metaContainer;
    private long timeout;
    private Set<String> unchekedFields = Collections.emptySet();
    private Set<String> ignoredFields = Collections.emptySet();
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

    @Override
    public IActionContext withCheckPoint(CheckPoint checkPoint) {
        return actionContext.withCheckPoint(checkPoint);
    }

    @Deprecated
    public ActionContextWrapper setCheckPoint(CheckPoint checkPoint) {
        this.checkPoint = checkPoint;
        return this;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public IActionContext withDescription(String description) {
        return actionContext.withDescription(description);
    }

    @Deprecated
    public ActionContextWrapper setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public SailfishURI getDictionaryURI() {
        return dictionaryURI;
    }

    @Override
    public IActionContext withDictionaryURI(SailfishURI dictionaryURI) {
        return actionContext.withDictionaryURI(dictionaryURI);
    }

    @Deprecated
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
    public IActionContext withId(String id) {
        return actionContext.withId(id);
    }

    @Override
    public Object getMessage(String reference) {
        return actionContext.getMessage(reference);
    }

    @Override
    @Deprecated
    public MessageCount getMessageCount() {
        return messageCount;
    }

    @Deprecated
    public ActionContextWrapper setMessageCount(MessageCount messageCount) {
        this.messageCount = messageCount;
        return this;
    }

    @Override
    public IFilter getMessageCountFilter() {
        return messageCountFilter;
    }

    @Override
    public IActionContext withMessageCountFilter(IFilter messageCountFilter) {
        return actionContext.withMessageCountFilter(messageCountFilter);
    }

    @Deprecated
    public ActionContextWrapper setMessageCountFilter(IFilter messageCountFilter) {
        this.messageCountFilter = messageCountFilter;
        return this;
    }

    @Override
    public MetaContainer getMetaContainer() {
        return metaContainer;
    }

    @Override
    public IActionContext withMetaContainer(MetaContainer metaContainer) {
        return actionContext.withMetaContainer(metaContainer);
    }

    @Deprecated
    public ActionContextWrapper setMetaContainer(MetaContainer metaContainer) {
        this.metaContainer = metaContainer;
        return this;
    }

    @Override
    @Deprecated
    public Map<String, Boolean> getNegativeMap() {
        return Collections.unmodifiableMap(negativeMap);
    }

    @Deprecated
    public ActionContextWrapper setNegativeMap(Map<String, Boolean> values) {
        negativeMap.clear();
        return putNegativeMap(values);
    }

    @Deprecated
    public ActionContextWrapper putNegativeMap(Map<String, Boolean> values) {
        negativeMap.putAll(values);
        return this;
    }

    @Deprecated
    public ActionContextWrapper putNegativeMap(String key, Boolean value) {
        negativeMap.put(key, value);
        return this;
    }

    @Override
    public String getReference() {
        return reference;
    }

    @Override
    public IActionContext withReference(String reference) {
        return actionContext.withReference(reference);
    }

    @Deprecated
    public ActionContextWrapper setReference(String reference) {
        this.reference = reference;
        return this;
    }

    @Override
    public IActionReport getReport() {
        return report;
    }

    @Deprecated
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

    @Override
    public IActionContext withServiceName(String serviceName) {
        return actionContext.withServiceName(serviceName);
    }

    @Deprecated
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

    @Override
    public IActionContext withTimeout(long timeout) {
        return actionContext.withTimeout(timeout);
    }

    @Deprecated
    public ActionContextWrapper setTimeout(long timeout) {
        this.timeout = timeout;
        return this;
    }

    @Override
    public boolean isAddToReport() {
        return actionContext.isAddToReport();
    }

    @Override
    public IActionContext withAddToReport(boolean addToReport) {
        return actionContext.withAddToReport(addToReport);
    }

    @Override
    public boolean isContinueOnFailed() {
        return actionContext.isContinueOnFailed();
    }

    @Override
    public IActionContext withContinueOnFailed(boolean continueOnFailed) {
        return actionContext.withContinueOnFailed(continueOnFailed);
    }

    @Override
    public boolean isCheckGroupsOrder() {
        return actionContext.isCheckGroupsOrder();
    }

    @Override
    public IActionContext withCheckGroupsOrder(boolean checkGroupsOrder) {
        return actionContext.withCheckGroupsOrder(checkGroupsOrder);
    }

    @Override
    public boolean isReorderGroups() {
        return actionContext.isReorderGroups();
    }

    @Override
    public IActionContext withReorderGroups(boolean reorderGroups) {
        return actionContext.withReorderGroups(reorderGroups);
    }

    @Override
    @Deprecated
    @SuppressWarnings("unchecked")
    public <T> T getSystemColumn(String fieldName) {
        return (T) systemColumn.getOrDefault(fieldName, actionContext.getSystemColumn(fieldName));
    }

    @Deprecated
    public ActionContextWrapper setSystemColumns(Map<String, Object> values) {
        systemColumn.clear();
        return putSystemColumns(values);
    }

    @Deprecated
    public ActionContextWrapper putSystemColumns(Map<String, Object> values) {
        systemColumn.putAll(values);
        return this;
    }

    @Deprecated
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
        return unchekedFields;
    }

    @Override
    public IActionContext withUncheckedFields(Set<String> uncheckedFields) {
        return actionContext.withUncheckedFields(uncheckedFields);
    }

    @Override
    public Set<String> getIgnoredFields() {
        return ignoredFields;
    }

    @Override
    public IActionContext withIgnoredFields(Set<String> ignoredFields) {
        return actionContext.withIgnoredFields(ignoredFields);
    }

    @Deprecated
    public ActionContextWrapper setUncheckedFields(Set<String> uncheckedFields) {
        this.unchekedFields = ImmutableSet.copyOf(uncheckedFields);
        return this;
    }

    @Override
    public ClassLoader getPluginClassLoader(String pluginAlias) {
        return actionContext.getPluginClassLoader(pluginAlias);
    }

    @Override
    public Optional<Object> handleKnownBugException(KnownBugException e, String reference) {
        return actionContext.handleKnownBugException(e, reference);
    }

    @Override
    public <T extends IActionCaller> void callAction(T actionClass, ConsumerAction<T> action, String tag, List<String> verificationOrder) {
        throw new UnsupportedOperationException("Wrappers do not support action calling");
    }

    @Override
    public <T extends IActionCaller, R> R callAction(T actionClass, FunctionAction<T, R> action, String tag, List<String> verificationOrder) {
        throw new UnsupportedOperationException("Wrappers do not support action calling");
    }

    @Override
    public <T extends IActionCaller, P> void callAction(T actionClass, ConsumerActionWithParameters<T, P> action, P parameters, String tag, List<String> verificationOrder) {
        throw new UnsupportedOperationException("Wrappers do not support action calling");
    }

    @Override
    public <T extends IActionCaller, P, R> R callAction(T actionClass, FunctionActionWithParameters<T, P, R> action, P parameters, String tag, List<String> verificationOrder) {
        throw new UnsupportedOperationException("Wrappers do not support action calling");
    }
}