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

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.exceptions.APICallException;
import com.exactpro.sf.exceptions.APIResponseException;
import com.exactpro.sf.scriptrunner.state.ScriptState;
import com.exactpro.sf.scriptrunner.state.ScriptStatus;
import com.exactpro.sf.testwebgui.restapi.xml.MatrixList;
import com.exactpro.sf.testwebgui.restapi.xml.XmlMatrixDescription;
import com.exactpro.sf.testwebgui.restapi.xml.XmlMatrixLinkUploadResponse;
import com.exactpro.sf.testwebgui.restapi.xml.XmlTestCaseDescription;
import com.exactpro.sf.testwebgui.restapi.xml.XmlTestscriptRunDescription;

public class TestMatrixPositive extends TestMatrix {

    private static SFAPIClient sfapi;
    private final static String matrixFile = "testMatrix.csv";
    private final static String longMatrixFile = "testLongMatrix.csv";
    private final static String matrixFileRange = "testMatrixRange.csv";
    private final static String matrixFileWithVariableDelay = "testMatrixTimeDelay.csv";
    private final static String matrixFileRangeForExcludeCase = "testMatrixRangeForExcludeCase.csv";
    private final String ENVIRONMENT = "testEnvironment";
    private static final String matrixFileWithPath = System.getProperty("matrixPath") == null
        ? TestMatrixPositive.class.getClassLoader().getResource(matrixFile).getFile()
        : Paths.get(System.getProperty("matrixPath"), matrixFile).toString(); // for containerized environment
    private static Matrix matrix;
    private static Matrix matrixVariableDelay;
    private static Matrix amlMatrix;
    private static Matrix matrixWithRanges;
    private static List<Integer> runs = new ArrayList<>();
    private static List<String> matricesNames;
    private static final Logger logger = LoggerFactory.getLogger(TestMatrixPositive.class);

