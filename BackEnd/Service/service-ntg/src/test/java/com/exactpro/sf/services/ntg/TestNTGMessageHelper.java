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
package com.exactpro.sf.services.ntg;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.util.AbstractTest;

/**
 * @author nikita.smirnov
 *
 */
public class TestNTGMessageHelper extends AbstractTest {

    private NTGMessageHelper messageHelper;
    private IMessageFactory messageFactory;
    private IDictionaryStructure dictionary;
    private String namespace;

    @Before
    public void init() throws IOException {
        try (InputStream inputStream = new FileInputStream(new File("src/test/plugin/cfg/dictionaries/", "ntg.xml"))) {
            this.dictionary = loadMessageDictionary(inputStream);
            this.messageFactory = DefaultMessageFactory.getFactory();
            this.messageHelper = new NTGMessageHelper();
            this.messageHelper.init(this.messageFactory, this.dictionary);
            this.namespace = this.messageHelper.getNamespace();
        }

    }

    @Test
    public void testMessageType() throws IOException {
        IFieldStructure typeField = this.dictionary.getFieldStructure("MessageType");
        for (String element : typeField.getValues().keySet()) {
            IMessage message = this.messageFactory.createMessage(element, this.namespace);
            this.messageHelper.prepareMessageToEncode(message, null);
            checkValueInMessageHeader(message, "MessageType", typeField.getValues().get(element).getCastValue());
            checkValueInMessageHeader(message, "StartOfMessage", 2);
        }
    }

    private void checkValueInMessageHeader(IMessage message, String field, Object value) {
        IMessage header = (IMessage) message.getField(NTGMessageHelper.MESSAGE_HEADER);
        Assert.assertEquals("MessageType = " + message.getName(), value, header.getField(field));
    }
}
