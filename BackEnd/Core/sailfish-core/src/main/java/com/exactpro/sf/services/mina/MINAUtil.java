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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.ssl.SslFilter;

import com.exactpro.sf.common.util.HexDumper;
import com.exactpro.sf.common.util.StringUtil;

public class MINAUtil {

    public static String getHexdumpAdv(final IoBuffer in, int lengthLimit) {
        if (lengthLimit == 0) {
            throw new IllegalArgumentException("lengthLimit: " + lengthLimit + " (expected: 1+)");
        }

        boolean truncate = in.remaining() > lengthLimit;
        int size;

        if (truncate) {
            size = lengthLimit;
        } else {
            size = in.remaining();
        }

        if (size == 0) {
            return "empty";
        }

        StringBuilder out = new StringBuilder(size * 2 + 2);

        int mark = in.position();
        byte[] bytes = new byte[size];
        in.get(bytes);

        out.append(HexDumper.getHexdump(bytes));
        in.position(mark);

        if (truncate) {
            out.append(StringUtil.EOL + "trancated ...");
        }

        return out.toString();
    }

    public static SslFilter createSslFilter(boolean clientMode, String protocol,
            String keyStoreType, String keyStore, char[] keyStorePassword)
            throws NoSuchAlgorithmException, KeyStoreException, FileNotFoundException, IOException,
            CertificateException, UnrecoverableKeyException, KeyManagementException {

        SSLContext sslContext = SSLContext.getInstance(protocol);
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }

        } };

        KeyManager[] keyManagers = null;

        if (!clientMode) {
            KeyStore ks = getKeyStore(keyStoreType, keyStore, keyStorePassword);
            KeyManagerFactory kmf = KeyManagerFactory
                    .getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, keyStorePassword);
            keyManagers = kmf.getKeyManagers();
        } else {
            if (keyStore != null) {
                KeyStore ks = getKeyStore(keyStoreType, keyStore, keyStorePassword);
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ks);
                trustAllCerts = tmf.getTrustManagers();
            }
        }

        sslContext.init(keyManagers, trustAllCerts, new SecureRandom());
        SslFilter sslFilter = new SslFilter(sslContext, true);
        sslFilter.setUseClientMode(clientMode);

        return sslFilter;
    }


    private static KeyStore getKeyStore(String keyStoreType, String keyStore, char[] keyStorePassword) throws FileNotFoundException, IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {

        KeyStore ks = KeyStore.getInstance(keyStoreType);
        try (InputStream is = new FileInputStream(new File(keyStore))) {
            ks.load(is, keyStorePassword);
        }
        return ks;
    }
}
