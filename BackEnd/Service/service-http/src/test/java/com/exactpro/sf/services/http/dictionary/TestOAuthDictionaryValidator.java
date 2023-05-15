/******************************************************************************
 * Copyright 2009-2023 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.http.dictionary;

import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationError;
import com.exactpro.sf.configuration.dictionary.impl.PrototypeDictionaryValidator;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestOAuthDictionaryValidator {

    private static final IDictionaryStructureLoader DICTIONARY_LOADER = new XmlDictionaryStructureLoader();
    private static final Set<String> MESSAGE_NAMES = new HashSet<String>() {{
        add("HttpHeaderMessage");
        add("HttpRequestMessage");
        add("HttpResponseMessage");
    }};

    private IDictionaryStructure loadDictionary(String name) throws IOException {
        try(InputStream inputStream = getClass().getResourceAsStream(name)) {
            return DICTIONARY_LOADER.load(inputStream);
        }
    }

    @Test
    public void testFactory() {
        final IDictionaryValidator validator = new OAuthDictionaryValidatorFactory()
                .createDictionaryValidator();

        Assert.assertTrue(validator instanceof PrototypeDictionaryValidator);
        Assert.assertSame(validator, OAuthDictionaryValidatorFactory.INSTANCE);
    }

    @Test
    public void testDictionaries() throws Exception {
        final IDictionaryValidator validator = new OAuthDictionaryValidatorFactory()
                .createDictionaryValidator();
        final IDictionaryStructure dictionary = loadDictionary("/dictionaries/negative.xml");
        final List<DictionaryValidationError> errors = validator.validate(dictionary, true, null);

        Assert.assertEquals(MESSAGE_NAMES.size(), errors.size());
        for(DictionaryValidationError error: errors) {
            Assert.assertTrue(MESSAGE_NAMES.contains(error.getMessage()));
        }
    }

}
