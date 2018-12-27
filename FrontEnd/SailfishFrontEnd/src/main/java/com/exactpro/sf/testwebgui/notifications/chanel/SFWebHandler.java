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
package com.exactpro.sf.testwebgui.notifications.chanel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.testwebgui.SFWebApplication;
import com.exactpro.sf.testwebgui.notifications.events.EventSubscriber;
import com.exactpro.sf.testwebgui.notifications.matrices.MatrixUpdateSubscriber;
import com.exactpro.sf.testwebgui.notifications.messages.CloseChannel;
import com.exactpro.sf.testwebgui.notifications.messages.CloseRequest;
import com.exactpro.sf.testwebgui.notifications.messages.EventUpdateRequest;
import com.exactpro.sf.testwebgui.notifications.messages.MatrixUpdateRequest;
import com.exactpro.sf.testwebgui.notifications.messages.MessagesUpdateRequest;
import com.exactpro.sf.testwebgui.notifications.messages.MessagesUpdateSubscriber;
import com.exactpro.sf.testwebgui.notifications.messages.RequestStatus;
import com.exactpro.sf.testwebgui.notifications.messages.ScriptrunnerUpdateRequest;
import com.exactpro.sf.testwebgui.notifications.messages.ServiceUpdateRequest;
import com.exactpro.sf.testwebgui.notifications.scriptrunner.ScriptrunUpdateSubscriber;
import com.exactpro.sf.testwebgui.notifications.services.EnvironmentUpdateSubscriber;
import com.exactprosystems.webchannels.IUpdateRequestListener;
import com.exactprosystems.webchannels.IUpdateRetriever;
import com.exactprosystems.webchannels.channel.AbstractChannel;
import com.exactprosystems.webchannels.channel.IChannelHandler;
import com.exactprosystems.webchannels.messages.AbstractMessage;
import com.exactprosystems.webchannels.messages.HeartBeat;

//TODO: investigate possible concurrency problems with subscribers map access
public class SFWebHandler implements IChannelHandler{
	
	private static final Logger logger = LoggerFactory.getLogger(SFWebHandler.class);
	private final String id;
	private AbstractChannel channel;
	private Map<IUpdateRetriever, List<IUpdateRequestListener>> subscribers;
	
	public SFWebHandler(String id) {
		
		this.id = id;
		this.subscribers = new ConcurrentHashMap<IUpdateRetriever, List<IUpdateRequestListener>>();
		logger.debug("SFWebHandler {} created", this);
		
	}
	
