/*******************************************************************************
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

package com.exactpro.sf.embedded.statistics.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.center.impl.CoreVersion;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.ILoadableManager;
import com.exactpro.sf.configuration.ILoadableManagerContext;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIRule;
import com.exactpro.sf.configuration.suri.SailfishURIUtils;
import com.exactpro.sf.embedded.statistics.handlers.impl.DefaultStatisticsHandlerFactory;

public class StatisticsReportHandlerLoader implements ILoadableManager {
    private final static Logger logger = LoggerFactory.getLogger(StatisticsReportHandlerLoader.class);

    private final Map<SailfishURI, IStatisticsReportHandlerFactory> uriToClass = new ConcurrentHashMap<>();

    public StatisticsReportHandlerLoader() {
        IVersion coreVersion = new CoreVersion();
        List<IStatisticsReportHandlerFactory> coreHandlers = new ArrayList<>();
        coreHandlers.add(new DefaultStatisticsHandlerFactory());

        for (IStatisticsReportHandlerFactory handlerFactory : coreHandlers) {
            registerHandlerFactory(coreVersion, handlerFactory);
        }
    }

    @Override
    public void load(ILoadableManagerContext context) throws Exception {
        IVersion pluginVersion = context.getVersion();
        if (!pluginVersion.isGeneral()) {
            ClassLoader classLoader = context.getClassLoaders()[0];
            ServiceLoader<IStatisticsReportHandlerFactory> reportHandlerFactories = ServiceLoader.load(IStatisticsReportHandlerFactory.class, classLoader);

            for (IStatisticsReportHandlerFactory reportHandlerFactory : reportHandlerFactories) {
                registerHandlerFactory(pluginVersion, reportHandlerFactory);
            }
        }
    }

    @Override
    public void finalize(ILoadableManagerContext context) throws Exception {

    }

    private void registerHandlerFactory(IVersion pluginVersion, IStatisticsReportHandlerFactory factory) {
        try {
            Class<? extends IStatisticsReportHandlerFactory> reportHandlerFactoryClass = factory.getClass();
            ResourceAliases resourceAliases = reportHandlerFactoryClass.getAnnotation(ResourceAliases.class);

            if (resourceAliases == null) {
                throw new EPSCommonException("No resource annotation for report handler factory class: " + reportHandlerFactoryClass.getCanonicalName());
            }

            String[] classAliases = resourceAliases.value();

            if (classAliases.length == 0) {
                throw new EPSCommonException("No resource aliases for report handler factory class: " + reportHandlerFactoryClass.getCanonicalName());
            }

            for (String alias : classAliases) {
                SailfishURI reportHandlerURI = new SailfishURI(pluginVersion.getAlias(), null, alias);
                IStatisticsReportHandlerFactory oldFactory = uriToClass.put(reportHandlerURI, factory);
                if (oldFactory != null) {
                    logger.warn("Duplicated resource name {}. Old factory class: {}; new factory class: {}",
                            reportHandlerURI, oldFactory.getClass().getCanonicalName(), reportHandlerFactoryClass.getCanonicalName());
                }
            }
            logger.info("StatisticsReportHandler {} has been loaded", reportHandlerFactoryClass.getCanonicalName());

        } catch (Exception e) {
            logger.warn("Can't register {} report handler", factory.getClass().getCanonicalName(), e);
        }
    }

    public Set<SailfishURI> getStatisticsReportHandlersURI() {
        return uriToClass.keySet();
    }

    public IStatisticsReportHandler getStatisticsReportHandler(SailfishURI uri) {
        if (uri == null) {
            throw new EPSCommonException("URI can't be NULL");
        }
        IStatisticsReportHandlerFactory reportHandlerFactory = SailfishURIUtils.getMatchingValue(uri, uriToClass, SailfishURIRule.REQUIRE_RESOURCE);
        if (reportHandlerFactory == null) {
            throw new EPSCommonException("Can't find reportHandler for URI: " + uri);
        }
        return reportHandlerFactory.createReportHandler();
    }
}
