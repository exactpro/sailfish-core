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
package com.exactpro.sf.services.fix;

import java.util.function.BiConsumer;

import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.IServiceMonitor;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.fix.converter.dirty.DirtyQFJIMessageConverter;

import quickfix.DataDictionaryProvider;
import quickfix.SessionSettings;

public class ApplicationContext {

    private final IServiceMonitor serviceMonitor;
    private final IServiceHandler serviceHandler;
    private final IServiceSettings serviceSettings;
    private final SessionSettings sessionSettings;
    private final MessageHelper messageHelper;
    private final DirtyQFJIMessageConverter converter;
    private final DataDictionaryProvider dictionaryProvider;
    private final BiConsumer<Boolean, String> problemConsumer;

    public ApplicationContext(IServiceMonitor serviceMonitor, IServiceHandler ServiceHandler, IServiceSettings serviceSettings, SessionSettings sessionSettings, MessageHelper messageHelper, DirtyQFJIMessageConverter converter,
            DataDictionaryProvider dictionaryProvider, BiConsumer<Boolean, String> problemConsumer) {
        this.serviceMonitor = serviceMonitor;
        this.serviceHandler = ServiceHandler;
        this.serviceSettings = serviceSettings;
        this.sessionSettings = sessionSettings;
        this.messageHelper = messageHelper;
        this.converter = converter;
        this.dictionaryProvider = dictionaryProvider;
        this.problemConsumer = problemConsumer;
    }

    public ApplicationContext(IServiceMonitor serviceMonitor, IServiceHandler serviceHandler, IServiceSettings serviceSettings, SessionSettings sessionSettings, MessageHelper messageHelper, DirtyQFJIMessageConverter converter,
            DataDictionaryProvider dictionaryProvider) {
        this(serviceMonitor, serviceHandler, serviceSettings, sessionSettings, messageHelper, converter, dictionaryProvider, null);
    }

    public IServiceSettings getServiceSettings() {
        return serviceSettings;
    }
    
    public SessionSettings getSessionSettings() {
        return sessionSettings;
    }

    public MessageHelper getMessageHelper() {
        return messageHelper;
    }

    public DirtyQFJIMessageConverter getConverter() {
        return converter;
    }

    public DataDictionaryProvider getDictionaryProvider() {
        return dictionaryProvider;
    }

    public IServiceHandler getServiceHandler() {
        return serviceHandler;
    }
    
    public IServiceMonitor getServiceMonitor() {
        return serviceMonitor;
    }

    public void connectionProblem(boolean isPresent, String reason) {
        if (problemConsumer != null) {
            problemConsumer.accept(isPresent, reason);
        }
    }
}
