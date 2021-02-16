/*******************************************************************************
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.sf.services.mina

import com.exactpro.sf.common.messages.IMessage
import mu.KotlinLogging
import org.apache.mina.core.filterchain.IoFilter
import org.apache.mina.core.session.IdleStatus
import org.apache.mina.core.session.IoSession
import org.apache.mina.core.write.WriteRequest

class MessageNextFilter : IoFilter.NextFilter {

    private val _exceptions = arrayListOf<Throwable>()
    val exceptions: Collection<Throwable>
        get() = _exceptions
    private val _results = arrayListOf<IMessage>()
    val results: Collection<IMessage>
        get() = _results

    override fun messageReceived(session: IoSession, message: Any) {
        if (message !is IMessage) {
            LOGGER.error { "Decoded result ${message::class.java} is not ${IMessage::class.java}" }
            return
        }
        _results += message
    }

    override fun exceptionCaught(session: IoSession, cause: Throwable) {
        _exceptions += cause
    }

    override fun sessionCreated(session: IoSession): Unit = throw UnsupportedOperationException()

    override fun sessionOpened(session: IoSession): Unit = throw UnsupportedOperationException()

    override fun sessionClosed(session: IoSession): Unit = throw UnsupportedOperationException()

    override fun sessionIdle(session: IoSession, status: IdleStatus?): Unit = throw UnsupportedOperationException()

    override fun inputClosed(session: IoSession): Unit = throw UnsupportedOperationException()

    override fun messageSent(session: IoSession, writeRequest: WriteRequest?): Unit = throw UnsupportedOperationException()

    override fun filterWrite(session: IoSession, writeRequest: WriteRequest?): Unit = throw UnsupportedOperationException()

    override fun filterClose(session: IoSession): Unit = throw UnsupportedOperationException()

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }
}