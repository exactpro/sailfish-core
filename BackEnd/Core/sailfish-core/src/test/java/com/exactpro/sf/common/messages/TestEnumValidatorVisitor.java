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
package com.exactpro.sf.common.messages;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.common.impl.messages.MapMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.util.EPSTestCase;

public class TestEnumValidatorVisitor extends EPSTestCase {
    private static final String DICTIONARY_PATH = "src" + File.separator + "test" + File.separator + "resources" + File.separator + "enums.xml";

    @Test
    public void testValidation() throws EPSCommonException, FileNotFoundException {
        IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();
        IDictionaryStructure dictionary = loader.load(new FileInputStream(new File(getBaseDir(), DICTIONARY_PATH)));
        IMessageStructure structure = dictionary.getMessageStructure("Message");

        IMessage nested1 = new MapMessage(dictionary.getNamespace(), "NestedMessage");

        nested1.addField("NestedSimple", 3);
        nested1.addField("NestedCollection", Arrays.asList(4, 5));

        IMessage nested2 = new MapMessage(dictionary.getNamespace(), "NestedMessage");

        nested2.addField("NestedSimple", 6);
        nested2.addField("NestedCollection", Arrays.asList(7, 8));

        IMessage nested3 = new MapMessage(dictionary.getNamespace(), "NestedMessage");

        nested3.addField("NestedSimple", 9);
        nested3.addField("NestedCollection", Arrays.asList(10, 11));

        IMessage message = new MapMessage(dictionary.getNamespace(), structure.getName());

        message.addField("Simple", 3);
        message.addField("Collection", Arrays.asList(4, 5));
        message.addField("NestedMessage", nested1);
        message.addField("NestedMessageCollection", Arrays.asList(nested2, nested3));

        EnumValidatorVisitor visitor = new EnumValidatorVisitor();
        MessageStructureReader reader = new MessageStructureReader();
        List<String> errors = visitor.getErrors();

        reader.traverse(visitor, structure, message, MessageStructureReaderHandlerImpl.instance());

        int i = 0;

        Assert.assertEquals(errors.get(i++), "Unknown value in field 'Simple': 3 (expected values: [1, 2])");
        Assert.assertEquals(errors.get(i++), "Unknown value in field 'Collection[0]': 4 (expected values: [1, 2])");
        Assert.assertEquals(errors.get(i++), "Unknown value in field 'Collection[1]': 5 (expected values: [1, 2])");
        Assert.assertEquals(errors.get(i++), "Unknown value in field 'NestedMessage.NestedSimple': 3 (expected values: [1, 2])");
        Assert.assertEquals(errors.get(i++), "Unknown value in field 'NestedMessage.NestedCollection[0]': 4 (expected values: [1, 2])");
        Assert.assertEquals(errors.get(i++), "Unknown value in field 'NestedMessage.NestedCollection[1]': 5 (expected values: [1, 2])");
        Assert.assertEquals(errors.get(i++), "Unknown value in field 'NestedMessageCollection[0].NestedSimple': 6 (expected values: [1, 2])");
        Assert.assertEquals(errors.get(i++), "Unknown value in field 'NestedMessageCollection[0].NestedCollection[0]': 7 (expected values: [1, 2])");
        Assert.assertEquals(errors.get(i++), "Unknown value in field 'NestedMessageCollection[0].NestedCollection[1]': 8 (expected values: [1, 2])");
        Assert.assertEquals(errors.get(i++), "Unknown value in field 'NestedMessageCollection[1].NestedSimple': 9 (expected values: [1, 2])");
        Assert.assertEquals(errors.get(i++), "Unknown value in field 'NestedMessageCollection[1].NestedCollection[0]': 10 (expected values: [1, 2])");
        Assert.assertEquals(errors.get(i++), "Unknown value in field 'NestedMessageCollection[1].NestedCollection[1]': 11 (expected values: [1, 2])");
    }
}
