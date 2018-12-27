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
package com.exactpro.sf.storage.xml;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.impl.messages.MapMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MessageStructureReader;
import com.exactpro.sf.common.messages.MessageStructureReaderHandlerImpl;
import com.exactpro.sf.common.messages.MessageStructureWriter;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;

public class DataMessageConverter {
	
	public static Logger logger = LoggerFactory.getLogger(DataMessageConverter.class);
	
	public static MapMessage createMapMessage(DataMessage message, IDictionaryStructure dataDicionary){
		
		MapMessage mapMessage = new MapMessage(message.getNamespace(), message.getName());
		IMessageStructure messageStructure = dataDicionary.getMessageStructure(message.getName());
		
		MessageStructureWriter wtraverser = new MessageStructureWriter();
		wtraverser.traverse(new DataMessageReaderVisitor(message, mapMessage), messageStructure);
		
		return mapMessage;
	}
	
	public static DataMessage createDataMessage(IMessage imessage, IDictionaryStructure dataDictionary){
		
		IMessageStructure messageStructure = dataDictionary.getMessageStructure(imessage.getName());
		DataMessage dataMessage = new DataMessage();
		dataMessage.setName(imessage.getName());
		dataMessage.setNamespace(imessage.getNamespace());
		
		MessageStructureReader rtrarverser = new MessageStructureReader();
		rtrarverser.traverse(new DataMessageWriterVisitor(dataMessage), messageStructure, imessage, MessageStructureReaderHandlerImpl.instance());
		
		return dataMessage;
	}
	
}
