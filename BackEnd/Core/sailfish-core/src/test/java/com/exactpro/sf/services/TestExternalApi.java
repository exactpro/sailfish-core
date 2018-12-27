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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.externalapi.IMessageFactoryProxy;
import com.exactpro.sf.externalapi.IServiceFactory;
import com.exactpro.sf.externalapi.IServiceProxy;
import com.exactpro.sf.externalapi.ISettingsProxy;
import com.exactpro.sf.externalapi.ServiceFactory;
import com.exactpro.sf.externalapi.impl.EmptyListener;

/**
 * @author sergey.smirnov
 *
 */
@RunWith(Parameterized.class)
public class TestExternalApi {

    private File writableLayer = Paths.get("build", "tmp", "test_external_api").toFile();
    private ServiceName serviceName = new ServiceName("env", "serviceName");
    private SailfishURI serviceType;
    private SailfishURI testServiceType;
    private SailfishURI testDictionary;
    private SailfishURI dictionary;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Parameterized.Parameter
    public boolean strict = true;

    @Parameterized.Parameters(name = "strictMode = {0}")
    public static Collection<?> data() {
        return Arrays.asList(false, true);
    }

    @Before
    public void before() throws SailfishURIException, IOException {
        if (writableLayer.exists()) {
            FileUtils.deleteDirectory(writableLayer);
        }
        
        this.serviceType = new SailfishURI(IVersion.GENERAL, null, "FAKE_CLIENT_SERVICE");
        this.dictionary = new SailfishURI(IVersion.GENERAL, null, "TestAML");
        this.testServiceType = new SailfishURI(IVersion.GENERAL, null, "TEST_SERVICE");
        this.testDictionary = new SailfishURI(IVersion.GENERAL, null, "Test");
    }
    
    private IServiceFactory createServiceFactory(boolean useResourceLayer, boolean useTestLayer) throws Exception {
        File[] layers = useTestLayer 
                ? new File[] { new File("src/test/workspace"), writableLayer } 
                : new File[] { writableLayer };
        return new ServiceFactory(0, 2, 2, useResourceLayer,strict, layers);
    }

    /**
     * @param sf
     */
    private void checkComponents(IServiceFactory sf) {
        Assert.assertTrue(sf.getServiceTypes().contains(serviceType));
        Assert.assertTrue(sf.getDictionaries().contains(dictionary));
    }
    
    private IServiceFactory createServiceFactory() throws Exception {
        IServiceFactory sf = createServiceFactory(false, true);
        checkComponents(sf);
        return sf;
    }

    @Test
    public void testFromResources() {
        IServiceProxy sp = null;
        try (IServiceFactory sf = createServiceFactory()) {
            sp = sf.createService(new FileInputStream(new File("src/test/resources/fake.xml")), new EmptyListener() {
                @Override
                public void onMessage(IServiceProxy service, IMessage message, boolean rejected, ServiceHandlerRoute route) {
                    Assert.assertTrue("SimpleMessage".equals(message.getName()));
                    Assert.assertTrue("TestAML".equals(message.getNamespace()));
                }
            });
            Path uploaded = new File("src/test/workspace/cfg/dictionaries/test_aml.xml").toPath();
            SailfishURI newDictionary = sf.registerDictionary("uploaded", Files.newInputStream(uploaded), false);
            IMessageFactoryProxy messageFactory = sf.getMessageFactory(sp.getType());
            IMessage simpleMessage = messageFactory.createMessage(newDictionary, "SimpleMessage");
            sp.start();
            sp.send(simpleMessage);
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            if (ServiceStatus.STARTED.equals(sp.getStatus())) {
                sp.stop();
            }
        }
    }

    @Test
    public void test() {
        IServiceProxy sp = null;
        try (IServiceFactory sf = createServiceFactory()) {
            sp = sf.createService(new FileInputStream(new File("src/test/resources/fake.xml")), new EmptyListener());
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            if (ServiceStatus.STARTED.equals(sp.getStatus())) {
                sp.stop();
            }
        }
    }

    @Test
    public void testResourceLayer() {
        IServiceProxy sp = null;
        try (IServiceFactory sf = createServiceFactory(true, false)) {
            Assert.assertTrue("Available service", sf.getServiceTypes().contains(testServiceType));
            Assert.assertTrue("Available dictionary", sf.getDictionaries().contains(testDictionary));
            sp = sf.createService(serviceName, testServiceType, new EmptyListener());
            sp.getSettings().setParameterValue("dictionaryName", testDictionary);
            sp.start();
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            if (sp != null && ServiceStatus.STARTED.equals(sp.getStatus())) {
                sp.stop();
            }
        }
    }

