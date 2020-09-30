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
package com.exactpro.sf.services.netty;

import javax.xml.bind.annotation.XmlRootElement;

import com.exactpro.sf.aml.Description;

@XmlRootElement
public class NettyMulticastClientSettings extends NettyClientSettings {
    private static final long serialVersionUID = -5243141927769743936L;

    @Description("Specify IP of an interface for outgoing multicast (service will resolve this IP to network interface)")
    private String interfaceIp;

	@Description("If specified - service will drop all messages receved from non-sourceIp addresses")
	private String sourceIp;

	@Description("Multicast address to listen")
	private String multicastIp;

	@Description("Multicast port (service have to bind to this port in order to listen multicast)")
	private int multicastPort;

    @Description("Specifies TTL for outgoing packets")
    private int ttl = 1;

    public String getInterfaceIp() {
        return interfaceIp;
    }

    public void setInterfaceIp(String interfaceIp) {
        this.interfaceIp = interfaceIp;
	}

	public String getSourceIp() {
		return sourceIp;
	}

	public void setSourceIp(String sourceIp) {
		this.sourceIp = sourceIp;
	}

	public String getMulticastIp() {
		return multicastIp;
	}

	public void setMulticastIp(String multicastIp) {
		this.multicastIp = multicastIp;
	}

	public int getMulticastPort() {
		return multicastPort;
	}

	public void setMulticastPort(int multicastPort) {
		this.multicastPort = multicastPort;
	}

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }
}
