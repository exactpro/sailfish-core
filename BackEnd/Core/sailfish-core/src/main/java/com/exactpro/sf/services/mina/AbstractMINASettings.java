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
package com.exactpro.sf.services.mina;

import javax.xml.bind.annotation.XmlRootElement;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.externalapi.DictionaryProperty;
import com.exactpro.sf.externalapi.DictionaryType;
import com.exactpro.sf.services.AbstractServiceSettings;
import com.exactpro.sf.services.RequiredParam;

@XmlRootElement
public abstract class AbstractMINASettings extends AbstractServiceSettings {
    private static final long serialVersionUID = -4850020892139075669L;

    @RequiredParam
    @DictionaryProperty(type = DictionaryType.MAIN)
    protected SailfishURI dictionaryName;

    @Description("Enables SSL usage for QFJ acceptor or initiator")
    private boolean useSSL;
    @Description("Controls which particular protocols for secure connection are enabled for handshake. Use SSL(older) or TLS")
    private String sslProtocol;

    @Description("KeyStore to use with SSL")
    private String sslKeyStore;
    @Description("KeyStore password to use with SSL")
    private String sslKeyStorePassword;
    @Description("Type of specified keystore. Can be JKS, JCEKS, PKCS12, PKCS11")
    private String keyStoreType;

    @Override
    public SailfishURI getDictionaryName() {
        return dictionaryName;
    }

    @Override
    public void setDictionaryName(SailfishURI dictionaryName) {
        this.dictionaryName = dictionaryName;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    public String getSslProtocol() {
        return sslProtocol;
    }

    public void setSslProtocol(String sslProtocol) {
        this.sslProtocol = sslProtocol;
    }

    public String getSslKeyStore() {
        return sslKeyStore;
    }

    public void setSslKeyStore(String sslKeyStore) {
        this.sslKeyStore = sslKeyStore;
    }

    public String getSslKeyStorePassword() {
        return sslKeyStorePassword;
    }

    public void setSslKeyStorePassword(String sslKeyStorePassword) {
        this.sslKeyStorePassword = sslKeyStorePassword;
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }

    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }
}
