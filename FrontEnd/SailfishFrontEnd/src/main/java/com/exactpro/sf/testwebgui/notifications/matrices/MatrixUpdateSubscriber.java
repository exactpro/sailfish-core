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
package com.exactpro.sf.testwebgui.notifications.matrices;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.testwebgui.notifications.messages.MatrixUpdateResponse;
import com.exactpro.sf.testwebgui.notifications.scriptrunner.ScriptrunUpdateSubscriber;
import com.exactprosystems.webchannels.IUpdateRequestListener;
import com.exactprosystems.webchannels.channel.AbstractChannel;

@SuppressWarnings("deprecation")
public class MatrixUpdateSubscriber implements IUpdateRequestListener{

	private static final Logger logger = LoggerFactory.getLogger(ScriptrunUpdateSubscriber.class);
	private final AbstractChannel channel;
	private String id;

	public MatrixUpdateSubscriber(String id, AbstractChannel channel) {

		this.id = id;
		this.channel = channel;
		logger.info("Create MatrixSubscriber {} for channel {}", new Object[] {this, channel});

	}

	@Override
	public void onEvent(Object event)  {

		logger.debug("Event {} happend for MatrixSubscriber {}", event, this);

		if (event instanceof MatrixUpdateEvent) {

			MatrixUpdateResponse updateResponse = new MatrixUpdateResponse();
			updateResponse.setRequestId(id);

			channel.sendMessage(updateResponse);

		} else {

			throw new RuntimeException("Incorrect event type " + event.getClass());

		}

	}

	@Override
	public void destroy() {
		//channel = null;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("id", id).toString();
	}

}
