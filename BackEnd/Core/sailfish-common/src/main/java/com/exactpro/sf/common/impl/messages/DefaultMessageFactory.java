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

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.configuration.suri.SailfishURI;

public class DefaultMessageFactory extends AbstractMessageFactory
{
	private static IMessageFactory factory = null;

	private DefaultMessageFactory() {}

	public static IMessageFactory getFactory()
	{
		synchronized (DefaultMessageFactory.class)
		{
			if ( factory == null )
				factory = new DefaultMessageFactory();
		}

		return factory;
	}

    @Override
    public void init(String namespace, SailfishURI dictionaryURI) {
        // Instance of this class should not be able to initialize
    }

    @Override
    public IMessage createMessage(String name) {
        throw new UnsupportedOperationException("Cannot create message without a namespace: " + name);
    }

    @Override
    public String getProtocol() {
        return null;
    }
}
