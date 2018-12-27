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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.embedded.statistics.StatisticsService;
import com.exactpro.sf.embedded.statistics.entities.Tag;
import com.exactpro.sf.matrixhandlers.LocalMatrixProviderFactory;
import com.exactpro.sf.storage.IMatrix;
import com.exactpro.sf.storage.IMatrixStorage;
import com.exactpro.sf.testwebgui.api.TestToolsAPI;
import com.exactpro.sf.testwebgui.restapi.xml.MatrixList;
import com.exactpro.sf.testwebgui.restapi.xml.XmlMatrixDescription;
import com.exactpro.sf.testwebgui.restapi.xml.XmlMatrixLinkUploadResponse;
import com.exactpro.sf.testwebgui.restapi.xml.XmlMatrixUploadResponse;
import com.exactpro.sf.testwebgui.restapi.xml.XmlResponse;
import com.exactpro.sf.testwebgui.restapi.xml.XmlRunReference;
import com.exactpro.sf.testwebgui.restapi.xml.XmlRunReference.SetOption;
import com.exactpro.sf.testwebgui.restapi.xml.XmlRunReference.SwitchOption;
import com.exactpro.sf.testwebgui.restapi.xml.XmlTestSciptrunActionList;
import com.exactpro.sf.testwebgui.restapi.xml.XmlTestscriptActionResponse;
import com.exactpro.sf.testwebgui.scriptruns.MatrixUtil;
import com.exactpro.sf.testwebgui.scriptruns.TestScriptsBean;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Path("scripts")
public class MatrixResource {

	private static final Logger logger = LoggerFactory.getLogger(MatrixResource.class);

	private List<Tag> tagNamesToInstances(List<String> names) {

		List<Tag> result = new ArrayList<>();

		StatisticsService statisticsService = SFLocalContext.getDefault().getStatisticsService();

		if(!statisticsService.isConnected()) {

			throw new IllegalStateException("Statistics DB is not available (disconnected)");

		}

		for(String name : names) {

			Tag tag = statisticsService.getStorage().getTagByName(name);

			if(tag == null) {
				throw new IllegalArgumentException("Tag [" + name + "] not found in DB!");
			}

			result.add(tag);

		}

		return result;
	}

