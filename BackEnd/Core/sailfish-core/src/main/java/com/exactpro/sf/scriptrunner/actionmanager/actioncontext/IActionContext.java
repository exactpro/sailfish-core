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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;

import com.exactpro.sf.aml.script.ActionCallResult;
import com.exactpro.sf.aml.script.CheckPoint;
import com.exactpro.sf.aml.script.MetaContainer;
import com.exactpro.sf.aml.scriptutil.MessageCount;
import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.actionmanager.IActionCaller;
import com.exactpro.sf.scriptrunner.actionmanager.IActionCaller.ConsumerAction;
import com.exactpro.sf.scriptrunner.actionmanager.IActionCaller.ConsumerActionWithParameters;
import com.exactpro.sf.scriptrunner.actionmanager.IActionCaller.FunctionAction;
import com.exactpro.sf.scriptrunner.actionmanager.IActionCaller.FunctionActionWithParameters;
import com.exactpro.sf.storage.MessageFilter;
import com.exactpro.sf.storage.MessageRow;
import com.exactpro.sf.util.KnownBugException;

public interface IActionContext {
    CheckPoint getCheckPoint();

    IActionContext withCheckPoint(CheckPoint checkPoint);

    String getDescription();

    IActionContext withDescription(String description);

    SailfishURI getDictionaryURI();

    IActionContext withDictionaryURI(SailfishURI dictionaryURI);

    String getId();

    IActionContext withId(String id);

    long getLine();

    Object getMessage(String reference);

    /**
     * @deprecated Used in AML 2 only
     */
    @Deprecated
    MessageCount getMessageCount();

    IFilter getMessageCountFilter();

    IActionContext withMessageCountFilter(IFilter messageCountFilter);

    MetaContainer getMetaContainer();

    IActionContext withMetaContainer(MetaContainer metaContainer);

    /**
     * @deprecated Used in AML 2 only
     */
    @Deprecated
    Map<String, Boolean> getNegativeMap();

    String getReference();

    IActionContext withReference(String reference);

    /**
     * @return last opened report (e.g. embedded report)
     */
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

    IActionContext withServiceName(String serviceName);

    // from ScriptContext
    Set<String> getServicesNames();

    long getTimeout();

    IActionContext withTimeout(long timeout);

    boolean isAddToReport();

    IActionContext withAddToReport(boolean addToReport);

    boolean isContinueOnFailed();

    IActionContext withContinueOnFailed(boolean continueOnFailed);

    boolean isCheckGroupsOrder();

    IActionContext withCheckGroupsOrder(boolean checkGroupsOrder);

    boolean isReorderGroups();

    IActionContext withReorderGroups(boolean reorderGroups);

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

    IActionContext withUncheckedFields(Set<String> uncheckedFields);

    Set<String> getIgnoredFields();

    IActionContext withIgnoredFields(Set<String> ignoredFields);

    ClassLoader getPluginClassLoader(String pluginAlias);

    default Optional<Object> handleKnownBugException(KnownBugException e, String reference) {
        throw new UnsupportedOperationException("Handling known bug exceptions is not supported");
    }

    /**
     * Executes action, performs necessary exception handling and adds it to report (optional).<br>
     * <br>
     * Example:
     * <pre>{@code
     * TestActions testActions = new TestActions();
     * IActionContext contextCopy = actionContext.withId("actionId")
     *         .withAddToReport(true)
     *         .withContinueOnFailed(false)
     *         .withServiceName("default@service")
     *         .withDescription("Action description")
     *         .withCheckPoint(checkPoint);
     *
     * contextCopy.callAction(testActions, TestActions::reconnectService, "actionTag", Collections.emptyList());
     * }</pre>
     *
     * @param actionClass instance of action class
     * @param action action method reference
     * @param tag action tag
     * @param verificationOrder verification order for report
     * @return instance of {@link ActionCallResult} containing action status, known bugs and exception (if present)
     * @throws Throwable action's exception if it's failed and {@link #isContinueOnFailed()} = false
     */
    <T extends IActionCaller> ActionCallResult<Void> callAction(T actionClass, ConsumerAction<T> action, String tag, List<String> verificationOrder) throws Exception;

    /**
     * Executes action, performs necessary exception handling and adds it to report (optional).<br>
     * <br>
     * Example:
     * <pre>{@code
     * CommonActions commonActions = new CommonActions();
     * IActionContext contextCopy = actionContext.withId("actionId")
     *         .withAddToReport(true)
     *         .withContinueOnFailed(false)
     *         .withServiceName("default@service")
     *         .withDescription("Action description");
     *
     * contextCopy.callAction(commonActions, CommonActions::GetCheckPoint, "actionTag", Collections.emptyList());
     * }</pre>
     *
     * @param actionClass instance of action class
     * @param action action method reference
     * @param tag action tag
     * @param verificationOrder verification order for report
     * @return instance of {@link ActionCallResult} containing action result, status, known bugs and exception (if present)
     * @throws Throwable action's exception if it's failed and {@link #isContinueOnFailed()} = false
     */
    <T extends IActionCaller, R> ActionCallResult<R> callAction(T actionClass, FunctionAction<T, R> action, String tag, List<String> verificationOrder) throws Exception;

    /**
     * Executes action, performs necessary exception handling and adds it to report (optional).<br>
     * <br>
     * Example:
     * <pre>{@code
     * HashMap<?, ?> parameters = new HashMap<>();
     * CommonActions commonActions = new CommonActions();
     * IActionContext contextCopy = actionContext.withId("actionId")
     *         .withAddToReport(true)
     *         .withContinueOnFailed(false)
     *         .withServiceName("default@service")
     *         .withDescription("Action description")
     *         .withCheckPoint(checkPoint);
     *
     * contextCopy.callAction(commonActions, CommonActions::RunScript, parameters, "actionTag", Collections.emptyList());
     * }</pre>
     * @param actionClass instance of action class
     * @param action action method reference
     * @param parameters action parameters
     * @param tag action tag
     * @param verificationOrder verification order for report
     * @return instance of {@link ActionCallResult} containing action status, known bugs and exception (if present)
     * @throws Throwable action's exception if it's failed and {@link #isContinueOnFailed()} = false
     */
    <T extends IActionCaller, P> ActionCallResult<Void> callAction(T actionClass, ConsumerActionWithParameters<T, P> action, P parameters, String tag, List<String> verificationOrder) throws Exception;

    /**
     * Executes action, performs necessary exception handling and adds it to report (optional).<br>
     * <br>
     * Example:
     * <pre>{@code
     * HashMap<?, ?> parameters = new HashMap<>();
     * CommonActions commonActions = new CommonActions();
     * IActionContext contextCopy = actionContext.withId("actionId")
     *         .withAddToReport(true)
     *         .withContinueOnFailed(false)
     *         .withServiceName("default@service")
     *         .withDescription("Action description");
     *         .withCheckPoint(checkPoint);
     *
     * contextCopy.callAction(commonActions, CommonActions::SetVariables, parameters, "actionTag", Collections.emptyList());
     * }</pre>
     * @param actionClass instance of action class
     * @param action action method reference
     * @param parameters action parameters
     * @param tag action tag
     * @param verificationOrder verification order for report
     * @return instance of {@link ActionCallResult} containing action result, status, known bugs and exception (if present)
     * @throws Throwable action's exception if it's failed and {@link #isContinueOnFailed()} = false
     */
    <T extends IActionCaller, P, R> ActionCallResult<R> callAction(T actionClass, FunctionActionWithParameters<T, P, R> action, P parameters, String tag, List<String> verificationOrder) throws Exception;
}
