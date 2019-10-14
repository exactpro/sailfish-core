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
package com.exactpro.sf.aml.script.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.exactpro.sf.aml.script.ActionContext;
import com.exactpro.sf.aml.script.CheckPoint;
import com.exactpro.sf.aml.script.MetaContainer;
import com.exactpro.sf.aml.script.actions.exceptions.WaitMessageException;
import com.exactpro.sf.aml.scriptutil.MessageCount;
import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.impl.messages.MapMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.Pair;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.configuration.DataManager;
import com.exactpro.sf.configuration.DefaultLoggingConfiguration;
import com.exactpro.sf.configuration.DictionaryManager;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.ILoggingConfiguration;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.configuration.LoggingConfigurator;
import com.exactpro.sf.configuration.workspace.DefaultWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.scriptrunner.impl.EmptyServiceMonitor;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityManager;
import com.exactpro.sf.services.CollectorServiceHandler;
import com.exactpro.sf.services.DefaultServiceContext;
import com.exactpro.sf.services.EmptyStubServiceHandler;
import com.exactpro.sf.services.IInitiatorService;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.IServiceMonitor;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.ITaskExecutor;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.TaskExecutor;
import com.exactpro.sf.services.fake.FakeSession;
import com.exactpro.sf.services.loopback.LoopbackService;
import com.exactpro.sf.services.loopback.LoopbackServiceSettings;
import com.exactpro.sf.storage.EmptyServiceStorage;
import com.exactpro.sf.storage.IMessageStorage;
import com.exactpro.sf.storage.IServiceStorage;
import com.exactpro.sf.storage.impl.FakeMessageStorage;
import com.exactpro.sf.util.AbstractTest;

import junit.framework.Assert;

public class WaitActionTest extends AbstractTest {

	private ServiceName serviceName = new ServiceName("serviceName");
	private IMessageFactory messageFactory;
	private IActionContext actionContext;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

	@Before
	public void init() {
		this.messageFactory = DefaultMessageFactory.getFactory();
		ScriptContext scriptContext = getScriptContext();

		IInitiatorService service = new LoopbackService();

		IWorkspaceDispatcher workspaceDispatcher = new DefaultWorkspaceDispatcher(Collections.emptyList());
		UtilityManager utilityManager = new UtilityManager();
		IDictionaryManager dictionaryManager = new DictionaryManager(workspaceDispatcher, utilityManager);
		IMessageStorage messageStorage = new FakeMessageStorage();
		IServiceStorage serviceStorage = new EmptyServiceStorage();
		ILoggingConfiguration loggingConfiguration = new DefaultLoggingConfiguration();
		ILoggingConfigurator loggingConfigurator = new LoggingConfigurator(workspaceDispatcher, loggingConfiguration);
		ITaskExecutor taskExecutor = new TaskExecutor();
		IDataManager dataManager = new DataManager(workspaceDispatcher);

		IServiceContext serviceContext = new DefaultServiceContext(dictionaryManager, messageStorage, serviceStorage, loggingConfigurator, taskExecutor, dataManager, workspaceDispatcher);

		IServiceMonitor serviceMonitor = new EmptyServiceMonitor();
		IServiceHandler handler = new EmptyStubServiceHandler();
		IServiceSettings settings = new LoopbackServiceSettings();
		ServiceName name = new ServiceName("ServiceName");

		service.init(serviceContext, serviceMonitor, handler, settings, name);
		service.start();

		Mockito.when(scriptContext.getEnvironmentManager().getConnectionManager().getService(Mockito.any())).thenReturn(service);

		ActionContext actionContext = new ActionContext(scriptContext, true);
		actionContext.setMetaContainer(new MetaContainer());
		actionContext.setServiceName(serviceName.toString());

		this.actionContext = actionContext;
	}

    @Test
    public void waitActionFailed() throws WaitMessageException{
        expectedEx.expect(WaitMessageException.class);
        expectedEx.expectMessage("Timeout");

        IMessage mInstrumentDirectoryDerivatives = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
        mInstrumentDirectoryDerivatives.addField("InstrumentClassId", "AAA");

        MetaContainer metaContainer = new MetaContainer();

        ComparatorSettings settings = new ComparatorSettings();
        settings.setMetaContainer(metaContainer);

        List<Pair<IMessage, ComparisonResult>> results = new ArrayList<>();
        results.add(new Pair<>(mInstrumentDirectoryDerivatives, new ComparisonResult("Test").setStatus(StatusType.FAILED)));
		WaitAction.processResults(actionContext.getReport(), settings, results, null, serviceName.toString(), true, true, "description", new CheckPoint());
    }

