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

import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.testwebgui.restapi.xml.XmlResponse;

@Path("storage")
public class StorageResource {
    
    private static final Logger logger = LoggerFactory.getLogger(StorageResource.class);

    @DELETE
    @Path("clean")
    @Produces(MediaType.APPLICATION_XML)
    public Response cleanStorage() {
        try {
            SFLocalContext.getDefault().getMessageStorage().clear();
            XmlResponse xmlResponse = new XmlResponse();
            xmlResponse.setMessage("All messages and events was successfully removed from storage");
            xmlResponse.setRootCause("");

            return Response.status(Response.Status.OK).entity(xmlResponse).build();
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            XmlResponse xmlResponse = new XmlResponse();
            xmlResponse.setMessage(e.getMessage());
            xmlResponse.setRootCause((e.getCause() != null) ? e.getCause().getMessage() : null);

            return Response.status(Response.Status.BAD_REQUEST).entity(xmlResponse).build();
        }
    }
}
