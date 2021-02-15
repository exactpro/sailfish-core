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
package com.exactpro.sf.services.fix;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

import org.quickfixj.CharsetSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.SendMessageFailedException;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.ServiceException;
import com.exactpro.sf.services.fix.converter.MessageConvertException;
import com.exactpro.sf.services.fix.converter.dirty.DirtyQFJIMessageConverter;
import com.exactpro.sf.services.fix.converter.dirty.struct.RawMessage;
import com.exactpro.sf.storage.IMessageStorage;

import quickfix.DataDictionary;
import quickfix.InvalidMessage;
import quickfix.Message;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionState;
import quickfix.SystemTime;

/**
 *
 * @author dmitry.guriev
 *
 */
public class FIXSession implements ISession {

    private final Logger logger;

	private final String name;
	private final SessionID sessionId;
	private final IMessageStorage storage;
	private final DirtyQFJIMessageConverter converter;
	private final MessageHelper messageHelper;
    private final DataDictionary dataDictionary;

	private volatile ServiceInfo serviceInfo;

	public FIXSession(String sessionName, SessionID sessionId, IMessageStorage storage, DirtyQFJIMessageConverter converter, MessageHelper messageHelper) {
		this.name = sessionName;
		this.sessionId = sessionId;
		this.storage = storage;
        this.converter = converter;
        this.messageHelper = messageHelper;
        this.logger = LoggerFactory.getLogger(getClass().getName() + "@" + Integer.toHexString(hashCode()));
        // it is the simples way to get the data dictionary here
        dataDictionary = new QFJDictionaryAdapter(converter.getDictionary());
    }

	@Override
	public void close() {
		lookupSession().logout();
	}

	@Override
	public void forceClose() {
        try {
            lookupSession().disconnect("Disconnect without logout", true);
        } catch (IOException e) {
            throw new ServiceException("Can not force close session for service " + name, e);
        }
    }

	@Override
	public boolean isClosed() {
		Session session = Session.lookupSession(sessionId);
        return session == null || !session.isLoggedOn();
	}

	@Override
	public IMessage send(Object message) {
        boolean isSendSuccess = false;
		if (message instanceof Message) {
            isSendSuccess = lookupSession().sendRaw((Message) message);
		} else if (message instanceof String) {
            isSendSuccess = lookupSession().send((String) message);
		} else if (message instanceof IMessage) {
			Session session = lookupSession();
			IMessage imsg = (IMessage) message;
            if(messageHelper != null) {
                imsg = messageHelper.prepareMessageToEncode(imsg, null);
			} else {
                throw new ServiceException("Service '" + name + "' is configured incorrectly");
			}

            try {
                if(converter != null) {
                    Message qfjMsg = converter.convert(imsg, session.getSessionID().isFIXT());
                    isSendSuccess = session.sendRaw(qfjMsg);
                    if (!isSendSuccess) {
                        throw new SendMessageFailedException("Send message " + imsg.getName() + " failed");
                    }
                    return converter.convert(qfjMsg);
                } else {
                    throw new ServiceException("Service '" + name + "' is configured incorrectly");
                }
            } catch (MessageConvertException e) {
                throw new ServiceException(new StringBuilder("Send message ").append(imsg.getName()).append(" failed").toString(), e);
            }
        } else {
			throw new EPSCommonException("Unknown type of message: " + message.getClass().getCanonicalName());
		}
        if (!isSendSuccess) {
            throw new SendMessageFailedException("Send message failed. Message: " + message);
        }

		return null;
	}

    protected Session lookupSession() {
        return Objects.requireNonNull(Session.lookupSession(sessionId), "FIX session has not yet been registered or has already been unregistered");
    }

	@Override
    public IMessage sendDirty(Object message) throws InterruptedException {
        if (message instanceof IMessage) {
            IMessage iMessage = (IMessage)message;
            iMessage.getMetaData().setDirty(true);

            lockSenderMsgSeqNum();

            try {
                RawMessage convertedMessage = converter.convertDirty(iMessage, iMessage.getName(), true, getBeginString(), getExpectedSenderNum(), getSenderCompID(), getTargetCompID());
                String messageString = convertedMessage.toString();

                sendRawMessage(getExpectedSenderNum(), messageString);
                storeMessage(iMessage, messageString);
            } catch(MessageConvertException | IOException e) {
                logger.error("Failed to send dirty message", e);
            }

            unlockSenderMsgSeqNum();

            return iMessage;
        } else {
            logger.error("This method not support send dirty message which have got type {}", message.getClass().getName());
            return send(message);
        }
    }

