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
package com.exactpro.sf.aml.generator.factory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.AMLException;
import com.exactpro.sf.aml.AMLLangConst;
import com.exactpro.sf.aml.generator.IGetterSetterGenerator;
import com.exactpro.sf.aml.generator.TypeConverter;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.comparison.Convention;

public class BaseMessageGetterSetterGeneratorImpl implements IGetterSetterGenerator
{
	private static final Logger logger = LoggerFactory.getLogger( BaseMessageGetterSetterGeneratorImpl.class );

	@Override
	public String addSubmessage( Class<?> type, String parameterName, String value, Class<?> paramClass ) throws AMLException
	{
		String setName = "set" + parameterName;
		String addName = "add" + parameterName;

		for (Method method : type.getMethods())
		{
			if (method.getName().equals(setName)) {
				if (method.getParameterTypes().length == 1) {
					if (paramClass == null) {
						Class<?> pType = method.getParameterTypes()[0];
						return "."+setName+"(("+pType.getCanonicalName()+")"+AMLLangConst.MAP_NAME+".get(\""+value+"\"))";
					}
					if (paramClass.isAssignableFrom(method.getParameterTypes()[0])) {
						Class<?> pType = method.getParameterTypes()[0];
						return "."+setName+"(("+pType.getCanonicalName()+")"+AMLLangConst.MAP_NAME+".get(\""+value+"\"))";
					}
				}
				else
				{
					throw new EPSCommonException("Setter "+setName+" have "+method.getParameterTypes().length+" parameters, but expected 1.");
				}
			}
			if (method.getName().equals(addName)) {
				if (method.getParameterTypes().length == 1) {
					if (paramClass == null) {
						Class<?> pType = method.getParameterTypes()[0];
						return "."+addName+"(("+pType.getCanonicalName()+")"+AMLLangConst.MAP_NAME+".get(\""+value+"\"))";
					}
					if (paramClass.isAssignableFrom(method.getParameterTypes()[0])) {
						Class<?> pType = method.getParameterTypes()[0];
						return "."+addName+"(("+pType.getCanonicalName()+")"+AMLLangConst.MAP_NAME+".get(\""+value+"\"))";
					}
				}
				else
				{
					throw new EPSCommonException("Setter "+addName+" have "+method.getParameterTypes().length+" parameters, but expected 1.");
				}
			}
		}
		throw new AMLException("No field "+parameterName+" in message "+type.getCanonicalName());
	}

	@Override
	public Class<?> getSubmessageClass( Class<?> type, String childClassName, Class<?> paramClass) throws AMLException
	{
		logger.debug("getSubmessageClass: {} {}", type.getCanonicalName(), childClassName);

		String setName = "set" + childClassName;
		String addName = "add" + childClassName;

		for (Method method : type.getMethods())
		{
			if (method.getName().equals(setName) || method.getName().equals(addName)) {
				if (method.getParameterTypes().length == 1) {
					if (paramClass == null) {
						logger.debug("return {}", method.getParameterTypes()[0].getCanonicalName());
						return method.getParameterTypes()[0];
					}
					if (paramClass.isAssignableFrom(method.getParameterTypes()[0])) {
						logger.debug("return {}({})", method.getParameterTypes()[0].getCanonicalName(), method.getParameterTypes()[0]);
						return method.getParameterTypes()[0];
					}
				}
				else
				{
					throw new EPSCommonException("Setter "+setName+" have "+method.getParameterTypes().length+" parameters.");
				}
			}
		}
		throw new AMLException("No field "+childClassName+" in message "+type.getCanonicalName());
	}

	/**
	 * @throws AMLException
	 *
	 */
	@Override
    public String getGetter(Class<?> type, String parameterName, String source) throws AMLException
	{
		Method method = getGetterMethod(type, parameterName);

		String proposedGetterName = "get" + parameterName;
		if(method == null)
		{
			logger.error("Requested getter {} was not found in the type {}.", proposedGetterName, type.getCanonicalName());
			throw new AMLException("No field "+parameterName+" in message "+type.getCanonicalName());
		}

        return source + "." + proposedGetterName + "()";
	}

	//@Override
	private Method getGetterMethod (Class<?> type, String parameterName)
	{
		String proposedGetterName = "get" + parameterName;

		for (Method method : type.getMethods())
		{
			if( method.getName().equals( proposedGetterName ))
			{
				return method;
			}
		}
		return null;
	}

