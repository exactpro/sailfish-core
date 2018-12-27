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

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.SendMessageFailedException;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.ITaskExecutor;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.itch.ITCHMessageHelper;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.DefaultWriteFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.AbstractProtocolEncoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by alexey.zarovny on 11/18/14.
 */
public class ITCHMulticastUDPSession implements ISession {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName() + "@" + Integer.toHexString(hashCode()));

    private final String name;
    private final SailfishURI dictionaryURI;
    private final ITCHMulticastServer service;
    private final int heartbeatInterval;
    private AbstractCodec codec;
    private DatagramSocket udpPrimarySocket;
    private DatagramSocket udpSecondarySocket;
    private InetAddress primaryAddress = null;
    private InetAddress secondaryAddress = null;
    private int primaryPort = 0;
    private int secondaryPort = 0;
    private final IMessageFactory msgFactory;
    private AtomicInteger sequenceNumber = new AtomicInteger(1);
    private byte marketDataGroup;
    private ITCHMulticastCache cache;
    private final IoSession dummySession;
    private Future<?> heartbeatFuture;
    private String remoteName;
    private MessageHelper itchHandler;

    private final IServiceContext serviceContext;

    public ITCHMulticastUDPSession(IServiceContext serviceContext, String name, SailfishURI dictionaryURI, byte marketDataGroup, ITCHMulticastServer service, MessageHelper itchHandler, IMessageFactory msgFactory) {
        this.name = name;
        this.serviceContext = serviceContext;
        this.dictionaryURI = dictionaryURI;
        this.msgFactory = msgFactory;
        this.itchHandler = itchHandler;

        this.codec = itchHandler.getCodec(serviceContext);
        this.marketDataGroup = marketDataGroup;
        this.service = service;
        this.heartbeatInterval = ((ITCHMulticastSettings) service.getSettings()).getHeartbeatInterval();
        this.remoteName = "To Dublin";
        this.dummySession = new DummySession();
    }

    private void runHeartBeatTimer() {
        ITaskExecutor taskExecutor = service.getTaskExecutor();
        heartbeatFuture = taskExecutor.addRepeatedTask(new HeartBeatTimerTask(this.serviceContext, this), 0L, heartbeatInterval, TimeUnit.SECONDS);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public IMessage send(Object message) throws InterruptedException {
        if (!(message instanceof IMessage)) {
            throw new EPSCommonException("Illegal type of IMessage");
        }

        ProtocolEncoder pdOut = new ProtocolEncoder(dummySession);
        IMessage iMsg = (IMessage) message;
        byte mdGroup = 0;
        if (iMsg.getField(ITCHMessageHelper.FAKE_FIELD_UH_MARKET_DATA_GROUP) != null)
            mdGroup = Byte.valueOf(Byte.valueOf(iMsg.getField(ITCHMessageHelper.FAKE_FIELD_UH_MARKET_DATA_GROUP).toString()));
        mdGroup = mdGroup != 0 ? mdGroup : marketDataGroup;
        final byte marketDataGroup = mdGroup;
        @SuppressWarnings("serial")
        Map<String, String> params = new HashMap<String, String>(){{
            put(ITCHMessageHelper.FIELD_MARKET_DATA_GROUP_NAME, String.valueOf(marketDataGroup));
            put(ITCHMessageHelper.FIELD_SEQUENCE_NUMBER_NAME, String.valueOf(sequenceNumber.getAndIncrement()));
        }};
        iMsg = itchHandler.prepareMessageToEncode(iMsg, params);

        try {
            codec.encode(dummySession, iMsg, pdOut);
            IoBuffer buff = (IoBuffer) pdOut.getMessageQueue().element();
            byte[] rawData = Arrays.copyOf(buff.array(), buff.limit());
            if (primaryAddress != null && primaryPort != 0) {
                udpPrimarySocket.send(new DatagramPacket(rawData, rawData.length, primaryAddress, primaryPort));
            }
            if (secondaryAddress != null && secondaryPort != 0) {
                udpSecondarySocket.send(new DatagramPacket(rawData, rawData.length, secondaryAddress, secondaryPort));
            }
        } catch (IOException e) {
            logger.error("{}", e);
            throw new SendMessageFailedException("Send message " + iMsg.getName() + " failed", e);
        } catch (Exception e) {
            if(e instanceof InterruptedException){
                throw (InterruptedException)e;
            }
            logger.error("{}", e);
            throw new SendMessageFailedException("Send message " + iMsg.getName() + " failed", e);
        }
        cache.add(sequenceNumber.get() - 1, iMsg, mdGroup);
        service.handleMessage(false, false, iMsg, this, remoteName);
        return iMsg;
    }

    @Override
    public IMessage sendDirty(Object message) {
        //stub
        return null;
    }

    public void open(int primaryPort, String primaryAddress, int secondaryPort, String secondaryAddress, ITCHMulticastCache cache) throws IOException {
        udpPrimarySocket = new MulticastSocket(primaryPort);
        this.primaryAddress = InetAddress.getByName(primaryAddress);
        this.primaryPort = primaryPort;

        udpSecondarySocket = new MulticastSocket(secondaryPort);
        this.secondaryAddress = InetAddress.getByName(secondaryAddress);
        this.secondaryPort = secondaryPort;

        this.cache = cache;
        runHeartBeatTimer();
    }

    @Override
    public void close() {
        if(heartbeatFuture != null)
            heartbeatFuture.cancel(true);

        if (udpPrimarySocket != null) {
            udpPrimarySocket.close();
        }
        if (udpSecondarySocket != null) {
            udpSecondarySocket.close();
        }
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean isLoggedOn() {
        return false;
    }

    private class ProtocolEncoder extends AbstractProtocolEncoderOutput {
        private final IoSession session;

        public ProtocolEncoder(IoSession session) {
            this.session = session;
        }

        @Override
        public WriteFuture flush() {
            return new DefaultWriteFuture(session);
        }
    }

    private class HeartBeatTimerTask implements Runnable {

        private final IServiceContext serviceContext;
        private final ISession iSession;

        public HeartBeatTimerTask(IServiceContext serviceContext, ISession iSession){
            this.serviceContext = serviceContext;
            this.iSession = iSession;
        }

        @Override
        public void run() {
            IDictionaryStructure dict = serviceContext.getDictionaryManager().getDictionary(dictionaryURI);
            IMessage iMsg = msgFactory.createMessage("UnitHeader", dict.getNamespace());
            iMsg.addField("MessageCount", (short) 0);
            iMsg.addField("MarketDataGroup", (short)marketDataGroup);
            iMsg.addField("Length", 8);
            iMsg.addField("SequenceNumber", (long)sequenceNumber.get());

            ProtocolEncoder pdOut = new ProtocolEncoder(dummySession);
            try {
                codec.encode(dummySession, iMsg, pdOut);
                IoBuffer buff = (IoBuffer) pdOut.getMessageQueue().element();
                byte[] rawData = Arrays.copyOf(buff.array(), buff.limit());
                if (primaryAddress != null && primaryPort != 0) {
                    udpPrimarySocket.send(new DatagramPacket(rawData, rawData.length, primaryAddress, primaryPort));
                }
                if (secondaryAddress != null && secondaryPort != 0) {
                    udpSecondarySocket.send(new DatagramPacket(rawData, rawData.length, secondaryAddress, secondaryPort));
                }
                if (((ITCHMulticastSettings) service.getSettings()).isStoreHeartbeat()) {
                    service.handleMessage(false, true, iMsg, iSession, remoteName);
                }
            } catch (IOException e) {
                logger.error("{}", e);
            } catch (Exception e) {
                logger.error("{}", e);
            }
        }

        @Override
        public String toString() {
            return HeartBeatTimerTask.class.getSimpleName();
        }
    }
}
