/*******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.sf.scriptrunner;

import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.ILoadableManagerContext;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class ScriptReportLoader implements IScriptReportLoader {

    private final Logger logger = LoggerFactory.getLogger(ILoggingConfigurator.getLoggerName(this));

    private final SetMultimap<String, IScriptReportFactory> factories = HashMultimap.create();

    @Override
    public void load(ILoadableManagerContext context) throws Exception {
        for (IScriptReportFactory factory : ServiceLoader.load(IScriptReportFactory.class, context.getClassLoaders()[0])) {
            factories.put(context.getVersion().getAlias(), factory);
        }
    }

    @Override
    public Set<IScriptReport> createScriptReports(String reportFolder, IWorkspaceDispatcher workspaceDispatcher, IDictionaryManager dictionaryManager, long scriptId) {
        return factories.entries().stream().map(entry ->
           createScriptReport(entry.getValue(),
                   reportFolder,
                   workspaceDispatcher,
                   dictionaryManager,
                   entry.getKey(),
                   scriptId)
        ).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private IScriptReport createScriptReport(IScriptReportFactory factory, String reportFolder, IWorkspaceDispatcher workspaceDispatcher, IDictionaryManager dictionaryManager, String pluginName, long scriptID) {
        try {
            return factory.createScriptReport(reportFolder, workspaceDispatcher, dictionaryManager);
        } catch (Exception e) {
            if (logger.isWarnEnabled()) {
                logger.warn("Can not create script report from class '{}' from plugin '{}' for script with id '{}'", factory.getClass().getName(), pluginName, scriptID, e);
            }
        }
        return null;
    }

    @Override
    public void finalize(ILoadableManagerContext context) throws Exception {}
}
