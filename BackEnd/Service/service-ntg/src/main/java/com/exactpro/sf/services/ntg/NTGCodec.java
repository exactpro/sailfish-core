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

import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MessageStructureReader;
import com.exactpro.sf.common.messages.MessageStructureReaderHandlerImpl;
import com.exactpro.sf.common.messages.MessageStructureWriter;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.HexDumper;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.ServiceException;
import com.exactpro.sf.services.mina.MINAUtil;
import com.exactpro.sf.services.ntg.exceptions.UndefinedMessageException;

public final class NTGCodec extends AbstractCodec {
    private static final Logger logger = LoggerFactory.getLogger(NTGCodec.class);

	// Length of the MessageHeader and of Heartbeat message.
	protected static final int MINIMAL_CAPACITY = 4 ;

    // Predefined value of the first byte of  NTG message.
	protected final static byte CLIENT_START_OF_MESSAGE_INDICATOR  = 2;

	// NOTE: this value must be qualified in production
	protected final static byte SERVER_START_OF_MESSAGE_INDICATOR  = 1;

	// FIXME: SessionAttribute.RESTORE may work incorrect with this attribute
	private static final String ATTRIBUTE_IS_INPUT_MESSAGE = "IsInput";
	private static final String ATTRIBUTE_MESSAGE_TYPE = "MessageType";

	private IMessageFactory msgFactory = null;
	private IDictionaryStructure dictionary = null;
	private static int parsedMessagesCount = 0;

	private MessageStructureReader messageStructureReader = null;
	private MessageStructureWriter messageStructureWriter = null;

	private Map<Byte, IMessageStructure> decodeMsgTypeToStructure = new HashMap<>();
	private Map<Long, Integer> msgIdents = new HashMap<>();

    public NTGCodec() {
		// default constructor
	}



	@Override
	public void init(IServiceContext serviceContext, ICommonSettings settings, IMessageFactory msgFactory, IDictionaryStructure dictionary)
	{

		if (null == msgFactory) {
			throw new IllegalArgumentException("Parameter [msgFactory] could not be null");
		}

		if (null == dictionary) {
			throw new IllegalArgumentException("Parameter [dictionary] could not be null");
		}

		this.msgFactory = msgFactory;
		this.dictionary = dictionary;

		Map<Byte, IMessageStructure> outputMap = new HashMap<>();
		Map<Byte, IMessageStructure> inputMap = new HashMap<>();

		Map<Byte, IMessageStructure> map = null;
		IMessageStructure curStructure = null;

		for ( IMessageStructure msgStruct : dictionary.getMessageStructures() )
		{
			Byte msgType = (Byte)msgStruct.getAttributeValueByName(ATTRIBUTE_MESSAGE_TYPE);

			if (msgType != null) {

			    Boolean isInput = (Boolean)msgStruct.getAttributeValueByName(ATTRIBUTE_IS_INPUT_MESSAGE);

			    map = Boolean.TRUE.equals(isInput) ? inputMap : outputMap;

			    curStructure = map.put(msgType, msgStruct);

			    if (curStructure != null) {
                    throw new ServiceException(String.format("[MessageType] attribute must be unique for input / output sets. " + "MessageNames: [%s, %s], message type [%d]",
                            curStructure.getName(), msgStruct.getName(), msgType));
                }
			}
		}

		this.decodeMsgTypeToStructure = outputMap;
		this.decodeMsgTypeToStructure.putAll(inputMap);

		for (Entry<Byte, IMessageStructure> entry : decodeMsgTypeToStructure.entrySet()) {

            int msgLength = 0;

            for (IFieldStructure fldStruct : entry.getValue().getFields()) {
                if (fldStruct.getAttributeValueByName(NTGProtocolAttribute.Length.toString()) == null) {
                    throw new ServiceException("Attribute [" + NTGProtocolAttribute.Length.toString() + "] missed in definition field "
                            + fldStruct.getName() + " in message " + entry.getValue().getName());
                }
                int fldLength = (Integer) fldStruct.getAttributeValueByName(NTGProtocolAttribute.Length.toString());

                msgLength += fldLength;
            }

            long iden = entry.getKey();

            iden <<= 24;

            iden |= ((msgLength - 3) << 8);

            iden |= 2;

            this.msgIdents.put(iden, msgLength - 3);
        }

		this.messageStructureReader = new MessageStructureReader();
		this.messageStructureWriter = new MessageStructureWriter();

	}


