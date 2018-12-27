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

import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator;
import com.exactpro.sf.services.ntg.TestNTGHelper;
import com.exactpro.sf.util.AbstractTest;
import junit.framework.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class TestNTGDictionaryValidator extends AbstractTest {

    @Test
    public void testNTGDictionaryValidator() throws IOException {
        IDictionaryStructure dictionary = TestNTGHelper.getDictionary();

        IDictionaryValidator dictionaryValidator = new NTGDictionaryValidatorFactory()
                .createDictionaryValidator();

        List<DictionaryValidationError> errors = dictionaryValidator.validate(dictionary, true, null);

        Assert.assertEquals(2, errors.size());
        /*
         * Assert.assertEquals(
         * "Message  <strong>\"Logout\"</strong> doesn't contain IsAdmin attribute"
         * , errors.get(0).getError()); Assert.assertEquals(
         * "Message  <strong>\"Reject\"</strong> doesn't contain IsAdmin attribute"
         * , errors.get(1).getError()); Assert.assertEquals(
         * "Message  <strong>\"LogonReply\"</strong> doesn't contain IsAdmin attribute"
         * , errors.get(2).getError());
         */
    }
}
