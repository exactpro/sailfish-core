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
package com.exactpro.sf.common.messages

import com.exactpro.sf.common.services.ServiceInfo
import com.exactpro.sf.configuration.suri.SailfishURI
import java.util.Date

enum class MetadataProperty(val propertyName: String, val propertyClass: Class<*>) {
    ID("id", Long::class.java),
    SEQUENCE("sequence", Long::class.java),
    TIMESTAMP("msgTimestamp", Date::class.java),
    NAMESPACE("msgNamespace", String::class.java),
    NAME("msgName", String::class.java),
    FROM_SERVICE("fromService", String::class.java),
    TO_SERVICE("toService", String::class.java),
    IS_ADMIN("isAdmin", Boolean::class.java),
    IS_REJECTED("isRejected", Boolean::class.java),
    IS_DIRTY("isDirty", Boolean::class.java),
    REJECT_REASON("rejectReason", String::class.java),
    RAW_MESSAGE("rawMessage", ByteArray::class.java),
    SERVICE_INFO("serviceInfo", ServiceInfo::class.java),
    DICTIONARY_URI("dictionaryURI", SailfishURI::class.java),
    PROTOCOL("protocol", String::class.java),
    SUBSEQUENCE("subsequence", Integer::class.java),
    BATCH_SEQUENCE("batchsequence", Long::class.java),
    IS_LAST_IN_BATCH("isLastInBatch", Boolean::class.java);

    operator fun component1(): String = propertyName
    operator fun component2(): Class<*> = propertyClass

    companion object {
        fun fromString(propertyName: String): MetadataProperty? = values().find { it.propertyName == propertyName }
    }
}