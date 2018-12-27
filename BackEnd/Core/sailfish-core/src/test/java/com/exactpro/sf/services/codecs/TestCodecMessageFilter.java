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
package com.exactpro.sf.services.codecs;

import java.io.InputStream;

import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;

public class TestCodecMessageFilter {

    @Test
    public void testFilter() throws Exception {
        XmlDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();
        try (InputStream stream =  getClass().getClassLoader().getResourceAsStream("dictionaries/testFilter.xml")) {
            IDictionaryStructure dictionary = loader.load(stream);
            IoSession session = new DummySession();
            CodecMessageFilter filter = new CodecMessageFilter("a, b, c");
            filter.init(dictionary);

            IMessage notDropped_1 = DefaultMessageFactory.getFactory().createMessage("Filter", dictionary.getNamespace());
            notDropped_1.addField("Hello", "a");
            notDropped_1.addField("World", "a");

            IMessage notDropped_2 = DefaultMessageFactory.getFactory().createMessage("Filter", dictionary.getNamespace());
            notDropped_2.addField("Hello", "b");
            notDropped_2.addField("World", "aaaaaaa");

            IMessage notDropped_3 = DefaultMessageFactory.getFactory().createMessage("Filter", dictionary.getNamespace());
            notDropped_3.addField("Hello", "c");
            notDropped_3.addField("World", "ddddddd");

            IMessage notDropped_4 = DefaultMessageFactory.getFactory().createMessage("Filter", dictionary.getNamespace());
            notDropped_4.addField("Hello", "dddddd");
            notDropped_4.addField("World", "c");

            IMessage notDropped_5 = DefaultMessageFactory.getFactory().createMessage("Filter", dictionary.getNamespace());
            notDropped_5.addField("Hello", "a");

            IMessage notDropped_6 = DefaultMessageFactory.getFactory().createMessage("SubFilter", dictionary.getNamespace());
            notDropped_6.addField("Hello", "a");

            IMessage notDropped_7 = DefaultMessageFactory.getFactory().createMessage("FilterSubFilter", dictionary.getNamespace());
            notDropped_7.addField("Hello", "a");
            notDropped_7.addField("World", "a");

            IMessage notDropped_8 = DefaultMessageFactory.getFactory().createMessage("NoFilters", dictionary.getNamespace());
            notDropped_8.addField("Hello", "a");
            notDropped_8.addField("World", "a");

            IMessage notDropped_9 = DefaultMessageFactory.getFactory().createMessage("NoFilters", dictionary.getNamespace());
            notDropped_9.addField("Hello", "aaaaaaaa");
            notDropped_9.addField("World", "asdasdadsasd");

            IMessage notDropped_10 = DefaultMessageFactory.getFactory().createMessage("FiltersSubFilter", dictionary.getNamespace());
            notDropped_10.addField("Hello", "a");
            notDropped_10.addField("World", "a");
            notDropped_10.addField("NewWorld", "asdasdadsasd");

            IMessage notDropped_11 = DefaultMessageFactory.getFactory().createMessage("FilterSubFilters", dictionary.getNamespace());
            notDropped_11.addField("Hello", "a");
            notDropped_11.addField("World", "a");
            notDropped_11.addField("NewWorld", "asdasdadsasd");

            IMessage dropped_1 = DefaultMessageFactory.getFactory().createMessage("Filter", dictionary.getNamespace());
            dropped_1.addField("Hello", "dddddd");
            dropped_1.addField("World", "dddddd");

            IMessage dropped_2 = DefaultMessageFactory.getFactory().createMessage("SubFilter", dictionary.getNamespace());
            dropped_2.addField("Hello", "c");

            IMessage dropped_3 = DefaultMessageFactory.getFactory().createMessage("FiltersSubFilter", dictionary.getNamespace());
            dropped_3.addField("Hello", "d");
            dropped_3.addField("World", "d");
            dropped_3.addField("NewWorld", "asdasdadsasd");

            IMessage dropped_4 = DefaultMessageFactory.getFactory().createMessage("FiltersSubFilter", dictionary.getNamespace());
            dropped_4.addField("World", "d");
            dropped_4.addField("NewWorld", "asdasdadsasd");

            IMessage dropped_5 = DefaultMessageFactory.getFactory().createMessage("SubFilter", dictionary.getNamespace());
            dropped_5.addField("Hello", "asdasdadsasd");

            IMessageStructure filterStructure = dictionary.getMessageStructure("Filter");
            IMessageStructure subFilterStructure = dictionary.getMessageStructure("SubFilter");
            IMessageStructure filterSubFilterStructure = dictionary.getMessageStructure("FilterSubFilter");
            IMessageStructure noFiltersStructure = dictionary.getMessageStructure("NoFilters");
            IMessageStructure filtersSubFilterStructure = dictionary.getMessageStructure("FiltersSubFilter");
            IMessageStructure filterSubFiltersStructure = dictionary.getMessageStructure("FilterSubFilters");

            Assert.assertEquals(false, filter.dropMessage(session, filterStructure, notDropped_1));
            Assert.assertEquals(false, filter.dropMessage(session, filterStructure, notDropped_2));
            Assert.assertEquals(false, filter.dropMessage(session, filterStructure, notDropped_3));
            Assert.assertEquals(false, filter.dropMessage(session, filterStructure, notDropped_4));
            Assert.assertEquals(false, filter.dropMessage(session, filterStructure, notDropped_5));

            Assert.assertEquals(true, filter.dropMessage(session, filterStructure, dropped_1));
            Assert.assertEquals(true, filter.dropMessage(session, subFilterStructure, dropped_2));

            Assert.assertEquals(true, filter.dropMessage(session, subFilterStructure, notDropped_6));
            Assert.assertEquals(false, filter.dropMessage(session, filterSubFilterStructure, notDropped_7));
            Assert.assertEquals(false, filter.dropMessage(session, subFilterStructure, notDropped_6));

            Assert.assertEquals(true, filter.dropMessage(session, filtersSubFilterStructure, dropped_3));
            Assert.assertEquals(true, filter.dropMessage(session, filterSubFiltersStructure, dropped_4));
            Assert.assertEquals(true, filter.dropMessage(session, subFilterStructure, dropped_5));

            Assert.assertEquals(false, filter.dropMessage(session, noFiltersStructure, notDropped_8));
            Assert.assertEquals(false, filter.dropMessage(session, noFiltersStructure, notDropped_9));

            Assert.assertEquals(false, filter.dropMessage(session, filtersSubFilterStructure, notDropped_10));
            Assert.assertEquals(false, filter.dropMessage(session, filterSubFiltersStructure, notDropped_11));
            Assert.assertEquals(false, filter.dropMessage(session, subFilterStructure, dropped_5));
        }
    }

}