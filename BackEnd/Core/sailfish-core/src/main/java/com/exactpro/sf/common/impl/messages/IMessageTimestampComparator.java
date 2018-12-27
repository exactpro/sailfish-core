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
package com.exactpro.sf.common.impl.messages;

import java.util.Comparator;
import java.util.Date;

import com.exactpro.sf.common.messages.IMessage;

public class IMessageTimestampComparator implements Comparator<IMessage> {

	public final static IMessageTimestampComparator instance = new IMessageTimestampComparator();

	@Override
	public int compare(IMessage m1, IMessage m2) {
		if (m1 == null || m2 == null)
			throw new NullPointerException("Can't compere: IMessage is null");

		if (m1.getMetaData() == null || m2.getMetaData() == null)
			throw new NullPointerException("Can't compere IMessages: metadata is null");

		Date d1 = m1.getMetaData().getMsgTimestamp();
		Date d2 = m2.getMetaData().getMsgTimestamp();

		if (d1 == null || d2 == null)
			throw new NullPointerException("Timestamp is null. Comparing... " + m1.getNamespace() + "@" + m1.getName() + "; " + m2.getNamespace() + "@" + m2.getName());

		if (d1.equals(d2))
			return 0;

		return d1.before(d2) ? -1 : 1;
	}

}
