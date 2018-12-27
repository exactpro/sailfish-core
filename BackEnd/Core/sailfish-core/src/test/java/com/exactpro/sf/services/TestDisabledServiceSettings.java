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

import com.exactpro.sf.aml.DictionarySettings;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.StaticServiceDescription;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.services.IStaticServiceManager;
import com.exactpro.sf.storage.util.ServiceStorageHelper;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestDisabledServiceSettings {
    private File enabledExpectedFile = new File("src/test/resources/services/enabled_expected.xml");
    private File disabledExpectedFile = new File("src/test/resources/services/disabled_expected.xml");

    private File enabledActualFile;
    private File disabledActualFile;

    private final SailfishURI serviceURI = SailfishURI.unsafeParse("test:enabled_service");
    private final SailfishURI dictionaryURI = SailfishURI.unsafeParse("test_dictionary");

    private final DictionarySettings dictionarySettings = new DictionarySettings();
    private final IDictionaryManager dictionaryManager = Mockito.mock(IDictionaryManager.class);
    private final IStaticServiceManager serviceManager = new ServiceManagerWithEnabled();
    private final IStaticServiceManager disabledServiceManager = Mockito.mock(IStaticServiceManager.class);

    private final ServiceMarshalManager enabledMarshalManager =
            new ServiceMarshalManager(serviceManager, dictionaryManager);
    private final ServiceMarshalManager disabledMarshalManager =
            new ServiceMarshalManager(disabledServiceManager, dictionaryManager);

    {
        dictionarySettings.setURI(dictionaryURI);
        Mockito.when(dictionaryManager.getSettings(serviceURI)).then(
                (Answer<DictionarySettings>) invocation -> dictionarySettings
        );
        Mockito.when(disabledServiceManager.createServiceSettings(serviceURI)).then(
                (Answer<IServiceSettings>) invocation -> new DisabledServiceSettings()
        );
    }

    @Before
    public void setUp() throws Exception {
        this.enabledActualFile = File.createTempFile("enabled_actual", ".xml");
        this.disabledActualFile = File.createTempFile("disabled_actual", ".xml");
    }

    @After
    public void tearDown() throws Exception {
        this.enabledActualFile.delete();
        this.disabledActualFile.delete();
    }

    @Test
    public void testEnabledToEnabled() throws Exception {
        ServiceDescription enabledDescription = createdEnabledDescription();

        // export
        Map<String, File> stringFileMap = enabledMarshalManager.exportServices(Arrays.asList(enabledDescription));
        Assert.assertEquals(1, stringFileMap.size());

        Map.Entry<String, File> next = stringFileMap.entrySet().iterator().next();
        File exported = next.getValue();
        Assert.assertTrue(FileUtils.contentEquals(enabledExpectedFile, exported));
        exported.deleteOnExit();

        // import
        try (FileInputStream inputStream = new FileInputStream(exported)) {
            List<ServiceDescription> results = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            enabledMarshalManager.unmarshalServices(inputStream, false, results, errors);

            Assert.assertEquals(1, results.size());
            Assert.assertEquals(0, errors.size());

            ServiceDescription unmarshalledDescription = results.get(0);
            compareEnabledDescriptions(enabledDescription, unmarshalledDescription);
        }
    }

    @Test
    public void testDisabledToDisabled() throws Exception {
        ServiceDescription disabledDescription = createDisabledDescription();

        // export disabled and check with expected
        Map<String, File> stringFileMap = disabledMarshalManager.exportServices(Arrays.asList(disabledDescription));
        Assert.assertEquals(1, stringFileMap.size());

        Map.Entry<String, File> next = stringFileMap.entrySet().iterator().next();
        File exported = next.getValue();
        Assert.assertTrue(FileUtils.contentEquals(disabledExpectedFile, exported));
        exported.deleteOnExit();

        // export disabled and check with expected
        Unmarshaller unmarshaller = disabledMarshalManager.createUnmarshaller(ServiceDescription.class, DisabledServiceSettings.class);
        disabledDescription = (ServiceDescription)unmarshall(unmarshaller, exported);
        ServiceDescription unmarshalledDescription = (ServiceDescription)unmarshall(unmarshaller, disabledExpectedFile);
        compareDescriptionHeads(disabledDescription, unmarshalledDescription);

        DisabledServiceSettings expectedSettings = (DisabledServiceSettings)disabledDescription.getSettings();
        DisabledServiceSettings actualSettings = (DisabledServiceSettings)unmarshalledDescription.getSettings();

        Assert.assertEquals(expectedSettings.getSettings(), actualSettings.getSettings());

        // import disabled by ServiceMarshalManager - must throw ClassNotFoundException
        try (FileInputStream inputStream = new FileInputStream(exported)) {
            List<ServiceDescription> results = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            disabledMarshalManager.unmarshalServices(inputStream, false, results, errors);

            Assert.assertEquals(0, results.size());
            Assert.assertEquals(1, errors.size());

            String expectedError = "Could not import service. Reason: Cannot find settings class for service URI:" +
                    " test:enabled_service";
            Assert.assertEquals(expectedError, errors.get(0));
        }
    }

    @Test
    public void testEnabledToDisabled() throws IOException {
        ServiceDescription description = createDisabledDescription();

        // export disabled and check with expected
        Map<String, File> stringFileMap = disabledMarshalManager.exportServices(Arrays.asList(description));
        Assert.assertEquals(1, stringFileMap.size());

        Map.Entry<String, File> next = stringFileMap.entrySet().iterator().next();
        File exported = next.getValue();
        Assert.assertTrue(FileUtils.contentEquals(disabledExpectedFile, exported));
        exported.deleteOnExit();
    }

    @Test
    public void testDisabledToEnabled() throws IOException {
        ServiceDescription enabledDescription = createdEnabledDescription();

        // import disabled to enabled and check with expected
        try (FileInputStream inputStream = new FileInputStream(disabledExpectedFile)) {
            List<ServiceDescription> results = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            enabledMarshalManager.unmarshalServices(inputStream, false, results, errors);

            Assert.assertEquals(1, results.size());
            Assert.assertEquals(0, errors.size());

            ServiceDescription unmarshalledDescription = results.get(0);
            compareEnabledDescriptions(enabledDescription, unmarshalledDescription);
        }
    }

    private void compareEnabledDescriptions(ServiceDescription expected, ServiceDescription actual) {
        compareDescriptionHeads(expected, actual);

        EnabledSettings expectedSettings = (EnabledSettings) expected.getSettings();
        EnabledSettings actualSettings = (EnabledSettings) expected.getSettings();

        Assert.assertEquals(expectedSettings.getDictionaryName(), actualSettings.getDictionaryName());
        Assert.assertEquals(expectedSettings.getExpectedTimeOfStarting(), actualSettings.getExpectedTimeOfStarting());
        Assert.assertEquals(expectedSettings.getWaitingTimeBeforeStarting(), actualSettings.getWaitingTimeBeforeStarting());
        Assert.assertEquals(expectedSettings.getComment(), actualSettings.getComment());
        Assert.assertEquals(expectedSettings.getIntegerSetting(), actualSettings.getIntegerSetting());
        Assert.assertEquals(expectedSettings.getStringSetting(), actualSettings.getStringSetting());
        Assert.assertEquals(expectedSettings.isBoolSetting(), actualSettings.isBoolSetting());
        Assert.assertEquals(expectedSettings.isPerformDump(), actualSettings.isPerformDump());
        Assert.assertEquals(expectedSettings.getStoredMessageTypes(), actualSettings.getStoredMessageTypes());
    }

    private void compareDescriptionHeads(ServiceDescription expected, ServiceDescription actual) {
        Assert.assertEquals(expected.getName(), actual.getName());
        Assert.assertEquals(expected.getType(), actual.getType());
        Assert.assertEquals(expected.getServiceHandlerClassName(), actual.getServiceHandlerClassName());
        Assert.assertEquals(expected.getEnvironment(), actual.getEnvironment());
    }

    private void marshall(Marshaller marshaller, Object source, File targetFile) {
        try {
            marshaller.marshal(source, targetFile);
        } catch (JAXBException e) {
            Assert.fail(String.format("Could not marshall [%s]. [%s]", source, e));
        }
    }

    private Object unmarshall(Unmarshaller unmarshaller, File sourceFile) {
        try {
            return unmarshaller.unmarshal(sourceFile);
        } catch (JAXBException e) {
            Assert.fail(String.format("Could not unmarshall [%s]. [%s]", sourceFile, e));
        }
        return null;
    }

    private ServiceDescription createdEnabledDescription() {
        EnabledSettings settings = new EnabledSettings();
        settings.setBoolSetting(true);
        settings.setDictionaryName(SailfishURI.unsafeParse("test_dictionary"));
        settings.setIntegerSetting(101);
        settings.setStringSetting("some string");
        settings.setComment("comment");
        settings.setExpectedTimeOfStarting(555);
        settings.setWaitingTimeBeforeStarting(777);
        settings.setStoredMessageTypes("Message1, Message2");

        ServiceDescription enabledDescription = new ServiceDescription();
        enabledDescription.setSettings(settings);
        enabledDescription.setEnvironment("test_environment");
        enabledDescription.setName("enabled");
        enabledDescription.setServiceHandlerClassName("handler");
        enabledDescription.setType(SailfishURI.unsafeParse("test:enabled_service"));

        return enabledDescription;
    }

    private ServiceDescription createDisabledDescription() {
        ServiceDescription disabledDescription = createdEnabledDescription();

        Map<String, String> parameters = new HashMap<>();
        parameters.put("comment", "comment");
        parameters.put("dictionaryName", "test_dictionary");
        parameters.put("expectedTimeOfStarting", "555");
        parameters.put("performDump", "false");
        parameters.put("waitingTimeBeforeStarting", "777");
        parameters.put("boolSetting", "true");
        parameters.put("integerSetting", "101");
        parameters.put("stringSetting", "some string");
        parameters.put("storedMessageTypes", "Message1, Message2");

        DisabledServiceSettings disabledSettings = new DisabledServiceSettings();
        disabledSettings.setSettings(parameters);
        disabledDescription.setSettings(disabledSettings);

        return disabledDescription;
    }

    private class ServiceManagerWithEnabled implements IStaticServiceManager {
        private final List<StaticServiceDescription> descriptions = new ArrayList<>();
        private final SailfishURI[] serviceURIs = new SailfishURI[]{SailfishURI.unsafeParse("test:enabled_service")};

        {
            StaticServiceDescription description = new StaticServiceDescription(
                    serviceURIs[0], null, null, "description", null, EnabledSettings.class.getCanonicalName(),
                    SailfishURI.unsafeParse("test_dictionary"), null);
            descriptions.add(description);
        }

        @Override
        public List<StaticServiceDescription> getStaticServicesDescriptions() {
            return descriptions;
        }

        @Override
        public SailfishURI[] getServiceURIs() {
            return serviceURIs;
        }

        @Override
        public StaticServiceDescription findStaticServiceDescription(SailfishURI serviceURI) {
            if (serviceURI.equals(serviceURIs[0])) {
                return descriptions.get(0);
            }
            return null;
        }

        @Override
        public IService createService(SailfishURI serviceURI) {
            return null;
        }

        @Override
        public IServiceSettings createServiceSettings(SailfishURI serviceURI) {
            if (serviceURI.equals(serviceURIs[0])) {
                return new EnabledSettings();
            }
            return null;
        }

        @Override
        public IServiceHandler createServiceHandler(SailfishURI serviceURI, String handlerClassName) {
            return null;
        }

        @Override
        public IDictionaryValidator createDictionaryValidator(SailfishURI serviceURI) {
            return null;
        }
    }

}

