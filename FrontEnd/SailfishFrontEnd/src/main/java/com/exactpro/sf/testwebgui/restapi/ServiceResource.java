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

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.dom4j.QName;
import org.dom4j.dom.DOMElement;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.scriptrunner.IConnectionManager;
import com.exactpro.sf.scriptrunner.IServiceNotifyListener;
import com.exactpro.sf.services.IService;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ServiceDescription;
import com.exactpro.sf.services.ServiceException;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.testwebgui.api.ImportServicesResult;
import com.exactpro.sf.testwebgui.api.ImportStatus;
import com.exactpro.sf.testwebgui.api.TestToolsAPI;
import com.exactpro.sf.testwebgui.restapi.xml.ServiceStatusList;
import com.exactpro.sf.testwebgui.restapi.xml.XmlImportServicesResponse;
import com.exactpro.sf.testwebgui.restapi.xml.XmlResponse;
import com.exactpro.sf.testwebgui.restapi.xml.XmlServiceImportStatus;
import com.exactpro.sf.testwebgui.restapi.xml.XmlServiceStatus;

@Path("services")
public class ServiceResource {

	private static final Logger logger = LoggerFactory.getLogger(ServiceResource.class);


	@GET
	@Produces(MediaType.APPLICATION_XML)
    public Response getServiceList(@DefaultValue(ServiceName.DEFAULT_ENVIRONMENT) @QueryParam("environment") String environment) {

	    try {
    		ISFContext context = SFLocalContext.getDefault();
    
    		ServiceName[] serviceNames = context.getConnectionManager().getServiceNames();
    
    		ServiceStatusList serviceList = new ServiceStatusList();
    
    		ArrayList<XmlServiceStatus> serviceStatusList = new ArrayList<XmlServiceStatus>();
    
    		for ( ServiceName serviceName : serviceNames ) {
                if (environment.equals(serviceName.getEnvironment())) {
                    ServiceDescription serviceDescription = context.getConnectionManager().getServiceDescription(serviceName);
    
                    XmlServiceStatus serviceStatus = new XmlServiceStatus();
                    serviceStatus.setType(serviceDescription.getType().toString());
                    serviceStatus.setServiceName(serviceDescription.getName());
                    serviceStatus.setStatus(context.getConnectionManager().getService(serviceName).getStatus().toString());
                    serviceStatus.setSettings(marshal(serviceDescription.getSettings()));
                    serviceStatusList.add(serviceStatus);
                }
            }
    
    		serviceList.setServiceStatusList(serviceStatusList);
    
    		return Response.ok(serviceList).build();
	    } catch (Exception e) {
	        logger.error(e.getMessage(), e);
	        return createResponse(e);
	    }

    }

	// FIXME: shouldn't be GET. GET method shouldn't mutate state. GET is idempotent
	@GET
	@Path("{servicename}")
	@Produces(MediaType.APPLICATION_XML)
    public Response executeAction(@PathParam("servicename") String serviceName, @QueryParam("action") String actionName,
                                  @DefaultValue(ServiceName.DEFAULT_ENVIRONMENT) @QueryParam("environment") String environment) {

		try {

            IService currentService = TestToolsAPI.getInstance().getService(environment, serviceName);

			if ( currentService != null ) {

				if ( actionName == null ) {
					ServiceStatus servStatus = currentService.getStatus();

					XmlServiceStatus xmlServiceStatus = new XmlServiceStatus();

					xmlServiceStatus.setServiceName(serviceName);
					xmlServiceStatus.setStatus(servStatus.toString());

					return Response.
			                status(Response.Status.OK).
			                entity(xmlServiceStatus).
			                build();
                } else if (actionName.equals("start")) {
                    TestToolsAPI.getInstance().startService(environment, serviceName, true, new ServiceNotifyListener());
					return Response.noContent().status(Response.Status.OK).build();
                } else if (actionName.equals("stop")) {
                    TestToolsAPI.getInstance().stopService(environment, serviceName, true, new ServiceNotifyListener());
					return Response.noContent().status(Response.Status.OK).build();
				} else {
				    return createResponse("unknown action");
				}

			} else {
			    return createResponse("unknown service");
			}
		}
		catch ( Throwable e ) {
		    logger.error(e.getMessage(), e);
		    return createResponse(e);
		}
    }

