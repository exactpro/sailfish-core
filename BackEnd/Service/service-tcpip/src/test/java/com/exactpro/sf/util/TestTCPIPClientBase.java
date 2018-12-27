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
package com.exactpro.sf.util;

import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.CollectorServiceHandler;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.services.tcpip.TCPIPClient;
import com.exactpro.sf.services.tcpip.TCPIPServerSettings;
import com.exactpro.sf.services.tcpip.TCPIPSettings;

import junit.framework.Assert;

public abstract class TestTCPIPClientBase extends TestClientBase {

    protected static TCPIPClient client;
    protected static int port = 9871;
    protected static SailfishURI dictionaryName = SailfishURI.unsafeParse("FAKE");

    protected void startServices(int timeout) {
        TCPIPSettings settingsClient = new TCPIPSettings();
        settingsClient.setDictionaryName(dictionaryName);
        settingsClient.setHost(host);
        settingsClient.setPort(port);
        settingsClient.setCodecClassName("com.exactpro.sf.services.tcpip.InternalJsonCodec");
        settingsClient.setAutoConnect(true);
        settingsClient.setUseSSL(true);
        settingsClient.setSslProtocol("TLS");
        settingsClient.setPersistMessages(false);
        client = new TCPIPClient();
        handlerClient = new CollectorServiceHandler();
        serviceName = new ServiceName(ServiceName.DEFAULT_ENVIRONMENT, "TCPIPClient");
        client.init(serviceContext, mockedMonitor, handlerClient, settingsClient, serviceName);
        Assert.assertEquals(ServiceStatus.INITIALIZED, client.getStatus());
        client.start();
    }

    protected static IServiceSettings getServerSettings() {
        TCPIPServerSettings settings = new TCPIPServerSettings();
        settings.setDictionaryName(dictionaryName);
        settings.setCodecClassName("com.exactpro.sf.services.tcpip.InternalJsonCodec");
        settings.setFieldConverterClassName("com.exactpro.sf.services.tcpip.DefaultFieldConverter");
        settings.setPersistMessages(false);
        settings.setPort(port);
        settings.setHost(host);
        settings.setDecodeByDictionary(false);
        settings.setUseSSL(true);
        settings.setSslProtocol("TLS");
        settings.setKeyStoreType("PKCS12");
        settings.setSslKeyStore("src/test/resources/serverKeyStore.p12");
        settings.setSslKeyStorePassword("changeit");
        return settings;
    }


}
