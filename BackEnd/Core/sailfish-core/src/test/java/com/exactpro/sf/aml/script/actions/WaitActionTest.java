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

import org.junit.Before;
import org.junit.Test;

import com.exactpro.sf.aml.script.CheckPoint;
import com.exactpro.sf.aml.script.DefaultSettings;
import com.exactpro.sf.aml.script.MetaContainer;
import com.exactpro.sf.aml.scriptutil.MessageCount;
import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.impl.messages.MapMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.services.CollectorServiceHandler;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.fake.FakeSession;
import com.exactpro.sf.util.AbstractTest;

import junit.framework.Assert;

public class WaitActionTest extends AbstractTest {

	private IMessageFactory messageFactory;
	private IActionContext actionContext;

	@Before
	public void init() {
		this.messageFactory = DefaultMessageFactory.getFactory();
		this.actionContext = new DefaultSettings(getScriptContext(), true);
	}

	@Test
	public void TestWaitForApplicationMessage() throws Exception
	{
		IMessage mInstrumentDirectoryDerivatives = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		mInstrumentDirectoryDerivatives.addField("InstrumentClassId", "AAA");

		MetaContainer metaContainer = new MetaContainer();

        ComparatorSettings settings = new ComparatorSettings();
		settings.setMetaContainer(metaContainer);

        IServiceHandler handler = new CollectorServiceHandler();
		ISession isession = new FakeSession(null);
		IMessage m = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m.addField("InstrumentClassId", "AAA");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m);