	@GET
	@Path("delete")
	@Produces(MediaType.APPLICATION_XML)
	public Response deleteService(
	        @DefaultValue(ServiceName.DEFAULT_ENVIRONMENT) @QueryParam("environment") String environment,
	        @QueryParam("service") String service
	        ) {
	    try {

            if (service != null && !service.isEmpty()) {
                TestToolsAPI.getInstance().removeService(environment, service, null);
                XmlResponse xmlResponse = new XmlResponse();
                xmlResponse.setMessage("Service  " + service + "in environment " + environment + " was successfully removed");
                xmlResponse.setRootCause("");
                return Response.status(Response.Status.OK).entity(xmlResponse).build();
            } else {
                TestToolsAPI.getInstance().removeServices(environment, null);
                XmlResponse xmlResponse = new XmlResponse();
                xmlResponse.setMessage("All services in environment " + environment + " was successfully removed");
                xmlResponse.setRootCause("");
                return Response.
                        status(Response.Status.OK).
                        entity(xmlResponse).
                        build();
            }

        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
	        return createResponse(e);
	    }
	}

	@POST
    @Path("import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_XML)
    public Response uploadFile(
    	@FormDataParam("file") InputStream uploadedInputStream,
    	@FormDataParam("file") FormDataContentDisposition fileDetail,
        @DefaultValue(ServiceName.DEFAULT_ENVIRONMENT) @QueryParam("environment") String environment,
    	@DefaultValue("false") @QueryParam("replaceexisting") boolean replaceExisting,
    	@DefaultValue("false") @QueryParam("skipexisting") boolean skipExisting) throws FileNotFoundException {
    
        boolean isZip = fileDetail.getFileName().endsWith(".zip");
        ImportServicesResult importServicesResult = TestToolsAPI.getInstance().importServices(uploadedInputStream, isZip, environment, replaceExisting, skipExisting, true, new ServiceNotifyListener());
    
    	logger.info("Upload finished");
    
        List<XmlServiceImportStatus> responseList = new ArrayList<XmlServiceImportStatus>();
        for (ImportStatus status : importServicesResult.getImportStatus()) {
            XmlServiceImportStatus xmlServiceImportStatus = new XmlServiceImportStatus();
            xmlServiceImportStatus.setName(status.getName());
            xmlServiceImportStatus.setStatus(status.getStatus());
            xmlServiceImportStatus.setProblem(status.getProblem());
            responseList.add(xmlServiceImportStatus);
        }
    
        XmlImportServicesResponse serviceListResponse = new XmlImportServicesResponse();
    	serviceListResponse.setServiceList(responseList);
    
    	return Response.ok(serviceListResponse).build();
    
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("start_env")
    public Response startEnvironmentServices(@DefaultValue(ServiceName.DEFAULT_ENVIRONMENT) @QueryParam("environment") String environment) {

        TestToolsAPI api = TestToolsAPI.getInstance();

        BiConsumer<ServiceName, IServiceNotifyListener> action = (serviceName, notifyListener) -> {
            try {
                api.startService(serviceName, true, notifyListener);
            } catch (InterruptedException | ExecutionException e) {
                ExceptionUtils.rethrow(e);
            }
        };

        return processServices(env -> env.equals(environment), action);

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("start_all")
    public Response startAllServices() {

        TestToolsAPI api = TestToolsAPI.getInstance();

        BiConsumer<ServiceName, IServiceNotifyListener> action = (serviceName, notifyListener) -> {
            try {
                api.startService(serviceName, true, notifyListener);
            } catch (InterruptedException | ExecutionException e) {
                ExceptionUtils.rethrow(e);
            }
        };

        return processServices(env -> true, action);

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("stop_env")
    public Response stopEnvironmentServices(@DefaultValue(ServiceName.DEFAULT_ENVIRONMENT) @QueryParam("environment") String environment) {

        TestToolsAPI api = TestToolsAPI.getInstance();

        BiConsumer<ServiceName, IServiceNotifyListener> action = (serviceName, listener) -> {
            try {
                api.stopService(serviceName, true, listener);
            } catch (InterruptedException | ExecutionException e) {
                ExceptionUtils.rethrow(e);
            }
        };

        return processServices(environment::equals, action);

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("stop_all")
    public Response stopEnvironmentServices() {

        TestToolsAPI api = TestToolsAPI.getInstance();

        BiConsumer<ServiceName, IServiceNotifyListener> action = (serviceName, listener) -> {
            try {
                api.stopService(serviceName, false, listener);
            } catch (InterruptedException | ExecutionException e) {
                ExceptionUtils.rethrow(e);
            }
        };

        return processServices(env -> true, action);

    }

    private Response processServices(Predicate<String> environment, BiConsumer<ServiceName, IServiceNotifyListener> action) {
        try {
            ISFContext context = SFLocalContext.getDefault();
            IConnectionManager connectionManager = context.getConnectionManager();

            List<ServiceName> targetServices = Arrays.stream(connectionManager.getServiceNames())
                    .filter(sn -> environment.test(sn.getEnvironment()))
                    .collect(Collectors.toList());

            Map<String, Map<?, ?>> response = new HashMap<>();

            targetServices.forEach(serviceName -> {
                List<String> errors = new ArrayList<>();
                List<String> infos = new ArrayList<>();
                Map<String, List<String>> wrapper = new HashMap<>();
                wrapper.put("errors", errors);
                wrapper.put("infos", infos);

                IServiceNotifyListener listener = new IServiceNotifyListener() {
                    @Override
                    public void onErrorProcessing(String message) {
                        errors.add(message);
                    }

                    @Override
                    public void onInfoProcessing(String message) {
                        infos.add(message);
                    }
                };

                try {
                    action.accept(serviceName, listener);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    if (ExceptionUtils.indexOfThrowable(e, ServiceException.class) == -1) {
                        throw e;
                    }
                } finally {
                    listener.onInfoProcessing("Service status: " + connectionManager.getService(serviceName).getStatus());
                }

                response.put(serviceName.getServiceName(), wrapper);
            });

            return Response.ok(response).build();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return createResponse(e);
        }
    }

    private Response createResponse(String errorMessage, String rootCause) {
	    XmlResponse xmlResponse = new XmlResponse();

        xmlResponse.setMessage(errorMessage);
        xmlResponse.setRootCause(rootCause);

        return Response.
                status(Response.Status.BAD_REQUEST).
                entity(xmlResponse).
                build();
	}
	
	private Response createResponse(String errorMessage) {
        return createResponse(errorMessage, null);
    }
	
    private Response createResponse(Throwable e) {
        return createResponse(e.getMessage(), (e.getCause() != null) ? e.getCause().getMessage() : null);
    }

	private Element marshal(IServiceSettings settings) throws Exception {
	    BeanMap beanMap = new BeanMap(settings);
	    
	    DOMElement rootElement = new DOMElement(new QName("settings"));
	    
	    for (Object key : beanMap.keySet()) {
             Object value = beanMap.get(key);
             if (value != null) {
                 DOMElement domElement = new DOMElement(new QName(key.toString()));
                 domElement.setText(value.toString());
                 rootElement.add(domElement);
             }
        }
	    
	    return rootElement;
    }
	
    private class ServiceNotifyListener implements IServiceNotifyListener {

        @Override
        public void onErrorProcessing(String message) {
            try {
                logger.error(message);
            } catch (Exception ignore){}
        }

        @Override
        public void onInfoProcessing(String message) {
            try {
                logger.debug(message);
            } catch (Exception ignore){}
        }
    }

}
