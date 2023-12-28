/******************************************************************************
 * Copyright 2009-2024 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.fix

import com.exactpro.sf.common.messages.IMessage
import com.exactpro.sf.common.messages.MsgMetaData
import com.exactpro.sf.common.services.ServiceInfo
import com.exactpro.sf.common.services.ServiceName
import com.exactpro.sf.services.IServiceContext
import com.exactpro.sf.services.MessageHelper
import com.exactpro.sf.services.ServiceHandlerRoute
import com.exactpro.sf.services.fix.converter.MessageConvertException
import com.exactpro.sf.services.fix.converter.dirty.DirtyQFJIMessageConverter
import com.exactpro.sf.storage.IMessageStorage
import org.quickfixj.CharsetSupport
import quickfix.FieldException
import quickfix.FieldNotFound
import quickfix.Message
import quickfix.SessionID
import quickfix.field.PossDupFlag
import java.util.Objects

abstract class AbstractApplication {

    lateinit var serviceInfo: ServiceInfo
    protected lateinit var applicationContext: ApplicationContext
    protected lateinit var converter: DirtyQFJIMessageConverter
    private var evolutionOptimize: Boolean = false

    open fun init(serviceContext: IServiceContext, applicationContext: ApplicationContext, serviceName: ServiceName) {
        this.applicationContext = applicationContext
        serviceInfo = Objects.requireNonNull(serviceContext.lookupService(serviceName), "serviceInfo cannot be null");
        converter = applicationContext.converter

        applicationContext.serviceSettings.let { settings ->
            if (settings is FIXCommonSettings) {
                this.evolutionOptimize = settings.isEvolutionOptimize
            }
        }
    }

    @JvmOverloads
    @Throws(MessageConvertException::class)
    protected fun convert(
            message: Message,
            from: String, to: String,
            isAdmin: Boolean,
            isOutComing: Boolean,
            verifyTags: Boolean? = null,
            isRejected: Boolean = false,
    ): IMessage {
        val rawMessage: ByteArray = extractRawData(message, isOutComing)
        val msg: IMessage = checkNotNull(converter.run {
            when {
                isRejected -> convertDirty(message, verifyTags, false, false, true)
                evolutionOptimize -> convertEvolution(message)
                else -> convert(message, verifyTags, null)
            }
        }) { "Converted message can't be null, origin message: $message" }
        val meta: MsgMetaData = msg.metaData

        meta.fromService = from
        meta.toService = to
        meta.isAdmin = isAdmin
        meta.rawMessage = rawMessage
        meta.serviceInfo = serviceInfo

        return msg
    }

    protected fun extractRawData(message: Message, isOutComingRoute: Boolean): ByteArray {
        val messageData = message.messageData
        return if (messageData == null || (isOutComingRoute && getBooleanFlag(message, PossDupFlag.FIELD))) {
            message.toString().toByteArray(CharsetSupport.getCharsetInstance())
        } else {
            messageData.toByteArray(CharsetSupport.getCharsetInstance())
        }
    }

    protected fun isOutComingRoute(route: ServiceHandlerRoute?): Boolean {
        return route != null && !route.isFrom
    }

    protected fun createFIXSession(sessionName: String, sessionId: SessionID, storage: IMessageStorage, converter: DirtyQFJIMessageConverter, messageHelper: MessageHelper): FIXSession? {
        return FIXSession(sessionName, sessionId, storage, converter, messageHelper, evolutionOptimize)
    }

    private fun getBooleanFlag(message: Message, flag: Int): Boolean {
        return try {
            message.isSetField(flag) && message.getBoolean(flag)
        } catch (e: FieldNotFound) {
            false
        } catch (e: FieldException) {
            false
        }
    }
}