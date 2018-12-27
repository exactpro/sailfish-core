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

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.common.logging.DailyMaxRollingFileAppender;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;

public class LoggingConfigurator implements ILoggingConfigurator {

    private static final String DATE_PATTERN = "'.'yyyy-MM-dd";

    private static final String LAYOUT_PATTERN = "%p %t %d{dd MMM yyyy HH:mm:ss,SSS} %c - %m%n";

    private static final int MAX_BACKUPS = 3;

    private static final Logger logger = Logger.getLogger(LoggingConfigurator.class);

    private static final String SERVICE_LOG_FOLDER = "services";

    private final Object lock = new Object();

    private final Map<String, DailyMaxRollingFileAppender> appenders = new HashMap<>();

    private final Map<String, DailyMaxRollingFileAppender> loggers = new HashMap<>();

    private final Map<String, String> loggerAppenderMapping = new HashMap<>();

    private DailyMaxRollingFileAppender oneAppenderForLoggingAll;

    private final IWorkspaceDispatcher wd;
    private final ILoggingConfiguration loggingConfiguration;

    public LoggingConfigurator(IWorkspaceDispatcher wd, ILoggingConfiguration loggingConfiguration) {

        this.wd = wd;
        this.loggingConfiguration = loggingConfiguration;
    }

    /* (non-Javadoc)
     * @see com.exactpro.sf.configuration.ILoggingConfigurator#createIndividualAppender(java.lang.String, com.exactpro.sf.common.services.ServiceName)
     */
    @Override
    public void createIndividualAppender(String loggerName, ServiceName serviceName) {
        String fileName = toLogPath(serviceName, true);
        Logger log = LogManager.getLogger(loggerName);

        try {
            synchronized (lock) {

                if (!loggers.containsKey(loggerName)) {

                    if (loggingConfiguration.isIndividualAppendersEnabled()) {
                        DailyMaxRollingFileAppender appender = null;
                        if (!appenders.containsKey(fileName)) {
                            File logFolder = wd.getOrCreateFile(FolderType.LOGS, fileName + ".log");
                            appender = createAppender(logFolder.getCanonicalPath());
                            appenders.put(fileName, appender);
                        } else {
                            appender = appenders.get(fileName);
                        }

                        appender.setThreshold(Level.toLevel(loggingConfiguration.getIndividualAppendersThereshold(), Level.ALL));
                        log.addAppender(appender);
                        loggers.put(loggerName, appender);
                    } else {
                        if (oneAppenderForLoggingAll == null) {
                            try {
                                File logFolder = wd.getOrCreateFile(FolderType.LOGS, "services.log");
                                oneAppenderForLoggingAll = createAppender(logFolder.getCanonicalPath());
                            } catch (Exception e) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                        oneAppenderForLoggingAll.setThreshold(Level.toLevel(loggingConfiguration.getIndividualAppendersThereshold(), Level.ALL));
                        log.addAppender(oneAppenderForLoggingAll);
                    }
                    if (!loggerAppenderMapping.containsKey(loggerName)) {
                        loggerAppenderMapping.put(loggerName, fileName);
                    }


                } else {
                    logger.debug("Separate logger for service " + fileName + " already created");
                }

            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    /* (non-Javadoc)
     * @see com.exactpro.sf.configuration.ILoggingConfigurator#destroyIndividualAppender(java.lang.String, com.exactpro.sf.common.services.ServiceName)
     */
    @Override
    public void destroyIndividualAppender(String loggerName, ServiceName serviceName) {
        String fileName = toLogPath(serviceName, true);
        Logger log = LogManager.getLogger(loggerName);

        try {
            synchronized (lock) {

                if (loggers.containsKey(loggerName)) {
                    log.removeAppender(appenders.get(fileName));
                    loggers.remove(loggerName);
                }

                if (appenders.containsKey(fileName)) {

                    Appender appender = appenders.get(fileName);
                    if (!loggers.values().contains(appender)) {
                        appenders.remove(fileName);
                        appender.close();
                    }

                }

                if (loggerAppenderMapping.containsKey(loggerName)) {
                    loggerAppenderMapping.remove(loggerName);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    @Override
    public void disableIndividualAppenders() {
        synchronized (lock) {
            if (loggingConfiguration.isIndividualAppendersEnabled()) {
                if (oneAppenderForLoggingAll == null) {
                    try {
                        File logFolder = SFLocalContext.getDefault().getWorkspaceDispatcher().createFolder(FolderType.LOGS, "");
                        oneAppenderForLoggingAll = createAppender(logFolder.getCanonicalPath() + File.separator + "services.log");
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                for (String logName : loggerAppenderMapping.keySet()) {
                    Logger log = LogManager.getLogger(logName);
                    String fileName = loggerAppenderMapping.get(logName);
                    Appender appender = appenders.get(fileName);
                    if (appender != null) {
                        log.removeAppender(appender);
                        appender.close();
                    }
                    appenders.remove(fileName);
                    loggers.remove(logName);
                    if (oneAppenderForLoggingAll != null) {
                        log.addAppender(oneAppenderForLoggingAll);
                    }
                }
                loggingConfiguration.setIndividualAppendersEnabled(false);
            } else {
                logger.info("Individual appenders are disabled already");
            }
        }
    }

    @Override
    public void enableIndividualAppenders() {
        try {
            File logFolder = SFLocalContext.getDefault().getWorkspaceDispatcher().createFolder(FolderType.LOGS, "");

            synchronized (lock) {
                if (!loggingConfiguration.isIndividualAppendersEnabled()) {
                    for (String logName : loggerAppenderMapping.keySet()) {
                        Logger log = LogManager.getLogger(logName);
                        log.removeAppender(oneAppenderForLoggingAll);
                        String fileName = loggerAppenderMapping.get(logName);
                        DailyMaxRollingFileAppender appender = null;
                        if (!appenders.containsKey(fileName)) {
                            appender = createAppender(logFolder.getCanonicalPath() + File.separator + fileName + ".log");
                            appenders.put(fileName, appender);
                            loggers.put(logName, appender);
                        } else {
                            appender = appenders.get(fileName);
                        }
                        log.addAppender(appender);
                    }
                    if (oneAppenderForLoggingAll != null) {
                        oneAppenderForLoggingAll.close();
                        oneAppenderForLoggingAll = null;
                    }
                    loggingConfiguration.setIndividualAppendersEnabled(true);
                } else {
                    logger.info("Individual appenders are enabled already");
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /* (non-Javadoc)
     * @see com.exactpro.sf.configuration.ILoggingConfigurator#getLogPath(com.exactpro.sf.common.services.ServiceName)
     */
    @Override
    public String getLogsPath(ServiceName serviceName) {
        return toLogPath(serviceName, true);
    }

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
            pathBuilder.append(separator).append(serviceName.toString());
        }

        return pathBuilder.toString();
    }

    private static DailyMaxRollingFileAppender createAppender(String filename) throws IOException {
        DailyMaxRollingFileAppender appender = new DailyMaxRollingFileAppender(new PatternLayout(LAYOUT_PATTERN), filename, DATE_PATTERN);
        appender.setBufferedIO(true);
        appender.setMaxBackupIndex(MAX_BACKUPS);
        return appender;
    }
}
