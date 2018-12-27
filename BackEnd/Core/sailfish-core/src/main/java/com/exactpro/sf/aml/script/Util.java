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

/**
 * Collection of various wide used utilities. 
 * @author dmitry.guriev
 *
 */
public class Util {

	public final static String EOL = System.getProperty("line.separator");

	private Util()
	{
		// hide constructor
	}

	/**
	 * Cast object to desired class.
	 * @param <T> desired class
	 * @param o initial object
	 * @return converted object
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Object> T cast(Object o)
	{
		return (T)o;
	}
	
}
