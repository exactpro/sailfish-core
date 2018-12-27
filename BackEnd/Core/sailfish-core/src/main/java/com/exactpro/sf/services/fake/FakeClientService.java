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
package com.exactpro.sf.services.fake;

import com.exactpro.sf.aml.generator.TypeConverter;
import com.exactpro.sf.aml.script.actions.WaitAction;
import com.exactpro.sf.aml.scriptutil.StaticUtil.SimpleMvelFilter;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.services.AbstractInitiatorService;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.storage.IMessageStorage;

/**
 * Simplest FAKE client to test new ideas
 *
 * @author Andrey Senov
 *
 */

public class FakeClientService extends AbstractInitiatorService {

	protected volatile FakeSession session;

	@Override
	protected void internalStart() throws Exception {
	    connect();
	}
	
    @Override
    protected void disposeResources() {
        try {
            FakeSession session = getSession();
            if (session != null) {
                session.close();
            }
        } finally {
            super.disposeResources();
        }
    }

    public IMessageStorage getStorage() {
        return this.storage;
    }

    public ServiceInfo getServiceInfo() {
        return this.serviceInfo;
    }

    public IDictionaryStructure getDictionary() {
        return this.dictionary;
    }

	@Override
	public FakeSession getSession() {
		return this.session;
	}

	public void removeFilters(IMessage message)
	{
		IMessageStructure msgStruct = dictionary.getMessageStructure(message.getName());

		for (IFieldStructure fldStruct : msgStruct.getFields()) {

			Object field = message.getField(fldStruct.getName());

			if(null == field)
			{
				continue;
			}

			if(field instanceof SimpleMvelFilter)
			{
				Class<?> clazz;
				try {
					clazz = Class.forName(fldStruct.getJavaType().value());
				} catch (ClassNotFoundException e) {
					throw new EPSCommonException("Cannot associate  [" + fldStruct.getJavaType().value() + "] with any class" );
				}
				SimpleMvelFilter filter = (SimpleMvelFilter) field;
				Object fieldValue = TypeConverter.convertToObject(clazz, filter.getCondition());

				message.removeField(fldStruct.getName());
				message.addField(fldStruct.getName(), fieldValue);
			}
		}
	}

	public void messageReceived(IMessage message) throws Exception
	{
		removeFilters(message);
		boolean admin = false;
		persistMessage(admin, message, serviceName.toString(), serviceName.toString());

		if (admin) {
			handler.putMessage(session, ServiceHandlerRoute.FROM_ADMIN, message);
		} else {
			handler.putMessage(session, ServiceHandlerRoute.FROM_APP, message);
		}
	}

	public void messageSent(IMessage message) throws Exception
	{
		boolean admin = false;
		persistMessage(admin, message, serviceName.toString(), serviceName.toString());

		// Putting message into a service handler
		if (admin)
		{
			handler.putMessage(session, ServiceHandlerRoute.TO_ADMIN, message);
		}
		else
		{
			handler.putMessage(session, ServiceHandlerRoute.TO_APP, message);
		}
	}

	// Persists message to storage
	private void persistMessage(boolean admin, IMessage message, String from, String to)
	{
		MsgMetaData metaData = message.getMetaData();
		metaData.setAdmin(admin);
		metaData.setFromService(from);
		metaData.setToService(to);
		metaData.setServiceInfo(serviceInfo);
        metaData.setProtocol("FAKE");
        metaData.setDictionaryURI(settings.getDictionaryName());

		try
		{
            storage.storeMessage(message);
		}
		catch (Exception e)
		{
			logger.error("{}", e);
		}
	}

	@Override
    public FakeSettings getSettings() {
        return (FakeSettings) super.getSettings();
    }

	@Override
	public void connect() {
	    this.session = new FakeSession(this);
	}

	@Override
    public IMessage receive(IActionContext actionContext, IMessage msg) throws InterruptedException {
		try {
	        messageReceived(msg);
        } catch (Exception e) {
	        throw new EPSCommonException(e);
        }

		return (IMessage) WaitAction.waitForMessage(actionContext, msg, !msg.getMetaData().isAdmin());
    }

    @Override
    protected void internalInit() throws Exception {
        // do nothing
    }

    @Override
    protected MessageHelper createMessageHelper(IMessageFactory messageFactory, IDictionaryStructure dictionary) {
        return null;
    }

    @Override
    protected String getEndpointName() {
        return "Fake";
    }
}
