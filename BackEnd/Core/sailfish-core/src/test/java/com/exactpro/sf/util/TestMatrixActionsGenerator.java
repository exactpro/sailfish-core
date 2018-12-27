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
package com.exactpro.sf.util;

import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import junit.framework.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class TestMatrixActionsGenerator extends AbstractTest
{
	@Test
	public void testMatrixActionsGenerator() throws Exception
	{
		IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();

		IDictionaryStructure dictionary;
		
		try (InputStream in = new FileInputStream("src/main/workspace/cfg/dictionaries/example.xml")) {
			dictionary = loader.load(in);
    	}

		String path = "build/test-results/generated";

		MatrixActionsGenerator.generate(
				path,
                "com.exactpro.sf.actions",
                "com.exactpro.sf.messages.impl",
				dictionary, "DefaultMessageFactory.getFactory().getUncheckedFields()"
		);

        File actionFile = new File(path + "/com/exactpro/sf/actions/EXAMPLE_SndRcvMatrixActions.java");
        if (!actionFile.exists())
            Assert.fail();
    }
}
