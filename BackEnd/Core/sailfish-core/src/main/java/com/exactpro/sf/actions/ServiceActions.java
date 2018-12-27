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
package com.exactpro.sf.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;

import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.aml.CommonColumn;
import com.exactpro.sf.aml.CommonColumns;
import com.exactpro.sf.aml.CustomColumn;
import com.exactpro.sf.aml.CustomColumns;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.actionmanager.ActionMethod;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionServiceManager;
import com.exactpro.sf.services.IService;
import com.exactpro.sf.services.IServiceSettings;

/**
 * @author nikita.smirnov
 *
 */
@ResourceAliases({"ServiceActions"})
public class ServiceActions extends AbstractCaller {

    //TODO: change to service_uri someday
    public final String SERVICE_TYPE = "service_type";

	private final ThreadLocal<ConvertUtilsBean> converter = new ThreadLocal<ConvertUtilsBean>() {
		@Override
        protected ConvertUtilsBean initialValue() {
			return new ConvertUtilsBean();
		}
	};

	@CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
    @CustomColumns({
        @CustomColumn(value = SERVICE_TYPE, required = true)
    })
	@ActionMethod
    public void createService(IActionContext actionContext, HashMap<?, ?> message) {
		try {
		    ServiceName serviceName = ServiceName.parse(actionContext.getServiceName());
	        SailfishURI serviceURI = SailfishURI.parse((String)message.get(SERVICE_TYPE));

			if (StringUtils.isEmpty(serviceName.getServiceName())) {
				throw new EPSCommonException("service_name is empty");
			}
			if (!serviceName.getServiceName().matches("[a-zA-Z](.+)?")) {
				throw new EPSCommonException("Service name must start with a roman letter");
			}
			if (!serviceName.getServiceName().matches("[\\w]*")) {
				throw new EPSCommonException("Service name must contain only roman letters, numbers and underscores");
			}
			if (StringUtils.isEmpty(serviceName.getEnvironment())) {
				throw new EPSCommonException("environment is empty");
			}
			if(checkServiceDuplicates(actionContext, serviceName)){
				actionContext.getServiceManager().addService(serviceName, serviceURI, null, null).get();
			} else {
			    throw new EPSCommonException("Environment '" + serviceName.getEnvironment() + "' has service with name '" + serviceName.getServiceName() + "' already");
			}
		} catch(SailfishURIException e) {
		    throw new EPSCommonException("service_type contains invalid service URI", e);
		} catch(Exception e){
			if(e instanceof RuntimeException){
				throw (RuntimeException)e;
			} else {
				throw new EPSCommonException(e);
			}
		}
	}

	@CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
	@ActionMethod
    public void deleteService(IActionContext actionContext) {
		try {
			ServiceName serviceName = new ServiceName(actionContext.getServiceName());
			SFLocalContext.getDefault().getConnectionManager().removeService(serviceName, null).get();
		} catch(Exception e){
			throw new EPSCommonException(e);
		}
	}

	@CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
	@ActionMethod
    public void startService(IActionContext actionContext) {
		ServiceName serviceName = ServiceName.parse(actionContext.getServiceName());

		try {
		    IActionServiceManager serviceManager = actionContext.getServiceManager();
		    serviceManager.initService(serviceName, null).get();
		    serviceManager.startService(serviceName, null).get();
		} catch (Exception e) {
			throw new EPSCommonException(e);
		}
	}

	@CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
	@ActionMethod
    public void stopService(IActionContext actionContext) {
		ServiceName serviceName = ServiceName.parse(actionContext.getServiceName());

		try {
		    actionContext.getServiceManager().disposeService(serviceName, null).get();
		} catch (Exception e){
			throw new EPSCommonException(e);
		}
	}

	@CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
	@ActionMethod
    public void initService(IActionContext actionContext, HashMap<?, ?> message) {
		ServiceName serviceName = ServiceName.parse(actionContext.getServiceName());
		IActionServiceManager serviceManager = actionContext.getServiceManager();

		try {
            IServiceSettings serviceSettings = serviceManager.getServiceSettings(serviceName);
			BeanMap beanMap = new BeanMap(serviceSettings);
			Set<String> incorrectProperty = new HashSet<>();
			for (Entry<?, ?> entry : message.entrySet()) {
				String property = convertProperty(entry.getKey().toString());
				if (beanMap.containsKey(property)){
					try {
						BeanUtils.setProperty(serviceSettings, property, converter.get().convert(entry.getValue(), beanMap.getType(property)));
					} catch (IllegalAccessException e) {
						throw new EPSCommonException(e);
					} catch (InvocationTargetException e) {
						throw new EPSCommonException(e);
					}
				} else {
					incorrectProperty.add(property);
				}
			}

			if (!incorrectProperty.isEmpty()) {
				throw new EPSCommonException(serviceSettings.getClass().getName() + " does not contain properties: " + incorrectProperty);
			}

			serviceManager.updateService(serviceName, serviceSettings, null);

			serviceManager.initService(serviceName, null).get();
		} catch (EPSCommonException e) {
			throw e;
		} catch (Exception e){
			throw new EPSCommonException(e);
		}
	}

	@CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
    @ActionMethod
    public HashMap<String, String> getServiceSettings(IActionContext actionContext) {
	    IService service = SFLocalContext.getDefault().getConnectionManager().getService(ServiceName.parse(actionContext.getServiceName()));
        BeanMap beanMap = new BeanMap(service.getSettings());
        HashMap<String, String> result = new HashMap<>();

        for(Object property : beanMap.keySet()) {
            Object value = beanMap.get(property);
            if (value != null) {
                result.put(property.toString(), value.toString());
            }
        }

        return result;
    }

    public static String convertProperty(String value) {
		if (value.contains(" ")) {
			return WordUtils.uncapitalize(WordUtils.capitalizeFully(value).replaceAll(" ", ""));
		} else {
			return WordUtils.uncapitalize(value);
		}
	}

    protected static boolean checkServiceDuplicates(IActionContext actionContext, ServiceName serviceName) {
        ServiceName[] serviceNames = actionContext.getServiceManager().getServiceNames();
        for (ServiceName sName : serviceNames) {
            if (sName.equals(serviceName)) {
                return false;
            }
        }
        return true;
    }
}
