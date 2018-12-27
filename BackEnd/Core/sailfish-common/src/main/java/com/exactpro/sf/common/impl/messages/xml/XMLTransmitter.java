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
package com.exactpro.sf.common.impl.messages.xml;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Unmarshaller.Listener;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.format.DateTimeFormatter;
import org.xml.sax.SAXException;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.util.DateTimeUtility;

public class XMLTransmitter {

	private static Logger logger = LoggerFactory.getLogger(XMLTransmitter.class);

	private static XMLTransmitter instance = new XMLTransmitter();

	private final Map<Package, JAXBContext> contexts = new HashMap<>();

	private XMLTransmitter(){}

	private <T> JAXBContext getContext(Class<T> tclass) throws Exception {
		JAXBContext context;
		Package pack = tclass.getPackage();
		if(contexts.containsKey(pack)){
			context = contexts.get(pack);
		}else{
			context = JAXBContext.newInstance(pack.getName());
			contexts.put(pack, context);
		}
		return context;
	}

	private static class ValidationEventWriteHandler implements ValidationEventHandler{

		@Override
		public boolean handleEvent(ValidationEvent ve) {
			String msg = ve.getMessage();
			ValidationEventLocator vel = ve.getLocator();
			org.w3c.dom.Node  node = vel.getNode();

			String name = node!=null?node.getLocalName():"null";
			logger.error(  "node  : {}.{}: {}", name, vel.getOffset(), msg );
			return false;
		}

	}

	private static class ValidationEventReadHandler implements ValidationEventHandler{

		@Override
		public boolean handleEvent(ValidationEvent ve) {
			String msg = ve.getMessage();
			ValidationEventLocator vel = ve.getLocator();
			logger.error(  "location  : {}.{}: {}", vel.getLineNumber(), vel.getColumnNumber(), msg );
			return false;
		}

	}

	public <T> void marshal(T instance, File xmlFile, File schemaFile) throws JAXBException {
		Schema schema = null;
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		try {
			schema = schemaFactory.newSchema(schemaFile);
		} catch (SAXException e) {
			throw new EPSCommonException("A schema file could not be parsed.", e);
		}

		Marshaller marshaller;
		try {
			marshaller = getContext(instance.getClass()).createMarshaller();
		} catch (Exception e) {
			throw new EPSCommonException("A marshaller instance could not created for class " + instance.getClass().getCanonicalName(),e);
		}
		marshaller.setSchema(schema);
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		marshaller.setEventHandler(new ValidationEventWriteHandler());
		marshaller.marshal(instance, xmlFile);
	}

	public <T> void marshal(T instance, File xmlFile) throws JAXBException {
		Marshaller marshaller;
		try {
			marshaller = getContext(instance.getClass()).createMarshaller();
		} catch (Exception e) {
			throw new EPSCommonException("A marshaller instance could not created for class " + instance.getClass().getCanonicalName(),e);
		}
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		marshaller.marshal(instance, xmlFile);
	}

    @SuppressWarnings("unchecked")
    public <T> T unmarshal(Class<T> tclass, File xmlFile, File schemaFile) throws JAXBException {
		Schema schema = null;
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		try {
			schema = schemaFactory.newSchema(schemaFile);
		} catch (SAXException e) {
			throw new EPSCommonException("A schema file could not be parsed.", e);
		}

		Unmarshaller unmarshaller;
		try {
			unmarshaller = getContext(tclass).createUnmarshaller();
		} catch (Exception e) {
			throw new EPSCommonException("An unmarshaller instance could not be created for class " + tclass.getCanonicalName(),e);
		}
		unmarshaller.setSchema(schema);
		unmarshaller.setEventHandler(new ValidationEventReadHandler());
		return (T)unmarshaller.unmarshal(xmlFile);
	}

    @SuppressWarnings("unchecked")
    public <T> T unmarshal(Class<T> tclass, File xmlFile) throws JAXBException {
		Unmarshaller unmarshaller;
		try {
			unmarshaller = getContext(tclass).createUnmarshaller();
		} catch (Exception e) {
			throw new EPSCommonException("An unmarshaller instance could not be created for class " + tclass.getCanonicalName(),e);
		}
		return (T)unmarshaller.unmarshal(xmlFile);
	}

