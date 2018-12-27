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
package com.exactpro.sf.services.ntg;


/**
 * Defines constants for keys for accessing values stored in the Map<String, object>
 * of Protocol attributes.
 */
public enum NTGProtocolAttribute
{
	/**
	 * Defines key for the specifying value of data offset protocol attribute.
	 */
	Offset,

	/**
	 * Defines key for the specifying value of data length protocol attribute.
	 */
	Length,

	/**
	 * Defines key for the specifying value of data format protocol attribute.
	 */
	Format,
	
	/**
	 * Defines key for the specifying value of data type protocol attribute.
	 */
	Type;
}
