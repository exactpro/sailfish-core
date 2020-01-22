/*
 *  Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.exactpro.sf.externalapi.impl

import com.exactpro.sf.common.impl.messages.AbstractMessageFactory
import com.exactpro.sf.common.impl.messages.StrictMessageWrapper
import com.exactpro.sf.common.messages.IMessage
import com.exactpro.sf.common.messages.IMessageFactory
import com.exactpro.sf.common.messages.TestMessageFactory.FILE_NAME_DICTIONARY
import com.exactpro.sf.common.messages.structures.IDictionaryStructure
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader
import com.exactpro.sf.configuration.suri.SailfishURI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.lang.Exception

class StrictMessageFactoryWrapperTest {
    private lateinit var dictionary: IDictionaryStructure
    private lateinit var messageFactory: IMessageFactory
    @Before
    @Throws(Exception::class)
    fun setUp() {
        this::class.java.classLoader.getResourceAsStream(FILE_NAME_DICTIONARY).use { inputStream ->
            assertNotNull("Resource '$FILE_NAME_DICTIONARY' not found", inputStream)
            dictionary = XmlDictionaryStructureLoader().load(inputStream)
        }
        messageFactory = StrictMessageFactoryWrapper(object : AbstractMessageFactory() {
            override fun getProtocol(): String {
                return "test"
            }
        }, dictionary).apply {
            init(SailfishURI.unsafeParse("test"), dictionary)
        }
    }

    @Test
    fun testMessageWithIsEncodeStructures() {
        val root : IMessage = messageFactory.createMessage("Root")
        with(root.getField<IMessage>("SubMessage")) {
            assertNotNull(this)
            assertEquals("$name field", StrictMessageWrapper::class.java, this::class.java)
            with(getField<IMessage>("SubSubMessage")) {
                assertNotNull(this)
                assertEquals("$name field", StrictMessageWrapper::class.java, this::class.java)
                with(getField<IMessage>("SubSubSubMessage")) {
                    assertNotNull(this)
                    assertEquals("$name field", StrictMessageWrapper::class.java, this::class.java)
                }
            }
        }
    }
}