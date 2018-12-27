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
package com.exactpro.sf.services.itch.multicast;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.WrapperNioSocketAcceptor;
import org.apache.mina.filter.codec.ProtocolCodecFilter;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Created by alexey.zarovny on 11/19/14.
 */
public class ITCHMulticastTCPSession implements ISession {

    private final SailfishURI dictionaryURI;
    private final IServiceContext serviceContext;
    private final String name;
    private final ITCHMulticastServer service;
    private final IMessageFactory factory;
    private int port;
    private WrapperNioSocketAcceptor acceptor;
    private boolean isClosed;
    private MessageHelper itchHandler;

    public ITCHMulticastTCPSession(IServiceContext serviceContext, String serviceName, SailfishURI dictionaryURI, ITCHMulticastServer service, MessageHelper itchHandler, IMessageFactory factory) {
        this.name = serviceName + "TCPSession";
        this.dictionaryURI = dictionaryURI;
        this.serviceContext = serviceContext;
        this.service = service;
        this.itchHandler = itchHandler;
        this.factory = factory;
        isClosed = true;
    }

    public void open(int port, ITCHMulticastCache cache) throws IOException {
        this.port = port;

        acceptor = new WrapperNioSocketAcceptor(this.serviceContext.getTaskExecutor());
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ITCHCodecFactory(this.serviceContext, itchHandler)));
        acceptor.setHandler(new ITCHMulticastTCPHandlerAdapter(cache, dictionaryURI, this.serviceContext.getTaskExecutor(), service, this, ((ITCHMulticastSettings) service.getSettings()).getMarketDataGroup(), itchHandler, factory));
        acceptor.setReuseAddress(true);
        acceptor.bind(new InetSocketAddress(this.port));
        isClosed = false;
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public IMessage send(Object message) {
        //especially left blank
        return null;
    }

    @Override
    public IMessage sendDirty(Object message) {
        return null;
    }

    @Override
    public void close() {
        if (acceptor != null) {
            acceptor.unbind(new InetSocketAddress(this.port));
            acceptor.dispose();
        }
        isClosed = true;
    }

    @Override
    public boolean isClosed() {
        return isClosed;
    }

    @Override
    public boolean isLoggedOn() {
        return false;
    }
}
