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

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.testwebgui.BeanUtil;
import com.google.common.collect.Iterables;

public class SFLogConfigurationVisualizer implements Serializable {
    private static final long serialVersionUID = 1553069784767606113L;
    private static final String LOGGER_PREFIX = "logger";
    private static final String ROOT_LOGGER_PREFIX = "rootLogger";
    private static final String APPENDER_PREFIX = "appender";
    public static final String PACKAGES_PREFIX = "packages";
    public static final String PROPERTY_PREFIX = "property";
    public static final String REF_GROUP = "ref";
    public static final String TOKEN_GROUP = "token";
    public static final String APPENDER_TOKEN_NAME = "name";
    public static final String APPENDER_TOKEN_TYPE = "type";
    public static final String LOGGER_TOKEN_NAME = "name";
    public static final String LOGGER_TOKEN_LEVEL = "level";
    public static final String LOGGER_TOKEN_APPENDER_REFS = "appenderRefs";
    public static final String PATH_SEPARATOR = ".";
    public static final String KEY_VALUE_SEPARATOR = "=";
    public static final String LOGGER_TOKEN_APPENDER_REF = "appenderRef";
    public static final String LOGGER_TOKEN_POSTFIX_REF = "ref";
    public static final String GLOBAL_LOGGERS_LINKS_PREFIX = "loggers=";
    public static final String GLOBAL_APPENDERS_LINKS_PREFIX = "appenders=";
    private static final Pattern VALUE_SEPARATOR_PATTERN = Pattern.compile("\\s*,\\s*");
    static final String DEFAULT_LEVEL = Level.INFO.toString();

    private final Map<String, LoggerAdapter> loggers;
    private final Map<String, AppenderAdapter> appenders;
    private final List<String> rootParameters;


    private List<String> levels;
    private String level;
    private TreeNode tree;

    private static final Pattern ROOT_LOGGER_PATTERN = Pattern.compile("rootLogger.(?<" + TOKEN_GROUP + ">[^=.]+)");
    private static final Pattern LOGGER_PATTERN = Pattern.compile("logger.(?<" + REF_GROUP + ">[^.]+).(?<" + TOKEN_GROUP + ">[^=]+)");
    private static final Pattern APPENDER_PATTERN = Pattern.compile("appender.(?<" + REF_GROUP + ">[^.]+).(?<" + TOKEN_GROUP + ">[^=]+)");

    private static final Logger logger = LoggerFactory.getLogger(SFLogConfigurationVisualizer.class);
    public static final String LINE_SEPARATOR = System.lineSeparator();

    public SFLogConfigurationVisualizer() {
        this("");
    }

    public SFLogConfigurationVisualizer(String configFileContent) {
        loggers = new LinkedHashMap<>();
        appenders = new LinkedHashMap<>();
        rootParameters = new ArrayList<>();
        levels = new ArrayList<>();
        for (Field f : Level.class.getDeclaredFields()) {
            if (f.getType().equals(Level.class)) {
                levels.add(f.getName());
            }

        }

        List<String> configLines = readConfigFile(configFileContent);
        parseLines(configLines);
    }


    private static List<String> readConfigFile(String configFileContent) {
        List<String> result = new ArrayList<>();
        for(String line : configFileContent.split("\n")) {
            result.add(line.trim());
        }
        return result;
    }

    private void parseLines(@NotNull List<String> configLines) {
        for (String line : configLines) {
            if (parseRootParameter(line)) {
                continue;
            }
            if (line.contains("=")) {
                String[] kv = line.split("=", 2);
                String key = kv[0].trim();
                String value = kv[1].trim();

                if (parseRootLogger(key, value)) {
                    continue;
                }
                if (parseLogger(key, value)) {
                    continue;
                }
                if (parseAppender(key, value)) {
                    continue;
                }

            }
        }
    }

    private boolean parseRootParameter(@NotNull String line) {
        if (line.startsWith(PACKAGES_PREFIX) || line.startsWith(PROPERTY_PREFIX)) {
            rootParameters.add(line);
            return true;
        }
        return false;
    }

    private boolean parseAppender(String key, String value) {
        Matcher matcher = APPENDER_PATTERN.matcher(key);
        if (matcher.find()) {
            String ref = matcher.group(REF_GROUP);
            String token = matcher.group(TOKEN_GROUP);
            AppenderAdapter appender;
            if (appenders.containsKey(ref)) {
                appender = appenders.get(ref);
            } else {
                appender = new AppenderAdapter();
                appenders.put(ref, appender);
            }
            switch (token) {
            case APPENDER_TOKEN_NAME:
                appender.setName(value);
                break;
            case APPENDER_TOKEN_TYPE:
                appender.setType(value);
                break;
            default:
                appender.setParam(token, value);
                break;
            }
            return true;
        }
        return false;
    }

