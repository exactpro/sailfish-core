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
package com.exactpro.sf.services.fix;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;

import quickfix.DataDictionaryProvider;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.MessageUtils;
import quickfix.Session;
import quickfix.field.ApplVerID;
import quickfix.fix50.MessageFactory;


public class QFJDictionaryAdapterTest {
    private Session session;
    private QFJDictionaryAdapter qfjDictionaryAdapter;

    @Before
    public void init() throws IOException {
        this.session = mock(Session.class);
        DataDictionaryProvider provider = mock(DataDictionaryProvider.class);
        when(session.getDataDictionaryProvider()).thenReturn(provider);
        when(session.getMessageFactory()).thenReturn(new MessageFactory());
        when(session.getTargetDefaultApplicationVersionID()).thenReturn(new ApplVerID(ApplVerID.FIX50SP2));

        IDictionaryStructure dictionaryStructure;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("dictionary/FIX50.TEST.xml")) {
        	dictionaryStructure = new XmlDictionaryStructureLoader().load(in);
    	}

        this.qfjDictionaryAdapter = new QFJDictionaryAdapter(dictionaryStructure);
        when(provider.getApplicationDataDictionary(new ApplVerID(ApplVerID.FIX50SP2))).thenReturn(this.qfjDictionaryAdapter);
    }

    @Test
    public void testValidateInsertGroup() throws Exception {
        try {
            String messageString =  "8=FIXT.1.19=24235=X49=TOR56=ft0134=852=20150529-11:30:20.596262=14328989480720000002-MBO1091=Y235=XD268=1279=0453=1448=25910=24";
            Message message = MessageUtils.parse(this.session, messageString);
            this.qfjDictionaryAdapter.setAllowUnknownMessageFields(true);
            this.qfjDictionaryAdapter.validate(message, false);
        } catch (Exception e){
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testSingleGroupInOtherGroup() throws Exception {
        try {
            String messageString = "8=FIXT.1.19=00010135=CM49=AAA56=EEEE134=252=20160222-16:06:47.7571677=11671=11691=EEE11669=11529=11530=41534=11535=110=25";
            Message msg = new Message();
            msg.fromString(messageString, qfjDictionaryAdapter, true);
//            Message message = MessageUtils.parse(this.session, messageString);
            this.qfjDictionaryAdapter.setAllowUnknownMessageFields(false);
            this.qfjDictionaryAdapter.validate(msg, false);
        } catch (Exception e){
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testAllowOtherValues() throws Exception {
        try {
            String messageString =  "8=FIXT.1.19=23535=X49=TOR56=ft0134=852=20150529-11:30:20.596262=14328989480720000002-MBO234=000.1235=XD268=1279=0453=1448=25910=126";
            Message message = MessageUtils.parse(this.session, messageString);
            this.qfjDictionaryAdapter.setAllowUnknownMessageFields(true);
            this.qfjDictionaryAdapter.validate(message, false);
        } catch (Exception e){
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            String messageString =  "8=FIXT.1.19=23535=X49=TOR56=ft0134=852=20150529-11:30:20.596262=14328989480720000002-MBO234=XD235=0.001268=1279=0453=1448=25910=126";
            Message message = MessageUtils.parse(this.session, messageString);
            this.qfjDictionaryAdapter.setAllowUnknownMessageFields(true);
            this.qfjDictionaryAdapter.validate(message, false);
        } catch (IncorrectTagValue e) {
            Assert.assertEquals(235, e.field);
        }
    }

}