		MapMessage o = (MapMessage) WaitAction.waitForMessage(actionContext, "serviceName", mInstrumentDirectoryDerivatives, handler, isession, null, 1000, false, true, true, settings);
		Assert.assertEquals(m, o);
	}

	@Test
	public void TestWaitForAdministrationMessage() throws Exception
	{
		IMessage mLogout = this.messageFactory.createMessage("Logout", "namespace");
		mLogout.addField("userid", "AAA");

		MetaContainer metaContainer = new MetaContainer();

        ComparatorSettings settings = new ComparatorSettings();
		settings.setMetaContainer(metaContainer);

        IServiceHandler handler = new CollectorServiceHandler();
		ISession isession = new FakeSession(null);
		IMessage m = this.messageFactory.createMessage("Logout", "namespace");
		m.addField("userid", "AAA");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_ADMIN, m);

		MapMessage o = (MapMessage) WaitAction.waitForMessage(actionContext, "serviceName", mLogout, handler, isession, null, 1000, false, true, false, settings);
		Assert.assertEquals(m, o);
	}

	@Test
	public void TestWaitForMessageWithUpdatedCheckPointValid() throws Exception
	{
		IMessage f1 = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		f1.addField("InstrumentClassId", "AAA");
		IMessage f2 = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		f2.addField("InstrumentClassId", "BBB");
		IMessage f3 = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		f3.addField("InstrumentClassId", "CCC");
		IMessage f4 = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		f4.addField("InstrumentClassId", "DDD");

		MetaContainer metaContainer = new MetaContainer();

        ComparatorSettings settings = new ComparatorSettings();
		settings.setMetaContainer(metaContainer);

		CollectorServiceHandler handler = new CollectorServiceHandler();
		ISession isession = new FakeSession(null);
        CheckPoint checkPoint = new CheckPoint(true);
        handler.registerCheckPoint(isession, ServiceHandlerRoute.FROM_APP, checkPoint);

		IMessage m1 = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m1.addField("InstrumentClassId", "AAA");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m1);

		IMessage m2 = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m2.addField("InstrumentClassId", "BBB");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m2);

		IMessage m3 = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m3.addField("InstrumentClassId", "CCC");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m3);

		IMessage m4 = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m4.addField("InstrumentClassId", "DDD");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m4);

		MapMessage o = (MapMessage) WaitAction.waitForMessage(actionContext, "serviceName", f1, handler, isession, checkPoint, 1000, false, true, true, settings);

		Assert.assertEquals(m1, o);
		Assert.assertEquals(1, handler.getCheckPointIndex(isession, ServiceHandlerRoute.FROM_APP, checkPoint));

		o = (MapMessage) WaitAction.waitForMessage(actionContext, "serviceName", f2, handler, isession, checkPoint, 1000, false, true, true, settings);

		Assert.assertEquals(m2, o);
		Assert.assertEquals(2, handler.getCheckPointIndex(isession, ServiceHandlerRoute.FROM_APP, checkPoint));

		o = (MapMessage) WaitAction.waitForMessage(actionContext, "serviceName", f3, handler, isession, checkPoint, 1000, false, true, true, settings);

		Assert.assertEquals(m3, o);
		Assert.assertEquals(3, handler.getCheckPointIndex(isession, ServiceHandlerRoute.FROM_APP, checkPoint));

		o = (MapMessage) WaitAction.waitForMessage(actionContext, "serviceName", f4, handler, isession, checkPoint, 1000, false, true, true, settings);

		Assert.assertEquals(m4, o);
		Assert.assertEquals(4, handler.getCheckPointIndex(isession, ServiceHandlerRoute.FROM_APP, checkPoint));
	}

	@Test
	public void TestWaitForMessageWithUpdatedCheckPointInvalid() throws Exception
	{
		// create filters
		IMessage f1 = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		f1.addField("InstrumentClassId", "AAA");
		IMessage f2 = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		f2.addField("InstrumentClassId", "BBB");
		IMessage f3 = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		f3.addField("InstrumentClassId", "CCC");
		IMessage f4 = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		f4.addField("InstrumentClassId", "DDD");

		MetaContainer metaContainer = new MetaContainer();

        ComparatorSettings settings = new ComparatorSettings();
		settings.setMetaContainer(metaContainer);

		// put messages to service handler
		CollectorServiceHandler handler = new CollectorServiceHandler();
		ISession isession = new FakeSession(null);
		CheckPoint checkPoint = new CheckPoint(true);
        handler.registerCheckPoint(isession, ServiceHandlerRoute.FROM_APP, checkPoint);

		IMessage m1 = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m1.addField("InstrumentClassId", "AAA");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m1);

		IMessage m2 = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m2.addField("InstrumentClassId", "BBB");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m2);

		IMessage m3 = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m3.addField("InstrumentClassId", "CCC");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m3);

		IMessage m4 = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m4.addField("InstrumentClassId", "DDD");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m4);

		MapMessage o = (MapMessage) WaitAction.waitForMessage(actionContext, "serviceName", f1, handler, isession, checkPoint, 1000, false, true, true, settings);

		Assert.assertEquals(m1, o);
		Assert.assertEquals(1, handler.getCheckPointIndex(isession, ServiceHandlerRoute.FROM_APP, checkPoint));

		o = (MapMessage) WaitAction.waitForMessage(actionContext, "serviceName", f3, handler, isession, checkPoint, 1000, false, true, true, settings);

		Assert.assertEquals(m3, o);
		Assert.assertEquals(3, handler.getCheckPointIndex(isession, ServiceHandlerRoute.FROM_APP, checkPoint));

		try {
			o = (MapMessage) WaitAction.waitForMessage(actionContext, "serviceName", f2, handler, isession, checkPoint, 1000, false, true, true, settings);
			Assert.fail();
		} catch (EPSCommonException e)
		{
			Assert.assertEquals("Timeout", e.getMessage());
			return;
		}

		Assert.assertTrue(false);
	}

	@Test
	public void TestWaitForMessageWithUpdatedCheckPointInvalid2() throws Exception
	{
		IMessage f1 = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		f1.addField("InstrumentClassId", "AAA");
		IMessage f2 = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		f2.addField("InstrumentClassId", "BBB");
		IMessage f3 = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		f3.addField("InstrumentClassId", "CCC");
		IMessage f4 = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		f4.addField("InstrumentClassId", "DDD");

		MetaContainer metaContainer = new MetaContainer();

        ComparatorSettings settings = new ComparatorSettings();
		settings.setMetaContainer(metaContainer);

		CollectorServiceHandler handler = new CollectorServiceHandler();
		ISession isession = new FakeSession(null);
		IMessage m1 = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m1.addField("InstrumentClassId", "AAA");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m1);

		CheckPoint checkPoint = new CheckPoint(true);
        handler.registerCheckPoint(isession, ServiceHandlerRoute.FROM_APP, checkPoint);

		IMessage m2 = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m2.addField("InstrumentClassId", "BBB");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m2);

		IMessage m3 = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m3.addField("InstrumentClassId", "CCC");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m3);

		IMessage m4 = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m4.addField("InstrumentClassId", "DDD");
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m4);

		MapMessage o = (MapMessage) WaitAction.waitForMessage(actionContext, "serviceName", f3, handler, isession, checkPoint, 1000, false, true, true, settings);

		Assert.assertEquals(m3, o);
		Assert.assertEquals(3, handler.getCheckPointIndex(isession, ServiceHandlerRoute.FROM_APP, checkPoint));

		try {
			o = (MapMessage) WaitAction.waitForMessage(actionContext, "serviceName", f2, handler, isession, checkPoint, 1000, false, true, true, settings);
			Assert.fail();
		} catch (EPSCommonException e)
		{
			Assert.assertEquals("Timeout", e.getMessage());
			return;
		}

		Assert.assertTrue(false);
	}

	@Test
	public void TestCountApplicationMessages() throws Exception
	{
		IMessage mInstrumentDirectoryDerivatives = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		mInstrumentDirectoryDerivatives.addField("InstrumentClassId", "PRY");

		MetaContainer metaContainer = new MetaContainer();

        ComparatorSettings settings = new ComparatorSettings();
		settings.setMetaContainer(metaContainer);

        IServiceHandler handler = new CollectorServiceHandler();
		ISession isession = new FakeSession(null);
		IMessage m = this.messageFactory.createMessage("InstrumentDirectoryDerivatives", "namespace");
		m.addField("InstrumentClassId", "PRY");

		IMessage addOrder = this.messageFactory.createMessage("AddOrder", "namespace");
		addOrder.addField("Instrument", 1);
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, addOrder);

		for (int i=0; i< 10; i++) {
			handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, m.cloneMessage());
			handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, addOrder.cloneMessage());
		}

		addOrder = this.messageFactory.createMessage("AddOrder", "namespace");
		addOrder.addField("Instrument", 2);
		handler.putMessage(isession, ServiceHandlerRoute.FROM_APP, addOrder);

		MessageCount mc = MessageCount.fromString("10");
		int o = WaitAction.countMessages(actionContext.getReport(), "serviceName", mInstrumentDirectoryDerivatives, mc , handler, isession, null, true, settings);
		Assert.assertEquals(10, o);
	}

	@Test
	public void TestCountAdministrationMessages() throws Exception
	{
		IMessage mLogout = this.messageFactory.createMessage("Logout", "namespace");
		mLogout.addField("userid", "AAA");

		MetaContainer metaContainer = new MetaContainer();

        ComparatorSettings settings = new ComparatorSettings();
		settings.setMetaContainer(metaContainer);

        IServiceHandler handler = new CollectorServiceHandler();
		ISession isession = new FakeSession(null);
		IMessage m = this.messageFactory.createMessage("Logout", "namespace");
		m.addField("userid", "AAA");

		IMessage addOrder = this.messageFactory.createMessage("AddOrder", "namespace");
		addOrder.addField("Instrument", 1);
		handler.putMessage(isession, ServiceHandlerRoute.FROM_ADMIN, addOrder);

		for (int i=0; i< 10; i++) {
			handler.putMessage(isession, ServiceHandlerRoute.FROM_ADMIN, m.cloneMessage());
			handler.putMessage(isession, ServiceHandlerRoute.FROM_ADMIN, addOrder.cloneMessage());
		}

		addOrder = this.messageFactory.createMessage("AddOrder", "namespace");
		addOrder.addField("Instrument", 2);
		handler.putMessage(isession, ServiceHandlerRoute.FROM_ADMIN, addOrder);

		MessageCount mc = MessageCount.fromString("10");
		int o = WaitAction.countMessages(actionContext.getReport(), "serviceName", mLogout, mc , handler, isession, null, false, settings);
		Assert.assertEquals(10, o);
	}
}