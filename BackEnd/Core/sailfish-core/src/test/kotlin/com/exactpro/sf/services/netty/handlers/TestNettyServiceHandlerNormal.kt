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

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory
import com.exactpro.sf.extensions.set
import com.exactpro.sf.services.IServiceHandler
import com.exactpro.sf.services.ISession
import com.exactpro.sf.services.ServiceHandlerRoute
import com.exactpro.sf.services.netty.OutgoingMessageWrapper
import org.junit.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.verify

class TestNettyServiceHandlerNormal : TestNettyServiceHandlerBase() {
    override fun createHandler(handler: IServiceHandler, session: ISession): NettyServiceHandler {
        return NettyServiceHandler(handler, session)
    }

    @Test
    fun `outgoing message goes to handler`() {
        val message = DefaultMessageFactory.getFactory().createMessage("Test", "test")
        handler.write(context, message, promise)

        verify(serviceHandler).putMessage(eq(session), eq(ServiceHandlerRoute.TO_APP), eq(message))
        verify(context).write(eq(message), eq(promise))
    }

    @Test
    fun `outgoing delimiter is ignored`() {
        val message = DefaultMessageFactory.getFactory().createMessage("Test", "test")
        handler.write(context, message, promise)
        handler.write(context, Delimiter, promise)

        verify(serviceHandler).putMessage(eq(session), eq(ServiceHandlerRoute.TO_APP), eq(message))
        verify(context).write(eq(message), eq(promise))
    }

    @Test
    fun `outgoing wrapper goes to handler but does not go further`() {
        val message = DefaultMessageFactory.getFactory().createMessage("Test", "test").apply { set("A", 42) }
        val wrapper = OutgoingMessageWrapper(message)
        handler.write(context, wrapper, promise)

        verify(serviceHandler).putMessage(eq(session), eq(ServiceHandlerRoute.TO_APP), eq(message))
        verify(context, Mockito.never()).write(any(), any())
    }

    @Test
    fun `incoming message goes to handler`() {
        val message = DefaultMessageFactory.getFactory().createMessage("Test", "test")
        handler.channelRead(context, message)

        verify(serviceHandler).putMessage(eq(session), eq(ServiceHandlerRoute.FROM_APP), eq(message))
        verify(context).fireChannelRead(eq(message))
    }

    @Test
    fun `incoming delimiter is ignored`() {
        val message = DefaultMessageFactory.getFactory().createMessage("Test", "test")
        handler.channelRead(context, message)
        handler.channelRead(context, Delimiter)

        verify(serviceHandler).putMessage(eq(session), eq(ServiceHandlerRoute.FROM_APP), eq(message))
        verify(context).fireChannelRead(eq(message))
    }
}