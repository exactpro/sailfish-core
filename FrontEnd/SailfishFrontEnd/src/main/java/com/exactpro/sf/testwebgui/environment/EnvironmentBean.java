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
package com.exactpro.sf.testwebgui.environment;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.management.RuntimeErrorException;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.context.RequestContext;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.data.SortEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.IConnectionManager;
import com.exactpro.sf.scriptrunner.IServiceNotifyListener;
import com.exactpro.sf.scriptrunner.services.IStaticServiceManager;
import com.exactpro.sf.services.EnvironmentDescription;
import com.exactpro.sf.services.IService;
import com.exactpro.sf.services.ServiceDescription;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.storage.StorageException;
import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.api.ImportServicesResult;
import com.exactpro.sf.testwebgui.api.TestToolsAPI;

@ManagedBean(name="environmentBean")
@SessionScoped
@SuppressWarnings("serial")
public class EnvironmentBean implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger(EnvironmentBean.class);

    private static final Map<String, String> statusToRowStyle = new HashMap<>();

    private SailfishURI[] serviceTypes;			// All service types, for new service dialog

    static {
        statusToRowStyle.put(ServiceStatus.ERROR.toString(),  "error-row");
        statusToRowStyle.put(ServiceStatus.WARNING.toString(), "warning-row");
        statusToRowStyle.put(ServiceStatus.STARTED.toString(),  "started-row");
        statusToRowStyle.put(ServiceStatus.INITIALIZED.toString(),  "init-row");
        statusToRowStyle.put(ServiceStatus.DISPOSED.toString(),  "disposed-row");
        statusToRowStyle.put(ServiceStatus.DISABLED.toString(),  "disabled-row");
    }

	private EnvironmentNode[] selectedNodes ;	// Selected services for massive start/stop/delete
	private EnvironmentNode[] filteredNodes;

	private boolean replaceExistingServices;
	private boolean skipExistingServices;
    private boolean replaceExistingVariableSets;

	private SailfishURI selectedType;    		// For new
	private String newServName = "";			// service
	private String oldServName = "";			// service

	private List<IService> services;
    private List<String> variables = emptyList();

    private ServiceNodeLazyModel<EnvironmentNode> lazyModel;

	private String nameFilter;
	private String typeFilter;

	private String sortField;
	private String sortOrder;

	// To remove when environments implemented
	private final boolean ENVIRONMENTS_ENABLED = true;
	private String currentEnvironment = ServiceName.DEFAULT_ENVIRONMENT;
    private String currentVariableSet;
	private String newEnvName = "";
	private String selectedEnvironment;
    private String selectedVariableSet;
	private IServiceNotifyListener notifyListener;

	private String currentEnvironmentForCopy = ServiceName.DEFAULT_ENVIRONMENT;
    private boolean respectFileName;

	 //refresh state after deserialization
    private Object readResolve()  {
        init();
        return this;
    }

    @PostConstruct
    public void init() {

        logger.debug("EnvironmentBean creation started.");

        try {
            serviceTypes = BeanUtil.getSfContext().getStaticServiceManager().getServiceURIs();

            Arrays.sort(serviceTypes);

            this.services = new ArrayList<>();

            lazyModel = new ServiceNodeLazyModel<>(currentEnvironment, notifyListener);
    		notifyListener = new ServiceNotifyListener();

        } catch(RuntimeErrorException ex) {
            logger.error("Unable to create EnvironmentBean. Unexpected error: ", ex);
        }

		logger.debug("EnvironmentBean created.");
	}

	public void selectServiceToEdit() {
		Map<String, String> map = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String name = map.get("name");
		logger.info("selectServiceToEdit invoked {} name[{}]", getUser(), name);
        lazyModel.selectServiceToEdit(name);
	}

	public void selectedServicesToEdit() {

        List<EnvironmentNode> selectedWithoutDisabled = new ArrayList<>();

		StringBuilder builder = new StringBuilder();

		int i = 0;
        for(EnvironmentNode node : selectedNodes) {
            if(node.getServiceStatus() != ServiceStatus.DISABLED) {
                selectedWithoutDisabled.add(node);

                builder.append(node.getName());

                if(++i < selectedNodes.length) {
                    builder.append(", ");
                }
            }

        }

		logger.info("selectedServicesToEdit invoked {} names[{}]", getUser(), builder);
		lazyModel.selectedServicesToEdit(selectedWithoutDisabled.toArray(new EnvironmentNode[0]));

		RequestContext.getCurrentInstance().execute("updateParamsFormAndShow()");
	}

	public void startServiceByAction() {
		Map<String, String> map = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String serviceName = map.get("name");
        logger.info("startServiceByAction invoked {} serviceName[{}]", getUser(), serviceName);
		try {
            TestToolsAPI.getInstance().startService(currentEnvironment, serviceName, false, notifyListener);
		} catch (Exception e) {
			logger.error("Failed to start service {}", serviceName, e);
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Failed to start service " + serviceName, e.getMessage() );
		}
    }

	public void stopServiceByAction() {
		Map<String, String> map = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String serviceName = map.get("name");
		logger.info("stopServiceByAction invoked {} serviceName[{}]", getUser(), serviceName);
		try {
            TestToolsAPI.getInstance().stopService(currentEnvironment, serviceName, false, notifyListener);
		} catch (Exception e) {
			logger.error("Failed to start service {}", serviceName, e);
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Failed to stop service " + serviceName, e.getMessage() );
		}
	}

	public void selectServiceForEvents() {
        Map<String, String> map = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String serviceName = map.get("name");
		logger.info("selectServiceForEvents invoked {} name[{}]", getUser(), serviceName);

        lazyModel.selectServiceForEvents(serviceName);
	}

    public String selectRowStyleClass(Object rowObject) {

        String stat = ((String)rowObject).trim();

        String rowStyle = statusToRowStyle.get(stat);
        return rowStyle == null ? "" : rowStyle;
    }

	public void addService()
	{
		logger.debug("addService() executed. Selected type: [{}]; Name: [{}].", selectedType, newServName);
        RequestContext context = RequestContext.getCurrentInstance();

		if (selectedType == null) {
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Failed to add new service", "Service type was not selected");
			context.addCallbackParam("validationFailed", true);
			return;
		}

        if("".equals(newServName.trim())) {
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Failed to add new service", "Service name was not set");
			context.addCallbackParam("validationFailed", true);
			return;
		}

		if (!newServName.trim().matches("[\\w]*")) {
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Failed to add new service", "Service name must contain only roman letters, numbers and underscores");
			context.addCallbackParam("validationFailed", true);
			return;
		}

		if(!newServName.trim().matches("[a-zA-Z](.+)?")) {
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Failed to add new service", "Service name must start with a roman letter");
			context.addCallbackParam("validationFailed", true);
			return;
		}

		try {

            boolean sameName = checkServiceDuplicates(currentEnvironment, newServName.trim());

			if (sameName) {
                throw new StorageException("Environment '" + currentEnvironment + "' has service with name '" + newServName.trim() + "' already");
			}
            ServiceDescription serviceDescription = new ServiceDescription(selectedType);
            serviceDescription.setEnvironment(currentEnvironment);
            serviceDescription.setName(newServName.trim());
            TestToolsAPI.getInstance().addService(serviceDescription, notifyListener);
			BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Success", "Service '" + newServName.trim() + "' with type '" + selectedType + "' has been created");
		} catch(Exception ex) {
			logger.error("Failed to add new service.", ex);
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Failed to add new service", ex.getMessage());
			context.addCallbackParam("validationFailed", true);
			return;
		}

		lazyModel.waitServiceToEdit(newServName.trim());
		newServName = "";
	}

	private boolean checkServiceDuplicates(String environment, String serviceName){
        IService service = TestToolsAPI.getInstance().getService(environment, serviceName);
        return service != null;
	}

	public void retriveLastName() {
        Map<String, String> map = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String name = map.get("name");
		logger.info("retriveLastName invoked {} name[{}]", getUser(), name);

        if(name == null || "".equals(name)) {
            logger.error("Name is not defined");
            throw new NullPointerException("Name is not defined");
        }

        lazyModel.selectServiceToEdit(name);
        newServName = name;
        oldServName = name;
	}

	public void renameService() {
		logger.debug("renameServices() executed.");
        RequestContext context = RequestContext.getCurrentInstance();

        if("".equals(newServName.trim())) {
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Failed to add new service", "Service name was not set");
			context.addCallbackParam("validationFailed", true);
			return;
		}

        if(!newServName.trim().matches("[a-zA-Z][\\w]*")) {
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Failed to add new service", "Service name is incorrect");
			context.addCallbackParam("validationFailed", true);
			return;
		}

        if(newServName.trim().equals(oldServName)) {
            return;
        }

        try {
            boolean sameName = checkServiceDuplicates(currentEnvironment, newServName.trim());

            if (sameName) {
                throw new StorageException("Environment '" + currentEnvironment + "' have service with name '" + newServName.trim() + "' already");
            }
            TestToolsAPI.getInstance().copyService(oldServName, currentEnvironment, newServName.trim(), currentEnvironment, notifyListener);
            TestToolsAPI.getInstance().removeService(currentEnvironment, oldServName, notifyListener);
        } catch (Exception ex) {
            logger.error("Failed to update service.", ex);
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Failed to update service.", ex.getMessage());
            context.addCallbackParam("validationFailed", true);
            return;
        }

		this.newServName = "";
		BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Info", "Service renamed.");
	}

	public void deleteServices() {

		logger.info("deleteServices invoked {} selectedNodes[{}]", getUser(), selectedNodes);

		if (selectedNodes == null) {
			return;
		}

        for(EnvironmentNode node : selectedNodes) {
            deleteService(currentEnvironment, node.getName());
		}

		this.selectedNodes = null;
	}

    protected void deleteService(String envName, String serviceName){
        try{
            TestToolsAPI.getInstance().removeService(envName, serviceName, notifyListener);
        } catch(Exception ex) {
            logger.error("Failed to delete service.", ex);
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Failed to delete service.", ex.getMessage());
        }
    }

	public void copyServices()
	{
		logger.info("copyServices invoked {} selectedNodes[{}]", getUser(), selectedNodes);

        if(selectedNodes == null) {
			return;
		}

        RequestContext context = RequestContext.getCurrentInstance();

        if("".equals(newServName.trim())) {
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Failed to copy service", "Service name was not set");
			context.addCallbackParam("validationFailed", true);
			return;
		}

        if(!newServName.matches("[a-zA-Z][\\w]*")) {
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Failed to add new service", "Service name is incorrect");
			context.addCallbackParam("validationFailed", true);
			return;
		}

		boolean sameName = false;

		for(EnvironmentNode node : selectedNodes)
		{
			try {
                sameName = checkServiceDuplicates(currentEnvironmentForCopy, newServName.trim());

				if (sameName) {
					context.addCallbackParam("validationFailed", true);
                    throw new StorageException("Environment '" + currentEnvironmentForCopy + "' have service with name '" + newServName.trim() + "' already");
				}
                TestToolsAPI.getInstance().copyService(node.getName(), currentEnvironment, newServName.trim(), currentEnvironmentForCopy, notifyListener);
                this.currentEnvironment = currentEnvironmentForCopy;
			} catch(Exception ex) {
				logger.error("Failed to copy service.", ex);
				BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Failed to copy service", ex.getMessage());
                return;
			}
		}

        changeEnvironment();
        lazyModel.waitServiceToEdit(newServName.trim());
        newServName = "";
	}

	public void addEnvironment()
	{
		logger.info("addEnvironment invoked {} Name[{}]", getUser(), newEnvName);
		RequestContext context = RequestContext.getCurrentInstance();

        if(newEnvName == null || "".equals(newEnvName.trim())) {
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Failed to add new environment", "Environment name was not set");
			context.addCallbackParam("validationFailed", true);
			return;
		}

        String newEnvNameTrimmed = newEnvName.trim();

		if (checkEnvironmentExisting(newEnvNameTrimmed)) {
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Failed to add new environment", "Environment with name '" + newEnvNameTrimmed + "' already exists");
			context.addCallbackParam("validationFailed", true);
			return;
		}

		try {
            TestToolsAPI.getInstance().addEnvironment(newEnvNameTrimmed);
			setSelectedEnvironment(newEnvNameTrimmed);
            setCurrentEnvironment(newEnvNameTrimmed);
            changeEnvironment();
		} catch (Exception ex){
			logger.error("Failed to add new environment.", ex);
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Failed to add new environment", ex.getMessage());
			context.addCallbackParam("validationFailed", true);
			return;
		}

		this.newEnvName = "";
	}

	private boolean checkEnvironmentExisting(String envName) {
		for (String key : getEnvironmentList()) {
            if(key.equalsIgnoreCase(newEnvName)) {
				return true;
			}
		}

		return false;
	}

	public void preEnvRename() {

        if(selectedEnvironment == null) {
			BeanUtil.addWarningMessage("Warning", "Select the environment");
			return;
		}

        if(selectedEnvironment.equals(ServiceName.DEFAULT_ENVIRONMENT)) {
			BeanUtil.addWarningMessage("Warning", "You can't rename the default environment");
			return;
		}

		logger.info("retriveLastEnvName invoked {}", getUser());
		newEnvName = (selectedEnvironment == null) ? "" : selectedEnvironment;

		RequestContext.getCurrentInstance().update("renameEnvForm");

		RequestContext.getCurrentInstance().execute("PF('renameEnvDialog').show()");
	}

	public void renameEnvironment() {

		logger.info("retriveLastEnvName invoked {}", getUser());
		RequestContext context = RequestContext.getCurrentInstance();

        if(selectedEnvironment == null) {
			return;
		}

        this.newEnvName = newEnvName.trim();

        if(StringUtils.isEmpty(newEnvName)) {
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Failed to rename environment", "Environment name was not set");
            context.addCallbackParam("validationFailed", true);
            return;
        }

        try {
            TestToolsAPI.getInstance().renameEnvironment(selectedEnvironment, newEnvName);
        } catch (Exception e){
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage());
            context.addCallbackParam("validationFailed", true);
            return;
		}

        if(currentEnvironment.equals(selectedEnvironment)) {
            this.currentEnvironment = newEnvName;
            changeEnvironment();
        }

        this.selectedEnvironment = newEnvName;
		this.newEnvName = "";

		BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Info", "Environment has been renamed");
	}

	public void preEnvDelete() {

        if(selectedEnvironment == null) {
			BeanUtil.addWarningMessage("Warning", "Select the environment");
			return;
		}

        if(selectedEnvironment.equals(ServiceName.DEFAULT_ENVIRONMENT)) {
			BeanUtil.addWarningMessage("Warning", "You can't remove the default environment");
			return;
		}

		RequestContext.getCurrentInstance().execute("PF('envConfirmation').show()");
	}

	public void deleteEnvironment() {
		logger.info("deleteEnvironment invoked {} selectedEnvironment[{}]", getUser(), selectedEnvironment);
		if (selectedEnvironment == null) {
			return;
		}
        try{
            TestToolsAPI.getInstance().removeEnvironment(selectedEnvironment, notifyListener);
        } catch (Exception e){
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage());
        }

        if(selectedEnvironment.equals(currentEnvironment)){
            setCurrentEnvironment(ServiceName.DEFAULT_ENVIRONMENT);
            changeEnvironment();
        }
        selectedEnvironment = null;
	}

	public void changeEnvironment() {
        logger.info("Environment changed to '{}'", currentEnvironment);
		lazyModel.setCurrentEnvironment(currentEnvironment);
	}

	public void startService() {

		if (selectedNodes == null || selectedNodes.length == 0) {
			BeanUtil.showMessage(FacesMessage.SEVERITY_WARN, "No services chosen.", "");
			return;
		}

		logger.info("startService() executed Selected {} services.", selectedNodes.length);

		for (EnvironmentNode node : selectedNodes) {
            try {
			    TestToolsAPI.getInstance().startService(currentEnvironment, node.getName(), false, notifyListener);
			} catch (Exception e) {
				logger.error("Failed to start service {}", node.getName(), e);
				BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Failed to start service "+node.getName(), e.getMessage() );
			}
		}
	}

	public void startAllServices() {
		logger.info("startAllServices invoked {}", getUser());
        try {
            TestToolsAPI.getInstance().startAllService(false, notifyListener);
        } catch (Exception ex) {
            logger.error("Failed to start service {}", ex.getMessage(), ex);
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "", ex.getMessage());
        }
	}

	public String getEachServiceCount() {
		logger.info("getEachServiceCount invoked {}", getUser());
		if(lazyModel == null) {
			logger.error("Lazy model is not initialized correctly");
			return "";
		}
		int total = lazyModel.getServicesCount(null);

		StringBuilder sb = new StringBuilder().append("TOTAL: ").append(total).append("; ");

		for(ServiceStatus status : ServiceStatus.values()) {
			int count = lazyModel.getServicesCount(status);
			if (count == 0) {
				continue;
			}

			sb.append(status).append(":").append(count).append("; ");
		}

		sb.setLength(sb.length() - 2);

		return sb.toString();
	}

	public void stopService()
	{
		if(selectedNodes == null) {
			return;
		}
		logger.info("stopService invoked {} selectedNodes[{}]", getUser(), selectedNodes);
		for (EnvironmentNode node : selectedNodes) {
			try {
				TestToolsAPI.getInstance().stopService(currentEnvironment, node.getName(), false, notifyListener);
			} catch ( Exception ex ) {
				logger.error(ex.getMessage(), ex);
				BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "", ex.getMessage());
			}
		}
	}

	public void stopAllServices() {
		logger.info("stopAllServices invoked {}", getUser());
        try {
            TestToolsAPI.getInstance().stopAllService(false, notifyListener);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "", ex.getMessage());
        }
	}

	public void loadNewName() {
		logger.info("loadNewName invoked {} currentEnvironment[{}]", getUser(), currentEnvironment);
        this.currentEnvironmentForCopy = currentEnvironment;

        if(selectedNodes.length != 1)
        {
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Failed to copy service", "You can copy only 1 service");
            return;
        }

        this.newServName = selectedNodes[0].getName();
	}

	public boolean isNotOneServiceSelected() {
        return selectedNodes == null || selectedNodes.length != 1;
    }

    public boolean isNotOneServiceSelectedAndNotDisabled() {
        if(selectedNodes != null &&  selectedNodes.length == 1) {
            EnvironmentNode node = selectedNodes[0];
            if(node != null ) {
                return node.getServiceStatus() == ServiceStatus.DISABLED;
            }
        }
        return true;
    }

	public boolean isNotSelected() {
		if(selectedNodes == null ) {
			return true;
		}
		if(selectedNodes.length == 0 ) {
			return true;
		}

		return false;
	}

    public boolean isNotSelectedDisabledOnly() {
        if(selectedNodes != null && selectedNodes.length != 0) {
            for (EnvironmentNode envNode : selectedNodes) {
                if(envNode.getServiceStatus() != ServiceStatus.DISABLED) {
                    return false;
                }
            }
        }

        return true;
    }

	public Map<String, File> exportServices() {
		logger.info("exportServices invoked {}", getUser());
		Map<String, File> services = Collections.emptyMap();

		IConnectionManager connManager = BeanUtil.getSfContext().getConnectionManager();

        if(selectedNodes.length > 0) {
			List<ServiceDescription> descriptions = new ArrayList<>();

            for(EnvironmentNode node : selectedNodes) {
                descriptions.add(connManager.getServiceDescription(new ServiceName(node.getEnvironment(), node.getName())));
			}

			for (ServiceDescription sd : descriptions) {
				sd.setEnvironment(null);
			}

			try {
				services = BeanUtil.getSfContext().getServiceMarshalManager().exportServices(descriptions);
			} catch(Exception e) {
				logger.error(e.getMessage(), e);
			}
		}

		//fillNodes();
		BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Export finished", "");
		logger.info("exportServices: end");

		return services;
	}

	public StreamedContent getServicesInZip() {
		logger.info("getServicesInZip invoked {}", getUser());

		try {
			InputStream stream = new FileInputStream(BeanUtil.getSfContext().getServiceMarshalManager().packInZip(exportServices()));
			return new DefaultStreamedContent(stream, "application/zip", "Services.zip");
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public StreamedContent getEnvironmentInZip() {
		logger.info("getEnvironmentsInZip invoked {}", getUser());
		Map<String, File> services = Collections.emptyMap();
		try {
			services = exportEnvironment();
		} catch (NullPointerException e) {
			return null;
		}

		try {
			InputStream stream = new FileInputStream(BeanUtil.getSfContext().getServiceMarshalManager().packInZip(services));
			return new DefaultStreamedContent(stream, "application/zip", selectedEnvironment + "_environment.zip");
		} catch (FileNotFoundException e) {
			logger.error("File not found exception. {}", e.getMessage());
		}

		return null;
	}

	private Map<String, File> exportEnvironment() throws NullPointerException{

        if(selectedEnvironment == null || selectedEnvironment.isEmpty()) {
			String errorMessage = "No selected environment to export";
			logger.error(errorMessage);
			throw new NullPointerException(errorMessage);
		}

		IConnectionManager manager = BeanUtil.getSfContext().getConnectionManager();
		List<ServiceDescription> servicesInEnvironment = new ArrayList<>();
		for(ServiceName serviceName : manager.getServiceNames()) {
			if(serviceName.getEnvironment().equals(selectedEnvironment)) {
				servicesInEnvironment.add(manager.getServiceDescription(serviceName));
			}
		}

		IStaticServiceManager staticServiceManager = BeanUtil.getSfContext().getStaticServiceManager();
        IDictionaryManager dictionaryManager = BeanUtil.getSfContext().getDictionaryManager();

		Map<String, File> fileMap = BeanUtil.getSfContext().getServiceMarshalManager().exportServices(servicesInEnvironment);

		String envDescFilename = "environment_description.xml";

        File envDescFile = BeanUtil.getSfContext().getServiceMarshalManager().exportEnvironmentDescription(envDescFilename, new EnvironmentDescription(selectedEnvironment, manager.getEnvironmentVariableSet(selectedEnvironment)));
		fileMap.put(envDescFilename, envDescFile);

		return fileMap;
	}

	public synchronized void handleServicesUpload(FileUploadEvent event) throws FileNotFoundException {

		logger.info("handleServicesUpload invoked {}", getUser());

		UploadedFile uploadedFile = event.getFile() ;

        InputStream stream;
        try {
            stream = uploadedFile.getInputstream();
        } catch (IOException e) {
            logger.error("Could not get stream from uploaded file", e);
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "", e.getMessage() );
            return;
        }

        if (respectFileName) {
        	int index = uploadedFile.getFileName().indexOf("_environment.zip");
        	if (index != -1) {
        		currentEnvironment = uploadedFile.getFileName().substring(0, index);
        	}
        }

        // For getting an environment name from environment_description.xml file
        ImportServicesResult importServicesResult = TestToolsAPI.getInstance().importServices(stream, uploadedFile.getFileName().endsWith(".zip"), currentEnvironment, replaceExistingServices, skipExistingServices, !respectFileName, notifyListener);

        setCurrentEnvironment(importServicesResult.getEnvironment());
        changeEnvironment();

        RequestContext.getCurrentInstance().update("form");

        logger.info("Upload finished");
    }

    public void clearEvents() {
        try{

        	ServiceDescription description = BeanUtil.getSfContext().getConnectionManager().getServiceDescription(
        			lazyModel.getLazyEventsModel().getServiceName());

            BeanUtil.getSfContext().getServiceStorage().removeServiceEvents(description);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Failed to clear service events", e.getMessage());
        }
    }

	public void saveFilter() {
		FacesContext context = FacesContext.getCurrentInstance();
		Map<String, String> map = context.getExternalContext().getRequestParameterMap();
		String column = map.get("filterColumn");
		String value = map.get("filterValue");

        if("name".equals(column)) {
			nameFilter = value;
        } else if("type".equals(column)) {
			typeFilter = value;
		}
	}

	public void onSort(SortEvent event) {
		String column = event.getSortColumn().getClientId();
		this.sortField = column;
		this.sortOrder = event.isAscending() ? "ascending" : "descending";
	}

	public void loadSort() {

		DataTable dataTable = (DataTable) FacesContext.getCurrentInstance().getViewRoot().findComponent("form:table");

        if(sortField == null || "".equals(sortField)) {
			dataTable.setValueExpression("sortBy", null);
			return;
		}

		String elRaw = null;
        if("table:nameColumn".equals(sortField)) {
			elRaw = "#{service.name}";
        } else if("table:typeColumn".equals(sortField)) {
			elRaw = "#{service.type}";
        } else if("table:statusColumn".equals(sortField)) {
			elRaw = "#{service.status}";
		}

		FacesContext facesContext = FacesContext.getCurrentInstance();
		ELContext elContext = facesContext.getELContext();
		ExpressionFactory elFactory = facesContext.getApplication().getExpressionFactory();
		ValueExpression valueExpresion = elFactory.createValueExpression(elContext, elRaw, Date.class);

        dataTable.setSortOrder(sortOrder);
		dataTable.setValueExpression("sortBy", valueExpresion);
	}

	public void removeFilters() {
		this.typeFilter = "";
		this.nameFilter = "";
		this.sortField = "";
	}

	public EnvironmentNode[] getSelectedNodes() {
		return selectedNodes;
	}

	public void setSelectedNodes(EnvironmentNode[] evNode)
	{
		logger.info("setSelectedNodes invoked {} evNode[{}]", getUser(), evNode);
		this.selectedNodes = evNode;
	}

	public final SailfishURI[] getServiceTypes() {
		return serviceTypes;
	}

	public SailfishURI getSelectedType() {
		return selectedType;
	}

	public void setSelectedType(SailfishURI selectedType) {
		logger.info("setSelectedType invoked {} selectedType[{}]", getUser(), selectedType);
		this.selectedType = selectedType;
	}

	public String getNewServName() {
		return newServName;
	}

	public void setNewServName(String newServName) {
		logger.info("setNewServName invoked {} newServName[{}]", getUser(), newServName);
		this.newServName = newServName;
	}

	public List<IService> getServices() {
		return services;
	}

	public void setServices(List<IService> services) {
		logger.info("setServices invoked {} services[{}]", getUser(), services);
		this.services = services;
	}

	public boolean isReplaceExistingServices() {
		return replaceExistingServices;
	}

	public void setReplaceExistingServices(boolean replaceExistingServices) {
		logger.info("setReplaceExistingServices invoked {} replaceExistingServices[{}]", getUser(), replaceExistingServices);
		this.replaceExistingServices = replaceExistingServices;
        if(replaceExistingServices) {
            skipExistingServices = false;
        }
	}

    public boolean isReplaceExistingVariableSets() {
        return replaceExistingVariableSets;
    }

    public void setReplaceExistingVariableSets(boolean replaceExistingVariableSets) {
        logger.info("setReplaceExistingVariableSets invoked {} replaceExistingVariableSets[{}]", getUser(), replaceExistingVariableSets);
        this.replaceExistingVariableSets = replaceExistingVariableSets;
    }

    public boolean isEnvironmentsEnabled() {
		return ENVIRONMENTS_ENABLED;
	}

    public String getNewEnvName() {
		return newEnvName;
	}

	public void setNewEnvName(String newEnvName) {
		logger.info("setNewEnvName invoked {} newEnvName[{}]", getUser(), newEnvName);
		this.newEnvName = newEnvName;
	}

	public List<String> getEnvironmentList() {
		return BeanUtil.findBean(BeanUtil.ENVIRONMENT_TRACKING_BEAN, EnvironmentTrackingBean.class)
                .getEnvironmentList();
	}

    public Set<String> getVariableSetList() {
        return BeanUtil.getSfContext().getConnectionManager().getVariableSets();
    }

	public String getSelectedEnvironment() {
		return selectedEnvironment;
	}

	public void setSelectedEnvironment(String selectedEnvironment) {
		logger.info("setSelectedEnvironment invoked {} selectedEnvironment[{}]", getUser(), selectedEnvironment);
		this.selectedEnvironment = selectedEnvironment;
	}

    public String getSelectedVariableSet() {
        return selectedVariableSet;
    }

    public void setSelectedVariableSet(String selectedVariableSet) {
        logger.info("setSelectedVariableSet invoked {} selectedVariableSet[{}]", getUser(), selectedVariableSet);
        this.selectedVariableSet = selectedVariableSet;
    }

    public String getCurrentEnvironment() {
        // hotfix for a case when environment was deleted not from GUI (e.g. by REST API)
        if(!BeanUtil.getSfContext().getConnectionManager().getEnvironmentList().contains(currentEnvironment)) {
            setCurrentEnvironment(ServiceName.DEFAULT_ENVIRONMENT);
        }

		return currentEnvironment;
	}

	public void setCurrentEnvironment(String currentEnvironment) {
		logger.info("setCurrentEnvironment invoked {} currentEnvironment[{}]", getUser(), currentEnvironment);
		this.currentEnvironment = currentEnvironment;

		this.selectedNodes = null;
	}

    public String getCurrentVariableSet() {
        this.currentVariableSet = BeanUtil.getSfContext().getConnectionManager().getEnvironmentVariableSet(getCurrentEnvironment());
        return currentVariableSet;
    }

    public void setCurrentVariableSet(String currentVariableSet) {
        logger.info("setCurrentVariableSet invoked {} currentVariableSet[{}]", getUser(), currentVariableSet);
        this.currentVariableSet = StringUtils.stripToNull(currentVariableSet);

        if(this.currentVariableSet == null) {
            this.variables = emptyList();
        } else {
            this.variables = copyOf(BeanUtil.getSfContext().getConnectionManager().getVariableSet(currentVariableSet).keySet());
        }
    }

    public void changeVariableSet() {
        try {
            BeanUtil.getSfContext().getConnectionManager().setEnvironmentVariableSet(currentEnvironment, currentVariableSet);
        } catch(Throwable t) {
            logger.error("Failed to change variable set to: {}", currentVariableSet, t);
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Failed to change variable set to: " + currentVariableSet, t.getMessage());
        }
    }

	public String getCurrentEnvironmentForCopy() {
		return currentEnvironmentForCopy;
	}

	public void setCurrentEnvironmentForCopy(String currentEnvironmentForCopy) {
		logger.info("setCurrentEnvironment invoked {} currentEnvironmentForCopy[{}]", getUser(), currentEnvironmentForCopy);
		this.currentEnvironmentForCopy = currentEnvironmentForCopy;
	}

    public void preVariableSetDelete() {
        if(selectedVariableSet == null) {
            BeanUtil.addWarningMessage("Warning", "Select a variable set");
            return;
        }

        RequestContext.getCurrentInstance().execute("PF('variableSetDeletionConfirmation').show()");
    }

    public void deleteVariableSet() {
        logger.info("deleteVariableSet invoked {} selectedVariableSet[{}]", getUser(), selectedVariableSet);

        if(selectedVariableSet == null) {
            return;
        }

        try {
            TestToolsAPI.getInstance().removeVariableSet(selectedVariableSet);
        } catch(Exception e) {
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage());
        }

        selectedVariableSet = null;
    }

    public synchronized void handleVariableSetsImport(FileUploadEvent event) throws FileNotFoundException {
        logger.info("handleVariableSetsImport invoked {}", getUser());

        try {
            TestToolsAPI.getInstance().importVariableSets(event.getFile().getInputstream(), replaceExistingVariableSets);
        } catch(Exception e) {
            logger.error("Failed to import variable sets", e);
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "", e.getMessage());
            return;
        }

        RequestContext.getCurrentInstance().update("form");

        logger.info("Import finished");
    }

	protected String getUser(){
		return System.getProperty("user.name");
	}

	protected class ServiceNotifyListener implements IServiceNotifyListener, Serializable {

		@Override
		public void onErrorProcessing(String message) {
			try {
                BeanUtil.addErrorMessage("Error", message);
			} catch (Exception ignore){}
		}

		@Override
		public void onInfoProcessing(String message) {
			try {
                BeanUtil.addInfoMessage("Info", message);
			} catch (Exception ignore){}
		}
	}

    public LazyDataModel<EnvironmentNode> getLazyModel() {
        return lazyModel;
    }

	public void setRespectFileName() {
		Map<String, String> map = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
		String name = map.get("respectFileName");
		this.respectFileName = Boolean.valueOf(name);
	}

	public boolean isSkipExistingServices() {
		return skipExistingServices;
	}

	public void setSkipExistingServices(boolean skipExistingServices) {
		this.skipExistingServices = skipExistingServices;
        if(skipExistingServices) {
            replaceExistingServices = false;
        }
	}

	public String getNameFilter() {
		return nameFilter;
	}

	public void setNameFilter(String nameFilter) {
		this.nameFilter = nameFilter;
	}

	public String getTypeFilter() {
		return typeFilter;
	}

	public void setTypeFilter(String typeFilter) {
		this.typeFilter = typeFilter;
	}

	public String getSortField() {
		return sortField;
	}

	public void setSortField(String sortField) {
		this.sortField = sortField;
	}

	public String getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(String sortOrder) {
		this.sortOrder = sortOrder;
	}

	public EnvironmentNode[] getFilteredNodes() {
		return filteredNodes;
	}

	public void setFilteredNodes(EnvironmentNode[] filteredNodes) {
		this.filteredNodes = filteredNodes;
	}

	public void setShowDisabled(boolean showDisabled) {
        lazyModel.setShowDisabled(showDisabled);
	}

    public boolean isShowDisabled() {
        return lazyModel.isShowDisabled();
	}

    public List<String> completeVariableName(String name) {
        if(isBlank(name)) {
            return variables;
        }

        return variables.stream()
                .filter(variable -> containsIgnoreCase(variable, name))
                .collect(toList());
    }
}
