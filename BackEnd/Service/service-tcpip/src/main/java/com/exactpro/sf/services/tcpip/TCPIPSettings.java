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

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.configuration.HierarchicalConfiguration;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.services.RequiredParam;
import com.exactpro.sf.services.mina.AbstractMINASettings;

@XmlRootElement
public class TCPIPSettings extends AbstractMINASettings
{
    private static final long serialVersionUID = -2095634003005468220L;

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

	@Description("Should service store messages to DB or not." +
            "Note: this setting is deprecated; please use 'Persist messages' instead")
    @Deprecated
	private boolean storeMessages;

	@Description("Positive inactivity period of time (in minues)\n+" +
			"since last messages was sent after which service \n" +
			"should be disposed. \n" +
			"If 0 than service should not be disposed.")
	private int idleTimeout;

	@Description("Connect on start automatically")
	private boolean autoConnect = false;

	@Description("Pipe-separated list of markers. Used to filter incoming raw message and mark it as 'admin'.<br>"
	        + "This functionality is used as workaround for for \"Unexpected message\" check."
	        + "Example: 35=a,48=B|34=4")
	private String unexpectedMessages;

	@Description("If TRUE, then the message structures <br> for decoding are taken from the dictionary, <br> otherwise message structures are taken from the current matrix <br> for decoding of all fields occur only into the message body.")
	private boolean decodeByDictionary = false;

	@Description("If TRUE, then the trailing zeros of decimal values<br> will be removed by dictionary during decoding.<br>This option will be enable only if <br>'decode by dictionary' is TRUE.")
	private boolean removeTrailingZeros = false;

    @Description("Receive limit in bytes to emulate Slow Consumer")
    private int receiveLimit = 0;

    @Description("Enables SSL usage for QFJ acceptor or initiator")
    private boolean useSSL = false;
    @Description("Controls which particular protocols for secure connection are enabled for handshake. Use SSL(older) or TLS")
    private String sslProtocol = null;

    @Description("KeyStore to use with SSL")
    private String sslKeyStore;
    @Description("KeyStore password to use with SSL")
    private String sslKeyStorePassword;
    @Description("Type of specified keystore. Can be JKS, JCEKS, PKCS12, PKCS11")
    private String keyStoreType;
	@Description("Forcefully change name of all incoming messages to 'Incoming' and handle them as business(application) message.")
    private boolean depersonalizationIncomingMessages = true;
    @Description("Set of pairs 'tag:values' for which allow receive messages. <br>" +
                 "Tag:values pair must be delimited by ':'<br>" +
                 "Pairs must be delimited by ';' and values must be delimited by ','<br>" +
                 "If empty then all received message will be stored.<br>" +
                 " Example: 35:D,A; 34:50,100")
    private String filterMessages;

	public boolean isStoreMessages() {
		return storeMessages;
	}

	public void setStoreMessages(boolean storeMessages) {
		this.storeMessages = storeMessages;
		setPersistMessages(storeMessages);
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

    public boolean isAutoConnect() {
        return autoConnect;
    }

    public void setAutoConnect(boolean autoConnect) {
        this.autoConnect = autoConnect;
    }

    public boolean isDecodeByDictionary() {
		return decodeByDictionary;
	}

	public void setDecodeByDictionary(boolean decodeByDictionary) {
		this.decodeByDictionary = decodeByDictionary;
	}

	public boolean isRemoveTrailingZeros() {
		return removeTrailingZeros;
	}

	public void setRemoveTrailingZeros(boolean removeTrailingZeros) {
		this.removeTrailingZeros = removeTrailingZeros;
	}

    public int getReceiveLimit() {
        return receiveLimit;
    }

    public void setReceiveLimit(int receiveLimit) {
        this.receiveLimit = receiveLimit;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    public String getSslProtocol() {
        return sslProtocol;
    }

    public void setSslProtocol(String sslProtocol) {
        this.sslProtocol = sslProtocol;
    }

    public String getSslKeyStore() {
        return sslKeyStore;
    }

    public void setSslKeyStore(String sslKeyStore) {
        this.sslKeyStore = sslKeyStore;
    }

    public String getSslKeyStorePassword() {
        return sslKeyStorePassword;
    }

    public void setSslKeyStorePassword(String sslKeyStorePassword) {
        this.sslKeyStorePassword = sslKeyStorePassword;
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }

    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

    public ICommonSettings createCodecSettings() {
        return this;
    }

    public boolean isDepersonalizationIncomingMessages() {
        return depersonalizationIncomingMessages;
    }

    public void setDepersonalizationIncomingMessages(boolean depersonalizationIncomingMessages) {
        this.depersonalizationIncomingMessages = depersonalizationIncomingMessages;
    }

    public String getFilterMessages() {
        return filterMessages;
    }

    public void setFilterMessages(String filterMessages) {
        this.filterMessages = filterMessages;
    }
}
