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

package com.exactpro.sf.externalapi;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.PropertyUtilsBean;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.configuration.suri.SailfishURI;

public class AbstractSettingsProxy implements ISettingsProxy {
    protected final ICommonSettings settings;
    protected final Map<String, PropertyDescriptor> descriptors;


    public AbstractSettingsProxy(ICommonSettings settings) {
        this.settings = settings;
        PropertyUtilsBean beanUtils = new PropertyUtilsBean();
        PropertyDescriptor[] array = beanUtils.getPropertyDescriptors(this.settings);

        this.descriptors = new HashMap<>();
        for (PropertyDescriptor propertyDescriptor : array) {
            if (propertyDescriptor.getReadMethod() != null && propertyDescriptor.getWriteMethod() != null) {
                this.descriptors.put(propertyDescriptor.getName(), propertyDescriptor);
            }
        }
    }

    @Override
    public Set<String> getParameterNames() {
        return this.descriptors.keySet();
    }

    @Override
    public Class<?> getParameterType(String name) {
        PropertyDescriptor descriptor = this.descriptors.get(name);
        if (descriptor != null) {
            return descriptor.getPropertyType();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getParameterValue(String name) {
        PropertyDescriptor descriptor = this.descriptors.get(name);
        if (descriptor != null) {
            try {
                return (T) descriptor.getReadMethod().invoke(settings);
            } catch (ClassCastException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new EPSCommonException("Incorrect type of " + name + " field", e);
            }
        }

        return null;
    }

    @Override
    public void setParameterValue(String name, Object value) {
        PropertyDescriptor descriptor = this.descriptors.get(name);
        if (descriptor != null) {
            try {
                descriptor.getWriteMethod().invoke(settings, value);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new EPSCommonException(e);
            }
        } else {
            throw new EPSCommonException(String.format("Setting with name %s not found", name));
        }
    }

    @Override
    public SailfishURI getDictionary() {
        throw new UnsupportedOperationException("Settings don't have a dictionary");
    }

    @Override
    public void setDictionary(SailfishURI dictionary) {
        throw new UnsupportedOperationException("Settings don't have a dictionary");
    }
}
