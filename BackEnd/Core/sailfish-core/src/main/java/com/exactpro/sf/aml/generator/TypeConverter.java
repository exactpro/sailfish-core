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
package com.exactpro.sf.aml.generator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.AMLException;

/**
 * Type converter collection.
 * @author dmitry.guriev
 *
 */
public class TypeConverter {

	private static Logger logger = LoggerFactory.getLogger(TypeConverter.class);
    private static final Pattern CAST_PATTERN = Pattern.compile("\\(\\s*\\w+(\\.\\w+\\s*)*\\)(?<value>.+)");

	private TypeConverter()
	{
		// hide constructor
	}

	/**
	 * Search for <code>public static final</code> constant or for enum
	 * field with specified name in specified a class.
	 * Return string representation of founded constant.
	 * @param type class
	 * @param constantName constant name
	 * @return string representation of founded constant or <code>null</code>
	 * if such constant not found.
	 */
	public static String findConstant(Class<?> type, String constantName)
	{
		return findConstant(type, constantName, null); // change this
	}

	/**
	 * Search for <code>public static final</code> constant or for enum
	 * field with specified name and type in specified a class.
	 * If {@code constType} is specified that only constant with this type will be selected.
	 * Return string representation of founded constant.
	 * @param type class
	 * @param value constant name
	 * @param constType desired type of constant
	 * @return string representation of founded constant or <code>null</code>
	 * if such constant not found.
	 */
	public static String findConstant(Class<?> type, String value, AtomicReference<Class<?>> constType)
	{
		if (type.isEnum())
		{
			for (Object constant : type.getEnumConstants())
			{
				if (constant.toString().equals(value))
				{
					return type.getCanonicalName()+"."+constant.toString();
				}
			}
			return null;
		}

		Field[] constants = type.getDeclaredFields();
		for (Field constant : constants)
		{
			if (constant.getName().equals(value)
					&& (constType == null || constType.get() == null || constant.getType().equals(constType.get()))
					&& ((constant.getModifiers() & Modifier.PUBLIC) == Modifier.PUBLIC)
					&& ((constant.getModifiers() & Modifier.STATIC) == Modifier.STATIC)
					&& ((constant.getModifiers() & Modifier.FINAL) == Modifier.FINAL))
			{
				if (constType != null && constType.get() == null)
					constType.set(constant.getType());
				return type.getCanonicalName()+"."+constant.getName();
			}
		}
		return null;
	}

	/**
	 * Search for <code>public static final</code> constant or for enum
	 * field with specified name and type in specified a class.
	 * If {@code constType} is specified that only constant with this type will be selected.
	 * Return constant or enum value.
	 * @param type class
	 * @param name constant name
	 * @param constType desired type of constant
	 * @return string representation of founded constant or <code>null</code>
	 * if such constant not found.
	 */
	public static Object findConstantValue(Class<?> type, String name, Class<?> constType)
			throws AMLException
			{
		try {
			if (type.isEnum())
			{
				Method getValueMethod = null;
				getValueMethod = type.getMethod("getValue", new Class<?>[0]);

				for (Object constant : type.getEnumConstants())
				{
					if (constant.toString().equals(name))
					{
						try {
							return getValueMethod.invoke(constant, new Object[0]);
						} catch (Exception e) {
							throw new AMLException(e);
						}
					}
				}
				return null;
			}

			Field[] constants = type.getDeclaredFields();
			Object obj = type.newInstance();
			for (Field constant : constants)
			{
				if (constant.getName().equals(name)
						&& (constType == null || constant.getType().equals(constType))
						&& ((constant.getModifiers() & Modifier.PUBLIC) == Modifier.PUBLIC)
						&& ((constant.getModifiers() & Modifier.STATIC) == Modifier.STATIC)
						&& ((constant.getModifiers() & Modifier.FINAL) == Modifier.FINAL))
				{
					return constant.get(obj);
				}
			}
		} catch (SecurityException e) {
			throw new AMLException(e);
		} catch (NoSuchMethodException e) {
			throw new AMLException(e);
		} catch (InstantiationException e) {
			throw new AMLException(e);
		} catch (IllegalAccessException e) {
			throw new AMLException(e);
		}
		return null;
			}


	/**
	 * Convert value dependent from it's type.<br>
	 * @param type
	 * @param value original value
	 * @return true if value has been converted,
	 * otherwise - false
	 */
	public static boolean convert(Class<?> type, AtomicReference<String> value) {
		boolean isConverted = false;
		if (value!=null && value.get()!=null) {
			String convertedValue = convert(type, value.get());
			if (convertedValue != null) {
				value.set(convertedValue);
				isConverted = true;
			}
		}
		return isConverted;
	}

