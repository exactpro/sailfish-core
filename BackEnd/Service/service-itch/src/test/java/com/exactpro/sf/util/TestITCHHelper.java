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

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.MockProtocolDecoderOutput;
import com.exactpro.sf.services.MockProtocolEncoderOutput;
import com.exactpro.sf.services.itch.ITCHCodec;
import com.exactpro.sf.services.itch.ITCHCodecSettings;
import com.exactpro.sf.services.itch.ITCHMessageHelper;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.AbstractProtocolEncoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.junit.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class TestITCHHelper extends AbstractTest {
	
	protected static final String SAILFISH_DICTIONARY_PATH = "cfg" + File.separator + "dictionaries" + File.separator;

    private MessageHelper messageHelper = new ITCHMessageHelper();
	private String namespace;
	private IoSession session = new DummySession();
	private static IMessageFactory msgFactory = DefaultMessageFactory.getFactory();
    private TestITCHMessageCreator messageCreator;

    protected TestITCHHelper() {
        IDictionaryStructure dictionary = serviceContext.getDictionaryManager().createMessageDictionary(SAILFISH_DICTIONARY_PATH + "itch.xml");
        this.namespace = dictionary.getNamespace();
        this.messageHelper.init(msgFactory, dictionary);
        this.messageCreator = new TestITCHMessageCreator(msgFactory, namespace, (ITCHMessageHelper) messageHelper);
    }

    protected TestITCHHelper(IDictionaryStructure dictionary) {
		this.namespace = dictionary.getNamespace();
        this.messageHelper.init(msgFactory, dictionary);
	}
	
	protected MessageHelper getMessageHelper(){
		return this.messageHelper;
	}
	
	protected IoSession getSession(){
		return this.session;
	}
	
	protected IMessage getUnitHeader(short messageCount){
		IMessage messageHeader = msgFactory.createMessage("UnitHeader", namespace);
        messageHeader.addField("Length", 60);
        messageHeader.addField("MessageCount", messageCount);
        messageHeader.addField("MarketDataGroup", "M");
        messageHeader.addField("SequenceNumber", 1L);
        return messageHeader;
	}

    protected TestITCHMessageCreator getMessageCreator() {
		return this.messageCreator;
	}
	
	protected static IDictionaryStructure getDictionaryWithDublicateMessages() throws IOException{
        return loadDictionary("itch_DublicateMessageTypeValue.xml");
	}
	
	protected static IDictionaryStructure getAdditionalDictionary() throws IOException{
        return loadDictionary("itch_additional.xml");
	}
	
	protected static IDictionaryStructure getInvalidDictionary() throws IOException{
        return loadDictionary("itch_invalid.xml");
	}
	
	protected static IDictionaryStructure getInvalidLengthDictionary() throws IOException{
        return loadDictionary("itch_invalid_length.xml");
	}
	
	protected static IDictionaryStructure getDictionary() throws IOException{
        return loadDictionary("itch.xml");
    }

    protected Object encode(IMessage message, ITCHCodec codec) throws Exception {
		if(codec==null)
            codec = (ITCHCodec) this.messageHelper.getCodec(serviceContext);
		ProtocolEncoderOutput output = new MockProtocolEncoderOutput();
		session.write(message);
		codec.encode(session, message, output);
		Queue<Object> msgQueue = ((AbstractProtocolEncoderOutput)output).getMessageQueue();
		Assert.assertNotNull("Message queue from AbstractProtocolEncoderOutput.", msgQueue);
		Assert.assertTrue("Message queue size must be equal 1.", 1 == msgQueue.size());
		Object lastMessage = msgQueue.element();
		Assert.assertNotNull(lastMessage);
		return lastMessage;
	}

    protected IMessage decode(Object lastMessage, ITCHCodec codec) throws Exception {
		if(codec==null)
            codec = (ITCHCodec) this.messageHelper.getCodec(serviceContext);
		MockProtocolDecoderOutput decoderOutput = new MockProtocolDecoderOutput();
		IoSession decodeSession = new DummySession();
		IoBuffer toDecode = IoBuffer.wrap( ((IoBuffer)lastMessage).array() );
		boolean decodeResult = codec.doDecode( decodeSession, toDecode, decoderOutput );
		    
		Assert.assertTrue("Decoding error.", decodeResult);
		Assert.assertTrue( "Message queue size must not less then 1.", 1 <= decoderOutput.getMessageQueue().size());

		return (IMessage) decoderOutput.getMessageQueue().element();
	}

    protected static ITCHCodec getCodecWithAdditionalDictionary() throws IOException {
        ITCHCodec codec = new ITCHCodec();
        ITCHCodecSettings settings = new ITCHCodecSettings();
        settings = new ITCHCodecSettings();
        settings.setMsgLength(1);
        codec.init(serviceContext, settings,msgFactory, getAdditionalDictionary());
        return codec;
	}

    protected static ITCHCodec getCodec(MessageHelper helper) throws IOException {
        ITCHCodec codec = new ITCHCodec();
        ITCHCodecSettings settings = new ITCHCodecSettings();
        IDictionaryStructure dictionary = helper.getDictionaryStructure();
        Integer msgLength = ObjectUtils.defaultIfNull(ITCHMessageHelper.extractLengthSize(dictionary), 1);
        settings.setMsgLength(msgLength);
        settings.setDictionaryURI(SailfishURI.unsafeParse(dictionary.getNamespace()));
        codec.init(serviceContext, settings, msgFactory, dictionary);
        return codec;
	}

    protected static ITCHCodec getCodecWithInvalidDictionary() throws IOException {
        ITCHCodec codec = new ITCHCodec();
        ITCHCodecSettings settings = new ITCHCodecSettings();
        settings = new ITCHCodecSettings();
        settings.setMsgLength(1);
        codec.init(serviceContext, settings,msgFactory, getInvalidDictionary());
        return codec;
	}

    protected static ITCHCodec getCodecWithInvalidLengthDictionary() throws IOException {
        ITCHCodec codec = new ITCHCodec();
        ITCHCodecSettings settings = new ITCHCodecSettings();
        settings = new ITCHCodecSettings();
        settings.setMsgLength(1);
        codec.init(serviceContext, settings,msgFactory, getInvalidLengthDictionary());
        return codec;
	}
	
	protected void compareFieldsValues(IMessage result, IMessage original, String type, Class<?> c){
		String t= String.format("Original value of %s field with Type=%s: "+original.getField(type)+"; \n"+
				"Result value of %s field with Type=%s: "+result.getField(type), c.getSimpleName(),type,c.getSimpleName(),type);
		if("STUB".equals(type)){
			Assert.assertTrue(t, result.getField(type)==null);
		}else{
			Assert.assertTrue(t, original.getField(type).equals(result.getField(type)) || 
					original.getField(type).toString().equals(result.getField(type).toString()));
		}		
	}

    protected void testNegativeEncode(IMessage message, ITCHCodec codec) throws Exception {
		@SuppressWarnings("unchecked")
        IMessage result = ((List<IMessage>) message.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME)).get(1);
		Set<String> names=result.getFieldNames();
		if(names.size()!=1)
			Assert.fail("Too many fields");
		String fieldName=names.iterator().next();
		Object fieldValue=result.getField(fieldName);
		String errorExpected="Travers problem for FieldName = "+fieldName+", FieldValue = "+fieldValue+"."
    			+ " in MessageStructure Name";
		try{
        	encode(message,codec);
        	Assert.fail("There is no exception was threw");
        }catch(EPSCommonException e){
        	Assert.assertTrue("Expected: "+errorExpected+"; Actual: "+e.getMessage(),e.getMessage().startsWith(errorExpected));
        }        
	}

    protected void testNegativeDecode(IMessage message, ITCHCodec codec, ITCHCodec codecValid, String type) throws Exception {
		String enumType=type.toUpperCase();
		String errorExpected="Incorrect type = "+enumType+" for "+type+" field. in field name = ["+type+"]. in MessageStructure Name";
		try{
        	Object o=encode(message,codecValid);
        	decode(o,codec);
        	Assert.fail("There is no exception was threw");
        }catch(EPSCommonException e){
        	Assert.assertTrue("Expected: "+errorExpected+"; Actual: "+e.getMessage(),e.getMessage().startsWith(errorExpected));
        }
	}
	
	protected void testFieldNames(IDictionaryStructure dictionary, List<String> expectedNames){
		List<String> names = new ArrayList<>();
		for ( IFieldStructure fieldType : dictionary.getFieldStructures() ){
			names.add(fieldType.getName());
		}
		for (String name : expectedNames){
			Assert.assertTrue("There is no field ["+name+"] in dictionary", names.contains(name));
		}
	}
	
	protected void testMessageNames(IDictionaryStructure dictionary, List<String> expectedNames){
		List<String> names = new ArrayList<>();
		for ( IMessageStructure msgStruct : dictionary.getMessageStructures() ){
			names.add(msgStruct.getName());
		}
		for (String name : expectedNames){
			Assert.assertTrue("There is no field ["+name+"] in dictionary", names.contains(name));
		}
	}
	
	private static IDictionaryStructure loadDictionary(String fileName) throws IOException {
	    IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();
        String fileFQN = BASE_DIR + File.separator + "src" + File.separator  + "test" + File.separator + 
        		"workspace" + File.separator + "cfg" + File.separator + "dictionaries" + File.separator + fileName;
        try (InputStream inputStream = new FileInputStream( fileFQN )) {
        	IDictionaryStructure dictionary = loader.load(inputStream);
            return dictionary;
        }
	}
}
