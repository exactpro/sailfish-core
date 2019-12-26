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
package com.exactpro.sf.common.messages.structures.loaders;

import com.exactpro.sf.common.impl.messages.xml.XMLTransmitter;
import com.exactpro.sf.common.impl.messages.xml.configuration.Dictionary;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.StringUtil;
import java.io.InputStream;
import javax.xml.bind.JAXBException;
import org.xml.sax.SAXParseException;

/**
 * Java class for load {@link IDictionaryStructure} from XML format.
 */
public class XmlDictionaryStructureLoader extends AbstractDictionaryStructureLoader {

	public static final String DEFAULT_SCHEMA_VALIDATOR = "/xsd/dictionary.xsd";

	public XmlDictionaryStructureLoader() {
		super();
	}

	public XmlDictionaryStructureLoader(boolean aggregateAttributes) {
		super(aggregateAttributes);
	}

	/**
	 * Get {@link Dictionary} from {@link InputStream}.
	 * @param inputStream {@link InputStream}
	 * @return {@link Dictionary}
	 */
	public Dictionary getDictionaryFromGeneratedClass(InputStream inputStream) {
		InputStream schemaStream = getClass().getResourceAsStream(DEFAULT_SCHEMA_VALIDATOR);

		XMLTransmitter transmitter = XMLTransmitter.getTransmitter();

		ClassLoader currentThreadClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
			
			return transmitter.unmarshal(Dictionary.class, inputStream, schemaStream, null);

		} catch (JAXBException jabEx) {

			SAXParseException saxParseException = null;

			if(jabEx.getCause() != null && (jabEx.getCause() instanceof SAXParseException)) {
				saxParseException = (SAXParseException)jabEx.getCause();
			} else if(jabEx.getLinkedException() != null
					&& (jabEx.getLinkedException() instanceof SAXParseException)) {
				saxParseException = (SAXParseException)jabEx.getLinkedException();
			}

			if(saxParseException != null) {

				String strError = String.format("%4$sThis xml stream structure could not be unmarshaled due to SAXParser error %4$s[%1$s].%4$sError was found on the line [%2$d], column [%3$d].%4$s",
						saxParseException.getMessage(), saxParseException.getLineNumber(),
						saxParseException.getColumnNumber(), StringUtil.EOL);

				throw new EPSCommonException(strError, jabEx);

			} else {
				throw new EPSCommonException("This xml stream structure could not be unmarshaled due to SAXParser error.", jabEx);
			}

		} catch (Exception e) {
			throw new EPSCommonException("This xml stream structure could not be unmarshaled.", e);
		} finally {
			Thread.currentThread().setContextClassLoader(currentThreadClassLoader);
		}
	}

	@Override
	public Dictionary getDictionary(InputStream inputStream) {
		return getDictionaryFromGeneratedClass(inputStream);
	}
}
