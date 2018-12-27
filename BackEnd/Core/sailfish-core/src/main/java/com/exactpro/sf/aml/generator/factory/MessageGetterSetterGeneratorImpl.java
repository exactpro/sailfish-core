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

import com.exactpro.sf.aml.AMLException;
import com.exactpro.sf.aml.AMLLangConst;
import com.exactpro.sf.aml.generator.IGetterSetterGenerator;
import com.exactpro.sf.common.messages.IMessage;

/**
 * @author nikita.smirnov
 *
 */
public class MessageGetterSetterGeneratorImpl implements IGetterSetterGenerator {

	/* (non-Javadoc)
     * @see com.exactpro.sf.aml.generator.IGetterSetterGenerator#getGetter(java.lang.Class, java.lang.String)
	 */
	@Override
    public String getGetter(Class<?> type, String parameterName, String source)
			throws AMLException {
        return source + ".getField(\"" + parameterName + "\")";
	}

	/* (non-Javadoc)
     * @see com.exactpro.sf.aml.generator.IGetterSetterGenerator#getSetter(java.lang.Class, java.lang.String, java.lang.String, boolean)
	 */
	@Override
	public String getSetter(Class<?> type, String parameterName, String value,
			boolean isReference) throws AMLException {
		if (isReference) {
			return ".addField(\""+parameterName+"\", "+value+")";
		}
		return ".addField(\""+parameterName+"\", \""+value+"\")";
	}

	/* (non-Javadoc)
     * @see com.exactpro.sf.aml.generator.IGetterSetterGenerator#addSubmessage(java.lang.Class, java.lang.String, java.lang.String, java.lang.Class)
	 */
	@Override
	public String addSubmessage(Class<?> type, String parameterName,
			String value, Class<?> paramClass) throws AMLException {
		return ".addField(\""+parameterName+":"+value+"\", ("+type.getCanonicalName()+")"+AMLLangConst.MAP_NAME+".get(\""+value+"\"))";
	}

	/* (non-Javadoc)
     * @see com.exactpro.sf.aml.generator.IGetterSetterGenerator#getSubmessageClass(java.lang.Class, java.lang.String, java.lang.Class)
	 */
	@Override
	public Class<?> getSubmessageClass(Class<?> type, String childClassName,
			Class<?> paramClass) throws AMLException {
		return IMessage.class;
	}

	/* (non-Javadoc)
     * @see com.exactpro.sf.aml.generator.IGetterSetterGenerator#getMethodForExtractingTreeEntity()
	 */
	@Override
	public String getMethodForExtractingTreeEntity() throws AMLException {
		return "";
	}

}
