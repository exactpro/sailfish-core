/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 ******************************************************************************/

package com.exactpro.sf.testwebgui.restapi.machinelearning.api;

import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.restapi.machinelearning.SessionStorage;
import com.exactpro.sf.testwebgui.restapi.machinelearning.model.ReportMLResponse;
import com.exactpro.sf.testwebgui.restapi.machinelearning.model.ReportMessageDescriptor;
import com.exactpro.sf.testwebgui.restapi.machinelearning.model.TokenWrapperResponse;
import org.apache.commons.codec.digest.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.List;

@Path("machinelearning/v2")
public class MachineLearningResourceV2 {

    @Context private HttpServletRequest httpRequest;

    @GET
    @Path("/init")
    @Produces(MediaType.APPLICATION_JSON)
    public Response initGet(@QueryParam("reportLink") @NotNull String reportLink) {

        HttpSession session = httpRequest.getSession();
        String sessionKey = DigestUtils.md5Hex(reportLink);

        SessionStorage sessionStorage;

        if (session.getAttribute(sessionKey) == null) {
            sessionStorage = new SessionStorage(reportLink, BeanUtil.getMLPersistenceManager());
            session.setAttribute(sessionKey, sessionStorage);
        } else {
            sessionStorage = (SessionStorage) session.getAttribute(sessionKey);
        }

        TokenWrapperResponse initResponse = new TokenWrapperResponse();
        initResponse.setToken(DigestUtils.md5Hex(reportLink));
        initResponse.setActive(sessionStorage.getCheckedMessages());

        return Response.ok().entity(initResponse).build();
    }

    @DELETE
    @Path("/{token}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response tokenDelete(@PathParam("token") String token, @Valid List<ReportMessageDescriptor> body) {

        HttpSession session = httpRequest.getSession();
        String sessionKey = token;

        SessionStorage sessionStorage = (SessionStorage) session.getAttribute(sessionKey);

        if (sessionStorage == null) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        body.forEach( descriptor -> sessionStorage.removeUserMark((int)descriptor.getActionId(), (int)descriptor.getMessageId()));

        return Response.ok().build();
    }

    @GET
    @Path("/{token}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response tokenGet(@QueryParam("testCaseId") @NotNull Integer testCaseId, @PathParam("token") String token) {

        HttpSession session = httpRequest.getSession();
        String sessionKey = token;

        SessionStorage sessionStorage = (SessionStorage) session.getAttribute(sessionKey);

        if (sessionStorage == null) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        ReportMLResponse response = new ReportMLResponse();
        response.setPredictions(sessionStorage.getPredictions(testCaseId));
        response.setUserMarks(sessionStorage.getCheckedMessages());
        response.setToken(token);

        return Response.ok().entity(response).build();
    }

    @PUT
    @Path("/{token}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response tokenPut(@PathParam("token") String token, @Valid List<ReportMessageDescriptor> body) {

        HttpSession session = httpRequest.getSession();
        String sessionKey = token;

        SessionStorage sessionStorage = (SessionStorage) session.getAttribute(sessionKey);

        if (sessionStorage == null) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        sessionStorage.addUserMark(body);

        return Response.ok().build();
    }

}
