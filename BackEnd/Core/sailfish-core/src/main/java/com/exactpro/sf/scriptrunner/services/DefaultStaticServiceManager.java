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
package com.exactpro.sf.scriptrunner.services;

import java.io.InputStream;
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

import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.ILoadableManager;
import com.exactpro.sf.configuration.ILoadableManagerContext;
import com.exactpro.sf.configuration.StaticServiceDescription;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidatorFactory;
import com.exactpro.sf.configuration.services.ServiceDefinition;
import com.exactpro.sf.configuration.services.Services;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.configuration.suri.SailfishURIRule;
import com.exactpro.sf.configuration.suri.SailfishURIUtils;
import com.exactpro.sf.services.DisabledService;
import com.exactpro.sf.services.DisabledServiceSettings;
import com.exactpro.sf.services.IService;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.IServiceSettings;
import com.google.common.collect.ImmutableList;

public class DefaultStaticServiceManager implements IStaticServiceManager, ILoadableManager {

	private static final Logger logger = LoggerFactory.getLogger(DefaultStaticServiceManager.class);

	private final Map<SailfishURI, StaticServiceDescription> descriptions = new HashMap<>();

    @Override
    public void load(ILoadableManagerContext context) {
    	
        IVersion version = context.getVersion();
        InputStream stream = context.getResourceStream();
        ClassLoader classLoader = context.getClassLoaders()[0];
        
    	try {
			JAXBContext jc = JAXBContext.newInstance(Services.class);
			Unmarshaller u = jc.createUnmarshaller();

			JAXBElement<Services> root = u.unmarshal(new StreamSource(stream), Services.class);
			Services services = root.getValue();

			for (ServiceDefinition sd : services.getService()) {

			    if (sd.getName() == null) {
                    throw new EPSCommonException("Null name in config");
			    }
			    if (sd.getClassName() == null) {
                    throw new EPSCommonException("Null ClassName in config");
                }
			    if (sd.getSettingsClassName() == null) {
			        throw new EPSCommonException("Null settingsClassName");
			    }

			    String safeName = sd.getName().replaceAll("\\W", "_");
			    SailfishURI uri = new SailfishURI(version.getAlias(), null, safeName);

				StaticServiceDescription descr = new StaticServiceDescription(
						uri,
						classLoader,
						version,
						sd.getDescription(),
						sd.getClassName(),
						sd.getSettingsClassName(),
						SailfishURI.parse(sd.getDictionary()),
						sd.getDictionaryValidatorFactoryName());

				if (descriptions.put(uri, descr) != null) {
					logger.warn("Duplicate ServiceDescription loading {}", uri);
				}
			}
		} catch (JAXBException | SailfishURIException e) {
			throw new EPSCommonException("Failed to load services", e);
		}
	}

    @Override
    public void finalize(ILoadableManagerContext context) throws Exception {
        // TODO Auto-generated method stub
    }

	@Override
	public List<StaticServiceDescription> getStaticServicesDescriptions() {
		return ImmutableList.copyOf(descriptions.values());
	}

	@Override
	public StaticServiceDescription findStaticServiceDescription(SailfishURI serviceURI) {
	    return SailfishURIUtils.getMatchingValue(serviceURI, descriptions, SailfishURIRule.REQUIRE_RESOURCE);
	}

	@Override
	public IDictionaryValidator createDictionaryValidator(SailfishURI serviceURI) {
       StaticServiceDescription description = SailfishURIUtils.getMatchingValue(serviceURI, descriptions, SailfishURIRule.REQUIRE_RESOURCE);

        if (description == null) {
            logger.warn("Could not find service description for service = [{}]", serviceURI);
            return null;
        }

        ClassLoader classLoader = description.getClassLoader();
        String dictionaryValidatorClassName = description.getDictionaryValidatorFactoryName();

        if (dictionaryValidatorClassName != null) {
			IDictionaryValidatorFactory factory = newInstance(classLoader, dictionaryValidatorClassName);

			if(factory != null) {
				return factory.createDictionaryValidator();
			}
        }

        return null;
	}

	@Override
	public IServiceSettings createServiceSettings(SailfishURI serviceURI) {
		StaticServiceDescription description = SailfishURIUtils.getMatchingValue(serviceURI, descriptions, SailfishURIRule.REQUIRE_RESOURCE);

		if (description == null) {
			logger.warn("Could not find service description for service = [{}]", serviceURI);
			return new DisabledServiceSettings();
		}

		ClassLoader classLoader = description.getClassLoader();
		String settingsClassName = description.getSettingsClassName();

		return newInstance(classLoader, settingsClassName);
	}

	@Override
	public IServiceHandler createServiceHandler(SailfishURI serviceURI, String handlerClassName) {
		StaticServiceDescription description = SailfishURIUtils.getMatchingValue(serviceURI, descriptions, SailfishURIRule.REQUIRE_RESOURCE);

		if (description == null) {
			logger.warn("Could not find service description for service = [{}]", serviceURI);
			return null;
		}

		ClassLoader classLoader = description.getClassLoader();

		return newInstance(classLoader, handlerClassName);
	}

	@Override
	public IService createService(SailfishURI serviceURI) {
		StaticServiceDescription description = SailfishURIUtils.getMatchingValue(serviceURI, descriptions, SailfishURIRule.REQUIRE_RESOURCE);

		if (description == null) {
			logger.info("Could not find service description for service = [{}]", serviceURI);
			return new DisabledService();
		}

		ClassLoader classLoader = description.getClassLoader();
		String serviceClassName = description.getClassName();

		return newInstance(classLoader, serviceClassName);
	}

	@SuppressWarnings("unchecked")
	private <T> T newInstance(ClassLoader classLoader, String className) throws EPSCommonException {
		if (classLoader == null) {
			throw new EPSCommonException("ClassLoader for instantation not specified");
		}
		if (className == null) {
			throw new EPSCommonException("ClassName for instantation not specified");
		}

		try {
			Class<?> serviceClass = classLoader.loadClass(className);
			return (T) serviceClass.newInstance();
		} catch (ClassNotFoundException e) {
			throw new EPSCommonException("Could not load [" + className + "] class", e);
		} catch (IllegalAccessException e) {
			throw new EPSCommonException("Could not instantiate [" + className + "] class", e);
		} catch (InstantiationException e) {
			throw new EPSCommonException("Could not instantiate [" + className + "] class", e);
		}
	}

	@Override
    public SailfishURI[] getServiceURIs() {
		return descriptions.keySet().toArray(new SailfishURI[descriptions.size()]);
	}
}
