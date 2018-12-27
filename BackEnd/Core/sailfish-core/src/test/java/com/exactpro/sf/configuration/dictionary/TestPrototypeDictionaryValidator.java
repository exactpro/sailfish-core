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

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.configuration.dictionary.impl.PrototypeDictionaryValidator;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator;

public class TestPrototypeDictionaryValidator {

    private final static String PROTOTYPE_DICTIONARY = Paths.get("dictionaries","prototypeDictionary.xml").toString();
    private final static String VALID_DICTIONARY = Paths.get("dictionaries", "validDictionary.xml").toString();
    private final static String INVALID_DICTIONARY = Paths.get("dictionaries", "invalidDictionary.xml").toString();

    private static IDictionaryValidator prototypeValidator;
    private static IDictionaryStructure validDictionary;
    private static IDictionaryStructure invalidDictionary;

    @BeforeClass
    public static void init() {
        prototypeValidator = new PrototypeDictionaryValidator(TestPrototypeDictionaryValidator.class.getClassLoader().getResourceAsStream(PROTOTYPE_DICTIONARY), true);
        XmlDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();
        validDictionary = loader.load(TestPrototypeDictionaryValidator.class.getClassLoader().getResourceAsStream(VALID_DICTIONARY));
        invalidDictionary = loader.load(TestPrototypeDictionaryValidator.class.getClassLoader().getResourceAsStream(INVALID_DICTIONARY));
    }

