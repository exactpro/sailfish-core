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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class LoggerNodeWrapper {

    private LoggerAdapter logger;
    private AppenderAdapter appender;
    private Pair<String, String> pairParam;

    private static final AppenderAdapter DEFAULT_APPENDER = new AppenderAdapter();
    private static final LoggerAdapter DEFAULT_LOGGER = new LoggerAdapter();
    private static final Pair<String, String> DEFAULT_PARAM = new ImmutablePair<String, String>("", "");

    public LoggerNodeWrapper(LoggerAdapter logger) {
        this.logger = logger;
        this.appender = DEFAULT_APPENDER;
        this.pairParam = DEFAULT_PARAM;
    }

    public LoggerNodeWrapper(AppenderAdapter appender) {
        this.appender = appender;
        this.logger = DEFAULT_LOGGER;
        this.pairParam = DEFAULT_PARAM;
    }

    public LoggerNodeWrapper(Pair<String, String> pair) {
        this.pairParam = pair;
        this.logger = DEFAULT_LOGGER;
        this.appender = DEFAULT_APPENDER;
    }

    public LoggerAdapter getLogger() {
        return logger;
    }

    public void setLogger(LoggerAdapter logger) {
        this.logger = logger;
    }

    public AppenderAdapter getAppender() {
        return appender;
    }

    public void setAppender(AppenderAdapter appender) {
        this.appender = appender;
    }

    public Pair<String, String> getPairParam() {
        return pairParam;
    }

    public void setPairParam(Pair<String, String> pairParam) {
        this.pairParam = pairParam;
    }
}
