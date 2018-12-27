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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

import com.exactpro.sf.common.impl.messages.xml.configuration.Dictionary;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.common.util.EPSCommonException;

public class XmlDictionaryWriter {
	
	public static void write(Dictionary dictionary, OutputStream output) {
		XMLTransmitter transmitter = XMLTransmitter.getTransmitter();
		
		File schema;
		try {
			schema = File.createTempFile("dictionary", ".xsd");
			schema.delete();
		} catch (IOException e) {
			throw new EPSCommonException("We couldn't create temporary file", e);
		}
		
		try {
			Files.copy(XmlDictionaryWriter.class.getResourceAsStream(XmlDictionaryStructureLoader.DEFAULT_SCHEMA_VALIDATOR), 
					schema.toPath());
		} catch (IOException e) {
			throw new EPSCommonException("We couldn't copy a schema file",e);
		}
		
		try {
			transmitter.marshal(dictionary, output, schema);
		} catch (Exception e) {
			throw new EPSCommonException("A dictionary convertation exception", e);
		}
	}
	
	public static void write(Dictionary dictionary, File file) {
		try {
			write(dictionary, new FileOutputStream(file));
		} catch (FileNotFoundException e) {
			throw new EPSCommonException("A dictionary convertation exception", e);
		}
	}
	
	public static void unprotectedWrite(Dictionary dictionary, File file) {
		XMLTransmitter transmitter = XMLTransmitter.getTransmitter();
		
		try {
			transmitter.marshal(dictionary, file);
		} catch (Exception e) {
			throw new EPSCommonException("A dictionary convertation exception", e);
		}
	}
}
