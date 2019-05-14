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

import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static org.apache.commons.lang3.StringUtils.stripToNull;

import java.io.InputStream;
import java.util.Objects;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.testwebgui.api.TestToolsAPI;
import com.exactpro.sf.testwebgui.restapi.xml.XmlResponse;
import com.exactpro.sf.testwebgui.restapi.xml.XmlVariableSets;

@Path("variable_sets")
public class VariableSetsResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(VariableSetsResource.class);

    @GET
    @Produces(APPLICATION_XML)
    public Response getVariableSets() {
        XmlVariableSets variableSetList = new XmlVariableSets();
        variableSetList.setVariableSets(TestToolsAPI.getInstance().getVariableSets());
        return Response.ok(variableSetList).build();
    }

    @GET //FIXME: use DELETE when SFAPIClient will be supporting it
    @Path("delete")
    @Produces(APPLICATION_XML)
    public Response removeVariableSet(@QueryParam("name") String name) {
        name = stripToNull(name);

        if(name != null) {
            try {
                TestToolsAPI.getInstance().removeVariableSet(name);
                XmlResponse response = new XmlResponse();
                response.setMessage("Variable set has been successfully removed: " + name);
                return Response.ok(response).build();
            } catch(Exception e) {
                LOGGER.error(e.getMessage(), e);
                XmlResponse response = new XmlResponse();
                response.setMessage(e.getMessage());
                response.setRootCause(Objects.toString(e.getCause(), null));
                return Response.status(Status.BAD_REQUEST).entity(response).build();
            }
        }

        XmlResponse response = new XmlResponse();
        response.setMessage("Variable set name cannot be empty");
        return Response.status(Status.BAD_REQUEST).entity(response).build();
    }

    @POST
    @Path("import")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_XML)
    public Response importVariableSets(@FormDataParam("file") InputStream data,
            @QueryParam("replace_existing") @DefaultValue("false") boolean replaceExisting) {
        try {
            XmlVariableSets response = new XmlVariableSets();
            response.setVariableSets(TestToolsAPI.getInstance().importVariableSets(data, replaceExisting));
            return Response.ok(response).build();
        } catch(Exception e) {
            LOGGER.error(e.getMessage(), e);
            XmlResponse response = new XmlResponse();
            response.setMessage(e.getMessage());
            response.setRootCause(Objects.toString(e.getCause(), null));
            return Response.status(Status.BAD_REQUEST).entity(response).build();
        }
    }
}