    @Test
    public void waitActionConditionallyFailed(){
        expectedEx.expect(WaitMessageException.class);
        expectedEx.expectMessage("ConditionallyFailedException");

        IMessage mInstrumentDirectoryDerivatives = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
        mInstrumentDirectoryDerivatives.addField("InstrumentClassId", "AAA");

        MetaContainer metaContainer = new MetaContainer();

        ComparatorSettings settings = new ComparatorSettings();
        settings.setMetaContainer(metaContainer);

        List<Pair<IMessage, ComparisonResult>> results = new ArrayList<>();
        ComparisonResult comparisonResult = new ComparisonResult("Test");
        comparisonResult.setStatus(StatusType.CONDITIONALLY_PASSED);
        comparisonResult.setException(new RuntimeException("ConditionallyFailedException"));

        results.add(new Pair<>(mInstrumentDirectoryDerivatives, comparisonResult));
        WaitAction.processResults(actionContext.getReport(), settings, results, null, serviceName.toString(), true, false, "description", new CheckPoint());
    }

	@Test
	public void TestWaitForApplicationMessage() throws Exception
	{
        IMessage mInstrumentDirectoryDerivatives = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		mInstrumentDirectoryDerivatives.addField("InstrumentClassId", "AAA");

		MetaContainer metaContainer = new MetaContainer();

        ComparatorSettings settings = new ComparatorSettings();
		settings.setMetaContainer(metaContainer);

        IServiceHandler handler = new CollectorServiceHandler();
		ISession isession = new FakeSession(null);
        IMessage m = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m.addField("InstrumentClassId", "AAA");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m);

