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
package com.exactpro.sf.aml.scriptutil;

import org.mvel2.integration.PropertyHandler;
import org.mvel2.integration.VariableResolverFactory;

import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;

public class IMessagePropertyHandler implements PropertyHandler {

	@Override
	public Object getProperty(
			String propName,
			Object object,
			VariableResolverFactory arg2) {
		if (object instanceof IMessage) {
			IMessage iMessage = (IMessage)object;
            Object propertyValue = iMessage.getField(propName);
            if(propertyValue instanceof IFilter){
                try {
                    propertyValue = ((IFilter) propertyValue).getValue();
                } catch(MvelException e) {
                    throw new EPSCommonException("Failed to get value: probably wrong filter type", e);
                }
            }
			return propertyValue;
		}
		throw new IllegalArgumentException("object is not an IMessage");
	}

	@Override
	public Object setProperty(
			String arg0,
			Object arg1,
			VariableResolverFactory arg2,
			Object arg3) {
		// TODO Auto-generated method stub
	    throw new UnsupportedOperationException("setting property for IMessage is not supported");
	}

}
