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
package com.exactpro.sf.aml;

import java.util.ArrayList;

import com.exactpro.sf.common.util.EPSCommonException;

public class AMLList<T> extends ArrayList<T> {

	boolean THROW = false;

	/**
	 *
	 */
	private static final long serialVersionUID = -5260897237446841671L;

	@Override
	public boolean add(T e) {
		if (THROW)
			throw new EPSCommonException(e.toString());
		return super.add(e);
	}
}
