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
package com.exactpro.sf.help.helpmarshaller.describers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.exactpro.sf.common.impl.messages.BaseMessage;
import com.exactpro.sf.common.impl.messages.IBaseEnumField;
import com.exactpro.sf.help.helpmarshaller.AbstrFieldMess;
import com.exactpro.sf.help.helpmarshaller.Message;

public class FieldsDescriber {

	public static void describeFields(List<AbstrFieldMess> fields, Class<?> messageClazz) {
		Method[] methods = messageClazz.getMethods();
		Map<String, Integer> getSeen = new TreeMap<String, Integer>();
		Map<String, Integer> setSeen = new TreeMap<String, Integer>();
		Map<String, Class<?>> parameter2Arg = new TreeMap<String, Class<?>>();

		findGettersAndSetters(methods, getSeen, setSeen, parameter2Arg);

		for (Entry<String, Integer> entry : getSeen.entrySet()) {
			String fieldName = entry.getKey();
			if (setSeen.containsKey(fieldName)) {
				AbstrFieldMess newField;

				Class<?> paramClass = parameter2Arg.get(fieldName);

				Class<?> qfjMessageComponent = null;
				Class<?> qfjGroup = null;
				Class<?> qfjField = null;

				try {
					//Check for system class loader
					if (paramClass.getClassLoader() != null) {
						qfjMessageComponent = paramClass.getClassLoader().loadClass("quickfix.MessageComponent");
						qfjGroup = paramClass.getClassLoader().loadClass("quickfix.Group");
						qfjField = paramClass.getClassLoader().loadClass("quickfix.Field");
					}
				} catch (ClassNotFoundException e) {
					//Do nothing
				}

				if (BaseMessage.class.isAssignableFrom(paramClass)) {
					newField = new Message();
				} else if (qfjMessageComponent != null && qfjMessageComponent.isAssignableFrom(paramClass)) {
					newField = new Message();
				} else if (qfjGroup != null && qfjGroup.isAssignableFrom(paramClass)) {
					newField = new Message();
				} else if (paramClass.isEnum()) {
                    newField = new com.exactpro.sf.help.helpmarshaller.Field();
				} else if (fieldName.equals(paramClass.getSimpleName())) {
                    newField = new com.exactpro.sf.help.helpmarshaller.Field();
				} else {
                    newField = new com.exactpro.sf.help.helpmarshaller.Field();
				}

				// Set Type
				if (paramClass.getDeclaredFields() != null) {
					try {
						Object obj = paramClass.newInstance();
						for (Field c : paramClass.getDeclaredFields()) {
							if (c.getName().equals("FIELD")) {
								newField.setTag(c.get(obj).toString());
								break;
							}
						}
					} catch (InstantiationException e) {
						// ignore exception
					} catch (IllegalAccessException e) {
						// ignore exception
					}
				} else {
					newField.setTag("");
				}

				// Set Name
				newField.setName(fieldName);

				// Set Type

				if (qfjField != null && qfjField.isAssignableFrom(paramClass)) {
					for (Constructor<?> constr : paramClass.getConstructors()) {
						if (constr.getParameterTypes().length == 1) {
							newField.setType(constr.getParameterTypes()[0].getSimpleName());
						}
					}
				} else if (paramClass.isEnum()) {
					newField.setType(paramClass.getSimpleName());
				} else if (!BaseMessage.class.isAssignableFrom(paramClass) && (qfjMessageComponent == null && qfjGroup == null
						|| !qfjMessageComponent.isAssignableFrom(paramClass) && !qfjGroup.isAssignableFrom(paramClass))) {
					newField.setType(paramClass.getSimpleName());
				}

				if (newField instanceof Message) {
					List<AbstrFieldMess> list = ((Message) newField).getFieldOrMessage();
					describeFields(list, paramClass);
					newField.setType(paramClass.getSimpleName());
				} else {
                    describeField(paramClass, (com.exactpro.sf.help.helpmarshaller.Field) newField);
				}

				fields.add(newField);
			}
		}

		if (java.util.HashMap.class.equals(messageClazz)) {
			return;
		}

		// add group classes
		Class<?>[] classes = messageClazz.getDeclaredClasses();
		for (Class<?> cls : classes) {
			Message newField = new Message();

			newField.setTag("group");

			newField.setName(cls.getSimpleName());

			newField.setType("");

			List<AbstrFieldMess> list = newField.getFieldOrMessage();

			describeFields(list, cls);
			fields.add(newField);
		}

	}

