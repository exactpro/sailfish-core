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

package com.exactpro.sf.testwebgui.scriptruns.converters;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.comparison.conversion.MultiConverter;
import com.exactpro.sf.testwebgui.GuiUtil;

public class ConverterNode {
    private static final Logger logger = LoggerFactory.getLogger(ConverterNode.class);


    private final List<ConverterNode> nodes;

    private final Class<?> paramClassType;

    private final String name;

    private String serviceParamRenderComponent = "defaultTextbox";

    private Object value;

    public ConverterNode(List<ConverterNode> nodes, Class<?> paramClassType, String name, Object value) {
        this.nodes = nodes;
        this.paramClassType = paramClassType;
        this.name = name;
        this.value = value;

        if (this.paramClassType.equals(Map.class)) {
            serviceParamRenderComponent = "mappingDataTable";
            this.value = new ConverterFormMapAdapter();
        } else if(this.paramClassType.equals(boolean.class)) {
            serviceParamRenderComponent = "booleanCheckbox";
        } else if(paramClassType.equals(int.class) || paramClassType.equals(long.class) || paramClassType.equals(Integer.class) || paramClassType.equals(Long.class)) {
            serviceParamRenderComponent = "integerTextbox";
        }
    }

    public List<ConverterNode> getNodes() {
        return nodes;
    }

    public Class<?> getParamClassType() {
        return paramClassType;
    }

    public String getName() {
        return name;
    }

    public String getServiceParamRenderComponent() {
        return serviceParamRenderComponent;
    }

    public Object getValue() {
        return value;
    }

    @SuppressWarnings("unchecked")
    public void setValue(Object value) {
        if(paramClassType.equals(Map.class)) {
            this.value = value;
        } else {
            this.value = MultiConverter.convert(value, ClassUtils.primitiveToWrapper(paramClassType));
        }
    }

	public String getReadableName() {
       return GuiUtil.getReadableName(name);
    }

    public void clear(){
        if(nodes == null){
            return;
        }

        for(ConverterNode node : nodes){
            try {
                node.value =  node.value.getClass().newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
