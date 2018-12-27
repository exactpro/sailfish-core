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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class AppenderAdapter  implements Serializable {
    private static final long serialVersionUID = -1010492361025052656L;

    private String name;
    private String type;
    private Map<String, String> params;
    private List<Pair<String, String>> pairs;

    public AppenderAdapter() {
        this.name = "";
        this.params = new HashMap<>();
        pairs = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
        pairs = new ArrayList<>();
        for(Map.Entry<String, String> entry : params.entrySet()) {
            pairs.add(new MutablePair<>(entry.getKey(), entry.getValue()));
        }
    }

    public List<Pair<String, String>> getPairs() {
        return pairs;
    }

    public void setPairs(List<Pair<String, String>> pairs) {
        this.pairs = pairs;
    }

}
