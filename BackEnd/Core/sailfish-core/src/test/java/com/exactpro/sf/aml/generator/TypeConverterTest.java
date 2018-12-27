/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.aml.generator;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.exactpro.sf.messages.testaml.components.SideITCH;
import com.exactpro.sf.util.AbstractTest;

public class TypeConverterTest extends AbstractTest
{
	@Test
	public void testFindConstByValue() throws Exception
	{
        assertEquals("com.exactpro.sf.messages.testaml.components.SideITCH.BUY", TypeConverter.findConstantByValue(SideITCH.class, "66"));
        assertEquals("com.exactpro.sf.messages.testaml.components.SideITCH.SELL", TypeConverter.findConstantByValue(SideITCH.class, "83"));
	}
}