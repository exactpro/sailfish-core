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
package com.exactpro.sf.services.fast;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.services.ISession;

class FASTInvalidSession implements ISession {

	private String name;

	FASTInvalidSession(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public IMessage send(Object message) {
		throw new EPSCommonException("Invalid session (Fast client has not been connected)");
	}


	@Override
	public IMessage sendDirty(Object message) {
		return send(message);
	}

	@Override
	public void close() {
	}

	@Override
	public boolean isClosed() {
		return true;
	}

	@Override
	public boolean isLoggedOn() {
		return false;
	}
}
