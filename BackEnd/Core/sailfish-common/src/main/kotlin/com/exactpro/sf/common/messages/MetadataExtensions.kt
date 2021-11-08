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
@file:JvmName("MetadataExtensions")

package com.exactpro.sf.common.messages

import com.exactpro.sf.common.messages.MetadataProperty.*
import com.exactpro.sf.common.services.ServiceInfo
import com.exactpro.sf.configuration.suri.SailfishURI
import java.util.Date

private const val MESSAGE_PROPERTIES_KEY = "messageProperties"

@Suppress("UNCHECKED_CAST")
private fun <T> IMetadata.getAs(propertyName: String): T? = get(propertyName) as T?

private fun <T> IMetadata.getAs(property: MetadataProperty): T? = getAs(property.propertyName)

private fun <T : Any> IMetadata.getRequired(property: MetadataProperty): T = checkNotNull(getAs(property)) { "${property.propertyName} is not set" }

private fun IMetadata.setOrRemove(propertyName: String, value: Any?) = value?.run { set(propertyName, value) } ?: remove(propertyName)

private fun IMetadata.setOrRemove(property: MetadataProperty, value: Any?) = setOrRemove(property.propertyName, value)

private fun IMetadata.setOnce(property: MetadataProperty, value: Any) {
    val propertyName = property.propertyName
    check(!contains(propertyName)) { "$propertyName is already set" }
    set(propertyName, value)
}

/**
 * Adds all data from the [other] metadata to the current [IMetadata].
 *
 * @return the same [IMetadata] the method was invoked for. All key from [other] will be added to the original metadata.
 */
fun IMetadata.merge(other: IMetadata): IMetadata = apply {
    for (key in other.keys) {
        set(key, checkNotNull(other[key]) { "Metadata contains key '$key' but the value is 'null'" })
    }
}

fun IMetadata.contains(property: MetadataProperty): Boolean = contains(property.propertyName)

var IMetadata.id: Long
    get() = getRequired(ID)
    set(value) = setOnce(ID, value)

var IMetadata.sequence: Long?
    get() = getAs(SEQUENCE)
    set(value) = setOrRemove(SEQUENCE, value)

var IMetadata.timestamp: Date
    get() = getRequired(TIMESTAMP)
    set(value) = setOnce(TIMESTAMP, value)

var IMetadata.namespace: String
    get() = getRequired(NAMESPACE)
    set(value) = setOnce(NAMESPACE, value)

var IMetadata.name: String
    get() = getRequired(NAME)
    set(value) = setOnce(NAME, value)

var IMetadata.fromService: String?
    get() = getAs(FROM_SERVICE)
    set(value) = setOrRemove(FROM_SERVICE, value)

var IMetadata.toService: String?
    get() = getAs(TO_SERVICE)
    set(value) = setOrRemove(TO_SERVICE, value)

var IMetadata.isAdmin: Boolean
    get() = getAs(IS_ADMIN) ?: false
    set(value) = set(IS_ADMIN.propertyName, value)

var IMetadata.isRejected: Boolean
    get() = getAs(IS_REJECTED) ?: contains(REJECT_REASON.propertyName)
    @Deprecated("use rejectReason instead") set(value) = set(IS_REJECTED.propertyName, value)

var IMetadata.isDirty: Boolean
    get() = getAs(IS_DIRTY) ?: false
    set(value) = set(IS_DIRTY.propertyName, value)

var IMetadata.rejectReason: String?
    get() = getAs(REJECT_REASON)
    set(value) {
        setOrRemove(REJECT_REASON, value)
        isRejected = isRejected || value != null
    }

var IMetadata.rawMessage: ByteArray?
    get() = getAs(RAW_MESSAGE)
    set(value) = setOrRemove(RAW_MESSAGE, value)

var IMetadata.serviceInfo: ServiceInfo?
    get() = getAs(SERVICE_INFO)
    set(value) = setOrRemove(SERVICE_INFO, value)

var IMetadata.dictionaryUri: SailfishURI?
    get() = getAs(DICTIONARY_URI)
    set(value) = setOrRemove(DICTIONARY_URI, value)

var IMetadata.protocol: String?
    get() = getAs(PROTOCOL)
    set(value) = setOrRemove(PROTOCOL, value)

var IMetadata.messageProperties: Map<String, String>?
    get() = getAs(MESSAGE_PROPERTIES_KEY)
    set(value) = setOrRemove(MESSAGE_PROPERTIES_KEY, value)

var IMetadata.subsequence: Int?
    get() = getAs(SUBSEQUENCE)
    set(value) = setOrRemove(SUBSEQUENCE, value)

var IMetadata.batchSequence: Long?
    get() = getAs(BATCH_SEQUENCE)
    set(value) = setOrRemove(BATCH_SEQUENCE, value)

var IMetadata.isLastInBatch: Boolean
    get() = getAs(IS_LAST_IN_BATCH) ?: false
    set(value) = set(IS_LAST_IN_BATCH.propertyName, value)
