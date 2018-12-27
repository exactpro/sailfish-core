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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.AMLException;
import com.exactpro.sf.aml.generator.Alert;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.generator.matrix.JavaStatement;
import com.exactpro.sf.aml.magic.IMessage2Action;
import com.exactpro.sf.aml.reader.struct.AMLBlock;
import com.exactpro.sf.aml.reader.struct.AMLElement;
import com.exactpro.sf.aml.reader.struct.AMLMatrix;
import com.exactpro.sf.aml.visitors.IAMLElementVisitor;
import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.embedded.statistics.entities.Tag;
import com.exactpro.sf.scriptrunner.IScriptReport;
import com.exactpro.sf.scriptrunner.TestScriptDescription;
import com.exactpro.sf.storage.IMatrix;
import com.exactpro.sf.storage.IMatrixStorage;
import com.exactpro.sf.testwebgui.api.TestToolsAPI;
import com.exactpro.sf.testwebgui.restapi.editor.EditorConfig;
import com.exactpro.sf.testwebgui.restapi.editor.GuiMatrixEditorScriptRunListener;
import com.exactpro.sf.testwebgui.restapi.editor.GuiMatrixEditorStorage;
import com.exactpro.sf.testwebgui.restapi.editor.IReceivedMessageProvider;
import com.exactpro.sf.testwebgui.restapi.editor.MatrixWithHistory;
import com.exactpro.sf.testwebgui.restapi.editor.RunMatrixSettings;
import com.exactpro.sf.testwebgui.restapi.editor.SfContextMatrixStorageProvider;
import com.exactpro.sf.testwebgui.restapi.editor.UnexpectedMessagesContainer;
import com.exactpro.sf.testwebgui.restapi.json.JsonAMLError;
import com.exactpro.sf.testwebgui.restapi.json.JsonColumnEntity;
import com.exactpro.sf.testwebgui.restapi.json.JsonError;
import com.exactpro.sf.testwebgui.restapi.json.JsonErrorMessage;
import com.exactpro.sf.testwebgui.restapi.json.LogMessage;
import com.exactpro.sf.testwebgui.restapi.json.editor.JsonMatrix;
import com.exactpro.sf.testwebgui.restapi.json.editor.JsonMatrixDescription;
import com.exactpro.sf.testwebgui.restapi.json.editor.JsonMatrixLine;
import com.exactpro.sf.testwebgui.restapi.json.editor.JsonMatrixList;
import com.exactpro.sf.testwebgui.restapi.json.editor.JsonMatrixTestCase;
import com.exactpro.sf.testwebgui.restapi.json.editor.JsonTestScriptDescription;
import com.exactpro.sf.testwebgui.restapi.json.editor.JsonUpdateTransaction;
import com.exactpro.sf.testwebgui.restapi.json.editor.JsonUpdateTransactionSpliceEntry;

@Path("editor")
public class MatrixEditorResource {

    private static final Logger logger = LoggerFactory.getLogger(MatrixEditorResource.class);

	public static final Column[] tc_columns = { Column.Id, Column.Reference, Column.Execute, Column.Description, Column.FailOnUnexpectedMessage, Column.Action };

	// We store GuiMatrixEditorStorage in HttpSessions

	private final String STORAGE_ATTRIBUTE = "GuiMatrixEditorStorage";

    private final static String EDITOR_USERNAME = "GUI_EDITOR";

	@Context
    private HttpServletRequest request;

	private GuiMatrixEditorStorage getMatrixEditorStorage() {
		HttpSession session = request.getSession();

		GuiMatrixEditorStorage result = (GuiMatrixEditorStorage) session.getAttribute(STORAGE_ATTRIBUTE);

		if (result == null || session.isNew()) {
			IMatrixStorage imstorage = SFLocalContext.getDefault().getMatrixStorage();
			if (imstorage == null) {
				throw new NullPointerException("MatrixStorage is null");
			}

			result = new GuiMatrixEditorStorage(new SfContextMatrixStorageProvider());
			session.setAttribute(STORAGE_ATTRIBUTE, result);
		}

		return result;
	}