	/**
	 * Convert value dependent from it's type.<br>
	 * <li>java.lang.String: convert special characters \t, \r, \n, \\ and \"
	 * and enclose original value in quotes<br>
	 * <li>char || java.lang.Character: enclose original value in single quotes<br>
	 * <li>boolean || java.lang.Boolean: if case insensitive original value
	 * is "y", "yes", "t", "true", "1" - return "true".
	 * For "n", "no", "f", "false", "0" return "false".
	 * <li>long || java.lang.Long: Add 'L' to the end<br>
	 * <li>short || java.lang.Short: Add '(short)' before value<br>
	 * <li>java.math.BigInteger : remove 'L' from the end<br>
	 * <li>other type: <code>null</code>
	 * @param type
	 * @param value original value
	 * @return converted value of <code>null</code>
	 */
	public static String convert(Class<?> type, String value)
	{
		if (type.equals(String.class))
		{
			String newValue = value
					.replace("\\", "\\\\")
					.replace("\r", "\\r")
					.replace("\n", "\\n")
					.replace("\t", "\\t")
					.replace("\"", "\\\"");
			return "\""+newValue+"\"";
		}

		if (type.equals(char.class) || type.equals(Character.class))
		{
			return "\'"+value+"\'";
		}

		if (type.equals(boolean.class) || type.equals(Boolean.class))
		{
			if (value.equalsIgnoreCase("y")
					|| value.equalsIgnoreCase("yes")
					|| value.equalsIgnoreCase("t")
					|| value.equalsIgnoreCase("true")
					|| value.equalsIgnoreCase("1")) {
				return "true";
			}
			if (value.equalsIgnoreCase("n")
					|| value.equalsIgnoreCase("no")
					|| value.equalsIgnoreCase("f")
					|| value.equalsIgnoreCase("false")
					|| value.equalsIgnoreCase("0")) {
				return "false";
			}
		}

		if (type.equals(long.class) || type.equals(Long.class))
		{
			value = value.trim();

			// backward capability:
			if (value.startsWith("(")) // backward capability: (long) value
				return value;

			if (value.endsWith("l") || value.endsWith("L"))
				return "(long) " + value;
			else
				return "(long) " + value + "L";
		}

		if (type.equals(double.class) || type.equals(Double.class)) {
			value = value.trim();

			// backward capability:
			if (value.startsWith("(")) // backward capability: (float) value
				return value;

			if (value.endsWith("d") || value.endsWith("D"))
				return "(double) " + value;
			else if (value.equalsIgnoreCase("NaN"))
				return "Double.NaN";
			else if (value.equalsIgnoreCase("NEGATIVE_INFINITY"))
				return "Double.NEGATIVE_INFINITY";
			else if (value.equalsIgnoreCase("POSITIVE_INFINITY"))
				return "Double.POSITIVE_INFINITY";
			else
			    return "(double) " + value + "D";
		}

		if (type.equals(float.class) || type.equals(Float.class)) {
			value = value.trim();

			// backward capability:
			if (value.startsWith("(")) // backward capability: (float) value
				return value;

			if (value.endsWith("f") || value.endsWith("F"))
				return "(float) " + value;
			else if (value.equalsIgnoreCase("NaN"))
				return "Float.NaN";
			else if (value.equalsIgnoreCase("NEGATIVE_INFINITY"))
				return "Float.NEGATIVE_INFINITY";
			else if (value.equalsIgnoreCase("POSITIVE_INFINITY"))
				return "Float.POSITIVE_INFINITY";
			else
				return "(float) " + value + "F";
		}

		if (type.equals(byte.class) || type.equals(Byte.class)) {
			value = value.trim();

			// backward capability:
			if (value.startsWith("(")) // backward capability: (byte) value
				return value;

			return "(byte) "+value;
		}

		if (type.equals(short.class) || type.equals(Short.class))
		{
			value = value.trim();

			if (value.startsWith("(")) // backward capability: (short) value
				return value;
			else
				return "(short) " + value;
		}

		if (type.equals(BigInteger.class))
		{
			value = value.trim();

			if (value.endsWith("L") || value.endsWith("l")) // Testers adds 'L' to the number because Excel cut off all numbers bigger than 10^15
				value = value.substring(0,  value.length()-1);

			return "new java.math.BigInteger(\"" + value + "\")";
		}

		if (type.equals(BigDecimal.class)) {
            value = value.trim();

            if (value.endsWith("B")) // Testers adds 'B' to the number because Excel cut off all numbers bigger than 10^15
                value = value.substring(0,  value.length()-1);

            return "new java.math.BigDecimal(\"" + value + "\")";
        }
		
		return null;
	}

