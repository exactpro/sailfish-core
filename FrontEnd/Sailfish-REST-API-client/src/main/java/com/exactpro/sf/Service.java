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

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Service {

    // TODO: this enum duplicate com.exactpro.sf.services.ServiceStatus in core
	public static enum Status {
		OK, // NOTE: this status use in ServiceImportResult (import result can be OK or ERROR)
		CREATING,
		CREATED,
		INITIALIZING,
		INITIALIZED,
		STARTING,
		STARTED,
		DISPOSING,
		DISPOSED,
		WARNING,
		ERROR
	}
	
	private final Map<String, String> settings;
	private String name;
	private Status status;
	private String type;
	
	private String serviceHandlerClassName;
	private String settingsTag;
	
	public Service() {
		settings = new HashMap<String, String>();
	}
	
	public String getName() {
		return name;
	}
	
	protected void setName(String value) {
		name = value;
	}
	
	public String getType() {
		return type;
	}
	
	protected void setType(String value) {
		type = value;
	}
	
	public Status getStatus() {
		return status;
	}
	
	protected void setStatus(Status value) {
		status = value;
	}
	
	public Map<String, String> getSettings() {
		return settings;
	}
	
	public String getServiceHandlerClassName() {
		return serviceHandlerClassName;
	}

	protected void setServiceHandlerClassName(String serviceHandlerClassName) {
		this.serviceHandlerClassName = serviceHandlerClassName;
	}

	public String getSettingsTag() {
		return settingsTag;
	}

	protected void setSettingsTag(String settingsTag) {
		this.settingsTag = settingsTag;
	}

	protected void write(OutputStream out, DocumentBuilder builder, Transformer transformer) throws IOException, TransformerException {
		if (type == null || name == null || serviceHandlerClassName == null || settingsTag == null) {
			throw new RuntimeException("Could not write service: some of required fields are empty");
		}
		
		Document doc = builder.newDocument();
		Element root = doc.createElement("serviceDescription");
		doc.appendChild(root);
		
		Element ch = doc.createElement("type");
		ch.setTextContent(getType());
		root.appendChild(ch);
		
		ch = doc.createElement("name");
		ch.setTextContent(getName());
		root.appendChild(ch);
		
		ch = doc.createElement("serviceHandlerClassName");
		ch.setTextContent(serviceHandlerClassName);
		root.appendChild(ch);
		
		ch = doc.createElement(settingsTag);
		for (Map.Entry<String, String> e : settings.entrySet()) {
			Element el = doc.createElement(e.getKey());
			el.setTextContent(e.getValue());
			ch.appendChild(el);
		}
		root.appendChild(ch);
		
		DOMSource src = new DOMSource(doc);
		StreamResult dest = new StreamResult(out);
		transformer.transform(src, dest);
	}
	
	// Static members
	
	protected static Service fromXml(Node n) {
		Element el = (Element)n;
		
		Service svc = new Service();
		svc.setName(Util.getTextContent(el, "serviceName"));
		svc.setType(Util.getTextContent(el, "type"));
		svc.setStatus(Enum.valueOf(Service.Status.class, Util.getTextContent(el, "status")));
		NodeList settings = el.getElementsByTagName("settings").item(0).getChildNodes();
		for (int j = 0; j < settings.getLength(); j++) {
			Node z = settings.item(j);
			svc.getSettings().put(z.getNodeName(), z.getTextContent());
		}
		
		return svc;
	}
	
	protected static Service fromXml(Node n, String serviceHandlerClassName) {
		Service svc = fromXml(n);
		svc.serviceHandlerClassName = serviceHandlerClassName;
		return svc;
	}
	
	public static Service fromDescriptor(ServiceDescriptor des, String name) {
		Service svc = new Service();
		
		svc.setName(name);
		svc.setType(des.getName());
        svc.setServiceHandlerClassName("com.exactpro.sf.services.CollectorServiceHandler");
		svc.setSettingsTag(des.getSettingsTagName());
		
		return svc;
	}
	
}
