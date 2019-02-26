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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;
import org.primefaces.model.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.scriptrunner.IServiceNotifyListener;
import com.exactpro.sf.services.IService;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ServiceDescription;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.GuiSettingsProxy;
import com.exactpro.sf.testwebgui.environment.EnvironmentNode.Type;

public class ServiceNodeLazyModel<T extends EnvironmentNode> extends LazyDataModel<EnvironmentNode> {

	private static final long serialVersionUID = -3562603547119046212L;

	private static final Logger logger = LoggerFactory.getLogger(ServiceNodeLazyModel.class);
	private static final Logger auditLogger = LoggerFactory.getLogger("audit");

    private String currentEnvironment;
    private IServiceNotifyListener notifyListener;
    private List<EnvironmentNode> data;
    private List<String> serviceNamesToEdit = new ArrayList<>();
    private ServiceEventLazyModel<ServiceEventModel> lazyEventsModel;

    private EnvironmentNode severalEdit;

    private TreeNode paramRoot;

    private boolean showDisabled = true;

    public ServiceNodeLazyModel(String currentEnvironment, IServiceNotifyListener notifyListener) {
		this.currentEnvironment = currentEnvironment;
        this.notifyListener = notifyListener;
        data = new ArrayList<EnvironmentNode>();
	}

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

