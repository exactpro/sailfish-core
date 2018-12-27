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
package com.exactpro.sf.xml;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.impl.messages.xml.XMLTransmitter;
import com.exactpro.sf.common.messages.CreateIMessageVisitor;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MessageStructureWriter;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.storage.FilterCriterion;
import com.exactpro.sf.storage.xml.DataMessage;
import com.exactpro.sf.storage.xml.DataMessageConverter;
import com.exactpro.sf.storage.xml.DataMessagePersister;
import com.exactpro.sf.storage.xml.XmlDataMessage;
import com.exactpro.sf.storage.xml.db.DBPersister;
import com.exactpro.sf.util.AbstractTest;

@Ignore
public class StoreTest extends AbstractTest {
    private static final SailfishURI DICTIONARY_URI = SailfishURI.unsafeParse("Example");

	public static void main(String[] str) throws Exception{

		//new StoreTest().entitiesCircle();
		new StoreTest().dbRetrieve();

	}

	@Test
	@Ignore //FIXME compare MapMessage not work
	public void entitiesCircle() throws Exception {

		IDictionaryStructure dictionary =  createNative108();

		IMessage imessage = createSimpleMessage(dictionary);

		DataMessage dataMessage = DataMessageConverter.createDataMessage(imessage, dictionary);

		IMessage traversedMessage = DataMessageConverter.createMapMessage(dataMessage, dictionary);

		Assert.assertEquals(traversedMessage.compare(imessage), true);
	}

	private IDictionaryStructure createNative108() throws IOException  {

		return SFLocalContext.getDefault().getDictionaryManager().getDictionary(DICTIONARY_URI);
	}

	private IMessage createSimpleMessage(IDictionaryStructure dictionary){

		IMessageFactory imf = DefaultMessageFactory.getFactory();

		MessageStructureWriter wtraverser = new MessageStructureWriter();

		IMessageStructure messageStructure = dictionary.getMessageStructures().get(0);

		CreateIMessageVisitor visitor = new CreateIMessageVisitor(imf, messageStructure.getName(), dictionary.getNamespace());

		wtraverser.traverse(visitor, messageStructure);

		return visitor.getImessage();
	}

    @Ignore //TODO: Check and fix later
    @Test
	public void serializationTime() throws Exception {

		IDictionaryStructure dictionary =  createNative108();

		IMessage imessage = createSimpleMessage(dictionary);

		DataMessage dataMessage = DataMessageConverter.createDataMessage(imessage, dictionary);

		List<DataMessage> dmessages = new LinkedList<DataMessage>();

		for(int i = 0 ; i < 1000 ; i++){
			dmessages.add(dataMessage);
		}

		long begin = System.currentTimeMillis();

		XMLTransmitter transmitter = XMLTransmitter.getTransmitter();

        transmitter.marshal(dmessages, new File(AbstractTest.BASE_DIR, FilenameUtils.concat("testdata", "dataMessages.xml")));

		long duration = System.currentTimeMillis() - begin;

		System.out.println("serialization duration = " + duration);

		Assert.assertEquals((duration - 1000) < 0 , true);
	}

	@Test
	public void dbStoring() throws Exception {

		DataMessagePersister persister = new DBPersister();

		IDictionaryStructure dictionary =  createNative108();

		IMessage imessage = createSimpleMessage(dictionary);

		DataMessage dataMessage = DataMessageConverter.createDataMessage(imessage, dictionary);

		persister.persist(dataMessage);
	}

	@Ignore //TODO: Check and fix later
    @Test
	public void dbRetrieve() throws Exception {
		DBPersister persister = new DBPersister();

		FilterCriterion crirerion = new FilterCriterion(XmlDataMessage.RAW_DATA, "0", FilterCriterion.Operation.LIKE);
		List<FilterCriterion> filterCriterions = new LinkedList<FilterCriterion>();
		filterCriterions.add(crirerion);
		DataMessage dataMessage = persister.getDataMessage(filterCriterions, null);

		Assert.assertNotNull(dataMessage);
		Assert.assertNotNull(dataMessage.getName());
	}
}