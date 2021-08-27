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

package com.exactpro.sf.externalapi.codec.impl

import com.exactpro.sf.common.util.ICommonSettings
import org.apache.commons.configuration.HierarchicalConfiguration
import org.junit.Assert.*
import org.junit.Test

class TestExternalCodecSettings {
    class GeneralSetters : ICommonSettings {
        var fieldA: Int = 0
        private var fieldB: Boolean = false
        fun isFieldB(): Boolean = fieldB
        fun setFieldB(value: Boolean) { fieldB = value }
        override fun load(config: HierarchicalConfiguration?) {
        }
    }

    class BuilderSetters : ICommonSettings {
        var fieldA: Int = 0
            private set
        private var fieldB: Boolean = false
        fun setFieldA(value: Int): BuilderSetters = apply {
            fieldA = value
        }

        fun isFieldB(): Boolean = fieldB
        fun setFieldB(value: Boolean): BuilderSetters = apply {
            fieldB = value
        }
        override fun load(config: HierarchicalConfiguration?) {
        }
    }

    @Test
    fun testFindsAllPropertiesWithGeneralSetters() {
        ExternalCodecSettings(GeneralSetters()).apply {
            assertEquals(mapOf(
                "fieldA" to Int::class.java,
                "fieldB" to Boolean::class.java
            ), propertyTypes)

            set("fieldA", 42)
            assertEquals(42, getSettings<GeneralSetters>().fieldA)

            set("fieldB", false)
            assertFalse(getSettings<GeneralSetters>().isFieldB())
        }
    }

    @Test
    fun testFindsAllPropertiesWithBuilderSetters() {
        ExternalCodecSettings(BuilderSetters()).apply {
            assertEquals(mapOf(
                "fieldA" to Int::class.java,
                "fieldB" to Boolean::class.java
            ), propertyTypes)

            set("fieldA", 42)
            assertEquals(42, getSettings<BuilderSetters>().fieldA)

            set("fieldB", false)
            assertFalse(getSettings<BuilderSetters>().isFieldB())
        }
    }
}