	/**
	 * Find public static final constant or enum in a class {@code type} which
	 * initialized with specified {@code value}. If the
	 * If no constant found the {@code null} will be returned.
	 * @param type class where constant will be searched
	 * @param value value of the constant
	 * @param constType desired type of constant
	 * @return string representation of founded constant or <code>null</code>
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static String findConstantByValue(Class<?> type, String value) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
	{
		return findConstByValue(type, value, null);
	}

	/**
	 * Find public static final constant or enum in a class {@code type} which
	 * initialized with specified {@code value}.
	 * If no constant found the {@code null} will be returned.
	 * If {@code constType} is specified that only constant with this type will be selected.
	 * @param type class where constant will be searched
	 * @param value value of the constant
	 * @return string representation of founded constant or <code>null</code>
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static String findConstByValue(Class<?> type, String value, Class<?> constType) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
	{
		if (type.isEnum())
		{
			Method[] methods = type.getDeclaredMethods();
			for (Method method : methods)
			{
				if (method.getName().equals("getEnumValue")) {
					if (method.getParameterTypes().length == 1)
					{
                        Matcher matcher = CAST_PATTERN.matcher(value);
                        if(matcher.find()) {
                            value = matcher.group("value");
						}
						Object objArg = convertToObject(method.getParameterTypes()[0], value);
						Object objVal = method.invoke(null, objArg);
						return type.getCanonicalName()+"."+objVal.toString();
					}
				}
			}
			for (Object constant : type.getEnumConstants())
			{
				if (constant.toString().equals(value))
				{
					return type.getCanonicalName()+"."+constant.toString();
				}
			}
			return null;
		}

		Field[] constants = type.getDeclaredFields();
		for (Field constant : constants)
		{
			if ((constType == null || constant.getType().equals(constType))
					&& ((constant.getModifiers() & Modifier.PUBLIC) == Modifier.PUBLIC)
					&& ((constant.getModifiers() & Modifier.STATIC) == Modifier.STATIC)
					&& ((constant.getModifiers() & Modifier.FINAL) == Modifier.FINAL))
			{
				try {
					Object objArg = convertToObject(constant.getType(), value);
					Object cvalue = constant.get(null);
					if (objArg.equals(cvalue)) {
						return type.getCanonicalName()+"."+constant.getName();
					}
				} catch (Throwable e) {
					logger.warn("Value '{}' can not be converted to type {}", value, constant.getType());
				}
			}
		}
		return null;
	}

	/**
	 * Convert string representation of object to instance of specified type
	 * @param type to convert to
	 * @param value string representation of the object
	 * @return instance of specified type with specified value
	 */
	public static Object convertToObject(Class<?> type, String value)
	{
		if (type.equals(int.class) || type.equals(Integer.class)) {
			return Integer.parseInt(value);
		}
		if (type.equals(float.class) || type.equals(Float.class)) {
			return Float.parseFloat(value);
		}
		if (type.equals(double.class) || type.equals(Double.class)) {
			return Double.parseDouble(value);
		}
		if (type.equals(long.class) || type.equals(Long.class)) {
			return Long.parseLong(value);
		}
		if (type.equals(short.class) || type.equals(Short.class)) {
			return Short.parseShort(value);
		}
		if (type.equals(char.class) || type.equals(Character.class)) {
			if (value.length() == 1) {
				return value.charAt(0);
			}
			if (value.startsWith("\\")) {
				return (char)Integer.parseInt(value.substring(1), 8);
			}
		}
		if (type.equals(byte.class) || type.equals(Byte.class)) {
			return Byte.parseByte(value);
		}
		if (type.equals(boolean.class) || type.equals(Boolean.class)) {
			return Boolean.parseBoolean(value);
		}
		if (type.equals(String.class)) {
			return value;
		}
		if (type.equals(BigDecimal.class)) {
			return new BigDecimal(value);
		}
		return null;
	}

	/**
	 * Search for constructor with specified
	 * parameter type in the specified class.
	 * Return a Constructor type object.
	 * @param type class
	 * @param parameterType single parameter of the constructor
	 * @return an appropriate constructor or <code>null</code>
	 * if such constructor not found.
	 */
	public static Constructor<?> findConstructor(Class<?> type, Class<?> parameterType) {
		Constructor<?>[] constructors = type.getConstructors();
		for (Constructor<?> constructor : constructors) {
			Class<?>[] parameterTypes = constructor.getParameterTypes();
			if (parameterTypes.length == 1 && parameterTypes[0].equals(parameterType))
				return constructor;
		}
		return null;
	}

	/**
	 * Infer a constructor for the specified class.
	 * Return a Constructor type object.
	 * @param type class
	 * @return an appropriate constructor or <code>null</code>
	 * if the class does not have any constructor.
	 */
	public static Constructor<?> inferConstructor(Class<?> type) {
		if (type.equals(String.class))
			return null;
		if (type.equals(BigDecimal.class) || type.equals((BigInteger.class))) {
			try {
				return type.getConstructor(String.class);
			} catch (Exception e) {
				return null;
			}
		} else {
			Constructor<?>[] constructors = type.getConstructors();
			for (Constructor<?> constructor : constructors) {
				Class<?>[] parameterTypes = constructor.getParameterTypes();
				if (parameterTypes.length == 1 && parameterTypes[0].equals(ClassUtils.wrapperToPrimitive(type)))
					return constructor;
			}
			return null;
		}
	}
}