    private boolean parseLogger(String key, String value) {
        Matcher matcher = LOGGER_PATTERN.matcher(key);
        if (matcher.find()) {
            String ref = matcher.group(REF_GROUP);
            String token = matcher.group(TOKEN_GROUP);
            LoggerAdapter logger;
            if (loggers.containsKey(ref)) {
                logger = loggers.get(ref);
            } else {
                logger = new LoggerAdapter();
                loggers.put(ref, logger);
            }
            switch (token) {
            case LOGGER_TOKEN_NAME:
                logger.setName(value);
                break;
            case LOGGER_TOKEN_LEVEL:
                logger.setLevel(value.toUpperCase());
                break;
            case LOGGER_TOKEN_APPENDER_REFS:
                logger.setAppenders(getAppendersList(value));
                break;
            }
            return true;
        }
        return false;
    }

    private boolean parseRootLogger(String key, String value) {
        Matcher matcher = ROOT_LOGGER_PATTERN.matcher(key);
        if (matcher.find()) {
            String token = matcher.group(TOKEN_GROUP);
            LoggerAdapter logger;
            if (loggers.containsKey(ROOT_LOGGER_PREFIX)) {
                logger = loggers.get(ROOT_LOGGER_PREFIX);
            } else {
                logger = new LoggerAdapter();
                loggers.put(ROOT_LOGGER_PREFIX, logger);
            }
            logger.setName(ROOT_LOGGER_PREFIX);
            switch (token) {
            case LOGGER_TOKEN_LEVEL:
                logger.setLevel(value.toUpperCase());
                break;
            case LOGGER_TOKEN_APPENDER_REFS:
                logger.setAppenders(getAppendersList(value));
                break;
            }
            return true;
        }
        return false;
    }

    @NotNull
    private static List<String> getAppendersList(@NotNull String value) {
        return Arrays.asList(VALUE_SEPARATOR_PATTERN.split(value.trim()));
    }

    public AppenderAdapter getAppenderByName(String name) {
        for (AppenderAdapter appenderAdapter : appenders.values()) {
            if (appenderAdapter.getName().equals(name)) {
                return appenderAdapter;
            }
        }
        return new AppenderAdapter();
    }

    private String getAppenderRefByName(String name) {
        for (Entry<String, AppenderAdapter> entry : appenders.entrySet()) {
            AppenderAdapter appender = entry.getValue();
            if (name.equals(appender.getName())) {
                return entry.getKey();
            }
        }
        return "";
    }

    @Override
    public String toString() {
        //print root params
        StringBuilder sb = new StringBuilder();
        for (String param : rootParameters) {
            sb.append(param);
            sb.append(LINE_SEPARATOR);
        }
        //print loggers
        printLoggers(sb);

        //print appenders
        printAppenders(sb);
        return sb.toString();

    }

    private void printAppenders(StringBuilder sb) {
        StringBuilder rootAppendersLinksBuilder = new StringBuilder();
        rootAppendersLinksBuilder.append(GLOBAL_APPENDERS_LINKS_PREFIX);
        String prefix = "";
        for (Entry<String, AppenderAdapter> entry : appenders.entrySet()) {
            String reference = entry.getKey();
            AppenderAdapter appender = entry.getValue();
            //name
            printAppenderName(sb, reference, appender);
            //type
            printAppenderType(sb, reference, appender);
            //params
            printAppenderParams(sb, reference, appender);

            sb.append(LINE_SEPARATOR);

            rootAppendersLinksBuilder.append(prefix);
            rootAppendersLinksBuilder.append(reference);
            prefix = ", ";
        }
        sb.append(rootAppendersLinksBuilder);
    }

    private void printLoggers(StringBuilder sb) {
        StringBuilder rootLoggersLinksBuilder = new StringBuilder();
        rootLoggersLinksBuilder.append(GLOBAL_LOGGERS_LINKS_PREFIX);
        String prefix = "";
        for (Entry<String, LoggerAdapter> entry : loggers.entrySet()) {
            String reference = entry.getKey();
            LoggerAdapter logger = entry.getValue();
            //name
            printLoggerName(sb, logger, reference);
            //level
            printLoggerLevel(sb, logger, reference);
            //appenders refs
            printLoggerAppendersRefs(sb, logger, reference);
            //appenders
            printLogAppenders(sb, logger, reference);
            sb.append(LINE_SEPARATOR);

            if (!ROOT_LOGGER_PREFIX.equals(reference)) {
                rootLoggersLinksBuilder.append(prefix);
                rootLoggersLinksBuilder.append(reference);
                prefix = ", ";
            }
        }
        sb.append(rootLoggersLinksBuilder);
        sb.append(LINE_SEPARATOR);
    }

    private static void printAppenderParams(StringBuilder sb, String reference, @NotNull AppenderAdapter appender) {
        appender.getParams().forEach((key, value) -> {
            printAppenderPrefix(sb, reference);
            sb.append(key);
            sb.append(KEY_VALUE_SEPARATOR);
            sb.append(value);
            sb.append(LINE_SEPARATOR);
        });
    }

    private static void printAppenderType(StringBuilder sb, String reference, @NotNull AppenderAdapter appender) {
        printAppenderPrefix(sb, reference);
        sb.append(APPENDER_TOKEN_TYPE);
        sb.append(KEY_VALUE_SEPARATOR);
        sb.append(appender.getType());
        sb.append(LINE_SEPARATOR);
    }

