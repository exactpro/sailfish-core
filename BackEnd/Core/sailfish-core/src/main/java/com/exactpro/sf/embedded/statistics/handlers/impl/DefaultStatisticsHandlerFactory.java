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

package com.exactpro.sf.embedded.statistics.handlers.impl;

import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.embedded.statistics.handlers.IStatisticsReportHandler;
import com.exactpro.sf.embedded.statistics.handlers.IStatisticsReportHandlerFactory;

@ResourceAliases({"Default_Report"})
public class DefaultStatisticsHandlerFactory implements IStatisticsReportHandlerFactory {
    @Override
    public IStatisticsReportHandler createReportHandler() {
        return new DefaultStatisticsHandler();
    }
}
