/*******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.sf.services.netty.handlers

import com.exactpro.sf.services.IServiceHandler
import com.exactpro.sf.services.ISession
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import org.junit.Before
import org.mockito.Mockito.mock

abstract class TestNettyServiceHandlerBase {
    protected lateinit var handler: NettyServiceHandler
    protected lateinit var serviceHandler: IServiceHandler
    protected lateinit var session: ISession
    protected val context: ChannelHandlerContext = mock(ChannelHandlerContext::class.java)
    protected val promise: ChannelPromise = mock(ChannelPromise::class.java)

    @Before
    fun setUp() {
        serviceHandler = mock(IServiceHandler::class.java)
        session = mock(ISession::class.java)
        handler = createHandler(serviceHandler, session)
    }

    protected abstract fun createHandler(handler: IServiceHandler, session: ISession): NettyServiceHandler
}