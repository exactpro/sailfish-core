/******************************************************************************
 * Copyright 2009-2024 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.aml.script.actions;

import static com.exactpro.sf.services.ServiceHandlerRoute.FROM_ADMIN;
import static com.exactpro.sf.services.ServiceHandlerRoute.FROM_APP;
import static java.util.Arrays.stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.actions.ActionUtil;
import com.exactpro.sf.aml.script.CheckPoint;
import com.exactpro.sf.aml.script.actions.exceptions.WaitMessageException;
import com.exactpro.sf.aml.scriptutil.ExpressionResult;
import com.exactpro.sf.aml.scriptutil.LegReorder;
import com.exactpro.sf.aml.scriptutil.MessageCount;
import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.Pair;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.comparison.ComparisonUtil;
import com.exactpro.sf.comparison.Formatter;
import com.exactpro.sf.comparison.IPostValidation;
import com.exactpro.sf.comparison.MessageComparator;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.scriptrunner.MessageLevel;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionReport;
import com.exactpro.sf.services.CSHIterator;
import com.exactpro.sf.services.ICSHIterator;
import com.exactpro.sf.services.IInitiatorService;
import com.exactpro.sf.services.IService;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.util.ServiceUtil;
import com.exactpro.sf.util.DateTimeUtility;
import com.exactpro.sf.util.KnownBugException;
import com.exactpro.sf.util.KnownBugPostValidation;
import com.exactpro.sf.util.MessageKnownBugException;

public class WaitAction {
    public static final String ACTUAL_COUNT_FIELD = "ActualCount";

    static Logger log = LoggerFactory.getLogger(WaitAction.class);

    public static final IMessage waitForMessage(IActionContext actionContext, IMessage messageFilter, boolean fromApp) throws InterruptedException {
        return waitForMessage(actionContext, messageFilter, fromApp, Objects.toString(actionContext.getDescription(), ""));
    }

	public static final IMessage waitForMessage(IActionContext actionContext, IMessage messageFilter, boolean fromApp, String description) throws InterruptedException
	{
		return waitForMessage(actionContext, messageFilter, fromApp, null, description);
	}

    public static final IMessage waitForMessage(IActionContext actionContext, IMessage messageFilter, boolean fromApp, IPostValidation postValidation) throws InterruptedException {
        return waitForMessage(actionContext, messageFilter, fromApp, postValidation, Objects.toString(actionContext.getDescription(), ""));
    }

    /**
     * @deprecated this method does not provide the ability to specify the service to receive the message.
     *              Will be removed in the future
     */
    @Deprecated
    public static final IMessage waitForMessage(IActionContext actionContext, IMessage messageFilter, boolean fromApp,
                                                SailfishURI dictionary, String storageMessageTypes, boolean invertStoredMessageTypes)
            throws InterruptedException
    {
        IInitiatorService client = ActionUtil.getService(actionContext, IInitiatorService.class);
        return waitForMessage(actionContext, client, messageFilter, fromApp);
    }

    /**
     * This method can be used to await the message from the certain service
     *
     * @param service the service to use to receive the expected message
     */
    @Nullable
    public static IMessage waitForMessage(IActionContext actionContext, IInitiatorService service, IMessage messageFilter, boolean fromApp) throws InterruptedException {
        String serviceName = actionContext.getServiceName();

        long waitTime = actionContext.getTimeout();
        boolean addToReport = actionContext.isAddToReport();

        actionContext.getLogger().info("{} service instance [{}] has been obtained.", service.getClass().getSimpleName(), serviceName);
        actionContext.getLogger().info("Settings [{}]", actionContext);

        IServiceHandler handler = getServiceHandler(service);
        ISession session = getSession(service);

        ComparatorSettings settings = createCompareSettings(actionContext, null, messageFilter);
        IServiceSettings serviceSettings = service.getSettings();
        SailfishURI dictionaryURI = actionContext.getDictionaryURI();
        if(dictionaryURI == null) {
            dictionaryURI = serviceSettings.getDictionaryName();
        }

        if (dictionaryURI != null) {
            settings.setDictionaryStructure(actionContext.getDictionary(dictionaryURI));
        }

        Collection<String> storedMessageTypes = getStoredMessageTypes(actionContext.getDataManager(), service);

        Logger logger = actionContext.getLogger();
        List<Pair<IMessage, ComparisonResult>> results = null;

        logger.debug("[{}]  start wait for message in {} queue", serviceName, fromApp ? "application" : "admin");
        CheckPoint checkpoint = actionContext.getCheckPoint();
        try {
            results = waitMessage(handler, session, fromApp ? FROM_APP : FROM_ADMIN, checkpoint,
                    waitTime, messageFilter, settings, storedMessageTypes, serviceSettings.isInvertStoredMessageTypes());
        } catch (InterruptedException e) {
            logger.error("[{}]  InterruptedException:{}", serviceName, e.getMessage(), e);
            throw e;
        }
        return processResults(actionContext.getReport(), settings, results, messageFilter, serviceName,
                false, addToReport, Objects.toString(actionContext.getDescription(), ""), checkpoint);
    }

    public static final IMessage waitForMessage(IActionContext actionContext, IMessage messageFilter, boolean fromApp,
                                                IPostValidation postValidation, String description)
	throws InterruptedException
	{
        String serviceName = actionContext.getServiceName(); // for example "NativeClient"
		IInitiatorService client = ActionUtil.getService(actionContext, IInitiatorService.class);

		long waitTime = actionContext.getTimeout();
		boolean addToReport = actionContext.isAddToReport();

		actionContext.getLogger().info("{} client instance [{}] has been obtained.", client.getClass().getSimpleName(), serviceName);
		actionContext.getLogger().info("Settings [{}]", actionContext);

        IServiceHandler collectorServiceHandler = getServiceHandler(client);
		ISession session = getSession(client);

        ComparatorSettings compSettings = createCompareSettings(actionContext, postValidation, messageFilter);

		IService service = actionContext.getServiceManager().getService(new ServiceName(serviceName));
        SailfishURI dictionaryURI = actionContext.getDictionaryURI();
        if(dictionaryURI == null) {
            dictionaryURI = service.getSettings().getDictionaryName();
        }

		if (dictionaryURI != null) {
            compSettings.setDictionaryStructure(actionContext.getDictionary(dictionaryURI));
		}

		return waitForMessage(actionContext, serviceName,  messageFilter,
				collectorServiceHandler, session, actionContext.getCheckPoint(), waitTime,	false, addToReport, fromApp, compSettings, description);
	}

    public static final IMessage waitForMessage(IActionContext actionContext,
            String serviceName,
            IMessage messageFilter,
            IServiceHandler handler,
            ISession isession,
            CheckPoint checkPoint,
            long waitTime,
            boolean returnExactOnly,
            boolean addToReport,
            boolean fromApp,
            ComparatorSettings settings) throws InterruptedException {
        return waitForMessage(actionContext, serviceName, messageFilter, handler, isession, checkPoint, waitTime, returnExactOnly, addToReport, fromApp, settings, "");
    }

    public static final IMessage waitForMessage(IActionContext actionContext,
            String serviceName,
            IMessage messageFilter,
            IServiceHandler handler,
            ISession isession,
            CheckPoint checkPoint,
            long waitTime,
            boolean returnExactOnly,
            boolean addToReport,
            boolean fromApp,
            ComparatorSettings settings,
            String description) throws InterruptedException
	{
        return waitForMessage(actionContext, serviceName, messageFilter, handler, isession, checkPoint,
                actionContext.getReport(), waitTime, returnExactOnly, addToReport, fromApp, settings, description);
	}

	public static final IMessage waitForMessage(IActionContext actionContext,
            String serviceName,
			IMessage messageFilter,
            IServiceHandler handler,
            ISession isession,
            CheckPoint checkPoint,
            IActionReport report,
            long waitTime,
            boolean returnExactOnly,
            boolean addToReport,
            boolean fromApp,
            ComparatorSettings settings,
            String description) throws InterruptedException
	{
	    Logger logger = actionContext.getLogger();
        Collection<String> storedMessageTypes = getStoredMessageTypes(actionContext);
		List<Pair<IMessage, ComparisonResult>> results = null;

        IInitiatorService client = ActionUtil.getService(actionContext, IInitiatorService.class);

		logger.debug("[{}]  start wait for message in {} queue", serviceName, fromApp ? "application" : "admin");

		try {
            results = waitMessage(handler, isession, fromApp ? FROM_APP : FROM_ADMIN, checkPoint, waitTime, messageFilter, settings, storedMessageTypes, client.getSettings().isInvertStoredMessageTypes());
		} catch (InterruptedException e) {
			logger.error("[{}]  InterruptedException:{}", serviceName, e.getMessage(), e);
			throw e;
		}
		return processResults(report, settings, results, messageFilter, serviceName, returnExactOnly, addToReport, description, checkPoint);
	}

    private static Collection<String> getStoredMessageTypes(IActionContext actionContext) {
        IInitiatorService client = ActionUtil.getService(actionContext, IInitiatorService.class);
        return getStoredMessageTypes(actionContext.getDataManager(), client);
    }

    private static Collection<String> getStoredMessageTypes(IDataManager dataManager, IService client) {
        return getStoredMessageTypes(dataManager, client.getSettings().getStoredMessageTypes());
    }

    private static Collection<String> getStoredMessageTypes(IDataManager dataManager, String rawStoredMessageTypes) {
        Collection<String> storedMessageTypes = Collections.emptySet();
        try {
            if (StringUtils.isNotBlank(rawStoredMessageTypes)) {
                rawStoredMessageTypes = ServiceUtil.loadStringFromAlias(dataManager, rawStoredMessageTypes, ",");
                storedMessageTypes = stream(rawStoredMessageTypes.split(","))
                        .map(String::trim)
                        .filter(StringUtils::isNoneEmpty)
                        .collect(Collectors.toSet());
            }
        } catch(SailfishURIException e){
            log.error("An Error occurs while getting client StoredMessageTypes property", e);
        }
        return storedMessageTypes;
    }

    // this is public because it's used in tests
    public static List<Pair<IMessage, ComparisonResult>> waitMessage(IServiceHandler handler, ISession session, ServiceHandlerRoute route, CheckPoint checkPoint, long timeout, IMessage filter, ComparatorSettings settings, Collection<String> storedMessageTypes, boolean invertStoredMessageTypes)
            throws InterruptedException {
        CSHIterator<IMessage> messagesIterator = handler.getIterator(session, route, checkPoint);
        return waitMessage(settings, filter, messagesIterator, timeout, storedMessageTypes, invertStoredMessageTypes);
    }

    public static List<Pair<IMessage, ComparisonResult>> waitMessage(ComparatorSettings settings, IMessage filter, ICSHIterator<IMessage> messagesIterator, IActionContext actionContext) throws InterruptedException {
        Collection<String> storedMessageTypes = getStoredMessageTypes(actionContext);
        IInitiatorService client = ActionUtil.getService(actionContext, IInitiatorService.class);
        return waitMessage(settings, filter, messagesIterator, actionContext.getTimeout(), storedMessageTypes, client.getSettings().isInvertStoredMessageTypes());
    }

    public static List<Pair<IMessage, ComparisonResult>> waitMessage(ComparatorSettings settings, IMessage filter,
            ICSHIterator<IMessage> messagesIterator, long timeout, Collection<String> storedMessageTypes, boolean invertStoredMessageTypes) throws InterruptedException {
        long endTime = System.currentTimeMillis() + timeout;
        List<Pair<IMessage, ComparisonResult>> partialList = new ArrayList<>();
        List<Pair<IMessage, ComparisonResult>> conditionallyPassedMessage = null;
        if (!storedMessageTypes.isEmpty() && (invertStoredMessageTypes == storedMessageTypes.contains(filter.getName()))) {
            throw new WaitMessageException(String.format("Message - '%s' is not allowed. Check the 'Stored Message Type' and 'Invert Stored Message Types' options in your service settings", filter.getName()));
        }

        while(messagesIterator.hasNext(endTime - System.currentTimeMillis())) {
            IMessage message = messagesIterator.next();

            ComparisonResult result = MessageComparator.compare(message, filter, settings);

            if(result == null) {
                continue;
            }

            Throwable t = result.getException();

            if(t != null && !(t instanceof KnownBugException)) {
                log.error("Got comparison result with exception: {}", result, t);
            }

            // calculate number with failed status
            // accept message if no fail results found
            int countFailed = ComparisonUtil.getResultCount(result, StatusType.FAILED);
            int countCP = ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_PASSED);
            int count = countFailed;
            count += ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_FAILED);
            count += countCP;

            if(count == 0) {
                return wrapToList(messagesIterator, message, result);
            }  else if (countCP != 0 && countFailed == 0 && conditionallyPassedMessage == null) {
                log.debug("waitMessage: add message with conditionally passed {} to list", message);
                conditionallyPassedMessage = wrapToList(messagesIterator, message, result);
                messagesIterator.updateCheckPoint();
            }

            count = ComparisonUtil.getResultCount(result, StatusType.PASSED);

            if(count > 0) {
                log.debug("waitMessage: add message {} to partial matched list", message);
                partialList.add(new Pair<>(message, result));
            }
        }

        return conditionallyPassedMessage == null ? partialList : conditionallyPassedMessage;
    }

    /**
     * Count all application or administration messages received after checkpoint and
     * successfully compared with messages filter.
     *
     * @param actionContext contains action context
     * @param message message filter
     * @param isApp is {@code true} for application messages and {@code false}
     * for administration messages
     * @throws Exception
     * @return
     */
    public static HashMap<String, Integer> countMessages(IActionContext actionContext, IMessage message, boolean isApp) throws Exception {
        return countMessages(actionContext, message, isApp, "");
    }

	/**
	 * Count all application or administration messages received after checkpoint and
	 * successfully compared with messages filter.
	 *
     * @param actionContext contains action context
	 * @param message message filter
	 * @param isApp is {@code true} for application messages and {@code false}
	 * for administration messages
	 * @param description
	 * @throws Exception
	 */
	public static HashMap<String, Integer> countMessages(IActionContext actionContext, IMessage message, boolean isApp, String description) throws Exception
	{
		return countMessages(actionContext, message, isApp, null, description);
	}

    /**
     * Count all application or administration messages received after checkpoint
     * without UnitHeader messages.
     * @param actionContext contains action context
     *
     * @throws Exception
     */
    public static HashMap<String, Integer> countMessages(IActionContext actionContext) throws Exception {
        return countMessages(actionContext, "");
    }

	/**
	 * Count all application or administration messages received after checkpoint
	 * without UnitHeader messages.
	 * @param description
     *
     * @param actionContext contains action context
	 * @throws Exception
	 */
	public static HashMap<String, Integer> countMessages(IActionContext actionContext, String description) throws Exception
	{
		IInitiatorService service = ActionUtil.getService(actionContext, IInitiatorService.class);
        IServiceHandler handler = getServiceHandler(service);
        ISession isession = getSession(service);

		long waitUntil = System.currentTimeMillis() + actionContext.getTimeout();

		List<IMessage> reportMessages = new ArrayList<>();
        CSHIterator<IMessage> messagesIterator = handler.getIterator(isession, FROM_ADMIN, actionContext.getCheckPoint());

		while(messagesIterator.hasNext(waitUntil - System.currentTimeMillis())) {
			IMessage msg = messagesIterator.next();

            if(!"UnitHeader".equals(msg.getName())) {
				reportMessages.add(msg);
			}
		}

		// add status to report
		int receivedMessages = reportMessages.size();
        @SuppressWarnings("deprecation")
        Object countObject = ObjectUtils.defaultIfNull(actionContext.getMessageCountFilter(), actionContext.getMessageCount());
        ComparisonResult result = validateMessageCount(receivedMessages, countObject);
        StatusType status = ComparisonUtil.getStatusType(result);
		IActionReport report = actionContext.getReport();
        String expected = Formatter.formatExpected(result);

        try(IActionReport embeddedReport = report.createEmbeddedReport("Received messages: " + receivedMessages + " of " + expected,
                description)) {
            embeddedReport.createVerification(status, "Count messages", description, status != StatusType.PASSED ? result.getExceptionMessage() : "", result, result.getException());
            for (IMessage message : reportMessages) {
                embeddedReport.createMessage(status, MessageLevel.INFO, message.toString());
            }
        } catch (Exception e) {
            throw new EPSCommonException("Failed to close ActionGroupReport", e);
        }

        if(status == StatusType.CONDITIONALLY_PASSED) {
            throw (KnownBugException)result.getException();
        }

        if(status == StatusType.FAILED) {
			throw new EPSCommonException("Expected messages quantity did not match actual messages quantity."
                    + " Expected=" + expected + ", actual=" + receivedMessages);
		}

        return countResult(receivedMessages);
	}

    /**
     * Count all application or administration messages received after checkpoint and
     * successfully compared with messages filter.
     *
     * @param actionContext contains action context
     * @param message message filter
     * @param isApp is {@code true} for application messages and {@code false}
     * for administration messages
     * @param postValidation do custom validation after standard validation
     * @throws Exception
     * @return
     */
    public static HashMap<String, Integer> countMessages(IActionContext actionContext, IMessage message, boolean isApp, IPostValidation postValidation) throws Exception {
        return countMessages(actionContext, message, isApp, postValidation, "");
    }

	/**
	 * Count all application or administration messages received after checkpoint and
	 * successfully compared with messages filter.
	 *
     * @param actionContext contains action context
	 * @param message message filter
	 * @param isApp is {@code true} for application messages and {@code false}
	 * for administration messages
     * @param postValidation do custom validation after standard validation
     * @param description
	 * @throws Exception
	 */
    @SuppressWarnings("deprecation")
    public static HashMap<String, Integer> countMessages(IActionContext actionContext, IMessage message, boolean isApp,
            IPostValidation postValidation, String description) throws Exception {
		String serviceName = actionContext.getServiceName();

		IInitiatorService service = ActionUtil.getService(actionContext, IInitiatorService.class);
        IServiceHandler handler = getServiceHandler(service);
        ISession isession = getSession(service);

        Object messageCount = ObjectUtils.defaultIfNull(actionContext.getMessageCountFilter(), actionContext.getMessageCount());

		long timeout = actionContext.getTimeout();
		Thread.sleep(timeout);

        ComparatorSettings compSettings = createCompareSettings(actionContext, postValidation, message);

        SailfishURI dictionaryURI = actionContext.getDictionaryURI();
        if(dictionaryURI == null) {
            dictionaryURI = service.getSettings().getDictionaryName();
        }

        if(dictionaryURI != null) {
            compSettings.setDictionaryStructure(actionContext.getDictionary(dictionaryURI));
        }

        int actualCount;
        if (message != null) {
            actualCount = countMessages(actionContext.getReport(), serviceName, message, messageCount, handler, isession,
                    actionContext.getCheckPoint(), isApp, compSettings, description);
        } else {
            actualCount = countMessages(actionContext.getReport(), serviceName, messageCount, handler, isession,
                    actionContext.getCheckPoint(), isApp, description);
        }
        return countResult(actualCount);
    }

    /**
     * Count all application or administration messages received after checkpoint and
     * successfully compared with messages filter.
     * @param serviceName service name
     * @param messageFilter message filter
     * @param expectedMessageCount expected message count
     * @param handler service handler
     * @param isession session
     * @param checkPoint checkpoint
     * @param fromApp is {@code true} for application messages and {@code false}
     * for administration messages
     * @param settings comparator settings
     * @return count of received messages passed throw message filter
     * @throws InterruptedException actually should not be thrown
     */
    public static final int countMessages(IActionReport report,
            String serviceName,
            IMessage messageFilter,
            MessageCount expectedMessageCount,
            IServiceHandler handler,
            ISession isession,
            CheckPoint checkPoint,
            boolean fromApp,
            ComparatorSettings settings) throws InterruptedException {
        return countMessages(report, serviceName, messageFilter, expectedMessageCount, handler, isession, checkPoint, fromApp, settings, "");
    }

	/**
	 * Count all application or administration messages received after checkpoint and
	 * successfully compared with messages filter.
     * @param report action report
	 * @param serviceName service name
	 * @param messageFilter message filter
     * @param expectedMessageCount expected message count
	 * @param handler service handler
	 * @param isession session
     * @param checkPoint checkpoint
     * @param fromApp is {@code true} for application messages and {@code false}
     * for administration messages
     * @param settings comparator settings
	 * @param description
	 * @return count of received messages passed throw message filter
	 */
    public static final int countMessages(IActionReport report, String serviceName, IMessage messageFilter,
                                          Object expectedMessageCount, IServiceHandler handler, ISession isession,
                                          CheckPoint checkPoint, boolean fromApp, ComparatorSettings settings,
                                          String description) {
        log.debug("countMessages 1");

        List<Pair<IMessage, ComparisonResult>> allResults = new ArrayList<>();
        CSHIterator<IMessage> messagesIterator = handler.getIterator(isession, fromApp ? FROM_APP : FROM_ADMIN, checkPoint);

        countMessages(messageFilter, messagesIterator, settings, allResults);

        int messageCount = allResults.size();

        try {
            addResultToReport(report, expectedMessageCount, description, allResults, messageCount, true);
        } catch (KnownBugException e) {
            throw new MessageKnownBugException(e.getMessage(), countResult(messageCount));
        }

        return messageCount;
    }

    public static boolean addResultToReport(IActionReport report,
            Object expectedMessageCount,
            String description,
            List<Pair<IMessage, ComparisonResult>> allResults, int messageCount, boolean exceptionOnFail)
    {
        ComparisonResult comparisonResult = validateMessageCount(messageCount, expectedMessageCount);
        StatusType status = ComparisonUtil.getStatusType(comparisonResult);
        String expected = Formatter.formatExpected(comparisonResult);

        if(status == StatusType.PASSED || status == StatusType.CONDITIONALLY_PASSED) { // was ==
            report.createVerification(status, "Received messages: " + messageCount + " from " + expected, description, "", comparisonResult, comparisonResult.getException());

            int i = 1;
            for (Pair<IMessage, ComparisonResult> result : allResults) {
                report.createVerification(status, "Message " + i++, "", "", result.getSecond(), null);
            }

            if(status == StatusType.CONDITIONALLY_PASSED) {
                throw (KnownBugException)comparisonResult.getException();
            }

            return true;
        } else {
            report.createVerification(status, "Received messages: " + messageCount + " from " + expected, description, "",
                    comparisonResult, comparisonResult.getException());

            int i = 1;
            for (Pair<IMessage, ComparisonResult> result : allResults) {
                report.createVerification(StatusType.PASSED, "Message " + i++, description, "", result.getSecond(), null);
            }
            if(exceptionOnFail) { // was !=
                throw new EPSCommonException("Expected messages quantity did not match actual messages quantity."
                        + " Expected=" + expected + ", actual=" + messageCount);
            }
            return false;
        }
    }

    /**
     * Count all application or administration messages received after checkpoint without comparing
     * @param messageFilter message filter
     * @param handler service handler
     * @param isession session
     * @param settings
     * @param checkPoint
     * @param fromApp is {@code true} for application messages and {@code false}
     * for administration messages
     * @param allResults
     * @return count of received messages passed throw message filter
     */
    public static final void countMessages(IMessage messageFilter, IServiceHandler handler, ISession isession,
                                           CheckPoint checkPoint, boolean fromApp, ComparatorSettings settings,
                                           List<Pair<IMessage, ComparisonResult>> allResults) {

        CSHIterator<IMessage> messagesIterator = handler.getIterator(isession, fromApp ? FROM_APP : FROM_ADMIN, checkPoint);
        countMessages(messageFilter, messagesIterator, settings, allResults);
    }

    public static void countMessages(IMessage messageFilter, ICSHIterator<IMessage> messagesIterator,
                                     ComparatorSettings settings, List<Pair<IMessage, ComparisonResult>> allResults) {

        while (messagesIterator.hasNext()) {
            IMessage message = messagesIterator.next();
            ComparisonResult result = MessageComparator.compare(message, messageFilter, settings);

            if (result == null) {
                continue;
            }

            // calculate number with failed status
            // accept message if no fail results found
            int count = ComparisonUtil.getResultCount(result, StatusType.FAILED);
            count += ComparisonUtil.getResultCount(result, StatusType.CONDITIONALLY_FAILED);

            if (count == 0) {
                allResults.add(new Pair<>(message, result));
                messagesIterator.updateCheckPoint();
            }
        }

    }

    /**
     * Count all application or administration messages received after checkpoint.
     * @param report action report
     * @param serviceName service name
     * @param mc
     * @param handler service handler
     * @param isession session
     * @param checkPoint checkpoint
     * @param fromApp is {@code true} for application messages and {@code false}
     * for administration messages
     * @return count of received messages passed throw message filter
     */
    public static final int countMessages(IActionReport report,
            String serviceName, Object mc,
            IServiceHandler handler, ISession isession, CheckPoint checkPoint, boolean fromApp) {
        return countMessages(report, serviceName, mc, handler, isession, checkPoint, fromApp, "");
    }

	/**
	 * Count all application or administration messages received after checkpoint.
	 * @param serviceName service name
	 * @param handler service handler
	 * @param isession session
	 * @param checkPoint checkpoint
	 * @param description
	 * @return count of received messages passed throw message filter
	 */
	public static final int countMessages(IActionReport report,
            String serviceName, Object mc,
            IServiceHandler handler, ISession isession, CheckPoint checkPoint, boolean fromApp, String description)
	{
        List<IMessage> messages = handler.getMessages(isession, fromApp ? FROM_APP : FROM_ADMIN, checkPoint);

		// add status to report
		int receivedMessages = messages.size();
        ComparisonResult result = validateMessageCount(receivedMessages, mc);
        StatusType status = ComparisonUtil.getStatusType(result);
        String expected = Formatter.formatExpected(result);

        try(IActionReport embeddedReport = report.createEmbeddedReport("Received messages: " + receivedMessages + " from " + expected, description)) {
            embeddedReport.createVerification(status, "Count messages", description, status != StatusType.PASSED ? result.getExceptionMessage() : "", result, result.getException());
            for (IMessage message : messages) {
                embeddedReport.createMessage(status, MessageLevel.INFO, message.toString());
            }
        } catch (Exception e) {
            throw new EPSCommonException("Failed to close ActionGroupReport", e);
        }

        if(status == StatusType.CONDITIONALLY_PASSED) {
            throw (KnownBugException)result.getException();
        }

        if(status == StatusType.FAILED) {
			throw new EPSCommonException("Expected messages quantity did not match actual messages quantity."
                    + " Expected=" + expected + ", actual=" + receivedMessages);
		}

		return receivedMessages;
	}

    public static HashMap<String, Integer> countMessages(IActionContext actionContext, List<String> messageTypes, boolean isIgnore, boolean isApp) {
        return countMessages(actionContext, messageTypes, isIgnore, isApp, "");
    }

	public static HashMap<String, Integer> countMessages(IActionContext actionContext, List<String> messageTypes, boolean isIgnore, boolean isApp, String description) {
        CheckPoint checkPoint = actionContext.getCheckPoint();
        @SuppressWarnings("deprecation")
        Object mc = ObjectUtils.defaultIfNull(actionContext.getMessageCountFilter(), actionContext.getMessageCount());

        IInitiatorService service = ActionUtil.getService(actionContext, IInitiatorService.class);
        IServiceHandler handler = getServiceHandler(service);
        ISession session = getSession(service);

        List<IMessage> messages = handler.getMessages(session, isApp ? FROM_APP : FROM_ADMIN, checkPoint);
        Iterator<IMessage> it = messages.iterator();

        while(it.hasNext()) {
            String msgName = it.next().getName();

            if((isIgnore && messageTypes.contains(msgName)) ||
                    (!isIgnore && !messageTypes.isEmpty() && !messageTypes.contains(msgName))) {
                it.remove();
            }
        }

        int actualCount = messages.size();
        ComparisonResult result = validateMessageCount(actualCount, mc);
        StatusType status = ComparisonUtil.getStatusType(result);
        IActionReport report = actionContext.getReport();
        String expected = Formatter.formatExpected(result);

        try(IActionReport embeddedReport = report.createEmbeddedReport("Received messages: " + actualCount + " from " + expected, description)) {
            embeddedReport.createVerification(status, "Count messages", description, status != StatusType.PASSED ? result.getExceptionMessage() : "", result, result.getException());
            for (IMessage message : messages) {
                embeddedReport.createMessage(status, MessageLevel.INFO, message.toString());
            }
        } catch (Exception e) {
            throw new EPSCommonException("Failed to close ActionGroupReport", e);
        }

        if(status == StatusType.CONDITIONALLY_PASSED) {
            throw (KnownBugException)result.getException();
        }

        if(status == StatusType.FAILED) {
            throw new EPSCommonException("Expected messages quantity did not match actual messages quantity. Expected = " + expected + ", actual = " + actualCount);
        }
        return countResult(actualCount);
	}

    public static ComparatorSettings createCompareSettings(IActionContext actionContext, IPostValidation postValidation, IMessage messageFilter) {
        IPostValidation validation = new KnownBugPostValidation(postValidation, messageFilter);

        ComparatorSettings compSettings = new ComparatorSettings();
    	compSettings.setPostValidation(validation);
    	compSettings.setMetaContainer(actionContext.getMetaContainer());
    	compSettings.setCheckGroupsOrder(actionContext.isCheckGroupsOrder());
    	compSettings.setNegativeMap(actionContext.getNegativeMap());
    	compSettings.setReorderGroups(actionContext.isReorderGroups());
    	compSettings.setUncheckedFields(actionContext.getUncheckedFields());
    	compSettings.setIgnoredFields(actionContext.getIgnoredFields());
        return compSettings;
    }

    public static IMessage processResults(IActionReport report, ComparatorSettings settings, List<Pair<IMessage, ComparisonResult>> results, IMessage messageFilter,
            String serviceName, boolean returnExactOnly, boolean addToReport, String description, CheckPoint checkPoint) {
        if (results.size() == 1)
    	{
    		Pair<IMessage, ComparisonResult> result = results.get(0);
            int countFailed = ComparisonUtil.getResultCount(result.getSecond(), StatusType.FAILED);
            countFailed += ComparisonUtil.getResultCount(result.getSecond(), StatusType.CONDITIONALLY_FAILED);
            int countCP = ComparisonUtil.getResultCount(result.getSecond(), StatusType.CONDITIONALLY_PASSED);
            int countPassed = countCP + ComparisonUtil.getResultCount(result.getSecond(), StatusType.PASSED);

            log.debug("[{}] Result Table:\n{}", serviceName, result.getSecond());
            log.debug("[{}] failed results count: {}. Result Table:\n{}", serviceName, countFailed, result.getSecond());

    		if (addToReport)
    		{
                StatusType status;
                if (countFailed != 0) {
                    status = StatusType.FAILED;
                }else if(countCP!=0){
                    status = StatusType.CONDITIONALLY_PASSED;
                }else{
                    status = StatusType.PASSED;
                }
                report.createVerification(status, "Wait for message", description, status != StatusType.PASSED ? result.getSecond().getExceptionMessage() : "", result.getSecond());
    		}

    		IMessage message = result.getFirst();
    		if (countFailed != 0) {
                throw new WaitMessageException(createWaitExceptionMessage(false, checkPoint));
            } else if (countCP != 0) {
                Throwable exception = result.getSecond().getException();
                if (exception instanceof KnownBugException) {
                    KnownBugException knownBugException = (KnownBugException) exception;
                    throw new MessageKnownBugException(StringUtils.defaultString(knownBugException.getMessage()),
                            getReorderMessage(message, settings, messageFilter), knownBugException.getPotentialDescriptions());
                } else {
                    throw new WaitMessageException("Timeout. Known bugs were partially reproduced, but there are also unknown bugs (" + StringUtils.defaultString(exception.getMessage()) + ')');
                }
            }
            return getReorderMessage(message, settings, messageFilter);
    	}

    	if (returnExactOnly)
    	{
    		return null;
    	}
        processFailed(results, report, serviceName, description, addToReport);

        throw new WaitMessageException(createWaitExceptionMessage(results.isEmpty(), checkPoint));
    }

    public static void processFailed(List<Pair<IMessage, ComparisonResult>> results, IActionReport report,
                                     String serviceName, String description, boolean addToReport) {
        int iMessage = 0;
        int countFailed = 0;
        int countConditionallyPassed = 0;
        for (Pair<IMessage, ComparisonResult> result : results) {
            iMessage++;

            log.debug("[{}] Result Table [{}]:\n{}", serviceName, iMessage, result.getSecond());

            if (addToReport)
            {
                int countPassed = ComparisonUtil.getResultCount(result.getSecond(), StatusType.PASSED);
                countConditionallyPassed = ComparisonUtil.getResultCount(result.getSecond(), StatusType.CONDITIONALLY_PASSED);
                countFailed = ComparisonUtil.getResultCount(result.getSecond(), StatusType.FAILED);
                countFailed += ComparisonUtil.getResultCount(result.getSecond(), StatusType.CONDITIONALLY_FAILED);
                int countNA = ComparisonUtil.getResultCount(result.getSecond(), StatusType.NA);
                report.createVerification(StatusType.FAILED,
                        "Similar message [" + iMessage + "]. Failed/Passed/ConditionallyPassed/NA: "+countFailed+"/"+countPassed+"/"+countConditionallyPassed+"/"+countNA, description, "",
                        result.getSecond(), null);
            }
        }
        if (countFailed == 0 && countConditionallyPassed != 0) {
            throw new MessageKnownBugException();
        }
    }

    /**
     * @param messagesIterator
     * @param message
     * @param result
     * @return
     */
    private static List<Pair<IMessage, ComparisonResult>> wrapToList(ICSHIterator<IMessage> messagesIterator, IMessage message,
            ComparisonResult result) {
        List<Pair<IMessage, ComparisonResult>> list = new ArrayList<>();

        list.add(new Pair<>(message, result));
        messagesIterator.updateCheckPoint();

        return list;
    }

    private static IServiceHandler getServiceHandler(IInitiatorService service) {
        IServiceHandler handler = service.getServiceHandler();
	    if (handler == null) {
	        throw new EPSCommonException("Service handler is null for service " + service.getServiceName());
	    }
	    return handler;
	}

	private static ISession getSession(IInitiatorService service) {
        ISession session = service.getSession();
        if (session == null) {
            throw new EPSCommonException("Session is null for service " + service.getServiceName());
        }
        return session;
    }

    private static IMessage getReorderMessage(IMessage message, ComparatorSettings settings, IMessage messageFilter) {
        if (settings.isReorderGroups()) {
            IMessage msg = message;
            IMessage msgFilter = messageFilter;

            return LegReorder.reorder(msg, msgFilter, settings);
        }
        return message;
    }

    private static ComparisonResult validateMessageCount(int messageCount, Object countObject) {
        ComparisonResult result = new ComparisonResult("Count").setActual(messageCount);

        if(countObject instanceof MessageCount) {
            MessageCount count = (MessageCount)countObject;
            StatusType status = count.checkInt(messageCount) ? StatusType.PASSED : StatusType.FAILED;
            result.setExpected(count).setStatus(status);
        } else if(countObject instanceof IFilter) {
            IFilter filter = (IFilter)countObject;
            result.setExpected(filter);

            try {
                ExpressionResult expressionResult = filter.validate(messageCount);
                result.setExpressionResult(expressionResult)
                    .setStatus(expressionResult.getResult() ? StatusType.PASSED : StatusType.FAILED);
            } catch(RuntimeException e) {
                result.setStatus(StatusType.FAILED).setException(e);
            }
        } else {
            throw new EPSCommonException("Niether message count nor message count filter is set");
        }

        return KnownBugPostValidation.process(result);
    }

    private static String createWaitExceptionMessage(boolean isEmpty, CheckPoint checkPoint){
        StringBuilder messageBuilder = new StringBuilder("Timeout. ");

        if (isEmpty) {
            messageBuilder.append("No messages matching filter ");
        } else {
            messageBuilder.append("No messages fully matched the filter ");
        }

        if (checkPoint != null) {
            messageBuilder.append("from the checkpoint ");
            if (!checkPoint.isSmart()) {

                messageBuilder.append("appxrox. ")
                        .append(DateTimeUtility.toLocalDate(checkPoint.getTimestamp()))
                        .append(" at ")
                        .append(DateTimeUtility.toLocalTime(checkPoint.getTimestamp()))
                        .append(' ');
            }
        } else {
            messageBuilder.append("from test case start ");
        }
        messageBuilder.append("till the timeout is exceeded ");

        long currentTime = System.currentTimeMillis();
        messageBuilder.append("appxrox. ")
                .append(DateTimeUtility.toLocalDate(currentTime))
                .append(" at ")
                .append(DateTimeUtility.toLocalTime(currentTime))
                .append(' ');

        return messageBuilder.toString();
    }

    @NotNull
    public static HashMap<String, Integer> countResult(int actualCount) {
        HashMap<String, Integer> result = new HashMap<>();
        result.put(ACTUAL_COUNT_FIELD, actualCount);
        return result;
    }
}
