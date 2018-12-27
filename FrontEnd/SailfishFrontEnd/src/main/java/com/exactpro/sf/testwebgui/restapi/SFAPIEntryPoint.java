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
package com.exactpro.sf.testwebgui.restapi;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/sfapi")
public class SFAPIEntryPoint extends ResourceConfig {
    public SFAPIEntryPoint() {
        register(MultiPartFeature.class);
        register(ServiceResource.class);
        register(MatrixResource.class);
        register(DictionaryResource.class);
        register(MatrixEditorResource.class);
        register(SFRestApiListener.class);
        register(ResponseResolver.class);
        register(TestscriptRunResource.class);
        register(EnvironmentResource.class);
        register(ActionsResource.class);
        register(StatisticsResource.class);
        register(CORSFilter.class);
        register(MachineLearningResource.class);
        register(BigButtonResource.class);
        register(TestLibraryResource.class);
        register(SailfishInfoResource.class);
        register(JacksonFeature.class);
        register(StorageResource.class);
        register(InternalResources.class);
    }
}
