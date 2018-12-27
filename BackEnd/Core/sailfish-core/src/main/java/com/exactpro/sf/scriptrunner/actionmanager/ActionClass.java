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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.scriptrunner.BaseClass;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityClass;

public class ActionClass extends BaseClass {

    private final List<UtilityClass> utilityClasses = new ArrayList<UtilityClass>();

    protected ActionClass(String className, String classDescription, IVersion version) {
        super(className, classDescription, version);
    }

    protected void addClassAlias(String classAlias) {
        classAliases.add(classAlias);
    }

    protected void addClassMethod(Method classMethod) {
        classMethods.add(classMethod);
    }

    public List<UtilityClass> getUtlityClasses() {
        return Collections.unmodifiableList(utilityClasses);
    }

    protected void addUtilityClass(UtilityClass utilityClass) {
        utilityClasses.add(utilityClass);
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);

        builder.append("className", className);
        builder.append("classAliases", classAliases);
        builder.append("classDescription", classDescription);
        builder.append("plugin", version);
        builder.append("classMethods", classMethods);
        builder.append("utilityClasses", utilityClasses);

        return builder.toString();
    }
}
