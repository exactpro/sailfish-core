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
package com.exactpro.sf.services.netty.handlers;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.ServiceHandlerRoute;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class NettyServiceHandler extends ChannelDuplexHandler {

	// FIXME: IServiceHandler throws exceptions!

	private IServiceHandler serviceHandler;

	private ISession session;

	public NettyServiceHandler(IServiceHandler serviceHandler, ISession session) {
		super();
		this.serviceHandler = serviceHandler;
		this.session = session;
	}


	@Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    	if (msg instanceof IMessage) {
    		IMessage imsg = (IMessage) msg;
    		if (imsg.getMetaData().isAdmin()) {
    			serviceHandler.putMessage(session, ServiceHandlerRoute.TO_ADMIN, imsg);
    		}
    		else {
    			serviceHandler.putMessage(session, ServiceHandlerRoute.TO_APP, imsg);
    		}
    	}
    	ctx.write(msg, promise);
    }

	@Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
    	serviceHandler.sessionClosed(session);
    	ctx.close(promise);
    }

    /**
     * The {@link Channel} of the {@link ChannelHandlerContext} was registered is now inactive and reached its
     * end of lifetime.
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    	if (msg instanceof IMessage) {
    		IMessage imsg = (IMessage) msg;
    		if (imsg.getMetaData().isAdmin()) {
    			serviceHandler.putMessage(session, ServiceHandlerRoute.FROM_ADMIN, imsg);
    		}
    		else {
    			serviceHandler.putMessage(session, ServiceHandlerRoute.FROM_APP, imsg);
    		}
    	}
        ctx.fireChannelRead(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    	serviceHandler.exceptionCaught(session, cause);
        ctx.fireExceptionCaught(cause);
    }



}
