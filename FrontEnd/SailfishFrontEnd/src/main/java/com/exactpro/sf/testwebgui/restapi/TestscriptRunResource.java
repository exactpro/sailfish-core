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
package com.exactpro.sf.testwebgui.restapi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.Action;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.ReportRoot;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.TestCase;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.TestCaseMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.SerializeUtil;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.scriptrunner.IScriptProgress;
import com.exactpro.sf.scriptrunner.TestScriptDescription;
import com.exactpro.sf.scriptrunner.TestScriptDescription.ScriptState;
import com.exactpro.sf.testwebgui.api.TestToolsAPI;
import com.exactpro.sf.testwebgui.restapi.xml.XmlFailedAction;
import com.exactpro.sf.testwebgui.restapi.xml.XmlResponse;
import com.exactpro.sf.testwebgui.restapi.xml.XmlTestCaseDescription;
import com.exactpro.sf.testwebgui.restapi.xml.XmlTestSciptrunList;
import com.exactpro.sf.testwebgui.restapi.xml.XmlTestScriptShortReport;
import com.exactpro.sf.testwebgui.restapi.xml.XmlTestscriptRunDescription;

import static com.exactpro.sf.storage.impl.DefaultTestScriptStorage.REPORT_DIR;

@Path("testscriptruns")
public class TestscriptRunResource {

	private static final Logger logger = LoggerFactory.getLogger(TestscriptRunResource.class);

