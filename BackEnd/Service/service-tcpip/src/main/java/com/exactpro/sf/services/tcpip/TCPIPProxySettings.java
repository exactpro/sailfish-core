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
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.configuration.HierarchicalConfiguration;

import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIAdapter;
import com.exactpro.sf.services.AbstractServiceSettings;

@XmlRootElement
public class TCPIPProxySettings extends AbstractServiceSettings
{
    private static final long serialVersionUID = 3381017339295573434L;

    private String host;
	private int port;
	private int listenPort;
	private String codecClassName;
	private long timeout;
	private boolean changeTags;
	private SailfishURI rulesAlias = SailfishURI.unsafeParse("TCPIPProxyRules");
	private SailfishURI dictionaryName;

	public String getCodecClassName() {
		return codecClassName;
	}

	public void setCodecClassName(String codecClassName) {
		this.codecClassName = codecClassName;
	}

	public final String getHost() {
		return this.host;
	}

	public final void setHost(String host) {
		this.host = host;
	}

	public final int getPort() {
		return this.port;
	}

	public final void setPort(int port) {
		this.port = port;
	}

	@Override
	public void load(HierarchicalConfiguration config)
	{
		this.host = config.getString("host");
		this.port = config.getInt("port");
		this.listenPort = config.getInt("listenPort");
		this.codecClassName = config.getString("codecClass");
		this.timeout = config.getLong("timeout");
	}

	public void setListenPort(int listenPort) {
		this.listenPort = listenPort;
	}

	public int getListenPort() {
		return this.listenPort;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public long getTimeout() {
		return this.timeout;
	}

	public boolean isChangeTags() {
		return changeTags;
	}

	public void setChangeTags(boolean changeTags) {
		this.changeTags = changeTags;
	}

	public SailfishURI getRulesAlias() {
		return rulesAlias;
	}

	@XmlJavaTypeAdapter(SailfishURIAdapter.class)
	public void setRulesAlias(SailfishURI rulesAlias) {
		this.rulesAlias = rulesAlias;
	}

	@Override
	public SailfishURI getDictionaryName() {
		return dictionaryName;
	}

	@Override
	public void setDictionaryName(SailfishURI dictionaryName) {
		this.dictionaryName = dictionaryName;
	}
}
