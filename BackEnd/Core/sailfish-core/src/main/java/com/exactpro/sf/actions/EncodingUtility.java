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
package com.exactpro.sf.actions;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityMethod;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterInputStream;
import java.util.zip.InflaterInputStream;

@MatrixUtils
@ResourceAliases({"EncodingUtility"})
public class EncodingUtility extends AbstractCaller {
    @Description("Encodes string to base64.<br/>"+
            "<b>content</b> - string to encode.<br/>" +
            "<b>compress</b> - perform compression of the result string (y / Y / n / N).<br/>" +
            "Example:<br/>" +
            "#EncodeBase64(\"Test content\", \"n\") returns \"VGVzdCBjb250ZW50\"<br/>"+
            "Example:<br/>" +
            "#EncodeBase64(\"Test content\", \"y\") returns \"eJwLSS0uUUjOzytJzSsBAB2pBLw=\"<br/>")
    @UtilityMethod
    public String EncodeBase64(String content, Object compress) throws Exception{
        Boolean doCompress = BooleanUtils.toBoolean((String)compress);
        byte[] result;
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
             try (OutputStream b64 = new Base64OutputStream(os, true, -1, new byte[0]);
                    InputStream ba = new ByteArrayInputStream(content.getBytes());
                    InputStream is = doCompress ? new DeflaterInputStream(ba) : ba) {
                IOUtils.copy(is, b64);
            }
            os.flush();
            result = os.toByteArray();
        }
        return new String(result);
    }

    @Description("Decodes base64 to string.<br/>"+
            "<b>base64Content</b> - base64 string to decode.<br/>" +
            "<b>compress</b> - perform compression of the result string (y / Y / n / N).<br/>" +
            "Example:<br/>" +
            "#DecodeBase64(\"VGVzdCBjb250ZW50\", \"n\") returns \"Test content\"<br/>"+
            "Example:<br/>" +
            "#DecodeBase64(\"eJwLSS0uUUjOzytJzSsBAB2pBLw=\", \"y\") returns \"Test content\"<br/>")
    @UtilityMethod
    public String DecodeBase64(String base64Content, Object compress) throws Exception{
        Boolean doCompress = BooleanUtils.toBoolean((String)compress);
        byte[] result;
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            try (InputStream ba = new ByteArrayInputStream(base64Content.getBytes());
                    InputStream b64 = new Base64InputStream(ba, false);
                    InputStream is = doCompress ? new InflaterInputStream(b64) : b64) {
                IOUtils.copy(is, os);
            }
            os.flush();
            result = os.toByteArray();
        }
        return new String(result);
    }

}
