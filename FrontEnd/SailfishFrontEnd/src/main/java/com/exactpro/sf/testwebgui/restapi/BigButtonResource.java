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
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.bigbutton.BigButtonSettings;
import com.exactpro.sf.bigbutton.RegressionRunner;
import com.exactpro.sf.bigbutton.execution.ProgressView;
import com.exactpro.sf.bigbutton.importing.CsvLibraryBuilder;
import com.exactpro.sf.bigbutton.importing.LibraryImportResult;
import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.configuration.workspace.ResourceCleaner;
import com.exactpro.sf.testwebgui.restapi.xml.BBNodeStatus;
import com.exactpro.sf.testwebgui.restapi.xml.BBSettings;
import com.exactpro.sf.testwebgui.restapi.xml.BbExecutionStatus;
import com.exactpro.sf.testwebgui.restapi.xml.XmlResponse;

@Path("bb")
@Singleton
public class BigButtonResource {
	
	private static final Logger logger = LoggerFactory.getLogger(BigButtonResource.class);

    private final AtomicLong importIdCounter = new AtomicLong();

    private final ConcurrentMap<Long, LibraryImportResult> importedLibraries = new ConcurrentHashMap<>();
    private static final String SETTINGS = "settings";

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

            long id = importIdCounter.incrementAndGet();
			
			importedLibrary.setId(id);
			
            if (importedLibrary.getCriticalErrorsQty() == 0) {

                importedLibraries.put(id, importedLibrary);
			
				return Response.
                        status(Status.OK).
	                entity(importedLibrary).
	                build();
				
			} else {
				
				return Response.
                        status(Status.BAD_REQUEST).
		                entity(importedLibrary).
		                build();
			}
			
		} catch (Exception e) {
			
			logger.error(e.getMessage(), e);
			
			return Response.
                    status(Status.INTERNAL_SERVER_ERROR).
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
                    status(Status.BAD_REQUEST).
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

            return Response.status(Status.OK).build();
		
		} catch(Exception e) {
			
			logger.error(e.getMessage(), e);
			
			return Response.
                    status(Status.INTERNAL_SERVER_ERROR).
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

        List<BBNodeStatus> nodeStatuses = progressView.getAllExecutors().stream()
                .map(e -> new BBNodeStatus(e.getExecutor().getName(), e.getState().name(), e.getErrorText()))
                .collect(Collectors.toList());

        result.setSlaveStatuses(nodeStatuses);
		
		return Response.
                status(Status.OK).
                entity(result).
                build();
		
	}
	
	@GET
	@Path("report")
	@Produces(MediaType.APPLICATION_XML)
	public Response downloadReport() {

        ProgressView progressView = SFLocalContext.getDefault().getRegressionRunner().getProgressView(0, 0);
		
		if(progressView.getReportFile() == null) {
			
			return Response.
                    status(Status.NOT_FOUND).
	                entity(new XmlResponse("Report file not found or not ready")).
	                build();
			
		}

        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream out) throws IOException, WebApplicationException {
                try(InputStream in = new FileInputStream(progressView.getReportFile())) {
                    int read;
                    byte[] bytes = new byte[1024];

                    while((read = in.read(bytes)) != -1) {
                        out.write(bytes, 0, read);
                    }
                } catch(Exception e) {
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

            return Response.status(Status.OK).build();
		
		} catch(Exception e) {
			
			logger.error(e.getMessage(), e);
			
			return Response.
                    status(Status.INTERNAL_SERVER_ERROR).
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

            return Response.status(Status.OK).build();

        } catch (Exception e) {

            logger.error(e.getMessage(), e);

            return Response.status(Status.INTERNAL_SERVER_ERROR)
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

            return Response.status(Status.OK).build();

        } catch (Exception e) {

            logger.error(e.getMessage(), e);

            return Response.status(Status.INTERNAL_SERVER_ERROR)
                           .entity(new XmlResponse("Internal Error", e.getMessage()))
                           .build();

        }

    }

    @POST
    @Path(SETTINGS)
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public BBSettings setSettings(BBSettings settingsBean) {
        //FIXME: should get by dependencies injection
        RegressionRunner regressionRunner = SFLocalContext.getDefault().getRegressionRunner();
        BigButtonSettings bbSettings = regressionRunner.getSettings();
        setIfNotNull(bbSettings::setCloneLoggingConfiguration, settingsBean::isCloneLoggingConfiguration);

        setIfNotNull(bbSettings::setEmailPassedRecipients, settingsBean::getEmailPassedRecipients);
        setIfNotNull(bbSettings::setEmailCondPassedRecipients, settingsBean::getEmailCondPassedRecipients);
        setIfNotNull(bbSettings::setEmailFailedRecipients, settingsBean::getEmailFailedRecipients);

        setIfNotNull(bbSettings::setEmailPrefix, settingsBean::getEmailPrefix);
        setIfNotNull(bbSettings::setEmailPostfix, settingsBean::getEmailPostfix);
        setIfNotNull(bbSettings::setEmailSubject, settingsBean::getEmailSubject);
        setIfNotNull(bbSettings::setEmailRecipients, settingsBean::getEmailRecipients);

        regressionRunner.setSettings(bbSettings);
        return createFrom(bbSettings);
    }

    @GET
    @Path(SETTINGS)
    @Produces(MediaType.APPLICATION_XML)
    public BBSettings getSettings() {
        //FIXME: should get by dependencies injection
        RegressionRunner regressionRunner = SFLocalContext.getDefault().getRegressionRunner();
        return createFrom(regressionRunner.getSettings());
    }

    private BBSettings createFrom(BigButtonSettings bbSettings) {
        BBSettings settings = new BBSettings();

        settings.setCloneLoggingConfiguration(bbSettings.isCloneLoggingConfiguration());

        settings.setEmailPassedRecipients(bbSettings.getEmailPassedRecipients());
        settings.setEmailCondPassedRecipients(bbSettings.getEmailCondPassedRecipients());
        settings.setEmailFailedRecipients(bbSettings.getEmailFailedRecipients());

        settings.setEmailPrefix(bbSettings.getEmailPrefix());
        settings.setEmailPostfix(bbSettings.getEmailPostfix());
        settings.setEmailSubject(bbSettings.getEmailSubject());
        settings.setEmailRecipients(bbSettings.getEmailRecipients());

        return settings;
    }

    private <T> void setIfNotNull(Consumer<T> setter, Supplier<T> getter) {
        T value = getter.get();
        if (value != null) {
            setter.accept(value);
        }
    }
}