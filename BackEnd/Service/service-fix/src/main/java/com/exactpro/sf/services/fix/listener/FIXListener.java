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
package com.exactpro.sf.services.fix.listener;

import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.MessageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FIXListener implements IFIXListener {
    protected final Logger logger = LoggerFactory.getLogger(ILoggingConfigurator.getLoggerName(this));

    protected IServiceContext serviceContext;
    protected IServiceSettings settings;
    protected MessageHelper messageHelper;

    @Override
    public void init(IServiceContext serviceContext, IServiceSettings settings, MessageHelper messageHelper) {
        this.serviceContext = serviceContext;
        this.messageHelper = messageHelper;
        this.settings = settings;
    }
}
