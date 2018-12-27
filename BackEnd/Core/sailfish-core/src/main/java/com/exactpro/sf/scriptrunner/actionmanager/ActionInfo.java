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
package com.exactpro.sf.scriptrunner.actionmanager;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.exactpro.sf.scriptrunner.AbstractInfo;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.exactpro.sf.aml.CommonColumn;
import com.exactpro.sf.aml.CustomColumn;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIRule;
import com.exactpro.sf.configuration.suri.SailfishURIUtils;
import com.google.common.collect.ImmutableMap;

@SuppressWarnings("serial")
public class ActionInfo extends AbstractInfo implements Cloneable, Serializable {
    private Set<SailfishURI> compatibleLanguageURIs = new HashSet<>();
    private ActionRequirements requirements;
    private Class<?> messageType;
    private Annotation[] annotations;
    private Map<String, CustomColumn> customColumns = Collections.emptyMap();
    private Map<String, CommonColumn> commonColumns = Collections.emptyMap();
    private String[] allowedMessageTypes;

    public ActionInfo() {
        // TODO Auto-generated constructor stub
    }

    public String getActionName() {
        return uri.getResourceName();
    }

    public void setActionName(String actionName) {
        throw new UnsupportedOperationException();
    }

    public Set<SailfishURI> getCompatibleLanguageURIs() {
        return Collections.unmodifiableSet(compatibleLanguageURIs);
    }

    protected void setCompatibleLanguageURIs(Set<SailfishURI> compatibleLanguageURIs) {
        this.compatibleLanguageURIs = compatibleLanguageURIs;
    }

    public boolean isLanguageCompatible(SailfishURI languageURI, boolean exclusive) {
        SailfishURIUtils.checkURI(languageURI, SailfishURIRule.REQUIRE_RESOURCE);
        Iterator<SailfishURI> it = compatibleLanguageURIs.iterator();

        if(exclusive) {
            return compatibleLanguageURIs.size() == 1 && it.next().matches(languageURI);
        }

        while(it.hasNext()) {
            if(it.next().matches(languageURI)) {
                return true;
            }
        }

        return false;
    }

    public ActionRequirements getRequirements() {
        return requirements;
    }

    protected void setRequirements(ActionRequirements requirements) {
        this.requirements = requirements;
    }

    public Class<?> getMessageType() {
        return messageType;
    }

    public void setMessageType(Class<?> messageType) {
        this.messageType = messageType;
    }

    @SuppressWarnings("unchecked")
    public <T> T getAnnotation(Class<T> annotationClass) {
        for(Annotation annotation : annotations) {
            if(annotation.annotationType() == annotationClass) {
                return (T)annotation;
            }
        }

        return null;
    }

    protected void setAnnotations(Annotation[] annotations) {
        this.annotations = annotations;
    }

    public Set<String> getCustomColumns() {
        return customColumns.keySet();
    }

    public CustomColumn getCustomColumn(String name) {
        return customColumns.get(name);
    }

    public void setCustomColumns(Map<String, CustomColumn> customColumns) {
        this.customColumns = ImmutableMap.copyOf(customColumns);
    }

    public Set<String> getCommonColumns() {
        return commonColumns.keySet();
    }

    public CommonColumn getCommonColumn(String name) {
        return commonColumns.get(name);
    }

    public void setCommonColumns(Map<String, CommonColumn> commonColumns) {
        this.commonColumns = ImmutableMap.copyOf(commonColumns);
    }

	public String[] getAllowedMessageTypes() {
        return allowedMessageTypes;
    }

    public void setAllowedMessageTypes(String[] allowedMessageTypes) {
        this.allowedMessageTypes = allowedMessageTypes;
    }

    @Override
    public ActionInfo clone() {
        ActionInfo that = new ActionInfo();

        that.setURI(this.uri);
        that.compatibleLanguageURIs.addAll(this.compatibleLanguageURIs);
        that.setAnnotations(this.annotations);
        that.setCustomColumns(this.customColumns);
        that.setCommonColumns(this.commonColumns);
        that.setMessageType(this.messageType);
        that.setRequirements(this.requirements);
        that.setReturnType(this.returnType);
        that.setDescription(this.description);

        return that;
    }

    @Override
    public boolean equals(Object o) {
        if(o == this) {
            return true;
        }

        if(!(o instanceof ActionInfo)) {
            return false;
        }

        ActionInfo that = (ActionInfo)o;
        EqualsBuilder builder = new EqualsBuilder();

        builder.append(this.uri, that.uri);
        builder.append(this.compatibleLanguageURIs, that.compatibleLanguageURIs);
        builder.append(this.requirements, that.requirements);
        builder.append(this.returnType, that.returnType);
        builder.append(this.messageType, that.messageType);
        builder.append(this.annotations, that.annotations);
        builder.append(this.customColumns, that.customColumns);

        return builder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.append(uri);
        builder.append(compatibleLanguageURIs);
        builder.append(requirements);
        builder.append(returnType);
        builder.append(messageType);
        builder.append(annotations);
        builder.append(customColumns);
        builder.append(commonColumns);
        builder.append(description);

        return builder.toHashCode();
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);

        builder.append("suri", uri);
        builder.append("compatibleLanguages", compatibleLanguageURIs);
        builder.append("requirements", requirements);
        builder.append("returnType", returnType);
        builder.append("messageType", messageType);
        builder.append("annotations", annotations);
        builder.append("customColumns", customColumns);
        builder.append("commonColumns", commonColumns);
        builder.append("description", description);

        return builder.toString();
    }
}
