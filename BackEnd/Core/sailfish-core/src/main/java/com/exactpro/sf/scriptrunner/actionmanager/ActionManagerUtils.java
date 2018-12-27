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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.exactpro.sf.aml.AllowedMessageTypes;
import com.exactpro.sf.aml.CommonColumns;
import com.exactpro.sf.aml.CustomColumn;
import com.exactpro.sf.aml.CustomColumns;
import com.exactpro.sf.aml.Description;
import com.exactpro.sf.configuration.suri.SailfishURI;

public class ActionManagerUtils {

    public static ActionRequirements getActionRequirements(Method actionMethod) {
        CommonColumns commonColumns = actionMethod.getAnnotation(CommonColumns.class);
        CustomColumns customColumns = actionMethod.getAnnotation(CustomColumns.class);

        return new ActionRequirements(commonColumns, customColumns);
    }

    public static Map<String, CustomColumn> getCustomColumns(Method actionMethod) {
        CustomColumns customColumns = actionMethod.getAnnotation(CustomColumns.class);

        if(customColumns == null) {
            return Collections.emptyMap();
        }

        Map<String, CustomColumn> columnMap = new HashMap<>();

        for(CustomColumn column : customColumns.value()) {
            columnMap.put(column.value(), column);
        }

        return columnMap;
    }

    public static ActionInfo getActionInfo(SailfishURI uri, Method actionMethod, Set<SailfishURI> compatibleLanguages) {
        ActionInfo actionInfo = new ActionInfo();

        actionInfo.setURI(uri);
        actionInfo.setCompatibleLanguageURIs(compatibleLanguages);
        actionInfo.setRequirements(getActionRequirements(actionMethod));
        actionInfo.setReturnType(actionMethod.getReturnType());

        Class<?>[] parameterTypes = actionMethod.getParameterTypes();

        if(parameterTypes.length == 2) {
            actionInfo.setMessageType(parameterTypes[1]);
        }

        actionInfo.setAnnotations(actionMethod.getAnnotations());
        actionInfo.setCustomColumns(getCustomColumns(actionMethod));

        if(actionMethod.isAnnotationPresent(AllowedMessageTypes.class)) {
            AllowedMessageTypes messageTypes = actionMethod.getAnnotation(AllowedMessageTypes.class);
            actionInfo.setAllowedMessageTypes(messageTypes.value());
        }

        if(actionMethod.isAnnotationPresent(Description.class)){
            Description description = actionMethod.getAnnotation(Description.class);
            actionInfo.setDescription(description.value());
        }

        return actionInfo;
    }
}