    private static void printAppenderName(StringBuilder sb, String reference, @NotNull AppenderAdapter appender) {
        printAppenderPrefix(sb, reference);
        sb.append(APPENDER_TOKEN_NAME);
        sb.append(KEY_VALUE_SEPARATOR);
        sb.append(appender.getName());
        sb.append(LINE_SEPARATOR);
    }

    private static void printAppenderPrefix(@NotNull StringBuilder sb, String reference) {
        sb.append(APPENDER_PREFIX);
        sb.append(PATH_SEPARATOR);
        sb.append(reference);
        sb.append(PATH_SEPARATOR);
    }

    private void printLogAppenders(StringBuilder sb, @NotNull LoggerAdapter logger, String reference) {
        for (String appenderName : logger.getAppenders()) {
            printLoggerPrefix(sb, reference);
            sb.append(PATH_SEPARATOR);
            sb.append(LOGGER_TOKEN_APPENDER_REF);
            sb.append(PATH_SEPARATOR);
            sb.append(getAppenderRefByName(appenderName));
            sb.append(PATH_SEPARATOR);
            sb.append(LOGGER_TOKEN_POSTFIX_REF);
            sb.append(KEY_VALUE_SEPARATOR);
            sb.append(appenderName);
            sb.append(LINE_SEPARATOR);
        }
    }

    private static void printLoggerPrefix(StringBuilder sb, String reference) {
        if (ROOT_LOGGER_PREFIX.equals(reference)) {
            sb.append(ROOT_LOGGER_PREFIX);
        } else {
            sb.append(LOGGER_PREFIX);
            sb.append(PATH_SEPARATOR);
            sb.append(reference);
        }
    }

    private static void printLoggerAppendersRefs(StringBuilder sb, @NotNull LoggerAdapter logger, String reference) {
        printLoggerPrefix(sb, reference);
        sb.append(PATH_SEPARATOR);
        sb.append(LOGGER_TOKEN_APPENDER_REFS);
        sb.append(KEY_VALUE_SEPARATOR);
        String prefix = "";
        for (String appenderName : logger.getAppenders()) {
            sb.append(prefix);
            sb.append(appenderName);
            prefix = ", ";
        }
        sb.append(LINE_SEPARATOR);
    }

    private static void printLoggerLevel(StringBuilder sb, @NotNull LoggerAdapter logger, String reference) {
        printLoggerPrefix(sb, reference);
        sb.append(PATH_SEPARATOR);
        sb.append(LOGGER_TOKEN_LEVEL);
        sb.append(KEY_VALUE_SEPARATOR);
        sb.append(logger.getLevel().toLowerCase());
        sb.append(LINE_SEPARATOR);
    }

    private static void printLoggerName(StringBuilder sb, LoggerAdapter logger, String reference) {
        if (ROOT_LOGGER_PREFIX.equals(reference)) {
            return;
        }
        sb.append(LOGGER_PREFIX);
        sb.append(PATH_SEPARATOR);
        sb.append(reference);
        sb.append(PATH_SEPARATOR);
        sb.append(LOGGER_TOKEN_NAME);
        sb.append(KEY_VALUE_SEPARATOR);
        sb.append(logger.getName());
        sb.append(LINE_SEPARATOR);
    }

    public void applyToAllLoggers() {
        for(LoggerAdapter log : loggers.values()) {
            log.setLevel(level);
        }
        BeanUtil.addInfoMessage("Success", "Level " + level + " applied for all loggers");
    }



    public List<String> getLevels() {
        return levels;
    }

    public void setLevels(List<String> levels) {
        this.levels = levels;
    }

    public String getLevel() {
        if (level == null) {
           LoggerAdapter loggerAdapter = Iterables.getFirst(loggers.values(), null);
           level = loggerAdapter == null ? DEFAULT_LEVEL : loggerAdapter.getLevel();
        }
        for (LoggerAdapter logger : loggers.values()) {
            if (!logger.getLevel().equals(level)) {
                return " -- ";
            }
        }
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public TreeNode buildTree() {
        TreeNode root = new DefaultTreeNode("root", null);
        logger.debug("Invoked building tree");

        for(LoggerAdapter logger : loggers.values()) {
            LoggerNodeWrapper loggerWrapper = new LoggerNodeWrapper(logger);
            TreeNode loggerNode = new DefaultTreeNode("logger", loggerWrapper, root);

            for(String appenderName : logger.getAppenders()) {
                AppenderAdapter appenderAdapter = getAppenderByName(appenderName);
                LoggerNodeWrapper appenderWrapper = new LoggerNodeWrapper(appenderAdapter);
                TreeNode appenderNode = new DefaultTreeNode("appender", appenderWrapper, loggerNode);

                for(Pair<String, String> param : appenderAdapter.getPairs()) {
                    LoggerNodeWrapper paramWrapper = new LoggerNodeWrapper(param);
                    new DefaultTreeNode("param", paramWrapper, appenderNode);
                }
            }
        }

        return root;
    }

    public TreeNode getTree() {
        if(tree == null) {
            this.tree = buildTree();
        }
        return tree;
    }

    public void setTree(TreeNode tree) {
        this.tree = tree;
    }
}
