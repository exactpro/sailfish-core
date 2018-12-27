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
package com.exactpro.sf.common.adapting.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import com.exactpro.sf.common.adapting.IAdapterFactory;
import com.exactpro.sf.common.util.EPSCommonException;

public class DefaultAdapterFactory implements IAdapterFactory 
{
	private Class<?>[] adapterList;
	private Map<Class<?>, Class<?>> adapterMap; 

	public DefaultAdapterFactory(Class<?>[] adapterList, Map<Class<?>, Class<?>> adapterMap) {
		this.adapterList = adapterList;
		this.adapterMap = adapterMap;

	}

	@Override
	public Object getAdapter(Object adaptableObject, Class<?> adapterType) 
	{
		Class<?> adapterClass = this.adapterMap.get(adapterType);

		if ( adapterClass == null )
			throw new EPSCommonException("Could not find adapterClass for adapter [" + adapterType + "]");

		Constructor<?>[] constructors = adapterClass.getConstructors();
		Constructor<?> constructor = constructors[0];

		Object obj = null;
		try
		{
			if ( adaptableObject == null && constructor.getParameterTypes().length == 0 )
			{
				obj = constructor.newInstance();
				return obj;
			}
			obj = constructor.newInstance(adaptableObject);
			return obj;

//			return adapterClass.getConstructor(new Class[]{}).newInstance(adaptableObject);
		}
//		catch ( NoSuchMethodException e )
//		{
//			throw new EPSCommonException(e);
//		}
		catch ( InvocationTargetException e)
		{
			throw new EPSCommonException("adaptableObject="+adaptableObject+", adapterType="+adapterType+", adapterClass="+adapterClass+", constructor="+constructor.getName()+", parameters="+constructor.getParameterTypes().length+", obj="+obj, e);
		}
		catch ( IllegalArgumentException e )
		{
			throw new EPSCommonException("adaptableObject="+adaptableObject+", adapterType="+adapterType+", adapterClass="+adapterClass+", constructor="+constructor.getName()+", parameters="+constructor.getParameterTypes().length+", obj="+obj, e);
		}
		catch ( IllegalAccessException e)
		{
			throw new EPSCommonException("adaptableObject="+adaptableObject+", adapterType="+adapterType+", adapterClass="+adapterClass+", constructor="+constructor.getName()+", parameters="+constructor.getParameterTypes().length+", obj="+obj, e);
		}
		catch ( InstantiationException e )
		{
			throw new EPSCommonException("adaptableObject="+adaptableObject+", adapterType="+adapterType+", adapterClass="+adapterClass+", constructor="+constructor.getName()+", parameters="+constructor.getParameterTypes().length+", obj="+obj, e);
		}
	}

	@Override
	public Class<?>[] getAdapterList() {
		return this.adapterList;
	}

}
