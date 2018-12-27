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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.SerializeUtil;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.scriptrunner.IScriptProgress;
import com.exactpro.sf.scriptrunner.ReportWriterOptions;
import com.exactpro.sf.scriptrunner.ReportWriterOptions.Duration;
import com.exactpro.sf.scriptrunner.TestScriptDescription;
import com.exactpro.sf.scriptrunner.TestScriptDescription.ScriptState;
import com.exactpro.sf.scriptrunner.reportbuilder.IXMLReportCreator;
import com.exactpro.sf.scriptrunner.reportbuilder.ReportType;
import com.exactpro.sf.scriptrunner.reportbuilder.ReportWriterException;
import com.exactpro.sf.scriptrunner.reportbuilder.XMLReportCreatorImpl;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlActionType;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlFunctionalReport;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlStatusType;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlTestCaseType;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlTestStepType;
import com.exactpro.sf.testwebgui.api.TestToolsAPI;
import com.exactpro.sf.testwebgui.restapi.xml.XmlFailedAction;
import com.exactpro.sf.testwebgui.restapi.xml.XmlResponse;
import com.exactpro.sf.testwebgui.restapi.xml.XmlTestCaseDescription;
import com.exactpro.sf.testwebgui.restapi.xml.XmlTestSciptrunList;
import com.exactpro.sf.testwebgui.restapi.xml.XmlTestScriptShortReport;
import com.exactpro.sf.testwebgui.restapi.xml.XmlTestscriptRunDescription;

@Path("testscriptruns")
public class TestscriptRunResource {

	private static final Logger logger = LoggerFactory.getLogger(TestscriptRunResource.class);

	private static final String DATE_FORMAT = "yyyyMMdd_HHmmss";
	private static final String REPORT_FILE_NAME = "report.xml";

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
						final File xmlReportFile = SFLocalContext.getDefault().getWorkspaceDispatcher().getFile(FolderType.REPORT, testScriptRun.getWorkFolder(), REPORT_FILE_NAME);

						StreamingOutput stream = null;

						stream = new StreamingOutput() {
							@Override
							public void write(OutputStream out)
									throws IOException, WebApplicationException {

								try (InputStream in = new FileInputStream(xmlReportFile)) {
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
												+ xmlReportFile.getName()).build();
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
    @Path("aggregate")
    @Produces(MediaType.APPLICATION_XML)
    public Response aggregateReport(@DefaultValue("") @QueryParam("startDate") String startDate,
            @DefaultValue("") @QueryParam("endDate") String endDate,
            @DefaultValue("false") @QueryParam("details") boolean details,
            @DefaultValue("Today") @QueryParam("duration") String duration,
            @DefaultValue("BASE") @QueryParam("type") String reportType) {
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

        try {
            ReportWriterOptions reportWriterOptions = new ReportWriterOptions();
            if (StringUtils.isNotEmpty(startDate)) {
                reportWriterOptions.setCustomStart(dateFormat.parse(startDate));
            }
            if (StringUtils.isNotEmpty(endDate)) {
                reportWriterOptions.setCustomEnd(dateFormat.parse(endDate));
            }
            reportWriterOptions.setWriteDetails(details);
            if (reportWriterOptions.getCustomStart() != null || reportWriterOptions.getCustomEnd() != null) {
                reportWriterOptions.setSelectedDuration(Duration.Custom);
            } else if (StringUtils.isNotEmpty(duration)) {
                try {
                    reportWriterOptions.setSelectedDuration(ReportWriterOptions.Duration.valueOf(duration));
                } catch (IllegalArgumentException e) {
                    logger.error(e.getMessage(), e);
                    return createBadResponse("Incorrect duration value, please use one of " + Arrays.toString(ReportWriterOptions.Duration.values()),
                            e.getMessage());
                }
            }

            if (StringUtils.isNotEmpty(reportType)) {
                try {
                    reportWriterOptions.setSelectedReportType(ReportType.valueOf(reportType));
                } catch (IllegalArgumentException e) {
                    logger.error(e.getMessage(), e);
                    return createBadResponse("Incorrect type value, please use one of " + Arrays.toString(ReportType.values()),
                            e.getMessage());
                }
            }

            final File file = TestToolsAPI.getInstance().createAggrigateReport("aggregate_report", reportWriterOptions);

            StreamingOutput stream = null;

            stream = new StreamingOutput() {
                @Override
                public void write(OutputStream out)
                        throws IOException, WebApplicationException {

                    try (InputStream in = new FileInputStream(file)) {
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

            return Response.ok(stream).header("content-disposition", "attachment; filename = " + file.getName()).build();
        } catch (ParseException e) {
            logger.error(e.getMessage(), e);
            return createBadResponse("Incorrect date format, please use " + DATE_FORMAT, e.getMessage());
        } catch (ReportWriterException | IOException e ) {
            logger.error(e.getMessage(), e);
            return createBadResponse("Creation of the aggregate report failed, reason: " + e.getMessage(), getRootCause(e));
        }
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
                IXMLReportCreator creator = new XMLReportCreatorImpl(SFLocalContext.getDefault().getWorkspaceDispatcher());
                XmlFunctionalReport report = creator.create(testScriptDescr);

                List<XmlTestCaseDescription> list = new ArrayList<>();
                for (XmlTestCaseType testcase : report.getTestcases()) {
                    XmlTestCaseDescription xmlTestCaseDescription = new XmlTestCaseDescription();
                    xmlTestCaseDescription.setDescription(testcase.getDescription());
                    xmlTestCaseDescription.setId(testcase.getId());
                    xmlTestCaseDescription.setMatrixOrder(testcase.getMatrixOrder());
                    xmlTestCaseDescription.setTestcaseName(testcase.getTestCaseName());
                    XmlStatusType statusType = testcase.getStatus() == null ? XmlStatusType.N_A : testcase.getStatus().getStatus();
                    xmlTestCaseDescription.setStatus(statusType.toString());


                    if (statusType.equals(XmlStatusType.FAILED)
                            || statusType.equals(XmlStatusType.CONDITIONALLY_FAILED)) {
                        for (int i = 0; i < testcase.getTestSteps().size(); i++) {
                            XmlTestStepType testStep = testcase.getTestSteps().get(i);
                            XmlActionType action = testStep.getAction();
                            if (action != null) {
                                if (action.getStatus().getStatus().equals(XmlStatusType.FAILED)
                                        || action.getStatus().getStatus().equals(XmlStatusType.CONDITIONALLY_FAILED)) {
                                    XmlFailedAction failedAction = new XmlFailedAction();
                                    failedAction.setActionNumber(i + 1);
                                    failedAction.setActionName(action.getName());
                                    failedAction.setDescription(action.getDescription());
                                    failedAction.setCause(action.getStatus().getDescription());
                                    xmlTestCaseDescription.setFailedAction(failedAction);
                                    break;
                                }
                            }
                        }
                    }
                    list.add(xmlTestCaseDescription);
                }
                xmlDescr.setTestcases(list);
            } catch (JAXBException | FileNotFoundException e) {
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
