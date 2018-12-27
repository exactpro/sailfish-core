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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.services.ntg.NTGServerTest.MessageType;


public final class NTGClientTest
{
	@SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(NTGClientTest.class);

	@SuppressWarnings("unused")
	private ClientStrategy strategy = ClientStrategy.SessionOnly;

	@SuppressWarnings("unused")
    private List<MessageType> msgs = new ArrayList<MessageType>();

    private final NTGClient ntgClient = null;

    public NTGClientTest(int clientID, NTGClientSettings settings)
	throws IOException
	{
		IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();

		try (InputStream in = new FileInputStream((((System.getProperty("basedir") == null) ? "." : System.getProperty("basedir")))
                + File.separator + "dictionaries" + File.separator + "ntg.xml")) {
			loader.load(in);
    	}
	}

    public NTGClientTest(int clientID, NTGClientSettings settings, ClientStrategy strategy, List<MessageType> msgs)
	throws IOException
	{
		this(clientID, settings );
		this.strategy = strategy;
		this.msgs = msgs;
	}


	public enum ClientStrategy
	{
		SessionOnly,

		LogonOnly,

		Normal;
	}

    public NTGClient getClient() {
        return ntgClient;
	}

}
