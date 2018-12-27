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
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ServiceDescriptors {
	
	private Map<String, ServiceDescriptor> descriptors;
	
	private ServiceDescriptors() {
		descriptors = new HashMap<String, ServiceDescriptor>();
	}
	
	public ServiceDescriptor get(String name) {
		return descriptors.get(name);
	}
	
	// Static members
	
	private static ServiceDescriptors instance;
	
	public static void init(DocumentBuilder db) throws IOException, SAXException {
		ServiceDescriptors tmp = new ServiceDescriptors();
		
		Document doc = db.parse(ServiceDescriptors.class.getClassLoader().getResourceAsStream("xml/cfg/descriptor.xml"));
		Element el = doc.getDocumentElement();
		el = (Element)el.getElementsByTagName("Descriptions").item(0);
		el = (Element)el.getElementsByTagName("Services").item(0);
		NodeList svcs = el.getElementsByTagName("Service");
		
		for (int i = 0; i < svcs.getLength(); i++) {
			el = (Element)svcs.item(i);
			String name = el.getAttribute("name");
			String className = Util.getTextContent(el, "className");
			String settingsClassName = Util.getTextContent(el, "settingsClassName");
			tmp.descriptors.put(name, new ServiceDescriptor(name, className, settingsClassName));
		}
		
		instance = tmp;
	}
	
	public static ServiceDescriptors getInstance() {
		return instance;
	}
	
	public static ServiceDescriptor forName(String name) {
		return instance == null ? null : instance.get(name);
	}
	
}
