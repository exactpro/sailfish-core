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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.IPreprocessor;
import com.exactpro.sf.aml.preprocessor.PreprocessorDefinition;
import com.exactpro.sf.aml.preprocessor.Preprocessors;
import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.IDataManager;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class PreprocessorLoader {

	private static final Logger logger = LoggerFactory.getLogger(PreprocessorLoader.class);

	private final IDataManager dataManager;

	private final Map<String, Class<? extends IPreprocessor>> nameToClass = new HashMap<>();
	private final ListMultimap<IVersion, PreprocessorDefinition> pluginToPreprocessors = ArrayListMultimap.create();

	public PreprocessorLoader(IDataManager dataManager) {
		this.dataManager = dataManager;
	}

	public List<IPreprocessor> getPreprocessors() {
	    List<IPreprocessor> preprocessors = new ArrayList<IPreprocessor>();

	    for(Class<? extends IPreprocessor> clazz : nameToClass.values()) {
            try {
                IPreprocessor preprocessor = clazz.newInstance();

                preprocessor.init(dataManager);
                preprocessors.add(preprocessor);
            } catch(InstantiationException | IllegalAccessException e) {
                logger.error(e.getMessage(), e);
            }
	    }

		return preprocessors;
	}

	public ListMultimap<IVersion, PreprocessorDefinition> getPluginToPreprocessorsMap() {
        return pluginToPreprocessors;
	}

	@SuppressWarnings("unchecked")
    public void loadPreprocessors(ClassLoader loader, InputStream stream, IVersion version) {
		try {
			JAXBContext jc = JAXBContext.newInstance(Preprocessors.class);
			Unmarshaller u = jc.createUnmarshaller();

			JAXBElement<Preprocessors> root = u.unmarshal(new StreamSource(stream), Preprocessors.class);
			Preprocessors preprocessors = root.getValue();

			for(PreprocessorDefinition validator : preprocessors.getPreprocessor()) {
				Class<?> cls = loader.loadClass(validator.getPreprocessorClassName().getName());
				try {
					IPreprocessor preprocessor = (IPreprocessor)cls.newInstance();
					String name = preprocessor.getName();

					if(nameToClass.containsKey(name)) {
						logger.warn("Duplicate Preprocessor loading {}", name);
					}

					pluginToPreprocessors.put(version, validator);
					nameToClass.put(name, (Class<? extends IPreprocessor>)cls);
				} catch (InstantiationException e) {
					throw new EPSCommonException("Can not create instantance for class "+cls.getCanonicalName(), e);
				} catch (IllegalAccessException e) {
					throw new EPSCommonException("IllegalAccessException for class: "+cls.getCanonicalName(), e);
				}
			}
		} catch (ClassNotFoundException e) {
			throw new EPSCommonException("Can not load class", e);
		} catch (JAXBException e) {
			throw new EPSCommonException("Can not read preprocessors file", e);
		}
	}
}