	private Map<String, String> getStaticVariablesMap(String staticVariables) {
	    try {
	        ObjectMapper mapper = new ObjectMapper();
	        TypeReference<Map<String, String>> typeRef = new TypeReference<Map<String,String>>() {};

	        return mapper.readValue(staticVariables, typeRef);
	    } catch(Exception e) {
	        logger.error(e.getMessage(), e);
	    }

	    return null;
	}

    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response matricesList(){
        ISFContext context = SFLocalContext.getDefault();
        try {
            List<XmlMatrixDescription> list = new ArrayList<>();
            for (IMatrix matrix: context.getMatrixStorage().getMatrixList()) {
                XmlMatrixDescription matrixDescription = new XmlMatrixDescription();
                matrixDescription.setId(matrix.getId());
                matrixDescription.setName(matrix.getName());
                matrixDescription.setDate(matrix.getDate());
                list.add(matrixDescription);

            }
            MatrixList matrixList = new MatrixList();
            matrixList.setMatrixList(list);
            return Response.ok(matrixList).build();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            XmlResponse response = new XmlResponse();
            response.setMessage("Can not retrive matrix list");
            response.setRootCause(e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        }
    }

	@POST
	@Path("upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(
		@FormDataParam("file") InputStream uploadedInputStream,
		@FormDataParam("file") FormDataContentDisposition fileDetail) {

		IMatrix uploaded = null;

		String errorMessage = null;
		String rootCause = null;

		Response reqResponse = null;

		try {
			uploaded = TestToolsAPI.getInstance().uploadMatrix(uploadedInputStream,
					fileDetail.getFileName(), null, "Unknown creator", null, null, null);

			XmlMatrixUploadResponse matrixUploadResponse = new XmlMatrixUploadResponse();

			matrixUploadResponse.setId(uploaded.getId());

			reqResponse = Response.ok(matrixUploadResponse).build();
		} catch (Throwable e) {
			logger.error("Could not store matrice [{}]", ((fileDetail != null) ? fileDetail.getFileName() : "null") , e);
			errorMessage = e.getMessage();
			rootCause = ExceptionUtils.getRootCauseMessage(e);
		}

		if (errorMessage != null) {

			XmlResponse xmlResponse = new XmlResponse();

			xmlResponse.setMessage(errorMessage);
			xmlResponse.setRootCause(rootCause);

			reqResponse = Response.status(Response.Status.BAD_REQUEST)
					.entity(xmlResponse).build();
		}

		return reqResponse;
 	}

    @POST
    @Path("uploadLink")
    public Response uploadFile(@QueryParam("link") String link,
                               @DefaultValue(LocalMatrixProviderFactory.ALIAS) @QueryParam("provider_uri")
                                       String providerURI) {

        List<IMatrix> uploaded = null;

        String errorMessage = null;
        String rootCause = null;

        Response reqResponse = null;

        if (StringUtils.isEmpty(link)) {
            XmlResponse xmlResponse = new XmlResponse();
            xmlResponse.setMessage("Parameter 'link' cannot be empty");

            return Response.status(Response.Status.BAD_REQUEST).entity(xmlResponse).build();
        }

        try {
            uploaded = MatrixUtil.addMatrixByLink(SFLocalContext.getDefault().getMatrixProviderHolder(), link,
                                                  SailfishURI.parse(providerURI));

            XmlMatrixLinkUploadResponse matrixUploadResponse = new XmlMatrixLinkUploadResponse();

            matrixUploadResponse.setMatrices(uploaded.stream()
                                                     .collect(Collectors.toMap(IMatrix::getName, IMatrix::getId)));

            reqResponse = Response.ok(matrixUploadResponse).build();
        } catch (Throwable e) {
            logger.error("Could not store matrix [{}]", link, e);
            errorMessage = e.getMessage();
            rootCause = ExceptionUtils.getRootCauseMessage(e);
        }

        if (errorMessage != null) {

            XmlResponse xmlResponse = new XmlResponse();

            xmlResponse.setMessage(errorMessage);
            xmlResponse.setRootCause(rootCause);

            reqResponse = Response.status(Response.Status.BAD_REQUEST).entity(xmlResponse).build();
        }

        return reqResponse;
    }

    @GET
	@Path("convert")
    public Response convertMatrix(@QueryParam("matrix_id") Long matrixId,
            @DefaultValue(ServiceName.DEFAULT_ENVIRONMENT) @QueryParam("environment") String environment,
            @QueryParam("converter_uri") String converterUri) {

	    if (matrixId == null) {
	        XmlResponse xmlResponse = new XmlResponse();
	        xmlResponse.setMessage("Parameter 'matrix_id' cannot be empty");

	        return Response.status(Response.Status.BAD_REQUEST)
                    .entity(xmlResponse).build();
	    }

        if(converterUri == null) {
            XmlResponse xmlResponse = new XmlResponse();
            xmlResponse.setMessage("Parameter 'converter_uri' cannot be empty");

            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(xmlResponse).build();
        }

		try {
            Long convertedMatrixId = TestToolsAPI.getInstance().convertMatrix(matrixId, environment, SailfishURI.parse(converterUri));

			XmlMatrixUploadResponse matrixUploadResponse = new XmlMatrixUploadResponse();

			matrixUploadResponse.setId(convertedMatrixId);

			return Response.ok(matrixUploadResponse).build();
		} catch (Throwable e) {
			logger.error(e.getMessage() , e);
			XmlResponse xmlResponse = new XmlResponse();

            xmlResponse.setMessage(e.getMessage());
			xmlResponse.setRootCause((e.getCause() != null) ? e.getCause().getMessage() : null);

            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(xmlResponse).build();

		}
 	}

	@GET
	@Path("download")
	@Produces(MediaType.APPLICATION_XML)
	public Response downloadMatrix(@QueryParam("id") Long id) {

		String errorMessage = null;
		String rootCause = null;

		Response reqResponse = null;

		try {
			IMatrixStorage matrixStorage = SFLocalContext.getDefault().getMatrixStorage();
			IMatrix matrix = matrixStorage.getMatrixById(id);
			final File file = SFLocalContext.getDefault().getWorkspaceDispatcher().getFile(FolderType.MATRIX, matrix.getFilePath());

			if (file.exists()) {
				StreamingOutput stream;

				stream = new StreamingOutput() {
					@Override
					public void write(OutputStream out) throws IOException, WebApplicationException {
						try (InputStream in = new FileInputStream(file)) {
							int read;
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
										+ file.getName()).build();
			}
		} catch (Throwable e) {
			logger.error(e.getMessage() , e);
			errorMessage = e.getMessage();
			rootCause = ExceptionUtils.getRootCauseMessage(e);
		}

		if (errorMessage != null) {

			XmlResponse xmlResponse = new XmlResponse();

			xmlResponse.setMessage(errorMessage);
			xmlResponse.setRootCause(rootCause);

			reqResponse = Response.status(Response.Status.BAD_REQUEST)
					.entity(xmlResponse).build();
		}

		return reqResponse;
 	}

    @GET
    @Path("reference/run")
    @Produces(MediaType.APPLICATION_XML)
    public Response getRunReference() {
        try {
            TestToolsAPI testToolsAPI = TestToolsAPI.getInstance();
            ISFContext context = SFLocalContext.getDefault();

            XmlRunReference runReference = new XmlRunReference()
                    .addSwitchOption(new SwitchOption("continueonfailed", "Continue if failed"))
                    .addSwitchOption(new SwitchOption("autostart", "Auto start"))
                    .addSwitchOption(new SwitchOption("ignoreaskforcontinue", "Ignore ask for continue"))
                    .addSwitchOption(new SwitchOption("runnetdumper", "Run NetDumper when matrix starts"))
                    .addOneItemOption(new SetOption("environment", testToolsAPI.getEnvNames()))
                    .addOneItemOption(new SetOption("language", context.getLanguageManager().getLanguageURIs().stream()
                            .map(Object::toString)
                            .collect(Collectors.toList())))
                    .addOneItemOption(new SetOption("encoding", TestScriptsBean.ENCODE_VALUES));

            if (context.getStatisticsService().isConnected()) {
                runReference.addMultiItemOption(new SetOption("tag", context.getStatisticsService().getStorage().getAllTags().stream()
                        .map(Tag::getName)
                        .collect(Collectors.toList())));
            }

            return Response.status(Response.Status.OK)
                    .entity(runReference)
                    .build();
        } catch (RuntimeException e) {
            logger.error(e.getMessage(), e);
            return createBadResponse(e.getMessage(), ExceptionUtils.getRootCauseMessage(e));
        }
    }

	@GET
	@Path("name_{matrixname}")
	@Produces(MediaType.APPLICATION_XML)
    public Response executeAction(
    		@PathParam("matrixname") String matrixName,
    		@QueryParam("action") String actionName,
    		@QueryParam("range") String rangeParam,
    		@DefaultValue(ServiceName.DEFAULT_ENVIRONMENT) @QueryParam("environment") String environmentParam,
    		@DefaultValue("ISO-8859-1") @QueryParam("encoding") String fileEncodingParam,
    		@DefaultValue("2") @QueryParam("aml") int amlParam,
    		@DefaultValue("false") @QueryParam("continueonfailed") boolean continueOnFailed,
    		@DefaultValue("false") @QueryParam("autostart") boolean autoStart,
    		@DefaultValue("true") @QueryParam("autorun") boolean autoRun,
    		@DefaultValue("true") @QueryParam("ignoreaskforcontinue") boolean ignoreAskForContinue,
    		@DefaultValue("true") @QueryParam("runnetdumper") boolean runNetDumper,
            @DefaultValue("false") @QueryParam("skipoptional") boolean skipOptional,
    		@QueryParam("tag") final List<String> tags,
    		@DefaultValue("{}") @QueryParam("staticvariables") String staticVariables,
			@DefaultValue("") @QueryParam("subfolder") String subFolder,
			@DefaultValue("") @QueryParam("language") String language) {

	    if("stop".equals(actionName)) {
	        XmlResponse xmlResponse = new XmlResponse();

            xmlResponse.setMessage("Cannot stop matrix by it's name");
            xmlResponse.setRootCause(null);

            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(xmlResponse).build();
	    }

		ISFContext context = SFLocalContext.getDefault();

		long matrixId = -1;

		for ( IMatrix curMatrix :  context.getMatrixStorage().getMatrixList() ) {
			if ( curMatrix.getName().equals(matrixName) )  {
				matrixId = curMatrix.getId();
				break;
			}
		}

		if (matrixId != -1) {

			return this.executeAction(matrixId, actionName, rangeParam,
					environmentParam, fileEncodingParam, amlParam,
					continueOnFailed, autoStart, autoRun,
					ignoreAskForContinue, runNetDumper, skipOptional, tags, staticVariables, subFolder, language);

		} else {
			XmlResponse xmlResponse = new XmlResponse();

			xmlResponse.setMessage("Cannot find matrix " + matrixName);
			xmlResponse.setRootCause(null);

			return Response.status(Response.Status.BAD_REQUEST)
					.entity(xmlResponse).build();
		}


	}

	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_XML)
    public Response executeAction(
    		@PathParam("id") long id,
    		@QueryParam("action") String actionName,
    		@QueryParam("range") String rangeParam,
    		@DefaultValue(ServiceName.DEFAULT_ENVIRONMENT) @QueryParam("environment") String environmentParam,
    		@DefaultValue("ISO-8859-1") @QueryParam("encoding") String fileEncodingParam,
    		@DefaultValue("2") @QueryParam("aml") int amlParam,
    		@DefaultValue("false") @QueryParam("continueonfailed") boolean continueOnFailed,
    		@DefaultValue("false") @QueryParam("autostart") boolean autoStart,
    		@DefaultValue("true") @QueryParam("autorun") boolean autoRun,
    		@DefaultValue("true") @QueryParam("ignoreaskforcontinue") boolean ignoreAskForContinue,
    		@DefaultValue("true") @QueryParam("runnetdumper") boolean runNetDumper,
    		@DefaultValue("false") @QueryParam("skipoptional") boolean skipOptional,
    		@QueryParam("tag") final List<String> tags,
    		@DefaultValue("{}") @QueryParam("staticvariables") String staticVariables,
    		@DefaultValue("") @QueryParam("subfolder") String subFolder,
    		@DefaultValue("") @QueryParam("language") String language) {

		ISFContext context = SFLocalContext.getDefault();

		String errorMessage = null;
		String rootCause = null;

		try {
			if ( actionName == null ) {
				errorMessage = "action is null";
			}
			else if ( actionName.equals("start") ) {
			    IMatrix matrix = context.getMatrixStorage().getMatrixById(id);

			    if(matrix != null) {
    				List<Tag> tagsList = null;

    				if(tags != null && !tags.isEmpty()) {

    					tagsList = tagNamesToInstances(tags);

    				}

    				logger.debug("Before adding matrix {} to queue...", matrix);

    				language = StringUtils.isNotBlank(language) ? language.trim() : "AML_v" + amlParam; //workaround
    				SailfishURI languageURI = SailfishURI.parse(language);

    				if(!SFLocalContext.getDefault().getLanguageManager().containsLanguage(languageURI)) {
    				    throw new EPSCommonException("Invalid language: " + language);
    				}

    				long enqueuedID = TestToolsAPI.getInstance().executeMatrix(
    						matrix, languageURI, rangeParam,
    						fileEncodingParam, environmentParam,
                            RESTUtil.getSystemUser(RESTUtil.REST_USER), continueOnFailed, autoStart,
    						autoRun, ignoreAskForContinue, runNetDumper, skipOptional, tagsList,
    						getStaticVariablesMap(staticVariables), null, subFolder);

    				logger.info("Test Script {} was enqueued under {}", matrix, enqueuedID );

    				XmlTestscriptActionResponse response = new XmlTestscriptActionResponse();
    				response.setId(enqueuedID);

    				return Response.status(Response.Status.OK).
    		                entity(response).
		                build();
			    } else {
			        errorMessage = "Matrix with id = [" + id + "] not found";
			    }
			}
			else if ( actionName.equals("stop") ) {
			    if(TestToolsAPI.getInstance().containsScriptRun(id)) {
    			    TestToolsAPI.getInstance().stopScriptRun(id);
    				return Response.noContent().status(Response.Status.OK).build();
			    } else {
			        errorMessage = "Script run with id = [" + id + "] not found";
			    }
			}
			else errorMessage = "unknown action";
		}
		catch ( Throwable e ) {
			logger.error(e.getMessage() , e);
			errorMessage = e.getMessage();
			rootCause = ExceptionUtils.getRootCauseMessage(e);
		}

		if ( errorMessage != null ) {

			return createBadResponse(errorMessage, rootCause);
		}

		return null;
    }

	@GET
	@Path("all")
	@Produces(MediaType.APPLICATION_XML)
    public Response executeAction(
    		@QueryParam("action") String actionName,
    		@DefaultValue(ServiceName.DEFAULT_ENVIRONMENT) @QueryParam("environment") String environmentParam,
    		@DefaultValue("ISO-8859-1") @QueryParam("encoding") String fileEncodingParam,
    		@DefaultValue("2") @QueryParam("aml") int amlParam,
    		@DefaultValue("false") @QueryParam("continueonfailed") boolean continueOnFailed,
    		@DefaultValue("false") @QueryParam("autostart") boolean autoStart,
    		@DefaultValue("true") @QueryParam("autorun") boolean autoRun,
    		@DefaultValue("true") @QueryParam("ignoreaskforcontinue") boolean ignoreAskForContinue,
    		@DefaultValue("true") @QueryParam("runnetdumper") boolean runNetDumper,
            @DefaultValue("false") @QueryParam("skipoptional") boolean skipOptional,
    		@QueryParam("tag") final List<String> tags,
    		@DefaultValue("{}") @QueryParam("staticvariables") String staticVariables,
			@DefaultValue("") @QueryParam("subfolder") String subFolder,
			@DefaultValue("") @QueryParam("language") String language) {

		ISFContext context = SFLocalContext.getDefault();

		String errorMessage = null;
		String rootCause = null;

		try {
			if ( actionName == null ) {
				errorMessage = "action is null";
			}
			else if ( actionName.equals("start") ) {

				List<Tag> tagsList = null;

				if(tags != null && !tags.isEmpty()) {

					tagsList = tagNamesToInstances(tags);

				}

				logger.debug("Before adding all matrixs to queue...");

				List<XmlTestscriptActionResponse> responseList = new ArrayList<>();

				language = StringUtils.isNotBlank(language) ? language.trim() : "AML_v" + amlParam; //workaround
				SailfishURI languageURI = SailfishURI.parse(language);

				if(!SFLocalContext.getDefault().getLanguageManager().containsLanguage(languageURI)) {
                    throw new EPSCommonException("Invalid language: " + language);
                }

				for ( IMatrix curMatrix :  context.getMatrixStorage().getMatrixList() ) {
					XmlTestscriptActionResponse response = new XmlTestscriptActionResponse();
					response.setId(TestToolsAPI.getInstance().executeMatrix(curMatrix, languageURI,
							null, fileEncodingParam, environmentParam,
                            RESTUtil.getSystemUser(RESTUtil.REST_USER), continueOnFailed, autoStart,
							autoRun, ignoreAskForContinue, runNetDumper, skipOptional, tagsList, getStaticVariablesMap(staticVariables), null, subFolder));
					responseList.add(response);
				}

				logger.info("Test Scripts were enqueued under");

				XmlTestSciptrunActionList response = new XmlTestSciptrunActionList();
				response.setTestscriptRuns(responseList);

				return Response.status(Response.Status.OK).
		                entity(response).
		                build();
			}
			else if ( actionName.equals("stop") ) {
			    TestToolsAPI.getInstance().stopAllScriptRuns();
				return Response.noContent().status(Response.Status.OK).build();
			}
			else errorMessage = "unknown action";
		}
		catch ( Throwable e ) {
			logger.error(e.getMessage() , e);
			errorMessage = e.getMessage();
			rootCause = ExceptionUtils.getRootCauseMessage(e);
		}

		if ( errorMessage != null ) {

			return createBadResponse(errorMessage, rootCause);
		}

		return null;
    }

    @GET
    @Path("delete/{matrix_id}")
    @Produces(MediaType.APPLICATION_XML)
    public Response deleteMatrix(@PathParam("matrix_id") long matrixId) {
        ISFContext context = SFLocalContext.getDefault();
        String errorMessage = null;
        String rootCause = null;

        try {
            IMatrixStorage matrixStorage = context.getMatrixStorage();
            if (matrixId != 0) {
                IMatrix matrix = matrixStorage.getMatrixById(matrixId);
                if (matrix != null) {
                    TestToolsAPI.getInstance().deleteMatrix(matrix);

                    XmlResponse xmlResponse = new XmlResponse();
                    xmlResponse.setMessage("Matrix " + matrixId + " was successfully removed");
                    xmlResponse.setRootCause("");

                    return Response.
                            status(Response.Status.OK).
                            entity(xmlResponse).
                            build();
                } else {
                    XmlResponse xmlResponse = new XmlResponse();
                    xmlResponse.setMessage("No matrix with id " + matrixId + " was found");
                    xmlResponse.setRootCause("");

                    return Response.
                            status(Response.Status.BAD_REQUEST).
                            entity(xmlResponse).
                            build();
                }
            } else {
                TestToolsAPI.getInstance().deleteAllMatrix();

                XmlResponse xmlResponse = new XmlResponse();
                xmlResponse.setMessage("All matrices was successfully removed");
                xmlResponse.setRootCause("");

                return Response.
                        status(Response.Status.OK).
                        entity(xmlResponse).
                        build();
            }
        } catch (Exception e) {
			logger.error(e.getMessage() , e);
            errorMessage = e.getMessage();
            rootCause = ExceptionUtils.getRootCauseMessage(e);
        }

        if ( errorMessage != null ) {

            return createBadResponse(errorMessage, rootCause);
        }

        return null;
    }

    @GET
    @Path("delete/name_{matrixName}")
    @Produces(MediaType.APPLICATION_XML)
    public Response deleteMatrixByName(@PathParam("matrixName") String matrixName) {
        ISFContext context = SFLocalContext.getDefault();
        String errorMessage = null;
        String rootCause = null;

        try {
            IMatrixStorage matrixStorage = context.getMatrixStorage();
            if (matrixName != null) {
                List<IMatrix> matrices = matrixStorage.getMatrixList();

                for(IMatrix matrix:matrices) {
                    if(matrix.getName().equals(matrixName)) {
                        TestToolsAPI.getInstance().deleteMatrix(matrix);
                        break;
                    }
                }

                XmlResponse xmlResponse = new XmlResponse();
                xmlResponse.setMessage("Matrix " + matrixName + " was successfully removed");
                xmlResponse.setRootCause("");

                return Response.
                        status(Response.Status.OK).
                        entity(xmlResponse).
                        build();
            } else {
                XmlResponse xmlResponse = new XmlResponse();
                xmlResponse.setMessage("Matrix name is empty");
                xmlResponse.setRootCause("");

                return Response.
                        status(Response.Status.OK).
                        entity(xmlResponse).
                        build();
            }
        } catch (Exception e) {
			logger.error(e.getMessage() , e);
            errorMessage = e.getMessage();
            rootCause = ExceptionUtils.getRootCauseMessage(e);
        }

        if ( errorMessage != null ) {

            return createBadResponse(errorMessage, rootCause);
        }

        return null;
    }

    @GET
    @Path("delete")
    @Produces(MediaType.APPLICATION_XML)
    public Response deleteMatrix() {
        String errorMessage = null;
        String rootCause = null;

        try {
            TestToolsAPI.getInstance().deleteAllMatrix();

            XmlResponse xmlResponse = new XmlResponse();
            xmlResponse.setMessage("All matrices was successfully removed");
            xmlResponse.setRootCause("");

            return Response.
                    status(Response.Status.OK).
                    entity(xmlResponse).
                    build();
        } catch (Exception e) {
			logger.error(e.getMessage() , e);
            errorMessage = e.getMessage();
            rootCause = ExceptionUtils.getRootCauseMessage(e);
        }

        if ( errorMessage != null ) {

            return createBadResponse(errorMessage, rootCause);
        }

        return null;
    }

    private Response createBadResponse(String errorMessage, String rootCause) {
        XmlResponse xmlResponse = new XmlResponse();
    
        xmlResponse.setMessage(errorMessage);
        xmlResponse.setRootCause(rootCause);
    
        return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(xmlResponse)
                .build();
    }
}
