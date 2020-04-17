/******************************************************************************
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
package com.exactpro.sf.scriptrunner

import com.exactpro.sf.aml.script.MetaContainer
import com.exactpro.sf.scriptrunner.SailFishAction.createMetaContainerByPath
import org.junit.Assert
import org.junit.Test

class TestSailFishAction {
    @Test
    fun testCreateMetaContainerByPath() {
        val expected = MetaContainer().apply {
            failUnexpected = "A"

            add("FieldA", MetaContainer().apply {
                add("FieldD", MetaContainer())
                add("FieldD", MetaContainer().setKeyFields(setOf("FieldG")))
            })

            add("FieldA", MetaContainer().apply {
                add("FieldB", MetaContainer().apply {
                    add("FieldC", MetaContainer())
                    add("FieldC", MetaContainer())
                    add("FieldC", MetaContainer().apply {
                        failUnexpected = "Y"
                        setKeyFields(setOf("FieldE", "FieldF"))
                    })
                })
            })
        }

        val actual = MetaContainer().apply {
            failUnexpected = "A"
            createMetaContainerByPath(this, "Y", "FieldA", "1", "FieldB", "0", "FieldC", "2").setKeyFields(setOf("FieldE", "FieldF"))
            createMetaContainerByPath(this, null, "FieldA", "0", "FieldD", "1").setKeyFields(setOf("FieldG"))
        }

        Assert.assertEquals(expected, actual)
    }
}