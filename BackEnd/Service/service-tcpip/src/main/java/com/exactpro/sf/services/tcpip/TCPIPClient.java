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
package com.exactpro.sf.services.tcpip;

import org.apache.commons.lang3.StringUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.session.IoSession;

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.messages.IncomingMessageFactory;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.ServiceException;
import com.exactpro.sf.services.mina.AbstractMINATCPService;
import com.exactpro.sf.services.mina.MINAUtil;
import com.exactpro.sf.services.util.ServiceUtil;

public class TCPIPClient extends AbstractMINATCPService {
	private Class<? extends AbstractCodec> codecClass;
	private IFieldConverter fieldConverter;
	protected IDataManager dataManager;

	@Override
    protected void internalInit() {
        if(getSettings().isDepersonalizationIncomingMessages()) {
            messageFactory = new IncomingMessageFactory(messageFactory);
        }

        String fieldConverterClassName = getSettings().getFieldConverterClassName();

        if(StringUtils.isNotEmpty(fieldConverterClassName)) {
            try {
                Class<?> fieldConverterClass = getClass().getClassLoader().loadClass(fieldConverterClassName);
                fieldConverter = (IFieldConverter)fieldConverterClass.newInstance();
                fieldConverter.init(dictionary, dictionary.getNamespace());
            } catch(Exception e) {
                throw new IllegalStateException("fieldConverterClass: " + e.getMessage(), e);
			}
        }

        if(this.fieldConverter == null) {
            this.fieldConverter = new DefaultFieldConverter();
        }

        String codecClassName = getSettings().getCodecClassName();

        try {
            this.codecClass = getClass().getClassLoader().loadClass(codecClassName).asSubclass(AbstractCodec.class);
        } catch(ClassNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new ServiceException("Could not find codec class: " + codecClassName, e);
        }

        if(getSettings().isUseSSL()) {
            String format = "UseSSL is enabled, but requred parameter %s are missing";

            if(getSettings().getSslProtocol() == null) {
                throw new EPSCommonException(String.format(format, "SslProtocol"));
            }

            if(getSettings().getSslKeyStore() != null || getSettings().getSslKeyStorePassword() != null || getSettings().getKeyStoreType() != null) {
                if(getSettings().getSslKeyStore() == null) {
                    throw new EPSCommonException(String.format(format, "SslKeyStore"));
                } else if(getSettings().getSslKeyStorePassword() == null) {
                    throw new EPSCommonException(String.format(format, "SslKeyStorePassword"));
                } else if(getSettings().getKeyStoreType() == null) {
                    throw new EPSCommonException(String.format(format, "KeyStoreType"));
                }
            }
        }
	}

	@Override
    protected void internalStart() throws Exception {
	    super.internalStart();

        if(getSettings().isAutoConnect()) {
            connect();
		}
	}

    @Override
    protected void initFilterChain(DefaultIoFilterChainBuilder filterChain) throws Exception {
        super.initFilterChain(filterChain);

        if(getSettings().isUseSSL()) {
            filterChain.addFirst("SSLFilter", MINAUtil.createSslFilter(true, getSettings().getSslProtocol(), null, null, null));
        }
    }

	@Override
	public TCPIPSession getSession() {
        return (TCPIPSession)super.getSession();
	}

    @Deprecated
	public boolean sendMessage(IMessage message, long timeOut) throws InterruptedException {
        TCPIPSession session = getSession();

        if(session == null) {
            return false;
        }

	    MsgMetaData metaData = message.getMetaData();
	    byte[] data = metaData.getRawMessage();
		IoBuffer buffer = IoBuffer.wrap(data);

        session.send(buffer);

        metaData.setAdmin(false);
        metaData.setFromService(getName());
        metaData.setToService(getEndpointName());
        metaData.setServiceInfo(serviceInfo);

        storage.storeMessage(message);

        return true;
	}

	@Override
	public void sessionCreated(IoSession session) throws Exception {
        super.sessionCreated(session);
        session.setReceiveLimit(getSettings().getReceiveLimit());
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
        super.sessionClosed(session);
        IMessage message = ServiceUtil.createServiceMessage("Connection closed!", getEndpointName(), getName(), serviceInfo, messageFactory);
        storage.storeMessage(message);
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		if(cause instanceof MessageParseException) {
			MessageParseException e = (MessageParseException)cause;
			byte[] rawMessage = e.getRawMessage().getBytes();
            IMessage msg = messageFactory.createMessage(TCPIPMessageHelper.REJECTED_MESSAGE_NAME_AND_NAMESPACE, TCPIPMessageHelper.REJECTED_MESSAGE_NAME_AND_NAMESPACE);
			MsgMetaData metaData = msg.getMetaData();

            metaData.setToService(getName());
            metaData.setFromService(getEndpointName());
			metaData.setRawMessage(rawMessage);
			metaData.setServiceInfo(serviceInfo);

            storage.storeMessage(msg);
            TCPIPSession tcpipSession = getSession();

            if(tcpipSession != null) {
                getServiceHandler().exceptionCaught(tcpipSession, cause);
            }
        } else {
            super.exceptionCaught(session, cause);
		}
	}

	@Override
    public TCPIPSettings getSettings() {
        return (TCPIPSettings)super.getSettings();
	}

	public IFieldConverter getFieldConverter() {
		return fieldConverter;
	}

	@Override
	public IMessage receive(IActionContext actionContext, IMessage msg) throws InterruptedException {
        return super.receive(actionContext, fieldConverter.convertFields(msg, messageFactory, true));
	}

    @Override
    protected TCPIPSession createSession(IoSession session) {
        return new TCPIPSession(getServiceName(), session, loggingConfigurator);
    }

    @Override
    protected String getHumanReadable(IMessage message) {
        return message.toString();
    }

    @Override
    protected String getHostname() {
        return getSettings().getHost();
    }

    @Override
    protected int getPort() {
        return getSettings().getPort();
    }

    @Override
    protected Class<? extends AbstractCodec> getCodecClass() throws Exception {
        return codecClass;
    }

    @Override
    protected MessageHelper createMessageHelper(final IMessageFactory messageFactory, final IDictionaryStructure dictionary) {
        return new TCPIPMessageHelper(getSettings().isDepersonalizationIncomingMessages()) {{
            init(messageFactory, dictionary);
        }};
    }
}