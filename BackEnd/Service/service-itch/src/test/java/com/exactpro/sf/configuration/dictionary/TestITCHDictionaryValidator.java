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
package com.exactpro.sf.configuration.dictionary;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator;
import com.exactpro.sf.util.AbstractTest;

import junit.framework.Assert;

public class TestITCHDictionaryValidator extends AbstractTest {

	protected static final String SAILFISH_DICTIONARY_PATH = "cfg" + File.separator + "dictionaries" + File.separator;

    @Test
    public void testITCHDictionaryValidator() throws IOException
    {
        IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();

        String sep = File.separator;
        IDictionaryStructure dictionary;

        try (InputStream in = new FileInputStream("src" + sep + "test" + sep + "plugin" +
                sep + "cfg" + sep + "dictionaries" + sep + "itch.xml")) {

            dictionary = loader.load(in);
        }

        IDictionaryValidator dictionaryValidator = new ITCHDictionaryValidatorFactory().createDictionaryValidator();

        List<DictionaryValidationError> errors = dictionaryValidator.validate(dictionary, true, null);

        for(DictionaryValidationError error : errors) {
            System.err.println(error.getError());
        }

        Assert.assertEquals(0, errors.size());
    }
}

