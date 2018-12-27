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
package com.exactpro.sf.services.tcpip;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.AbstractServiceSettings;
import com.exactpro.sf.services.RequiredParam;
import org.apache.commons.configuration.HierarchicalConfiguration;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TCPIPSettingsServer extends AbstractServiceSettings
{
    private static final long serialVersionUID = 806869687825350122L;

    @RequiredParam
	@Description("Host name or ip address of the remote server")
	private String host;

	@RequiredParam
	@Description("Listening port of the remote server to connect")
	private int port;

	@RequiredParam
	@Description("Name of the codec class to encode and decode messages")
	private String codecClassName;

    private String fieldConverterClassName = "com.exactpro.sf.services.tcpip.DefaultFieldConverter";

	@Description("Should service store messages to DB or not")
	private boolean storeMessages;

	@Description("Positive inactivity period of time (in minues)\n+" +
			"since last messages was sent after which service \n" +
			"should be disposed. \n" +
			"If 0 than service should not be disposed.")
	private int idleTimeout;

	private SailfishURI dictionaryName;

	@Description("Tag's list unexpected messages. Example: 35=a,48=B|34=4")
	private String unexpectedMessages;

	@Description("If TRUE, then the message structures <br> for encoding are taken from the dictionary, <br> otherwise message structures are taken from the current matrix <br> for encoding of all fields occur only into the message body.")
	private boolean encodeByDictionary = false;

	@Description("If TRUE, then the message structures <br> for decoding are taken from the dictionary, <br> otherwise message structures are taken from the current matrix <br> for decoding of all fields occur only into the message body.")
	private boolean decodeByDictionary = false;

	@Description("If TRUE, then the trailing zeros of decimal values<br> will be removed by dictionary during decoding.<br>This option will be enable only if <br>'decode by dictionary' is TRUE.")
	private boolean removeTrailingZeros = false;

	public boolean isStoreMessages() {
		return storeMessages;
	}

	public void setStoreMessages(boolean storeMessages) {
		this.storeMessages = storeMessages;
	}


	public String getCodecClassName() {
		return codecClassName;
	}


	public void setCodecClassName(String codecClassName) {
		this.codecClassName = codecClassName;
	}


	public final String getHost() {
		return host;
	}


	public final void setHost(String host) {
		this.host = host;
	}


	public final int getPort() {
		return port;
	}


	public final void setPort(int port) {
		this.port = port;
	}

	public int getIdleTimeout() {
		return idleTimeout;
	}

	public void setIdleTimeout(int idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	@Override
	public void load(HierarchicalConfiguration config)
	{
		this.host = config.getString( "host" );
		this.port = config.getInt( "port" );
		this.codecClassName = config.getString( "codecClass" );
		this.storeMessages = config.getBoolean( "storeMessages" );
	}


	@Override
	public SailfishURI getDictionaryName() {
		return dictionaryName;
	}

	@Override
	public void setDictionaryName(SailfishURI dictionaryName) {
		this.dictionaryName = dictionaryName;
	}

	public String getUnexpectedMessages() {
		return unexpectedMessages;
	}

	public void setUnexpectedMessages(String unexpectedMessages) {
		this.unexpectedMessages = unexpectedMessages;
	}

	public String getFieldConverterClassName() {
		return fieldConverterClassName;
	}

	public void setFieldConverterClassName(String fieldConverterClassName) {
		this.fieldConverterClassName = fieldConverterClassName;
	}

    public boolean isDecodeByDictionary() {
		return decodeByDictionary;
	}

	public void setDecodeByDictionary(boolean decodeByDictionary) {
		this.decodeByDictionary = decodeByDictionary;
	}

	public boolean isEncodeByDictionary() {
		return encodeByDictionary;
	}

	public void setEncodeByDictionary(boolean encodeByDictionary) {
		this.encodeByDictionary = encodeByDictionary;
	}

	public boolean isRemoveTrailingZeros() {
		return removeTrailingZeros;
	}

	public void setRemoveTrailingZeros(boolean removeTrailingZeros) {
		this.removeTrailingZeros = removeTrailingZeros;
	}
}

