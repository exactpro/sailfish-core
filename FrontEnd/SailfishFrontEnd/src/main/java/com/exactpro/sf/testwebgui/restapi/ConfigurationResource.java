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

package com.exactpro.sf.testwebgui.restapi;

import static com.exactpro.sf.center.impl.PluginLoader.LOG4J_PROPERTIES_FILE_NAME;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceStructureException;
import com.exactpro.sf.testwebgui.restapi.xml.XmlResponse;
import com.exactpro.sf.testwebgui.servlets.CustomPropertyConfigurator;

@Path(ConfigurationResource.ROOT_PATH)
public class ConfigurationResource {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationResource.class);

    public static final String ROOT_PATH = "/configuration";

    private static final String LOGGING = "/logging";

    @POST
    @Path(LOGGING)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML})
    public Response setLoggingConfiguration(@FormDataParam("file") InputStream inputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail) {
        if (inputStream == null || fileDetail == null) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Invalid form data").build();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Setting logging configuration");
        }
        //FIXME: Should be get by dependencies injections
        IWorkspaceDispatcher workspaceDispatcher = SFLocalContext.getDefault().getWorkspaceDispatcher();
        File logCfg = null;
        try {
            logCfg = workspaceDispatcher.createFile(FolderType.CFG, true, LOG4J_PROPERTIES_FILE_NAME);
            try (OutputStream outputStream = new FileOutputStream(logCfg)) {
                IOUtils.copy(inputStream, outputStream);
            }
            CustomPropertyConfigurator.doOnChange(logCfg);
            return Response.ok("Settings successfully applied")
                    .build();
        } catch (WorkspaceStructureException | FileNotFoundException e) {
            logger.error("Can't create {} file", LOG4J_PROPERTIES_FILE_NAME, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(new XmlResponse("Can't create " + LOG4J_PROPERTIES_FILE_NAME + " file", e.getMessage()))
                    .build();
        }  catch (IOException e) {
            logger.error("Can't write logging configuration", e);
            if (logCfg != null) {
                if (logCfg.delete()) {
                    logger.error("Can't remove corrupted log file");
                }
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(new XmlResponse("Can't write logging configuration")).build();
        }
    }

    @GET
    @Path(LOGGING)
    @Produces({MediaType.MULTIPART_FORM_DATA, MediaType.APPLICATION_XML})
    public Response getLoggingConfiguration() {
        if (logger.isDebugEnabled()) {
            logger.debug("Logging configuration was requested");
        }
        //FIXME: Should be get by dependencies injections
        IWorkspaceDispatcher workspaceDispatcher = SFLocalContext.getDefault().getWorkspaceDispatcher();
        try {
            File logCfg = workspaceDispatcher.getFile(FolderType.CFG, LOG4J_PROPERTIES_FILE_NAME);
            return Response.ok(logCfg)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=" + LOG4J_PROPERTIES_FILE_NAME)
                    .build();
        } catch (FileNotFoundException e) {
            logger.error("Can't find file {}", LOG4J_PROPERTIES_FILE_NAME, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(new XmlResponse("Can't get file " + LOG4J_PROPERTIES_FILE_NAME))
                    .build();
        }
    }

}
