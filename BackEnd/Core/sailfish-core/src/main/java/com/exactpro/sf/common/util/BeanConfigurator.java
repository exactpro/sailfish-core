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
package com.exactpro.sf.common.util;

import java.beans.PropertyDescriptor;


import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.configuration.HierarchicalConfiguration;

public class BeanConfigurator 
{
	
	public static void loadBean(HierarchicalConfiguration context, Object beanObject, ConvertUtilsBean converter)   
	{
		PropertyUtilsBean beanUtils = new PropertyUtilsBean(); 
		
		PropertyDescriptor[] descriptors = beanUtils.getPropertyDescriptors(beanObject);

		try
		{
			for ( PropertyDescriptor descr : descriptors )
			{
				//check that setter exists
				if ( descr.getWriteMethod() != null )
				{
					String value = context.getString(descr.getName());
					
					if ( converter.lookup(descr.getPropertyType()) != null )
						BeanUtils.setProperty(beanObject, descr.getName(), converter.convert(value, descr.getPropertyType()));
				}
			}
		}
		catch ( Exception e )
		{
			throw new EPSCommonException(e);
		}
	}
	
	
	public static void loadBean(HierarchicalConfiguration context, Object beanObject)   
	{
		ConvertUtilsBean converter = new ConvertUtilsBean();
		
		loadBean(context, beanObject, converter);
	}

	
	
	public static void saveBean(HierarchicalConfiguration context, Object beanObject)
	{
		ConvertUtilsBean converter = new ConvertUtilsBean();
		
		PropertyUtilsBean beanUtils = new PropertyUtilsBean(); 
		
		PropertyDescriptor[] descriptors = beanUtils.getPropertyDescriptors(beanObject);

		try
		{
			for ( PropertyDescriptor descr : descriptors )
			{
				//check that setter exists
				if ( descr.getWriteMethod() != null )
				{
					Object value = BeanUtils.getProperty(beanObject, descr.getName());
					
					context.setProperty(descr.getName(), converter.convert(value));
				}
			}
		}
		catch ( Exception e )
		{
			throw new EPSCommonException(e);
		}
		
	}
	
	
	public static void copyBean(Object beanOrig, Object beanClone)
	{
		PropertyUtilsBean beanUtils = new PropertyUtilsBean();
		
		try
		{
			beanUtils.copyProperties(beanClone, beanOrig);
		}
		catch ( Exception e )
		{
			throw new EPSCommonException(e);
		}
		
	}
	
}
