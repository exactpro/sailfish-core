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
package com.exactpro.sf;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.methods.CloseableHttpResponse;

/**
 * @author nikita.smirnov
 *
 */
public class FileDownloadWrapper implements Closeable {
    
    private final String fileName;
    
    private final CloseableHttpResponse response;
    
    private final InputStream inputStream;

    public FileDownloadWrapper(String fileName, CloseableHttpResponse response) throws UnsupportedOperationException, IOException {
        if (fileName == null || fileName.isEmpty())
            throw new IllegalArgumentException("File name can't be null");
        
        if (response == null)
            throw new IllegalArgumentException("Response can't be null");
        
        this.response = response;
        this.inputStream = response.getEntity().getContent();
        this.fileName = fileName;
    }
    
    @Override
    public void close() throws IOException {
        this.inputStream.close();
        this.response.close();
    }

    public String getFileName() {
        return fileName;
    }

    public InputStream getInputStream() {
        return inputStream;
    }
}
