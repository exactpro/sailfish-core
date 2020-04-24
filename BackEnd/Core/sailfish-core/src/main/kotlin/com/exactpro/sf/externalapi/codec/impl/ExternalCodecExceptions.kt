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
package com.exactpro.sf.externalapi.codec.impl

open class ExternalCodecException(message: String, cause: Throwable?) : Exception(message, cause)
class DecodeException(message: String, cause: Throwable? = null) : ExternalCodecException(message, cause)
class EncodeException(message: String, cause: Throwable? = null) : ExternalCodecException(message, cause)

open class ExternalCodecSettingsException(message: String, cause: Throwable? = null) : Exception(message, cause)
class PropertyReadException(message: String, cause: Throwable? = null) : ExternalCodecSettingsException(message, cause)
class PropertyWriteException(message: String, cause: Throwable? = null) : ExternalCodecSettingsException(message, cause)