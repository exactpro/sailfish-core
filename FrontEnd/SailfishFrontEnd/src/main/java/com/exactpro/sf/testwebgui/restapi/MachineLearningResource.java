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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.embedded.machinelearning.JsonEntityParser;
import com.exactpro.sf.embedded.machinelearning.MachineLearningService;
import com.exactpro.sf.embedded.machinelearning.entities.FailedAction;
import com.exactpro.sf.embedded.machinelearning.storage.MLFileStorage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

@Path("machinelearning")
public class MachineLearningResource {

    private static final Logger logger = LoggerFactory.getLogger(MachineLearningResource.class);

    private ObjectWriter jsonWriter = new ObjectMapper().writer();

    @Context
    private ServletContext context;

    @POST
    @Path("store_training_data")
    @Produces(MediaType.APPLICATION_JSON)
    public Response storeTrainingData(@Context HttpServletRequest request, InputStream requestBody) {
        MachineLearningService machineLearningService = getMachineLearningService();
        if (machineLearningService.isConnected()) {
            try {
                MLFileStorage storage = machineLearningService.getStorage();
                IDictionaryManager dictionaryManager = getDictionaryManager();
                FailedAction failedAction = JsonEntityParser.parse(dictionaryManager, requestBody);
                storage.storeFailedAction(failedAction);
                return Response.ok().build();
            } catch (RuntimeException | IOException e) {
                logger.error(e.getMessage(), e);
                Throwable root = ExceptionUtils.getRootCause(e);
                JsonMultiError response = new JsonMultiError("Processing message failed",
                        ExceptionUtils.getThrowableList(e).stream().filter(t -> root != t)
                                .map(t -> t.getMessage() + " , Reason: " + root.getMessage())
                                .collect(Collectors.toList()));
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
            }
        }
        return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Machine Learning service is disabled").build();
    }

    @GET
    @Path("predictions_ready")
    @Produces(MediaType.TEXT_PLAIN)
    public Response ready() {
        
        if (getMachineLearningService().getMlPredictor() == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Predictor not ready").build();
        } else {
            return Response.ok().build();
        }
    }
    
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("predict_training_data")
    public Response predictTrainingData(InputStream requestBody) {

        if (getMachineLearningService().getMlPredictor() == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Predictions plugin is not installed").build();
        }

        FailedAction fa;
        try {
            fa = JsonEntityParser.parse(getDictionaryManager(), requestBody, true);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            Throwable root = ExceptionUtils.getRootCause(e);
            JsonMultiError response = new JsonMultiError("Processing message failed",
                    ExceptionUtils.getThrowableList(e).stream().filter(t -> root != t)
                            .map(t -> t.getMessage() + " , Reason: " + root.getMessage())
                            .collect(Collectors.toList()));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
        }

        Map result = getMachineLearningService().getMlPredictor().classifyFailedAction(fa);
        try {
            return Response.ok().entity(jsonWriter.writeValueAsString(result)).build();
        } catch (JsonProcessingException e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("get_dump")
    public Response getDump(@DefaultValue("4") @QueryParam("compression_lvl") int compression) {

        MachineLearningService mlService = getMachineLearningService();

        StreamingOutput streamingOutput = outputStream -> {

            try (OutputStream os = outputStream) {
                mlService.getStorage().zipDocumentsToStream(os, compression);
            }
        };

        return Response.ok(streamingOutput).header("Content-Disposition",
                String.format("attachment; filename=\"%s\"", "dump" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".zip")).build();
    }

    private MachineLearningService getMachineLearningService() {
        return ((ISFContext)context.getAttribute("sfContext")).getMachineLearningService();
    }

    private IDictionaryManager getDictionaryManager() {
        return ((ISFContext)context.getAttribute("sfContext")).getDictionaryManager();
    }

}
