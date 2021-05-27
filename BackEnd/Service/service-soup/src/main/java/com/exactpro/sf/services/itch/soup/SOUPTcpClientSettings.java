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
package com.exactpro.sf.services.itch.soup;

import javax.xml.bind.annotation.XmlRootElement;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.aml.Ignore;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.itch.ITCHTCPClientSettings;

@XmlRootElement
public class SOUPTcpClientSettings extends ITCHTCPClientSettings {
    private static final long serialVersionUID = -2123394223164788920L;

    @Ignore
	private boolean doLiteLoginOnStart = false;
    @Ignore
	private byte flag1 = 0;
    @Ignore
	private boolean compressionUsed = false;
    @Ignore
	private int compressedChunkDelimeter = 0;

    private boolean sendLogoutOnDisconnect = true;

	private String member = "";

	private String password = "";

	private String version = "";

    private long requestedSequenceNumber = 0;

    private String requestedSession = "";

	@Override
    public boolean isDoLiteLoginOnStart() {
		return doLiteLoginOnStart;
	}

	@Override
    public void setDoLiteLoginOnStart(boolean doLiteLoginOnStart) {
		this.doLiteLoginOnStart = doLiteLoginOnStart;
	}

	@Override
    public byte getFlag1() {
		return flag1;
	}

	@Override
    public void setFlag1(byte flag1) {
		this.flag1 = flag1;
	}

	@Override
    public boolean isCompressionUsed() {
		return compressionUsed;
	}

	@Override
    public void setCompressionUsed(boolean compressionUsed) {
		this.compressionUsed = compressionUsed;
	}

	@Override
    public int getCompressedChunkDelimeter() {
		return compressedChunkDelimeter;
	}

	@Override
    public void setCompressedChunkDelimeter(int compressedChunkDelimeter) {
		this.compressedChunkDelimeter = compressedChunkDelimeter;
	}

	public String getMember() {
		return member;
	}

	public void setMember(String member) {
		this.member = member;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

    public long getRequestedSequenceNumber() {
        return requestedSequenceNumber;
    }

    public void setRequestedSequenceNumber(long requestedSequenceNumber) {
        this.requestedSequenceNumber = requestedSequenceNumber;
    }

    public String getRequestedSession() {
        return requestedSession;
    }

    public void setRequestedSession(String requestedSession) {
        this.requestedSession = requestedSession;
    }

    public boolean isSendLogoutOnDisconnect() {
        return sendLogoutOnDisconnect;
    }

    public void setSendLogoutOnDisconnect(boolean sendLogoutOnDisconnect) {
        this.sendLogoutOnDisconnect = sendLogoutOnDisconnect;
    }
}
