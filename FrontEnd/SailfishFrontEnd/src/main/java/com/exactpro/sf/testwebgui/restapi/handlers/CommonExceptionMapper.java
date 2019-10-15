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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.exactpro.sf.testwebgui.restapi.xml.XmlResponse;

/**
 * This class is using for handling any exception that happened during request/response processing and wasn't handled.
 * <br/>
 * If you want handle exception with certain type just define your own mapper with required exception type.
 * After that, you should register it in {@link com.exactpro.sf.testwebgui.restapi.SFAPIEntryPoint SFAPIEntryPoint}
 */
public class CommonExceptionMapper implements ExceptionMapper<Exception> {
    @Override
    public Response toResponse(Exception exception) {
        return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(new XmlResponse(exception.getMessage(),
                        ExceptionUtils.getStackTrace(exception)))
                .type(MediaType.APPLICATION_XML_TYPE)
                .build();
    }
}