    @Override
    public void sendRaw(byte[] rawData) throws InterruptedException {
        // FIX uses this encoding by default
        String messageData = CharsetSupport.getCharsetInstance().decode(ByteBuffer.wrap(rawData)).toString();
        try {
            logger.trace("Processing raw message: {}", messageData);
            Message message = new Message();
            // do not validate anything. it will be checked during the regular send action
            message.fromString(messageData, dataDictionary, false);
            IMessage converted = converter.convert(message, false, false);
            converted.removeField(FixMessageHelper.HEADER);
            converted.removeField(FixMessageHelper.TRAILER);
            logger.debug("Sending message converted from raw: {} - {}", converted.getName(), converted);
            send(converted);
        } catch (InvalidMessage ex) {
            throw new SendMessageFailedException("Cannot parse the raw message: " + messageData, ex);
        } catch (MessageConvertException ex) {
            throw new SendMessageFailedException("Cannot convert QFJ message to IMessage", ex);
        }
    }

    @Override
	public String getName() {
        return name;
	}

	@Override
	public boolean isLoggedOn() {
		return lookupSession().isLoggedOn();
	}

	public DataDictionary getDataDictionary() {
		return lookupSession().getDataDictionary();
	}

	public void lockSenderMsgSeqNum() {
		Session session = lookupSession();

		SessionState state = session.getSessionState();

		state.lockSenderMsgSeqNum();
	}

	public void unlockSenderMsgSeqNum() {
		Session session = lookupSession();

		SessionState state = session.getSessionState();

		state.unlockSenderMsgSeqNum();
	}

	/**
	 * Send raw message. Usually is used in incorrect test scenarios
	 */
	public void sendRawMessage(int msgSeqNum, String messageName, String messageString) throws IOException {
        IMessage message = messageHelper.getMessageFactory().createMessage(messageName, getBeginString());
        message.addField("RawMessage", messageString);

        sendRawMessage(msgSeqNum, messageString);
        storeMessage(message, messageString);
	}

	private void sendRawMessage(int msgSeqNum, String messageString) throws IOException {
		Session session = lookupSession();
		SessionState state = session.getSessionState();

        state.setLastSentTime(SystemTime.currentTimeMillis());
        session.send(messageString);
        state.set(msgSeqNum, messageString);
        state.incrNextSenderMsgSeqNum();
	}

    private void storeMessage(IMessage message, String messageString) {
        MsgMetaData metaData = message.getMetaData();

        metaData.setAdmin(false);
        metaData.setFromService(getSenderCompID());
        metaData.setToService(getTargetCompID());
        metaData.setRawMessage(messageString.getBytes(CharsetSupport.getCharsetInstance()));
        metaData.setServiceInfo(serviceInfo);

        storage.storeMessage(message);
    }

	public int getExpectedSenderNum() {

		Session session = lookupSession();

		SessionState state = session.getSessionState();

		try {
			return state.getMessageStore().getNextSenderMsgSeqNum();
		} catch (IOException e) {
			session.getLog().onEvent(
					"getNextSenderMsgSeqNum failed: " + e.getMessage());
			return -1;
		}
	}

	public int setExpectedSenderNum(int seq) {

		Session session = lookupSession();

		SessionState state = session.getSessionState();

		try {
			state.getMessageStore().setNextSenderMsgSeqNum(seq);
            return seq;
        } catch(IOException e) {
			session.getLog().onEvent(
					"setNextSenderMsgSeqNum failed: " + e.getMessage());
			return -1;
		}
    }

	public int addExpectedSenderNum(int seq) {
		int oldSeq = getExpectedSenderNum();
		if (oldSeq == -1) {
			return -1;
		}
		oldSeq += seq;
		return setExpectedSenderNum(oldSeq);
	}

	public String getBeginString() {
		Session session = lookupSession();
		return session.getSessionID().getBeginString();
	}


	public String getSenderCompID() {
		Session session = lookupSession();
		return session.getSessionID().getSenderCompID();
	}

	public String getTargetCompID() {
		Session session = lookupSession();
		return session.getSessionID().getTargetCompID();
	}

	public SessionID getSessionID() {
        return sessionId;
	}

	public ServiceInfo getServiceInfo() {
		return serviceInfo;
	}

	public void setServiceInfo(ServiceInfo serviceInfo) {
		this.serviceInfo = serviceInfo;
	}

    public DirtyQFJIMessageConverter getConverter() {
        return converter;
	}

	@Override
	public int hashCode()
	{
		return getName().hashCode();
	}

	@Override
	public boolean equals(Object o) {
        return o instanceof FIXSession && getName().equals(((ISession)o).getName());
    }
}
