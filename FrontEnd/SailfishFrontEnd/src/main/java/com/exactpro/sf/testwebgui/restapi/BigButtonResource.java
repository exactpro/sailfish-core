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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;

import com.exactpro.sf.bigbutton.RegressionRunner;
import com.exactpro.sf.bigbutton.execution.ProgressView;
import com.exactpro.sf.bigbutton.importing.CsvLibraryBuilder;
import com.exactpro.sf.bigbutton.importing.LibraryImportResult;
import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.testwebgui.configuration.ResourceCleaner;
import com.exactpro.sf.testwebgui.restapi.xml.BbExecutionStatus;
import com.exactpro.sf.testwebgui.restapi.xml.XmlResponse;

@Path("bb")
@Singleton
public class BigButtonResource {
	
	private static final Logger logger = LoggerFactory.getLogger(BigButtonResource.class);
	
	private AtomicLong importIdCounter = new AtomicLong(0l);
	
	private ConcurrentMap<Long, LibraryImportResult> importedLibraries = new ConcurrentHashMap<>();
	
	@POST
	@Path("upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail) {
		
		try {

            ISFContext sfContext = SFLocalContext.getDefault();
			
            CsvLibraryBuilder builder = new CsvLibraryBuilder(uploadedInputStream,
                    sfContext.getWorkspaceDispatcher(), sfContext.getStaticServiceManager(),
                    sfContext.getDictionaryManager());
			
			LibraryImportResult importedLibrary = builder.buildFromCsv(fileDetail.getFileName());
			
			long id = this.importIdCounter.incrementAndGet();
			
			importedLibrary.setId(id);
			
            if (importedLibrary.getCriticalErrorsQty() == 0) {
				
				this.importedLibraries.put(id, importedLibrary);
			
				return Response.
	                status(Response.Status.OK).
	                entity(importedLibrary).
	                build();
				
			} else {
				
				return Response.
		                status(Response.Status.BAD_REQUEST).
		                entity(importedLibrary).
		                build();
			}
			
		} catch (Exception e) {
			
			logger.error(e.getMessage(), e);
			
			return Response.
	                status(Response.Status.INTERNAL_SERVER_ERROR)   .
	                entity(new XmlResponse("Internal Error", e.getMessage())).
	                build();
			
		}
		
	}
	
	@GET
	@Path("run/{importid}")
	@Produces(MediaType.APPLICATION_XML)
    public Response pressBb(
    		@PathParam("importid") long importId,
    		@DefaultValue("false") @QueryParam("preclean") boolean preclean) {
		
		LibraryImportResult importResult = importedLibraries.remove(importId);
		
		if(importResult == null) {
			
			return Response.
	                status(Response.Status.BAD_REQUEST).
	                entity(new XmlResponse("Library descriptor with id " + importId + " not found")).
	                build();
			
		}
		
		ISFContext context = SFLocalContext.getDefault();
        RegressionRunner runner = context.getRegressionRunner();
		
		try {
			
		    if (preclean) {
		        Instant now = Instant.now();
		        logger.info("performing pre-run cleanup older than {}", now);
                
                for (ResourceCleaner cleaner : ResourceCleaner.values()) {
                    cleaner.clean(now, context);
                }
		    }
		    
			runner.reset();
			
            runner.prepare(importResult);
			
			runner.run();
			
			return Response.status(Response.Status.OK).build();
		
		} catch(Exception e) {
			
			logger.error(e.getMessage(), e);
			
			return Response.
	                status(Response.Status.INTERNAL_SERVER_ERROR).
	                entity(new XmlResponse("Internal Error", e.getMessage())).
	                build();
			
		}
		
	}
	
	@GET
	@Path("status")
	@Produces(MediaType.APPLICATION_XML)
    public Response getStatus() {
		
		ProgressView progressView = SFLocalContext.getDefault().getRegressionRunner().getProgressView(0, 0);
		
		BbExecutionStatus result = new BbExecutionStatus();
		
        String status = progressView.getStatus().toString();
        result.setStatus(status);
        result.setErrorMessage(progressView.getErrorText());
        result.setWarnMessages(progressView.getWarns());

		result.setProgress(progressView.getCurrentTotalProgressPercent());
		
		return Response.
                status(Response.Status.OK).
                entity(result).
                build();
		
	}
	
	@GET
	@Path("report")
	@Produces(MediaType.APPLICATION_XML)
	public Response downloadReport() {
		
		final ProgressView progressView = SFLocalContext.getDefault().getRegressionRunner().getProgressView(0, 0);
		
		if(progressView.getReportFile() == null) {
			
			return Response.
	                status(Response.Status.NOT_FOUND).
	                entity(new XmlResponse("Report file not found or not ready")).
	                build();
			
		}
		
		StreamingOutput stream;

		stream = new StreamingOutput() {
			@Override
			public void write(OutputStream out) throws IOException, WebApplicationException {
				try (InputStream in = new FileInputStream(progressView.getReportFile())) {
					int read;
					byte[] bytes = new byte[1024];

					while ((read = in.read(bytes)) != -1) {
						out.write(bytes, 0, read);
					}
				} catch (Exception e) {
				    logger.error(e.getMessage(), e);
					throw new WebApplicationException(e);
				}
			}
		};

		return Response
				.ok(stream)
				.header("content-disposition",
						"attachment; filename = "
								+ "bb_report.csv").build();
		
	}
	
	@GET
	@Path("interrupt")
	@Produces(MediaType.APPLICATION_XML)
    public Response interrupt() {
		
		try {

            SFLocalContext.getDefault().getRegressionRunner().interrupt("Interrupted by response");
			
			return Response.status(Response.Status.OK).build();
		
		} catch(Exception e) {
			
			logger.error(e.getMessage(), e);
			
			return Response.
	                status(Response.Status.INTERNAL_SERVER_ERROR).
	                entity(new XmlResponse("Internal Error", e.getMessage())).
	                build();
			
		}
		
	}

    @GET
    @Path("pause")
    @Produces(MediaType.APPLICATION_XML)
    public Response pause() {

        try {

            SFLocalContext.getDefault().getRegressionRunner().pause();

            return Response.status(Response.Status.OK).build();

        } catch (Exception e) {

            logger.error(e.getMessage(), e);

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(new XmlResponse("Internal Error", e.getMessage()))
                           .build();

        }

    }

    @GET
    @Path("resume")
    @Produces(MediaType.APPLICATION_XML)
    public Response resume() {

        try {

            SFLocalContext.getDefault().getRegressionRunner().resume();

            return Response.status(Response.Status.OK).build();

        } catch (Exception e) {

            logger.error(e.getMessage(), e);

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(new XmlResponse("Internal Error", e.getMessage()))
                           .build();

        }

    }
	
}