	private static final String DATE_FORMAT = "yyyyMMdd_HHmmss";
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }

    @GET
    @Produces(MediaType.APPLICATION_XML)
	public Response handlerGET() {
	    return getTestscriptRunsList(false);
    }

	/**
	 * Skips the generation of run descriptions
	 * @return
	 */
	@HEAD
	public Response handlerHEAD() {
        return getTestscriptRunsList(true);
    }

    @GET
	@Path("{testscriptrunid}")
	@Produces(MediaType.APPLICATION_XML)
    public Response executeAction(@PathParam("testscriptrunid") long testScriptRunId, @QueryParam("action") String actionName) {
		String errorMessage = null;
		String rootCause = null;

		try {
			TestScriptDescription testScriptRun = SFLocalContext.getDefault().getScriptRunner().getTestScriptDescription(testScriptRunId);

			if ( testScriptRun != null ) {

				if ( actionName == null ) {
					XmlTestscriptRunDescription xmlDescr = convertTestScriptDescription(testScriptRun);

					return Response.
			                status(Response.Status.OK).
			                entity(xmlDescr).
			                build();
				} else if ( actionName.equals("report")) {

					try {
                        if (testScriptRun.isLocked()) {
                            return getLockedReportResponse();
                        }
                        final File reportFolder = SFLocalContext.getDefault().getWorkspaceDispatcher().getFile(FolderType.REPORT, testScriptRun.getWorkFolder(), REPORT_DIR);
                        File zipFile = File.createTempFile(UUID.randomUUID().toString(), ".zip");

                        try (ZipOutputStream archive = new ZipOutputStream(new FileOutputStream(zipFile))) {
                            Consumer<java.nio.file.Path> reportZipper = path -> {
                                try {
                                    archive.putNextEntry(new ZipEntry(path.toFile().getName()));
                                    Files.copy(path, archive);
                                    archive.closeEntry();
                                } catch (IOException e) {
                                    throw new EPSCommonException(e);
                                }
                            };

                            Files.walk(reportFolder.toPath()).filter(path -> path.toFile().isFile()).forEach(reportZipper);
                        }

						StreamingOutput stream = null;

						stream = out -> {

                            try {
                                Files.copy(zipFile.toPath(), out);
                            } catch (Exception e) {
                                logger.error(e.getMessage(), e);
                                throw new WebApplicationException(e);
                            }
                        };

						return Response
								.ok(stream)
								.header("content-disposition",
										"attachment; filename = "
												+ zipFile.getName()).build();
					} catch (FileNotFoundException e) {
					    logger.error(e.getMessage(), e);
						errorMessage = "report doesn't exists";
					}
				}
                else if (actionName.equals("shortreport")) {
                    XmlTestScriptShortReport xmlProp = createShortReport(testScriptRun);

                    return Response.status(Response.Status.OK)
                            .entity(xmlProp)
                            .build();
                }
				else if ( actionName.equals("reportzip")) {

					try {
                        if (testScriptRun.isLocked()) {
                            return getLockedReportResponse();
                        }
					    File reportFile = TestToolsAPI.getInstance().getTestScriptRunZip(testScriptRunId);

						final InputStream zipInput = new FileInputStream(reportFile);

						StreamingOutput stream = null;

						stream = new StreamingOutput() {
							@Override
							public void write(OutputStream out)
									throws IOException, WebApplicationException {

								try (InputStream in = zipInput) {
									int read = 0;
									byte[] bytes = new byte[1024];

									while ((read = in.read(bytes)) != -1) {
										out.write(bytes, 0, read);
									}
								} catch (Exception e) {
								    logger.error(e.getMessage(), e);
									throw new WebApplicationException(e);
								}
							}
						};

						return Response
								.ok(stream)
								.header("content-disposition",
										"attachment; filename = "
												+ reportFile.getName()).build();
					} catch (FileNotFoundException e) {
					    logger.error(e.getMessage(), e);
						errorMessage = "report doesn't exists";
					}
				} else if ( actionName.equals("stop") ) {
                    try {
                        TestToolsAPI.getInstance().stopScriptRun(testScriptRunId);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                        XmlResponse xmlResponse = new XmlResponse();

                        xmlResponse.setMessage(e.getMessage());
                        xmlResponse.setRootCause(e.getCause().getMessage());
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(xmlResponse).build();
                    }
                    XmlResponse xmlResponse = new XmlResponse();

                    xmlResponse.setMessage("Testscripts " + testScriptRunId + " stoped succesfully");
                    xmlResponse.setRootCause(rootCause);
                    return Response.status(Response.Status.OK).entity(xmlResponse).build();
				} else if( actionName.equals("compileScript") ) {
					try{
						SFLocalContext.getDefault().getScriptRunner().compileScript(testScriptRunId);
					}catch(Exception e){
					    logger.error(e.getMessage(), e);
						XmlResponse xmlResponse = new XmlResponse();

                        xmlResponse.setMessage(e.getMessage());
                        xmlResponse.setRootCause(e.getCause().getMessage());
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(xmlResponse).build();
					}
					XmlResponse xmlResponse = new XmlResponse();

                    xmlResponse.setMessage("Testscripts " + testScriptRunId + " compiled succesfully");
                    xmlResponse.setRootCause(rootCause);
                    return Response.status(Response.Status.OK).entity(xmlResponse).build();
				} else if( actionName.equals("runCompileScript") ) {
					try{
						SFLocalContext.getDefault().getScriptRunner().runCompiledScript(testScriptRunId);
					}catch(Exception e){
						XmlResponse xmlResponse = new XmlResponse();

                        xmlResponse.setMessage(e.getMessage());
                        xmlResponse.setRootCause(e.getCause().getMessage());
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(xmlResponse).build();
					}
					XmlResponse xmlResponse = new XmlResponse();

                    xmlResponse.setMessage("Testscripts " + testScriptRunId + " run compiled script succesfully");
                    xmlResponse.setRootCause(rootCause);
                    return Response.status(Response.Status.OK).entity(xmlResponse).build();
                } else {
                    errorMessage = "unknown action";
                }

			} else {
                errorMessage = "unknown testscriptrun";
            }
		} catch ( Throwable e ) {
		    logger.error(e.getMessage(), e);
		    errorMessage = e.getMessage();
			rootCause = ( e.getCause() != null ) ?  e.getCause().getMessage() : null;
		}

		if ( errorMessage == null ) {

		    errorMessage = "Unexpected error";
		}

        XmlResponse xmlResponse = new XmlResponse();

        xmlResponse.setMessage(errorMessage);
        xmlResponse.setRootCause(rootCause);

        return Response.status(Response.Status.BAD_REQUEST).entity(xmlResponse).build();



    }

    @GET
    @Path("delete/{testscriptrunid}")
    @Produces(MediaType.APPLICATION_XML)
    public Response deleteReport(@PathParam("testscriptrunid") long testScriptRunId,
                                 @DefaultValue("false") @QueryParam("deleteOnDisk") boolean deleteOnDisk) {
        String errorMessage = null;
        String rootCause = null;

        try {
            if (testScriptRunId != 0) {
                TestScriptDescription testScriptRun = SFLocalContext.getDefault().getScriptRunner().getTestScriptDescription(testScriptRunId);

                if ( testScriptRun != null ) {
                    if(testScriptRun.isLocked()){
                        errorMessage = "the run of the matrix did not end";
                    }else {
                        logger.info("Wait unlock report: " + testScriptRunId);
                        Thread.sleep(1000);

                        if(testScriptRun.isLocked()){
                            errorMessage = "the run of the matrix did not end";
                        }else {
                            SFLocalContext.getDefault().getScriptRunner().removeTestScripts(deleteOnDisk, Arrays.asList(testScriptRunId));
                            XmlResponse xmlResponse = new XmlResponse();
                            xmlResponse.setMessage("Script report " + testScriptRunId + " was successfully deleted.");
                            return Response.status(Response.Status.OK).entity(xmlResponse).build();
                        }
                    }

                } else {
                    errorMessage = "unknown testscriptrun";
                }
            } else {
                SFLocalContext.getDefault().getScriptRunner().removeAllTestScripts(deleteOnDisk);
                XmlResponse xmlResponse = new XmlResponse();
                xmlResponse.setMessage("All script report was successfully deleted.");
                return Response.status(Response.Status.OK).entity(xmlResponse).build();
            }
        }
        catch ( Throwable e ) {
            logger.error(e.getMessage(), e);
            errorMessage = e.getMessage();
            rootCause = ( e.getCause() != null ) ?  e.getCause().getMessage() : null;
        }

        if ( errorMessage != null ) {

            XmlResponse xmlResponse = new XmlResponse();

            xmlResponse.setMessage(errorMessage);
            xmlResponse.setRootCause(rootCause);

            return Response.
                    status(Response.Status.BAD_REQUEST).
                    entity(xmlResponse).
                    build();
        }

        return null;
    }

    @GET
    @Path("delete")
    @Produces(MediaType.APPLICATION_XML)
    public Response deleteAllReport(@DefaultValue("false") @QueryParam("deleteOnDisk") boolean deleteOnDisk) {
        String errorMessage = null;
        String rootCause = null;

        try {
            List<TestScriptDescription> errors = SFLocalContext.getDefault().getScriptRunner().removeAllTestScripts(deleteOnDisk);
            XmlResponse xmlResponse = new XmlResponse();
            if(errors.isEmpty()){
                xmlResponse.setMessage("All script report was successfully deleted.");
            }else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to remove reports with id: ");
                for(TestScriptDescription error : errors){
                    stringBuilder.append(error.getId());
                    stringBuilder.append(",");
                }
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                xmlResponse.setMessage(stringBuilder.toString());
            }

            return Response.status(Response.Status.OK).entity(xmlResponse).build();
        }
        catch ( Throwable e ) {
            logger.error(e.getMessage(), e);
            errorMessage = e.getMessage();
            rootCause = ( e.getCause() != null ) ?  e.getCause().getMessage() : null;
        }

        if ( errorMessage != null ) {

            XmlResponse xmlResponse = new XmlResponse();

            xmlResponse.setMessage(errorMessage);
            xmlResponse.setRootCause(rootCause);

            return Response.
                    status(Response.Status.BAD_REQUEST).
                    entity(xmlResponse).
                    build();
        }
        return null;
    }

    @GET
    @Path("update/{testscriptrunid}")
    @Produces(MediaType.APPLICATION_XML)
    public Response updateTestScriptRun(@PathParam("testscriptrunid") long testScriptRunId,
                                        @QueryParam("sfCurrentID") long sfCurrentID) {
        XmlResponse xmlResponse = new XmlResponse();
        Response.Status status = Response.Status.OK;
        String message;

        try {
            TestScriptDescription testScriptRun = SFLocalContext.getDefault().getScriptRunner().getTestScriptDescription(testScriptRunId);
            if (testScriptRun == null) {
                status = Response.Status.BAD_REQUEST;
                message = String.format(
                        "Could not update test script run - TestScriptDescription with [%s] id is missed", testScriptRunId);
            } else {
                long matrixRunId = testScriptRun.getMatrixRunId();
                SFLocalContext.getDefault().getStatisticsService().getStorage().updateSfCurrentID(matrixRunId, sfCurrentID);
                message = String.format("sf current id was successfully set for test script run [%s]", testScriptRunId);
            }
        } catch ( Throwable e ) {
            logger.error("Could not update test script run with [{}] id. Message: {}", testScriptRunId, e.getMessage(), e);
            String rootCause = ( e.getCause() != null ) ?  e.getCause().getMessage() : null;
            message = e.getMessage();
            xmlResponse.setRootCause(rootCause);
            status = Response.Status.INTERNAL_SERVER_ERROR;
        }

        xmlResponse.setMessage(message);
        return Response.
                status(status).
                entity(xmlResponse).
                build();
    }

    private Response getTestscriptRunsList(boolean isHead) {

        XmlTestSciptrunList testScriptRunList = null;

        if (!isHead) {
        	List<XmlTestscriptRunDescription> runList = new ArrayList<>();
			for (TestScriptDescription testScriptDescription : SFLocalContext.getDefault().getScriptRunner().getDescriptions()) {
				XmlTestscriptRunDescription xmlDescr = convertTestScriptDescription(testScriptDescription);
				runList.add(xmlDescr);
			}
    		testScriptRunList = new XmlTestSciptrunList();
    		testScriptRunList.setTestscriptRuns(runList);
        }
    	return Response.ok(testScriptRunList).build();
    }

    private XmlTestscriptRunDescription convertTestScriptDescription(
            TestScriptDescription testScriptDescr) {

        XmlTestscriptRunDescription xmlDescr = new XmlTestscriptRunDescription();

        xmlDescr.setId(testScriptDescr.getId());
        xmlDescr.setMatrixFileName(testScriptDescr.getMatrixFileName());
        xmlDescr.setDescription(testScriptDescr.getDescription());
        xmlDescr.setScriptState(testScriptDescr.getState().name());
        xmlDescr.setScriptStatus(testScriptDescr.getStatus().name());
        xmlDescr.setPassed(testScriptDescr.getContext().getScriptProgress().getPassed());
        xmlDescr.setConditionallyPassed(testScriptDescr.getContext().getScriptProgress().getConditionallyPassed());
        xmlDescr.setFailed(testScriptDescr.getContext().getScriptProgress().getFailed());
        xmlDescr.setWorkFolder(testScriptDescr.getWorkFolder());
        xmlDescr.setSubFolder(testScriptDescr.getSubFolder());
        xmlDescr.setLocked(testScriptDescr.isLocked());

        xmlDescr.setProblem(testScriptDescr.getProblem());
        xmlDescr.setCause(testScriptDescr.getCauseMessage());

        if (ScriptState.FINISHED == testScriptDescr.getState()) {
            try {

                IWorkspaceDispatcher workspaceDispatcher = SFLocalContext.getDefault().getWorkspaceDispatcher();

                ReportRoot report = OBJECT_MAPPER
                        .readValue(workspaceDispatcher.getFile(FolderType.REPORT, testScriptDescr.getWorkFolder(), "reportData", "report.json"),
                                ReportRoot.class);


                List<XmlTestCaseDescription> list = new ArrayList<>();

                for (TestCaseMetadata metadata : report.getMetadata()) {
                    TestCase testCase = OBJECT_MAPPER.readValue(workspaceDispatcher.getFile(FolderType.REPORT, testScriptDescr
                            .getWorkFolder(), "reportData", metadata.getJsonFileName()), TestCase.class);

                    XmlTestCaseDescription xmlTestCaseDescription = new XmlTestCaseDescription();
                    xmlTestCaseDescription.setDescription(testCase.getDescription());
                    xmlTestCaseDescription.setId(String.valueOf(testCase.getId()));
                    xmlTestCaseDescription.setMatrixOrder(testCase.getMatrixOrder());
                    xmlTestCaseDescription.setTestcaseName(testCase.getName());
                    StatusType statusType = (testCase.getStatus() == null) ?
                            StatusType.NA :
                            testCase.getStatus().getStatus();
                    xmlTestCaseDescription.setStatus(statusType.toString());


                    if (statusType.equals(StatusType.FAILED)
                            || statusType.equals(StatusType.CONDITIONALLY_FAILED)) {
                        List<?> actions = testCase.getActions();
                        for (int i = 0; i < actions.size(); i++) {
                            Action action = (Action) actions.get(i);
                            if (action != null) {
                                StatusType actionStatusType = action.getStatus().getStatus();
                                if (StatusType.FAILED.equals(actionStatusType)
                                        || StatusType.CONDITIONALLY_FAILED.equals(actionStatusType)) {
                                    XmlFailedAction failedAction = new XmlFailedAction();
                                    failedAction.setActionNumber(i++);
                                    failedAction.setActionName(action.getName());
                                    failedAction.setDescription(action.getDescription());
                                    failedAction.setCause(action.getStatus().getCause().getMessage());
                                    xmlTestCaseDescription.setFailedAction(failedAction);
                                    break;
                                }
                            }
                        }
                    }
                    list.add(xmlTestCaseDescription);
                }

                xmlDescr.setTestcases(list);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        return xmlDescr;
    }

    /**
     * @param testScriptRun
     * @return
     */
    private XmlTestScriptShortReport createShortReport(TestScriptDescription testScriptRun) {
        XmlTestScriptShortReport xmlProperties = new XmlTestScriptShortReport();

        xmlProperties.setState(testScriptRun.getState().name());
        xmlProperties.setStatus(testScriptRun.getStatus().name());
        xmlProperties.setAutostart(testScriptRun.getAutoStart());
        xmlProperties.setEnvironmentName(testScriptRun.getContext().getEnvironmentName());
        xmlProperties.setStartTime(testScriptRun.getStartedTime());
        xmlProperties.setFinishTime(testScriptRun.getFinishedTime());
        xmlProperties.setLanguageURI(testScriptRun.getLanguageURI().toString());
        xmlProperties.setUser(testScriptRun.getUsername());
        xmlProperties.setScriptFolder(testScriptRun.getWorkFolder());
        xmlProperties.setMatrixFileName(testScriptRun.getMatrixFileName());
        xmlProperties.setRange(testScriptRun.getRange());
        xmlProperties.setServices(testScriptRun.getServices());
        xmlProperties.setTimestamp(testScriptRun.getTimestamp().getTime());
        xmlProperties.setLocked(testScriptRun.isLocked());

        IScriptProgress progress = testScriptRun.getContext().getScriptProgress();
        xmlProperties.setConditionallyPassed(progress.getConditionallyPassed());
        xmlProperties.setFailed(progress.getFailed());
        xmlProperties.setPassed(progress.getPassed());
        xmlProperties.setTotal(progress.getLoaded());

        if (testScriptRun.getCause() != null) {
            xmlProperties.setCause(SerializeUtil.serializeToBase64(testScriptRun.getCause()));
        }
        return xmlProperties;
    }

    private Response createBadResponse(String message, String rootCause) {
        XmlResponse xmlResponse = new XmlResponse();

        xmlResponse.setMessage(message);
        xmlResponse.setRootCause(rootCause);

        return Response.status(Response.Status.BAD_REQUEST).entity(xmlResponse).build();
    }

    private String getRootCause(Throwable e) {
        if (e.getCause() != null) {
            return getRootCause(e.getCause());
        }
        return e.getMessage();
    }

    private Response getLockedReportResponse() {
        XmlResponse xmlResponse = new XmlResponse();

        xmlResponse.setMessage("Report hasn't been unlocked yet");
        xmlResponse.setRootCause(null);

        return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(xmlResponse)
                .build();
    }
}
