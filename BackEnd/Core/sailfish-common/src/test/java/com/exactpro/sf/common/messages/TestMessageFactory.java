/******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.sf.common.impl.messages.AbstractMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.configuration.suri.SailfishURI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestMessageFactory {
    /**
     * This dictionary contains message marked by IsEncodeStructure attribute
     */
    public static final String FILE_NAME_DICTIONARY = "withIsEncodeStructure.xml";

    private IDictionaryStructure dictionary;
    private IMessageFactory messageFactory;

    @Before
    public void setUp() throws IOException {
        try (InputStream inputStream = Files.newInputStream(Paths.get("src","test", "resources", FILE_NAME_DICTIONARY))) {
            IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();
            dictionary = loader.load(inputStream);
        }
        messageFactory = new MessageFactory();
        messageFactory.init(SailfishURI.unsafeParse("test"), dictionary);
    }

    @Test
    public void testMessageWithIsEncodeStructures() {
        IMessage root = messageFactory.createMessage("Root");
        IMessage subMessage = root.getField("SubMessage");
        Assert.assertNotNull(subMessage);
        IMessage subSubMessage = subMessage.getField("SubSubMessage");
        Assert.assertNotNull(subSubMessage);
        IMessage subSubSubMessage = subSubMessage.getField("SubSubSubMessage");
        Assert.assertNotNull(subSubSubMessage);
    }

    @Test
    public void testDisabledIsEncodeStructures() {
        IMessage rootWithoutEncode = messageFactory.createMessage("RootWithoutEncode");
        Assert.assertNull(rootWithoutEncode.getField("SubMessage"));
    }

    private static class MessageFactory extends AbstractMessageFactory {
        @Override
        public String getProtocol() {
            return "test";
        }
    }
}