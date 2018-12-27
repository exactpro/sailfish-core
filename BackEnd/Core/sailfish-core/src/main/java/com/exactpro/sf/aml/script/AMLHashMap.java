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
package com.exactpro.sf.aml.script;

import java.util.HashMap;

import com.exactpro.sf.common.util.EPSCommonException;

public class AMLHashMap<K, V> extends HashMap<K, V> {

	/**
	 *
	 */
	private static final long serialVersionUID = 7862823666505415930L;

	@Override
	public V get(Object obj)
	{
		V v = super.get(obj);
		if (v == null) {
			throw new EPSCommonException("Message is not available by reference '"+obj+"'");
		}
		return v;
	}
}
