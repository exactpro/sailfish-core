/******************************************************************************
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Values;

/**
 * @author sergey.smirnov
 *
 */
public enum DecodingContentHelper {
    
    GZIP {
        @Override
        public InputStream getStream(InputStream input) throws IOException {
            return new GZIPInputStream(input);
        }

        @Override
        public String getName() {
            return Values.GZIP;
        }
    },
    DEFLATE {

        @Override
        public InputStream getStream(InputStream input) throws IOException {
            return new DeflaterInputStream(input);
        }

        @Override
        public String getName() {
            return Values.DEFLATE;
        }
        
    },
    IDENTITY {

        @Override
        public InputStream getStream(InputStream input) throws IOException {
            return input;
        }

        @Override
        public String getName() {
            return Values.IDENTITY;
        }
        
    };
    
    public abstract InputStream getStream(InputStream input) throws IOException;
    public abstract String getName();
    
}
