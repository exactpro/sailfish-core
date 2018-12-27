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
package com.exactpro.sf.testwebgui.notifications.messages;

import com.exactprosystems.webchannels.messages.AbstractMessage;
import com.exactprosystems.webchannels.messages.ChannelsMessage;
import org.apache.commons.lang3.builder.ToStringBuilder;

@ChannelsMessage
public class RequestStatus extends AbstractMessage {

	private Boolean success;
	private String error;
	private String requestId;
	
	public RequestStatus() {
		super();
	}

	public Boolean getSuccess() {
		return success;
	}

	public void setSuccess(Boolean success) {
		this.success = success;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).
				append("messageType", SFMessageType.RequestStatus).
				append("success", success).
				append("error", error).
				append("requestId", requestId).
				toString();
	}

	@Override
	public boolean isAdmin() {
		return false;
	}
}
