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

import com.exactpro.sf.help.helpmarshaller.HelpEntityType;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class FieldJsonContainer extends HelpJsonContainer{
    private String javaType;

    public FieldJsonContainer(String name, String htmlPath, String icon, HelpEntityType type, String javaType) {
        super(name, htmlPath, icon, type);

        this.javaType = javaType;
    }

    public FieldJsonContainer(String name, String htmlPath, String icon, HelpEntityType type, String javaType, List<HelpJsonContainer> childNodes) {
        super(name, htmlPath, icon, type, childNodes);

        this.javaType = javaType;
    }

    public FieldJsonContainer() {
    }

    public String getJavaType() {
        return javaType;
    }

}

