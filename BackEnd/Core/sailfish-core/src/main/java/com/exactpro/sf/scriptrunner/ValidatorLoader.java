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
package com.exactpro.sf.scriptrunner;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.IValidator;
import com.exactpro.sf.aml.validator.ValidatorDefinition;
import com.exactpro.sf.aml.validator.Validators;
import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.common.util.EPSCommonException;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class ValidatorLoader {

	private static final Logger logger = LoggerFactory.getLogger(ValidatorLoader.class);

	private final List<IValidator> references = new ArrayList<>();
	
	private final ListMultimap<IVersion, IValidator> pluginToValidators = ArrayListMultimap.create();

	public List<IValidator> getValidators() {
		return references;
	}
	
	public ListMultimap<IVersion, IValidator> getPluginToValidatorsMap() {
	    return pluginToValidators;
	}

	public void loadValidator(ClassLoader loader, InputStream stream, IVersion version) {
		try {
			JAXBContext jc = JAXBContext.newInstance(Validators.class);
			Unmarshaller u = jc.createUnmarshaller();

			JAXBElement<Validators> root = u.unmarshal(new StreamSource(stream), Validators.class);
			Validators validators = root.getValue();

			for (ValidatorDefinition def : validators.getValidator()) {
				Class<?> cls = loader.loadClass(def.getValidatorClassName().getName());
				try {
					IValidator validator = (IValidator) cls.newInstance();
					if (!isUnique(validator.getName())) {
						logger.warn("Duplicate Validator loading {}", validator.getName());
					}
					
					pluginToValidators.put(version, validator);
					references.add(validator);
					
				} catch (InstantiationException e) {
					throw new EPSCommonException("Can not create instantance for class "+cls.getCanonicalName(), e);
				} catch (IllegalAccessException e) {
					throw new EPSCommonException("IllegalAccessException for class: "+cls.getCanonicalName(), e);
				}
			}
			
		} catch (ClassNotFoundException e) {
			throw new EPSCommonException("Can not load class", e);
		} catch (JAXBException e) {
			throw new EPSCommonException("Can not read validators file", e);
		}
	}
	
	private boolean isUnique(String name) {
		for (IValidator validator : references) {
			if (name.equals(validator.getName())) {
				return false;
			}
		}
		return true;
	}

}