    @SuppressWarnings("unchecked")
    public <T> T unmarshal(Class<T> tclass, InputStream input, File schemaFile) throws JAXBException {
		Schema schema = null;
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		try {
			schema = schemaFactory.newSchema(schemaFile);
		} catch (SAXException e) {
			throw new EPSCommonException("A schema file could not be parsed.", e);
		}

		Unmarshaller unmarshaller;
		try {
			unmarshaller = getContext(tclass).createUnmarshaller();
		} catch (Exception e) {
			throw new EPSCommonException("An unmarshaller instance could not be created for class " + tclass.getCanonicalName(),e);
		}
		unmarshaller.setSchema(schema);
		return (T)unmarshaller.unmarshal(input);
	}

    @SuppressWarnings("unchecked")
    public <T> T unmarshal(Class<T> tclass, InputStream input, InputStream schemaInput, Listener listener) throws JAXBException {
		Schema schema = null;
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		try {
			schema = schemaFactory.newSchema(new StreamSource(schemaInput));
		} catch (SAXException e) {
			throw new EPSCommonException("A schema file could not be parsed.", e);
		}

		Unmarshaller unmarshaller;
		try {
			unmarshaller = getContext(tclass).createUnmarshaller();
			unmarshaller.setListener(listener);
		} catch (Exception e) {
			throw new EPSCommonException("An unmarshaller instance could not be created for class " + tclass.getCanonicalName(),e);
		}
		unmarshaller.setSchema(schema);
		return (T)unmarshaller.unmarshal(input);
	}

    @SuppressWarnings("unchecked")
    public <T> T unmarshal(Class<T> tclass, InputStream input) throws JAXBException {
		Unmarshaller unmarshaller;
		try {
			unmarshaller = getContext(tclass).createUnmarshaller();
		} catch (Exception e) {
			throw new EPSCommonException("An unmarshaller instance could not be created for class " + tclass.getCanonicalName(),e);
		}
		return (T)unmarshaller.unmarshal(input);
	}

	public <T> void marshal(T instance, OutputStream output, File schemaFile) throws JAXBException {
		Schema schema = null;
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		try {
			schema = schemaFactory.newSchema(schemaFile);
		} catch (SAXException e) {
			throw new EPSCommonException("A schema file could not be parsed.", e);
		}

		Marshaller marshaller;
		try {
			marshaller = getContext(instance.getClass()).createMarshaller();
		} catch (Exception e) {
			throw new EPSCommonException("A marshaller instance could not be created for class " + instance.getClass().getCanonicalName(),e);
		}

		writeCopyright(output);

		marshaller.setSchema(schema);
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
		marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		marshaller.marshal(instance, output);
	}

    public <T> void marshal(T instance, OutputStream output) throws JAXBException {

		Marshaller marshaller;
		try {
			marshaller = getContext(instance.getClass()).createMarshaller();
		} catch (Exception e) {
			throw new EPSCommonException("A marshaller instance could not be created for class " + instance.getClass().getCanonicalName(),e);
		}

		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		marshaller.marshal(instance, output);
	}

	public static XMLTransmitter getTransmitter(){
		return instance;
	}

    /**
     * @param output
     * @throws FactoryConfigurationError
     */
    private void writeCopyright(OutputStream output) throws FactoryConfigurationError {
        try {
    	    XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
    	    XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(output);//;
    	    try {
    	        xmlStreamWriter.writeStartDocument(Charset.defaultCharset().name(), "1.0");
    	        xmlStreamWriter.writeCharacters("\r\n");
    	        xmlStreamWriter.writeComment("\r\n" +
                        "  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\r\n" +
                        "  ~ Copyright (c) 2009-" + DateTimeFormatter.ofPattern("yyyy").format(DateTimeUtility.nowLocalDate()) + ", Exactpro Systems LLC\r\n" +
                        "  ~ www.exactpro.com\r\n" +
                        "  ~ Build Software to Test Software\r\n" +
                        "  ~ \r\n" +
                        "  ~ All rights reserved.\r\n" +
                        "  ~ This is unpublished, licensed software, confidential and proprietary\r\n" +
                        "  ~ information which is the property of Exactpro Systems LLC or its licensors.\r\n" +
                        "  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\r\n");
    	    } finally {
    	        xmlStreamWriter.flush();
                xmlStreamWriter.close();
    	    }
    	} catch (XMLStreamException e) {
    	    throw new EPSCommonException("A XML stream writer instance could not be created", e);
    	}
    }
}
