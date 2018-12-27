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

import org.apache.log4j.Level;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class LoggerAdapter implements Serializable {

    private String level;
    private String name;
    private List<String> appenders;

    public LoggerAdapter() {
        this.name = "";
        this.level = Level.INFO.toString();
        this.appenders = new ArrayList<String>();
    }

    public String getAppendersList() {
        StringBuilder sb = new StringBuilder();
        for(String appenderAdapter : appenders) {
            sb.append(appenderAdapter).append(",");
        }

        String result = sb.toString();
        result = result.substring(0, result.length()-1);
        return result;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        if(!level.equals("")) {
            this.level = level;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getAppenders() {
        return appenders;
    }

    public void setAppenders(List<String> appenders) {
        this.appenders = appenders;
    }
}