    @Test
    public void testStream() {
        IServiceFactory ssf;
        try {
            ssf = new ServiceFactory(0, 2, 2, new File("src/test/workspace"), Files.createTempDirectory("sf-tests").toFile());
            ssf.createService(new FileInputStream(new File("src/test/resources/fake.xml")), new EmptyListener());
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

    }

    @Test
    public void testMessageFactory() {

        Assume.assumeFalse(strict);

        IServiceProxy sp = null;
        try (IServiceFactory sf = createServiceFactory()) {
            sp = sf.createService(new FileInputStream(new File("src/test/resources/fake.xml")), new EmptyListener());

            IMessage msg = sf.getMessageFactory(serviceType).createMessage(sp.getSettings().getDictionary(), "Test");
            boolean catched = false;
            try {
                Assert.assertEquals(msg, sp.send(msg));
            } catch (IllegalStateException e) {
                catched = true;
            }
            Assert.assertTrue(catched);
            sp.start();
            Assert.assertEquals(msg, sp.send(msg));

        } catch (Throwable e1) {
            e1.printStackTrace();
            Assert.fail(e1.getMessage());
        } finally {
            if (ServiceStatus.STARTED.equals(sp.getStatus())) {
                sp.stop();
            }
        }
    }

    @Test
    public void testMessageFactoryStrict() throws Exception {

        Assume.assumeTrue(strict);

        exception.expect(EPSCommonException.class);
        exception.expectMessage("Can't find structure for message Test");

        IServiceProxy sp = null;
        try (IServiceFactory sf = createServiceFactory()) {
            sp = sf.createService(new FileInputStream(new File("src/test/resources/fake.xml")), new EmptyListener());

            IMessage msg = sf.getMessageFactory(serviceType).createMessage(sp.getSettings().getDictionary(), "Test");
            boolean catched = false;
            try {
                Assert.assertEquals(msg, sp.send(msg));
            } catch (IllegalStateException e) {
                catched = true;
            }
            Assert.assertTrue(catched);
        }
    }

    @Test
    public void testServiceProxy() {
        IServiceProxy sp = null;
        try (IServiceFactory sf = createServiceFactory()) {
            sp = sf.createService(new FileInputStream(new File("src/test/resources/fake.xml")), new EmptyListener());

            sp.start();
            Assert.assertEquals(ServiceStatus.STARTED, sp.getStatus());
            sp.stop();
            Assert.assertEquals(ServiceStatus.DISPOSED, sp.getStatus());
            sp.start();
            Assert.assertEquals(ServiceStatus.STARTED, sp.getStatus());
            boolean catched = false;
            try {
                sp.start();
            } catch (IllegalStateException e) {
                catched = true;
            }
            Assert.assertEquals(true, catched);

            sp.getName();
            sp.getSettings().getParameterNames();
            sp.getType();
        } catch (Throwable e1) {
            e1.printStackTrace();
            Assert.fail(e1.getMessage());
        } finally {
            if (ServiceStatus.STARTED.equals(sp.getStatus())) {
                sp.stop();
            }
        }
    }

    @Test
    public void testRegisterDictionary() throws SailfishURIException {
        SailfishURI sURI = new SailfishURI(IVersion.GENERAL, null, "FAKE_DICTIONARY");
        
        try (IServiceFactory sf = createServiceFactory()) {
            int count = sf.getDictionaries().size();
            Assert.assertEquals(sURI, sf.registerDictionary(sURI.getResourceName(), new FileInputStream(new File("src/test/workspace/cfg/dictionaries/example.xml")), false));
            Assert.assertEquals(count + 1, sf.getDictionaries().size());
            try {
                sf.registerDictionary(sURI.getResourceName(), new FileInputStream(new File("src/test/workspace/cfg/dictionaries/example.xml")), false);
                Assert.fail("Overwrite dictionary");
            } catch (Exception e) {
                //Expected
            }
            Assert.assertEquals(sURI, sf.registerDictionary(sURI.getResourceName(), new FileInputStream(new File("src/test/workspace/cfg/dictionaries/example.xml")), true));
        } catch (Throwable e1) {
            e1.printStackTrace();
            Assert.fail(e1.getMessage());
        }

        try (IServiceFactory sf = createServiceFactory()) {
            Assert.assertTrue("Check registered dictionary", sf.getDictionaries().contains(sURI));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Reinitialisation after dictionary registration");
        }
    }

    @Test
    public void testSettingsProxy() {

        IServiceProxy sp = null;
        try (IServiceFactory sf = createServiceFactory()) {
            sp = sf.createService(new FileInputStream(new File("src/test/resources/fake.xml")), new EmptyListener());
            ISettingsProxy ssp = sp.getSettings();

            for (String name : ssp.getParameterNames()) {
                Object val = ssp.getParameterValue(name);
                Class<?> klass = ssp.getParameterType(name);
                ssp.setParameterValue(name, val);
                System.out.println(String.format("%s = %s. type: %s", name, val, klass.getCanonicalName()));
            }
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            if (ServiceStatus.STARTED.equals(sp.getStatus())) {
                sp.stop();
            }
        }

    }

}
