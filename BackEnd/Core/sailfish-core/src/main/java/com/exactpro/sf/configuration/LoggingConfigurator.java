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
package com.exactpro.sf.configuration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jetbrains.annotations.NotNull;

import com.exactpro.sf.common.logging.DailyMaxRollingFileAppender;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class LoggingConfigurator implements ILoggingConfigurator {

    private static final String DATE_PATTERN = "'.'yyyy-MM-dd";

    private static final String LAYOUT_PATTERN = "%p %t %d{dd MMM yyyy HH:mm:ss,SSS} %c - %m%n";

    private static final int MAX_BACKUPS = 3;

    private static final Logger logger = Logger.getLogger(LoggingConfigurator.class);

    private static final String SERVICE_LOG_FOLDER = "services";

    private final Object lock = new Object();

    private final SetMultimap<ServiceName, String> serviceLoggers = HashMultimap.create();

    private final Map<ServiceName, DailyMaxRollingFileAppender> serviceAppenders = new HashMap<>();

    private DailyMaxRollingFileAppender mainAppender;

    private final IWorkspaceDispatcher wd;
    private final ILoggingConfiguration loggingConfiguration;

    public LoggingConfigurator(@NotNull IWorkspaceDispatcher wd, @NotNull ILoggingConfiguration loggingConfiguration) {
        this.wd = Objects.requireNonNull(wd, "Workspace Dispatcher can`t be null");
        this.loggingConfiguration = Objects.requireNonNull(loggingConfiguration, "Logging configuration can`t be null");
    }

    /* (non-Javadoc)
     * @see com.exactpro.sf.configuration.ILoggingConfigurator#createAppender(java.lang.String, com.exactpro.sf.common.services.ServiceName)
     */
    @Override
    public void createAppender(@NotNull ServiceName serviceName) {

        try {
            synchronized (lock) {

                if (!serviceAppenders.containsKey(serviceName)) {
                    if (loggingConfiguration.isIndividualAppendersEnabled()) {
                        getOrCreateServiceAppender(serviceName);
                    }
                } else if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Separate logger for service '%s' already created", serviceName));
                }
            }

        } catch (RuntimeException e) {
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("Failed to create appender for service: " + serviceName, e);
            }
        }
    }

    /* (non-Javadoc)
     * @see com.exactpro.sf.configuration.ILoggingConfigurator#destroyAppender(java.lang.String, com.exactpro.sf.common.services.ServiceName)
     */
    @Override
    public void destroyAppender(@NotNull ServiceName serviceName) {
        try {
            synchronized (lock) {

                DailyMaxRollingFileAppender serviceAppender = loggingConfiguration.isIndividualAppendersEnabled() ? serviceAppenders.remove(serviceName) : getOrCreateMainAppender();

                if (serviceAppender == null) {
                    if (logger.isInfoEnabled()) {
                        logger.info(String.format("Appender for service '%s' already destroyed", serviceName));
                    }
                    return;
                }

                serviceLoggers.removeAll(serviceName).forEach(loggerName -> {
                    LogManager.getLogger(loggerName).removeAppender(serviceAppender);
                });

                if (loggingConfiguration.isIndividualAppendersEnabled()) {
                    serviceAppender.close();
                } else if (serviceLoggers.isEmpty()) {
                    mainAppender.close();
                    mainAppender = null;
                }
            }
        } catch (RuntimeException e) {
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("Failed to destroy appender for service: " + serviceName, e);
            }
        }
    }

    @Override
    public void registerLogger(@NotNull Object obj, @NotNull ServiceName serviceName) {
        String loggerName = ILoggingConfigurator.getLoggerName(obj);

        Logger log = LogManager.exists(loggerName);

        if (log == null) {
            throw new EPSCommonException(String.format("Logger with name '%s' doesn`t exists", loggerName));
        }

        try {
            synchronized (lock) {
                DailyMaxRollingFileAppender appender;
                if (loggingConfiguration.isIndividualAppendersEnabled()) {
                    appender = getOrCreateServiceAppender(serviceName);
                } else {
                    appender = getOrCreateMainAppender();
                }

                log.addAppender(appender);
                serviceLoggers.put(serviceName, loggerName);
            }
        } catch (RuntimeException e) {
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error(String.format("Failed to register logger '%s' for service: %s", loggerName, serviceName), e);
            }
        }
    }

    @Override
    public void enableIndividualAppenders() {
        try {
            synchronized (lock) {
                if (!loggingConfiguration.isIndividualAppendersEnabled()) {

                    DailyMaxRollingFileAppender removingAppender = getOrCreateMainAppender();
                    serviceLoggers.forEach((serviceName, loggerName) -> {
                        DailyMaxRollingFileAppender serviceAppender = getOrCreateServiceAppender(serviceName);

                        Logger log = LogManager.getLogger(loggerName);

                        log.removeAppender(removingAppender);
                        log.addAppender(serviceAppender);
                    });

                    mainAppender.close();
                    mainAppender = null;
                    loggingConfiguration.setIndividualAppendersEnabled(true);
                }
            }
        } catch (RuntimeException e) {
            logger.error("Failed to enable individual appenders", e);
        }
    }

    @Override
    public void disableIndividualAppenders() {
        try {
            synchronized (lock) {
                if (loggingConfiguration.isIndividualAppendersEnabled()) {

                    DailyMaxRollingFileAppender addingAppender = getOrCreateMainAppender();

                    serviceLoggers.forEach((serviceName, loggerName) -> {
                        DailyMaxRollingFileAppender serviceAppender = serviceAppenders.get(serviceName);

                        Logger log = LogManager.getLogger(loggerName);
                        log.removeAppender(serviceAppender);
                        log.addAppender(addingAppender);
                    });

                    serviceAppenders.values().forEach(Appender::close);
                    serviceAppenders.clear();

                    loggingConfiguration.setIndividualAppendersEnabled(false);
                }
            }
        } catch (RuntimeException e) {
            logger.error("Failed to disable individual appenders", e);
        }
    }

    /* (non-Javadoc)
     * @see com.exactpro.sf.configuration.ILoggingConfigurator#getLogPath(com.exactpro.sf.common.services.ServiceName)
     */
    @Override
    public String getLogsPath(@NotNull ServiceName serviceName) {
        return toLogPath(serviceName, true);
    }

    @NotNull
    @Override
    public ILoggingConfiguration getConfiguration() {
        return loggingConfiguration;
    }

    private static String toLogPath(ServiceName serviceName, boolean folderOnly) {
        StringBuilder pathBuilder = new StringBuilder();
        String separator = File.separator;

        pathBuilder.append(SERVICE_LOG_FOLDER).append(separator)
                .append(serviceName.getEnvironment()).append(separator)
                .append(serviceName.getServiceName());
        if (!folderOnly) {
            pathBuilder.append(separator).append(serviceName);
        }

        return pathBuilder.toString();
    }

    private static DailyMaxRollingFileAppender createAppender(String filename) throws IOException {
        DailyMaxRollingFileAppender appender = new DailyMaxRollingFileAppender(new PatternLayout(LAYOUT_PATTERN), filename, DATE_PATTERN);
        appender.setBufferedIO(true);
        appender.setMaxBackupIndex(MAX_BACKUPS);
        return appender;
    }

    private DailyMaxRollingFileAppender getOrCreateMainAppender() {
        if (mainAppender == null) {
            try {
                File logFile = wd.getOrCreateFile(FolderType.LOGS, "services.log");
                mainAppender = createAppender(logFile.getCanonicalPath());
                mainAppender.setThreshold(Level.toLevel(loggingConfiguration.getIndividualAppendersThereshold(), Level.ALL));
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                throw new EPSCommonException("Can`t create file service.log for main appender.", e);
            }
        }
        return mainAppender;
    }

    private DailyMaxRollingFileAppender getOrCreateServiceAppender(ServiceName serviceName) {
        return serviceAppenders.computeIfAbsent(serviceName, key -> {
            String filename = toLogPath(key, true);
            try {
                File logFolder = wd.getOrCreateFile(FolderType.LOGS, filename + ".log");
                DailyMaxRollingFileAppender appender = createAppender(logFolder.getCanonicalPath());
                appender.setThreshold(Level.toLevel(loggingConfiguration.getIndividualAppendersThereshold(), Level.ALL));
                return appender;
            } catch (IOException e) {
                throw new EPSCommonException(String.format("Can`t create file %s for service %s", filename, key), e);
            }
        });
    }
}
