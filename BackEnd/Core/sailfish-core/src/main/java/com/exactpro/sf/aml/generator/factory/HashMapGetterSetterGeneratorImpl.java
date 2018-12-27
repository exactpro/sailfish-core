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

import java.lang.reflect.Method;
import java.util.HashMap;

import com.exactpro.sf.aml.AMLLangConst;
import com.exactpro.sf.aml.generator.IGetterSetterGenerator;
import com.exactpro.sf.common.messages.IMessage;

public class HashMapGetterSetterGeneratorImpl implements IGetterSetterGenerator {

	@Override
	public String addSubmessage(Class<?> type, String parameterName,
			String value, Class<?> paramClass)
	{
		return ".put(\""+parameterName+"\", ("+type.getCanonicalName()+")"+AMLLangConst.MAP_NAME+".get(\""+value+"\"))";
	}

	@Override
	public String getGetter(Class<?> type, String parameterName, String source) {
        return source + ".get(\"" + parameterName + "\")";
	}

	@Override
	public String getMethodForExtractingTreeEntity() {
		return "";
	}

	@Override
	public String getSetter(Class<?> type, String parameterName, String value,
			boolean isReference) {
		if (isReference) {
			return ".put(\""+parameterName+"\", "+value+")";
		}
		return ".put(\""+parameterName+"\", \""+value+"\")";
	}

	@Override
	public Class<?> getSubmessageClass(Class<?> type, String childClassName, Class<?> paramClass)
	{
	    if(paramClass == IMessage.class) {
	        return IMessage.class;
	    }

		return HashMap.class;
	}

	//@Override
	//private Method getGetterMethod(Class<?> type, String parameterName)
	//throws AMLException
	//{
	//	for (Method method : type.getMethods())
	//	{
	//		if (method.getName().equals("get")) {
	//			return method;
	//		}
	//	}
	//	return null;
	//}

	//@Override
	//public Object getConstantValue(Class<?> type, String parameterName, String constantName) throws AMLException
	//{
	//	throw new AMLException("Method not implemented");
	//}

	//@Override
	//private Method getSetterMethodPrimitive(Class<?> type, String parameterName)
	//{
	//	for (Method method : type.getMethods())
	//	{
	//		if (method.getName().equals("put")) {
	//			return method;
	//		}
	//	}
	//	return null;
	//}

	public Method getSetterMethodEnum(Class<?> type, String parameterName)
	{
		return null;
	}

}
