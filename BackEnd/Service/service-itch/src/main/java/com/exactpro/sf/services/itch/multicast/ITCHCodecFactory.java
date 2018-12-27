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


import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.MessageHelper;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

/**
 * Created by alexey.zarovny on 11/29/14.
 */
public class ITCHCodecFactory implements ProtocolCodecFactory {

    private final IServiceContext serviceContext;
    private final MessageHelper itchHandler;

    public ITCHCodecFactory(IServiceContext serviceContext, MessageHelper itchHandler) {
        this.serviceContext = serviceContext;
        this.itchHandler = itchHandler;
    }

    @Override
    public ProtocolEncoder getEncoder(IoSession ioSession) throws Exception {
        return itchHandler.getCodec(this.serviceContext);
    }

    @Override
    public ProtocolDecoder getDecoder(IoSession ioSession) throws Exception {
        return itchHandler.getCodec(this.serviceContext);
    }
}