    @Test
    public void testInvalid() {
        List<DictionaryValidationError> errors = prototypeValidator.validate(invalidDictionary, true, null);
        String[] expectedErrors = new String[] {
                "DictionaryValidationError[message=<null>,field=EnumNumber,error=Value <strong>SECOND</strong> was missed,level=FIELD,type=ERR_VALUES]",
                "DictionaryValidationError[message=<null>,field=EnumNumber,error=Value <strong>THIRD</strong> should be <strong>3</strong> but was <strong>4</strong>,level=FIELD,type=ERR_VALUES]",
                "DictionaryValidationError[message=SecondMessage,field=ComplexField,error=<strong>JavaType</strong> should be <strong>null</strong> but was <strong>JAVA_LANG_INTEGER</strong>,level=FIELD,type=ERR_FIELD_TYPE]",
                "DictionaryValidationError[message=SecondMessage,field=ComplexField,error=<strong>IsServiceName</strong> should be <strong>null</strong> but was <strong>false</strong>,level=FIELD,type=ERR_FIELD_TYPE]",
                "DictionaryValidationError[message=SecondMessage,field=ComplexField,error=<strong>IsComplex</strong> should be <strong>true</strong> but was <strong>false</strong>,level=FIELD,type=ERR_FIELD_TYPE]",
                "DictionaryValidationError[message=SecondMessage,field=ComplexField,error=<strong>IsEnum</strong> should be <strong>false</strong> but was <strong>true</strong>,level=FIELD,type=ERR_FIELD_TYPE]",
                "DictionaryValidationError[message=SecondMessage,field=ComplexField,error=Attribute <strong>BoolAttr</strong> was missed,level=FIELD,type=ERR_ATTRIBUTES]",
                "DictionaryValidationError[message=SecondMessage,field=FieldWithAttr,error=Attribute <strong>NumbAttr</strong> was missed,level=FIELD,type=ERR_ATTRIBUTES]",
                "DictionaryValidationError[message=SecondMessage,field=<null>,error=Attribute <strong>LongAttr</strong> should be <strong>10</strong> but was <strong>101</strong>,level=MESSAGE,type=ERR_ATTRIBUTES]",
                "DictionaryValidationError[message=FirstMessage,field=FirstField,error=<strong>IsRequired</strong> should be <strong>true</strong> but was <strong>false</strong>,level=FIELD,type=ERR_FIELD_TYPE]",
                "DictionaryValidationError[message=FirstMessage,field=CollectionField,error=<strong>IsCollection</strong> should be <strong>true</strong> but was <strong>false</strong>,level=FIELD,type=ERR_FIELD_TYPE]",
                "DictionaryValidationError[message=FirstMessage,field=FourthField,error=<strong>JavaType</strong> should be <strong>JAVA_LANG_CHARACTER</strong> but was <strong>JAVA_LANG_SHORT</strong>,level=FIELD,type=ERR_FIELD_TYPE]",
                "DictionaryValidationError[message=FirstMessage,field=EnumField,error=Value <strong>SECOND</strong> was missed,level=FIELD,type=ERR_VALUES]",
                "DictionaryValidationError[message=FirstMessage,field=EnumField,error=Value <strong>THIRD</strong> should be <strong>3</strong> but was <strong>4</strong>,level=FIELD,type=ERR_VALUES]",
                "DictionaryValidationError[message=FirstMessage,field=SecondEnumField,error=<strong>JavaType</strong> should be <strong>JAVA_LANG_INTEGER</strong> but was <strong>JAVA_LANG_DOUBLE</strong>,level=FIELD,type=ERR_FIELD_TYPE]",
                "DictionaryValidationError[message=FirstMessage,field=SecondEnumField,error=<strong>IsEnum</strong> should be <strong>true</strong> but was <strong>false</strong>,level=FIELD,type=ERR_FIELD_TYPE]",
                "DictionaryValidationError[message=FirstMessage,field=SecondEnumField,error=Value <strong>FIRST</strong> was missed,level=FIELD,type=ERR_VALUES]",
                "DictionaryValidationError[message=FirstMessage,field=SecondEnumField,error=Value <strong>SECOND</strong> was missed,level=FIELD,type=ERR_VALUES]",
                "DictionaryValidationError[message=FirstMessage,field=SecondEnumField,error=Value <strong>THIRD</strong> was missed,level=FIELD,type=ERR_VALUES]",
                "DictionaryValidationError[message=FirstMessage,field=<null>,error=Attribute <strong>NumAttr</strong> was missed,level=MESSAGE,type=ERR_ATTRIBUTES]",
                "DictionaryValidationError[message=FirstMessage,field=<null>,error=At this position message should have field <strong>CollectionField</strong> but it was <strong>ThirdField</strong>,level=MESSAGE,type=ERR_NAME]",
                "DictionaryValidationError[message=FirstMessage,field=<null>,error=At this position message should have field <strong>EnumField</strong> but it was <strong>SecondEnumField</strong>,level=MESSAGE,type=ERR_NAME]",
                "DictionaryValidationError[message=FirstMessage,field=<null>,error=At this position message should have field <strong>FourthField</strong> but it was <strong>EnumField</strong>,level=MESSAGE,type=ERR_NAME]",
                "DictionaryValidationError[message=FirstMessage,field=<null>,error=At this position message should have field <strong>SecondEnumField</strong> but it was <strong>null</strong>,level=MESSAGE,type=ERR_NAME]",
                "DictionaryValidationError[message=FirstMessage,field=<null>,error=At this position message should have field <strong>SecondField</strong> but it was <strong>CollectionField</strong>,level=MESSAGE,type=ERR_NAME]",
                "DictionaryValidationError[message=FirstMessage,field=<null>,error=Missed required field <strong>SecondField</strong>,level=FIELD,type=ERR_REQUIRED_FIELD]",
                "DictionaryValidationError[message=FirstMessage,field=<null>,error=At this position message should have field <strong>ThirdField</strong> but it was <strong>FourthField</strong>,level=MESSAGE,type=ERR_NAME]",
                "DictionaryValidationError[message=FourthMessage,field=<null>,error=At this position message should have field <strong>Third</strong> but it was <strong>SomeExtraField</strong>,level=MESSAGE,type=ERR_NAME]",
                "DictionaryValidationError[message=SubMessage,field=FieldWithAttr,error=Attribute <strong>StrAttr</strong> should be <strong>value</strong> but was <strong>invalidValue</strong>,level=FIELD,type=ERR_ATTRIBUTES]",
                "DictionaryValidationError[message=ThirdMessage,field=<null>,error=Missed required message ThirdMessage,level=DICTIONARY,type=ERR_REQUIRED_FIELD]"
        };
        if (expectedErrors.length != errors.size()) {
            errors.forEach(System.out::println);
        }
        Assert.assertEquals("Some unexpected errors", expectedErrors.length, errors.size());

        List<String> actualErrors = errors.stream()
                .map(DictionaryValidationError::toString)
                .collect(Collectors.toList());

        boolean allFind = true;
        for (String expectedError : expectedErrors) {
            if (!actualErrors.contains(expectedError)) {
                System.out.println("Error \"" + expectedError + "\" wasn't find");
                allFind = false;
            }
        }

        Assert.assertTrue("Some errors wasn't find", allFind);
    }

    @Test
    public void testValid() {
        List<DictionaryValidationError> errors = prototypeValidator.validate(validDictionary, true, null);
        if (!errors.isEmpty()) {
            for (DictionaryValidationError error : errors) {
                System.out.println(error);
            }
        }
        Assert.assertEquals("Have some unexpected errors", 0, errors.size());
    }
}
