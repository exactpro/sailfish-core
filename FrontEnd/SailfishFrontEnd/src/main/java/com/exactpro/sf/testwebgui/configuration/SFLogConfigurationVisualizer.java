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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Level;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.testwebgui.BeanUtil;

public class SFLogConfigurationVisualizer implements Serializable {
    private static final long serialVersionUID = 1553069784767606113L;
    private static final String LOGGER_PREFIX = "log4j.logger.";

    private List<LoggerAdapter> loggers;
    private List<AppenderAdapter> appenders;

    private List<String> levels;
    private String level = null;
    private TreeNode tree = null;

    private static final Logger logger = LoggerFactory.getLogger(SFLogConfigurationVisualizer.class);

    public SFLogConfigurationVisualizer() {
        this("");
    }

    public SFLogConfigurationVisualizer(String configFileContent) {
        loggers = new ArrayList<>();
        appenders = new ArrayList<>();

        levels = new ArrayList<>();
        for (Field f : Level.class.getDeclaredFields()) {
            if (f.getType().equals(Level.class)) {
                levels.add(f.getName());
            }

        }

        List<String> configLines = readConfigFile(configFileContent);
        try {
            parseLines(configLines);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    private List<String> readConfigFile(String configFileContent) {
        List<String> result = new ArrayList<>();
        for(String line : configFileContent.split("\n")) {
            result.add(line);
        }
        return result;
    }

    private void parseLines(List<String> configLines) throws ClassNotFoundException {

        for(String line : configLines) {
            if(line.contains(LOGGER_PREFIX) || line.contains("log4j.rootLogger")) {

                String loggerName;
                if(line.contains("log4j.rootLogger")) {
                    loggerName = "log4j.rootLogger";
                } else {
                    loggerName = line.substring(line.indexOf(LOGGER_PREFIX) + LOGGER_PREFIX.length(), line.indexOf("="));
                }

                if(!line.contains(",")) {
                    continue;
                }
                String loggerLevel = line.substring(line.indexOf("=") + 1, line.indexOf(",")).trim();
                LoggerAdapter logger = new LoggerAdapter();
                logger.setName(loggerName);
                logger.setLevel(loggerLevel);
                String[] _appenders = line.substring(line.indexOf(",", line.indexOf("="))).replace("\r","").split(",", -1);
                List<String> appenderList = new ArrayList<>();
                for(String s : _appenders) {
                    if(!s.trim().isEmpty()) {
                        appenderList.add(s.trim());
                    }
                }
                logger.setAppenders(appenderList);

                for(String appenderName : appenderList) {
                    Map<String, String> params = readAppenderParams(appenderName, configLines);
                    AppenderAdapter appender = new AppenderAdapter();
                    appender.setName(appenderName);
                    appender.setType(params.get("instance"));
                    params.remove("instance");
                    appender.setParams(params);
                    if(!isDublicates(appender.getName())) {
                        appenders.add(appender);
                    }
                }
                loggers.add(logger);
            }
        }
    }

    private boolean isDublicates(String name) {
        for(AppenderAdapter ap : appenders) {
            if(ap.getName().trim().equalsIgnoreCase(name.trim())) {
                return true;
            }
        }
        return false;
    }

    public LoggerAdapter getLoggerByName(String name) {

        for(LoggerAdapter log : loggers) {
            if(log.getName().equals(name)) {
                return log;
            }
        }
        return new LoggerAdapter();
    }

    private Map<String, String> readAppenderParams(String appenderName, List<String> configLines) throws ClassNotFoundException {
        Map<String, String> params = new HashMap<>();
        String type = "";
        for(String line : configLines) {
            line = line.replace("\r", "");
            int index = line.indexOf("log4j.appender." + appenderName + "=");
            if(index > -1) {
                type = line.substring(line.indexOf("=") + 1);
                params.put("instance", type);
            } else if (line.contains("log4j.appender." + appenderName + ".")) {
                String s = "log4j.appender." + appenderName + ".";
                String name = line.substring(line.indexOf(s) + s.length(), line.indexOf("="));
                String value = line.substring(line.indexOf("=") + 1);
                params.put(name, value);
            }
        }
        return params;
    }

    public AppenderAdapter getAppenderByName(String name) {
        for(AppenderAdapter appenderAdapter : appenders) {
            if(appenderAdapter.getName().equals(name)) {
                return appenderAdapter;
            }
        }
        return new AppenderAdapter();
    }

    public List<AppenderAdapter> getAppendersByLogger(LoggerAdapter logger) {
        List<AppenderAdapter> result = new ArrayList<>();

        for(String apName : logger.getAppenders()) {
            AppenderAdapter appenderAdapter = getAppenderByName(apName);
            result.add(appenderAdapter);
        }

        return result;
    }

    @Override
    public String toString() {
        String LS = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder();
        List<String> alreadyRenderesAppenders = new ArrayList<>();
        List<LoggerAdapter> result = new ArrayList<>(loggers);
        for(LoggerAdapter loggerAdapter : result) {

            String fullName = loggerAdapter.getName().contains("rootLogger") ? loggerAdapter.getName() : LOGGER_PREFIX + loggerAdapter.getName();
            sb.append(fullName).append("=").append(loggerAdapter.getLevel()).append(",").append(loggerAdapter.getAppendersList()).append(LS).append(LS);
            for(String appenderName : loggerAdapter.getAppenders()) {
                if(alreadyRenderesAppenders.contains(appenderName)) {
                    continue;
                }
                alreadyRenderesAppenders.add(appenderName);
                AppenderAdapter appenderAdapter = getAppenderByName(appenderName);
                sb.append("log4j.appender.").append(appenderAdapter.getName()).append("=").append(appenderAdapter.getType()).append(LS);
                for(Map.Entry<String, String> entry : appenderAdapter.getParams().entrySet()) {
                    sb.append("log4j.appender.").append(appenderAdapter.getName()).append(".").append(entry.getKey()).append("=").append(entry.getValue()).append(LS);
                }
            }
            sb.append(LS).append(LS);
        }
        return sb.toString();
    }

    public void applyToAllLoggers() {
        for(LoggerAdapter log : loggers) {
            log.setLevel(level);
        }
        BeanUtil.addInfoMessage("Success", "Level " + level + " applied for all loggers");
    }

    public List<LoggerAdapter> getLoggers() {
        return loggers;
    }

    public void setLoggers(List<LoggerAdapter> loggers) {
        this.loggers = loggers;
    }

    public List<AppenderAdapter> getAppenders() {
        return appenders;
    }

    public void setAppenders(List<AppenderAdapter> appenders) {
        this.appenders = appenders;
    }

    public List<String> getLevels() {
        return levels;
    }

    public void setLevels(List<String> levels) {
        this.levels = levels;
    }

    public String getLevel() {
        if(level == null) {
            level = loggers.get(0).getLevel();
        }

        for(LoggerAdapter logger : this.loggers) {
            if(!logger.getLevel().equals(level)) {
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

        for(LoggerAdapter logger : loggers) {
            LoggerNodeWrapper loggerWrapper = new LoggerNodeWrapper(logger);
            TreeNode loggerNode = new DefaultTreeNode("logger", loggerWrapper, root);

            for(String appenderName : logger.getAppenders()) {
                AppenderAdapter appenderAdapter = this.getAppenderByName(appenderName);
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
        if(this.tree == null) {
            this.tree = buildTree();
        }
        return tree;
    }

    public void setTree(TreeNode tree) {
        this.tree = tree;
    }
}
