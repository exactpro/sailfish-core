/******************************************************************************
 * Copyright 2009-2024 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.itch;

import com.exactpro.sf.aml.Description;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ITCHTCPClientSettings extends ITCHClientSettings {
    private static final long serialVersionUID = -5299816885595794380L;

    private boolean doLiteLoginOnStart;

	private byte flag1;

	@Description("Trim messages from left")
	private boolean trimLeftPadding = true;

    @Description("Enable the feature to send heartbeat automatically. You can specify a time interval in 'Heartbeat Timeout' option.")
	private boolean sendHeartBeats;

    @Description("Send Heartbeat every specified seconds. This feature may be enabled by 'Send Heart Beats' option")
	private int heartbeatTimeout = 5;

	private String username;

	private boolean compressionUsed;

	private int compressedChunkDelimeter = 57005;

	private boolean doLoginOnStart;

	private boolean reconnecting;

	private boolean disposeWhenSessionClosed;

    private int reconnectingTimeout = 5000;

	public boolean isDoLiteLoginOnStart() {
		return doLiteLoginOnStart;
	}

	public void setDoLiteLoginOnStart(boolean doLiteLoginOnStart) {
		this.doLiteLoginOnStart = doLiteLoginOnStart;
	}

	public byte getFlag1() {
		return flag1;
	}

	public void setFlag1(byte flag1) {
		this.flag1 = flag1;
	}

	public boolean isTrimLeftPadding() {
		return trimLeftPadding;
	}

	public void setTrimLeftPadding(boolean trimLeftPadding) {
		this.trimLeftPadding = trimLeftPadding;
	}

	public boolean isSendHeartBeats() {
		return sendHeartBeats;
	}

	public void setSendHeartBeats(boolean sendHeartBeats) {
		this.sendHeartBeats = sendHeartBeats;
	}

	public int getHeartbeatTimeout() {
		return heartbeatTimeout;
	}

	public void setHeartbeatTimeout(int heartbeatTimeout) {
		this.heartbeatTimeout = heartbeatTimeout;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public boolean isCompressionUsed() {
		return compressionUsed;
	}

	public void setCompressionUsed(boolean compressionUsed) {
		this.compressionUsed = compressionUsed;
	}

	public int getCompressedChunkDelimeter() {
		return compressedChunkDelimeter;
	}

	public void setCompressedChunkDelimeter(int compressedChunkDelimeter) {
		this.compressedChunkDelimeter = compressedChunkDelimeter;
	}

	public boolean isDoLoginOnStart() {
		return doLoginOnStart;
	}

	public void setDoLoginOnStart(boolean doLoginOnStart) {
		this.doLoginOnStart = doLoginOnStart;
	}

	public boolean isReconnecting() {
		return reconnecting;
	}

	public void setReconnecting(boolean reconnecting) {
		this.reconnecting = reconnecting;
	}

	public boolean isDisposeWhenSessionClosed() {
		return disposeWhenSessionClosed;
	}

	public void setDisposeWhenSessionClosed(boolean disposeWhenSessionClosed) {
		this.disposeWhenSessionClosed = disposeWhenSessionClosed;
	}

    /**
     * @return the reconnectingTimeout
     */
    public int getReconnectingTimeout() {
        return reconnectingTimeout;
    }

    /**
     * @param reconnectingTimeout the reconnectingTimeout to set
     */
    public void setReconnectingTimeout(int reconnectingTimeout) {
        this.reconnectingTimeout = reconnectingTimeout;
    }
}
