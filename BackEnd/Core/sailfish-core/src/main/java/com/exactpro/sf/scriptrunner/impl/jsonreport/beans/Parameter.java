/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 ******************************************************************************/

package com.exactpro.sf.scriptrunner.impl.jsonreport.beans;

import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.comparison.Formatter;
import com.exactpro.sf.scriptrunner.ReportEntity;
import com.google.common.collect.Iterables;
import org.apache.commons.lang3.ClassUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Parameter {
    private String name;
    private String value;
    private String type;
    private List<Parameter> subParameters;
    private MsgMetaData msgMetadata;

    public Parameter() {

    }

    public Parameter(ReportEntity e) {
        this.name = e.getName();
        this.value = Formatter.formatForHtml(e.getValue(), true);
        this.subParameters = e.getFields().stream().map(Parameter::new).collect(Collectors.toList());
        if (e.getValue() instanceof IMessage) {
            msgMetadata = ((IMessage) e.getValue()).getMetaData();
        }
        this.type = getClassName(e.getValue());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<Parameter> getSubParameters() {
        return subParameters;
    }

    public void setSubParameters(List<Parameter> subParameters) {
        this.subParameters = subParameters;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    private String getClassName(Object o) {

        String possibleType = ClassUtils.getSimpleName(o, null);

        if(o instanceof IFilter) {
            IFilter filter = (IFilter) o;

            if (filter.hasValue()) {
                possibleType = ClassUtils.getSimpleName(filter.getValue(), null);
            }
        }

        if (o instanceof IMessage) {
            possibleType = IMessage.class.getSimpleName();
        }

        if (o instanceof List) {
            List<?> list = (List<?>) o;
            possibleType = List.class.getSimpleName() + "<" + getClassName(Iterables.get(list, 0, null)) + ">";
        }

        return possibleType;
    }

    public MsgMetaData getMsgMetadata() {
        return msgMetadata;
    }
}
