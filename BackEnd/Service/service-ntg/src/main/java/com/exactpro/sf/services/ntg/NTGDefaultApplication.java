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
package com.exactpro.sf.services.ntg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;

public class NTGDefaultApplication implements INTGApplication {

    private static final Logger logger = LoggerFactory.getLogger(NTGDefaultApplication.class);

	private List<IMessage> inputMessages = new ArrayList<IMessage>(10000);
	protected List<IMessage> outputMessages = new ArrayList<IMessage>(10000);

	@Override
	public void onMessageReceived(final IMessage message) {
        logger.trace(" DefaultNTGApplication.onMessageReceived()  [{}].", message);
		inputMessages.add(message);
	}

	@Override
	public void onMessageSent(final IMessage message) {
        logger.trace(" DefaultNTGApplication.onMessageSent() Message has been sent.");
		outputMessages.add(message);
	}

	@Override
	public void onException(Throwable throwable) {
        logger.trace(" DefaultNTGApplication onException()  [{}].", throwable);

		throwable.printStackTrace();
	}

	@Override
	public void onSessionOpened() {
        logger.trace("DefaultNTGApplication [%d].onSessionOpened()");
	}

	@Override
	public void onSessionClosed() {
        logger.trace("DefaultNTGApplication [%d].onSessionClosed()");
	}

	@Override
	public void onSessionIdle() {
        logger.trace("  DefaultNTGApplication[ %d ].onSessionIdle().");
	}

	@Override
    public List<IMessage> getInputMessages() {
		return Collections.unmodifiableList(inputMessages);
	}

	@Override
    public List<IMessage> getOutputMessages() {
		return Collections.unmodifiableList(outputMessages);
	}
}
