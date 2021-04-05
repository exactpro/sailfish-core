/******************************************************************************
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
package com.exactpro.sf.services.http.dictionary;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationError;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator;


/**
 * @author oleg.smirnov
 *
 */
public class TestHTTPDictionaryValidator {
    private final IDictionaryValidator validator = new HTTPDictionaryValidatorFactory().createDictionaryValidator();
    private final IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();

    @Test
    public void testValidatorPositive() {
        IDictionaryStructure dictionaryStructure = null;
        try (InputStream in = getClass().getResourceAsStream("/dictionaries/positive.xml")) {
            dictionaryStructure = loader.load(in);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail();
        }

        List<DictionaryValidationError> errors = validator.validate(dictionaryStructure, true, null);
        for (DictionaryValidationError err : errors) {
            System.out.println(err);
        }
        Assert.assertEquals("Dictionary contains errors", 0, errors.size());
    }

    @Test
    public void testValidatorNegative() {
        IDictionaryStructure dictionaryStructure = null;
        try (InputStream in = getClass().getResourceAsStream("/dictionaries/negative.xml")) {
            dictionaryStructure = loader.load(in);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail();
        }

        List<DictionaryValidationError> errors = validator.validate(dictionaryStructure, true, null);
        List<String> errorsText = new ArrayList<>(errors.size());
        for (DictionaryValidationError err : errors) {
            System.out.println(err);
            errorsText.add(err.toString());
        }
        String[] expectedErrors = {
                "DictionaryValidationError[message=GetEmailInbox,field=URI,error=Message doesn't have field inboxId for parameter in URI,level=MESSAGE,type=ERR_REQUIRED_FIELD]",
                "DictionaryValidationError[message=GetEmailInbox,field=<null>,error=Message have attribute Method with unknown HTTP Method UNKNOWN. Available: GET, HEAD, POST, PUT, PATCH, DELETE, TRACE, CONNECT,level=MESSAGE,type=ERR_ATTRIBUTES]",
                "DictionaryValidationError[message=GetEmailInbox,field=URI,error=Message doesn't have field inboxId for parameter in URI,level=MESSAGE,type=ERR_REQUIRED_FIELD]",
                "DictionaryValidationError[message=MsgWithoutURI,field=<null>,error=Message has field URI but doesn't contains attribute URI,level=MESSAGE,type=ERR_ATTRIBUTES]",
                "DictionaryValidationError[message=SomeMessage,field=<null>,error=Message have attribute Response with unknown message name SomeOtherMessage,level=MESSAGE,type=ERR_ATTRIBUTES]",
                "DictionaryValidationError[message=SomeMessage,field=<null>,error=Message doesn't have field URI with parameters for attribute URI,level=MESSAGE,type=ERR_ATTRIBUTES]",
                "DictionaryValidationError[message=AnotherValidationError,field=<null>,error=Code 404 already mapped to [AnotherValidationError,ValidationError],level=MESSAGE,type=ERR_ATTRIBUTES]",
                "DictionaryValidationError[message=ValidationError,field=<null>,error=Code 404 already mapped to [AnotherValidationError,ValidationError],level=MESSAGE,type=ERR_ATTRIBUTES]"
        };

        Assert.assertEquals("Errors count more or less than expected", expectedErrors.length, errors.size());
        for (String error : expectedErrors) {
            if (!errorsText.contains(error)) {
                Assert.fail("Validator doesn't find error: " + error);
            }
        }
    }
}
