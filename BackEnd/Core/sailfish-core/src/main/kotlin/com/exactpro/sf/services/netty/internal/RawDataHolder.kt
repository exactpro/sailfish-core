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

package com.exactpro.sf.services.netty.internal

import com.exactpro.sf.common.messages.IMetadata

/**
 * This class suppose to be use only internally.
 *
 * Holds the information for decoding raw data as IMessage to send it via the service
 */
class RawDataHolder(
    val bytes: ByteArray,
    val metadata: IMetadata
)