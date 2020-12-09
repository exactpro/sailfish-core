/*******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.sf.services.fix;

import javax.xml.bind.annotation.XmlRootElement;

import com.exactpro.sf.services.util.ServiceUtil;
import org.apache.commons.configuration.HierarchicalConfiguration;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.aml.Description;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.AbstractServiceSettings;
import com.exactpro.sf.services.RequiredParam;

@XmlRootElement
public class FIXServerSettings extends FIXCommonSettings {

    private static final long serialVersionUID = -7663638300222588201L;
    
	@RequiredParam
	@Description("Socket port for listening to incomming connections")
	private int SocketAcceptPort;

    @Description("Session setting that causes the session to reset sequence numbers when initiating a logon")
    private boolean ResetOnLogon;

	@Description("Tell session whether or not to expect a data dictionary. \n" +
			"You should always use a DataDictionary if you are using repeating groups.")
	private boolean UseDataDictionary;

	@RequiredParam
	@Description("Class name of the FIX Server application")
	private String ApplicationClassName;

	@Description("Tell application to keep messages in memory and store them to DB or not")
	private boolean KeepMessagesInMemory;

	@Description("if TRUE then server will not wait logout message from system\n"
			+ " and will close session immediately after sending of logout")
	private boolean forceDisconnectByDispose;

	@Description("The heartbeat interval that will be used if tag 108 absents in the client Logon message. "
            + "If this parameter has value less than ZERO it won't be used")
	private int defaultHeartbeatInterval;

	@Description("Please provide listner names to use in service. \n"
			+ "It can be comma separated list of values or or an alias to a data file with these values (one value per line).\n"
			+ "Must be supported by selected server application. \n"
			+ "Example: a,b,c or " + ServiceUtil.ALIAS_PREFIX + "listnerNames\n"
	)
	private String listenerNames;

	public FIXServerSettings() {
        setApplicationClassName("com.exactpro.sf.services.fix.FIXTESTApplication");
	}

	@Override
	public void load(HierarchicalConfiguration config) {
		// do nothing
	}

    public int getSocketAcceptPort() {
        return SocketAcceptPort;
    }

    public void setSocketAcceptPort(int socketAcceptPort) {
        SocketAcceptPort = socketAcceptPort;
    }


    public boolean isUseDataDictionary() {
		return UseDataDictionary;
	}

	public void setUseDataDictionary(boolean useDataDictionary) {
		UseDataDictionary = useDataDictionary;
	}

	public String getApplicationClassName() {
		return ApplicationClassName;
	}

	public void setApplicationClassName(String applicationClassName) {
		ApplicationClassName = applicationClassName;
		setApplicationClass(ApplicationClassName);
	}

	public boolean getKeepMessagesInMemory() {
		return KeepMessagesInMemory;
	}

	public void setKeepMessagesInMemory(boolean save) {
		this.KeepMessagesInMemory = save;
	}

	public boolean isForceDisconnectByDispose() {
		return forceDisconnectByDispose;
	}

	public void setForceDisconnectByDispose(boolean forceDisconnectByDispose) {
		this.forceDisconnectByDispose = forceDisconnectByDispose;
	}

    /**
     * @return the resetOnLogon
     */
    public boolean isResetOnLogon() {
        return ResetOnLogon;
    }

    /**
     * @param resetOnLogon the resetOnLogon to set
     */
    public void setResetOnLogon(boolean resetOnLogon) {
        ResetOnLogon = resetOnLogon;
    }

    public int getDefaultHeartbeatInterval() {
        return defaultHeartbeatInterval;
    }

    public void setDefaultHeartbeatInterval(int defaultHeartbeatInterval) {
        this.defaultHeartbeatInterval = defaultHeartbeatInterval;
    }

	public String getListenerNames() { return listenerNames; }

	public void setListenerNames(String listenerNames) { this.listenerNames = listenerNames; }
}
