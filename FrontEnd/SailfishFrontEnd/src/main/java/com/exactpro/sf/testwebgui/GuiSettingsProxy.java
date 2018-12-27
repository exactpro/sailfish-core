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

package com.exactpro.sf.testwebgui;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.aml.Ignore;
import com.exactpro.sf.aml.InputMask;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.externalapi.AbstractSettingsProxy;
import com.exactpro.sf.services.RequiredParam;

/**
 * Used to display setting in gui
 */
public class GuiSettingsProxy extends AbstractSettingsProxy {
    private final Map<Pair<String, Class<?>>, Field> fieldsCache;

    public GuiSettingsProxy(ICommonSettings settings) {
        super(settings);
        fieldsCache = new HashMap<>();
    }

    private Field getSettingsParameter(String name) {

        Class<?> clazz = settings.getClass();
        Field field = fieldsCache.get(new ImmutablePair<String, Class<?>>(name, clazz));

        if (field == null) {
            field = getSettingsParameter(clazz,name);
            fieldsCache.put(new ImmutablePair<String, Class<?>>(name, clazz), field);
        }

        return field;
    }

    private  Field getSettingsParameter(Class<?> clazz, String name){
        while (!clazz.equals(Object.class)) {
            //fixme different naming style for service setting classes
            Field[] fields = clazz.getDeclaredFields();
            for (Field f : fields) {
                if (f.getName().equalsIgnoreCase(name)) {
                    return f;

                }
            }
            clazz = clazz.getSuperclass();
        }
        return  null;
    }

    public  boolean isShowElement(String name){
        Field element = getSettingsParameter(name);

        if(element == null){
            return false;
        }
        return !element.isAnnotationPresent(Ignore.class);
    }

    public String getParameterMask(String name) {
        Field field = getSettingsParameter(name);

        if (field == null) {
            return null;
        }

        InputMask mask = field.getAnnotation(InputMask.class);
        if (mask == null) {
            return null;
        }

        return mask.value();
    }

    public String getParameterDescription(String name) {
        Field field = getSettingsParameter(name);
        if (field == null) {
            return null;
        }

        Description descr = field.getAnnotation(Description.class);
        if (descr == null) {
            return null;
        }

        return descr.value();
    }

    public boolean checkRequiredParameter(String name) {
        Field field = getSettingsParameter(name);
        return field != null && field.isAnnotationPresent(RequiredParam.class);
    }

    public boolean haveWriteMethod(String name){
        return descriptors.get(name).getWriteMethod() != null;
    }
}
