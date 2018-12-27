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
package com.exactpro.sf.statictesting;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang3.StringUtils;

import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.center.SFContextSettings;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.configuration.workspace.DefaultWorkspaceDispatcherBuilder;
import com.exactpro.sf.configuration.workspace.DefaultWorkspaceLayout;

public abstract class AbstractStaticTest {
    protected static ISFContext initContext(List<File> workspaceLayers) throws Exception {
        HierarchicalConfiguration config = new HierarchicalConfiguration();
        config.setRootNode(new HierarchicalConfiguration.Node("configuration"));

        config.addProperty("Environment.GeneralSettings.StorageType", "file");
        config.addProperty("Environment.GeneralSettings.FileStoragePath", "storage");
        config.addProperty("Environment.GeneralSettings.StoreAdminMessages", "true");
        config.addProperty("Environment.GeneralSettings.AsyncRunMatrix", "false");
        config.addProperty("Environment.ScriptRun.NotificationIfSomeServicesNotStarted", "false");
        config.addProperty("Environment.ScriptRun.FailUnexpected", "false");

        SFContextSettings settings = new SFContextSettings();

        settings.setConfig(config);
        settings.setCompilerClassPath(StringUtils.EMPTY);

        DefaultWorkspaceDispatcherBuilder builder = new DefaultWorkspaceDispatcherBuilder();

        for(File workspaceLayer : workspaceLayers) {
            builder.addWorkspaceLayer(workspaceLayer, DefaultWorkspaceLayout.getInstance());
        }

        SFLocalContext context = SFLocalContext.createContext(builder.build(), settings);
        String separator = System.getProperty("path.separator");
        Set<String> classPathElements = new LinkedHashSet<>();

        for(String classPathElement : System.getProperty("java.class.path").split(separator)) {
            classPathElements.add(classPathElement);
        }

        for(ClassLoader classLoader : context.getPluginClassLoaders().values()) {
            URL[] urls = ((URLClassLoader)classLoader).getURLs();

            for(URL url : urls) {
                classPathElements.add(url.getFile());
            }
        }

        context.setCompilerClassPath(String.join(separator, classPathElements));

        return context;
    }
}
