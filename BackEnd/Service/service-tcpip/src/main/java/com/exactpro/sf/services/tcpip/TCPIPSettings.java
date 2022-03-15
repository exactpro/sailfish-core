/******************************************************************************
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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
import com.exactpro.sf.aml.Ignore;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.services.RequiredParam;
import com.exactpro.sf.services.mina.AbstractMINASettings;
import org.apache.commons.configuration.HierarchicalConfiguration;

import javax.xml.bind.annotation.XmlRootElement;

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
	private boolean autoConnect;

	@Description("Pipe-separated list of markers. Used to filter incoming raw message and mark it as 'admin'.<br>"
	        + "This functionality is used as workaround for for \"Unexpected message\" check."
	        + "Example: 35=a,48=B|34=4")
	private String unexpectedMessages;

	@Description("If TRUE, then the message structures <br> "
            + "for decoding are taken from the dictionary, <br> "
            + "otherwise message structures are taken from the current matrix <br> "
            + "for decoding of all fields occur only into the message body.")
	private boolean decodeByDictionary;

    @Description("If the option is true, then the internal codec verifies structure of incoming/outgoing messages by dictionary.<br>"
            + "The verification feature requires the `decode by dictionary` to be enabled and the `depersonalization incoming messages` to be disabled.")
	private boolean verifyMessageStructure;

    @Description("Enable prevalidation of incoming messages. For example, for the fix protocol, prevalidation at the qfj level will be enabled.")
    private boolean preValidationMessage = true;

	@Description("If TRUE, then the trailing zeros of decimal values<br> will be removed by dictionary during decoding.<br>This option will be enable only if <br>'decode by dictionary' is TRUE.")
	private boolean removeTrailingZeros;

    @Description("Receive limit in bytes to emulate Slow Consumer")
    private int receiveLimit;

	@Description("Forcefully change name of all incoming messages to 'Incoming' and handle them as business(application) message.")
    private boolean depersonalizationIncomingMessages = true;
    @Description("Set of pairs 'tag:values' for which allow receive messages. <br>" +
                 "Tag:values pair must be delimited by ':'<br>" +
                 "Pairs must be delimited by ';' and values must be delimited by ','<br>" +
                 "If empty then all received message will be stored.<br>" +
                 " Example: 35:D,A; 34:50,100")
    private String filterMessages;
    @Description("Value to be used to field separation in the message upon decoding")
    private String fieldSeparator;

	@Description("Determines if milliseconds should be added to date-time / time fields during encoding")
	private boolean includeMilliseconds;

	@Description("Determines if microseconds should be added to date-time / time fields during encoding")
	private boolean includeMicroseconds;

	@Description("Determines if nanoseconds should be added to date-time / time fields during encoding")
	private boolean includeNanoseconds;

	@Ignore
    private boolean evolutionOptimize;

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

    public boolean isVerifyMessageStructure() {
        return verifyMessageStructure;
    }

    public void setVerifyMessageStructure(boolean verifyMessageStructure) {
        this.verifyMessageStructure = verifyMessageStructure;
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

    public String getFieldSeparator() {
        return fieldSeparator;
    }

    public void setFieldSeparator(String fieldSeparator) {
        this.fieldSeparator = fieldSeparator;
    }

	public boolean isIncludeMilliseconds() {
		return includeMilliseconds;
	}

	public void setIncludeMilliseconds(boolean includeMilliseconds) {
		this.includeMilliseconds = includeMilliseconds;
	}

	public boolean isIncludeMicroseconds() {
		return includeMicroseconds;
	}

	public void setIncludeMicroseconds(boolean includeMicroseconds) {
		this.includeMicroseconds = includeMicroseconds;
	}

	public boolean isIncludeNanoseconds() {
		return includeNanoseconds;
	}

	public void setIncludeNanoseconds(boolean includeNanoseconds) {
		this.includeNanoseconds = includeNanoseconds;
	}

    public boolean isEvolutionOptimize() {
        return evolutionOptimize;
    }

    public void setEvolutionOptimize(boolean evolutionOptimize) {
        this.evolutionOptimize = evolutionOptimize;
    }

    public boolean isPreValidationMessage() {
        return preValidationMessage;
    }

    public void setPreValidationMessage(boolean preValidationMessage) {
        this.preValidationMessage = preValidationMessage;
    }
}
