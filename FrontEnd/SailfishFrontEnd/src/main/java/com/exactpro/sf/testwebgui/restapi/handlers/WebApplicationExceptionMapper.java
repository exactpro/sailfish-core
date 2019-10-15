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

package com.exactpro.sf.testwebgui.restapi.handlers;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.exactpro.sf.testwebgui.restapi.xml.XmlResponse;

/**
 * Handler for most of possible exceptions that can happened during request processing.
 * E.g. {@link javax.ws.rs.BadRequestException BadRequestException}, {@link javax.ws.rs.NotFoundException NotFoundException} and etc.
 */
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {
    @Override
    public Response toResponse(WebApplicationException exception) {
        return Response.status(exception.getResponse().getStatusInfo())
                .entity(new XmlResponse(exception.getMessage(),
                        ExceptionUtils.getStackTrace(exception.getCause() == null ? exception : exception.getCause())))
                .type(MediaType.APPLICATION_XML_TYPE)
                .build();
    }
}