    @BeforeClass
    public static void setUpClass() throws Exception {
        logger.info("Start positive tests of matricies");
        try {
            sfapi = new SFAPIClient(TestMatrix.SF_GUI_URL);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
        matricesNames = new ArrayList<String>();
        matricesNames.add(matrixFile);
        matricesNames.add(matrixFileRange);
        matricesNames.add(matrixFileWithVariableDelay);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        logger.info("Finish positive tests of matrix");
        try {
            if (matrix != null) {
                sfapi.deleteMatrix(matrix);
            }
            if (amlMatrix != null) {
                sfapi.deleteMatrix(amlMatrix);
            }
            if (matrixVariableDelay != null) {
                sfapi.deleteMatrix(matrixVariableDelay);
            }
            if (matrixWithRanges != null) {
                sfapi.deleteMatrix(matrixWithRanges);
            }

            for (int id : runs) {
                sfapi.deleteTestScriptRun(id);
            }
            sfapi.close();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Try to upload matrix
     */
    @Test
    public void testUploadMatrix() throws Exception {
        logger.info("Start testUploadMatrix()");
        try {
            int matrixId = (int) sfapi.uploadMatrix(TestMatrixPositive.class.getClassLoader().getResourceAsStream(matrixFile), matrixFile).getId();
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixId, matrixFile);
            Assert.assertTrue("getMatrixList(...) doesn't contain " + matrixFile, matrix != null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to upload matrix link
     */
    @Test
    public void testUploadMatrixLink() throws Exception {
        logger.info("Start testUploadMatrixLink()");
        try {
            XmlMatrixLinkUploadResponse response = sfapi.uploadMatrixLink(matrixFileWithPath);
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, response.getMatrices().get(matrixFile).intValue(), matrixFile);
            Assert.assertTrue("getMatrixList(...) doesn't contain " + matrixFile, matrix != null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }


    /**
     * Try to upload matrix link by provider
     */
    @Test
    public void testUploadMatrixLinkByProvider() throws Exception {
        logger.info("Start testUploadMatrixLinkByProvider()");
        try {
            XmlMatrixLinkUploadResponse response = sfapi.uploadMatrixLink(matrixFileWithPath, SailfishURI.unsafeParse("LOCAL"));
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, response.getMatrices().get(matrixFile).intValue(), matrixFile);
            Assert.assertTrue("getMatrixList(...) doesn't contain " + matrixFile, matrix != null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to delete uploaded matrix
     */
    @Test
    public void testDeleteMatrix() throws Exception {
        logger.info("Start testDeleteMatrix()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFile);
            if (matrix == null) {
                int matrixId = (int) sfapi.uploadMatrix(TestMatrixPositive.class.getClassLoader().getResourceAsStream(matrixFile), matrixFile)
                        .getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFile);
            }
            if (matrix != null) {
                sfapi.deleteMatrix(matrix);
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrix.getId(), matrix.getName());
                Assert.assertTrue("getMatrixList(...) doesn't contain " + matrixFile, matrix == null);
            } else
                Assert.fail("Matrix " + matrixFile + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to delete all uploaded matrices. After test its will be upload
     */
    @Test
    public void testDeleteAllMatrices() throws Exception {
        logger.info("Start testDeleteAllMatrices()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFile);
            int matrixId;
            if (matrix == null) {
                matrixId = (int) sfapi.uploadMatrix(TestMatrixPositive.class.getClassLoader().getResourceAsStream(matrixFile), matrixFile).getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFile);
            }
            if (matrix != null) {
                List<File> files = new ArrayList<>();
                for (XmlMatrixDescription xmlMatrix : matrixList.getMatrixList()) {
                    InputStream mat = sfapi.downloadMatrix(xmlMatrix.getId().intValue());
                    File file = new File(xmlMatrix.getName());
                    file.delete();
                    Files.copy(mat, file.toPath());
                    files.add(file);
                }
                sfapi.deleteAllMatrices();
                Assert.assertTrue("There is matrices after deleting all matrices", sfapi.getMatrixList().getMatrixList() == null);
                for (File file : files) {
                    int id = (int) sfapi.uploadMatrix(Files.newInputStream(file.toPath()), file.getName()).getId();
                    if (matrixFile.equals(file.getName())) {
                        matrix = getMatrixFromList(sfapi.getMatrixList(), id, matrixFile);
                    } else if (matrixFileRange.equals(file.getName())) {
                        matrixWithRanges = getMatrixFromList(sfapi.getMatrixList(), id, matrixFileRange);
                    } else if (matrixFileWithVariableDelay.equals(file.getName())) {
                        matrixVariableDelay = getMatrixFromList(sfapi.getMatrixList(), id, matrixFileWithVariableDelay);
                    } else if (("AML3_" + matrixFile).equals(file.getName())) {
                        amlMatrix = getMatrixFromList(sfapi.getMatrixList(), id, "AML3_" + matrixFile);
                    }
                    Files.delete(file.toPath());
                }
                Assert.assertTrue(matrix != null);
            } else
                Assert.fail("Matrix " + matrixFile + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to start matrix with specified language
     */
    @Test
    public void testRunMatrixWithLanguage() throws Exception {
        logger.info("Start testRunMatrixWithLanguage()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFile);
            if (matrix == null) {
                int matrixId = (int) sfapi.uploadMatrix(TestMatrixPositive.class.getClassLoader().getResourceAsStream(matrixFile), matrixFile)
                        .getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFile);
            }
            if (matrix != null) {
                int testScriptId;
                // Its unknown in what form will be the space in URL
                try {
                    testScriptId = (int) sfapi.performMatrixAction(matrix.getId(), "start", null, "default", "ISO-8859-1", 3, false, false, true,
                            true, null, null, null, "aml_3:AML_v3").getId();
                } catch (APIResponseException resp) {
                    try {
                        testScriptId = (int) sfapi.performMatrixAction(matrix.getId(), "start", null, "default", "ISO-8859-1", 3, false, false, true,
                                true, null, null, null, "aml_3:AML%20v3").getId();
                    } catch (APIResponseException e) {
                        testScriptId = (int) sfapi.performMatrixAction(matrix.getId(), "start", null, "default", "ISO-8859-1", 3, false, false, true,
                                true, null, null, null, "AML_v3").getId();
                    }
                }

                runs.add(testScriptId);
                testScriptRunDescription(true, sfapi, testScriptId);
            } else
                Assert.fail("Matrix " + matrixFile + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to start matrix by id
     */
    @Test
    public void testRunMatrixById() throws Exception {
        logger.info("Start testRunMatrixById()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFile);
            if (matrix == null) {
                int matrixId = (int) sfapi.uploadMatrix(TestMatrixPositive.class.getClassLoader().getResourceAsStream(matrixFile), matrixFile)
                        .getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFile);
            }
            if (matrix != null) {
                int testScriptId = (int) sfapi
                        .performMatrixAction(matrix.getId(), "start", null, "default", "ISO-8859-1", 3, false, false, true, true, null, null, null)
                        .getId();
                runs.add(testScriptId);
                testScriptRunDescription(true, sfapi, testScriptId);
            } else
                Assert.fail("Matrix " + matrixFile + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to start matrix by name
     */
    @Test
    public void testRunMatrixByName() throws Exception {
        logger.info("Start testRunMatrixByName()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFile);
            if (matrix == null) {
                int matrixId = (int) sfapi.uploadMatrix(TestMatrixPositive.class.getClassLoader().getResourceAsStream(matrixFile), matrixFile)
                        .getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFile);
            }
            if (matrix != null) {
                int testScriptId = (int) sfapi
                        .performMatrixAction(matrix.getName(), "start", null, "default", "ISO-8859-1", 3, false, false, true, true, null, null, null)
                        .getId();
                runs.add(testScriptId);
                testScriptRunDescription(true, sfapi, testScriptId);
            } else
                Assert.fail("Matrix " + matrixFile + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to start matrix on the specified environment
     */
    @Test
    public void testRunMatrixOnTestEnvironment() throws Exception {
        logger.info("Start testRunMatrixOnTestEnvironment()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFile);
            if (matrix == null) {
                int matrixId = (int) sfapi.uploadMatrix(TestMatrixPositive.class.getClassLoader().getResourceAsStream(matrixFile), matrixFile)
                        .getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFile);
            }
            if (matrix != null) {
                try (AutoCloseable deleteEnv = () -> sfapi.deleteEnvironment(ENVIRONMENT)) {
                    sfapi.createEnvironment(ENVIRONMENT);
                    int testScriptId = (int) sfapi
                            .performMatrixAction(matrix.getId(), "start", null, ENVIRONMENT, "ISO-8859-1", 3, false, false, true, true, null, null,
                                    null).getId();

                    runs.add(testScriptId);
                    testScriptRunDescription(true, sfapi, testScriptId);
                }
            } else
                Assert.fail("Matrix " + matrixFile + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to start matrix with range. Check
     */
    @Test
    public void testRunMatrixWithRange() throws Exception {
        logger.info("Start testRunMatrixWithRange()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrixWithRanges = getMatrixFromList(matrixList, matrixFileRange);
            if (matrixWithRanges == null) {
                int matrixId = (int) sfapi
                        .uploadMatrix(TestMatrixPositive.class.getClassLoader().getResourceAsStream(matrixFileRange), matrixFileRange).getId();
                matrixList = sfapi.getMatrixList();
                matrixWithRanges = getMatrixFromList(matrixList, matrixId, matrixFileRange);
            }
            if (matrixWithRanges != null) {
                int testScriptId = (int)sfapi.performMatrixAction(matrixWithRanges.getId(), "start", "2", "default", "ISO-8859-1", 3, false, false,
                        true, true, null, null, null).getId();
                runs.add(testScriptId);
                testScriptRunDescription(true, sfapi, testScriptId);
            } else
                Assert.fail("Matrix " + matrixFileRange + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Ignore
    @Test
    public void testRunMatrixWithTags() throws Exception {
        logger.info("Start testRunMatrixWithTags()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFile);
            if (matrix == null) {
                int matrixId = (int) sfapi.uploadMatrix(TestMatrixPositive.class.getClassLoader().getResourceAsStream(matrixFile), matrixFile)
                        .getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFile);
            }
            if (matrix != null) {
                List<String> tags = new ArrayList<String>();
                tags.add("testTag");
                int testScriptId = (int) sfapi
                        .performMatrixAction(matrix.getId(), "start", null, "default", "ISO-8859-1", 3, false, false, true, true, tags, null, null)
                        .getId();
                runs.add(testScriptId);
                testScriptRunDescription(true, sfapi, testScriptId);
            } else
                Assert.fail("Matrix " + matrixFile + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to start matrix on AML3 and check result
     */
    @Test
    public void testRunMatrixWithAml3() throws Exception {
        logger.info("Start testRunMatrixWithAml3()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFile);
            if (matrix == null) {
                int matrixId = (int) sfapi.uploadMatrix(TestMatrixPositive.class.getClassLoader().getResourceAsStream(matrixFile), matrixFile)
                        .getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFile);
            }
            if (matrix != null) {
                int testScriptId = (int) sfapi
                        .performMatrixAction(matrix.getId(), "start", null, "default", "ISO-8859-1", 3, false, false, true, true, null, null, null)
                        .getId();
                runs.add(testScriptId);
                testScriptRunDescription(true, sfapi, testScriptId);
            } else
                Assert.fail("Matrix " + matrixFile + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to start matrix with UTF-8 encoding and check result
     */
    @Test
    public void testRunMatrixWithUTFEncoding() throws Exception {
        logger.info("Start testRunMatrixWithUTFEncoding()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFile);
            if (matrix == null) {
                int matrixId = (int) sfapi.uploadMatrix(TestMatrixPositive.class.getClassLoader().getResourceAsStream(matrixFile), matrixFile)
                        .getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFile);
            }
            if (matrix != null) {
                int testScriptId = (int) sfapi
                        .performMatrixAction(matrix.getId(), "start", null, "default", "UTF-8", 3, false, false, true, true, null, null, null)
                        .getId();
                runs.add(testScriptId);
                testScriptRunDescription(true, sfapi, testScriptId);
            } else
                Assert.fail("Matrix " + matrixFile + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to start matrix with continueOnFail parameters = true
     */
    @Test
    public void testRunMatrixWithContinueOnFailed() throws Exception {
        logger.info("Start testRunMatrixWithContinueOnFailed()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFile);
            if (matrix == null) {
                int matrixId = (int) sfapi.uploadMatrix(TestMatrixPositive.class.getClassLoader().getResourceAsStream(matrixFile), matrixFile)
                        .getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFile);
            }
            if (matrix != null) {
                int testScriptId = (int) sfapi
                        .performMatrixAction(matrix.getId(), "start", null, "default", "ISO-8859-1", 3, true, false, true, true, null, null, null)
                        .getId();
                runs.add(testScriptId);
                testScriptRunDescription(true, sfapi, testScriptId);
            } else
                Assert.fail("Matrix " + matrixFile + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to start matrix with parameter autostart=true
     */
    @Test
    public void testRunMatrixWithAutostart() throws Exception {
        logger.info("Start testRunMatrixWithAutostart()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFile);
            if (matrix == null) {
                int matrixId = (int) sfapi.uploadMatrix(TestMatrixPositive.class.getClassLoader().getResourceAsStream(matrixFile), matrixFile)
                        .getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFile);
            }
            if (matrix != null) {
                int testScriptId = (int) sfapi
                        .performMatrixAction(matrix.getId(), "start", null, "default", "ISO-8859-1", 3, false, true, true, true, null, null, null)
                        .getId();
                runs.add(testScriptId);
                testScriptRunDescription(true, sfapi, testScriptId);
            } else
                Assert.fail("Matrix " + matrixFile + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to start matrix with parameter ignoreAskForContinue=true
     */
    @Test
    public void testRunMatrixWithoutIgnoreAskForContinue() throws Exception {
        logger.info("Start testRunMatrixWithoutIgnoreAskForContinue()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFile);
            if (matrix == null) {
                int matrixId = (int) sfapi.uploadMatrix(TestMatrixPositive.class.getClassLoader().getResourceAsStream(matrixFile), matrixFile)
                        .getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFile);
            }
            if (matrix != null) {
                int testScriptId = (int) sfapi
                        .performMatrixAction(matrix.getId(), "start", null, "default", "ISO-8859-1", 3, false, false, true, false, null, null, null)
                        .getId();
                runs.add(testScriptId);
                testScriptRunDescription(true, sfapi, testScriptId);
            } else
                Assert.fail("Matrix " + matrixFile + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to start matrix with parameter autorun=false
     */
    @Test
    public void testRunMatrixWithoutAutorun() throws Exception {
        logger.info("Start testRunMatrixWithoutAutorun()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFile);
            if (matrix == null) {
                int matrixId = (int) sfapi.uploadMatrix(TestMatrixPositive.class.getClassLoader().getResourceAsStream(matrixFile), matrixFile)
                        .getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFile);
            }
            if (matrix != null) {
                int testScriptId = (int) sfapi
                        .performMatrixAction(matrix.getId(), "start", null, "default", "ISO-8859-1", 3, false, false, false, true, null, null, null)
                        .getId();

                XmlTestscriptRunDescription xmlDescription = sfapi.getTestScriptRunInfo(testScriptId);
                Assert.assertEquals("Status of matrix is wrong: " + xmlDescription.getScriptState(), ScriptState.PENDING, xmlDescription.getScriptState());
                sfapi.compileTestScriptRun(testScriptId);
                try {
                    int timewait = 0;
                    while (timewait != 60 && sfapi.getTestScriptRunInfo(testScriptId).getScriptState() != ScriptState.READY) {
                        timewait++;
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    throw new APICallException(e);
                }
                xmlDescription = sfapi.getTestScriptRunInfo(testScriptId);
                Assert.assertEquals("Status of matrix is wrong: " + xmlDescription.getScriptState(), ScriptState.READY, xmlDescription.getScriptState());
                sfapi.runCompiledTestScript(testScriptId);
                runs.add(testScriptId);
                testScriptRunDescription(true, sfapi, testScriptId);
            } else
                Assert.fail("Matrix " + matrixFile + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to start matrix with parameter variableDelay=true
     */
    @Test
    public void testRunMatrixWithVariableDelay() throws Exception {
        logger.info("Start testRunMatrixWithVariableDelay()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrixVariableDelay = getMatrixFromList(matrixList, matrixFileWithVariableDelay);
            if (matrixVariableDelay == null) {
                int matrixId = (int) sfapi.uploadMatrix(TestMatrixPositive.class.getClassLoader().getResourceAsStream(matrixFileWithVariableDelay),
                        matrixFileWithVariableDelay).getId();
                matrixList = sfapi.getMatrixList();
                matrixVariableDelay = getMatrixFromList(matrixList, matrixId, matrixFileWithVariableDelay);
            }
            if (matrixVariableDelay != null) {
                int testScriptId = (int)sfapi.performMatrixAction(matrixVariableDelay.getId(), "start", null, "default", "ISO-8859-1", 3, false,
                        false, true, true, null, "%22s1%22:%22500%22", null).getId();
                runs.add(testScriptId);
                testScriptRunDescription(true, sfapi, testScriptId);
            } else
                Assert.fail("Matrix " + matrixFileWithVariableDelay + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to start all upload matrices by one command
     */
    @Test
    public void testRunAllMatrix() throws Exception {
        logger.info("Start testRunAllMatrix()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFile);
            if (matrix == null) {
                int matrixId = (int) sfapi.uploadMatrix(TestMatrixPositive.class.getClassLoader().getResourceAsStream(matrixFile), matrixFile)
                        .getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFile);
            }
            if (matrix != null) {
                List<TestScriptRun> testScriptRuns = sfapi.runAllMatrices();
                for (TestScriptRun run : testScriptRuns)
                    runs.add(run.getId());
                Assert.assertFalse(testScriptRuns.isEmpty());
            } else
                Assert.fail("Matrix " + matrixFile + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    @Ignore
    public void stressTest(){
        int count = 0;
        for (int i = 1; i < 10001; i++){
            try {
                testRunAndStopMatrix();
                testStopScript();
            }
            catch (Error e){
                count++;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        Assert.assertEquals(0, count);
    }

    /**
     * Try to start matrix and stop it before it's finished
     */

    @Test
    public void testRunAndStopMatrix() throws Exception {
        logger.info("Start testRunAndStopMatrix()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            String matrixFileName = longMatrixFile;
            matrix = getMatrixFromList(matrixList, matrixFileName);
            if (matrix == null) {
                int matrixId = (int) sfapi.uploadMatrix(TestMatrixPositive.class.getClassLoader().getResourceAsStream(matrixFileName), matrixFileName)
                        .getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFileName);
            }
            if (matrix != null) {
                int testScriptId = (int) sfapi.runMatrix(matrix).getId();
                int timewait = 0;
                while (timewait != 6000 && sfapi.getTestScriptRunInfo(testScriptId).getScriptState() != ScriptState.RUNNING) {
                    timewait++;
                    Thread.sleep(100);
                }
                runs.add(testScriptId);
                sfapi.stopMatrix(testScriptId);
                testMatrixStop(sfapi, testScriptId);
            } else
                Assert.fail("Matrix " + matrixFileName + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to start matrix with specified subfolder. Check it
     */
    @Test
    public void testRunMatrixSubfolder() throws Exception {
        logger.info("Start testRunMatrixSubfolder()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFile);
            if (matrix == null) {
                int matrixId = (int) sfapi.uploadMatrix(TestMatrixPositive.class.getClassLoader().getResourceAsStream(matrixFile), matrixFile)
                        .getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFile);
            }
            if (matrix != null) {
                int testScriptId = (int)sfapi.performMatrixAction(matrix.getName(), "start", null, "default", "ISO-8859-1", 3, false, false, true,
                        true, null, null, "testSubfolder").getId();
                runs.add(testScriptId);
                XmlTestscriptRunDescription description = sfapi.getTestScriptRunInfo(testScriptId);
                Assert.assertEquals("Subfolder is wrong", "testSubfolder", description.getSubFolder());
                testScriptRunDescription(true, sfapi, testScriptId);
            } else
                Assert.fail("Matrix " + matrixFile + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to start matrix and check TestScriptRun report
     */
    @Test
    public void testScriptRunReport() throws Exception {
        logger.info("Start testScriptRunReport()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFile);
            if (matrix == null) {
                int matrixId = (int) sfapi.uploadMatrix(TestMatrixPositive.class.getClassLoader().getResourceAsStream(matrixFile), matrixFile)
                        .getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFile);
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
                    testReportFile(sfapi, testScriptId, true, 2);
                } catch (Exception e) {
                    throw new APICallException(e);
                }
            } else
                Assert.fail("Matrix " + matrixFile + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to start matrix and check TestScriptRun zip report
     */
    @Test
    public void testScriptRunReportZip() throws Exception {
        logger.info("Start testScriptRunReportZip()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFile);
            if (matrix == null) {
                int matrixId = (int) sfapi.uploadMatrix(TestMatrixPositive.class.getClassLoader().getResourceAsStream(matrixFile), matrixFile)
                        .getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFile);
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
                    testReportZip(sfapi, testScriptId, true, 2);
                } catch (Exception e) {
                    throw new APICallException(e);
                }
            } else
                Assert.fail("Matrix " + matrixFile + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to start matrix and stop it with TestScriptRun
     */
    @Test
    @Ignore("Unstable")
    public void testStopScript() throws Exception {
        logger.info("Start testStopScript()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            String matrixFileName = longMatrixFile;
            matrix = getMatrixFromList(matrixList, matrixFileName);
            if (matrix == null) {
                int matrixId = (int) sfapi.uploadMatrix(TestMatrixPositive.class.getClassLoader().getResourceAsStream(matrixFileName), matrixFileName)
                        .getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFileName);
            }
            if (matrix != null) {
                int testScriptId = (int) sfapi.runMatrix(matrix).getId();
                runs.add(testScriptId);
                Thread.sleep(500);      //FIXME: pause added cause SF can't correctly stop test script during its preparation
                sfapi.stopTestScriptRun(testScriptId);
                Thread.sleep(1500);
                XmlTestscriptRunDescription xmlDescription = sfapi.getTestScriptRunInfo(testScriptId);
                Assert.assertEquals("Wrong status", ScriptStatus.INTERRUPTED, xmlDescription.getScriptStatus()); //FIXME: must be CANCELED
                // Assert.assertEquals("Wrong status", SCRIPT_STATUS_CANCELED, xmlDescription.getScriptStatus());

                Assert.assertEquals("Wrong state", ScriptState.FINISHED, xmlDescription.getScriptState()); //FIXME: must be CANCELED
                // Assert.assertEquals("Wrong state", SCRIPT_STATE_CANCELED, xmlDescription.getScriptState());
                Assert.assertFalse("Report locked",  xmlDescription.isLocked());
                Assert.assertEquals("Wrong number of passed tests for canceled matrix", 0, xmlDescription.getPassed());

                Assert.assertEquals("Wrong number of failed tests for canceled matrix", 1, xmlDescription.getFailed()); //FIXME: must be 0
                // Assert.assertEquals("Wrong number of failed tests for canceled matrix", 0, xmlDescription.getFailed());
            } else
                Assert.fail("Matrix " + matrixFileName + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * try to delete TestScriptRun
     */
    @Test
    public void testDeleteScript() throws Exception {
        logger.info("Start testDeleteScript()");
        try {
            MatrixList matrixList = sfapi.getMatrixList();
            matrix = getMatrixFromList(matrixList, matrixFile);
            if (matrix == null) {
                int matrixId = (int) sfapi.uploadMatrix(TestMatrixPositive.class.getClassLoader().getResourceAsStream(matrixFile), matrixFile)
                        .getId();
                matrixList = sfapi.getMatrixList();
                matrix = getMatrixFromList(matrixList, matrixId, matrixFile);
            }
            if (matrix != null) {
                int testScriptId = (int) sfapi.runMatrix(matrix).getId();
                int timewait = 0;
                while (timewait != 600 && sfapi.getTestScriptRunInfo(testScriptId).isLocked()) {
                    timewait++;
                    Thread.sleep(100);
                }
                sfapi.deleteTestScriptRun(testScriptId);
                List<TestScriptRun> testScriptRuns = sfapi.getTestScriptRunList();
                boolean scriptIsDelete = true;
                for (TestScriptRun run : testScriptRuns) {
                    if (testScriptId == run.getId())
                        scriptIsDelete = false;
                }
                Assert.assertTrue("Script hasn't be delete. getTestScriptRunList() contain it", scriptIsDelete);
            } else
                Assert.fail("Matrix " + matrixFile + " hasn't been uploaded");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testRangeForExcludedTestCase() throws Exception {
        logger.info("Start testRangeForExcludedTestCase()");
        try {
            sfapi.createEnvironment(ENVIRONMENT);
            try (AutoCloseable deleteEnv = () -> sfapi.deleteEnvironment(ENVIRONMENT)) {

                startService(sfapi, SERVER, SERVER_NAME, ENVIRONMENT);
                try (AutoCloseable deleteServer = () -> sfapi.deleteService(ENVIRONMENT, SERVER_NAME)) {

                    startService(sfapi, CLIENT, CLIENT_NAME, ENVIRONMENT);
                    try (AutoCloseable deleteClient = () -> sfapi.deleteService(ENVIRONMENT, CLIENT_NAME)) {

                        int matrixId = (int) sfapi.uploadMatrix(getClass().getClassLoader().getResourceAsStream(matrixFileRangeForExcludeCase), matrixFileRangeForExcludeCase)
                                .getId();
                        Matrix matrixRangeForExcludeCase = getMatrixFromList(sfapi.getMatrixList(), matrixId, matrixFileRangeForExcludeCase);
                        if (matrixRangeForExcludeCase != null) {
                            try (AutoCloseable deletMatrix = () -> sfapi.deleteMatrix(matrixRangeForExcludeCase)) {

                                int testScriptId = (int) sfapi
                                        .performMatrixAction(matrixRangeForExcludeCase, "start", "1,2", ENVIRONMENT, "UTF-8",
                                                3, false, false, true, true, null, null, null)
                                        .getId();
                                runs.add(testScriptId);
                                int timewait = 0;
                                while (timewait != 600 && sfapi.getTestScriptRunInfo(testScriptId).isLocked()) {
                                    timewait++;
                                    Thread.sleep(100);
                                }
                                XmlTestscriptRunDescription description = sfapi.getTestScriptRunInfo(testScriptId);
                                Assert.assertEquals("Unexpected testcase count", 1, description.getTestcases().size());
                                XmlTestCaseDescription testcase = description.getTestcases().get(0);
                                Assert.assertEquals("Unexpected testcase description", "First", testcase.getDescription());
                                Assert.assertEquals("Unexpected testcase id", "1", testcase.getId());
                            }
                        } else {
                            Assert.fail("Matrix " + matrixFileRangeForExcludeCase + " hasn't been uploaded");
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }
}
