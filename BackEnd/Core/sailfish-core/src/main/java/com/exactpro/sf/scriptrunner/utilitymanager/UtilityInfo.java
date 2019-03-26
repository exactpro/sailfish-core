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
package com.exactpro.sf.scriptrunner.utilitymanager;

import java.util.Arrays;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.exactpro.sf.scriptrunner.AbstractInfo;

public class UtilityInfo extends AbstractInfo implements Cloneable {
    private String[] parameterNames;
    private Class<?>[] parameterTypes;

    public String[] getParameterNames() {
        return Arrays.copyOf(parameterNames, parameterNames.length);
    }

    public void setParameterNames(String[] parameterNames) {
        this.parameterNames = parameterNames;
    }

    public Class<?>[] getParameterTypes() {
        return Arrays.copyOf(parameterTypes, parameterTypes.length);
    }

    protected void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    @Override
    public UtilityInfo clone() {
        UtilityInfo that = new UtilityInfo();

        that.setURI(uri);
        that.setParameterNames(getParameterNames());
        that.setParameterTypes(getParameterTypes());
        that.setReturnType(returnType);
        that.setDescription(description);

        return that;
    }

    @Override
    public boolean equals(Object o) {
        if(o == this) {
            return true;
        }

        if(!(o instanceof UtilityInfo)) {
            return false;
        }

        UtilityInfo that = (UtilityInfo)o;
        EqualsBuilder builder = new EqualsBuilder();

        builder.append(uri, that.uri);
        builder.append(parameterNames, that.parameterNames);
        builder.append(parameterTypes, that.parameterTypes);
        builder.append(returnType, that.returnType);
        builder.append(description, that.description);

        return builder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.append(uri);
        builder.append(parameterNames);
        builder.append(parameterTypes);
        builder.append(returnType);
        builder.append(description);

        return builder.toHashCode();
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);

        builder.append("suri", uri);
        builder.append("parameterNames", parameterNames);
        builder.append("parameterTypes", parameterTypes);
        builder.append("returnType", returnType);
        builder.append("description", description);

        return builder.toString();
    }
}
