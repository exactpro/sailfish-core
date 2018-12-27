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

import java.lang.reflect.Constructor;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;

public class BaseMessage implements Cloneable
{
	protected final IMessage msg;

	public BaseMessage(IMessage message)
	{
		this.msg = message;
	}

	public IMessage getMessage()
	{
		return this.msg;
	}

	public boolean isAdmin(){
		throw new UnsupportedOperationException();
	}

	@Override
	public BaseMessage clone() {
		Constructor<? extends BaseMessage> ctor = null;
		BaseMessage clone = null;
		try {
			ctor = this.getClass().getConstructor(IMessage.class);
			clone = ctor.newInstance(this.msg.cloneMessage());
		} catch (Exception e) {
			throw new EPSCommonException(e);
		}
		return clone;
	}

}
