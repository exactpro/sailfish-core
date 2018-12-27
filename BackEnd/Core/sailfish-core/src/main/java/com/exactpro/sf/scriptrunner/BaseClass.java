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
package com.exactpro.sf.scriptrunner;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.exactpro.sf.center.IVersion;

public abstract class BaseClass {
    protected final String className;
    protected final List<String> classAliases = new ArrayList<>();
    protected final String classDescription;
    protected final IVersion version;
    protected final List<Method> classMethods = new ArrayList<>();

    protected BaseClass(String className, String classDescription, IVersion version) {
        this.className = className;
        this.classDescription = classDescription;
        this.version = version;
    }

    public String getClassName() {
        return className;
    }

    public List<String> getClassAliases() {
        return Collections.unmodifiableList(classAliases);
    }

    public String getClassDescription() {
        return classDescription;
    }

    public List<Method> getClassMethods() {
        // return new list to allow sorting in GUI
        return new ArrayList<>(classMethods);
    }

    public IVersion getPlugin() {
        return version;
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);

        builder.append("className", className);
        builder.append("classAliases", classAliases);
        builder.append("classDescription", classDescription);
        builder.append("version", version);
        builder.append("classMethods", classMethods);

        return builder.toString();
    }
}