	@GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response matricesList() {
		logger.info("MatrixList request");
		//FIXME Singletons should be removed from SF framework
        ISFContext context = SFLocalContext.getDefault();
        try {
            List<JsonMatrixDescription> list = new ArrayList<>();
            for (IMatrix matrix: context.getMatrixStorage().getMatrixList()) {
                list.add(
                	new JsonMatrixDescription(
                		matrix.getId(),
                		matrix.getName(),
                		matrix.getDate(),
                		matrix.getLanguageURI()));
            }
            JsonMatrixList matrixList = new JsonMatrixList(list);
            return Response.ok(matrixList).build();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            JsonError response = new JsonError("Can not retrive matrix list", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
        }
    }

	@Path("{matrixId}")
	@POST
    @Produces(MediaType.APPLICATION_JSON)
	public Response getMatrix(
			@PathParam("matrixId") long matrixId,
			@QueryParam("shallow") String shallow,
			EditorConfig config) {

		logger.info("Matrix {} requested (shallow = {})", matrixId, shallow);
		boolean isShallow = (shallow != null);
        try {
        	GuiMatrixEditorStorage storage = getMatrixEditorStorage();
        	if (config != null) {
        		storage.configure(config);
        	}
        	MatrixWithHistory matrix =  storage.getMatrix(matrixId);

        	synchronized (matrix) {
    			AMLMatrix mm = matrix.getMatrix();
    			List<JsonMatrixTestCase> data = new LinkedList<>();
            	for (AMLBlock tc : mm.getBlocks()) {
            		data.add(convertTestCase(tc, null, isShallow));
            	}

            	JsonMatrix jmatrix = new JsonMatrix(
            			matrix.getId(),
            			matrix.getName(),
            			matrix.getDate(),
            			matrix.getLanguageURI(),
                		data,
                		JsonAMLUtil.getErrors(matrix));

            	return Response.ok(jmatrix).build();
			}
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            JsonError response = new JsonError("Can not retrive matrix list", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
        }
    }

    @Path("{matrixId}/save")
	@POST
    @Produces(MediaType.APPLICATION_JSON)
	public Response saveMatrix(@PathParam("matrixId") long matrixId) {
    	logger.info("Matrix {} save request", matrixId);
    	try {
    		GuiMatrixEditorStorage storage = getMatrixEditorStorage();
    		MatrixWithHistory matrix = storage.getMatrix(matrixId);
    		synchronized (matrix) {
        		storage.saveMatrix(matrix); // FIXME: use matrixId?
        		return Response.ok(JsonError.NO_ERROR).build();
			}
    	} catch (Exception e) {
            logger.error(e.getMessage(), e);
            JsonError response = new JsonError("Can not save matrix", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
    	}
    }

	@Path("{matrixId}/reset")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response resetMatrix(@PathParam("matrixId") long matrixId) {
		logger.info("Matrix {} reset request", matrixId);
		try {
			GuiMatrixEditorStorage storage = getMatrixEditorStorage();
			storage.releaseMatrix(matrixId);
			return Response.ok(JsonError.NO_ERROR).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			JsonError response = new JsonError("Can not reset matrix", e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
		}
	}

	@Path("{matrixId}/configure")
	@POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response configure(
    		@PathParam("matrixId") long matrixId,
    		EditorConfig config) {

		logger.info("POST Editor /{}/configure", matrixId, config);

		try {
			GuiMatrixEditorStorage storage = getMatrixEditorStorage();
			storage.configure(config);
			return Response.ok().build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			JsonError response = new JsonError("Can not configure MatrixEditor", e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
		}
	}

	@Path("{matrixId}/configuration")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getRunSettings() {
		logger.info("GET Editor /configuration");

		try {
			GuiMatrixEditorStorage storage = getMatrixEditorStorage();
			return Response.ok(storage.getConfig()).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			JsonError response = new JsonError("Can not configure MatrixEditor", e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
		}
	}


	// Here we use following approach[1]:
	// we create temporary resource /{matrixId}/report/{reportId}
	//
	// [1] http://evertpot.com/dropbox-post-api/ (refer to Alternative approach)
    @Path("{matrixId}/run")
	@POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response enqueue(
    		@PathParam("matrixId") long matrixId,
    		RunMatrixSettings settings) {

		logger.info("Editor /{}/run request: {}", matrixId, settings);

		try {
			GuiMatrixEditorStorage storage = getMatrixEditorStorage();
			IMatrix imatrix = storage.getMatrix(matrixId);
			String currentRange = settings.getRange();
			String selectedEncoding = settings.getEncoding();
			String selectedEnvironment = settings.getEnvironment();
			boolean continueOnFailed = settings.isContinueOnFailed();
			boolean autoStart = settings.isAutoStart();
			boolean autoRun = settings.isAutoRun();
			boolean ignoreAskForContinue = settings.isIgnoreAskForContinue();
			boolean runNetDumper = settings.isRunNetDumper();
			boolean skipOptional = settings.isSkipOptional();

			GuiMatrixEditorScriptRunListener listener = new GuiMatrixEditorScriptRunListener();

			long executionId = TestToolsAPI.getInstance().executeMatrix(
					imatrix,
					SailfishURI.unsafeParse("AML_v3"),
					currentRange,
					selectedEncoding,
					selectedEnvironment,
                    RESTUtil.getSystemUser(EDITOR_USERNAME),
					continueOnFailed,
					autoStart,
					autoRun,
					ignoreAskForContinue,
					runNetDumper,
					skipOptional,
					new ArrayList<Tag>(/* no tags */),
					new HashMap<String, String>(), Arrays.<IScriptReport>asList(listener), null);

			storage.addMessageProvider(matrixId, listener.getProvider());
			storage.putExecutionId(matrixId, executionId);

			logger.info("Editor: Test Script {} was enqueued under {}", imatrix, executionId);

			if (executionId == -1) {
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(executionId).build();
			}

			return Response.ok(executionId).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			JsonError response = new JsonError("Can not runn matrix", e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
		}
	}

    @Path("{matrixId}/run")
	@GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getExecutionResult(
    		@PathParam("matrixId") long matrixId,
    		@QueryParam("toAML") @DefaultValue("true") Boolean toAML, // FIXME: ignored at this time... remove it?
    		@QueryParam("unexpectedOnly") @DefaultValue("true") Boolean unexpectedOnly) {

		logger.info("Editor /{}/run", matrixId);

		try {
			GuiMatrixEditorStorage storage = getMatrixEditorStorage();

			long executionId = storage.getLastExecutionId(matrixId);

			if (executionId == -1) {
				// FIXME: Ok?
				return Response.ok().build();
			}

			TestScriptDescription descr = SFLocalContext.getDefault().getScriptRunner().getTestScriptDescription(executionId);

			if (descr == null) {
				return Response.status(Response.Status.NOT_FOUND).build();
			}

			List<UnexpectedMessagesContainer> container = storage.getMessageProvider(matrixId).getUnexpectedContainer();

			JsonTestScriptDescription result = new JsonTestScriptDescription(descr, toAMLTestCases(container, unexpectedOnly));
			return Response.ok(result).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			JsonError response = new JsonError("Can not get matrix execution", e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
		}
	}

	private JsonMatrix toAMLTestCases(List<UnexpectedMessagesContainer> containers, boolean unexpectedOnly) throws AMLException {
		List<JsonMatrixTestCase> testCases = new ArrayList<>();
		int setId = 0;

		for (UnexpectedMessagesContainer contaier : containers) {
			HashMap<String, String> tcValues = new HashMap<>();
			if (contaier.getTestCaseName() != null) {
				tcValues.put(Column.Reference.getName(), contaier.getTestCaseName());
			}
			tcValues.put(Column.Action.getName(), "Unexpected");

			List<JsonMatrixLine> actions = new ArrayList<>();

			Set<IMessage> ins;
			if (unexpectedOnly) {
				ins = contaier.getUnexpectedMessages();
			} else {
				ins = contaier.getAllMessages();
			}
			Set<JsonMatrixLine> outs = new LinkedHashSet<>(); // preserve order
			for (IMessage in : ins) {
				for (AMLElement action : IMessage2Action.convert(in)) {
					JsonMatrixLine line = JsonAMLUtil.convertLine(action, null);
					line.setSetId(setId);
					outs.add(line);
				}
				setId++;
			}
			actions.addAll(outs);

			testCases.add(new JsonMatrixTestCase(tcValues, actions));
		}

		return new JsonMatrix(null, null, null, SailfishURI.unsafeParse("AML_v3"), testCases, Collections.<JsonAMLError>emptyList());
	}

	@Path("{matrixId}/run/")
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteExecutionResult(@PathParam("matrixId") long matrixId) {

		logger.info("Editor DELETE /{}/run/", matrixId);

		try {
			GuiMatrixEditorStorage storage = getMatrixEditorStorage();

			IReceivedMessageProvider provider = storage.releaseMessageProvider(matrixId);

			if (provider == null) {
				return Response.status(Response.Status.NOT_FOUND).build();
			}
			return Response.ok().build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			JsonError response = new JsonError("Can not runn matrix", e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
		}
	}

	@Path("{matrixId}/undo")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response undo(@PathParam("matrixId") long matrixId) {
		logger.info("Matrix {} undo request", matrixId);
		try {
			GuiMatrixEditorStorage storage = getMatrixEditorStorage();
			MatrixWithHistory matrix = storage.getMatrix(matrixId);
			synchronized (matrix) {
				matrix.undo();
				return Response.ok(JsonError.NO_ERROR).build();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			JsonError response = new JsonError("Can not undo", e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
		}
	}

	@Path("{matrixId}/redo")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response redo(@PathParam("matrixId") long matrixId) {
		logger.info("Matrix {} redo request", matrixId);
		try {
			GuiMatrixEditorStorage storage = getMatrixEditorStorage();
			MatrixWithHistory matrix = storage.getMatrix(matrixId);
			synchronized (matrix) {
				matrix.redo();
				return Response.ok(JsonError.NO_ERROR).build();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			JsonError response = new JsonError("Can not redo", e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
		}
	}

	@Path("{matrixId}/{blcok_idx}/{action_idx}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response readAction(
    		@PathParam("matrixId") long matrixId,
			@PathParam("blcok_idx") int blockIdx,
			@PathParam("action_idx") String actionIndex) {
		logger.info("READ action {}/{}/{}", matrixId, blockIdx, actionIndex.replace("_", "/"));
		try {
			int[] actionPath = toPath(blockIdx, actionIndex);

			GuiMatrixEditorStorage storage = getMatrixEditorStorage();
			MatrixWithHistory matrix = storage.getMatrix(matrixId);
			synchronized (matrix) {
				AMLElement action = getAMLElement(matrix.getMatrix(), actionPath);
				JsonMatrixLine line = JsonAMLUtil.convertLine(action, matrix);
				return Response.ok(line).build();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			JsonError response = new JsonError("Can not read acton", e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
		}
	}

	@Path("{matrixId}/{block_idx}/{action_idx}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAction(
    		@PathParam("matrixId") long matrixId,
			@PathParam("block_idx") int blockIdx,
			@PathParam("action_idx") String actionIndex,
			JsonMatrixLine line) {
		logger.info("CREATE action {}/{}/{} requested: {}", matrixId, blockIdx, actionIndex.replace("_", "/"), line);
        try {
        	int[] actionPath = toPath(blockIdx, actionIndex);
    		int actionIdx = actionPath[actionPath.length-1];

        	GuiMatrixEditorStorage storage = getMatrixEditorStorage();
        	MatrixWithHistory matrix = storage.getMatrix(matrixId);

	        	// Copy-On-Write (copy block)
        	synchronized (matrix) {
				AMLMatrix mm = cloneToPath(matrix.getMatrix(), actionPath);
	        	AMLBlock block = getAMLBlock(mm, actionPath);
	        	block.addElement(actionIdx, line.toAMLElement(0));

	        	matrix.newSnaphot(mm);
	        	try {
		        	matrix = storage.updateMatrix(matrix);
		        	AMLElement action = getAMLElement(matrix.getMatrix(), actionPath);
					JsonMatrixLine resultLine = JsonAMLUtil.convertLine(action, matrix);
		        	return Response.ok(resultLine).build();
	        	} finally {
	        		boolean rollbacked = matrix.rollbackIfUncommited();
	        		if (rollbacked) {
	        			logger.error("Matrix editor transaction 'create action {}/{}/{}' was rollbacked", matrixId, blockIdx, actionIndex.replace("_", "/"));
	        		}
	        	}
        	}
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            JsonError response = new JsonError("Can not create action", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
        }
    }



	@Path("{matrixId}/{block_idx}/{action_idx}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateAction(
    		@PathParam("matrixId") long matrixId,
			@PathParam("block_idx") int blockIdx,
			@PathParam("action_idx") String actionIndex,
			JsonMatrixLine line) {
		logger.info("UPDATE action {}/{}/{} requested: {}", matrixId, blockIdx, actionIndex.replace("_", "/"), line);
        try {
        	int[] actionPath = toPath(blockIdx, actionIndex);
    		int actionIdx = actionPath[actionPath.length-1];

        	GuiMatrixEditorStorage storage = getMatrixEditorStorage();
			MatrixWithHistory matrix = storage.getMatrix(matrixId);

			synchronized (matrix) {
	        	// Copy-On-Write (copy block)
				AMLMatrix mm = cloneToPath(matrix.getMatrix(), actionPath);
	        	AMLBlock block = getAMLBlock(mm, actionPath);

				AMLElement oldAction = block.getElement(actionIdx);
				AMLElement action = line.toAMLElement(oldAction.getLine());

				JavaStatement oldStatement = JavaStatement.value(oldAction.getValue(Column.Action));
				JavaStatement newStatement = JavaStatement.value(action.getValue(Column.Action));

				if (JsonAMLUtil.isBlockAction(newStatement) != JsonAMLUtil.isBlockAction(oldStatement)) {
					// action -> block or block -> action
					throw new IllegalArgumentException("Use update method call to change action/block type");
				}

				block.setElement(actionIdx, action);

	        	matrix.newSnaphot(mm);
	        	try {
		        	matrix = storage.updateMatrix(matrix);
		        	action = getAMLElement(matrix.getMatrix(), actionPath);
		        	JsonMatrixLine resultLine = JsonAMLUtil.convertLine(action, matrix);
		        	return Response.ok(resultLine).build();
	        	} finally {
	        		boolean rollbacked = matrix.rollbackIfUncommited();
	        		if (rollbacked) {
	        			logger.error("Matrix editor transaction 'update action {}/{}/{}' was rollbacked", matrixId, blockIdx, actionIndex.replace("_", "/"));
	        		}
	        	}
			}
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            JsonError response = new JsonError("Can not update action", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
        }
    }

	@Path("{matrixId}/{block_idx}/{action_idx}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAction(
    		@PathParam("matrixId") long matrixId,
			@PathParam("block_idx") int blockIdx,
			@PathParam("action_idx") String actionIndex) {
		logger.info("DELETE action {}/{}/{} requested", matrixId, blockIdx, actionIndex.replace("_", "/"));
		try {
        	int[] actionPath = toPath(blockIdx, actionIndex);
    		int actionIdx = actionPath[actionPath.length-1];

			GuiMatrixEditorStorage storage = getMatrixEditorStorage();
			MatrixWithHistory matrix = storage.getMatrix(matrixId);

			synchronized (matrix) {
				// Copy-On-Write (copy block)
				AMLMatrix mm = cloneToPath(matrix.getMatrix(), actionPath);
	        	AMLBlock block = getAMLBlock(mm, actionPath);
	        	block.removeElement(actionIdx);

	        	matrix.newSnaphot(mm);
	        	try {
		         	matrix = storage.updateMatrix(matrix);
		         	JsonErrorMessage result = new JsonErrorMessage(JsonAMLUtil.getErrors(matrix));
		         	return Response.ok(result).build();
	        	} finally {
	        		boolean rollbacked = matrix.rollbackIfUncommited();
	        		if (rollbacked) {
	        			logger.error("Matrix editor transaction 'delete action {}/{}/{}' was rollbacked", matrixId, blockIdx, actionIndex.replace("_", "/"));
	        		}
	        	}
			}
         } catch (Exception e) {
             logger.error(e.getMessage(), e);
             JsonError response = new JsonError("Can not delete action", e.getMessage());
             return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
         }
    }

	@Path("{matrixId}/{block_idx}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response readBlock(
    		@PathParam("matrixId") long matrixId,
			@PathParam("block_idx") int blockIdx) {
		logger.info("READ block {}/{}", matrixId, blockIdx);
		try {
			GuiMatrixEditorStorage storage = getMatrixEditorStorage();
			MatrixWithHistory matrix = storage.getMatrix(matrixId);
			synchronized (matrix) {
				AMLBlock testCase = matrix.getMatrix().getBlock(blockIdx);
				JsonMatrixTestCase result = convertTestCase(testCase, matrix, false);
				return Response.ok(result).build();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			JsonError response = new JsonError("Can not read acton", e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
		}
	}

	@Path("{matrixId}/{block_idx}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createBlock(
    		@PathParam("matrixId") long matrixId,
			@PathParam("block_idx") int blockIdx,
			JsonMatrixTestCase testCase) {
		logger.info("CREATE block {}/{}: {}", matrixId, blockIdx, testCase);
		try {
			GuiMatrixEditorStorage storage = getMatrixEditorStorage();
			MatrixWithHistory matrix = storage.getMatrix(matrixId);

			synchronized (matrix) {
				// Copy-On-Write
				AMLMatrix mm = matrix.getMatrix().clone(false);
				int lineNumber = (blockIdx > 0) ? mm.getBlock(blockIdx-1).getLine() : blockIdx;
				mm.addBlock(blockIdx, testCase.toAMLBlock(lineNumber));

				matrix.newSnaphot(mm);
	        	try {
		        	matrix = storage.updateMatrix(matrix);
		        	AMLBlock updatedBlock = matrix.getMatrix().getBlock(blockIdx);
		        	JsonMatrixTestCase resultTestCase = convertTestCase(updatedBlock, matrix, false);
		        	return Response.ok(resultTestCase).build();
	        	} finally {
	        		boolean rollbacked = matrix.rollbackIfUncommited();
	        		if (rollbacked) {
	        			logger.error("Matrix editor transaction 'create block {}/{}' was rollbacked", matrixId, blockIdx);
	        		}
	        	}
			}
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            JsonError response = new JsonError("Can not create action", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
        }
    }

	@Path("{matrixId}/{block_idx}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateBlock(
    		@PathParam("matrixId") long matrixId,
			@PathParam("block_idx") int blockIdx,
			JsonMatrixTestCase testCase) {
		logger.info("UPDATE block {}/{}: {}", matrixId, blockIdx, testCase);
		try {
			GuiMatrixEditorStorage storage = getMatrixEditorStorage();
			MatrixWithHistory matrix = storage.getMatrix(matrixId);

			synchronized (matrix) {
				// Copy-On-Write
				AMLMatrix mm = matrix.getMatrix().clone(false);
				AMLBlock tc = mm.getBlock(blockIdx).clone(false);
				//  we don't override items here.
				fillTestCase(tc, testCase, false);
				mm.setBlock(blockIdx, tc);

				matrix.newSnaphot(mm);
	        	try {
		        	matrix = storage.updateMatrix(matrix);
		        	AMLBlock reulstTestCase = matrix.getMatrix().getBlock(blockIdx);
		        	JsonMatrixTestCase resultLine = convertTestCase(reulstTestCase, matrix, false);
		        	return Response.ok(resultLine).build();
	        	} finally {
	        		boolean rollbacked = matrix.rollbackIfUncommited();
	        		if (rollbacked) {
	        			logger.error("Matrix editor transaction 'update block {}/{}' was rollbacked", matrixId, blockIdx);
	        		}
	        	}
			}
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            JsonError response = new JsonError("Can not update action", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
        }
    }

	@Path("{matrixId}/{block_idx}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteBlock(
    		@PathParam("matrixId") long matrixId,
			@PathParam("block_idx") int blockIdx) {
		logger.info("DELETE block {}/{}: {}", matrixId, blockIdx);
		try {
			GuiMatrixEditorStorage storage = getMatrixEditorStorage();
			MatrixWithHistory matrix = storage.getMatrix(matrixId);

			synchronized (matrix) {
				// Copy-On-Write
				AMLMatrix mm = matrix.getMatrix().clone(false);
				mm.removeBlock(blockIdx);

				matrix.newSnaphot(mm);
				try {
					matrix = storage.updateMatrix(matrix);
					JsonErrorMessage result = new JsonErrorMessage(JsonAMLUtil.getErrors(matrix));
					return Response.ok(result).build();
				} finally {
					boolean rollbacked = matrix.rollbackIfUncommited();
					if (rollbacked) {
						logger.error("Matrix editor transaction 'delete block {}/{}' was rollbacked", matrixId, blockIdx);
					}
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			JsonError response = new JsonError("Can not delete action", e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
		}
	}

	@Path("{matrixId}/update")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
	public Response update(
    		@PathParam("matrixId") long matrixId,
    		JsonUpdateTransaction transaction) {

		/* replace example:
		{
		 	"actions": [
				{
					"path": [0],
					"start": 0,
					"deleteCount":1,
					"shallowReplace": false,
					"data": [
						{
		    				"values": {
		        				"#add_to_report": "y",
		        				"#execute": "n",
		        				"#timeout": "0",
		        				"#dictionary": "FIX_5_0",
		        				"#service_name": "srv1",
		        				"#reference": "ord1",
		        				"CreditProfile": "Me01",
		        				"#action": "SetRelatives"
		    				}
						}
					]
				}
			]
		}
		*/

		logger.info("Update action {}: {}", matrixId, transaction);
		try {
			GuiMatrixEditorStorage storage = getMatrixEditorStorage();
			MatrixWithHistory matrix = storage.getMatrix(matrixId);

			checkTransaction(transaction);

			synchronized (matrix) {
				// Copy-On-Write
				AMLMatrix mm = matrix.getMatrix(); // no clone(). We lone required elements in apply()

				for (JsonUpdateTransactionSpliceEntry entry: transaction.getActions()) {
					mm = apply(mm, entry.getPath(), entry.getStart(), entry.getDeleteCount(), entry.isShallowReplace(), entry.getData());
				}

				matrix.newSnaphot(mm);

				try {
					matrix = storage.updateMatrix(matrix);

					JsonErrorMessage result = new JsonErrorMessage(JsonAMLUtil.getErrors(matrix));
					return Response.ok(result).build();
				} finally {
					boolean rollbacked = matrix.rollbackIfUncommited();
					if (rollbacked) {
						logger.error("Matrix editor transaction 'update matrix {}' was rollbacked", matrixId);
					}
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			JsonError response = new JsonError("Can not execute transaction", e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
		}
	}


	@Path("{matrixId}/log")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
	public Response logClientError(
    		@PathParam("matrixId") long matrixId,
    		List<LogMessage> messages) {

		if (messages == null) {
			logger.info("Log (null) messages collection from client: matrixId={}", matrixId);
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		if (messages.isEmpty()) {
			logger.info("Log empty messages collection from client: matrixId={}", matrixId);
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		for (LogMessage logMessage: messages) {
			StringBuilder stacktrace = new StringBuilder();
			if (logMessage.getStacktrace() != null) {
				for (String line : logMessage.getStacktrace()) {
					stacktrace.append('\n').append(line);
				}
			}

			String logLevel = logMessage.getLevel();
			if (logLevel == null) {
				logLevel = "debug";
			}
			logLevel = logLevel.toLowerCase();

			switch (logLevel) {
			case "error":
				logger.error("MatrixEditor. MatrixId={}: {} {}", matrixId, logMessage.getMessage(), stacktrace);
				break;
			case "info":
				logger.info("MatrixEditor. MatrixId={}: {} {}", matrixId, logMessage.getMessage(), stacktrace);
				break;
			case "debug":
			default:
				logger.debug("MatrixEditor. MatrixId={}: {} {}", matrixId, logMessage.getMessage(), stacktrace);
				break;
			}
		}

		return Response.status(Response.Status.OK).build();
	}

	@Path("language/aml/3/columns")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getColumns() {
		ArrayList<JsonColumnEntity> result = new ArrayList<>();
		for (Column column : Column.values()) {
			if (column.getName() != null) {
				JsonColumnEntity entity = new JsonColumnEntity(column.getName(), column.getHelpString());
				result.add(entity);
			}
		}

		return Response.status(Response.Status.OK).entity(result).build();
	}

	private void checkTransaction(JsonUpdateTransaction transaction) {
		for (JsonUpdateTransactionSpliceEntry entry: transaction.getActions()) {
			if (entry.isShallowReplace()) {
				if (entry.getData().size() != 1) {
					throw new IllegalArgumentException("shallow-replace request with more than one data-item");
				}
				if (entry.getDeleteCount() != 0) {
					throw new IllegalArgumentException("shallow-replace request with deleteCount != 0");
				}
			}
		}
	}

	private AMLMatrix apply(AMLMatrix matrix, List<Integer> path, int start, int deleteCount, boolean isShallowReplace, List<JsonMatrixTestCase> data) throws AMLException {
		int[] fullPath = toPath(path, start);
		matrix = cloneToPath(matrix, fullPath);

		if (path.size() == 0) {
			// Apply to TestCase
			if (isShallowReplace) {
				JsonMatrixTestCase line = data.get(0);
				AMLBlock block = matrix.getBlock(start);
				fillTestCase(block, line, false);
			} else {
				while (deleteCount-- > 0) {
					matrix.removeBlock(start);
				}
				for (JsonMatrixTestCase line : data) {
					AMLBlock block = new AMLBlock();
					fillTestCase(block, line, true);
					matrix.addBlock(start++, block);
				}
			}
		} else {
			// Apply to block/action
			if (isShallowReplace) {
				JsonMatrixTestCase line = data.get(0);
				AMLBlock block = getAMLBlock(matrix, fullPath);

                block.setElement(start, line.toAMLElement(start));
			} else {
		    	AMLBlock block = getAMLBlock(matrix, fullPath);

	        	while (deleteCount-- > 0) {
		    		block.removeElement(start);
		    	}
		    	for (JsonMatrixTestCase line : data) {
                    block.addElement(start, line.toAMLElement(start));
		    		start++;
		    	}
			}
		}
		return matrix;
	}

	private JsonMatrixTestCase convertTestCase(AMLBlock tc, MatrixWithHistory matrix, boolean isShallow) throws AMLException {
		Map<String, String> values = new HashMap<>();
		for (Column column : tc_columns) {
			values.put(column.getName(), tc.getValue(column));
		}

		List<JsonMatrixLine> items = new LinkedList<>();
		if (!isShallow) {
			for (AMLElement action : tc.getElements()) {
				JsonMatrixLine line = JsonAMLUtil.convertLine(action, null); /* don't substitute errors to actions */
				items.add(line);
			}
		}

		if (matrix != null && matrix.getErrors() != null) {
			Collection<Alert> errors = matrix.getErrors();
			if (errors != null && !errors.isEmpty()) {
				return new JsonMatrixTestCase(values, items, JsonAMLUtil.getErrors(matrix));
			}
		}

		return new JsonMatrixTestCase(values, items);
	}

	private void fillTestCase(AMLBlock tc, JsonMatrixTestCase jsonTestCase, boolean appendActions) throws AMLException {
		for (Column column : tc_columns) {
			fillTestCaseValue(tc, jsonTestCase.getValues(), column);
		}

		if (jsonTestCase.getItems() != null && appendActions) {
			tc.removeAllElements();
	    	for (JsonMatrixLine line : jsonTestCase.getItems()) {
	    		tc.addElement(line.toAMLElement(tc.getElements().size()));
	    	}
    	}
	}

	private int[] toPath(int blockIdx, String actionIndex) {
		String[] actionIndexes = actionIndex.split("_");
		int[] actionPath = new int[actionIndexes.length + 1];

		actionPath[0] = blockIdx;

		for (int i = 0; i< actionIndexes.length; i++) {
			actionPath[i + 1] = Integer.parseInt(actionIndexes[i]);
		}

		return actionPath;
	}


	private int[] toPath(List<Integer> path, int last) {
		int[] actionPath = new int[path.size() + 1];
		for (int i = 0; i < path.size(); i++) {
			actionPath[i] = path.get(i);
		}
		actionPath[actionPath.length - 1] = last;
		return actionPath;
	}

	private AMLMatrix cloneToPath(AMLMatrix matrix, int[] actionPath) {
		AMLMatrix result = matrix.clone(false);
		if (actionPath.length == 0) {
			return result;
		}

		// copy first block
		int idx = actionPath[0];
		AMLBlock parent = result.getBlock(idx).clone(false);
		matrix.setBlock(idx, parent);

		// go deeper:
		for (int i=1; i<actionPath.length-1; i++) {
			idx = actionPath[i];
			AMLElement element = parent.getElement(idx);
			if (element instanceof AMLBlock) {
				AMLBlock block = ((AMLBlock) element).clone(false);
				parent.setElement(idx, block);
				parent = block;
			} else {
				throw new IllegalStateException("Not a AMLBlock");
			}
		}


		return result;
	}

	private void fillTestCaseValue(AMLBlock tc, Map<String, String> values, Column column) {
		String value = values.get(column.getName());
    	if (value != null) {
    		tc.setValue(column, value);
    	} else {
    		tc.removeCell(column);
    	}
	}

	private AMLBlock getAMLBlock(AMLMatrix amlMatrix, int[] actionPath) throws AMLException {
    	AMLBlockSearchVisitor visitor = new AMLBlockSearchVisitor(
    			actionPath,
    			1, // starting from second element of path
    			actionPath.length-2); // skip first-level-block and action
    	amlMatrix.getBlock(actionPath[0]).accept(visitor);
    	return visitor.getBlock();
	}

	private AMLElement getAMLElement(AMLMatrix amlMatrix, int[] actionPath) throws AMLException {
		AMLBlock block = getAMLBlock(amlMatrix, actionPath);
		return block.getElement(actionPath[actionPath.length-1]);
	}

	private static class AMLBlockSearchVisitor implements IAMLElementVisitor {

		private final int[] elementIndexes;
		private AMLBlock block = null;
		private int index = 0;
		private int length = 0;

		public AMLBlock getBlock() {
			return block;
		}

		public AMLBlockSearchVisitor(int[] elementIndexes, int start, int length) {
			this.elementIndexes = elementIndexes;
			this.index = start;
			this.length = length;
		}

		@Override
		public void visit(AMLElement element) throws AMLException {
			if (length > 0) {
				throw new AMLException("AMLElement isn't an AMLBlock: ");
			}  else if (length == 0 && block == null) {
				throw new AMLException("AMLElement cannot be choosen as AMLBlock");
			}
		}

		@Override
		public void visit(AMLBlock block) throws AMLException {
			if (length > 0) {
				AMLElement element = block.getElement(elementIndexes[index]);
				index++;
				length--;
				element.accept(this);
			} else if (length == 0) {
				this.block = block;
			}
		}
	}



}
