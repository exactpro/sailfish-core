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

import static com.exactpro.sf.util.LogUtils.addAppender;
import static com.exactpro.sf.util.LogUtils.removeAllAppenders;
import static com.exactpro.sf.util.LogUtils.removeAppender;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.action.Action;
import org.apache.logging.log4j.core.appender.rolling.action.DeleteAction;
import org.apache.logging.log4j.core.appender.rolling.action.IfFileName;
import org.apache.logging.log4j.core.appender.rolling.action.PathCondition;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.jetbrains.annotations.NotNull;

import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.util.LogUtils;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class LoggingConfigurator implements ILoggingConfigurator {

    private static final String DATE_PATTERN =  ".%d{MM-dd-yyyy}";

    private static final String LAYOUT_PATTERN = "%p %t %d{dd MMM yyyy HH:mm:ss,SSS} %c - %m%n";

    private static final Logger logger = LogManager.getLogger(LoggingConfigurator.class);

    private static final String SERVICE_LOG_FOLDER = "services";
    private static final String OLD_FILE_NAME_ENDING = ".*";

    private final Object lock = new Object();

    private final SetMultimap<ServiceName, String> serviceLoggers = HashMultimap.create();

    private final Map<ServiceName, Appender> serviceAppenders = new HashMap<>();

    private Appender mainAppender;

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
            if (logger.isEnabled(Level.ERROR)) {
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

                Appender serviceAppender = loggingConfiguration.isIndividualAppendersEnabled() ? serviceAppenders.remove(serviceName) : getOrCreateMainAppender();

                if (serviceAppender == null) {
                    if (logger.isInfoEnabled()) {
                        logger.info(String.format("Appender for service '%s' already destroyed", serviceName));
                    }
                    return;
                }
                serviceLoggers.removeAll(serviceName).forEach(loggerName -> {
                    removeAllAppenders(LogManager.getLogger(loggerName));
                });

                if (loggingConfiguration.isIndividualAppendersEnabled()) {
                    serviceAppender.stop();
                } else if (serviceLoggers.isEmpty()) {
                    mainAppender.stop();
                    mainAppender = null;
                }
            }
        } catch (RuntimeException e) {
            if (logger.isEnabled(Level.ERROR)) {
                logger.error("Failed to destroy appender for service: " + serviceName, e);
            }
        }
    }

    @Override
    public void registerLogger(@NotNull Object obj, @NotNull ServiceName serviceName) {
        String loggerName = ILoggingConfigurator.getLoggerName(obj);

        Logger log = LogManager.getLogger(loggerName);

        if (log == null) {
            throw new EPSCommonException(String.format("Logger with name '%s' doesn`t exists", loggerName));
        }

        try {
            synchronized (lock) {
                Appender appender;
                if (loggingConfiguration.isIndividualAppendersEnabled()) {
                    appender = getOrCreateServiceAppender(serviceName);
                } else {
                    appender = getOrCreateMainAppender();
                }
                addAppender(log, appender);

                serviceLoggers.put(serviceName, loggerName);
            }
        } catch (RuntimeException e) {
            if (logger.isEnabled(Level.ERROR)) {
                logger.error(String.format("Failed to register logger '%s' for service: %s", loggerName, serviceName), e);
            }
        }
    }

    @Override
    public void enableIndividualAppenders() {
        try {
            synchronized (lock) {
                if (!loggingConfiguration.isIndividualAppendersEnabled()) {

                    Appender removingAppender = getOrCreateMainAppender();
                    serviceLoggers.forEach((serviceName, loggerName) -> {
                        Appender serviceAppender = getOrCreateServiceAppender(serviceName);
                        Logger log = LogManager.getLogger(loggerName);
                        removeAppender(log, removingAppender);
                        addAppender(log, serviceAppender);
                    });

                    mainAppender.stop();
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

                    Appender addingAppender = getOrCreateMainAppender();

                    serviceLoggers.forEach((serviceName, loggerName) -> {
                        Appender serviceAppender = serviceAppenders.get(serviceName);
                        Logger log = LogManager.getLogger(loggerName);
                        removeAppender(log, serviceAppender);
                        addAppender(log, addingAppender);
                    });

                    serviceAppenders.values().forEach(Appender::stop);
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

    private Appender createAppender(String filename) {

        PatternLayout layout = PatternLayout.newBuilder()
                .withPattern(LAYOUT_PATTERN)
                .build();

        TriggeringPolicy policy = TimeBasedTriggeringPolicy.newBuilder()
                .withInterval(1)
                .build();

        IfFileName ifFileName = IfFileName.createNameCondition(FilenameUtils.getName(filename) + OLD_FILE_NAME_ENDING, null);

        DeleteAction deleteAction = getDeleteAction(filename, ifFileName);

        RolloverStrategy strategy = DefaultRolloverStrategy.newBuilder()
                .withCustomActions(new Action[] { deleteAction })
                .build();

        String filePattern = getBackupFilePattern(filename);

        RollingFileAppender appender = RollingFileAppender.newBuilder()
                .setName("Appender_" + filename)
                .withFileName(filename)
                .withFilePattern(filePattern)
                .setFilter(ThresholdFilter.createFilter(Level.toLevel(loggingConfiguration.getIndividualAppendersThereshold(), Level.ALL), Result.ACCEPT, Result.DENY))
                .setLayout(layout)
                .withPolicy(policy)
                .withStrategy(strategy)
                .build();
        return appender;
    }

    @NotNull
    private static DeleteAction getDeleteAction(String filename, IfFileName ifFileName) {
        return DeleteAction.createDeleteAction(FilenameUtils.getFullPathNoEndSeparator(filename),
                false, // whether to follow symbolic links. For us - false
                1, // the maximum number of levels of directories for action. For us - 1
                false, // test mode for delete files. For us - false
                null, // PathSorter, if null, the default sorter will be used
                new PathCondition[] {  ifFileName },
                null, // Not used, used PathCondition
                LogUtils.getConfiguration()); // default configuration is used
    }

    @NotNull
    private static String getBackupFilePattern(String filename) {
        return filename + DATE_PATTERN;
    }

    private Appender getOrCreateMainAppender() {
        if (mainAppender == null) {
            try {
                File logFile = wd.getOrCreateFile(FolderType.LOGS, "services.log");
                mainAppender = createAppender(logFile.getCanonicalPath());
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                throw new EPSCommonException("Can`t create file service.log for main appender.", e);
            }
        }
        return mainAppender;
    }

    private Appender getOrCreateServiceAppender(ServiceName serviceName) {
        return serviceAppenders.computeIfAbsent(serviceName, key -> {
            String filename = toLogPath(key, true);
            try {
                File logFolder = wd.getOrCreateFile(FolderType.LOGS, filename + ".log");
                Appender appender = createAppender(logFolder.getCanonicalPath());
                return appender;
            } catch (IOException e) {
                throw new EPSCommonException(String.format("Can`t create file %s for service %s", filename, key), e);
            }
        });
    }
}
