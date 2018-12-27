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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.testwebgui.restapi.xml.XmlResponse;

@Path("testlibraries")
public class TestLibraryResource {

	private static final Logger logger = LoggerFactory.getLogger(TestLibraryResource.class);

	@POST
	@Path("upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(
		@FormDataParam("file") InputStream uploadedInputStream,
		@FormDataParam("file") FormDataContentDisposition fileDetail,
		@DefaultValue("false") @QueryParam("overwrite") String overwrite) {

		String errorMessage = null;
		String rootCause = null;

		Response reqResponse = null;

		try {

			File resultFile = SFLocalContext.getDefault().getWorkspaceDispatcher().createFile(FolderType.TEST_LIBRARY,
					"true".equals(overwrite), fileDetail.getFileName());

			try (OutputStream os = new FileOutputStream(resultFile)) {

				byte[] buffer = new byte[1024];
				int bytesRead;
				while ((bytesRead = uploadedInputStream.read(buffer)) != -1) {
					os.write(buffer, 0, bytesRead);
				}
			}

			reqResponse = Response.ok().build();

		} catch (Throwable e) {
			logger.error("Could not store test library [{}]", ((fileDetail != null) ? fileDetail.getFileName() : "null") , e);
			errorMessage = e.getMessage();
			rootCause = (e.getCause() != null) ? e.getCause().getMessage() : null;
		}

		if (errorMessage != null) {

			XmlResponse xmlResponse = new XmlResponse();

			xmlResponse.setMessage(errorMessage);
			xmlResponse.setRootCause(rootCause);

			reqResponse = Response.status(Response.Status.BAD_REQUEST)
					.entity(xmlResponse).build();
		}

		return reqResponse;
 	}

}
