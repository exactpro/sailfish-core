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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exactpro.sf.aml.converter.IMatrixConverterSettings;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.testwebgui.GuiSettingsProxy;
import com.exactpro.sf.testwebgui.api.TestToolsAPI;

public class ConverterModel {

    private Map<SailfishURI, ConverterNode> settings;

    public ConverterModel(Set<SailfishURI> converters){
        settings = new HashMap<>();
        for (SailfishURI converter :converters){
            settings.put(converter, createNode(converter));
        }
    }


    private ConverterNode createNode(SailfishURI converterUri){
        IMatrixConverterSettings settings = TestToolsAPI.getInstance().getMatrixConverterSettings(converterUri);
        GuiSettingsProxy proxy = new GuiSettingsProxy(settings);

        List<ConverterNode> params = new ArrayList<>();
        ConverterNode root = new ConverterNode(params, String.class, "Converter Settings", null);

        for (String name : proxy.getParameterNames()) {
            if (proxy.isShowElement(name) && proxy.haveWriteMethod(name)) {
                ConverterNode param = new ConverterNode(null, proxy.getParameterType(name), name, proxy.getParameterValue(name));
                params.add(param);
            }
        }

        return root;
    }

    public Map<SailfishURI, ConverterNode> getSettings() {
        return settings;
    }

    public ConverterNode getSettings(SailfishURI uri) {
        return settings.get(uri);
    }
}
