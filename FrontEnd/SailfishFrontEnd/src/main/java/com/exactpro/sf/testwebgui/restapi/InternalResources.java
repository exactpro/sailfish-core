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

import java.util.Arrays;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.testwebgui.configuration.ResourceCleaner;
import com.exactpro.sf.testwebgui.restapi.xml.XmlResponse;

@Path("resources")
public class InternalResources {
    private static final Logger logger = LoggerFactory.getLogger(InternalResources.class);
    private static final String DEFAULT_TARGETS = ResourceCleaner.MESSAGES.getName() + "," + ResourceCleaner.EVENTS.getName();

    @DELETE
    @Path("clean")
    @Produces(MediaType.APPLICATION_XML)
    public Response clean(@QueryParam("olderthan") @DefaultValue("") String olderThan,
            @QueryParam("targets") @DefaultValue("") String targets) {
        try {
            ISFContext context = SFLocalContext.getDefault();
            Instant instant = StringUtils.isBlank(olderThan) ? Instant.now() : DateTimeFormatter.ISO_DATE_TIME.parse(olderThan, Instant::from);
            targets = StringUtils.defaultIfBlank(targets, DEFAULT_TARGETS);

            Arrays.stream(targets.split(","))
                    .map(String::toLowerCase)
                    .distinct()
                    .map(ResourceCleaner::value)
                    .forEach(cleaner -> cleaner.clean(instant, context));

            XmlResponse xmlResponse = new XmlResponse();
            xmlResponse.setMessage("Successfully cleaned up following targets: " + targets);
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
