/******************************************************************************
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.sf.util;

import java.io.File;
import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.jetbrains.annotations.NotNull;

public class LogUtils {
    public static final String LOG4J_PROPERTIES_FILE_NAME = "log4j2.properties";

    private LogUtils() {
    }

    public static void addAppender(@NotNull Logger logger, @NotNull Appender appender) {
        LoggerContext context = getContext();
        Configuration config = getConfiguration(context);
        appender.start();
        config.getLoggerConfig(logger.getName()).addAppender(appender, null, null);
        context.updateLoggers();
    }

    public static void addRootLoggerAppender(@NotNull Appender appender) {
        LoggerContext context = getContext();
        Configuration config = getConfiguration(context);
        appender.start();
        config.getRootLogger().addAppender(appender, null, null);
        context.updateLoggers();
    }

    private static LoggerContext getContext() {
        return (LoggerContext)LogManager.getContext(false);
    }

    public static void removeAppender(@NotNull Logger logger, @NotNull Appender appender) {
        LoggerContext context = getContext();
        Configuration config = getConfiguration(context);
        LoggerConfig loggerConfig = config.getLoggerConfig(logger.getName());
        appender.stop();
        loggerConfig.removeAppender(appender.getName());
        context.updateLoggers();
    }

    private static Configuration getConfiguration(@NotNull LoggerContext context) {
        return context.getConfiguration();
    }

    public static Configuration getConfiguration() {
        LoggerContext context = getContext();
        return context.getConfiguration();
    }

    public static void removeAllAppenders(@NotNull Logger logger) {
        LoggerContext context = getContext();
        Configuration config = getConfiguration(context);
        LoggerConfig loggerConfig = config.getLoggerConfig(logger.getName());

        loggerConfig.getAppenders().forEach((key, value) -> {
            value.stop();
            loggerConfig.removeAppender(value.getName());
        });
        context.updateLoggers();
    }

    public static void setConfigLocation(URI uri) {
        LoggerContext context = getContext();
        context.setConfigLocation(uri);
    }

    public static void setConfigLocation(@NotNull File file) {
        setConfigLocation(file.toURI());
    }

    public static void setConfigLocation(String path) {
        setConfigLocation(new File(path));
    }
}
