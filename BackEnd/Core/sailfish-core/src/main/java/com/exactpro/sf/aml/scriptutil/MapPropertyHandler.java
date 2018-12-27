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
package com.exactpro.sf.aml.scriptutil;

import java.util.Map;

import org.mvel2.integration.PropertyHandler;
import org.mvel2.integration.VariableResolverFactory;

public class MapPropertyHandler implements PropertyHandler {
    @Override
    public Object getProperty(String name, Object contextObj, VariableResolverFactory variableFactory) {
        if(contextObj instanceof Map<?, ?>) {
            return ((Map<?, ?>)contextObj).get(name);
        }

        throw new IllegalArgumentException("contextObj is not a Map");
    }

    @Override
    public Object setProperty(String name, Object contextObj, VariableResolverFactory variableFactory, Object value) {
        throw new UnsupportedOperationException("setting property for map is not supported");
    }
}
