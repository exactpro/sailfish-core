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
package com.exactpro.sf.aml.generator.matrix;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.exactpro.sf.aml.AMLException;
import com.exactpro.sf.aml.generator.TypeConverter;

public class TypeHelper {

	private static Map<String, Class<?>> types = new HashMap<String, Class<?>>();

	static {
		add(String.class);
		add(int.class);
		add(Integer.class);
		add(double.class);
		add(Double.class);
		add(float.class);
		add(Float.class);
		add(boolean.class);
		add(Boolean.class);
		add(long.class);
		add(Long.class);
		add(byte.class);
		add(Byte.class);
		add(short.class);
		add(Short.class);
		add(char.class);
		add(Character.class);
		add(Object.class);
        add(LocalDateTime.class);
        add(LocalDate.class);
        add(LocalTime.class);
        add(Date.class, LocalDateTime.class); // Backward compatibility with Date type in matrix
		add(BigDecimal.class);
	}

    private static void add(Class<?> keyClass, Class<?> valueClass) {
        types.put(keyClass.getCanonicalName(), valueClass);
        types.put(keyClass.getSimpleName(), valueClass);
    }

	private static void add(Class<?> clazz) {
        add(clazz, clazz);
	}

	private TypeHelper() {
		// hide constructor
	}

	public static Class<?> getClass(String clazz) {
		return types.get(clazz);
	}

	public static String convertValue(String type, String value) throws AMLException
	{
        Class<?> clazz = getClass(type);
        
        if (clazz == null) {
            throw new AMLException("Invalid type: " + type);
        } else if (clazz.equals(Object.class) || clazz.equals(LocalDateTime.class) || clazz.equals(LocalDate.class)
                || clazz.equals(LocalTime.class)) {
            throw new AMLException("Cannot convert " + clazz.getSimpleName());
        }

		return ObjectUtils.defaultIfNull(TypeConverter.convert(clazz, value), value);
	}

}
