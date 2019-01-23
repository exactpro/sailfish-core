/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.storage.entities;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.exactpro.sf.storage.impl.AbstractPersistentObject;

public class StoredVariableSet extends AbstractPersistentObject {
    private String name;
    private Map<String, String> variables = new HashMap<>();

    public String getName() {
        return name;
    }

    public StoredVariableSet setName(String name) {
        this.name = name;
        return this;
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public StoredVariableSet setVariables(Map<String, String> variables) {
        this.variables = variables;
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("name", name)
                .append("variables", variables)
                .toString();
    }
}
