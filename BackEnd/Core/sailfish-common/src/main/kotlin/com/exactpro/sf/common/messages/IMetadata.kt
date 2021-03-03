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

interface IMetadata : Cloneable {
    val keys: Set<String>
    operator fun get(key: String): Any?
    operator fun set(key: String, value: Any)
    fun contains(key: String): Boolean
    fun remove(key: String)

    companion object {
        @JvmField
        val EMPTY: IMetadata = EmptyMetadata()
    }
}

private class EmptyMetadata : IMetadata {
    override val keys: Set<String> = emptySet()

    override fun get(key: String): Any? = null

    override fun set(key: String, value: Any) = Unit

    override fun contains(key: String): Boolean = false

    override fun remove(key: String) = Unit

    override fun clone(): EmptyMetadata = this
}