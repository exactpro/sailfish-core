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
package com.exactpro.sf.testwebgui.restapi.editor;

import java.util.SortedSet;
import java.util.TreeSet;

import com.exactpro.sf.common.impl.messages.IMessageTimestampComparator;
import com.exactpro.sf.common.messages.IMessage;

public class UnexpectedMessagesContainer {

	private final String testCaseName;

	private final SortedSet<IMessage> allMessages = new TreeSet<>(IMessageTimestampComparator.instance);

	private final SortedSet<IMessage> unexpectedMessages = new TreeSet<>(IMessageTimestampComparator.instance);

	private final SortedSet<IMessage> receivedMessages = new TreeSet<>(IMessageTimestampComparator.instance);

	public UnexpectedMessagesContainer(String testCaseName) {
		super();
		this.testCaseName = testCaseName;
	}

	public SortedSet<IMessage> getAllMessages() {
		return allMessages;
	}

	public SortedSet<IMessage> getUnexpectedMessages() {
		return unexpectedMessages;
	}

	public SortedSet<IMessage> getReceivedMessages() {
		return receivedMessages;
	}

	public String getTestCaseName() {
		return testCaseName;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UnexpectedMessagesContainer [testCaseName=");
		builder.append(testCaseName);
		builder.append(", allMessages=");
		builder.append(allMessages);
		builder.append(", unexpectedMessages=");
		builder.append(unexpectedMessages);
		builder.append(", receivedMessages=");
		builder.append(receivedMessages);
		builder.append("]");
		return builder.toString();
	}

}
