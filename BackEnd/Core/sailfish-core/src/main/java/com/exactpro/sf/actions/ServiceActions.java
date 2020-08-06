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

import static com.exactpro.sf.actions.ActionUtil.unwrapFilters;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.text.WordUtils;

import com.exactpro.sf.aml.CommonColumn;
import com.exactpro.sf.aml.CommonColumns;
import com.exactpro.sf.aml.CustomColumn;
import com.exactpro.sf.aml.CustomColumns;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.actionmanager.ActionMethod;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionServiceManager;
import com.exactpro.sf.services.IService;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.util.DateTimeUtility;

/**
 * @author nikita.smirnov
 *
 */
@ResourceAliases("ServiceActions")
public class ServiceActions extends AbstractCaller {

    private static final DateTimeFormatter FORMATTER = DateTimeUtility.createFormatter("_dd_MM_yyyy_HH_mm_ss_SSS");
    private static final String servicesFolder = "services";
    private static final String changingSettingFolder = "changingSetting";

    //TODO: change to service_uri someday
    public final String SERVICE_TYPE = "service_type";

    private static class SailfishURIConverter implements Converter {
        @Override
        public <T> T convert(Class<T> type, Object value) {
            if (value instanceof String) {
                try {
                    return (T) SailfishURI.parse((String) value);
                } catch (SailfishURIException e) {
                    throw new EPSCommonException("Wrong value [" + value + "] for parse to SailfishURI", e);
                }
            }

            throw new EPSCommonException("Wrong value [" + value + "] for parse to SailfishURI");
        }
    }

	private final ThreadLocal<ConvertUtilsBean> converter = new ThreadLocal<ConvertUtilsBean>() {
		@Override
        protected ConvertUtilsBean initialValue() {
		    ConvertUtilsBean convertUtilsBean = new ConvertUtilsBean();
		    convertUtilsBean.register(new SailfishURIConverter(), SailfishURI.class);
		    return convertUtilsBean;
		}
	};

	@CommonColumns(@CommonColumn(value = Column.ServiceName, required = true))
    @CustomColumns(@CustomColumn(value = SERVICE_TYPE, required = true))
	@ActionMethod
    public void createService(IActionContext actionContext, HashMap<?, ?> message) {
		try {
		    ServiceName serviceName = ServiceName.parse(actionContext.getServiceName());
            SailfishURI serviceURI = SailfishURI.parse(unwrapFilters(message.get(SERVICE_TYPE)));

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

	@CommonColumns(@CommonColumn(value = Column.ServiceName, required = true))
	@ActionMethod
    public void deleteService(IActionContext actionContext) {
		try {
			ServiceName serviceName = new ServiceName(actionContext.getServiceName());
			SFLocalContext.getDefault().getConnectionManager().removeService(serviceName, null).get();
		} catch(Exception e){
			throw new EPSCommonException(e);
		}
	}

	@CommonColumns(@CommonColumn(value = Column.ServiceName, required = true))
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

	@CommonColumns(@CommonColumn(value = Column.ServiceName, required = true))
	@ActionMethod
    public void stopService(IActionContext actionContext) {
		ServiceName serviceName = ServiceName.parse(actionContext.getServiceName());

		try {
		    actionContext.getServiceManager().disposeService(serviceName, null).get();
		} catch (Exception e){
			throw new EPSCommonException(e);
		}
	}

    private static final String servicesFileExpression = ".xml";

	@CommonColumns(@CommonColumn(value = Column.ServiceName, required = true))
	@ActionMethod
    public void initService(IActionContext actionContext, HashMap<?, ?> message) throws IllegalAccessException, InvocationTargetException, InterruptedException, IOException {
		ServiceName serviceName = ServiceName.parse(actionContext.getServiceName());
		IActionServiceManager serviceManager = actionContext.getServiceManager();

		try {
            IServiceSettings serviceSettings = serviceManager.getServiceSettings(serviceName);
			BeanMap beanMap = new BeanMap(serviceSettings);
			Set<String> editedProperty = new HashSet<>();
			Set<String> incorrectProperty = new HashSet<>();
			for (Entry<?, ?> entry : message.entrySet()) {
				String property = convertProperty(entry.getKey().toString());
				if (beanMap.containsKey(property)){
					BeanUtils.setProperty(serviceSettings, property, converter.get().convert((Object)unwrapFilters(entry.getValue()), beanMap.getType(property)));
                    editedProperty.add(property);
				} else {
					incorrectProperty.add(property);
				}
			}

			if (!incorrectProperty.isEmpty()) {
				throw new EPSCommonException(serviceSettings.getClass().getName() + " does not contain properties: " + incorrectProperty);
			}

            serviceManager.updateService(serviceName, serviceSettings, null).get();

			try (FileOutputStream out = new FileOutputStream(actionContext.getReport().createFile(StatusType.NA, servicesFolder, changingSettingFolder, serviceName + FORMATTER.format(DateTimeUtility.nowLocalDateTime()) + servicesFileExpression))) {
                actionContext.getServiceManager().serializeServiceSettings(serviceName, out);
            }
        } catch (ExecutionException e) {
            ExceptionUtils.rethrow(ObjectUtils.defaultIfNull(e.getCause(), e));
        }
	}

    @CommonColumns(@CommonColumn(value = Column.ServiceName, required = true))
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

    /**
     * Converts property name to be used in the {@link BeanMap}.<br/>
     * Note: if the value is fully in upper case (like, URL) it will be returned unmodified.
     * @param value property name to convert
     * @return property name with removed whitespaces and uncapitalized the first character (e.g. Foo -> foo, FooBar -> fooBar, Foo Bar -> fooBar).
     * In case the value contains only capitalized characters it will be returned unmodified (e.g. URL -> URL).
     */
    public static String convertProperty(String value) {
		if (value.contains(" ")) {
			return WordUtils.uncapitalize(WordUtils.capitalizeFully(value).replaceAll(" ", ""));
		}
        if (value.length() > 1 && StringUtils.isAllUpperCase(value)) {
            return value;
        }
        return WordUtils.uncapitalize(value);
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