		MapMessage o = (MapMessage) WaitAction.waitForMessage(actionContext, serviceName.toString(), mInstrumentDirectoryDerivatives, handler, isession, null, 1000, false, true, true, settings);
		Assert.assertEquals(m, o);
	}

	@Test
	public void TestWaitForAdministrationMessage() throws Exception
	{
        IMessage mLogout = messageFactory.createMessage("Logout", "namespace");
		mLogout.addField("userid", "AAA");

		MetaContainer metaContainer = new MetaContainer();

        ComparatorSettings settings = new ComparatorSettings();
		settings.setMetaContainer(metaContainer);

        IServiceHandler handler = new CollectorServiceHandler();
		ISession isession = new FakeSession(null);
        IMessage m = messageFactory.createMessage("Logout", "namespace");
		m.addField("userid", "AAA");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_ADMIN, m);

		MapMessage o = (MapMessage) WaitAction.waitForMessage(actionContext, serviceName.toString(), mLogout, handler, isession, null, 1000, false, true, false, settings);
		Assert.assertEquals(m, o);
	}

	@Test
	public void TestWaitForMessageWithUpdatedCheckPointValid() throws Exception
	{
        IMessage f1 = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		f1.addField("InstrumentClassId", "AAA");
        IMessage f2 = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		f2.addField("InstrumentClassId", "BBB");
        IMessage f3 = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		f3.addField("InstrumentClassId", "CCC");
        IMessage f4 = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		f4.addField("InstrumentClassId", "DDD");

		MetaContainer metaContainer = new MetaContainer();

        ComparatorSettings settings = new ComparatorSettings();
		settings.setMetaContainer(metaContainer);

		CollectorServiceHandler handler = new CollectorServiceHandler();
		ISession isession = new FakeSession(null);
        CheckPoint checkPoint = new CheckPoint(true);
        handler.registerCheckPoint(isession, ServiceHandlerRoute.FROM_APP, checkPoint);

        IMessage m1 = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m1.addField("InstrumentClassId", "AAA");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m1);

        IMessage m2 = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m2.addField("InstrumentClassId", "BBB");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m2);

        IMessage m3 = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m3.addField("InstrumentClassId", "CCC");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m3);

        IMessage m4 = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m4.addField("InstrumentClassId", "DDD");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m4);

		MapMessage o = (MapMessage) WaitAction.waitForMessage(actionContext, serviceName.toString(), f1, handler, isession, checkPoint, 1000, false, true, true, settings);

		Assert.assertEquals(m1, o);
		Assert.assertEquals(1, handler.getCheckPointIndex(isession, ServiceHandlerRoute.FROM_APP, checkPoint));

		o = (MapMessage) WaitAction.waitForMessage(actionContext, serviceName.toString(), f2, handler, isession, checkPoint, 1000, false, true, true, settings);

		Assert.assertEquals(m2, o);
		Assert.assertEquals(2, handler.getCheckPointIndex(isession, ServiceHandlerRoute.FROM_APP, checkPoint));

		o = (MapMessage) WaitAction.waitForMessage(actionContext, serviceName.toString(), f3, handler, isession, checkPoint, 1000, false, true, true, settings);

		Assert.assertEquals(m3, o);
		Assert.assertEquals(3, handler.getCheckPointIndex(isession, ServiceHandlerRoute.FROM_APP, checkPoint));

		o = (MapMessage) WaitAction.waitForMessage(actionContext, serviceName.toString(), f4, handler, isession, checkPoint, 1000, false, true, true, settings);

		Assert.assertEquals(m4, o);
		Assert.assertEquals(4, handler.getCheckPointIndex(isession, ServiceHandlerRoute.FROM_APP, checkPoint));
	}

	@Test
	public void TestWaitForMessageWithUpdatedCheckPointInvalid() throws Exception
	{
		// create filters
        IMessage f1 = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		f1.addField("InstrumentClassId", "AAA");
        IMessage f2 = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		f2.addField("InstrumentClassId", "BBB");
        IMessage f3 = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		f3.addField("InstrumentClassId", "CCC");
        IMessage f4 = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		f4.addField("InstrumentClassId", "DDD");

		MetaContainer metaContainer = new MetaContainer();

        ComparatorSettings settings = new ComparatorSettings();
		settings.setMetaContainer(metaContainer);

		// put messages to service handler
		CollectorServiceHandler handler = new CollectorServiceHandler();
		ISession isession = new FakeSession(null);
		CheckPoint checkPoint = new CheckPoint(true);
        handler.registerCheckPoint(isession, ServiceHandlerRoute.FROM_APP, checkPoint);

        IMessage m1 = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m1.addField("InstrumentClassId", "AAA");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m1);

        IMessage m2 = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m2.addField("InstrumentClassId", "BBB");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m2);

        IMessage m3 = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m3.addField("InstrumentClassId", "CCC");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m3);

        IMessage m4 = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m4.addField("InstrumentClassId", "DDD");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m4);

		MapMessage o = (MapMessage) WaitAction.waitForMessage(actionContext, serviceName.toString(), f1, handler, isession, checkPoint, 1000, false, true, true, settings);

		Assert.assertEquals(m1, o);
		Assert.assertEquals(1, handler.getCheckPointIndex(isession, ServiceHandlerRoute.FROM_APP, checkPoint));

		o = (MapMessage) WaitAction.waitForMessage(actionContext, serviceName.toString(), f3, handler, isession, checkPoint, 1000, false, true, true, settings);

		Assert.assertEquals(m3, o);
		Assert.assertEquals(3, handler.getCheckPointIndex(isession, ServiceHandlerRoute.FROM_APP, checkPoint));

		try {
			o = (MapMessage) WaitAction.waitForMessage(actionContext, serviceName.toString(), f2, handler, isession, checkPoint, 1000, false, true, true, settings);
			Assert.fail();
		} catch (EPSCommonException e)
		{
            Assert.assertTrue(e.getMessage().startsWith("Timeout. No messages matching filter from the checkpoint till the timeout is exceeded appx"));
			return;
		}

		Assert.assertTrue(false);
	}

	@Test
	public void TestWaitForMessageWithUpdatedCheckPointInvalid2() throws Exception
	{
        IMessage f1 = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		f1.addField("InstrumentClassId", "AAA");
        IMessage f2 = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		f2.addField("InstrumentClassId", "BBB");
        IMessage f3 = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		f3.addField("InstrumentClassId", "CCC");
        IMessage f4 = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		f4.addField("InstrumentClassId", "DDD");

		MetaContainer metaContainer = new MetaContainer();

        ComparatorSettings settings = new ComparatorSettings();
		settings.setMetaContainer(metaContainer);

		CollectorServiceHandler handler = new CollectorServiceHandler();
		ISession isession = new FakeSession(null);
        IMessage m1 = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m1.addField("InstrumentClassId", "AAA");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m1);

		CheckPoint checkPoint = new CheckPoint(true);
        handler.registerCheckPoint(isession, ServiceHandlerRoute.FROM_APP, checkPoint);

        IMessage m2 = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m2.addField("InstrumentClassId", "BBB");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m2);

        IMessage m3 = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m3.addField("InstrumentClassId", "CCC");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m3);

        IMessage m4 = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m4.addField("InstrumentClassId", "DDD");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m4);

		MapMessage o = (MapMessage) WaitAction.waitForMessage(actionContext, serviceName.toString(), f3, handler, isession, checkPoint, 1000, false, true, true, settings);

		Assert.assertEquals(m3, o);
		Assert.assertEquals(3, handler.getCheckPointIndex(isession, ServiceHandlerRoute.FROM_APP, checkPoint));

		try {
			o = (MapMessage) WaitAction.waitForMessage(actionContext, serviceName.toString(), f2, handler, isession, checkPoint, 1000, false, true, true, settings);
			Assert.fail();
		} catch (EPSCommonException e)
		{
            Assert.assertTrue(e.getMessage().startsWith("Timeout. No messages matching filter from the checkpoint till the timeout is exceeded appx"));
			return;
		}

		Assert.assertTrue(false);
	}

	@Test
	public void TestCountApplicationMessages() throws Exception
	{
        IMessage mInstrumentDirectoryDerivatives = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		mInstrumentDirectoryDerivatives.addField("InstrumentClassId", "PRY");

		MetaContainer metaContainer = new MetaContainer();

        ComparatorSettings settings = new ComparatorSettings();
		settings.setMetaContainer(metaContainer);

        IServiceHandler handler = new CollectorServiceHandler();
		ISession isession = new FakeSession(null);
        IMessage m = messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m.addField("InstrumentClassId", "PRY");

        IMessage addOrder = messageFactory.createMessage("AddOrder", "namespace");
		addOrder.addField("Instrument", 1);
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, addOrder);

		for (int i=0; i< 10; i++) {
			handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m.cloneMessage());
			handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, addOrder.cloneMessage());
		}

        addOrder = messageFactory.createMessage("AddOrder", "namespace");
		addOrder.addField("Instrument", 2);
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, addOrder);

		MessageCount mc = MessageCount.fromString("10");
		int o = WaitAction.countMessages(actionContext.getReport(), serviceName.toString(), mInstrumentDirectoryDerivatives, mc , handler, isession, null, true, settings);
		Assert.assertEquals(10, o);
	}

	@Test
	public void TestCountAdministrationMessages() throws Exception
	{
        IMessage mLogout = messageFactory.createMessage("Logout", "namespace");
		mLogout.addField("userid", "AAA");

		MetaContainer metaContainer = new MetaContainer();

        ComparatorSettings settings = new ComparatorSettings();
		settings.setMetaContainer(metaContainer);

        IServiceHandler handler = new CollectorServiceHandler();
		ISession isession = new FakeSession(null);
        IMessage m = messageFactory.createMessage("Logout", "namespace");
		m.addField("userid", "AAA");

        IMessage addOrder = messageFactory.createMessage("AddOrder", "namespace");
		addOrder.addField("Instrument", 1);
		handler.putMessage(isession, ServiceHandlerRoute.FROM_ADMIN, addOrder);

		for (int i=0; i< 10; i++) {
			handler.putMessage(isession, ServiceHandlerRoute.FROM_ADMIN, m.cloneMessage());
			handler.putMessage(isession, ServiceHandlerRoute.FROM_ADMIN, addOrder.cloneMessage());
		}

        addOrder = messageFactory.createMessage("AddOrder", "namespace");
		addOrder.addField("Instrument", 2);
		handler.putMessage(isession, ServiceHandlerRoute.FROM_ADMIN, addOrder);

		MessageCount mc = MessageCount.fromString("10");
		int o = WaitAction.countMessages(actionContext.getReport(), serviceName.toString(), mLogout, mc , handler, isession, null, false, settings);
		Assert.assertEquals(10, o);
	}
}