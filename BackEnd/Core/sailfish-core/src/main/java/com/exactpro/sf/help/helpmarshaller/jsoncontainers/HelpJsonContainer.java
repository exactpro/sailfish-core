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
package com.exactpro.sf.help.helpmarshaller.jsoncontainers;

import java.util.List;

import com.exactpro.sf.help.helpmarshaller.HelpEntityName;
import com.exactpro.sf.help.helpmarshaller.HelpEntityType;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class HelpJsonContainer {

    public static final String PLUGIN_ICON = "ui-icon-suitcase";
    public static final String ACTION_ICON = "ui-icon-document";
    public static final String DICTIONARY_ICON = "ui-icon-script";
    public static final String UTIL_ICON = "ui-icon-document-b";
    public static final String FIELD_ICON = "ui-icon-note";
    public static final String FIELD_STRUCTURE_ICON = "ui-icon-note";
    public static final String MESSAGE_ICON = "ui-icon-mail-closed";
    public static final String MESSAGE_STRUCTURE_ICON = "ui-icon-contact";
    public static final String METHOD_ICON = "ui-icon-gear";
    public static final String LANGUAGE_ICON = "ui-icon-bookmark";
    public static final String PROVIDER_ICON = "ui-icon-disk";
    public static final String SERVICE_ICON = "ui-icon-calculator";
    public static final String VALIDATOR_ICON = "ui-icon-flag";
    public static final String PREPROCESSOR_ICON = "ui-icon-lightbulb";

    private String filePath;

    private String icon;

    private String name;

    private HelpEntityType type;

    private List<HelpJsonContainer> childNodes;

    public HelpJsonContainer(String name, String filePath, String icon, HelpEntityType type, List<HelpJsonContainer> childNodes) {
        this.filePath = filePath;
        this.childNodes = childNodes;
        this.name = name;
        this.icon = icon;
        this.type = type;

    }

    public HelpJsonContainer(HelpEntityName name, String filePath, String icon, HelpEntityType type, List<HelpJsonContainer> childNodes) {
        this.filePath = filePath;
        this.childNodes = childNodes;
        this.name = name.getValue();
        this.icon = icon;
        this.type = type;

    }

    public HelpJsonContainer(String name, String filePath, String icon, HelpEntityType type) {
        this.filePath = filePath;
        this.name = name;
        this.icon = icon;
        this.type = type;

        this.childNodes = null;
    }



    public HelpJsonContainer(String htmlPath, List<HelpJsonContainer> childNodes) {
        this.filePath = htmlPath;
        this.childNodes = childNodes;

        this.name = null;
        this.icon = null;
        this.type = null;
    }

    public HelpJsonContainer() {
    }

    public String getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }

    public List<HelpJsonContainer> getChildNodes() {
        return childNodes;
    }

    public void addChild(HelpJsonContainer child) {
        this.childNodes.add(child);
    }

    public String getFilePath() {
        return filePath;
    }


    public HelpEntityType getType() {
        return type;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }


    public void setChildNodes(List<HelpJsonContainer> childNodes) {
        this.childNodes = childNodes;
    }

}