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
package com.exactpro.sf.testwebgui.configuration;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import com.exactpro.sf.testwebgui.configuration.IterableConverter.CollectionType;

@SuppressWarnings("serial")
public class EnvironmentEntity implements Serializable {
    private final CollectionType type; 
    private final String paramName;
    private final String paramDescription;
    private final String paramValidateRegex;
    private Object paramValue;

    public EnvironmentEntity(String paramName, Object paramValue, String paramValidateRegex, String paramDescription) {
        this.paramName = paramName;
        this.paramValue = paramValue;
        this.paramValidateRegex = paramValidateRegex;
        this.paramDescription = paramDescription;
        if (paramValue instanceof Set) {
            this.type = CollectionType.SET;
        } else if (paramValue instanceof List) {
            this.type = CollectionType.LIST;
        } else {
            this.type = null;
        }
    }

    public String getParamName() {
        return paramName;
    }

    public Object getParamValue() {
        return paramValue;
    }

    public void setParamValue(Object paramValue) {
        this.paramValue = paramValue;
    }

    public String getParamValidateRegex() {
        return paramValidateRegex;
    }
    
    public String getParamDescription() {
        return paramDescription;
    }

    public String getReadableName() {
        return (Character.toUpperCase(paramName.charAt(0)) + paramName.substring(1)).replaceAll("([A-Z0-9]+)", " $1");
    }
    
    public String getType() {
        return this.type != null ? this.type.name() : null;
    }
}
