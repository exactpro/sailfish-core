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

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.exceptions.APICallException;
import com.exactpro.sf.exceptions.APIResponseException;
import com.exactpro.sf.scriptrunner.state.ScriptState;
import com.exactpro.sf.testwebgui.restapi.xml.MatrixList;
import com.exactpro.sf.testwebgui.restapi.xml.XmlTestscriptRunDescription;

public class TestMatrixNegative extends TestMatrix {

    private static SFAPIClient sfapi;

    private static final String noMatrix = "noMatrix.csv";
    private static final String matrixFileNegative = "testMatrixNegative.csv";
    private static final String matrixFile = "testMatrix.csv";
    private static List<String> matricesNames;
    private static Matrix matrix;
    private static List<Integer> runs = new ArrayList<Integer>();
    private static final Logger logger = LoggerFactory.getLogger(TestMatrixNegative.class);

    @BeforeClass
    public static void setUpClass() throws Exception {
        logger.info("Start negative tests of matrices");
        try {
            sfapi = new SFAPIClient(TestMatrix.SF_GUI_URL);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
        matricesNames = new ArrayList<String>();
        matricesNames.add(matrixFile);
        matricesNames.add(matrixFileNegative);
        matricesNames.add(noMatrix);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        logger.info("Finish negative tests of matrices");
        try {
            if (matrix != null) {
                sfapi.deleteMatrix(matrix);
                matrix = null;
            }
            for (int id : runs)
                sfapi.deleteTestScriptRun(id);
            sfapi.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @After
    public void tearDown() throws Exception {
        try {
            if (matrix != null) {
                sfapi.deleteMatrix(matrix);
                matrix = null;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to upload lacking matrix
     */
    @Test
    public void testUploadMatrixNegative() throws Exception {
        logger.info("Start testUploadMatrixNegative()");
        try {
            int matrixId = -1;
            try {
                matrixId = (int) sfapi.uploadMatrix(TestMatrixNegative.class.getClassLoader().getResourceAsStream(noMatrix), noMatrix).getId();
                Assert.fail("There is no matrix " + noMatrix + ", but upload has been done.");
            } catch (APICallException e) {
                Assert.assertEquals("java.lang.IllegalArgumentException: Input stream may not be null", e.getMessage());
            }
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, noMatrix);
            Assert.assertTrue("Method uploadMatrix(...) return id, but upload cann't be done", matrixId == -1);
            Assert.assertTrue("Matrix " + noMatrix + " is uploaded", matrix == null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to upload matrix link with negative provider
     */
    @Test
    public void testUploadMatrixLinkByProviderNegative() throws Exception {
        logger.info("Start testUploadMatrixLinkByProviderNegative()");
        SailfishURI negativeSURI = SailfishURI.unsafeParse("LOCAL2");

        try {
            sfapi.uploadMatrixLink(TestMatrixPositive.class.getClassLoader().getResource(matrixFile).getFile(),
                                   negativeSURI);
            Assert.fail("Upload matrix link with negative provider has been done");
        } catch (APIResponseException e) {
            checkErrorMessage(e.getCause(), "Illegal provider SURI: " + negativeSURI);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to upload lacking matrix link
     */
    @Test
    public void testUploadMatrixLinkNegative() throws Exception {
        logger.info("Start testUploadMatrixLinkNegative()");
        try {
            sfapi.uploadMatrixLink(noMatrix);
            Assert.fail("Upload lacking matrix link has been done");
        } catch (APIResponseException e) {
            checkErrorMessage(e.getCause(), "Incorrect path: " + noMatrix);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to upload matrix with empty link
     */
    @Test
    public void testUploadMatrixLinkNegativeEmpty() throws Exception {
        logger.info("Start testUploadMatrixLinkNegativeEmpty()");
        try {
            sfapi.uploadMatrixLink("");
            Assert.fail("Upload matrix with empty link has been done");
        } catch (APIResponseException e) {
            checkErrorMessage(e.getCause(), "Parameter 'link' cannot be empty");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }
    /**
     * Try to delete matrix with negative id
     */
    @Test
    public void testDeleteMatrixNegative() throws Exception {
        logger.info("Start testDeleteMatrixNegative()");
        try {
            try {
                sfapi.deleteMatrix(-1);
                Assert.fail("There is no matrix with id=-1, but removal has been done.");
            } catch (APIResponseException e) {
                checkErrorMessage(e, "No matrix with id -1 was found;");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Start by id invalid matrix and test result
     */
    @Test
    public void testRunIncorrectMatrixById() throws Exception {
        logger.info("Start testRunIncorrectMatrixById()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFileNegative);
            if (matrix == null) {
                int matrixId = (int) sfapi
                        .uploadMatrix(TestMatrixNegative.class.getClassLoader().getResourceAsStream(matrixFileNegative), matrixFileNegative).getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFileNegative);
            }
            if (matrix != null) {
                int testScriptId = (int) sfapi
                        .performMatrixAction(matrix.getId(), "start", null, "default", "ISO-8859-1", 3, false, false, true, true, null, null, null)
                        .getId();
                runs.add(testScriptId);
                testScriptRunDescription(false, sfapi, testScriptId);
            } else
                Assert.fail("Matrix " + matrixFileNegative + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Start by name invalid matrix and test result
     */
    @Test
    public void testRunIncorrectMatrixByName() throws Exception {
        logger.info("Start testRunIncorrectMatrixByName()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFileNegative);
            if (matrix == null) {
                int matrixId = (int) sfapi
                        .uploadMatrix(TestMatrixNegative.class.getClassLoader().getResourceAsStream(matrixFileNegative), matrixFileNegative).getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFileNegative);
            }
            if (matrix != null) {
                int testScriptId = (int) sfapi
                        .performMatrixAction(matrix.getName(), "start", null, "default", "ISO-8859-1", 3, false, false, true, true, null, null, null)
                        .getId();
                runs.add(testScriptId);
                testScriptRunDescription(false, sfapi, testScriptId);
            } else
                Assert.fail("Matrix " + matrixFileNegative + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to start matrix with negative id
     */
    @Test
    public void testRunMatrixByIncorrectId() throws Exception {
        logger.info("Start testRunMatrixByIncorrectId()");
        try {
            int testScriptId = -1;
            try {
                testScriptId = (int) sfapi
                        .performMatrixAction(-1, "start", null, "default", "ISO-8859-1", 3, false, false, true, true, null, null, null).getId();
                Assert.fail("There is no matrix with id=-1, but running has been done.");
            } catch (APIResponseException e) {
                checkErrorMessage(e, "Matrix with id = [-1] not found;");

            }
            Assert.assertTrue("Method performMatrixAction(...) return -1, but starting cann't be done", testScriptId == -1);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to start matrix with incorrect name
     */
    @Test
    public void testRunMatrixByIncorrectName() throws Exception {
        logger.info("Start testRunMatrixByIncorrectName()");
        try {
            int testScriptId = -1;
            try {
                testScriptId = (int) sfapi
                        .performMatrixAction("-1", "start", null, "default", "ISO-8859-1", 3, false, false, true, true, null, null, null).getId();
                Assert.fail("There is no matrix with name \"-1\", but running has been done.");
            } catch (APIResponseException e) {
                checkErrorMessage(e, "Cannot find matrix -1;");
            }
            Assert.assertTrue("Method performMatrixAction(...) return -1, but starting cann't be done", testScriptId == -1);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to start matrix with incorrect range and test result
     */
    @Test
    public void testRunMatrixWithIncorrectRange() throws Exception {
        logger.info("Start testRunMatrixWithIncorrectRange()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFile);
            if (matrix == null) {
                int matrixId = (int) sfapi.uploadMatrix(TestMatrixNegative.class.getClassLoader().getResourceAsStream(matrixFile), matrixFile)
                        .getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFile);
            }
            if (matrix != null) {
                int testScriptId = (int) sfapi
                        .performMatrixAction(matrix.getName(), "start", "3", "default", "ISO-8859-1", 3, false, false, true, true, null, null, null)
                        .getId();
                runs.add(testScriptId);
                testScriptRunDescription(false, sfapi, testScriptId);
            } else
                Assert.fail("Matrix " + matrixFile + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to start matrix with incorrect language
     */
    @Test
    public void testRunMatrixWithIncorrectLanguage() throws Exception {
        logger.info("Start testRunMatrixWithIncorrectLanguage()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFile);
            if (matrix == null) {
                int matrixId = (int) sfapi.uploadMatrix(TestMatrixNegative.class.getClassLoader().getResourceAsStream(matrixFile), matrixFile)
                        .getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFile);
            }
            if (matrix != null) {
                try {
                    sfapi.performMatrixAction(matrix.getId(), "start", null, "default", "ISO-8859-1", 3, false, false, true, true, null, null, null,
                            "-41").getId();
                    Assert.fail("Try to start matrix with language parameters = \"-41\", but exception wasn't thrown");
                } catch (APIResponseException e) {
                    checkErrorMessage(e, "Invalid URI: -41");
                }
            } else
                Assert.fail("Matrix " + matrixFile + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to start matrix with incorrect AML
     */
    @Test
    public void testRunMatrixWithIncorrectAml() throws Exception {
        logger.info("Start testRunMatrixWithIncorrectAml()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFile);
            if (matrix == null) {
                int matrixId = (int) sfapi.uploadMatrix(TestMatrixNegative.class.getClassLoader().getResourceAsStream(matrixFile), matrixFile)
                        .getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFile);
            }
            if (matrix != null) {
                try {
                    sfapi.performMatrixAction(matrix.getId(), "start", null, "default", "ISO-8859-1", -41, false, false, true, true, null, null, null,
                            "").getId();
                    Assert.fail("Try to start matrix with language parameters = \"-41\", but exception wasn't thrown");
                } catch (APIResponseException e) {
                    checkErrorMessage(e, "Invalid URI: AML_v-41");
                }
            } else
                Assert.fail("Matrix " + matrixFile + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to start matrix with incorrect encoding and test result
     */
    @Test
    public void testRunMatrixWithIncorrectEncoding() throws Exception {
        logger.info("Start testRunMatrixWithIncorrectEncoding()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFileNegative);
            if (matrix == null) {
                int matrixId = (int) sfapi
                        .uploadMatrix(TestMatrixNegative.class.getClassLoader().getResourceAsStream(matrixFileNegative), matrixFileNegative).getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFileNegative);
            }
            if (matrix != null) {
                int testScriptId = (int)sfapi.performMatrixAction(matrix.getId(), "start", null, "default", "incorrectEncoding", 3, false, false,
                        true, true, null, null, null).getId();
                runs.add(testScriptId);
                testScriptRunDescription(false, sfapi, testScriptId);
            } else
                Assert.fail("Matrix " + matrixFileNegative + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Start matrix without autorun, check status and run
     */
    @Test
    public void testRunIncorrectMatrixWithoutAutorun() throws Exception {
        logger.info("Start testRunIncorrectMatrixWithoutAutorun()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFileNegative);
            if (matrix == null) {
                int matrixId = (int) sfapi
                        .uploadMatrix(TestMatrixNegative.class.getClassLoader().getResourceAsStream(matrixFileNegative), matrixFileNegative).getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFileNegative);
            }
            if (matrix != null) {
                int testScriptId = (int) sfapi
                        .performMatrixAction(matrix.getId(), "start", null, "default", "ISO-8859-1", 3, false, false, false, true, null, null, null)
                        .getId();

                XmlTestscriptRunDescription xmlDescription = sfapi.getTestScriptRunInfo(testScriptId);
                Assert.assertEquals("Wrong status before running matrix: " + xmlDescription.getScriptState(), ScriptState.PENDING,
                        xmlDescription.getScriptState());
                sfapi.compileTestScriptRun(testScriptId);
                try {
                    int timewait = 0;
                    while (timewait != 60 && sfapi.getTestScriptRunInfo(testScriptId).isLocked()) {
                        timewait++;
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    throw new APICallException(e);
                }
                xmlDescription = sfapi.getTestScriptRunInfo(testScriptId);
                Assert.assertEquals("Wrong status after running matrix: " + xmlDescription.getScriptState(), ScriptState.FINISHED,
                        xmlDescription.getScriptState());
                runs.add(testScriptId);
                testScriptRunDescription(false, sfapi, testScriptId);
            } else
                Assert.fail("Matrix " + matrixFileNegative + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Check all incorrect matrix with aggregate report
     */
    @Test
    public void testRunAllIncorrectMatrix() throws Exception {
        logger.info("Start testRunAllIncorrectMatrix()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFileNegative);
            if (matrix == null) {
                int matrixId = (int) sfapi
                        .uploadMatrix(TestMatrixNegative.class.getClassLoader().getResourceAsStream(matrixFileNegative), matrixFileNegative).getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFileNegative);
            }
            if (matrix != null) {
                List<TestScriptRun> testScriptRuns = sfapi.runAllMatrices();
                List<Integer> runIds = new ArrayList<>();
                for (TestScriptRun run : testScriptRuns) {
                    runIds.add(run.getId());
                    if (matrixFileNegative.equals(run.getMatrixName()))
                        testScriptRunDescription(false, sfapi, run.getId());
                }
                Assert.assertFalse("There is no TestScriptRuns", testScriptRuns.isEmpty());
                Assert.assertFalse(runIds.isEmpty());
                for (int runId : runIds) {
                    runs.add(runId);
                }
            } else
                Assert.fail("Matrix " + matrixFileNegative + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to stop matrix with incorrect id
     */
    @Test
    public void testStopMatrixByIncorrectId() throws Exception {
        logger.info("Start testStopMatrixByIncorrectId()");
        try {
            try {
                sfapi.stopMatrix(-1);
                Assert.fail("There is no matrix with id=-1, but stopping has been done.");
            } catch (APIResponseException e) {
                Assert.assertTrue("Exception message doesn't contain \"Script run with id = [-1] not found;\"",
                        e.getMessage().contains("Script run with id = [-1] not found;"));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try stop matrix with incorrect name
     * @throws Exception
     */
    @Test
    public void testStopMatrixByIncorrectName() throws Exception {
        logger.info("Start testStopMatrixByIncorrectName()");
        try {
            String matrixName = "Incorrect_matrix_name";
            try {
                sfapi.stopMatrix(matrixName);
                Assert.fail(String.format("Matrix \"%s\" was successfully stopped", matrixName));
            } catch (APIResponseException e) {
                checkErrorMessage(e.getCause(), "Cannot stop matrix by it's name");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to load not exists file
     */
    @Test
    public void testUploadingNotExistsMatrixFile() throws Exception {
        logger.info("Start testUploadingNotExistsMatrixFile()");
        try {
            int matrixId = -1;
            try {
                matrixId = (int) sfapi.uploadMatrix(TestMatrixNegative.class.getClassLoader().getResourceAsStream(noMatrix), noMatrix).getId();
                Assert.fail("There is no matrix " + noMatrix + ", but stopping has been done.");
            } catch (APICallException e) {
                checkErrorMessage(e, "Input stream may not be null");
            }
            Assert.assertTrue(matrixId == -1);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to upload and start matrices and get testScriptRunInfo as the
     * InputStream. Test testScriptRunInfo' InputStream
     */
    @Test
    public void testScriptRunReportNegative() throws Exception {
        logger.info("Start testScriptRunReportNegative()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFileNegative);
            if (matrix == null) {
                int matrixId = (int) sfapi
                        .uploadMatrix(TestMatrixNegative.class.getClassLoader().getResourceAsStream(matrixFileNegative), matrixFileNegative).getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFileNegative);
            }
            if (matrix != null) {
                int testScriptId = (int) sfapi.runMatrix(matrix).getId();
                runs.add(testScriptId);
                int timewait = 0;
                while (timewait != 600 && sfapi.getTestScriptRunInfo(testScriptId).isLocked()) {
                    timewait++;
                    Thread.sleep(100);
                }
                try {
                    testReportFile(sfapi, testScriptId, false, 2);
                } catch (Exception e) {
                    throw new APICallException(e);
                }
            } else
                Assert.fail("Matrix " + matrixFileNegative + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to upload and start matrices and get testScriptRunInfo as the Zip
     * file. Test testScriptRunInfo' Zip file
     */
    @Test
    public void testScriptRunReportZipNegative() throws Exception {
        logger.info("Start testScriptRunReportZipNegative()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFileNegative);
            if (matrix == null) {
                int matrixId = (int) sfapi
                        .uploadMatrix(TestMatrixNegative.class.getClassLoader().getResourceAsStream(matrixFileNegative), matrixFileNegative).getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFileNegative);
            }
            if (matrix != null) {
                int testScriptId = (int) sfapi.runMatrix(matrix).getId();
                runs.add(testScriptId);
                int timewait = 0;
                while (timewait != 600 && sfapi.getTestScriptRunInfo(testScriptId).isLocked()) {
                    timewait++;
                    Thread.sleep(100);
                }
                try {
                    testReportZip(sfapi, testScriptId, false, 2);
                } catch (Exception e) {
                    throw new APICallException(e);
                }
            } else
                Assert.fail("Matrix " + matrixFileNegative + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to stop matrix by the TestScriptRun with invalid id
     */
    @Test
    public void testStopScript() throws Exception {
        logger.info("Start testStopScript()");
        try {
            try {
                sfapi.stopTestScriptRun(-1);
                Assert.fail("There is no TestScriptRun with id=-1, but stopping has been done.");
            } catch (APIResponseException e) {
                checkErrorMessage(e, "unknown testscriptrun;");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to delete TestScriptRun with invalid id
     */
    @Test
    public void testDeleteScript() throws Exception {
        logger.info("Start testDeleteScript()");
        try {
            try {
                sfapi.deleteTestScriptRun(-1);
                Assert.fail("There is no TestScriptRun with id=-1, but removal has been done.");
            } catch (APIResponseException e) {
                checkErrorMessage(e, "unknown testscriptrun;");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

}
