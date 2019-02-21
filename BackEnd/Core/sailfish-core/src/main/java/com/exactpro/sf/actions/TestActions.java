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
package com.exactpro.sf.actions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.Pair;
import com.exactpro.sf.aml.CommonColumn;
import com.exactpro.sf.aml.CommonColumns;
import com.exactpro.sf.aml.CustomColumn;
import com.exactpro.sf.aml.CustomColumns;
import com.exactpro.sf.aml.Description;
import com.exactpro.sf.aml.Direction;
import com.exactpro.sf.aml.MessageDirection;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.script.actions.WaitAction;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.configuration.DummyDictionaryManager;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.MessageLevel;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.actionmanager.ActionMethod;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionReport;
import com.exactpro.sf.services.IAcceptorService;
import com.exactpro.sf.services.ICSHIterator;
import com.exactpro.sf.services.IInitiatorService;
import com.exactpro.sf.services.IService;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.storage.MessageFilter;
import com.exactpro.sf.storage.MessageRow;
import com.exactpro.sf.storage.util.JsonMessageConverter;
import com.exactpro.sf.util.DateTimeUtility;
import com.google.common.collect.ImmutableSet;

@MatrixActions
@ResourceAliases({"TestActions"})
public class TestActions extends AbstractCaller {
    private static final Logger logger = LoggerFactory.getLogger(TestActions.class);
    private static final String FROM_TIMESTAMP = "#from_timestamp";
    private static final String TO_TIMESTAMP = "#to_timestamp";
    private static final String FORCE = "#force";

    private final String MESSAGES_SEPARATOR = ",";
    private final long MIN_TIMEOUT = 2000l;
    private final String SCRIPT_URIS_COLUMN = "#script_uris";
    private final String CONTEXT_COLUMN = "#context";
    private final List<Class<?>> SUPPORTED_TYPES;

    {
        SUPPORTED_TYPES = Arrays.stream(JavaType.values()).map(x -> {
            try {
                return Class.forName(x.value());
            } catch(ClassNotFoundException e) {
                throw new EPSCommonException(e);
            }
        }).collect(Collectors.toList());
    }

    private final Map<String, ScriptEngineManager> pluginAliasToEngineManager = new HashMap<>();

