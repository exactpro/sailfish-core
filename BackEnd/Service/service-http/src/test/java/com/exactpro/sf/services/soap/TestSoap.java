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
package com.exactpro.sf.services.soap;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.configuration.factory.SOAPMessageFactory;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.http.HTTPClientSettings;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TestSoap {
    public static SOAPDecoder soapDecoder = new SOAPDecoder();
    public static IDictionaryStructure dictionary;
    public static IMessageFactory factory;

    @BeforeClass
    public static void init() throws IOException {
        try(InputStream is = TestSoap.class.getClassLoader().getResourceAsStream("dictionaries/soap.xml")) {
            dictionary = new XmlDictionaryStructureLoader().load(is);
        }

        factory = new SOAPMessageFactory();
        factory.init(SailfishURI.unsafeParse("TEST"), dictionary);

        HTTPClientSettings settings = new HTTPClientSettings();
        settings.setURI("http://test.com");

        soapDecoder.init(settings, factory, dictionary, "test");
    }

    @Test
    public void testSoapMessageWithNilTags() throws Exception {
        IMessage message = factory.createMessage("Message");
        message.getMetaData().setRawMessage(
                Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource("soapMessage.xml").toURI())));
        List<Object> out = new ArrayList<Object>();
        soapDecoder.decode(null, message, out);
        IMessage decoded = (IMessage) out.get(0);
        Assert.assertNotNull(decoded);
        IMessage msg = decoded.getField("message");
        Assert.assertNotNull(msg);
        IMessage ret = msg.getField("return");
        Assert.assertNotNull(ret);
        Assert.assertNull(ret.getField("field"));
        List<Integer> list = ret.getField("collection");
        Assert.assertNotNull(list);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals(128, (int)list.get(0));
    }
}
