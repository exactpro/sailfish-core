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
package com.exactpro.sf.embedded.statistics;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DimensionMap {
    private Map<String, List<String>> dimensions = new LinkedHashMap<>();

    @JsonAnyGetter
    public Map<String, List<String>> getDimensions() {
        return dimensions;
    }

    @JsonAnySetter
    public void setDimensions(String key, List<String> value) {
        this.dimensions.put(key, value);
    }

    public List<String> getTagNames() {
        return dimensions.getOrDefault("Tags", Collections.emptyList());
    }

    public List<String> getColumns() {
        return dimensions.getOrDefault("Columns", Collections.emptyList());
    }
}
