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

import static org.apache.commons.lang3.StringUtils.stripToNull;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.faces.application.FacesMessage;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.converters.BooleanConverter;
import org.apache.commons.beanutils.converters.ByteConverter;
import org.apache.commons.beanutils.converters.CharacterConverter;
import org.apache.commons.beanutils.converters.DoubleConverter;
import org.apache.commons.beanutils.converters.FloatConverter;
import org.apache.commons.beanutils.converters.IntegerConverter;
import org.apache.commons.beanutils.converters.LongConverter;
import org.apache.commons.beanutils.converters.ShortConverter;
import org.apache.commons.beanutils.converters.StringConverter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.primefaces.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIConverter;
import com.exactpro.sf.scriptrunner.IConnectionManager;
import com.exactpro.sf.scriptrunner.IServiceNotifyListener;
import com.exactpro.sf.services.ServiceDescription;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.GuiUtil;

public class EnvironmentNode implements Serializable, Comparable<Object> {

    private static final long serialVersionUID = 7408809506587055016L;

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentNode.class);

    private static final ConvertUtilsBean converter; // not serializable

	private final String id;
    private final String description;

	private final List<String> enumeratedValues;
    private final String inputMask;
    private final Type type;
    private final List<EnvironmentNode> nodes;
    private final Class<?> paramClassType;
    private final IServiceNotifyListener notifyListener;
    private final boolean serviceParamRequired;
    private final SailfishURI parentType;
    
	private Object value;
    private String variable;
    private final Map<String, String> variableSet;
    private final String name;
    private final String environment;
	private ServiceStatus status;
	
    private String serviceParamRenderComponent = "defaultTextbox";

    private boolean differentValues; // for several services editing
    private boolean differentVariables;
	
	static {
		
        converter = new ConvertUtilsBean();
        converter.register(new IntegerConverter(null), Integer.TYPE);
        converter.register(new IntegerConverter(null), Integer.class);

        converter.register(new ByteConverter(), Byte.TYPE);
        converter.register(new ByteConverter(), Byte.class);

        converter.register(new ShortConverter(), Short.TYPE);
        converter.register(new ShortConverter(), Short.class);

        converter.register(new LongConverter(),  Long.TYPE);
        converter.register(new LongConverter(),  Long.class);

        converter.register(new FloatConverter(), Float.TYPE);
        converter.register(new FloatConverter(), Float.class);

        converter.register(new DoubleConverter(), Double.TYPE);
        converter.register(new DoubleConverter(), Double.class);

        converter.register(new CharacterConverter(), Character.TYPE);
        converter.register(new CharacterConverter(), Character.class);

        converter.register(new BooleanConverter(), Boolean.TYPE);
        converter.register(new BooleanConverter(), Boolean.class);

        converter.register(new StringConverter(), String.class);
        converter.register(new SailfishURIConverter(), SailfishURI.class);
    }
	
    public EnvironmentNode(
            Type type,
            ServiceDescription parent,
			String name,
			String description,
			List<String> enumeratedValues,
			boolean serviceParamRequired,
            String inputMask,
			Object value,
            String variable,
            Map<String, String> variableSet,
			Class<?> paramClassType,
			List<EnvironmentNode> nodes,
			IServiceNotifyListener notifyListener,
			String environment) {
		
		this.type = type;
		this.name = name;
		this.environment = environment;
		this.value = value;
        setVariable(variable);
        this.variableSet = variableSet;
		this.nodes = nodes;
		this.paramClassType = paramClassType;
		this.notifyListener = notifyListener;
		this.serviceParamRequired = serviceParamRequired;
		this.parentType = parent.getType();
        this.inputMask = inputMask;
        this.description = description == null ? null : description.trim();
        this.enumeratedValues = enumeratedValues;

        this.id = type == Type.SERVICE ? parent.toString() : parent + name;

        if(type == Type.PARAMETER) {
            if(paramClassType == SailfishURI.class) {
				serviceParamRenderComponent = "dictionaryNameSelect";
            } else if("amlVersion".equals(name)) {
				serviceParamRenderComponent = "amlVersionSelect";
			} else if(paramClassType.equals(boolean.class)) {
				serviceParamRenderComponent = "booleanCheckbox";
			} else if(paramClassType.equals(int.class) || paramClassType.equals(long.class) || paramClassType.equals(Integer.class)) {
				serviceParamRenderComponent = "integerTextbox";
			}
        } else if(type == Type.DESCRIPTION) {
            if("HandlerClassName".equals(name)) {
				serviceParamRenderComponent = "handlerClassNameSelect";
			}
		}
	}

	public static EnvironmentNode createDefaultNode(String title, String description) {

		EnvironmentNode clone = new EnvironmentNode(
				Type.DESCRIPTION,
				new ServiceDescription(),
				title,
				description,
				Collections.emptyList(),
				false,
                null,
				"false",
                null,
                null,
				boolean.class,
				null,
				null,
				null
		);
		clone.setServiceParamRenderComponent("hidden");
		return clone;
	}

	@Override
	public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this, SHORT_PREFIX_STYLE);

        List<String> nodeNames = nodes.stream()
                .map(EnvironmentNode::getName)
                .collect(Collectors.toList());

        builder.append("name", name);
        builder.append("description", description);
        builder.append("value", value);
        builder.append("variable", variable);
        builder.append("type", type);
        builder.append("id", id);
        builder.append("nodes", nodeNames);
        builder.append("status", status);

        return builder.toString();
	}

	public enum Type {
		SERVICE,
		PARAMETER,
		DESCRIPTION
	}
	
	public String getName() {
        return name;
	}

	public String getReadableName() {
		return GuiUtil.getReadableName(name);
	}

	public String getValue() {
        return convert(value);
	}

	public void setValue(String val) {

		try {
			this.value = convert(val, paramClassType);
			logger.debug("set value {}", val);
		} catch (Exception e) {
			logger.error("Could not set value {}", val, e);

			RequestContext context = RequestContext.getCurrentInstance();
			context.addCallbackParam("validationFailed", true);
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Could not set parameter value", e.getMessage());
		}
	}

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = stripToNull(variable);
    }

    public Map<String, String> getVariableSet() {
        return variableSet;
    }

    public void saveParamToDataBase(IConnectionManager conManager, ServiceDescription parent) throws Exception {
        conManager.updateService(parent, notifyListener).get();
	}
	
	private synchronized String convert(Object value) {
		return converter.convert(value);
	}
	
	private synchronized Object convert(Object value, Class<?> targetType) {
		return converter.convert(value, paramClassType);
	}
	
	private synchronized Object convert(String value, Class<?> targetType) {
        return (value == null || (value.isEmpty() && !String.class.equals(targetType))) ? null : converter.convert(value, paramClassType);
    }
	
	public void updateParentProperty(ServiceDescription parent) throws Exception {

		if ( type == Type.PARAMETER ) {
            Object convertedValue = (value == null || value.toString().trim().isEmpty()) ? null : convert(value, paramClassType);
			BeanUtils.setProperty(parent.getSettings(), name, convertedValue);

            if(variable == null) {
                parent.getVariables().remove(name);
            } else {
                parent.getVariables().put(name, variable);
            }
		} else if ( type == Type.DESCRIPTION ) {
            parent.setServiceHandlerClassName(value.toString());
		}
	}

	public SailfishURI getServiceType() {
        return parentType;
	}

	public List<EnvironmentNode> getNodeChildren() {
        return nodes;
	}

	public boolean getNodeHasChildren() {
        return nodes != null;
	}

	public String getId() {
        return id;
	}

	@Override
	public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }

        EnvironmentNode node = (EnvironmentNode)o;

        if(id != null ? !id.equals(node.id) : node.id != null) {
            return false;
        }

		return true;
	}

	@Override
	public int hashCode() {
        return id != null ? id.hashCode() : 0;
	}

	public final Type getType() {
		return type;
	}

	public final Class<?> getParamClassType() {
		return paramClassType;
	}

	public final String getStatus() {
        if(type == Type.SERVICE) {
            return status == null ? "NULL" : status.toString();
        }
		return "";
	}

    public final ServiceStatus getServiceStatus() {
        return status;
    }

	public void setStatus(ServiceStatus status) {
		this.status = status;
	}

	public String getDescription() {

		StringBuilder result = new StringBuilder();

        if(paramClassType != null) {
            result.append("Type: ").append(paramClassType.getSimpleName()).append("<br />");
        }
        if(description != null && !description.isEmpty()) {
            result.append("Description: ").append(description.replace("\n", "<br />"));
        }

		return result.toString();
	}

	@Override
	public int compareTo(Object o) {
        return ((EnvironmentNode)o).getName().compareToIgnoreCase(name);
	}

	public boolean isServiceParamRequired() {
		return serviceParamRequired;
	}

	public String getServiceParamRenderComponent() {
		return serviceParamRenderComponent;
	}

	public void setServiceParamRenderComponent(String serviceParamRenderComponent) {
		this.serviceParamRenderComponent = serviceParamRenderComponent;
	}

	public String getEnvironment() {
		return environment;
	}

	public boolean isDifferentValues() {
		return differentValues;
	}

	public void setDifferentValues(boolean differentValues) {
		this.differentValues = differentValues;
	}

    public boolean isDifferentVariables() {
        return differentVariables;
    }

    public void setDifferentVariables(boolean differentVariables) {
        this.differentVariables = differentVariables;
    }

    public String getInputMask() {
        return inputMask;
    }

    public String getFinalValue() {
        return environment != null && variableSet != null && variable != null ? variableSet.getOrDefault(variable, getValue()) : getValue();
    }

    public List<String> getEnumeratedValues() {
        return enumeratedValues;
    }

    public boolean hasEnumeratedValues() {
        return !enumeratedValues.isEmpty();
    }

    public List<String> completeEnumeratedValues(String query) {
        return StringUtils.isNotEmpty(query)
                ? enumeratedValues.stream().filter(value -> StringUtils.containsIgnoreCase(value, query)).collect(Collectors.toList())
                : enumeratedValues;
    }
}
