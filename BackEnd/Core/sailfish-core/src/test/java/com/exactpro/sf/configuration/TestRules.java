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
package com.exactpro.sf.configuration;

import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;
import com.exactpro.sf.services.util.RulesProcessor;
import com.exactpro.sf.util.AbstractTest;

public class TestRules extends AbstractTest {

	private Rules load(String alias) throws FileNotFoundException, WorkspaceSecurityException, JAXBException, SailfishURIException {
		JAXBContext jc = JAXBContext.newInstance(new Class[] { Rules.class });
		Unmarshaller u = jc.createUnmarshaller();

		InputStream stream = SFLocalContext.getDefault().getDataManager().getDataInputStream(SailfishURI.parse(alias));
		JAXBElement<Rules> root = u.unmarshal(new StreamSource(stream), Rules.class);
		Rules rules = root.getValue();

		Assert.assertNotNull(rules);
		return rules;
	}

	@Test
    public void testSimpleRules() throws FileNotFoundException, WorkspaceSecurityException, JAXBException, SailfishURIException {
    	Rules rules = load("TestSimpleRules");
    	RulesProcessor processor = new RulesProcessor(rules);

    	IMessage msg = null;

    	// empty messages
    	msg = DefaultMessageFactory.getFactory().createMessage("AddOrder", "TEST");
    	processor.processMessage(msg);
    	Assert.assertTrue(msg.getFieldNames().isEmpty());

    	msg = DefaultMessageFactory.getFactory().createMessage("AddOrderShort", "TEST");
    	processor.processMessage(msg);
    	Assert.assertTrue(msg.getFieldNames().isEmpty());

    	// When failed:
    	msg = DefaultMessageFactory.getFactory().createMessage("AddOrder", "TEST");
    	msg.addField("When", "Nope");
    	msg.addField("Change", "ChangeValue");
    	processor.processMessage(msg);
    	Assert.assertEquals(2, msg.getFieldNames().size());
    	Assert.assertEquals("Nope", msg.getField("When"));
    	Assert.assertEquals("ChangeValue", msg.getField("Change"));

    	// Load Before Save:
    	msg = DefaultMessageFactory.getFactory().createMessage("AddOrderShort", "TEST");
    	msg.addField("When", "OK");
    	msg.addField("Change", "ChangeValue");
    	processor.processMessage(msg);
    	Assert.assertEquals(4, msg.getFieldNames().size());
    	Assert.assertEquals("OK", msg.getField("When"));
    	Assert.assertEquals("ChangeValue", msg.getField("Change"));
        Assert.assertEquals(null, msg.<Object>getField("Load"));
        Assert.assertEquals(null, msg.<Object>getField("FakeToMapping"));

    	// SAVE + CHANGE + REMOVE
    	msg = DefaultMessageFactory.getFactory().createMessage("AddOrder", "TEST");
    	msg.addField("When", "OK");
    	msg.addField("Change", "ChangeValue");
    	msg.addField("ToRemove", "RemoveValue");
    	msg.addField("Save", "SaveValue");
    	msg.addField("FromMapping", "FromMappingValue");
    	msg.addField("ToMapping", "MappingValue");
    	processor.processMessage(msg);
    	Assert.assertEquals(5, msg.getFieldNames().size());
    	Assert.assertEquals("OK", msg.getField("When"));
    	Assert.assertEquals("NEW VALUE", msg.getField("Change"));
        Assert.assertEquals(null, msg.<Object>getField("ToRemove"));
    	Assert.assertEquals("SaveValue", msg.getField("Save"));
    	Assert.assertEquals("FromMappingValue", msg.getField("FromMapping"));
    	Assert.assertEquals("MappingValue", msg.getField("ToMapping"));

    	// LOAD
    	msg = DefaultMessageFactory.getFactory().createMessage("AddOrderShort", "TEST");
    	msg.addField("When", "OK");
    	msg.addField("FromMapping", "FromMappingValue");
    	processor.processMessage(msg);
    	Assert.assertEquals(4, msg.getFieldNames().size());
    	Assert.assertEquals("OK", msg.getField("When"));
    	Assert.assertEquals("SaveValue", msg.getField("Load"));
    	Assert.assertEquals("FromMappingValue", msg.getField("FromMapping"));
    	Assert.assertEquals("MappingValue", msg.getField("FakeToMapping"));
    }

}
