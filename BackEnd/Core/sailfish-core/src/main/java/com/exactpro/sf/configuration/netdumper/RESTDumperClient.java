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
package com.exactpro.sf.configuration.netdumper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

public class RESTDumperClient {
	
	private final String rdRoot;
	private final String host;
	private final int port;
	private final String identifier;
	private final HttpClient http;
	private final String iface;
	private String index;
	
	public RESTDumperClient(String rdRoot, String host, int port, String iface, String identifier) {
		this(rdRoot, host, port, iface, identifier, HttpClients.createDefault());
	}
	
	public RESTDumperClient(String rdRoot, String host, int port, String iface, String identifier, HttpClient http) {
		this.http = http;
		this.rdRoot = rdRoot;
		this.host = host;
		this.port = port;
		this.identifier = identifier;
		this.iface = iface;
	}
	
	public String getIdentifier() {
		return identifier;
	}
	
	public String generateFilename() {

        StringBuilder builder = new StringBuilder();
        builder.append(identifier)
                .append(".")
                .append(host)
                .append("_")
                .append(String.valueOf(port))
                .append(".tcpdump");

        return builder.toString();
	}
	
    public void start() throws NetDumperException, ClientProtocolException, IOException {

        StringBuilder builder = new StringBuilder();

        builder.append(this.rdRoot)
                .append("/start?host=")
                .append(this.host)
                .append("&port=")
                .append(String.valueOf(this.port));

        if (this.iface != null) {
            builder.append("&iface=")
                    .append(this.iface);
        }

        HttpGet req = new HttpGet(builder.toString());
		HttpResponse resp = http.execute(req);
		
        try (InputStream str = resp.getEntity().getContent()) {

            this.index = IOUtils.toString(str);

            try {

                Integer.parseInt(index);

            } catch (NumberFormatException e) {
                throw new NetDumperException("RESTDumper sent incorrect data instead of request index (received data: '" + index + "')");
            }
		}
	}
	
	public void stopRecord(File target) throws ClientProtocolException, IOException {
		try (OutputStream out = new FileOutputStream(target)) {
			stopRecord(out);
		}
	}
	
	public void stopRecord(OutputStream target) throws ClientProtocolException, IOException {

        StringBuilder builder = new StringBuilder();

        builder.append(this.rdRoot)
                .append("/stop?id=")
                .append(this.index);

        HttpGet req = new HttpGet(builder.toString());
		HttpResponse resp = http.execute(req);
		
        try (InputStream str = resp.getEntity().getContent()) {
            IOUtils.copy(str, target);
        }
	}
}