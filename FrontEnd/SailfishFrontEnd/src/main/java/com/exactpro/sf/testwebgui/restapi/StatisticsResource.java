/******************************************************************************
 * Copyright 2009-2023 Exactpro (Exactpro Systems Limited)
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

import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.exactpro.sf.embedded.statistics.entities.MatrixRun;
import com.exactpro.sf.embedded.statistics.entities.MatrixRunTag;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.embedded.statistics.DimensionMap;
import com.exactpro.sf.embedded.statistics.MatrixInfo;
import com.exactpro.sf.embedded.statistics.StatisticsService;
import com.exactpro.sf.embedded.statistics.StatisticsUtils;
import com.exactpro.sf.embedded.statistics.configuration.StatisticsServiceSettings;
import com.exactpro.sf.embedded.statistics.entities.Tag;
import com.exactpro.sf.embedded.statistics.entities.TagGroup;
import com.exactpro.sf.embedded.statistics.handlers.IStatisticsReportHandler;
import com.exactpro.sf.embedded.statistics.storage.AggregatedReportRow;
import com.exactpro.sf.embedded.statistics.storage.IStatisticsStorage;
import com.exactpro.sf.embedded.statistics.storage.reporting.AggregateReportParameters;
import com.exactpro.sf.embedded.statistics.storage.reporting.TagGroupDimension;
import com.exactpro.sf.embedded.statistics.storage.reporting.TagGroupReportParameters;
import com.exactpro.sf.embedded.statistics.storage.reporting.TagGroupReportResult;
import com.exactpro.sf.testwebgui.api.TestToolsAPI;
import com.exactpro.sf.testwebgui.restapi.xml.XmlResponse;
import com.exactpro.sf.testwebgui.restapi.xml.XmlStatisticStatusResponse;
import com.exactpro.sf.testwebgui.restapi.xml.XmlStatisticsDBSettings;
import com.exactpro.sf.util.DateTimeUtility;

@Path("statistics")
public class StatisticsResource {
	private static final Logger logger = LoggerFactory.getLogger(StatisticsResource.class);
	private static final ThreadLocal<SimpleDateFormat> parseFormat = new ThreadLocal<SimpleDateFormat>() {
	    @Override
	    protected SimpleDateFormat initialValue() {
            SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyy-HH:mm:ss");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
	        return formatter;
	    }
    };
    private static final ThreadLocal<SimpleDateFormat> xmlFormat = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy'T'HH:mm:ss");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            return formatter;
        }
    };
	
	@Context
	private ServletContext context;
	
	private Tag loadTagByName(String name) {
		
		return getStatisticsService().getStorage().getTagByName(name);
		
	}
	
	private TagGroup loadGroupByName(String name) {
		
		return getStatisticsService().getStorage().getGroupByName(name);
		
	}
	
	private boolean isConnectedToDb() {
		
		return getStatisticsService().isConnected();
		
	}
	
	private StatisticsService getStatisticsService() {
		
		return ((ISFContext)context.getAttribute("sfContext")).getStatisticsService();
		
	}
	
	@GET
    @Path("register_tag")
    @Produces(MediaType.APPLICATION_XML)
	public Response registerTag(@QueryParam("name") String tagName, 
			@QueryParam("group") String groupName) {
		
		XmlResponse xmlResponse = new XmlResponse();
		Status status = Status.OK;
		
		TagGroup loadedGroup = null;
		
		String actionsPerformed = "";
		
		try {
			
			StatisticsService service = getStatisticsService();
			
			if(StringUtils.isEmpty(tagName)) {
				
				throw new IllegalArgumentException("name parameter is mandatory");
				
			}
			
			if(isConnectedToDb()) {
				
				// verify group exists
				
				if(groupName != null) {
					
					loadedGroup = loadGroupByName(groupName);
					
					if(loadedGroup == null) {
						throw new IllegalArgumentException("Group [" + groupName + "] does not exists");
					}
					
				}
				
				Tag tag = loadTagByName(tagName);
				
				if(tag == null) { // need to add
					
					tag = new Tag();
					
					tag.setName(tagName);
					
					tag.setGroup(loadedGroup);
					
					service.getStorage().add(tag);
					
					actionsPerformed += "Added; ";
					
				} else { // Move to specified group if neccusarry
					
					if(tag.getGroup() == null || !tag.getGroup().equals(loadedGroup)) {
						
						tag.setGroup(loadedGroup);
						
						service.getStorage().update(tag);
						
						actionsPerformed += "Group set;";
						
					}
					
				}
				
				xmlResponse.setMessage(actionsPerformed + "Success;");
				
			} else {
				
				xmlResponse.setMessage("Disconnected from statistics DB");
				status = Status.BAD_REQUEST;
				
			}
		
		} catch(Exception e) {
			
			logger.error(e.getMessage(), e);
			
			xmlResponse.setMessage(e.getMessage());
			if(e.getCause() != null) {
				xmlResponse.setRootCause(e.getCause().toString());
			}
			status = Status.BAD_REQUEST;
			
		}
		
		return Response.
                status(status).
                entity(xmlResponse).
                build();
		
	}

    @GET
    @Path("script_runs_history")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response statictcsHistoryReport(@DefaultValue("junit") @QueryParam("type") String type,
                                           @QueryParam("from") String from, @QueryParam("to") String to,
                                           @DefaultValue("Sailfish") @QueryParam("title") String title,
                                           @DefaultValue("true") @QueryParam("alltags") boolean allTags,
                                           @DefaultValue("false") @QueryParam("exportwithtcsinfo") boolean exportWithTCsInfo,
                                           @DefaultValue("false") @QueryParam("exportwithactionsinfo") boolean exportWithActionsInfo,
                                           DimensionMap dimension) {
	    
	    XmlResponse xmlResponse = new XmlResponse();
	    Status status = Status.OK;

	    if (!getStatisticsService().isConnected()) {
	        xmlResponse.setMessage("Statistics service is not available.");
	        return Response.status(Status.BAD_REQUEST).entity(xmlResponse.toString()).build();
	    }
	    
	    try {
	        switch (type) {
            case "junit":
                return generateJunitReport(from, to, title);
            case "csv":
                return generateCSVReport(from, to, allTags, exportWithTCsInfo, exportWithActionsInfo, dimension);

            default:
                throw new EPSCommonException("Unknown report type");
            }
            
        } catch (Exception e) {
            logger.error(e.getMessage(), e);

            xmlResponse.setMessage(e.getMessage());
            if(e.getCause() != null) {
                xmlResponse.setRootCause(e.getCause().toString());
            }
            status = Status.BAD_REQUEST;
        }

	    return Response.
                status(status).
                entity(xmlResponse.toString()).
                build();
	}

    private Response generateCSVReport(String from, String to, boolean allTags, boolean exportWithTCsInfo,
                                       boolean exportWithActionsInfo, DimensionMap dimension) throws ParseException {

        StatisticsService service = getStatisticsService();
        AggregateReportParameters params = new AggregateReportParameters();
        params.setFrom(parseFormat.get().parse(from));
        params.setTo(parseFormat.get().parse(to));
        params.setSfInstances(service.getStorage().getAllSfInstances());
        params.setAllTags(allTags);
        params.setSortBy("MR.startTime");
        params.setSortAsc(true);

        List<Tag> allRegisteredTags = service.getStorage().getAllTags();

        List<String> columns;

        if (dimension != null) {
            List<Tag> tags = new ArrayList<>();

            for (String tagName : dimension.getTagNames()) {
                boolean registered = false;

                for (Tag tag : allRegisteredTags) {
                    if (tag.getName().equalsIgnoreCase(tagName)) {
                        tags.add(tag);
                        registered = true;
                    }
                }
                if (!registered) {
                    throw new EPSCommonException("Unregistered tag: " + tagName);
                }
            }

            params.setTags(tags);

            columns = dimension.getColumns().isEmpty() ? new ArrayList<>(
                    Arrays.asList(StatisticsUtils.AVAILABLE_SCRIPT_RUN_HISTORY_COLUMNS)) : dimension.getColumns();
        } else {
            columns = new ArrayList<>(Arrays.asList(StatisticsUtils.AVAILABLE_SCRIPT_RUN_HISTORY_COLUMNS));
        }

        List<AggregatedReportRow> reportRows = service.getReportingStorage().generateTestScriptsReport(params);

        StreamingOutput stream = out -> {
            try {
                StatisticsUtils.writeScriptRunsHistory((ISFContext)context.getAttribute("sfContext"), out, columns,
                        reportRows, exportWithTCsInfo, exportWithActionsInfo);
            } catch(Exception e) {
                logger.error(e.getMessage(), e);
                throw new WebApplicationException(e);
            }
        };

        return Response.ok(stream)
                       .header("content-disposition",
                               "attachment; filename = " + StatisticsUtils.createScriptRunsHistoryName())
                       .build();
    }
    /**
     * @param from
     * @param to
     * @param title
     * @return
     * @throws ParseException
     */
    private Response generateJunitReport(String from, String to, String title) throws ParseException {
        
        AggregateReportParameters params = new AggregateReportParameters();
        params.setFrom(parseFormat.get().parse(from));
        params.setTo(parseFormat.get().parse(to));
        params.setSfInstances(getStatisticsService().getStorage().getAllSfInstances());
        params.setSortBy("MR.startTime");
        params.setSortAsc(true);

        List<AggregatedReportRow> result = getStatisticsService().getReportingStorage().generateTestScriptsReport(params);

        MatrixInfo matrixInfo = MatrixInfo.extractMatrixInfo(result);
        long failures = matrixInfo.getAllCasesFailed();
        long test = matrixInfo.getAllCases();

        long tmp = 0;
        
        for (AggregatedReportRow row: result) {

                if (row.isMatrixRow()) {
                tmp+=row.getExecutionTime();
            }
        }

        long totalTime = tmp;
        
        StreamingOutput output = new StreamingOutput() {

            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {

                XMLStreamWriter writer = null;
                XMLOutputFactory factory = XMLOutputFactory.newInstance();
                try {
                    writer = factory.createXMLStreamWriter(output);
                    try {
                        writer.writeStartDocument();
                        writer.writeStartElement("testsuites");
                        writer.writeAttribute("name", "Regression");
                        writer.writeAttribute("failures", failures + "");
                        writer.writeAttribute("tests", test + "");
                        writer.writeAttribute("time", totalTime / 1000.0d + "");

                        boolean one = true;
                        for (AggregatedReportRow row : result) {

                            String matrixName = row.getMatrixName().replace('.', '_');

                            if (row.isMatrixRow()) {

                                if (one) {
                                    one = false;
                                } else {
                                    writer.writeEndElement();
                                }

                                writer.writeStartElement("testsuite");
                                writer.writeAttribute("package", title);
                                writer.writeAttribute("errors", "" + row.getFailedCount());
                                writer.writeAttribute("failures", "" + row.getFailedCount());
                                writer.writeAttribute("hostname", row.getHost());
                                writer.writeAttribute("name", matrixName);
                                writer.writeAttribute("skipped", "0");
                                writer.writeAttribute("tests", row.getPassedCount() + row.getConditionallyPassedCount() + row.getFailedCount() + "");
                                Long executionTime = row.getExecutionTime();
                                writer.writeAttribute("time", executionTime == null ? "-1" : String.valueOf(TimeUnit.MILLISECONDS.toSeconds(executionTime)));
                                writer.writeAttribute("timestamp", xmlFormat.get().format(row.getMatrixStartTime()));

                            } else {
                                writer.writeStartElement("testcase");
                                writer.writeAttribute("classname", String.format("%s.%s", title, matrixName));
                                writer.writeAttribute("name", StringUtils.defaultString(row.getDescription())); 
                                writer.writeAttribute("time", String.valueOf(diffDates(row.getFinishTime(), row.getStartTime())));

                                if (!StringUtils.isEmpty(row.getFailReason())) {
                                    writer.writeStartElement("failure");
                                    writer.writeAttribute("message", row.getFailReason());
                                    writer.writeAttribute("type", "testFailure");
                                    writer.writeEndElement();
                                }

                                writer.writeEndElement();
                            }
                        }

                        writer.writeEndElement();
                        writer.writeEndDocument();
                    } finally {
                        writer.close();
                    }
                } catch (XMLStreamException e) {
                    logger.error(e.getMessage(), e);
                    throw new EPSCommonException(e);
                }
            }
        };

        return Response.ok(output).build();
    }	
	    
	@GET
    @Path("register_group")
    @Produces(MediaType.APPLICATION_XML)
	public Response registerGroup(@QueryParam("name") String groupName) {
		
		XmlResponse xmlResponse = new XmlResponse();
		Status status = Status.OK;
		
		TagGroup loadedGroup = null;
		
		String actionsPerformed = "";
		
		try {
			
			if(StringUtils.isEmpty(groupName)) {
				
				throw new IllegalArgumentException("name parameter is mandatory");
				
			}
			
			StatisticsService service = getStatisticsService();
			
			if(isConnectedToDb()) {
				
				// verify group exists
					
				loadedGroup = loadGroupByName(groupName);
				
				if(loadedGroup == null) { // add
					
					loadedGroup = new TagGroup();
					
					loadedGroup.setName(groupName);
					
					service.getStorage().add(loadedGroup);
					
					actionsPerformed += "Added; ";
					
				} else {
					
					actionsPerformed += "Already exists; ";
					
				}
				
				xmlResponse.setMessage(actionsPerformed + "Success;");
				
			} else {
				
				xmlResponse.setMessage("Disconnected from statistics DB");
				status = Status.BAD_REQUEST;
				
			}
		
		} catch(Exception e) {
			
			logger.error(e.getMessage(), e);
			
			xmlResponse.setMessage(e.getMessage());
			xmlResponse.setRootCause(e.getCause().toString());
			status = Status.BAD_REQUEST;
			
		}
		
		return Response.
                status(status).
                entity(xmlResponse).
                build();
		
	}
	
	@GET
    @Path("status")
    @Produces(MediaType.APPLICATION_XML)
	public Response status() {
		
		XmlStatisticStatusResponse xmlResponse = new XmlStatisticStatusResponse();
		Status status = Status.OK;
		
		try {
			
			StatisticsService service = getStatisticsService();
			
			if(service.isConnected()) {
				
				xmlResponse.setMessage("Connected");
				
			} else {
				
				xmlResponse.setMessage("Disconnected");
				
				String message = service.getErrorMsg();
				
				if(StringUtils.isNotEmpty(message)) {
					xmlResponse.setRootCause(message);
				}
				
			}

            xmlResponse.setMigrationRequired(service.isMigrationRequired());
            xmlResponse.setSailfishUpdateRequired(service.isSfUpdateRequired());
			
		} catch (Exception e) {
			
			logger.error(e.getMessage(), e);
			status = Status.INTERNAL_SERVER_ERROR;
			xmlResponse.setMessage("Unexpected error");
			xmlResponse.setRootCause(e.getMessage());
			
		}
		
		return Response.
                status(status).
                entity(xmlResponse).
                build();
		
	}
	
	@POST
    @Path("set_db_settings")
    @Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
    public Response setDBSettings(XmlStatisticsDBSettings dbSettings) throws IOException {
        
        XmlResponse xmlResponse = new XmlResponse();
        Status status = Status.OK;
        
        StatisticsServiceSettings settings = new StatisticsServiceSettings(getStatisticsService().getSettings());
        settings.setServiceEnabled(dbSettings.getStatisticsServiceEnabled());
        settings.getStorageSettings().setDbms(dbSettings.getDbms());
        settings.getStorageSettings().setHost(dbSettings.getHost());
        settings.getStorageSettings().setPort(dbSettings.getPort());
        settings.getStorageSettings().setDbName(dbSettings.getDbName());
        settings.getStorageSettings().setConnectionOptionsQuery(dbSettings.getConnectionOptionsQuery());
        settings.getStorageSettings().setUsername(dbSettings.getUsername());
        settings.getStorageSettings().setPassword(dbSettings.getPassword());
        
        try {
            TestToolsAPI.getInstance().setStatisticsDBSettings(settings);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            status = Status.INTERNAL_SERVER_ERROR;
            xmlResponse.setMessage("Unexpected error");
            xmlResponse.setRootCause(e.getMessage());
        }
        xmlResponse.setMessage("OK");
          
        return Response.
                status(status).
                entity(xmlResponse).
                build();        
    }
	
	@GET
    @Path("migrate")
    @Produces(MediaType.APPLICATION_XML)
	public Response migrate() {
		
		XmlResponse xmlResponse = new XmlResponse();
		Status status = Status.OK;
		
		try {
			
			StatisticsService service = getStatisticsService();
			
			if(service.isMigrationRequired()) {
				
				service.migrateDB();
				
				service.init();
				
				xmlResponse.setMessage("Success");
				
			} else {
				
				xmlResponse.setMessage("Migration is not required");
				
			}
			
		} catch (Exception e) {
			
			logger.error(e.getMessage(), e);
			status = Status.OK;
			xmlResponse.setMessage("Migration failed");
			xmlResponse.setRootCause(e.getCause() != null ? e.getCause().getMessage() : "");
			
		}
		
		return Response.
                status(status).
                entity(xmlResponse).
                build();
		
	}

    @GET
    @Path("stats_per_tags")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML})
    public Response getStatsPerTagsFromJson(DimensionMap dimensionMap) {
        XmlResponse xmlResponse = new XmlResponse();
        Status status;
        try {
            List<TagGroupDimension> selectedDimensions = parseDimensions(dimensionMap);
            return generateStatsPerTags(selectedDimensions);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            status = Status.INTERNAL_SERVER_ERROR;
            xmlResponse.setMessage(e.getMessage());
            xmlResponse.setRootCause((e.getCause() != null) ? e.getCause().getMessage() : null);
        }
        return Response.
                status(status).
                entity(xmlResponse).
                build();
    }

    @GET
    @Path("aggregated_report")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML})
    public Response getAggregatedReport(AggregateReportParameters parameters) {
        XmlResponse xmlResponse = new XmlResponse();
        Status status;
        try {
            StatisticsService statisticsService = getStatisticsService();
            String reportType = parameters.getReportType();
            if (reportType == null) {
                throw new IllegalArgumentException("ReportType is not set");
            }
            IStatisticsReportHandler statisticsReportHandler =
                    statisticsService.getStatisticsReportHandler(SailfishURI.parse(reportType));
            IStatisticsStorage statisticsStorage = statisticsService.getStorage();
            StatisticsUtils.loadSfInstanceIdsFromDb(statisticsStorage, parameters);
            StatisticsUtils.loadTagIdsFromDb(statisticsStorage, parameters);
            StatisticsUtils.generateAggregatedReport(statisticsService, parameters, statisticsReportHandler);
            return generateAggregatedReport(statisticsReportHandler, parameters);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            status = Status.INTERNAL_SERVER_ERROR;
            xmlResponse.setMessage(e.getMessage());
            xmlResponse.setRootCause((e.getCause() != null) ? e.getCause().getMessage() : null);
        }
        return Response.
                status(status).
                entity(xmlResponse).
                build();
    }

    @DELETE
    @Path("delete_matrix_runs_by_tag")
    @Produces(MediaType.APPLICATION_XML)
    public Response deleteMatrixRunsByTag(@QueryParam("name") String tagName) {
        if(StringUtils.isEmpty(tagName)) {
            return Response
                    .status(Status.BAD_REQUEST)
                    .entity(new XmlResponse("'name' parameter is required"))
                    .build();
        }

        IStatisticsStorage storage = getStatisticsService().getStorage();
        Tag tag = storage.getTagByName(tagName);
        if(tag == null) {
            String errorMsg = String.format("Tag with name '%s' is not found", tagName);
            return Response
                    .status(Status.NOT_FOUND)
                    .entity(new XmlResponse(errorMsg))
                    .build();
        }

        Set<MatrixRun> uniqueMatrixRuns = storage.getAllMatrixRunTagsForTag(tag)
                .stream()
                .map(MatrixRunTag::getMatrixRun)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        uniqueMatrixRuns.forEach(storage::deleteMatrixRun);

        return Response
                .status(Status.OK)
                .entity(new XmlResponse("Number of deleted matrix runs: " + uniqueMatrixRuns.size()))
                .build();
    }

	public ServletContext getContext() {
		return context;
	}

	public void setContext(ServletContext context) {
		this.context = context;
	}

	private double diffDates(LocalDateTime one, LocalDateTime two) {
	    return Math.abs(DateTimeUtility.getMillisecond(one) - DateTimeUtility.getMillisecond(two)) / 1000.0;
	}

    private Response generateStatsPerTags(List<TagGroupDimension> selectedDimensions) {
        List<TagGroupReportResult> tagGroupReportResults = StatisticsUtils.generateTagGroupReportResults(
                getStatisticsService(), selectedDimensions, new TagGroupReportParameters());
        String reportName = StatisticsUtils.createStatsPerTagsName();
        StreamingOutput stream = out -> {
            try {
                StatisticsUtils.writeTagGroupReportToCsv(out, tagGroupReportResults);
            } catch(Exception e) {
                logger.error(e.getMessage(), e);
                throw new WebApplicationException(e);
            }
        };
        return Response
                .ok(stream)
                .header("content-disposition",
                        "attachment; filename = "
                                + reportName).build();
    }

    private Response generateAggregatedReport(IStatisticsReportHandler statisticsReportHandler,
                                              AggregateReportParameters parameters) {

        String reportName = statisticsReportHandler.getReportName(parameters);
        StreamingOutput stream = out -> {
            try {
                statisticsReportHandler.writeReport(out);
            } catch(Exception e) {
                logger.error(e.getMessage(), e);
                throw new WebApplicationException(e);
            }
        };
        return Response
                .ok(stream)
                .header("content-disposition",
                        "attachment; filename = "
                                + reportName).build();
    }

    private List<TagGroupDimension> parseDimensions(DimensionMap dimensions) {
        IStatisticsStorage storage = getStatisticsService().getStorage();
        List<TagGroupDimension> parsedDimensions = new ArrayList<>();
        for (Entry<String, List<String>> dimension : dimensions.getDimensions().entrySet()) {
            String tagOrGroupName = dimension.getKey();
            Tag tag = storage.getTagByName(tagOrGroupName);
            if (tag != null) {
                TagGroupDimension tagGroupDimension = TagGroupDimension.fromTag(tag);
                parsedDimensions.add(tagGroupDimension);
                continue;
            }
            TagGroup group = storage.getGroupByName(tagOrGroupName);
            if (group == null) {
                throw new IllegalArgumentException(String.format("key [%s] neither group nor tag", tagOrGroupName));
            }
            TagGroupDimension tagGroupDimension = TagGroupDimension.fromGroup(group);
            parsedDimensions.add(tagGroupDimension);
            List<String> selectedTags = dimension.getValue();
            if (selectedTags != null && !selectedTags.isEmpty()) {
                List<TagGroupDimension> selectedSubTags = tagGroupDimension.getSelectedSubTags();
                Set<String> selectedTagsSet = new HashSet<>(selectedTags);
                selectedSubTags = selectedSubTags
                        .stream()
                        .filter(subTag -> selectedTagsSet.contains(subTag.getName()))
                        .collect(Collectors.toList());
                tagGroupDimension.setSelectedSubTags(selectedSubTags);
            }
        }
        return parsedDimensions;
    }


}
