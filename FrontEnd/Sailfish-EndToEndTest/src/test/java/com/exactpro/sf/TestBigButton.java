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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.exceptions.APICallException;
import com.exactpro.sf.testwebgui.restapi.xml.XmlBbExecutionStatus;
import com.exactpro.sf.testwebgui.restapi.xml.XmlLibraryImportResult;
import com.google.common.io.ByteStreams;

public class TestBigButton {

    private final static Logger logger = LoggerFactory.getLogger(TestBigButton.class.getName());
    private static SFAPIClient sfapi;

    @BeforeClass
    public static void setUpClass() throws Exception {
        logger.info("Start positive tests of matricies");
        try {
            sfapi = new SFAPIClient(TestMatrix.SF_GUI_URL);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        logger.info("Finish positive tests of matrix");
        sfapi.close();
    }

    @Test
    public void runBB() throws Exception {

        String statisticsSettings = new String(ByteStreams.toByteArray(getClass().getClassLoader().getResourceAsStream("statisticsDBSettings.xml")));
        try {
            sfapi.setStatisticsDBSettings(statisticsSettings);
        } catch (APICallException e) {
            if (e.getMessage().contains(" OK;")) {
                sfapi.forceMigrateStatistics();
            } else {
                logger.error(e.getMessage());
                throw e;
            }
        }

        File testSuites = Files.createTempFile(UUID.randomUUID().toString(), ".zip").toFile();

        try (ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(testSuites))) {

            Map<String, String> testLibraryMap = new HashMap<>();
            testLibraryMap.put("TestSuite1/csv/testMatrix.csv", "testMatrix.csv");
            testLibraryMap.put("TestSuite1/csv/testSendDirty.csv", "aml3positive/testSendDirty.csv");
            testLibraryMap.put("TestSuite1/csv/validTest.csv", "aml3positive/validTest.csv");
            testLibraryMap.put("TestSuite1/services/env/fake.xml", "fake.xml");
            testLibraryMap.put("TestSuite1/services/env/FIXServerTest.xml", "FIXServerTest.xml");

            for (Map.Entry<String, String> entry : testLibraryMap.entrySet()) {
                String k = entry.getKey();
                File v = new File(getClass().getClassLoader().getResource(entry.getValue()).getFile());
                zipStream.putNextEntry(new ZipEntry(k));
                Files.copy(v.toPath(), zipStream);
                zipStream.closeEntry();
            }
        }

        sfapi.uploadTestLibrary(new FileInputStream(testSuites), "TestSuite1.zip", true);
        XmlLibraryImportResult importResult = sfapi.uploadBBLibrary(new File(getClass().getClassLoader().getResource("testBB.csv").getFile()), "testBB.csv");
        sfapi.runBB(importResult.getId());

        String status = "";
        while (!"Finished".equals(status)) {

            XmlBbExecutionStatus bbStatus = sfapi.getBBStatus();
            status = bbStatus.getStatus();

            StringWriter writer = new StringWriter();
            if (status.equals("Error")) {

                writer.write("BB state is error.\n");
                writer.write("Error message: " + bbStatus.getErrorMessage() + "\n");


                bbStatus.getSlaveStatuses().stream().forEach(slave -> {
                    writer.write(slave.getName() + "\t" + slave.getStatus() + ":");

                    if (slave.getErrorMessage() != null) {
                        for (String err : slave.getErrorMessage()) {
                            writer.write(err+"\n");
                        }
                    }
                });

                if (logger.isErrorEnabled()) {
                    logger.error(writer.toString());
                }
            }

            Assert.assertNotEquals(writer.toString(), "Error", status);

            if ("Running".equals(status)) {
                sfapi.pauseBB();
                status = sfapi.getBBStatus().getStatus();
                Assert.assertEquals("Pause", status);
                sfapi.resumeBB();
                status = sfapi.getBBStatus().getStatus();
                Assert.assertEquals("Running", status);
            }

            logger.info(status);
            Thread.sleep(1500);
        }

        try (BufferedReader actual = new BufferedReader(new InputStreamReader(sfapi.getBBReport()));
                BufferedReader expected = new BufferedReader(
                        new InputStreamReader(getClass().getClassLoader().getResourceAsStream("bbRunExpectedResult.csv")))) {

            Assert.assertArrayEquals("fail", expected.lines().toArray(String[]::new), actual.lines().toArray(String[]::new));
        }

    }


}
