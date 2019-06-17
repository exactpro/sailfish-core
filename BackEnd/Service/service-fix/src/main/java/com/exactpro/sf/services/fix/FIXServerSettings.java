/******************************************************************************
 * Copyright (c) 2009-2018, Exactpro Systems LLC
 * www.exactpro.com
 * Build Software to Test Software
 *
 * All rights reserved.
 * This is unpublished, licensed software, confidential and proprietary
 * information which is the property of Exactpro Systems LLC or its licensors.
 ******************************************************************************/
package com.exactpro.sf.services.fix;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.configuration.HierarchicalConfiguration;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.aml.Description;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.AbstractServiceSettings;
import com.exactpro.sf.services.RequiredParam;

@XmlRootElement
public class FIXServerSettings extends FIXCommonSettings {

    private static final long serialVersionUID = -7663638300222588201L;
    
	private Class<?> application;

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

	public FIXServerSettings() {
        setApplicationClassName("com.exactpro.sf.services.fix.FIXTESTApplication");
	}

	@Override
	public void load(HierarchicalConfiguration config) {
		// do nothing
	}

	public void setApplicationClass(String clazz) {
		if (clazz == null) {
			return;
		}
		try {
			this.application = Class.forName(clazz);
		} catch (ClassNotFoundException e) {
			throw new EPSCommonException("Cannot load application class: "+clazz, e);
		}
	}

	public Class<?> getApplicationClass() {
		return application;
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
}