	/**
	 * Find getter and setter methods.
	 *
	 * @param methods    all declared methods
	 * @param getSeen    declared getter methods
	 * @param setSeen    declared setter methods
	 * @param paramTypes return types for getMethods
	 */
	private static void findGettersAndSetters(Method[] methods, Map<String, Integer> getSeen, Map<String, Integer> setSeen,
			Map<String, Class<?>> paramTypes) {
		for (Method m : methods) {
			if (m.getName().startsWith("get") && m.getParameterTypes().length == 0) {
				String fname = m.getName().substring(3);
				if (getSeen.containsKey(fname)) {
					getSeen.put(fname, getSeen.get(fname) + 1);
				} else {
					getSeen.put(fname, 1);
				}
				if (!paramTypes.containsKey(fname)) {
					paramTypes.put(fname, m.getReturnType());
				}
			}
			// FIX implementation
			else if (m.getName().equals("set") && m.getParameterTypes().length == 1) {
				String fname = m.getParameterTypes()[0].getSimpleName();
				if (!fname.isEmpty()) {
					if (setSeen.containsKey(fname)) {
						setSeen.put(fname, setSeen.get(fname) + 1);
					} else {
						setSeen.put(fname, 1);
					}
				}
			}
			// generic implementation
			else if ((m.getName().startsWith("set") || m.getName().startsWith("add")) && m.getParameterTypes().length == 1) {
				String fname = m.getName().substring(3);
				if (!fname.isEmpty()) {
					if (setSeen.containsKey(fname)) {
						setSeen.put(fname, setSeen.get(fname) + 1);
					} else {
						setSeen.put(fname, 1);
					}
					if (m.getName().startsWith("add")) {
						paramTypes.put(fname, m.getParameterTypes()[0]);
					}
				}
			}
		}
	}

    private static void describeField(Class<?> clazz, com.exactpro.sf.help.helpmarshaller.Field toDescribe) {
		List<String> validValues = toDescribe.getValues();

		if (isNotStandartClass(clazz)) {
			if (clazz.getEnumConstants() != null) {
				Method getValueMethod = null;
				try {
					getValueMethod = clazz.getMethod("getValue", new Class<?>[0]);
				} catch (SecurityException e) {
					// ignore exception
				} catch (NoSuchMethodException e) {
					// ignore exception
				}

				for (Object c : clazz.getEnumConstants()) {
					String valueString = c.toString();
					if(valueString.equals(IBaseEnumField.MISSED) || valueString.equals(IBaseEnumField.PRESENT)){
						continue;
					}
					if (getValueMethod != null) {
						try {
							Object value = getValueMethod.invoke(c, new Object[0]);
							valueString += "=" + value;
						} catch (Exception e) {
							// ignore exception
						}
					}
					validValues.add(valueString);
				}
			}

			if (clazz.getDeclaredFields() != null && !clazz.isEnum()) {
				try {
					Object obj = clazz.newInstance();
					for (Field c : clazz.getDeclaredFields()) {
						if (!(c.getName().equals("FIELD") || c.getName().equals("serialVersionUID") || (c.getName().equals("MSGTYPE") && c.get(obj)
								.equals("")))) {
							String valueString = c.getName() + "=" + c.get(obj);
							validValues.add(valueString);
						}
					}
				} catch (InstantiationException e) {
					// ignore exception
				} catch (IllegalAccessException e) {
					// ignore exception
				}
			}
		}

	}

	private static boolean isNotStandartClass(Class<?> paramClass) {
		return !(paramClass.equals(String.class) || paramClass.equals(Integer.class));
	}


}
