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
package com.exactpro.sf.testwebgui.dictionaries;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FacesConverter("classNameConverter")
public class ClassNameConverter implements Converter {

	private static final Logger logger = LoggerFactory.getLogger(ClassNameConverter.class);

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {

		if (StringUtils.isEmpty(value)) return null;

		Class<?> result = null;

		try {
			result = Class.forName(value);
		} catch (ClassNotFoundException e) {
			logger.error("Class {} not found", value, e);
		}

		return result;
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {

		if (!(value instanceof Class<?>)) return null;

		return ((Class<?>) value).getName();
	}

}
