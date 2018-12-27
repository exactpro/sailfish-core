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
package com.exactpro.sf.services.fast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.openfast.Context;
import org.openfast.IntegerValue;
import org.openfast.Message;
import org.openfast.MessageOutputStream;
import org.openfast.template.TemplateRegistry;
import org.openfast.template.loader.XMLMessageTemplateLoader;
import org.openfast.template.type.codec.TypeCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.connectivity.mina.net.IoBufferWithAddress;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.fast.converter.ConverterException;
import com.exactpro.sf.services.fast.converter.FastToIMessageConverter;
import com.exactpro.sf.services.fast.converter.IMessageToFastConverter;
import com.exactpro.sf.services.fast.fixup.EofCheckedStream;

public class FASTCodec extends AbstractCodec {

	private static final String INPUT_CONTEXT_ATTR_NAME = "FAST_INPUT_CONTEXT";
	private static final String OUTPUT_CONTEXT_ATTR_NAME = "FAST_OUTPUT_CONTEXT";

	static final Logger logger = LoggerFactory.getLogger(FASTCodec.class);

	private IServiceContext serviceContext;
	private IDictionaryStructure msgDictionary;
	private IMessageFactory msgFactory;
	private FastToIMessageConverter converter;

	private FASTCodecSettings settings;

	private TemplateRegistry registry;

	private IMessageToFastConverter iMsgToFastConverter;

	@Override
	public void init(
			IServiceContext serviceContext,
			ICommonSettings settings,
			IMessageFactory msgFactory, IDictionaryStructure dictionary) {

	    this.serviceContext = Objects.requireNonNull(serviceContext, "'Service context' parameter cannot be null");
		this.msgDictionary = Objects.requireNonNull(dictionary, "'dictionary' parameter cannot be null");
		String fastTemplate = Objects.requireNonNull((String) dictionary.getAttributeValueByName(FASTMessageHelper.TEMPLATE_ATTRIBYTE), "'Template attribute' parameter");
		this.msgFactory = msgFactory;

		if ( settings != null )
		{
			this.settings = (FASTCodecSettings) settings;
		}

        SailfishURI dictionaryName = Objects.requireNonNull(this.settings.getDictionaryName(), "'Dictionary name' parameter");
        IDataManager dataManager = Objects.requireNonNull(this.serviceContext.getDataManager(), "'Data manager' parameter");

        loadFastTemplates(dataManager, dictionaryName.getPluginAlias(), fastTemplate);

		createConverter();
	}


	private void createConverter() {
		if (this.converter == null) {
			FastToIMessageConverter converter = new FastToIMessageConverter(
					msgFactory,
					msgDictionary.getNamespace()
			);
			this.converter = converter;
		}
	}

	private void loadFastTemplates(final IDataManager dataManager, String pluginAlias, String templateName) {
		XMLMessageTemplateLoader loader = new XMLMessageTemplateLoader();
		loader.setLoadTemplateIdFromAuxId(true);

		try (InputStream templateStream = dataManager.getDataInputStream(pluginAlias, FASTMessageHelper.getTemplatePath(templateName))) {
			loader.load(templateStream);
		} catch (IOException e) {
			logger.warn("Can not read template {} from resources", templateName, e);
			throw new EPSCommonException("Can not read template " + templateName + " from resources", e);
		}

        setRegistry(loader.getTemplateRegistry());
	}

	private void setRegistry(TemplateRegistry registry) {
		this.registry = registry;
	}


	private TemplateRegistry getRegistry() {
		return registry;
	}

	protected Context createFastContext() {
		Context context = new FASTContext();

		context.setTemplateRegistry(getRegistry());
		context.setTraceEnabled(true);
		context.setDecodeTrace(new LoggingTrace());
		return context;
	}

	protected FASTCodecSettings getSettings() {
		return settings;
	}

	@Override
	protected boolean doDecode(
			IoSession session,
			IoBuffer in,
			ProtocolDecoderOutput out) throws Exception {

		if (!in.hasRemaining()) {
			return false;
		}
		int position = in.position();
		try {
			if (doRealDecode(session, in, out)) {
				return true;
			}
		} catch (Exception e) {
			logger.error("Can not decode message", e);
		}
		in.position(position);
		return false;
	}

