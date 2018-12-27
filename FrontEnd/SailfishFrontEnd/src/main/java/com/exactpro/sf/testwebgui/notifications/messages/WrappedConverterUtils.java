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
package com.exactpro.sf.testwebgui.notifications.messages;

import org.apache.commons.beanutils.ConvertUtilsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WrappedConverterUtils extends ConvertUtilsBean {

	private static final Logger logger = LoggerFactory.getLogger(WrappedConverterUtils.class);

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public Object convert(String value, Class clazz) {
		if (clazz.isEnum()){
			Object result = null;
			try {
				result = Enum.valueOf(clazz, value);
			} catch (IllegalArgumentException e) {
				logger.error("Can't convert {}", value, e);
				throw new RuntimeException(e);
			}
			return result;
		} else {
            return super.convert(value, clazz);
		}
	}

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public Object convert(Object value, Class clazz) {
		if (clazz.isEnum()){
			Object result = null;
			try {
				result = Enum.valueOf(clazz, (String) value);
			} catch (IllegalArgumentException e) {
				logger.error("Can't convert {}", value, e);
				throw new RuntimeException(e);
			}
			return result;
		} else {
            return super.convert(value, clazz);
		}
	}

}
