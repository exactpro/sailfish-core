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
package com.exactpro.sf.testwebgui.general;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ManagedBean(name="sessionStorage")
@SessionScoped
@SuppressWarnings("serial")
public class SessionStorage implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger(SessionStorage.class);

	private Map<String, Object> storage = new ConcurrentHashMap<String, Object>();

	private Object cloneBySerialization(Serializable value) {

		return SerializationUtils.clone(value);

	}

	public void put(String key, Object value) {

		this.storage.put(key, value);

	}

	public <E extends Object> E get(String key, Class<E> clazz) {

		return clazz.cast(storage.get(key));

	}

	public void saveStateOfAnnotatedBean(Object bean) {

		String className = bean.getClass().getSimpleName();

        for (Field f : getAllObjectFields(bean)) {

			if (f.isAnnotationPresent(SessionStored.class)) {

				String fieldName = f.getName();

				Annotation annotation = f.getAnnotation(SessionStored.class);
				SessionStored ssAnotation = (SessionStored) annotation;

				try {

					f.setAccessible(true);

					Object value = f.get(bean);

					if(value != null) {

						String storageKey = className + "." + fieldName;

						if(!ssAnotation.cloneBySerialization()) {
							this.storage.put(storageKey, value);
						} else {
							logger.debug("Cloned");
							this.storage.put(storageKey, cloneBySerialization((Serializable)value));
						}

						logger.debug("Value stored {}", storageKey);

					}

				} catch (Exception e) {
					logger.error("Could not access value of field {} of class {}", fieldName, className, e);
				}

			}

		}

	}

	public void restoreStateOfAnnotatedBean(Object bean) {

		String className = bean.getClass().getSimpleName();



        for (Field f : getAllObjectFields(bean)) {

			if (f.isAnnotationPresent(SessionStored.class)) {

				String fieldName = f.getName();

				try {

					String storageKey = className + "." + fieldName;

					Object value = this.storage.get(storageKey);//f.get(bean);

					if(value != null) {

						f.setAccessible(true);

						f.set(bean, value);

						logger.debug("Value restored {}", storageKey);

					}

				} catch (Exception e) {
					logger.error("Could not access value of field {} of class {}", fieldName, className, e);
				}

			}

		}

	}

	private Field[] getAllObjectFields(Object bean){
        Field[] fields = new Field[0];

        Class<?> clazz = bean.getClass();

        while (clazz.getSuperclass() != null) {
            fields = ArrayUtils.addAll(fields, clazz.getDeclaredFields());
            clazz = clazz.getSuperclass();
        }
        return fields;
    }
}

