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

import static com.exactpro.sf.common.services.ServiceName.DEFAULT_ENVIRONMENT;
import static org.apache.commons.lang.StringUtils.stripToNull;

import java.util.Objects;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.testwebgui.api.TestToolsAPI;
import com.exactpro.sf.testwebgui.restapi.xml.EnvironmentList;
import com.exactpro.sf.testwebgui.restapi.xml.XmlResponse;

@Path("environment")
public class EnvironmentResource {
    private static final Logger logger = LoggerFactory.getLogger(EnvironmentResource.class);

    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response environmentList() {
        EnvironmentList list = new EnvironmentList();
        list.setEnvironmentList(TestToolsAPI.getInstance().getEnvNames());
        return Response.ok(list).build();
    }

    @GET // FIXME: @POST
    @Path("add")
    @Produces(MediaType.APPLICATION_XML)
    public Response addEnvironment(@QueryParam("name") String name) {
        if (name != null && !name.isEmpty()) {
            try {
                TestToolsAPI.getInstance().addEnvironment(name);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                XmlResponse response = new XmlResponse();
                response.setMessage(e.getMessage());
                response.setRootCause(e.getCause().toString());
                return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
            }
        } else {
            XmlResponse response = new XmlResponse();
            response.setMessage("Can't create environment with empty name");
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        }
        XmlResponse response = new XmlResponse();
        response.setMessage("Environment " + name + " was successfully created");
        return Response.ok(response).build();
    }

    @GET // FIXME: @DELETE
    @Path("delete")
    @Produces(MediaType.APPLICATION_XML)
    public Response removeEnvironment(@QueryParam("name") String name) {
        if (name != null && !name.isEmpty()) {
            try {
                TestToolsAPI.getInstance().removeEnvironment(name, null);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                XmlResponse response = new XmlResponse();
                response.setMessage(e.getMessage());
                response.setRootCause(e.getCause().toString());
                return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
            }
        } else {
            XmlResponse response = new XmlResponse();
            response.setMessage("Can't remove environment with empty name");
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        }
        XmlResponse response = new XmlResponse();
        response.setMessage("Environment " + name + " was successfully removed");
        return Response.ok(response).build();
    }

    @GET // FIXME: @PUT
    @Path("rename")
    @Produces(MediaType.APPLICATION_XML)
    public Response renameEnvironment(@QueryParam("oldname") String oldName, @QueryParam("newname") String newName) {
        if (oldName != null && !oldName.isEmpty()) {
            if (newName != null && !newName.isEmpty()) {
                try {
                    TestToolsAPI.getInstance().renameEnvironment(oldName, newName);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    XmlResponse response = new XmlResponse();
                    response.setMessage(e.getMessage());
                    response.setRootCause(e.getCause().toString());
                    return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
                }
            } else {
                XmlResponse response = new XmlResponse();
                response.setMessage("Can't rename environment to empty name");
                return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
            }
        } else {
            XmlResponse response = new XmlResponse();
            response.setMessage("Can't rename environment with empty name");
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        }
        XmlResponse response = new XmlResponse();
        response.setMessage("Environment " + oldName + " was successfully renamed to " + newName);
        return Response.ok(response).build();
    }

    @GET
    @Path("get_variable_set")
    @Produces(MediaType.APPLICATION_XML)
    public Response getEnvironmentVariableSet(@QueryParam("name") @DefaultValue(DEFAULT_ENVIRONMENT) String environmentName) {
        environmentName = stripToNull(environmentName);

        if(environmentName != null) {
            try {
                XmlResponse response = new XmlResponse();
                response.setMessage(TestToolsAPI.getInstance().getEnvironmentVariableSet(environmentName));
                return Response.ok(response).build();
            } catch(Exception e) {
                logger.error(e.getMessage(), e);
                XmlResponse response = new XmlResponse();
                response.setMessage(e.getMessage());
                response.setRootCause(Objects.toString(e.getCause(), null));
                return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
            }
        }

        XmlResponse response = new XmlResponse();
        response.setMessage("Environment name cannot be empty");
        return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
    }

    @GET //FIXME: use PUT when SFAPIClient will be supporting it
    @Path("set_variable_set")
    @Produces(MediaType.APPLICATION_XML)
    public Response setEnvironmentVariableSet(@QueryParam("environment") @DefaultValue(DEFAULT_ENVIRONMENT) String environmentName,
            @QueryParam("variable_set") String variableSetName) {
        environmentName = stripToNull(environmentName);
        variableSetName = stripToNull(variableSetName);

        if(environmentName != null) {
            try {
                TestToolsAPI.getInstance().setEnvironmentVariableSet(environmentName, variableSetName);
            } catch(Exception e) {
                logger.error(e.getMessage(), e);
                XmlResponse response = new XmlResponse();
                response.setMessage(e.getMessage());
                response.setRootCause(Objects.toString(e.getCause(), null));
                return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
            }
        } else {
            XmlResponse response = new XmlResponse();
            response.setMessage("Environment name cannot be empty");
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        }

        XmlResponse response = new XmlResponse();

        if(variableSetName != null) {
            response.setMessage("Successfully changed variable set for environment '" + environmentName + "' to: " + variableSetName);
        } else {
            response.setMessage("Successfully removed variable set from environment: " + environmentName);
        }

        return Response.ok(response).build();
    }
}
