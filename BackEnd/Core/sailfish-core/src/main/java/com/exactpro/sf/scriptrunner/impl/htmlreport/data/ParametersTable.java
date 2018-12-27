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
package com.exactpro.sf.scriptrunner.impl.htmlreport.data;

import java.util.List;

public class ParametersTable {
    private final int id;
    private final String messageName;
    private final List<ActionParameter> parameters;
    private final boolean hasHeaders;

    public ParametersTable(int id, String messageName, List<ActionParameter> parameters, boolean hasHeaders) {
        this.id = id;
        this.messageName = messageName;
        this.parameters = parameters;
        this.hasHeaders = hasHeaders;
    }

    public int getId() {
        return id;
    }

    public String getMessageName() {
        return messageName;
    }

    public List<ActionParameter> getParameters() {
        return parameters;
    }

    public boolean isHasHeaders() {
        return hasHeaders;
    }
}
