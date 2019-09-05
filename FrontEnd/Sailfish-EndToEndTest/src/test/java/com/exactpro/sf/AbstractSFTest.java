/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.sf.Service.Status;
import com.exactpro.sf.exceptions.APICallException;
import com.exactpro.sf.exceptions.APIResponseException;
import org.junit.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public abstract class AbstractSFTest {

    protected final static String CLIENT = "fake.xml";
    protected final static String SERVER = "FIXServerTest.xml";
    protected final static String CLIENT_NAME = "fake";
    protected final static String SERVER_NAME = "FIXServerTest";
    protected final static String SF_GUI_URL = System.getenv("SF_GUI_URL") == null 
            ? "http://localhost:8080/sfgui/" // for local run and debug
            : System.getenv("SF_GUI_URL");
    protected final static String SF_EXECUTOR_URL = System.getenv("SF_EXECUTOR_URL") == null 
            ? SF_GUI_URL
            : System.getenv("SF_EXECUTOR_URL");

    protected static void startService(SFAPIClient sfapiClient, String cfgName, String serviceName, String environment) throws Exception {
        // import
        Map<String, Service> services = sfapiClient.getServices(environment);
        if (!services.containsKey(serviceName)) {

            byte[] content = getByteContent(cfgName);
            try (InputStream inputStream = new ByteArrayInputStream(content)) {
                List<ServiceImportResult> importedServices = sfapiClient.importServices(cfgName, environment, inputStream, false, false);

                if (importedServices.isEmpty())
                    Assert.fail("There is no service in " + cfgName);
            }

            awaitServiceStatus(sfapiClient, environment, serviceName, 10000, Status.INITIALIZED);
        }

        // start
        sfapiClient.startService(environment, serviceName);
        awaitServiceStatus(sfapiClient, environment, serviceName, 10000, Status.STARTED);
    }

    protected static byte[] getByteContent(String serviceFile) throws IOException {
        InputStream serviceStream = AbstractSFTest.class.getClassLoader().getResourceAsStream(serviceFile);
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n = 0;
        while ((n = serviceStream.read(buf)) >= 0)
            arrayOutputStream.write(buf, 0, n);
        return arrayOutputStream.toByteArray();
    }

    protected void checkErrorMessage(Throwable e, String expectedMessage) {
        Assert.assertTrue(String.format("Exception message \"%s\" doesn't contain \"%s\"", e.getMessage(), expectedMessage), e.getMessage().contains(expectedMessage));
    }

    protected static void awaitServiceStatus(SFAPIClient sfapi, String environment, String serviceName, long await, Status expectedStatus) throws APIResponseException, APICallException, InterruptedException {
        int sleepTime = 100;
        long hop = await / sleepTime;
        while (hop > 0 && !expectedStatus.equals(sfapi.getServices(environment).get(serviceName).getStatus())) {
            hop--;
            Thread.sleep(sleepTime);
        }
    }
}