	@MessageDirection(direction=Direction.SEND)
	@CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(value = Column.MessageType, required = true)
    })
	@ActionMethod
    public IMessage send(IActionContext actionContext, IMessage msg) throws InterruptedException {
        String serviceName = actionContext.getServiceName();
        msg.getMetaData().setFromService(serviceName);
        IInitiatorService initiatorService = ActionUtil.getService(actionContext, IInitiatorService.class);

        ISession session = initiatorService.getSession();

        if (session == null) {
            logger.error("Can not get session from service: {} (session is null)", serviceName);
            throw new EPSCommonException("Can not get session from service:" + serviceName + "(session is null)");
        }

		return session.send(msg);
	}

    @MessageDirection(direction=Direction.SENDDIRTY)
    @CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
    @ActionMethod
    public IMessage sendDirty(IActionContext actionContext, IMessage msg) throws InterruptedException {
	    String serviceName = actionContext.getServiceName();
        msg.getMetaData().setFromService(serviceName);
        msg.getMetaData().setDirty(true);
        IInitiatorService initiatorService = ActionUtil.getService(actionContext, IInitiatorService.class);

        ISession session = initiatorService.getSession();

        if (session == null) {
            logger.error("Can not get session from service: {} (session is null)", serviceName);
            throw new EPSCommonException("Can not get session from service:" + serviceName + "(session is null)");
        }

        return session.sendDirty(msg);
    }

	@MessageDirection(direction=Direction.RECEIVE)
	@CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
	@ActionMethod
    public IMessage receive(IActionContext actionContext, IMessage msg) throws InterruptedException
	{
		return ActionUtil.getService(actionContext, IInitiatorService.class).receive(actionContext, msg);
	}

    @Description("Retrieve nearest message in the past which matched with filter. <br/>" +
                 "Execution may be slow, because we load message from message storage")
    @MessageDirection(direction= Direction.RECEIVE)
    @CommonColumns({
            @CommonColumn(value = Column.ServiceName, required = true),
            @CommonColumn(value = Column.Timeout, required = true)
    })
    @CustomColumns({
            @CustomColumn(value = FROM_TIMESTAMP, type = LocalDateTime.class),
            @CustomColumn(value = TO_TIMESTAMP, type = LocalDateTime.class)
    })
    @ActionMethod
    public IMessage retrieve(final IActionContext actionContext, IMessage message) throws InterruptedException {
        IService service = ActionUtil.getService(actionContext, IService.class);
        IActionReport report = actionContext.getReport();

        ComparatorSettings compSettings = WaitAction.createCompareSettings(actionContext, null, message);

        ICSHIterator<IMessage> iterator = new JsonMessageIterator(actionContext, service, message);
        
        List<Pair<IMessage, ComparisonResult>> results = WaitAction.waitMessage(compSettings, message, iterator, actionContext.getTimeout());
        
        return WaitAction.processResults(report, compSettings, results, message, actionContext.getServiceName(), false, actionContext.isAddToReport(), actionContext.getDescription());
    }

    @Description("Count nearest messages in the past which matched with filter. <br/>"
                         + "Execution may be slow, because we load message from message storage")
    @MessageDirection(direction = Direction.RECEIVE)
    @CommonColumns({
            @CommonColumn(value = Column.MessageCount, required = true),
            @CommonColumn(value = Column.ServiceName, required = true),
            @CommonColumn(value = Column.Timeout, required = true) })
    @CustomColumns({
            @CustomColumn(value = FROM_TIMESTAMP, type = LocalDateTime.class),
            @CustomColumn(value = TO_TIMESTAMP, type = LocalDateTime.class)
    })
    @ActionMethod
    public void countStored(final IActionContext actionContext, IMessage message) throws InterruptedException {
        IService service = ActionUtil.getService(actionContext, IService.class);
        ICSHIterator<IMessage> iterator = new JsonMessageIterator(actionContext, service, message);
        ComparatorSettings compSettings = WaitAction.createCompareSettings(actionContext, null, message);
        IActionReport report = actionContext.getReport();
        List<Pair<IMessage, ComparisonResult>> allResults = new ArrayList<>();
        Object expectedMessageCount = ObjectUtils.defaultIfNull(actionContext.getMessageCountFilter(), actionContext.getMessageCount());

        WaitAction.countMessages(message, iterator, compSettings, allResults);
        WaitAction.addResultToReport(report, expectedMessageCount, "", allResults, allResults.size(), true);
	}

	@MessageDirection(direction=Direction.RECEIVE)
	@CommonColumns({
        @CommonColumn(value = Column.MessageCount, required = true),
        @CommonColumn(value = Column.ServiceName, required = true)
    })
	@ActionMethod
    public void count(IActionContext actionContext, IMessage msg) throws Exception
	{
		WaitAction.countMessages(actionContext, msg, !msg.getMetaData().isAdmin());
	}

	@CommonColumns({
        @CommonColumn(value = Column.MessageCount, required = true),
        @CommonColumn(value = Column.ServiceName, required = true)
    })
	@ActionMethod
    public void countApp(IActionContext actionContext) throws Exception
	{
	    WaitAction.countMessages(actionContext, null, true);
	}

	@CommonColumns({
        @CommonColumn(value = Column.MessageCount, required = true),
        @CommonColumn(value = Column.ServiceName, required = true)
    })
    @ActionMethod
    public void countAdmin(IActionContext actionContext) throws Exception
    {
        WaitAction.countMessages(actionContext, null, false);
    }

	@CommonColumns({
	    @CommonColumn(value = Column.Timeout),
        @CommonColumn(value = Column.ServiceName, required = true)
    })
	@ActionMethod
    public void connectService(IActionContext actionContext)
	{
		IService service = ActionUtil.getService(actionContext, IService.class);

		if (service instanceof IInitiatorService)
		{
			IInitiatorService initiatorService = (IInitiatorService) service;
            ISession session = initiatorService.getSession();

			try
			{
                if(session == null || session.isClosed()) {
                    initiatorService.connect();
                }
            }
			catch (Exception e)
			{
				throw new EPSCommonException(e);
			}
            isConnected(actionContext, initiatorService);
		}
	}

    @Description("Disconnect service and check status of session. <br/>" +
            "Use the '" + FORCE + "' column to control disconnect logic: if 'true' then no logout message will be sent." +
            "The default value is 'false'.<br>")
	@CommonColumns({
	    @CommonColumn(value = Column.Timeout),
        @CommonColumn(value = Column.ServiceName, required = true)
    })
    @CustomColumns({
            @CustomColumn(value = FORCE, type = Boolean.class)
    })
	@ActionMethod
    public void disconnectService(IActionContext actionContext)
	{
		IService service = ActionUtil.getService(actionContext, IService.class);

		if (service instanceof IInitiatorService)
		{
			IInitiatorService initiatorService = (IInitiatorService) service;
			ISession session = initiatorService.getSession();

			if (session == null)
			{
			    logger.error("Can not get session from service: {} (session is null)", initiatorService.getName());
				throw new EPSCommonException("Can not get session from service:" + initiatorService.getName() + "(session is null)");
			}

            Boolean isForce = actionContext.getMetaContainer().getSystemColumn(FORCE);
            if (Boolean.TRUE.equals(isForce)) {
                session.forceClose();
            } else {
                session.close();
            }
            isDisconnected(actionContext, initiatorService);
		}
	}

    @CommonColumns({
        @CommonColumn(value = Column.Timeout),
        @CommonColumn(value = Column.ServiceName, required = true)
    })
    @ActionMethod
    public void reconnectService(IActionContext actionContext) {
        IService service = ActionUtil.getService(actionContext, IService.class);

        if (service instanceof IInitiatorService) {
            IInitiatorService initiatorService = (IInitiatorService) service;
            ISession session = initiatorService.getSession();

            if (session != null) {
                session.close();
                isDisconnected(actionContext, initiatorService);
            }

            try {
                initiatorService.connect();
            }
            catch (Exception e) {
                logger.error("Can not reconnect service [{}]", initiatorService.getName(), e);
                throw new EPSCommonException(e);
            }

            isConnected(actionContext, (IInitiatorService) service);
        }
    }


    @CommonColumns({
        @CommonColumn(value = Column.Timeout),
        @CommonColumn(value = Column.ServiceName, required = true)
    })
	@ActionMethod
    public void isConnectedService(IActionContext actionContext) throws Exception
	{
		ServiceName serviceName = ServiceName.parse(actionContext.getServiceName());
		IService service = actionContext.getServiceManager().getService(serviceName);

		if (service instanceof IInitiatorService)
		{
			isConnected(actionContext, (IInitiatorService) service);
		}
	}

	@CommonColumns({
	    @CommonColumn(value = Column.Timeout),
        @CommonColumn(value = Column.ServiceName, required = true)
    })
	@ActionMethod
    public void isDisconnectedService(IActionContext actionContext) throws Exception
	{
		ServiceName serviceName = ServiceName.parse(actionContext.getServiceName());
		IService service = actionContext.getServiceManager().getService(serviceName);

		if (service instanceof IInitiatorService)
		{
            isDisconnected(actionContext, (IInitiatorService) service);
		}
	}

    @ActionMethod
    public HashMap<?,?> initMap(IActionContext actionContext, HashMap<?,?> inputData) {
        return inputData;
    }

    @MessageDirection(direction=Direction.SEND)
    @ActionMethod
    public IMessage initSendMessage(IActionContext actionContext, IMessage msg) {
        return msg;
    }

    @MessageDirection(direction=Direction.SENDDIRTY)
    @ActionMethod
    public IMessage initSendDirtyMessage(IActionContext actionContext, IMessage msg) {
        return initSendMessage(actionContext, msg);
    }

    @MessageDirection(direction=Direction.RECEIVE)
    @ActionMethod
    public IMessage initReceiveMessage(IActionContext actionContext, IMessage msg) {
        return msg;
    }

	@CommonColumns({
        @CommonColumn(value = Column.MessageCount, required = true),
        @CommonColumn(value = Column.ServiceName, required = true)
    })
    @CustomColumns({
        @CustomColumn(value = "IsAdmin", required = true),
        @CustomColumn(value = "IsIgnore", required = true),
        @CustomColumn("MessageTypes")
    })
    @ActionMethod
    public void countFilter(IActionContext actionContext, HashMap<?,?> inputData) throws InterruptedException {
        Boolean isAdmin = Boolean.valueOf(inputData.get("IsAdmin").toString());
        Boolean isIgnore = Boolean.valueOf(inputData.get("IsIgnore").toString());
        String types = (String)inputData.get("MessageTypes");

        List<String> messageTypes = new ArrayList<>();

        if(types != null) {
            for(String msgName : types.split(MESSAGES_SEPARATOR)) {
                msgName = msgName.trim();

                if(!msgName.isEmpty()) {
                    messageTypes.add(msgName);
                }
            }
        }

        WaitAction.countMessages(actionContext, messageTypes, isIgnore, !isAdmin);
    }
	@Description("Marks the matrix as AML 3, if the script consists only of universal actions and the AML version<br/>"
			+ "auto-detection parameter is selected when running.")
	@MessageDirection(direction=Direction.SEND)
	@ActionMethod
    public void MarkerAML3(IActionContext actionContext){
		IActionReport report = actionContext.getReport();
		report.createMessage(StatusType.PASSED, MessageLevel.INFO, "Marks the matrix as AML3");
    }

	@CommonColumns({
			@CommonColumn(value = Column.ServiceName, required = true)
	})
	@ActionMethod
	public void CheckActiveClients (IActionContext actionContext)
	{
		String serviceName = actionContext.getServiceName();
		actionContext.getLogger().info("[{}] disconnect.", serviceName);

		IAcceptorService service = ActionUtil.getService(actionContext, IAcceptorService.class);
		for (ISession session : service.getSessions()) {
			if (!session.isClosed()) {
				return;
			}
		}

		throw new EPSCommonException("Service '"+serviceName+"' don't has active sessions");
	}

    @ActionMethod
    @CommonColumns(@CommonColumn(Column.Reference))
    @CustomColumns({
            @CustomColumn(value = SCRIPT_URIS_COLUMN, required = true),
            @CustomColumn(value = CONTEXT_COLUMN, required = true)
    })
    @Description("Executes scripts loaded by data URIs specified in the <b>" + SCRIPT_URIS_COLUMN + "</b> column.<br>"
            + "Plugin alias must be specified in the <b>" + CONTEXT_COLUMN + "</b> column to set the script execution context.<br>"
            + "Main script must contain the <b>main(actionContext, inputData)</b> method.<br>"
            + "Script execution result will be placed into the <b>ScriptResult</b> field.<br>"
            + "<br>"
            + "Only the following return types are allowed:<br>"
            + "<ul>"
            + "<li>Simple type"
            + "<li>List of allowed types"
            + "<li>Map: string to allowed type"
            + "<li>IMessage with the field values of the allowed types"
            + "</ul>"
            + "Action parameters are accessible through the <b>ScriptArgs</b> field.")
    public HashMap<?, ?> exec(IActionContext actionContext, HashMap<?, ?> inputData) throws SailfishURIException, IOException {
        String systemColumn = actionContext.getSystemColumn(SCRIPT_URIS_COLUMN);
        IDataManager dataManager = actionContext.getDataManager();
        List<SailfishURI> scriptURIs = Arrays.stream(systemColumn.split(",")).map(SailfishURI::unsafeParse).collect(Collectors.toList());
        Set<String> extensions = scriptURIs.stream().map(x -> dataManager.getExtension(x)).collect(Collectors.toSet());

        if(extensions.size() > 1) {
            throw new EPSCommonException("Scripts are of different types: " + String.join(", ", extensions));
        }

        String scriptContext = actionContext.getSystemColumn(CONTEXT_COLUMN);
        String extension = extensions.iterator().next();
        ClassLoader classLoader = actionContext.getPluginClassLoader(scriptContext);
        ScriptEngineManager engineManager = pluginAliasToEngineManager.computeIfAbsent(scriptContext, x -> new ScriptEngineManager(classLoader));
        ScriptEngine engine = engineManager.getEngineByName(extension);

        if(engine == null) {
            List<ScriptEngineFactory> factories = engineManager.getEngineFactories();
            String supportedExtensions = factories.stream().flatMap(x -> x.getExtensions().stream()).collect(Collectors.joining(", "));
            throw new EPSCommonException(String.format("Failed to get script engine by extension: %s (supported: %s)", extension, supportedExtensions));
        }

        for(SailfishURI scriptURI : scriptURIs) {
            try(InputStream stream = dataManager.getDataInputStream(scriptURI)) {
                String script = IOUtils.toString(stream);
                File scriptFile = actionContext.getReport().createFile(StatusType.PASSED, "userscripts", scriptURI.toString().replace(":", File.separator) + "." + extension);

                FileUtils.writeStringToFile(scriptFile, script);

                try {
                    engine.eval(script);
                } catch(ScriptException e) {
                    throw new EPSCommonException("Failed to evaluate script: " + scriptURI, e);
                }
            }
        }

        Invocable invocable = (Invocable)engine;

        try {
            HashMap<String, Object> outputData = new HashMap<>();
            Object result = invocable.invokeFunction("main", actionContext, inputData);
            Set<String> errors = checkType(result, SUPPORTED_TYPES, ArrayUtils.toArray("ScriptResult"));

            if(!errors.isEmpty()) {
                String joinedErrors = String.join(System.lineSeparator(), errors);
                throw new EPSCommonException("Got following errors while validating return value type: " + joinedErrors);
            }

            outputData.put("ScriptArgs", inputData);
            outputData.put("ScriptResult", result);

            return outputData;
        } catch(NoSuchMethodException e) {
            throw new EPSCommonException("None of the scripts contains main(actionContext, inputData) method", e);
        } catch(ScriptException e) {
            throw new EPSCommonException("Failed to execute scripts", e);
        }
    }

    private Set<String> checkType(Object value, List<Class<?>> supportedTypes, String... path) {
        if(value == null) {
            return Collections.emptySet();
        }

        Set<String> errors = new HashSet<>();
        Class<? extends Object> valueClass = value.getClass();

        if(value instanceof List) {
            List<?> list = (List<?>)value;

            for(int i = 0; i < list.size(); i++) {
                errors.addAll(checkType(list.get(i), supportedTypes, ArrayUtils.add(path, String.valueOf(i))));
            }
        } else if(value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>)value;

            for(Object key : map.keySet()) {
                if(key == null) {
                    errors.add(formatError(path, "Map contains a null key"));
                } else if(!(key instanceof String)) {
                    errors.add(formatError(path, "Map contains a non-string key: %s (type: %s)", key, key.getClass()));
                }

                errors.addAll(checkType(map.get(key), supportedTypes, ArrayUtils.add(path, String.valueOf(key))));
            }
        } else if(value instanceof IMessage) {
            IMessage message = (IMessage)value;

            for(String fieldName : message.getFieldNames()) {
                errors.addAll(checkType(message.getField(fieldName), supportedTypes, ArrayUtils.add(path, fieldName)));
            }
        } else if(!supportedTypes.contains(valueClass)) {
            errors.add(formatError(path, "Unsupported value type: %s (value: %s)", valueClass, value));
        }

        return errors;
    }

    private String formatError(String[] path, String error, Object... args) {
        return String.format(String.join(".", path) + ": " + error, args);
    }

    private void isConnected(IActionContext actionContext, IInitiatorService initiatorService) {
        ISession session;
        boolean isConnected = false;
        long waitUntil = System.currentTimeMillis() + Math.max(MIN_TIMEOUT, actionContext.getTimeout());

        do {
            session = initiatorService.getSession();
            isConnected = null != session && !session.isClosed();
        } while(!isConnected && waitUntil > System.currentTimeMillis());

        IActionReport report = actionContext.getReport();
        report.createVerification(isConnected ? StatusType.PASSED : StatusType.FAILED,
                String.format("Service [%s] is connected", initiatorService.getName()),"", "");

        if (!isConnected)
        {
            throw new EPSCommonException(String.format("Service [%s] is not connected", initiatorService.getName()));
        }
    }

    private void isDisconnected(IActionContext actionContext, IInitiatorService initiatorService) {
        ISession session;
        boolean isDisconnected = false;
        long waitUntil = System.currentTimeMillis() + Math.max(MIN_TIMEOUT, actionContext.getTimeout());

        do {
            session = initiatorService.getSession();
            isDisconnected = session == null || session.isClosed();
        } while(!isDisconnected && waitUntil > System.currentTimeMillis());

        IActionReport report = actionContext.getReport();
        report.createVerification(isDisconnected ? StatusType.PASSED : StatusType.FAILED,
                String.format("Service [%s] is disconnected", initiatorService.getName()), "", "");

        if (!isDisconnected)
        {
            throw new EPSCommonException(String.format("Service [%s] is not disconnected", initiatorService.getName()));
        }
    }
    
    private static class JsonMessageIterator implements ICSHIterator<IMessage> {
        
        private final IDictionaryManager dictionaryManager;
        private final Iterator<MessageRow> iterator;
        private final long endTime;

        public JsonMessageIterator(IActionContext actionContext, IService service, IMessage message) {
            SailfishURI dictionaryURI = actionContext.getDictionaryURI();
            IDictionaryStructure dictionary = actionContext.getDictionary(dictionaryURI);
            MessageFilter messageFilter = createMessageFilter(actionContext, message, service, dictionary);
            
            this.dictionaryManager = new DummyDictionaryManager(dictionary, dictionaryURI);
            this.iterator = actionContext.loadMessages(-1, messageFilter).iterator();
            this.endTime = System.currentTimeMillis() + actionContext.getTimeout();
        }
        
        @Override
        public boolean hasNext(long timeout) throws InterruptedException {
            return hasNext(); 
        }

        @Override
        public boolean hasNext() {
            return System.currentTimeMillis() < this.endTime && this.iterator.hasNext();
        }

        @Override
        public IMessage next() {
            return JsonMessageConverter.fromJson(iterator.next().getJson(), dictionaryManager, true);
        }

        @Override
        public void updateCheckPoint() {
            // do nothing
            
        }
        
        private MessageFilter createMessageFilter(IActionContext actionContext, IMessage message, IService service,
                IDictionaryStructure dictionary) {
            ServiceInfo serviceInfo = actionContext.getServiceManager().getServiceInfo(service.getServiceName());

            MessageFilter messageFilter = new MessageFilter();
            messageFilter.setMsgName(message.getName());
            messageFilter.setMsgNameSpace(message.getNamespace());
            messageFilter.setServicesIdSet(ImmutableSet.of(serviceInfo.getID()));
            messageFilter.setSortOrder(false);

            IMessageStructure messageStructure = dictionary.getMessageStructure(message.getName());

            Boolean isAdmin = false;
            if (messageStructure != null) {
                Object isAdminFromDict = messageStructure.getAttributeValueByName(MessageHelper.ATTRIBUTE_IS_ADMIN);
                if (isAdminFromDict instanceof Boolean) {
                    isAdmin = (Boolean) isAdminFromDict;
                }
            }
            messageFilter.setShowAdmin(isAdmin);

            LocalDateTime fromTimestamp = actionContext.getSystemColumn(FROM_TIMESTAMP);
            if (fromTimestamp != null) {
                messageFilter.setStartTime(DateTimeUtility.toTimestamp(fromTimestamp));
            }
            LocalDateTime toTimestamp = actionContext.getSystemColumn(TO_TIMESTAMP);
            if (toTimestamp != null) {
                messageFilter.setFinishTime(DateTimeUtility.toTimestamp(toTimestamp));
            }

            return messageFilter;
        }
    }
}
