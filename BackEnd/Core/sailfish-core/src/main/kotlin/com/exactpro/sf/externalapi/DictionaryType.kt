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
package com.exactpro.sf.externalapi

import com.exactpro.sf.configuration.suri.SailfishURI

enum class DictionaryType {
    /**
     * Main dictionary of a codec/service (mandatory)
     */
    MAIN,

    /**
     * Dictionary for the first level in a multi-level protocol
     */
    LEVEL1,

    /**
     * Dictionary for the second level in a multi-level protocol
     */
    LEVEL2,

    /**
     * Dictionary for incoming messages
     */
    INCOMING,

    /**
     * Dictionary for outgoing messages
     */
    OUTGOING;

    fun toUri(): SailfishURI = SailfishURI(null, this::class.java.simpleName, toString())
}