    private void register(IUpdateRequestListener listener, IUpdateRetriever retriever) throws Exception{
		
		List<IUpdateRequestListener> retriverLsteners = subscribers.get(retriever);
		
		if (retriverLsteners == null) {
			retriverLsteners = new CopyOnWriteArrayList<IUpdateRequestListener>();
			subscribers.put(retriever, retriverLsteners);
		}
		
		for (IUpdateRequestListener subscriber : retriverLsteners) {
			
			try {
				logger.debug("Handler {} Channel {} unregister subscriber {}", new Object[] {id, channel, subscriber});
				retriever.unregisterUpdateRequest(subscriber);
				subscriber.destroy();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
			
		}
		
		retriverLsteners.clear();
			
		logger.debug("Handler {} Channel {} register subscriber {}", new Object[] {id, channel, listener});
		retriever.registerUpdateRequest(listener);		
		
		retriever.synchronizeUpdateRequest(listener);
		retriverLsteners.add(listener);
		
	}
	
	@Override
	public void onCreate(AbstractChannel channel) {
		
		logger.debug("SFWebHandler {} onCreate() on channel {}", id, channel);
		this.channel = channel;
		
	}

	@Override
	public AbstractMessage onReceive(Object message, long seqnum) {
		
		logger.debug("SFWebHandler {} onReceive() message {} for channel {}", id, message, channel);
		
		AbstractMessage response = null;
		
		try {
			
			if (message instanceof ScriptrunnerUpdateRequest) {
				
				ScriptrunnerUpdateRequest request = (ScriptrunnerUpdateRequest) message;
				
				RequestStatus reqStatus = new RequestStatus();
				reqStatus.setRequestId(request.getRequestId());
				
				ScriptrunUpdateSubscriber scriptrunSubscriber = new ScriptrunUpdateSubscriber(request.getRequestId(), channel);
				register(scriptrunSubscriber, SFWebApplication.getInstance().getScriptrunsUpdateRetriever());
				reqStatus.setSuccess(true);
				response = reqStatus;			
				
			} else if (message instanceof MatrixUpdateRequest) {
				
				MatrixUpdateRequest request = (MatrixUpdateRequest) message;
				
				RequestStatus reqStatus = new RequestStatus();
				reqStatus.setRequestId(request.getRequestId());
				
				MatrixUpdateSubscriber matrixSubscriber = new MatrixUpdateSubscriber(request.getRequestId(), channel);
				register(matrixSubscriber, SFWebApplication.getInstance().getMatrixUpdateRetriever());
				reqStatus.setSuccess(true);
				response = reqStatus;
			
			} else if (message instanceof EventUpdateRequest) {
				
				EventUpdateRequest request = (EventUpdateRequest) message;
				
				RequestStatus reqStatus = new RequestStatus();
				reqStatus.setRequestId(request.getRequestId());
				
				EventSubscriber eventSubscriber = new EventSubscriber(request.getRequestId(), channel);
				register(eventSubscriber, SFWebApplication.getInstance().getEventRetriever());
				reqStatus.setSuccess(true);
				response = reqStatus;
				
			} else if (message instanceof MessagesUpdateRequest) {
				MessagesUpdateRequest request = (MessagesUpdateRequest) message;

				RequestStatus reqStatus = new RequestStatus();
				reqStatus.setRequestId(request.getRequestId());

				MessagesUpdateSubscriber updateSubscriber = new MessagesUpdateSubscriber(request.getRequestId(), channel);
				register(updateSubscriber, SFWebApplication.getInstance().getMessagesUpdateRetriever());
				reqStatus.setSuccess(true);
				response = reqStatus;
			} else if (message instanceof ServiceUpdateRequest) {
				ServiceUpdateRequest request = (ServiceUpdateRequest) message;

				RequestStatus reqStatus = new RequestStatus();
				reqStatus.setRequestId(request.getRequestId());

				EnvironmentUpdateSubscriber updateSubscriber = new EnvironmentUpdateSubscriber(request.getRequestId(), channel);
				register(updateSubscriber, SFWebApplication.getInstance().getEnvironmentUpdateRetriever());
				reqStatus.setSuccess(true);
				response = reqStatus;
			} else if (message instanceof CloseRequest)	{
			
				//TODO
				//Implement logic for unsubscribing
				
			} else if(message instanceof HeartBeat){

            } else {
				
				throw new Exception("Handler " + this + " error. Incorrect incoming message " + message + " for channel " + channel);
				
			}
		
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		
		logger.debug("Invoke SFWebHandler {} send response {} for channel {}", new Object[] {id, response, channel});
		
		return response;
		
	}

	@Override
	public void onSend(Object message, long seqnum) {

		logger.debug("Handler {} Channel {} send message {}", new Object[] {id, channel, message});
		
	}

	@Override
	public void onClose() {
		
		for (IUpdateRetriever retriever : subscribers.keySet()) {
			for (IUpdateRequestListener subscriber : subscribers.get(retriever)) {
				try {
					logger.debug("Handler {} Channel {} unregister subscriber {}", new Object[] {id, channel, subscriber});
					retriever.unregisterUpdateRequest(subscriber);
					subscriber.destroy();
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
			subscribers.get(retriever).clear();
		}
		
		subscribers.clear();
		
		logger.debug("Handler {} Channel {} close", id, channel);
		channel.sendMessage(new CloseChannel());
		
	}

	@Override
	public void onException(Throwable t) {
		
		logger.error(t.getMessage(), t);
		
	}

	@Override
	public void onIdle() {
		
		logger.debug("Handler {} Channel {} onIdle() invoke.", id, channel);
		HeartBeat heartBeat = new HeartBeat();
		channel.sendMessage(heartBeat);
		
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this).append("id", id).toString();
	}
}
