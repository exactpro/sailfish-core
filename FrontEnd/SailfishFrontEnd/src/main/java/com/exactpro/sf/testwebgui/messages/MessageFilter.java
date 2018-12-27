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
package com.exactpro.sf.testwebgui.messages;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageFilter<T> {

	private static final Logger logger = LoggerFactory.getLogger(MessageFilter.class);

	public List<T> filter(Map<String, Object> map, List<T> objs) {

		List<T> result = new ArrayList<T>();

		for (T obj : objs) {

			boolean r = true;

			for (String field : map.keySet()) {
				try {
					Field f = obj.getClass().getDeclaredField(field);
					f.setAccessible(true);
					String value = f.get(obj).toString();
					String filterText = String.valueOf(map.get(field));

					int index = filterText.indexOf('*');
					if(index < 0) {
						r = r && value.contains(filterText);
					} else if (index == 0){
                        filterText = filterText.substring(1);
						r = r && value.endsWith(filterText);
					} else if (index == filterText.length()-1) {
                        filterText = filterText.substring(0, filterText.length() - 2);
						r = r && value.startsWith(filterText);
					} else {
                        String prefix = filterText.substring(0, filterText.indexOf("*"));
                        String suffix = filterText.substring(filterText.indexOf("*") + 1, filterText.length());
						boolean isStartsWith = value.startsWith(prefix);
						boolean isEndsWith = value.endsWith(suffix);
						r = r && isStartsWith && isEndsWith;
					}


				} catch (Exception e) {
					logger.error("{} {}", e.getMessage(), field, e);
				}
			}

			if (r) {
				result.add(obj);
			}

		}

		return result;

	}

}
