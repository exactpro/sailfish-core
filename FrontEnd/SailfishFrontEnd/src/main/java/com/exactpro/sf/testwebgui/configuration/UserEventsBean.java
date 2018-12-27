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

package com.exactpro.sf.testwebgui.configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.testwebgui.BeanUtil;


@ManagedBean(name="userEventsBean")
@ViewScoped
public class UserEventsBean {

    private static Map styleMapping = new HashMap() {
        {
            put("DEBUG", "ue_table_priority_debug");
            put("INFO", "ue_table_priority_info");
            put("WARN", "ue_table_priority_warn");
            put("ERROR", "ue_table_priority_error");
        }
    };

    private static final DateTimeFormatter  LOGGERFORMAT = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd HH:mm:ss,SSS").toFormatter();

    private final static String LOG_SEPARATOR = "[$]";
    private final String[] properties = {"datetime", "priority", "message"};

    private List<Map<String, ?>> filteredLogs;
    private List<Map<String, ?>> logs;

    @PostConstruct
    public void readLogs() {
        ISFContext sfContext = BeanUtil.getSfContext();
        IWorkspaceDispatcher workspaceDispatcher = sfContext.getWorkspaceDispatcher();

        Function<String, Map<String, Object>> parseString = (s) -> {
            Map<String, Object> result = new HashMap<>();
            int counter = 0;
            for (String element : s.split(Pattern.quote(LOG_SEPARATOR))) {
                result.put(properties[counter++], element);
            }

            return result;
        };

        Function<Map<String, Object>, Map<String, Object>> process = (sourceMap) -> {
            String targetStyle = (String) styleMapping.getOrDefault(sourceMap.get("priority"), "");
            sourceMap.put("style", targetStyle);
            String datetime = (String) sourceMap.get("datetime");
            LocalDateTime logTime = LocalDateTime.parse(datetime, LOGGERFORMAT);
            sourceMap.put("datetime", logTime);
            return sourceMap;
        };

        try {
            File eventsLog = workspaceDispatcher.getFile(FolderType.LOGS, "user_events.log");
            logs = Files.lines(eventsLog.toPath()).map(parseString).map(process).collect(Collectors.toList());
        } catch (IOException e) {
            logs = Collections.emptyList();
        }
    }


    public List<Map<String, ?>> getPluginLoaderLog() {
        return logs;
    }

    public List<Map<String, ?>> getFilteredLogs() {
        return filteredLogs;
    }

    public void setFilteredLogs(List<Map<String, ?>> filteredLogs) {
        this.filteredLogs = filteredLogs;
    }

    public Converter getConverter() {
        return new Converter() {
            @Override
            public Object getAsObject(FacesContext context, UIComponent component, String value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getAsString(FacesContext context, UIComponent component, Object value) {
                LocalDateTime localDateTime = (LocalDateTime) value;
                return localDateTime.toLocalDate() + "\n" + localDateTime.toLocalTime();
            }
        };
    }
}
