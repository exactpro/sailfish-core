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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.configuration.dictionary.converter.SailfishDictionaryToQuckfixjConverter;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator;
import com.exactpro.sf.services.fix.FixMessageHelper;
import com.exactpro.sf.util.AbstractTest;

public class SfDictionaryConverterTest extends AbstractTest {

    private static IDictionaryStructure dictionary;
    private IDictionaryStructure newSfDictionary;
    private static IDictionaryValidator dictionaryValidator;
    private static SailfishDictionaryToQuckfixjConverter converter;
    private static Transformer transformer;
    private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();
    private static final Logger logger = LoggerFactory.getLogger(SfDictionaryConverterTest.class);
    private static final String fileName = "FIX50.CONVERTER.TEST.xml";
    private static final String outputFolder = "testConvert";
    private final List<String> entityTypes = new ArrayList<>(Arrays.asList(FixMessageHelper.MESSAGE_ENTITY,
            FixMessageHelper.HEADER_ENTITY, FixMessageHelper.TRAILER_ENTITY));

    @BeforeClass
    public static void initClass() {
        ClassLoader classLoader = SfDictionaryConverterTest.class.getClassLoader();
        try (InputStream in = classLoader.getResourceAsStream("fix/qfj2dict.xsl");
                InputStream inTypes = classLoader.getResourceAsStream("fix/types.xml")) {
            dictionaryValidator = new FIXDictionaryValidatorFactory().createDictionaryValidator();
            converter = new SailfishDictionaryToQuckfixjConverter();
            File xsl = new File(outputFolder, "qfj2dict.xsl");
            File types = new File("types.xml");
            try (OutputStream out = FileUtils.openOutputStream(xsl);
                    OutputStream outTypes = FileUtils.openOutputStream(types)) {
                IOUtils.copy(in, FileUtils.openOutputStream(xsl));
                IOUtils.copy(inTypes, FileUtils.openOutputStream(types));
            }
            try (InputStream inXsl = new FileInputStream(xsl)) {
                transformer = transformerFactory.newTransformer(new StreamSource(inXsl));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testConvert() throws Exception {
        try {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("dictionary/" + fileName);
                    InputStream inConverter = SailfishDictionaryToQuckfixjConverter.class.getClassLoader()
                            .getResourceAsStream("dictionary/" + fileName)) {
                dictionary = loadMessageDictionary(in);
                converter.convertToQuickFixJ(inConverter, outputFolder);
            }

            String sessionDictionary = new File(
                    outputFolder, "FIXT11.xml").getAbsolutePath();
            transformer.setParameter("sessionDictionary", sessionDictionary);

            String pathToNewSfDict = outputFolder + File.separator + fileName;

            try (InputStream in = new FileInputStream(
                    outputFolder + File.separator + "FIX50.xml");
                    OutputStream out = FileUtils.openOutputStream(new File(pathToNewSfDict))) {
                StreamResult result = new StreamResult(out);
                transformer.transform(new StreamSource(in), result);
            }

            try (InputStream in = new FileInputStream(pathToNewSfDict)) {
                newSfDictionary = loadMessageDictionary(in);
            }

            List<DictionaryValidationError> errors = dictionaryValidator.validate(newSfDictionary, true, null);
            Assert.assertEquals(0, errors.size());

            for (IMessageStructure message : dictionary.getMessageStructures()) {
                if (entityTypes.contains(message.getAttributeValueByName(FixMessageHelper.ATTRIBUTE_ENTITY_TYPE))) {
                    assertEqualsMessage(message, newSfDictionary.getMessageStructure(message.getName()));
                }
            }

            for (IMessageStructure complexMessage : newSfDictionary.getMessageStructures()) {
                if (!entityTypes
                        .contains(complexMessage.getAttributeValueByName(FixMessageHelper.ATTRIBUTE_ENTITY_TYPE))) {
                    assertEqualsMessage(complexMessage, dictionary.getMessageStructure(complexMessage.getName()));
                }
            }

            for (IFieldStructure field : newSfDictionary.getFieldStructures()) {
                assertEqualsField(field, dictionary.getFieldStructure(field.getName()));
            }
        } finally {
            File del = new File(outputFolder);
            if (del.exists())
                FileUtils.deleteDirectory(del);
            File types = new File("types.xml");
            if (types.exists())
                types.delete();
        }
    }

    private void assertEqualsField(IFieldStructure expected, IFieldStructure actual) {
        Assert.assertEquals(expected.getName(), actual.getName());
        Assert.assertEquals(expected.getJavaType().value(), actual.getJavaType().value());

        Assert.assertEquals(expected.getAttributes().size(), actual.getAttributes().size());

        for (String attrName : expected.getAttributeNames()) {
            Assert.assertEquals(expected.getAttributes().get(attrName).getValue(),
                    actual.getAttributes().get(attrName).getValue());
        }

        Assert.assertEquals(expected.getValues().size(), actual.getValues().size());
        
        if (expected.getValues() != null) {
            Assert.assertEquals(expected.getValues().size(), actual.getValues().size());

            for (String value : expected.getValues().keySet()) {
                Assert.assertEquals(expected.getValues().get(value).getName(), actual.getValues().get(value).getName());
                Assert.assertEquals(expected.getValues().get(value).getValue(),
                        actual.getValues().get(value).getValue());
            }
        }
    }

    private void assertEqualsMessage(IMessageStructure expected, IMessageStructure actual) {
        Assert.assertEquals(expected.getName(), actual.getName());
        Assert.assertEquals(expected.getAttributes().size(), actual.getAttributes().size());

        for (String attrName : expected.getAttributeNames()) {
            Assert.assertEquals(expected.getAttributes().get(attrName).getValue(),
                    actual.getAttributes().get(attrName).getValue());
        }

        Assert.assertEquals(expected.getFields().size(), actual.getFields().size());

        for (String fieldName : expected.getFieldNames()) {
            assertEqualsMessageField(expected.getField(fieldName), actual.getField(fieldName));
        }
    }

    private void assertEqualsMessageField(IFieldStructure expected, IFieldStructure actual) {
        Assert.assertEquals(expected.getName(), actual.getName());
        Assert.assertEquals(expected.getReferenceName(), actual.getReferenceName());
        Assert.assertEquals(expected.getStructureType().name(), actual.getStructureType().name());
        Assert.assertEquals(expected.isRequired(), actual.isRequired());
    }
}