	@Override
	public String getSetter( Class<?> type, String parameterName, String value, boolean isReference ) throws AMLException
	{
		Method methodWithEnum = getSetterMethodEnum(type, parameterName);
		Method methodWithPrimitive = getSetterMethodPrimitive(type, parameterName);

		if (methodWithEnum == null && methodWithPrimitive == null)
		{
			throw new AMLException("No field "+parameterName+" in message "+type.getCanonicalName());
		}

		if (isReference)
		{
			String methodName = null;
			if (methodWithEnum != null) methodName = methodWithEnum.getName();
			else if (methodWithPrimitive != null) methodName = methodWithPrimitive.getName();

			return "."+methodName+"("+value+")";
		}

		int flag = 0;
		if (methodWithEnum != null)
		{
			String convertedValue = value;
			Class<?> valueType = methodWithEnum.getParameterTypes()[0];
			convertedValue = TypeConverter.findConstant(valueType, value);
			String methodName = methodWithEnum.getName();

			if (Convention.CONV_PRESENT_STRING.equals(value)){
				convertedValue = valueType.getCanonicalName() + "." + "Present";
			} else if (Convention.CONV_MISSED_STRING.equals(value)){
				convertedValue = valueType.getCanonicalName() + "." + "Missed";
			}
			if (convertedValue != null){
				return "." + methodName + "(" + convertedValue + ")";
			}
			try {
				convertedValue = TypeConverter.findConstantByValue(valueType, value);
				return 	"."+methodName+"("+convertedValue+")";
			} catch (IllegalArgumentException e) {
				logger.error(e.getMessage(), e);
			} catch (IllegalAccessException e) {
				logger.error(e.getMessage(), e);
			} catch (InvocationTargetException e) {
				logger.error(e.getMessage(), e);
			}
			flag = 1;
		}

		if (methodWithPrimitive != null)
		{
			String methodName = methodWithPrimitive.getName();

			flag = 2;

			Class<?> methodParameterType = methodWithPrimitive.getParameterTypes()[0];

			try{
				if (Convention.hasConventionForType(methodParameterType)) {
					String convertedValue;
					if (value.equals(Convention.CONV_PRESENT_STRING)) {
						convertedValue = Convention.getReplacementForPresent(methodParameterType);
						//logger.debug("convention for check if tag exist: "+convertedValue);
						return "."+methodName+"("+convertedValue+")";
					}
					if (value.equals(Convention.CONV_MISSED_STRING)) {
						convertedValue = Convention.getReplacementForMissed(methodParameterType);
						//logger.debug("convention for check if tag not exist: "+convertedValue);
						return "."+methodName+"("+convertedValue+")";
					}
				}
			} catch (Exception e) {
				throw new AMLException(e);
			}

			AtomicReference<Class<?>> constType = new AtomicReference<Class<?>>();
			String convertedValue = TypeConverter.findConstant(methodParameterType, value, constType);
			if (convertedValue != null)
			{
				if ((TypeConverter.findConstructor(methodParameterType, constType.get()))!=null) {
					return "." + methodName + "( new "+ methodParameterType.getCanonicalName() + "(" + convertedValue + ")" + ")";
				} else if (constType.get().equals(methodParameterType)){
					return "." + methodName + "(" + convertedValue + ")";
				}
			} else {
				AtomicReference<String> escapedValue = new AtomicReference<String>(value);
				Constructor<?> appropriateConstructor = TypeConverter.inferConstructor(methodParameterType);
				if (appropriateConstructor == null) {
					TypeConverter.convert(methodParameterType, escapedValue);
					return "." + methodName + "(" + escapedValue + ")";
				} else {
					TypeConverter.convert(appropriateConstructor.getParameterTypes()[0], escapedValue);
					return "." + methodName + "( new " + methodParameterType.getCanonicalName() + "(" + escapedValue + ")" + ")";
				}
			}


		}
		logger.error("Requested setter for field [{}] was not found in the type [{}].", parameterName, type.getCanonicalName());
		switch (flag)
		{
		case 1:
			throw new AMLException("No enum value '"+value+"' found for "+parameterName+" in message "+type.getCanonicalName());
		case 2:
			throw new AMLException("No sutable constructors found for field "+parameterName+" in message "+type.getCanonicalName());
		default:
			throw new AMLException("No field "+parameterName+" found in message "+type.getCanonicalName()+" by unknown reason");
		}

	}

	//@Override
	private Method getSetterMethodPrimitive( Class<?> type, String parameterName)
	{
		String setMethodName = "set"+parameterName;
		String addMethodName = "add"+parameterName;
		for (Method method : type.getMethods())
		{
			String methodName = method.getName();
			if (methodName.equals(setMethodName) || methodName.equals(addMethodName))
			{
				Class<?> methodParameterType = method.getParameterTypes()[0];
				if (!methodParameterType.isEnum())
				{
					return method;
				}
			}
		}
		return null;
	}

	//@Override
	private Method getSetterMethodEnum( Class<?> type, String parameterName)
	{
		String setMethodName = "set"+parameterName;
		String addMethodName = "add"+parameterName;
		for (Method method : type.getMethods())
		{
			String methodName = method.getName();
			if (methodName.equals(setMethodName) || methodName.equals(addMethodName))
			{
				Class<?> methodParameterType = method.getParameterTypes()[0];
				if (methodParameterType.isEnum())
				{
					return method;
				}
			}
		}
		return null;
	}

	//@Override
	//private Object getConstantValue(Class<?> type, String parameterName, String constantName) throws AMLException
	//{
	//	Method method = getSetterMethodEnum(type, parameterName);
	//	if (method != null) {
	//		Class<?> constType = method.getParameterTypes()[0];
	//		return TypeConverter.findConstantValue(type, constantName, constType);
	//	}
	//	return null;
	//}

	@Override
	public String getMethodForExtractingTreeEntity()
	{
		return ".getMessage()";
	}
}
