/******************************************************************************
 * Copyright 2009-2023 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.http;

import static com.exactpro.sf.services.util.ServiceUtil.ALIAS_PREFIX;

import javax.xml.bind.annotation.XmlRootElement;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.services.RequiredParam;
import com.exactpro.sf.services.netty.NettyClientSettings;
import com.exactpro.sf.services.util.ServiceUtil;

@XmlRootElement
public class HTTPClientSettings extends NettyClientSettings {
    private static final long serialVersionUID = 183932429809985050L;

    @Description("Max size of incoming http message")
    private int maxHTTPMessageSize = 1048576;

    @Description("Use name for authentication")
    private String userName;

    @Description("Custom headers. Format: name=value[;name=value]")
    private String customHeaders;

    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @param userName the userName to set
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    @Description("Password for authentication")
    private String password;

    @RequiredParam
    private String uri = "http://localhost:8080";

    @Description("Client's certificate in PEM format. It is required for mutual authentication.<br/>"
            + "The value either the certificate content itself or the alias of the file with the certificate.<br/>"
            + "Correct certificate starts with -----BEGIN CERTIFICATE----- and ends with -----END CERTIFICATE-----<br/>"
            + "You can specify alias using the alias prefix " + ALIAS_PREFIX + ".<br/>"
            + "Example: " + ALIAS_PREFIX + "someFileAlias")
    private String clientCertificate;

    @Description("Client's PKCS #8 private key for the certificate in PEM format. It is required for mutual authentication.<br/>"
            + "The value either the private key content itself or the alias of the file with the private key.<br/>"
            + "Correct key starts with -----BEGIN PRIVATE KEY----- and ends with -----END PRIVATE KEY-----<br/>"
            + "You can specify alias using the alias prefix " + ALIAS_PREFIX + ".<br/>"
            + "Example: " + ALIAS_PREFIX + "someFileAlias")
    private String privateKey;

    @Description("The key phrase for client's private key")
    private String keyPhrase;

    @Description("Endpoint for PING_IDP access token (example: http://example.com/token)")
    private String tokenRequestUrl;

    public String getURI() {
        return uri;
    }

    public void setURI(String uri) {
        this.uri = uri.trim();
    }

    public int getMaxHTTPMessageSize() {
        return maxHTTPMessageSize;
    }

    public void setMaxHTTPMessageSize(int maxHTTPMessageSize) {
        this.maxHTTPMessageSize = maxHTTPMessageSize;
    }

    public String getCustomHeaders() {
        return customHeaders;
    }

    public void setCustomHeaders(String customHeaders) {
        this.customHeaders = customHeaders;
    }

    public String getClientCertificate() {
        return clientCertificate;
    }

    public void setClientCertificate(String clientCertificate) {
        this.clientCertificate = clientCertificate;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getKeyPhrase() {
        return keyPhrase;
    }

    public void setKeyPhrase(String keyPhrase) {
        this.keyPhrase = keyPhrase;
    }

    public String getTokenRequestUrl() {
        return tokenRequestUrl;
    }

    public void setTokenRequestUrl(String tokenRequestUrl) {
        this.tokenRequestUrl = tokenRequestUrl;
    }

}
