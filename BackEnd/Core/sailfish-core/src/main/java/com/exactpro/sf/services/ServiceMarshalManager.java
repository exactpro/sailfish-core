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
package com.exactpro.sf.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.input.CloseShieldInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.configuration.suri.SailfishURIUtils;
import com.exactpro.sf.scriptrunner.services.IStaticServiceManager;
import com.exactpro.sf.storage.util.ServiceStorageHelper;


public class ServiceMarshalManager {
	private static final Logger logger = LoggerFactory.getLogger(ServiceMarshalManager.class);

	private DocumentBuilder domBuilder;
	private XPathExpression typeExpression;
	private IStaticServiceManager staticServiceManager;
	private IDictionaryManager dictionaryManager;

	public ServiceMarshalManager(IStaticServiceManager staticServiceManager, IDictionaryManager dictionaryManager) {
	    this.staticServiceManager = staticServiceManager;
	    this.dictionaryManager = dictionaryManager;

		try {
            domBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            typeExpression = XPathFactory.newInstance().newXPath().compile("serviceDescription/type");
        } catch(ParserConfigurationException | XPathExpressionException e) {
            throw new EPSCommonException(e);
		}
	}

	public Map<String, File> exportServices(List<ServiceDescription> descriptions) {
		logger.info("exportServices: begin");

		Map<String, File> services = new HashMap<>();

		for (ServiceDescription descr : descriptions) {
            try {
                Marshaller m = createMarshaller(ServiceDescription.class, DisabledServiceSettings.class, getServiceSettingsClass(descr.getType()));
                String name = descr.getName() + ".xml";
                File f = File.createTempFile(name, null);
                m.marshal(descr, f);
                services.put(name, f);
            } catch (JAXBException | IOException | SailfishURIException | ClassNotFoundException e) {
                logger.error("Could not marshal service description {}", descr.getName(), e);
            }
        }

		logger.info("exportServices: end");

		return services;
	}
	
	public File exportEnvironmentDescription(String fileName, EnvironmentDescription envDesc) {
		logger.info("exportEnvironmentDescription: begin");
		
		File f = null;
		
		try {
			f = File.createTempFile(fileName, null);
			createMarshaller(EnvironmentDescription.class).marshal(envDesc, f);
		} catch (JAXBException | IOException e) {
	        logger.error("Could not marshal environment description {}", envDesc.getName(), e);
	    }
        
        logger.info("exportEnvironmentDescription: end");
        
        return f;
	}

	public File packInZip(Map<String, File> files) {
		File archiveFile = null;

		try {
		    archiveFile = File.createTempFile("Archive", ".zip");
			ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(archiveFile));
			zos.setLevel(Deflater.DEFAULT_COMPRESSION);
			byte[] buf = new byte[1024];

			for (Entry<String, File> e : files.entrySet()) {
				FileInputStream fis = new FileInputStream(e.getValue());
				zos.putNextEntry(new ZipEntry(e.getKey()));
	               int len;
	               while ((len = fis.read(buf)) > 0)
	                  zos.write(buf, 0, len);
	               fis.close();
	               zos.closeEntry();
			}
			zos.flush();
			zos.close();
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

		return archiveFile;
	}

	public EnvironmentDescription unmarshalServices(InputStream stream, boolean isZip, List<ServiceDescription> results, List<String> errors) {
	    if (isZip) {
	        return unmarshalFromZip(stream, results, errors);
	    } else {
	        try {
	            results.add(unmarshalService(stream));
            } catch (JAXBException | XPathExpressionException | ClassNotFoundException | SAXException | IOException | SailfishURIException e) {
                errors.add("Could not import service. Reason: " + e.getMessage());
                logger.error(e.getMessage(), e);
            }
	        return null;
	    }
	}
	
	private ServiceDescription unmarshalService(InputStream stream) throws JAXBException, SAXException, IOException, SailfishURIException, XPathExpressionException, ClassNotFoundException {
	    Document document = domBuilder.parse(stream);
	    String type = (String)typeExpression.evaluate(document, XPathConstants.STRING);
	    SailfishURI uri = SailfishURI.parse(SailfishURIUtils.sanitize(type));
	    Class<?> settingsClass = getServiceSettingsClass(uri);

	    if(settingsClass == DisabledServiceSettings.class) {
	        throw new ClassNotFoundException("Cannot find settings class for service URI: " + uri);
	    }

	    Unmarshaller unmarshaller = createUnmarshaller(ServiceDescription.class, DisabledServiceSettings.class, settingsClass);
		ServiceDescription serviceDescription = (ServiceDescription)unmarshaller.unmarshal(document);
		IServiceSettings sourceSettings = serviceDescription.getSettings();

        if(ServiceStorageHelper.isDisabledSettings(sourceSettings)) {
            DisabledServiceSettings disabledSettings = (DisabledServiceSettings)sourceSettings;
            IServiceSettings targetSettings = staticServiceManager.createServiceSettings(serviceDescription.getType());

            ServiceStorageHelper.convertMapToServiceSettings(targetSettings, disabledSettings.getSettings());
            serviceDescription.setSettings(targetSettings);
        }

		return ServiceStorageHelper.processDescription(serviceDescription, staticServiceManager, dictionaryManager);
	}
	
	private EnvironmentDescription unmarshalEnvironment(InputStream stream) throws JAXBException {
		return (EnvironmentDescription)createUnmarshaller(EnvironmentDescription.class).unmarshal(stream);
	}

	private EnvironmentDescription unmarshalFromZip(InputStream stream, List<ServiceDescription> results, List<String> errors) {

		ZipInputStream zis = new ZipInputStream(stream);
		ZipEntry ze;
		EnvironmentDescription envDescr = null;

		try {
			while ((ze = zis.getNextEntry()) != null) { //TODO: rewrite without temp file

				try {

					if ( !ze.isDirectory() ) {
						
						if (ze.getName().equals("environment_description.xml")) {
							envDescr = unmarshalEnvironment(new CloseShieldInputStream(zis));
						} else {
							ServiceDescription descr = unmarshalService(new CloseShieldInputStream(zis));
							results.add(descr);
						}
						
						zis.closeEntry();
					}

				} catch (Exception e) {
					errors.add("Could not import service from file "+ ze.getName()+ ". ");
					logger.error(e.getMessage(), e);
				}
			}
		} catch (IOException e) { //zis.getNextEntry() handler
			errors.add(e.getMessage());
			logger.error(e.getMessage(), e);
		}
		
		return envDescr;
	}

	private Marshaller createMarshaller(Class<?>... classes) throws JAXBException {
	    Marshaller marshaller = JAXBContext.newInstance(classes).createMarshaller();
	    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
	    return marshaller;
	}

	public Unmarshaller createUnmarshaller(Class<?>...classes) throws JAXBException {
	    return JAXBContext.newInstance(classes).createUnmarshaller();
	}

	private Class<?> getServiceSettingsClass(SailfishURI uri) throws SailfishURIException, ClassNotFoundException {
        return staticServiceManager.createServiceSettings(uri).getClass();
	}
}
