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
package com.exactpro.sf.storage.impl;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.stripToNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.exactpro.sf.storage.IVariableSetStorage;
import com.exactpro.sf.storage.StorageException;

public abstract class AbstractVariableSetStorage implements IVariableSetStorage {
    protected String requireNonEmpty(String value, String message) {
        return requireNonNull(stripToNull(value), message);
    }

    protected String checkName(String name) {
        return requireNonEmpty(name, "name cannot be empty").toLowerCase();
    }

    protected Map<String, String> checkVariableSet(Map<String, String> variableSet) {
        Map<String, String> checkedVariableSet = new HashMap<>();

        variableSet.forEach((name, value) -> {
            name = requireNonEmpty(name, "Variable name cannot be empty").toLowerCase();
            value = requireNonEmpty(value, "Variable value cannot be empty: " + name);

            if(checkedVariableSet.put(name, value) != null) {
                throw new StorageException("Duplicate variable: " + name);
            }
        });

        return Collections.unmodifiableMap(checkedVariableSet);
    }
}
