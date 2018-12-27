/*******************************************************************************
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
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator;

public class TestDefaultDictionaryValidator {

    private static final IDictionaryValidator validator = new DefaultDictionaryValidatorFactory().createDictionaryValidator();
    private static final String dictionaryPath = "dictionaries" + File.separator + "testDictionary.xml";
    private static IDictionaryStructure dictionaryStructure;

    @BeforeClass
    public static void init() throws Exception {
        try (InputStream in = TestDefaultDictionaryValidator.class.getClassLoader().getResourceAsStream(dictionaryPath)) {
            dictionaryStructure = new XmlDictionaryStructureLoader().load(in);
        }
    }

    @Test
    public void testValidator() {
        List<DictionaryValidationError> errors = validator.validate(dictionaryStructure, true, null);
        String[] expectedErrors = new String[] {
                "DictionaryValidationError[message=<null>,field=UnknownDefaultValue,error=Value <strong>\"3.3\"</strong> wasn't found in values table,level=FIELD,type=ERR_DEFAULT_VALUE]",
                "DictionaryValidationError[message=<null>,field=WrongAttributeValue,error=Value <strong>\"net\"</strong> is not applicable for Boolean type,level=FIELD,type=ERR_ATTRIBUTES]",
                "DictionaryValidationError[message=<null>,field=for,error=Prohibited name <strong>for</strong> for Field. Name can't be Java keyword or have value that presents in this list [_],level=FIELD,type=ERR_NAME]",
                "DictionaryValidationError[message=<null>,field=for,error=Prohibited name <strong>char</strong> for Value. Name can't be Java keyword or have value that presents in this list [Missed,Present,_],level=FIELD,type=ERR_VALUES]",
                "DictionaryValidationError[message=<null>,field=for,error=Prohibited name <strong>Missed</strong> for Value. Name can't be Java keyword or have value that presents in this list [Missed,Present,_],level=FIELD,type=ERR_VALUES]",
                "DictionaryValidationError[message=<null>,field=for,error=Prohibited name <strong>_</strong> for Value. Name can't be Java keyword or have value that presents in this list [Missed,Present,_],level=FIELD,type=ERR_VALUES]",
                "DictionaryValidationError[message=<null>,field=for,error=Prohibited name <strong>while</strong> for Attribute. Name can't be Java keyword or have value that presents in this list [_],level=FIELD,type=ERR_ATTRIBUTES]",
                "DictionaryValidationError[message=protected,field=<null>,error=Prohibited name <strong>protected</strong> for Message. Name can't be Java keyword or have value that presents in this list [_],level=MESSAGE,type=ERR_NAME]",
                "DictionaryValidationError[message=protected,field=<null>,error=Prohibited name <strong>static</strong> for Attribute. Name can't be Java keyword or have value that presents in this list [_],level=MESSAGE,type=ERR_ATTRIBUTES]",
                "DictionaryValidationError[message=Test,field=DuplicateField,error=Duplicated field <strong>\"DuplicateField\"</strong>,level=MESSAGE,type=ERR_DUPLICATE_NAME]",
                "DictionaryValidationError[message=DuplicateMessage,field=Field1,error=Value <strong>\"3.3\"</strong> wasn't found in values table,level=FIELD,type=ERR_DEFAULT_VALUE]",
                "DictionaryValidationError[message=DuplicateMessage,field=SomeField1,error=Value <strong>\"net\"</strong> is not applicable for Boolean type,level=FIELD,type=ERR_ATTRIBUTES]"
        };
        if (expectedErrors.length != errors.size()) {
            errors.forEach(System.err::println);
        }
        Assert.assertEquals("Unexpected errors", expectedErrors.length, errors.size());

        List<String> actualErrors = errors.stream().map(DictionaryValidationError::toString).collect(Collectors.toList());
        boolean findAll = true;
        for (String error : expectedErrors) {
            if (!actualErrors.contains(error)) {
                findAll = false;
                System.err.println(error);
            }
        }

        Assert.assertTrue("Some errors wasn't found", findAll);
    }
}
