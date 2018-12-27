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
package com.exactpro.sf.configuration;

import java.io.FileNotFoundException;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;
import com.exactpro.sf.util.AbstractTest;

public class TestDictionaryManager extends AbstractTest {

    @Test
	public void testLoadDictionaries() throws FileNotFoundException, WorkspaceSecurityException {
    	IDictionaryManager manager = SFLocalContext.getDefault().getDictionaryManager();
		IDictionaryStructure dict = manager.getDictionary(SailfishURI.unsafeParse("Example"));

		Assert.assertNotNull(dict);
		Assert.assertEquals("example", dict.getNamespace());
		dict = manager.getDictionary(SailfishURI.unsafeParse("TestAML"));
		Assert.assertNotNull(dict);
		Assert.assertEquals("TestAML", dict.getNamespace());
	}

    @Test
    public void testInvalidateDictionaries() {
        SailfishURI dictionaryURI = SailfishURI.unsafeParse("Example");
        IDictionaryManager manager = SFLocalContext.getDefault().getDictionaryManager();

        IDictionaryStructure dictFirst = manager.getDictionary(dictionaryURI);
        IDictionaryStructure dictSecond = manager.getDictionary(dictionaryURI);

        Assert.assertEquals(dictFirst, dictSecond);

        manager.invalidateDictionaries();
        dictSecond = manager.getDictionary(dictionaryURI);
        Assert.assertThat(dictFirst, CoreMatchers.not(dictSecond));

        manager.invalidateDictionaries(dictionaryURI);
        dictSecond = manager.getDictionary(dictionaryURI);
        Assert.assertThat(dictFirst, CoreMatchers.not(dictSecond));
    }
}