	@Override
	public void decode(IoSession session, IoBuffer in,
			ProtocolDecoderOutput out) throws Exception {
		if (!session.getTransportMetadata().hasFragmentation()) {
			if (settings.isResetContextAfterEachUdpPacket())
			getInputContext(session).reset();
		}
		super.decode(session, in, out);
	}

	private boolean doRealDecode(
			IoSession session,
			IoBuffer in,
			ProtocolDecoderOutput out) throws IOException, ConverterException {
		int position = in.position();
		byte data [] = new byte[in.remaining()];
		in.get(data);

        try(InputStream is = new EofCheckedStream(new ByteArrayInputStream(data))) {
            int msgLen = (TypeCodec.UINT.decode(is)).toInt();
            if(is.available() < msgLen) {
                return false;
            }
            FASTMessageInputStream msgStream = new FASTMessageInputStream(is, getInputContext(session));
            Message msg = msgStream.readMessage(settings.getSkipInitialByteAmount());

            IMessage imsg = converter.convert(msg);

            int available = is.available();
            int size = data.length - available;
            byte[] rawMessage = Arrays.copyOf(data, size);
            position += size;
            in.position(position);

            fillMessageMetadata(session, in, imsg, rawMessage);

            out.write(imsg);
		}

		return true;
	}


	private void fillMessageMetadata(
			IoSession session,
			IoBuffer in,
			IMessage imsg,
			byte[] rawMessage) {
		IMessageStructure msgStructure = msgDictionary.getMessageStructure(imsg.getName());
		Boolean isAdmin = (Boolean)msgStructure.getAttributeValueByName("IsAdmin");
		if (isAdmin == null) {
			isAdmin = false;
		}

		MsgMetaData metaData = imsg.getMetaData();
		metaData.setAdmin(isAdmin);
		//metaData.setToService(this.serviceName);

		String packetAddress;
		if (in instanceof IoBufferWithAddress) {
			packetAddress = ((IoBufferWithAddress)in).getAddress();
		} else {
			packetAddress = session.getRemoteAddress().toString();
		}

		metaData.setFromService(packetAddress);

		metaData.setRawMessage(rawMessage);
	}


	private Context getInputContext(IoSession session) {
		Context inputContext = (Context) session.getAttribute(INPUT_CONTEXT_ATTR_NAME);
		if (inputContext == null) {
			inputContext = createFastContext();
			session.setAttribute(INPUT_CONTEXT_ATTR_NAME, inputContext);
		}
		return inputContext;
	}

	private Context getOutputContext(IoSession session) {
		Context outputContext = (Context) session.getAttribute(OUTPUT_CONTEXT_ATTR_NAME);
		if (outputContext == null) {
			outputContext = createFastContext();
			session.setAttribute(OUTPUT_CONTEXT_ATTR_NAME, outputContext);
		}
		return outputContext;
	}

	@Override
	public void encode(
			IoSession session,
			Object message,
			ProtocolEncoderOutput out) throws Exception {

		IMessageToFastConverter converter = getIMessageToFastConverter();
		IMessage iMsg = (IMessage) message;
		Message fastMsg;

		fastMsg = converter.convert(iMsg);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Context oc = getOutputContext(session);
		MessageOutputStream msgOutStream = new MessageOutputStream(os, oc);
		msgOutStream.writeMessage(fastMsg);
		byte[] outputdata = os.toByteArray();
		byte[] blockHeader = TypeCodec.UINT.encode(new IntegerValue(outputdata.length));

		byte[] result = Arrays.copyOf(blockHeader, blockHeader.length + outputdata.length);
		System.arraycopy(outputdata, 0, result, blockHeader.length, outputdata.length);

		out.write(IoBuffer.wrap(result));
		if (settings.isResetContextAfterEachUdpPacket()) {
			oc.reset();
		}
	}

	private IMessageToFastConverter getIMessageToFastConverter() {
		if (iMsgToFastConverter == null) {
			IDictionaryStructure dictionary = this.msgDictionary;
			iMsgToFastConverter = new IMessageToFastConverter(dictionary, getRegistry());
		}
		return iMsgToFastConverter;
	}

}
