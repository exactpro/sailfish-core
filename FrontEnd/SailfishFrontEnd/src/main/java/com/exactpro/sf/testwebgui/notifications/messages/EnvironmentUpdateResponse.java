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

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.exactpro.sf.services.ChangeEnvironmentEvent;
import com.exactprosystems.webchannels.messages.AbstractMessage;
import com.exactprosystems.webchannels.messages.ChannelsMessage;

@ChannelsMessage
public class EnvironmentUpdateResponse extends AbstractMessage {

    private String requestId;
    private String envName;
    private String newEnvName;
    private ChangeEnvironmentEvent.Status status;

    public EnvironmentUpdateResponse() {
        super();
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
                append("messageType", SFMessageType.EnvironmentUpdateResponse).
                toString();
    }

    public String getEnvName() {
        return envName;
    }

    public void setEnvName(String envName) {
        this.envName = envName;
    }

    public ChangeEnvironmentEvent.Status getStatus() {
        return status;
    }

    public void setStatus(ChangeEnvironmentEvent.Status status) {
        this.status = status;
    }

    public String getNewEnvName() {
        return newEnvName;
    }

    public void setNewEnvName(String newEnvName) {
        this.newEnvName = newEnvName;
    }

	@Override
	public boolean isAdmin() {
		return false;
	}
}