    	in.defaultReadObject();

    }

	@Override
    public EnvironmentNode getRowData(String rowKey) {
		logger.info("getRowData {} invoked {}",rowKey,  BeanUtil.getUser());
        return getEnvironmentNode(rowKey);
    }

    @Override
    public Object getRowKey(EnvironmentNode node) {
        return node.getName();
    }


	@Override
    public void setRowIndex(final int rowIndex) {
        if (rowIndex == -1 || getPageSize() == 0) {
            super.setRowIndex(-1);
        } else {
            super.setRowIndex(rowIndex % getPageSize());
        }
    }

	@Override
	public List<EnvironmentNode> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String,Object> filters) {

		logger.debug("load data");
        data.clear();

        /*if(conManager == null) {
            return new ArrayList<EnvironmentNode>();
        }*/

        ServiceName[] serviceNames = BeanUtil.getSfContext().getConnectionManager().getServiceNames();
        for (ServiceName serviceName: serviceNames) {

            if (serviceName.getEnvironment() != null) {
                if (!currentEnvironment.equals(serviceName.getEnvironment())) {
                    continue;
                }
            } else {
                if (!currentEnvironment.equals(ServiceName.DEFAULT_ENVIRONMENT)) {
                    continue;
                }
            }

            if(!isFiltered(serviceName, filters)) {
                EnvironmentNode envNode = createServiceNode(serviceName);
                if(!isHidden(envNode)) {
                    this.data.add(envNode);
                }
            }
        }

        if(sortField == null) {
            sortField = "name";
            sortOrder = SortOrder.ASCENDING;
        }
        Collections.sort(data, new LazySorter(sortField, sortOrder));

        int dataSize = data.size();
        this.setRowCount(dataSize);

        updateParamRoot();
        if(dataSize > pageSize && pageSize != 0) {
            if(data.size() > (first + pageSize)){
                return data.subList(first, first + pageSize);
            } else{
                return data.subList(first, first + (dataSize % pageSize));
            }
        } else {
            return data;
        }

	}

    public void setCurrentEnvironment(String currentEnvironment) {
        this.currentEnvironment = currentEnvironment;
    }

    public void selectServiceToEdit(String name) {
    	auditLogger.info("selectServiceToEdit {} invoked {}", name, BeanUtil.getUser());
        this.serviceNamesToEdit.clear();
        this.serviceNamesToEdit.add(name);

        if (name == null || name.isEmpty()) {
            logger.error("Service name to edit is not defined");
        }
        load(0, 1000, null,SortOrder.ASCENDING, Collections.<String, Object>emptyMap());
    }

    public void waitServiceToEdit(String name) {
    	logger.info("waitServiceToEdit {} invoked {}", name, BeanUtil.getUser());
    	this.serviceNamesToEdit.clear();
        this.serviceNamesToEdit.add(name);

        if (name == null || name.isEmpty()) {
            logger.error("Service name to edit is not defined");
        }
    }

    public void selectedServicesToEdit(EnvironmentNode[] selectedServices) {

    	this.serviceNamesToEdit.clear();

    	for (EnvironmentNode service : selectedServices) {
    		auditLogger.info("selectServiceToEdit {} invoked {}", service.getName(), BeanUtil.getUser());
            this.serviceNamesToEdit.add(service.getName());
    	}
        load(0, 1000, null,SortOrder.ASCENDING, Collections.<String, Object>emptyMap());
    }

    private void updateParamRoot() {

    	logger.info("updateParamRoot invoked {}", BeanUtil.getUser());

    	TreeNode root = new DefaultTreeNode("root", null);

    	if (this.serviceNamesToEdit.isEmpty()) {
    		this.paramRoot = root;
    		return;
    	}

        EnvironmentNode node;

        if (this.serviceNamesToEdit.size() > 1) {

        	if (this.severalEdit == null) {

                this.severalEdit = new EnvironmentNode(Type.SERVICE, new ServiceDescription(), "Services", null, Collections.emptyList(), false, null, null, null,
		        		new ArrayList<>(), null, null);

		        for (String name : this.serviceNamesToEdit) {

		        	EnvironmentNode current = getEnvironmentNode(name);

		        	if (current == null) {
		        		continue;
		        	}

		        	if (this.severalEdit.getNodeChildren().isEmpty()) {

		        		for (EnvironmentNode toClone : current.getNodeChildren()) {
		        			this.severalEdit.getNodeChildren().add(
		        					new EnvironmentNode(toClone.getType(), new ServiceDescription(), toClone.getName(),
                                            toClone.getDescription(), toClone.getEnumeratedValues(), toClone.isServiceParamRequired(), toClone.getInputMask(),
                                            toClone.getValue(), toClone.getParamClassType(), null, null, null));
		        		}

		        	} else {

		        		Iterator<EnvironmentNode> iter = this.severalEdit.getNodeChildren().iterator();

		        		while (iter.hasNext()) {

		        			EnvironmentNode paramInSeveral = iter.next();

		        			EnvironmentNode found = null;

		        			for (EnvironmentNode param : current.getNodeChildren()) {

		        				if (paramInSeveral.getName().equals(param.getName())) {
		            				found = param;
		            				break;
		            			}
		    	        	}

		        			if (found == null) {
		        				iter.remove();
		        			} else {
		        				if (!nullEquals(found.getValue(), paramInSeveral.getValue())) {
		        					paramInSeveral.setValue(null);
		        					paramInSeveral.setDifferentValues(true);
		        				}
		        			}
		        		}
		        	}
		        }
        	}

	        node = this.severalEdit;

        } else {
        	node = getEnvironmentNode(this.serviceNamesToEdit.get(0));
        }

        if (node != null) {
            EnvironmentNode required = EnvironmentNode.createDefaultNode("Required", "Required parameters for starting service");
            TreeNode requiredNode = new DefaultTreeNode("RequiredContainer", required, root);
            requiredNode.setExpanded(true);

            EnvironmentNode optional = EnvironmentNode.createDefaultNode("Optional", "Optional parameters for starting service");
            TreeNode optionalNode = new DefaultTreeNode("OptionalContainer", optional, root);
            optionalNode.setExpanded(true);

            for (EnvironmentNode param : node.getNodeChildren()) {
                TreeNode subParent = param.isServiceParamRequired() ? requiredNode : optionalNode;
                new DefaultTreeNode(param, subParent);
            }

            if (requiredNode.getChildCount() == 0) {
            	root.getChildren().remove(requiredNode);
            }
            if (optionalNode.getChildCount() == 0) {
            	root.getChildren().remove(requiredNode);
            }
            if (root.getChildCount() == 0) {
            	new DefaultTreeNode("OptionalContainer", "No common parameters found for selected services", root);
            }
        }

        this.paramRoot = root;
    }

    public TreeNode getParamRoot() {
    	return this.paramRoot;
    }

    private boolean nullEquals(Object obj1, Object obj2) {
    	if (obj1 == obj2) return true;
    	if (obj1 == null) {
    		if (obj2 == null) return true;
    		return false;
    	}
    	return obj1.equals(obj2);
    }

    public ServiceEventLazyModel<ServiceEventModel> getLazyEventsModel() {
        return lazyEventsModel;
    }

    public void setLazyEventsModel(ServiceEventLazyModel<ServiceEventModel> lazyEventsModel) {
        this.lazyEventsModel = lazyEventsModel;
    }

    private boolean isHidden(EnvironmentNode envNode) {
        return envNode.getServiceStatus() == ServiceStatus.DISABLED && !this.showDisabled;
    }

    class LazySorter implements Comparator<EnvironmentNode>
    {
        private String sortField;

        private SortOrder sortOrder;

        public LazySorter(String sortField, SortOrder sortOrder) {
            this.sortField = sortField;
            this.sortOrder = sortOrder;
        }

        @Override
        public int compare(EnvironmentNode node1, EnvironmentNode node2) {
            try {

                int result = 0;
                if(sortField.equals("name"))
                {
                    result = node1.getName().compareToIgnoreCase(node2.getName());
                }
                else if (sortField.equals("serviceType"))
                {
                    result = node1.getServiceType().compareTo(node2.getServiceType());
                }
                else if(sortField.equals("status"))
                {
                    result = node1.getStatus().compareToIgnoreCase(node2.getStatus());
                }
                else
                {
                    logger.warn("Unknown column name: {}", sortField);
                }

                return SortOrder.ASCENDING.equals(sortOrder) ? result : -1 * result;
            }
            catch(Exception e) {
                throw new RuntimeException();
            }
        }
    }



    public int getServicesCount(ServiceStatus status) {
        if(status == null){
            return data.size();
        }
        int count = 0;

        for(EnvironmentNode service : data) {
            if(service.getServiceStatus() == status) {
                count ++;
            }
        }

        return count;
    }

    public String getHeaderForEditDialog() {

    	if (this.serviceNamesToEdit.isEmpty()) {
    		return "";
    	}

    	StringBuilder builder = new StringBuilder();

    	logger.info("getHeaderForEditDialog invoked {}", BeanUtil.getUser());

    	if (this.serviceNamesToEdit.size() == 1) {

    		EnvironmentNode node = getEnvironmentNode(this.serviceNamesToEdit.get(0));

        	if (node != null) {

    	    	builder.append(node.getName())
    	    		   .append(" (")
    	    		   .append(node.getServiceType())
    	    		   .append(")");
        	}

    	} else {

    		int i = 0;
    		for (String name : this.serviceNamesToEdit) {

    			builder.append(name);

    			if (++i < this.serviceNamesToEdit.size()) {
    				builder.append(", ");
    			}
    		}
    	}

    	return builder.toString();
    }

    public void selectServiceForEvents(String name) {
    	logger.info("selectServiceForEvents {} invoked {}", name, BeanUtil.getUser());
        EnvironmentNode node = getEnvironmentNode(name);
        if (node != null) {
            lazyEventsModel = new ServiceEventLazyModel<>(new ServiceName(node.getEnvironment(), node.getName()));
        }
    }

    protected boolean isFiltered(ServiceName serviceName, Map<String,Object> filters){
        if(MapUtils.isEmpty(filters)) {
            return false;
        }
        ServiceDescription description = BeanUtil.getSfContext().getConnectionManager().getServiceDescription(serviceName);
        for(Map.Entry<String, Object> filterProperty: filters.entrySet()) {
            Object filterValue = filterProperty.getValue();
            String fieldValue;

            if(filterProperty.getKey().equals("name")) {
                fieldValue = serviceName.getServiceName();
            } else if (filterProperty.getKey().equals("serviceType")) {
                fieldValue = description.getType().toString();
            } else if(filterProperty.getKey().equals("status")) {
                IService iService = BeanUtil.getSfContext().getConnectionManager().getService(serviceName);
                fieldValue = iService.getStatus().name();
            } else {
                logger.error("Unknown filter property: {}", filterProperty.getKey());
                return false;
            }

            if(StringUtils.containsIgnoreCase(fieldValue, filterValue.toString())){
                return false;
            }
        }

        return true;
    }

    public void applyServiceSettings() {

    	for (String name : this.serviceNamesToEdit) {

    		auditLogger.info("applyServiceSettings {} invoked {}", name, BeanUtil.getUser());

	        EnvironmentNode serviceNode = getEnvironmentNode(name);

	        if (this.serviceNamesToEdit.size() > 1) {

	        	if (this.severalEdit == null) {
	        		return;
	        	}

	        	for (EnvironmentNode paramInSeveral : this.severalEdit.getNodeChildren()) {

	        		for (EnvironmentNode param : serviceNode.getNodeChildren()) {

	        			if (paramInSeveral.getName().equals(param.getName())) {

	        				if (!paramInSeveral.isDifferentValues() ||
	        						(paramInSeveral.isDifferentValues() && paramInSeveral.getValue() != null)) {

	        					param.setValue(paramInSeveral.getValue());
	        				}

	        				break;
	        			}
	        		}
	        	}
	        }

	        if(serviceNode == null) {
	            BeanUtil.addErrorMessage("Error", "Can not find service with name: " + name + " to apply its setting");
	            return;
	        }

	        ServiceName serviceName = new ServiceName(currentEnvironment, name);
	        ServiceDescription serviceDescription = BeanUtil.getSfContext().getConnectionManager().getServiceDescription(serviceName);

	        boolean error = false;

	        IServiceSettings settingsCopy = serviceDescription.clone().getSettings();
	        String serviceHandlerClassNameCopy = serviceDescription.getServiceHandlerClassName();

	        EnvironmentNode serviceParam = null;
	        try {
	            Iterator<EnvironmentNode> iterator = serviceNode.getNodeChildren().iterator();
	            while(iterator.hasNext()) {
	                serviceParam = iterator.next();
	                logger.info("update param {}: {}", serviceParam.getName(), serviceParam.getValue());
	                serviceParam.updateParentProperty(serviceDescription);
	            }

	            try {
	                serviceNode.saveParamToDataBase(serviceName, BeanUtil.getSfContext().getConnectionManager(), serviceDescription);
	            } catch (Exception e) {
	                error = true;
	                BeanUtil.addErrorMessage("Can not set parameters in database", "Problem with setting to " + serviceNode.getName());
	            }

	        } catch (Exception e) {
                logger.error(e.getMessage(), e);
	            error = true;
	            BeanUtil.addErrorMessage("Can not set parameter", "Problem with setting to " + serviceParam.getName() + " value " + serviceParam.getValue());

	            // rollback settings
	            serviceDescription.setSettings(settingsCopy);
	            serviceDescription.setServiceHandlerClassName(serviceHandlerClassNameCopy);
	        }

	        if (error) {
	            ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
	            HttpServletResponse response = (HttpServletResponse) context.getResponse();
	            response.setStatus(500);
	        }
    	}

    	this.severalEdit = null;
    }

    public void restoreServiceSettings() {

    	for (String name : this.serviceNamesToEdit) {

    		auditLogger.info("restoreServiceSettings {} invoked {}", name, BeanUtil.getUser());

	        EnvironmentNode serviceNode = getEnvironmentNode(name);

	        if(serviceNode == null) {
	            BeanUtil.addErrorMessage("Error", "Can not find service with name: " + name + " to restore its settings");
	            return;
	        }

	        int index = this.data.indexOf(serviceNode);

	        ServiceName serviceName = new ServiceName(currentEnvironment, name);

	        EnvironmentNode envNode = createServiceNode(serviceName);
	        this.data.set(index, envNode);
    	}

    	this.severalEdit = null;
    }

    private EnvironmentNode createServiceNode(ServiceName serviceName) {

        ServiceDescription sd = BeanUtil.getSfContext().getConnectionManager().getServiceDescription(serviceName);

        GuiSettingsProxy proxy = new GuiSettingsProxy(sd.getSettings());
        List<EnvironmentNode> params = new ArrayList<>();

        EnvironmentNode handlerClassParamNode = new EnvironmentNode(EnvironmentNode.Type.DESCRIPTION, sd,
                "HandlerClassName", "", Collections.emptyList(), false, null, sd.getServiceHandlerClassName(), String.class, null,
                notifyListener, null);

        params.add(handlerClassParamNode);

        for (String name : proxy.getParameterNames()) {

            if (proxy.haveWriteMethod(name)) {

                EnvironmentNode paramNode = new EnvironmentNode(
                        EnvironmentNode.Type.PARAMETER,
                        sd,
                        name,
                        proxy.getParameterDescription(name),
                        proxy.getEnumeratedValues(name),
                        proxy.checkRequiredParameter(name),
                        proxy.getParameterMask(name),
                        proxy.getParameterValue(name),
                        proxy.getParameterType(name),
                        null, notifyListener, null);

                params.add(paramNode);
            }
        }

        Collections.sort(params, Collections.reverseOrder());

        EnvironmentNode envNode = new EnvironmentNode(
                EnvironmentNode.Type.SERVICE, sd, sd.getName(), "",
                Collections.emptyList(), false, null, null, null, params,
                notifyListener, serviceName.getEnvironment());

        envNode.setStatus(BeanUtil.getSfContext().getConnectionManager().getService(serviceName).getStatus());
        return envNode;
    }

    private EnvironmentNode getEnvironmentNode(String serviceName) {
        if (serviceName != null) {
            for (EnvironmentNode node : this.data) {
                if (node.getName().equals(serviceName)) {
                    return node;
                }
            }
            logger.warn("Service {} not found in the environment {}", serviceName, currentEnvironment);
        }
        return null;
    }

    public void setShowDisabled(boolean showDisabled) {
        this.showDisabled = showDisabled;
    }

    public boolean isShowDisabled() {
        return showDisabled;
    }
}