	public boolean decodable(IoSession session, IoBuffer inputBuffer)
	{
		inputBuffer.order(ByteOrder.LITTLE_ENDIAN);

		short messageLength = 0;
		boolean isDecodable = false;
		byte messageType = -1;

		if( inputBuffer.remaining() < MINIMAL_CAPACITY ) {
			return false;
		}

		inputBuffer.order(ByteOrder.LITTLE_ENDIAN);
		inputBuffer.mark();
		byte messageStartByte  = inputBuffer.get();

		// Implementation of SEVRER_START_OF_MESSAGE_INDICATOR need to be qualified.
		if( CLIENT_START_OF_MESSAGE_INDICATOR != messageStartByte
		/* && SEVRER_START_OF_MESSAGE_INDICATOR != messageStartByte */ )
		{
            inputBuffer.reset();

            logger.error("Unexpected start of message: {} (expected: {})", messageStartByte, CLIENT_START_OF_MESSAGE_INDICATOR);
            logger.error("Buffer hexdump:{}{}", System.lineSeparator(), HexDumper.getHexdump(inputBuffer, inputBuffer.remaining()));

            throw new EPSCommonException("Unexpected start of message: " + messageStartByte);
		}

		messageLength = inputBuffer.getShort();

		if( messageLength < 0 )
		{
			throw new EPSCommonException( "Message length cannot be negative." );
		}

		if( inputBuffer.remaining() >= messageLength )
		{
			isDecodable = true;
		}

		messageType = inputBuffer.get();
		inputBuffer.reset();

		if(logger.isDebugEnabled())
		{
			messageLength += messageLength == 0 ? 0 : 3;
			logger.debug("decodable() result [{}].", isDecodable);
			logger.debug("decodable() message length [{}], message type [{}]", messageLength, messageType);
            //logger.debug( String.format(" decodable() as hex    [%s]", NTGUtility.getHexdump( inputBuffer, messageLength )));
			inputBuffer.reset();
			logger.debug(MINAUtil.getHexdumpAdv(inputBuffer, messageLength));
			inputBuffer.reset();
		}
		return isDecodable;
	}

	@Override
	public boolean doDecode(IoSession session, IoBuffer inputBuffer, ProtocolDecoderOutput pdOutput) throws Exception
	{
		if ( !decodable(session, inputBuffer) ) {
			return false;
		}

		boolean resultDecode = false;
		inputBuffer.order(ByteOrder.LITTLE_ENDIAN);
		inputBuffer.mark();
		inputBuffer.position(inputBuffer.markValue() + MINIMAL_CAPACITY - 1);
		byte messageType = inputBuffer.get();
		inputBuffer.reset();
		int startposition = inputBuffer.position();

		IMessageStructure msgStructure = this.decodeMsgTypeToStructure.get( messageType );

		if(null == msgStructure ) {
			throw new UndefinedMessageException("Message type ["+messageType+"] is not defined in the dictionary.");
		}

		IMessage msg = msgFactory.createMessage(msgStructure.getName(), msgStructure.getNamespace());
        NTGVisitorDecode visitorNTGDecode = new NTGVisitorDecode(inputBuffer, msgFactory, msg);
        this.messageStructureWriter.traverse(visitorNTGDecode, msgStructure);
        IMessage msgDecoded = visitorNTGDecode.getMessage();

		if ( null != msgDecoded )
		{
            pdOutput.write(visitorNTGDecode.getMessage());
			resultDecode = true;
		}
		else
		{
			resultDecode = false;
		}

		int sizeofmessage = inputBuffer.position();
		sizeofmessage -= startposition;
		byte[] rawMsg = new byte[sizeofmessage];
		System.arraycopy( inputBuffer.array(), startposition, rawMsg, 0, sizeofmessage );
		if( null != msgDecoded ) {
			msgDecoded.getMetaData().setRawMessage( rawMsg );
		}

		return resultDecode;
	}

	@Override
    public void encode(IoSession session, Object objMessage, ProtocolEncoderOutput peOutput)
	{
		if ( !(objMessage instanceof IMessage ) )
			throw new IllegalArgumentException("Message parameter is not instance of " + IMessage.class.getCanonicalName() );

		IMessage message = (IMessage)objMessage;

		logger.debug(" Encoding message [{}] from namespace [{}].", message.getName(), message.getNamespace());

        NTGVisitorEncode visitorNTG = new NTGVisitorEncode();

		IMessageStructure msgStructure = dictionary.getMessageStructure(message.getName());

		if (msgStructure == null) {
			throw new NullPointerException("MsgStructure is null. Namespace="
					+message.getNamespace()+", MsgName="+message.getName());
		}

        messageStructureReader.traverse(visitorNTG, msgStructure, message,
				MessageStructureReaderHandlerImpl.instance());

        byte[] rawMsg = new byte[visitorNTG.getBuffer().position()];

        System.arraycopy(visitorNTG.getBuffer().array(), 0, rawMsg, 0, visitorNTG.getBuffer().position());

		message.getMetaData().setRawMessage( rawMsg );

        peOutput.write(visitorNTG.getBuffer().flip());

		if(logger.isDebugEnabled())
		{
            logger.debug(" encode() as hex    [{}]", NTGUtility.getHexdump(visitorNTG.getBuffer(), visitorNTG.getAccumulatedLength()));
            logger.debug(MINAUtil.getHexdumpAdv(visitorNTG.getBuffer(), visitorNTG.getAccumulatedLength()));
		}
	}


	public static int getParsedMessagesCount()
	{
		return parsedMessagesCount;
	}

}