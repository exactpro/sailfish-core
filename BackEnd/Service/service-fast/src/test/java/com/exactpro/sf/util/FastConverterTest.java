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
package com.exactpro.sf.util;

import java.io.IOException;
import java.io.InputStream;

import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.SequenceValue;
import org.openfast.template.BasicTemplateRegistry;
import org.openfast.template.Group;
import org.openfast.template.MessageTemplate;
import org.openfast.template.Sequence;
import org.openfast.template.loader.MessageTemplateLoader;
import org.openfast.template.loader.XMLMessageTemplateLoader;

import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;

public class FastConverterTest {

	protected static BasicTemplateRegistry initRegistry(String templateName){
		InputStream templateInputStream = FastConverterTest.class.getClassLoader().
				getResourceAsStream(templateName);
		MessageTemplateLoader templateLoader = new XMLMessageTemplateLoader();
		MessageTemplate[] templates = templateLoader.load(templateInputStream);
		BasicTemplateRegistry registry = new BasicTemplateRegistry();  
		for(MessageTemplate template: templates) {
			template.getId();
			registry.register(Integer.valueOf(template.getId()), template);
		}		
		return registry;
	}
	
	protected static IDictionaryStructure initDictionary(String dictionaryName) throws IOException {
		try (InputStream in = FastConverterTest.class.getClassLoader().getResourceAsStream(dictionaryName)) {
			return new XmlDictionaryStructureLoader().load(in);
    	}
	}
	
	protected static Message initMessage(BasicTemplateRegistry registry, String templateName){
		Message fastMessage = new Message(registry.get(templateName));
		fastMessage.setString("stringx", "strVal");
		fastMessage.setString("int8", "32");
		fastMessage.setString("int32", "33");
		fastMessage.setString("uInt32", "34");
		fastMessage.setString("int64", "35");
		fastMessage.setString("uInt64", "36");
		fastMessage.setDecimal("decimal", 37);
		fastMessage.setByteVector("bv", new byte[] {20,30,40});
		Group grp = fastMessage.getTemplate().getGroup("grp");
		GroupValue grpVal = new GroupValue(grp);
		grpVal.setString("stringx", "strVal1");
		fastMessage.setFieldValue("grp", grpVal);
		
		Sequence sqs = fastMessage.getTemplate().getSequence("sqs");
		SequenceValue sqsVal = new SequenceValue(sqs);
		grp = sqs.getGroup();
		grpVal = new GroupValue(grp);
		grpVal.setString("sqsv", "strVal2");
		sqsVal.add(grpVal);
		grpVal = new GroupValue(grp);
		grpVal.setString("sqsv", "strVal3");
		sqsVal.add(grpVal);
		fastMessage.setFieldValue(sqs, sqsVal);
		
		sqs = fastMessage.getTemplate().getSequence("seq");
		sqsVal = new SequenceValue(sqs);
		grp = sqs.getGroup();
		grpVal = new GroupValue(grp);
		grpVal.setString("seqv", "strVal2");
		sqsVal.add(grpVal);
		grpVal = new GroupValue(grp);
		grpVal.setString("seqv", "strVal3");
		sqsVal.add(grpVal);
		fastMessage.setFieldValue(sqs, sqsVal);
		
		return fastMessage;
	}
	
	protected boolean isEqual(byte[] firstArray, byte[] secondArray){
		if(firstArray.length!=secondArray.length)
			return false;
		for(int i=0;i<firstArray.length;i++)
			if(firstArray[i]!=secondArray[i])
				return false;
		return true;
		
	}
	
}
