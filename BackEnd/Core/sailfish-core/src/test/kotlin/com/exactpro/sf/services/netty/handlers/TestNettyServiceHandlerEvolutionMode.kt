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
import com.exactpro.sf.common.messages.IMessage
import com.exactpro.sf.common.util.EvolutionBatch
import com.exactpro.sf.extensions.set
import com.exactpro.sf.services.IServiceHandler
import com.exactpro.sf.services.ISession
import com.exactpro.sf.services.ServiceHandlerRoute
import com.exactpro.sf.services.netty.OutgoingMessageWrapper
import org.junit.Test
import org.mockito.ArgumentMatchers.argThat
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.any
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

class TestNettyServiceHandlerEvolutionMode : TestNettyServiceHandlerBase() {
    override fun createHandler(handler: IServiceHandler, session: ISession): NettyServiceHandler =
        NettyServiceHandler(handler, this.session, DefaultMessageFactory.getFactory(), true)

    @Test
    fun `outgoing message does not go to handler`() {
        val message = DefaultMessageFactory.getFactory().createMessage("Test", "test")
        handler.write(context, message, promise)

        verify(serviceHandler, never()).putMessage(eq(session), any(ServiceHandlerRoute::class.java), any(IMessage::class.java))
        verify(context).write(eq(message), eq(promise))
    }

    @Test
    fun `outgoing batch goes to handler after delimiter`() {
        val message = DefaultMessageFactory.getFactory().createMessage("Test", "test")
        handler.write(context, message, promise)
        handler.write(context, Delimiter, promise)

        verify(serviceHandler).putMessage(eq(session), any(ServiceHandlerRoute::class.java), argThat {
            EvolutionBatch.MESSAGE_NAME == it.name
                && EvolutionBatch(it).size() == 1
        })
        verify(context).write(eq(message), eq(promise))
    }

    @Test
    fun `outgoing wrapper goes to batch but does not go further`() {
        val message = DefaultMessageFactory.getFactory().createMessage("Test", "test").apply { set("A", 42) }
        val wrapper = OutgoingMessageWrapper(message)
        handler.write(context, wrapper, promise)
        handler.write(context, Delimiter, promise)

        verify(serviceHandler).putMessage(eq(session), any(ServiceHandlerRoute::class.java), argThat {
            EvolutionBatch.MESSAGE_NAME == it.name
                && EvolutionBatch(it).size() == 1
        })
        verify(context, never()).write(any(), any())
    }

    @Test
    fun `incoming message does not go to handler`() {
        val message = DefaultMessageFactory.getFactory().createMessage("Test", "test")
        handler.channelRead(context, message)

        verify(serviceHandler, never()).putMessage(eq(session), any(ServiceHandlerRoute::class.java), any(IMessage::class.java))
        verify(context).fireChannelRead(eq(message))
    }

    @Test
    fun `incoming batch goes to handler after delimiter`() {
        val message = DefaultMessageFactory.getFactory().createMessage("Test", "test")
        handler.channelRead(context, message)
        handler.channelRead(context, Delimiter)

        verify(serviceHandler).putMessage(eq(session), any(ServiceHandlerRoute::class.java), argThat {
            EvolutionBatch.MESSAGE_NAME == it.name
                && EvolutionBatch(it).size() == 1
        })
        verify(context).fireChannelRead(eq(message))
    }
}