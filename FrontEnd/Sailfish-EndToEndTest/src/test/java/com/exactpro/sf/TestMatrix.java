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

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.exceptions.APICallException;
import com.exactpro.sf.exceptions.APIResponseException;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.ReportProperties;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.ReportRoot;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.TestCase;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.TestCaseMetadata;
import com.exactpro.sf.scriptrunner.state.ScriptState;
import com.exactpro.sf.scriptrunner.state.ScriptStatus;
import com.exactpro.sf.testwebgui.restapi.xml.MatrixList;
import com.exactpro.sf.testwebgui.restapi.xml.XmlMatrixDescription;
import com.exactpro.sf.testwebgui.restapi.xml.XmlTestCaseDescription;
import com.exactpro.sf.testwebgui.restapi.xml.XmlTestscriptRunDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class TestMatrix extends AbstractSFTest {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
       OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }

    public static final String STATUS_PASSED = "PASSED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String SCRIPT_STATUS_INIT_FAILED = "INIT_FAILED";
    private final static String ENVIRONMENT = "default";
    private static List<ServiceImportResult> importedServices = null;
    private static List<Integer> matricesIds = new ArrayList<>();
    private static List<Integer> runs = new ArrayList<>();
    private final static int PORT = 9881;

    private static final Logger logger = LoggerFactory.getLogger(TestMatrix.class);

    protected static void addRun(int id) {
        runs.add(id);
    }

    protected static void addMatrix(int id) {
        matricesIds.add(id);
    }

    /**
     * Run quickfix.Executor, upload FIX Client and start FIX Client.
     * 
     * @param sfapi
     * @throws Exception
     */
    protected static void init(SFAPIClient sfapi) throws Exception {
        try {
            runs.clear();

            // start service FIX-Server
            startService(sfapi, SERVER, SERVER_NAME, ENVIRONMENT);

            // start service FIX-client
            startService(sfapi, CLIENT, CLIENT_NAME, ENVIRONMENT);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Delete uploaded service, matrices and TestScriptRuns. Close SFAPIClient.
     * 
     * @param sfapi
     * @throws Exception
     */
    protected static void destroy(SFAPIClient sfapi) throws Exception {
        try {
            Map<String, Service> services = sfapi.getServices(ENVIRONMENT);
            if (services.get(CLIENT_NAME) != null) {
                sfapi.deleteService(ENVIRONMENT, services.get(CLIENT_NAME));
            }
            if (services.get(SERVER_NAME) != null) {
                sfapi.deleteService(ENVIRONMENT, services.get(SERVER_NAME));
            }
            while (matricesIds.size() != 0) {
                int id = matricesIds.get(0);
                sfapi.deleteMatrix(id);
                matricesIds.remove(0);
            }

            while (runs.size() != 0) {
                int id = runs.get(0);
                sfapi.deleteTestScriptRun(id);
                runs.remove(0);
            }
            sfapi.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Checks to see if a specific port is available.
     *
     * @param port
     *            the port to check for availability
     */
    protected static boolean available(int port) throws Exception {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid start port: " + port);
        }
        boolean connected = false;
        int timewait = 0;
        while (timewait != 1000 && connected == false) {
            timewait++;
            ServerSocket ss = null;
            try {
                Thread.sleep(10);
                ss = new ServerSocket(port);
                ss.setReuseAddress(true);
                connected = true;
            } catch (IOException e) {
                if (timewait == 1000) {
                    logger.error(e.getMessage(), e);
                    throw e;
                } else
                    continue;
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
                throw e;
            } finally {
                if (ss != null) {
                    try {
                        ss.close();
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                        throw e;
                    }
                }
            }
        }
        return connected;
    }

    /**
     * Test valid or invalid TestScriptRunDescription after matrix run
     * 
     * @param valid
     * @param sfapi
     * @param testScriptId
     * @throws APICallException
     * @throws APIResponseException
     */
    protected void testScriptRunDescription(boolean valid, SFAPIClient sfapi, int testScriptId) throws APICallException, APIResponseException {
        try {
            int timewait = 0;
            while (timewait != 600 && sfapi.getTestScriptRunInfo(testScriptId).isLocked()) {
                timewait++;
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            throw new APICallException(e);
        }
        XmlTestscriptRunDescription xmlDescription = sfapi.getTestScriptRunInfo(testScriptId);
        if (valid) {
            List<XmlTestCaseDescription> xmlCases = xmlDescription.getTestcases();
            Assert.assertEquals("Wrong ScriptStatus value in TestScriptRun.", ScriptStatus.EXECUTED, xmlDescription.getScriptStatus());
            Assert.assertEquals("Wrong ScriptState value in TestScriptRun.", ScriptState.FINISHED, xmlDescription.getScriptState());
            Assert.assertEquals("Wrong Passed value in TestScriptRun.", 1, xmlDescription.getPassed());
            Assert.assertEquals("Wrong Failed value in TestScriptRun.", 0, xmlDescription.getFailed());
            for (XmlTestCaseDescription xCase : xmlCases) {
                Assert.assertEquals(STATUS_PASSED, xCase.getStatus());
            }
        } else {
            Assert.assertEquals("Wrong ScriptStatus value in TestScriptRun.", ScriptStatus.INIT_FAILED, xmlDescription.getScriptStatus());
            Assert.assertEquals("Wrong ScriptState value in TestScriptRun.", ScriptState.FINISHED, xmlDescription.getScriptState());
            Assert.assertEquals("Wrong Passed value in TestScriptRun.", 0, xmlDescription.getPassed());
            Assert.assertEquals("Wrong Failed value in TestScriptRun.", 0, xmlDescription.getFailed());
        }
    }

    /**
     * Test valid or invalid report of matrices run from file
     * @param sfapi
     * @param id
     * @param valid
     * @param attempt
     * @throws APICallException
     * @throws APIResponseException
     */
    protected void testReportFile(SFAPIClient sfapi, int id, boolean valid, int attempt) throws APICallException, APIResponseException {
        try {
            InputStream stream = sfapi.getTestScriptRunReport(id);
            testReport(stream, valid);
        } catch (APIResponseException e) {
            if (attempt > 0 && e.getCause().getMessage().contains("Report hasn't been unlocked yet")) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                logger.warn("Report was lock. Try again");
                testReportFile(sfapi, id, valid, --attempt);
            } else {
                throw e;
            }
        }
    }

    /**
     * Test valid or invalid report of matrices run from input stream
     * 
     * @param stream
     * @param valid
     * @throws APICallException
     */
    protected void testReport(InputStream stream, boolean valid) throws APICallException {

        try {
            File archive = File.createTempFile("report" + UUID.randomUUID(), ".zip");
            Files.copy(stream, archive.toPath(), StandardCopyOption.REPLACE_EXISTING);
            ZipFile zipFile = new ZipFile(archive);

            ReportRoot report = OBJECT_MAPPER.readValue(zipFile.getInputStream(zipFile.getEntry("report.json")), ReportRoot.class);


            if (!valid) {
                ReportProperties reportProperties = report.getReportProperties();
                Assert.assertEquals("Wrong ScriptStatus value in TestScriptRun.", SCRIPT_STATUS_INIT_FAILED, reportProperties.getStatus().name());
                Assert.assertEquals("Wrong Passed value in TestScriptRun.", 0L, reportProperties.getPassed());
                Assert.assertEquals("Wrong Failed value in TestScriptRun.", 0L, reportProperties.getFailed());
            }

            for (TestCaseMetadata link : report.getMetadata()) {
                TestCase testCase = OBJECT_MAPPER.readValue(zipFile.getInputStream(zipFile.getEntry(link.getJsonFileName())), TestCase.class);
                if (valid) {
                    Assert.assertEquals(testCase.getStatus().getStatus().name(), (STATUS_PASSED));
                }
            }
        } catch (Exception e) {
            throw new APICallException(e);
        }
    }

    /**
     * Waiting from matrix stop and test that it's stop correctly
     * 
     * @param sfapi
     * @param testScriptId
     * @throws APICallException
     * @throws APIResponseException
     * @throws InterruptedException
     */
    protected void testMatrixStop(SFAPIClient sfapi, int testScriptId) throws APICallException, APIResponseException, InterruptedException {
        try {
            int timewait = 0;
            while (timewait != 600 && sfapi.getTestScriptRunInfo(testScriptId).isLocked()) {
                timewait++;
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            throw new APICallException(e);
        }
        XmlTestscriptRunDescription xmlDescription = sfapi.getTestScriptRunInfo(testScriptId);
        Assert.assertEquals("Wrong ScriptStatus value in TestScriptRun.", ScriptStatus.INTERRUPTED, xmlDescription.getScriptStatus());
        Assert.assertEquals("Wrong ScriptState value in TestScriptRun.", ScriptState.FINISHED, xmlDescription.getScriptState());
        Assert.assertEquals("Wrong Passed value in TestScriptRun.", 0, xmlDescription.getPassed());

        if(xmlDescription.isLocked()){
            Thread.sleep(1000);
            xmlDescription = sfapi.getTestScriptRunInfo(testScriptId);
        }

        Assert.assertFalse("Report locked",  xmlDescription.isLocked());
        // Assert.assertEquals("Wrong Failed value in TestScriptRun.", 1,
        // xmlDescription.getFailed());
    }

    /**
     * Test valid or invalid report of matrices run from ZIP
     * @param sfapi
     * @param id
     * @param valid
     * @param attempt
     * @throws APICallException
     */
    protected void testReportZip(SFAPIClient sfapi, int id, boolean valid, int attempt) throws APICallException {
        try {
            try {
                FileDownloadWrapper report = sfapi.getTestScriptRunReportZip(id);
                File file = new File("report.zip");
                file.delete();
                Files.copy(report.getInputStream(), file.toPath());
                File reportRepack = File.createTempFile("repack"+UUID.randomUUID(), ".zip");
                repackReport(file, reportRepack);
                try (InputStream in = new FileInputStream(reportRepack)) {
                    testReport(in, valid);
                }
                Files.delete(file.toPath());
            } catch (APIResponseException ex) {
                if (attempt > 0 && ex.getCause().getMessage().contains("Report hasn't been unlocked yet")) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    logger.warn("Report was lock. Try again");
                    testReportZip(sfapi, id, valid, --attempt);
                } else {
                    throw ex;
                }
            }
        } catch (Exception e) {
            throw new APICallException(e);
        }
    }

    private void repackReport(File src, File target) throws Exception {

        ZipFile zipFile = new ZipFile(src);
        File tmpDir = Files.createTempDirectory("tmpReport"+UUID.randomUUID()).toFile();
        zipFile.stream().forEach(o -> {
            File out = new File(tmpDir, ((ZipEntry) o).getName());
            out.mkdirs();
            try {
                Files.copy(zipFile.getInputStream(o), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        try(ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(target))) {
            File reportData = tmpDir.listFiles(File::isDirectory)[0].listFiles(file -> file.isDirectory() && file.getName().equals("reportData"))[0];

            for (File f : reportData.listFiles()) {
                zipOutputStream.putNextEntry(new ZipEntry(f.getName()));
                Files.copy(f.toPath(), zipOutputStream);
                zipOutputStream.closeEntry();
            }
        }
    }

    private InputStream getInputStream(File zip, String entry) throws IOException {
        ZipInputStream zin = new ZipInputStream(new FileInputStream(zip));
        for (ZipEntry e; (e = zin.getNextEntry()) != null;) {
            if (e.getName().contains(entry)) {
                return zin;
            }
        }
        throw new EOFException("Cannot find " + entry);
    }

    protected static Matrix getMatrixFromList(MatrixList matrixList, int matrixId, String name) throws APICallException {
        List<XmlMatrixDescription> list = matrixList.getMatrixList();;
        Matrix mat = null;
        if (list != null) {
            for (XmlMatrixDescription m : list) {
                if (m.getId() == matrixId && name.equals(m.getName())) {
                    mat = new Matrix(matrixId, m.getName(), m.getDate());
                }
            }
        }
        return mat;
    }

    protected Matrix getMatrixFromList(MatrixList matrixList, String name) {
        List<XmlMatrixDescription> list = matrixList.getMatrixList();
        Matrix mat = null;
        if (list != null) {
            for (XmlMatrixDescription m : list) {
                if (name.equals(m.getName())) {
                    mat = new Matrix(m.getId().intValue(), m.getName(), m.getDate());
                }
            }
        }
        return mat;
    }

    protected static int runMatrix(SFAPIClient sfapi, String matrixName, String path) throws APIResponseException, APICallException {
        int testsMatrixID = (int) sfapi.uploadMatrix(TestMatrixPositive.class.getClassLoader().getResourceAsStream(path + matrixName), matrixName)
                .getId();
        int testsScriptID = (int) sfapi
                .performMatrixAction(testsMatrixID, "start", null, "default", "ISO-8859-1", 3, false, false, true, true, null, null, null).getId();
        addRun(testsScriptID);
        addMatrix(testsMatrixID);
        long time = System.currentTimeMillis() + 60000;
        try {
            while (time > System.currentTimeMillis() && sfapi.getTestScriptRunInfo(testsScriptID).isLocked()) {
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            throw new APICallException(e);
        }
        return testsScriptID;
    }
}
